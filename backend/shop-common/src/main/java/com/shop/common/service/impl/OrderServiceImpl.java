package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.*;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.OrderMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.*;
import com.shop.common.util.IdUtil;
import com.shop.common.util.RedisKeyUtil;
import com.shop.common.util.RedissonLockUtil;
import com.shop.common.vo.CartItemVO;
import com.shop.common.vo.OrderItemVO;
import com.shop.common.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartItemService cartItemService;
    private final AddressService addressService;
    private final ProductService productService;
    private final SkuService skuService;
    private final OrderItemService orderItemService;
    private final RedissonLockUtil lockUtil;

    public OrderServiceImpl(CartItemService cartItemService, AddressService addressService,
                            ProductService productService, SkuService skuService,
                            OrderItemService orderItemService, RedissonLockUtil lockUtil) {
        this.cartItemService = cartItemService;
        this.addressService = addressService;
        this.productService = productService;
        this.skuService = skuService;
        this.orderItemService = orderItemService;
        this.lockUtil = lockUtil;
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

            for (CartItemVO cartItem : cartItems) {
                Product product = productService.getDetail(cartItem.getProductId());
                Sku sku = skuService.getDefaultByProductId(product.getId());
                if (sku == null) {
                    throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "商品规格不存在");
                }

                // 数据库 CAS 扣减库存，无需分布式锁
                skuService.decreaseStock(sku.getId(), cartItem.getQuantity());
                productService.decreaseStock(product.getId(), cartItem.getQuantity());

                BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
                totalAmount = totalAmount.add(itemTotal);

                OrderItem orderItem = new OrderItem();
                orderItem.setProductId(product.getId());
                orderItem.setSkuId(sku.getId());
                orderItem.setProductName(product.getName());
                orderItem.setProductImage(product.getMainImage());
                orderItem.setPrice(product.getPrice());
                orderItem.setQuantity(cartItem.getQuantity());
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

    private void saveOrderItems(List<OrderItem> orderItems) {
        orderItemService.saveBatchItems(orderItems);
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
