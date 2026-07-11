package com.shop.common.stock;

import com.shop.common.util.RedisKeyUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStockService {

    private final StringRedisTemplate redisTemplate;

    private RedisScript<Long> stockDeductScript;
    private RedisScript<Long> stockRefundScript;

    @PostConstruct
    public void init() throws IOException {
        String deduct = StreamUtils.copyToString(
                new ClassPathResource("scripts/stock_deduct.lua").getInputStream(), StandardCharsets.UTF_8);
        this.stockDeductScript = RedisScript.of(deduct, Long.class);

        String refund = StreamUtils.copyToString(
                new ClassPathResource("scripts/stock_refund.lua").getInputStream(), StandardCharsets.UTF_8);
        this.stockRefundScript = RedisScript.of(refund, Long.class);
    }

    public void initProductStock(Long productId, Integer stock) {
        redisTemplate.opsForValue().set(RedisKeyUtil.productStockKey(productId), String.valueOf(stock));
    }

    public void initSkuStock(Long skuId, Integer stock) {
        redisTemplate.opsForValue().set(RedisKeyUtil.skuStockKey(skuId), String.valueOf(stock));
    }

    public boolean deductProductStock(Long productId, Integer quantity) {
        return deduct(RedisKeyUtil.productStockKey(productId), quantity);
    }

    public boolean deductSkuStock(Long skuId, Integer quantity) {
        return deduct(RedisKeyUtil.skuStockKey(skuId), quantity);
    }

    public void refundProductStock(Long productId, Integer quantity) {
        refund(RedisKeyUtil.productStockKey(productId), quantity);
    }

    public void refundSkuStock(Long skuId, Integer quantity) {
        refund(RedisKeyUtil.skuStockKey(skuId), quantity);
    }

    private boolean deduct(String key, Integer quantity) {
        try {
            Long result = redisTemplate.execute(stockDeductScript,
                    Collections.singletonList(key), String.valueOf(quantity));
            return result != null && result >= 0;
        } catch (Exception e) {
            log.error("deduct stock error, key={}", key, e);
            // Redis 异常时降级到数据库兜底
            return true;
        }
    }

    private void refund(String key, Integer quantity) {
        try {
            redisTemplate.execute(stockRefundScript,
                    Collections.singletonList(key), String.valueOf(quantity));
        } catch (Exception e) {
            log.error("refund stock error, key={}", key, e);
        }
    }
}
