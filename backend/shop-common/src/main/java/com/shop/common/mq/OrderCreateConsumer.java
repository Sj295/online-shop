package com.shop.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.dto.OrderCreateTask;
import com.shop.common.service.OrderService;
import com.shop.common.stock.RedisStockService;
import com.shop.common.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final RedisStockService redisStockService;
    private final OrderCreateProducer producer;

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::consume, "order-create-consumer");
        worker.setDaemon(true);
        worker.start();
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String payload = redisTemplate.opsForList()
                        .rightPop(RedisKeyUtil.orderCreateQueueKey(), 2, TimeUnit.SECONDS);
                if (payload == null) {
                    continue;
                }
                processTask(payload);
            } catch (Exception e) {
                log.error("Order create consumer error", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processTask(String payload) {
        try {
            OrderCreateTask task = objectMapper.readValue(payload, OrderCreateTask.class);
            producer.setStatus(task.getOrderNo(), OrderCreateStatus.PROCESSING, "订单处理中");
            orderService.processAsyncOrder(task);
            producer.setStatus(task.getOrderNo(), OrderCreateStatus.SUCCESS, "订单创建成功");
            redisTemplate.delete(RedisKeyUtil.orderCreateTaskKey(task.getOrderNo()));
            log.info("Order created asynchronously, orderNo={}", task.getOrderNo());
        } catch (Exception e) {
            log.error("Async order create failed", e);
            try {
                OrderCreateTask task = objectMapper.readValue(payload, OrderCreateTask.class);
                refundStock(task);
                producer.setStatus(task.getOrderNo(), OrderCreateStatus.FAILED, "订单创建失败：" + e.getMessage());
            } catch (Exception ex) {
                log.error("Refund stock after failed order error", ex);
            }
        }
    }

    private void refundStock(OrderCreateTask task) {
        if (task == null || task.getItems() == null) {
            return;
        }
        for (OrderCreateTask.OrderCreateItem item : task.getItems()) {
            redisStockService.refundProductStock(item.getProductId(), item.getQuantity());
            redisStockService.refundSkuStock(item.getSkuId(), item.getQuantity());
        }
    }
}
