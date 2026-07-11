package com.shop.common.archive;

import com.shop.common.entity.Order;
import com.shop.common.entity.OrderArchive;
import com.shop.common.entity.OrderItem;
import com.shop.common.mapper.OrderArchiveMapper;
import com.shop.common.service.OrderItemService;
import com.shop.common.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderArchiveService {

    private final OrderService orderService;
    private final OrderItemService orderItemService;
    private final OrderArchiveMapper orderArchiveMapper;

    /**
     * 每天凌晨 3 点归档 90 天前已完成或已取消的订单。
     * 归档动作：将订单头迁移到 oms_order_archive，并删除原订单及明细。
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void archiveOldOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<Order> orders = orderService.lambdaQuery()
                .in(Order::getStatus, 3, 4)
                .lt(Order::getCreateTime, threshold)
                .last("LIMIT 500")
                .list();
        if (orders.isEmpty()) {
            return;
        }

        List<OrderArchive> archives = orders.stream().map(o -> {
            OrderArchive archive = new OrderArchive();
            BeanUtils.copyProperties(o, archive);
            archive.setArchiveTime(LocalDateTime.now());
            return archive;
        }).collect(Collectors.toList());
        for (OrderArchive archive : archives) {
            orderArchiveMapper.insert(archive);
        }

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        // 批量删除订单明细
        orderItemService.lambdaUpdate().in(OrderItem::getOrderId, orderIds).remove();
        // 批量删除订单
        orderService.removeByIds(orderIds);

        log.info("Archived {} orders older than {}", orders.size(), threshold);
    }
}
