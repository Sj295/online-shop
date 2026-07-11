package com.shop.common.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.dto.OrderCreateTask;
import com.shop.common.util.RedisKeyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateProducer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String sendOrderCreateTask(OrderCreateTask task) {
        try {
            String payload = objectMapper.writeValueAsString(task);
            redisTemplate.opsForList().leftPush(RedisKeyUtil.orderCreateQueueKey(), payload);

            redisTemplate.opsForValue().set(
                    RedisKeyUtil.orderCreateTaskKey(task.getOrderNo()),
                    payload,
                    Duration.ofMinutes(30));

            setStatus(task.getOrderNo(), OrderCreateStatus.PENDING, "订单已提交，等待处理");
            log.info("Order create task sent, orderNo={}", task.getOrderNo());
            return task.getOrderNo();
        } catch (JsonProcessingException e) {
            log.error("serialize order create task error, orderNo={}", task.getOrderNo(), e);
            throw new RuntimeException("订单任务序列化失败", e);
        }
    }

    public void deleteTask(String orderNo) {
        redisTemplate.delete(RedisKeyUtil.orderCreateTaskKey(orderNo));
    }

    public void setStatus(String orderNo, OrderCreateStatus status, String message) {
        try {
            StatusValue value = StatusValue.builder()
                    .status(status.name())
                    .message(message)
                    .createTime(System.currentTimeMillis())
                    .build();
            redisTemplate.opsForValue().set(
                    RedisKeyUtil.orderCreateStatusKey(orderNo),
                    objectMapper.writeValueAsString(value),
                    Duration.ofMinutes(30));
        } catch (JsonProcessingException e) {
            log.error("set order create status error, orderNo={}", orderNo, e);
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class StatusValue {
        private String status;
        private String message;
        private long createTime;
    }
}
