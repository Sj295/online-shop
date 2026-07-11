package com.shop.common.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.dto.OrderCreateTask;
import com.shop.common.stock.RedisStockService;
import com.shop.common.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutRefundJob {

    private static final long TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStockService redisStockService;
    private final OrderCreateProducer producer;

    @Scheduled(fixedDelay = 60_000)
    public void refundTimeoutOrders() {
        Set<String> keys = redisTemplate.keys(RedisKeyUtil.orderCreateStatusKey("*"));
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            try {
                String json = redisTemplate.opsForValue().get(key);
                if (json == null) {
                    continue;
                }
                OrderCreateProducer.StatusValue status = objectMapper.readValue(json, OrderCreateProducer.StatusValue.class);
                if (OrderCreateStatus.PENDING.name().equals(status.getStatus())
                        || OrderCreateStatus.PROCESSING.name().equals(status.getStatus())) {
                    if (System.currentTimeMillis() - status.getCreateTime() > TIMEOUT_MILLIS) {
                        String orderNo = key.substring(key.lastIndexOf(":") + 1);
                        refundTask(orderNo);
                        producer.setStatus(orderNo, OrderCreateStatus.FAILED, "订单处理超时，库存已回滚");
                        log.warn("Order create timeout refunded, orderNo={}", orderNo);
                    }
                }
            } catch (Exception e) {
                log.error("Refund timeout order error, key={}", key, e);
            }
        }
    }

    private void refundTask(String orderNo) {
        try {
            String payload = redisTemplate.opsForValue().get(RedisKeyUtil.orderCreateTaskKey(orderNo));
            if (payload == null) {
                return;
            }
            OrderCreateTask task = objectMapper.readValue(payload, OrderCreateTask.class);
            if (task.getItems() != null) {
                for (OrderCreateTask.OrderCreateItem item : task.getItems()) {
                    redisStockService.refundProductStock(item.getProductId(), item.getQuantity());
                    redisStockService.refundSkuStock(item.getSkuId(), item.getQuantity());
                }
            }
        } catch (Exception e) {
            log.error("Refund timeout task error, orderNo={}", orderNo, e);
        }
    }
}
