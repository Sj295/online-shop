package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.dto.OrderCreateResponse;
import com.shop.common.dto.OrderCreateStatusResponse;
import com.shop.common.dto.OrderCreateTask;
import com.shop.common.entity.*;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.OrderMapper;
import com.shop.common.mq.OrderCreateProducer;
import com.shop.common.mq.OrderCreateStatus;
import com.shop.common.result.ResultCode;
import com.shop.common.service.*;
import com.shop.common.stock.RedisStockService;
import com.shop.common.util.IdUtil;
import com.shop.common.util.RedisKeyUtil;
import com.shop.common.util.RedissonLockUtil;
import com.shop.common.vo.CartItemVO;
import com.shop.common.vo.OrderItemVO;
import com.shop.common.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartItemService cartItemService;
    private final AddressService addressService;
    private final ProductService productService;
    private final SkuService skuService;
    private final OrderItemService orderItemService;
    private final RedissonLockUtil lockUtil;
    private final RedisStockService redisStockService;
    private final OrderCreateProducer orderCreateProducer;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(CartItemService cartItemService, AddressService addressService,
                            ProductService productService, SkuService skuService,
                            OrderItemService orderItemService, RedissonLockUtil lockUtil,
                            RedisStockService redisStockService,
                            OrderCreateProducer orderCreateProducer,
                            StringRedisTemplate redisTemplate,
                            ObjectMapper objectMapper) {
        this.cartItemService = cartItemService;
        this.addressService = addressService;
        this.productService = productService;
        this.skuService = skuService;
        this.orderItemService = orderItemService;
        this.lockUtil = lockUtil;
        this.redisStockService = redisStockService;
        this.orderCreateProducer = orderCreateProducer;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String createOrder(Long userId, Long addressId, String remark) {
        String cartLockKey = RedisKeyUtil.cartLockKey(userId);
        if (!lockUtil.tryLock(cartLockKey, 3, 10, TimeUnit.SECONDS)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "订单处理中，请稍后再试");
        }

        try {
            Address address = addressService.getById(addressId);
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "收货地址不存在");
            }

            List<CartItemVO> cartItems = cartItemService.listCart(userId).stream()
                    .filter(item -> item.getSelected() != null && item.getSelected() == 1)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(cartItems)) {
                throw new BusinessException(ResultCode.CART_EMPTY);
            }

            String orderNo = IdUtil.generateOrderNo();
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            List<StockRecord> deductedRecords = new ArrayList<>();

            for (CartItemVO cartItem : cartItems) {
                Product product = productService.getDetail(cartItem.getProductId());
                Sku sku = skuService.getDefaultByProductId(product.getId());
                if (sku == null) {
                    rollbackRedisStock(deductedRecords);
                    throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "商品规格不存在");
                }

                int quantity = cartItem.getQuantity();
                boolean redisOk = redisStockService.deductProductStock(product.getId(), quantity)
                        && redisStockService.deductSkuStock(sku.getId(), quantity);
                if (!redisOk) {
                    rollbackRedisStock(deductedRecords);
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
                }
                deductedRecords.add(new StockRecord(product.getId(), sku.getId(), quantity));

                try {
                    // 数据库 CAS 扣减库存，作为最终兜底
                    skuService.decreaseStock(sku.getId(), quantity);
                    productService.decreaseStock(product.getId(), quantity);
                } catch (BusinessException e) {
                    rollbackRedisStock(deductedRecords);
                    throw e;
                }

                BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(quantity));
                totalAmount = totalAmount.add(itemTotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setSkuId(sku.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getMainImage());
                orderItem.setPrice(product.getPrice());
                orderItem.setQuantity(quantity);
                orderItem.setTotalAmount(itemTotal);
                orderItems.add(orderItem);
            }

            BigDecimal freightAmount = BigDecimal.ZERO;
            BigDecimal payAmount = totalAmount.add(freightAmount);

            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setTotalAmount(totalAmount);
            order.setPayAmount(payAmount);
            order.setFreightAmount(freightAmount);
            order.setStatus(0);
            order.setReceiverName(address.getReceiverName());
            order.setReceiverPhone(address.getPhone());
            order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
            order.setRemark(remark);
            order.setCreateTime(LocalDateTime.now());
            save(order);

            for (OrderItem orderItem : orderItems) {
                orderItem.setOrderId(order.getId());
            }
            saveOrderItems(orderItems);

            cartItemService.clearSelected(userId);

            return orderNo;
        } finally {
            lockUtil.unlock(cartLockKey);
        }
    }

    private void rollbackRedisStock(List<StockRecord> records) {
        for (StockRecord record : records) {
            redisStockService.refundProductStock(record.productId(), record.quantity());
            redisStockService.refundSkuStock(record.skuId(), record.quantity());
        }
    }

    private record StockRecord(Long productId, Long skuId, Integer quantity) {
    }

    private void saveOrderItems(List<OrderItem> orderItems) {
        orderItemService.saveBatchItems(orderItems);
    }

    @Override
    public OrderCreateResponse submitOrder(Long userId, Long addressId, String remark) {
        String cartLockKey = RedisKeyUtil.cartLockKey(userId);
        if (!lockUtil.tryLock(cartLockKey, 3, 10, TimeUnit.SECONDS)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "订单处理中，请稍后再试");
        }

        try {
            Address address = addressService.getById(addressId);
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "收货地址不存在");
            }

            List<CartItemVO> cartItems = cartItemService.listCart(userId).stream()
                    .filter(item -> item.getSelected() != null && item.getSelected() == 1)
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(cartItems)) {
                throw new BusinessException(ResultCode.CART_EMPTY);
            }

            List<OrderCreateTask.OrderCreateItem> taskItems = new ArrayList<>();
            for (CartItemVO cartItem : cartItems) {
                Product product = productService.getDetail(cartItem.getProductId());
                Sku sku = skuService.getDefaultByProductId(product.getId());
                if (sku == null) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "商品规格不存在");
                }

                // 用户+商品维度幂等锁，防止重复提交
                String idempotencyKey = RedisKeyUtil.orderCreateLockKey(userId, product.getId());
                Boolean locked = redisTemplate.opsForValue()
                        .setIfAbsent(idempotencyKey, "1", Duration.ofSeconds(10));
                if (Boolean.FALSE.equals(locked)) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "订单提交过于频繁");
                }

                int quantity = cartItem.getQuantity();
                boolean redisOk = redisStockService.deductProductStock(product.getId(), quantity)
                        && redisStockService.deductSkuStock(sku.getId(), quantity);
                if (!redisOk) {
                    rollbackRedisStockForItems(taskItems);
                    // 当前商品已扣减成功但后续失败时，也需要回滚当前商品
                    redisStockService.refundProductStock(product.getId(), quantity);
                    redisStockService.refundSkuStock(sku.getId(), quantity);
                    throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
                }

                taskItems.add(OrderCreateTask.OrderCreateItem.builder()
                        .productId(product.getId())
                        .skuId(sku.getId())
                        .quantity(quantity)
                        .build());
            }

            String orderNo = IdUtil.generateOrderNo();
            OrderCreateTask task = OrderCreateTask.builder()
                    .orderNo(orderNo)
                    .userId(userId)
                    .addressId(addressId)
                    .remark(remark)
                    .items(taskItems)
                    .createTime(System.currentTimeMillis())
                    .build();

            orderCreateProducer.sendOrderCreateTask(task);
            cartItemService.clearSelected(userId);

            return OrderCreateResponse.builder()
                    .orderNo(orderNo)
                    .status(OrderCreateStatus.PENDING.name())
                    .build();
        } finally {
            lockUtil.unlock(cartLockKey);
        }
    }

    @Override
    @Transactional
    public void processAsyncOrder(OrderCreateTask task) {
        Address address = addressService.getById(task.getAddressId());
        if (address == null || !address.getUserId().equals(task.getUserId())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "收货地址不存在");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderCreateTask.OrderCreateItem item : task.getItems()) {
            Product product = productService.getDetail(item.getProductId());
            Sku sku = skuService.getDefaultByProductId(product.getId());
            if (sku == null) {
                throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "商品规格不存在");
            }

            int quantity = item.getQuantity();
            skuService.decreaseStock(sku.getId(), quantity);
            productService.decreaseStock(product.getId(), quantity);

            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(quantity));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setSkuId(sku.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setTotalAmount(itemTotal);
            orderItems.add(orderItem);
        }

        BigDecimal payAmount = totalAmount;

        Order order = new Order();
        order.setOrderNo(task.getOrderNo());
        order.setUserId(task.getUserId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setStatus(0);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(task.getRemark());
        order.setCreateTime(LocalDateTime.now());
        save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
        }
        saveOrderItems(orderItems);
    }

    @Override
    public OrderCreateStatusResponse getCreateStatus(Long userId, String orderNo) {
        String key = RedisKeyUtil.orderCreateStatusKey(orderNo);
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) {
            try {
                OrderCreateProducer.StatusValue status = objectMapper.readValue(json, OrderCreateProducer.StatusValue.class);
                return OrderCreateStatusResponse.builder()
                        .orderNo(orderNo)
                        .status(status.getStatus())
                        .message(status.getMessage())
                        .build();
            } catch (Exception e) {
                log.error("parse order create status error, orderNo={}", orderNo, e);
            }
        }

        // 状态缓存已过期或不存在，回查数据库
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId).one();
        if (order != null) {
            return OrderCreateStatusResponse.builder()
                    .orderNo(orderNo)
                    .status(OrderCreateStatus.SUCCESS.name())
                    .message("订单创建成功")
                    .build();
        }
        return OrderCreateStatusResponse.builder()
                .orderNo(orderNo)
                .status(OrderCreateStatus.FAILED.name())
                .message("订单创建失败或已超时")
                .build();
    }

    private void rollbackRedisStockForItems(List<OrderCreateTask.OrderCreateItem> items) {
        for (OrderCreateTask.OrderCreateItem item : items) {
            redisStockService.refundProductStock(item.getProductId(), item.getQuantity());
            redisStockService.refundSkuStock(item.getSkuId(), item.getQuantity());
        }
    }

    @Override
    @Transactional
    public void payOrder(Long userId, String orderNo) {
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId).one();
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "订单状态异常");
        }
        // 模拟支付：随机成功率 99%
        if (Math.random() < 0.01) {
            throw new BusinessException(ResultCode.PAYMENT_FAILED);
        }
        lambdaUpdate().eq(Order::getId, order.getId())
                .set(Order::getStatus, 1)
                .set(Order::getPayTime, LocalDateTime.now())
                .update();
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId).one();
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "只能取消待付款订单");
        }
        // 回退库存
        // 简化处理：查询订单项并回退
        lambdaUpdate().eq(Order::getId, order.getId()).set(Order::getStatus, 4).update();
    }

    @Override
    public OrderVO getOrderDetail(Long userId, String orderNo) {
        Order order = lambdaQuery().eq(Order::getOrderNo, orderNo).eq(Order::getUserId, userId).one();
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        return convertToVO(order);
    }

    @Override
    public List<OrderVO> listOrders(Long userId, Integer status) {
        List<Order> orders = lambdaQuery().eq(Order::getUserId, userId)
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime).list();
        return orders.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusText(getStatusText(order.getStatus()));
        return vo;
    }

    private String getStatusText(Integer status) {
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "已付款";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
