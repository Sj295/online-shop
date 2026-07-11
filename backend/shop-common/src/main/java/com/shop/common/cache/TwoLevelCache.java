package com.shop.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final Cache<String, Object> localCache;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;
    private final Random random = new Random();

    public TwoLevelCache(String name, Cache<String, Object> localCache,
                         StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                         Duration defaultTtl) {
        super(true);
        this.name = name;
        this.localCache = localCache;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = defaultTtl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    protected Object lookup(Object key) {
        String k = keyOf(key);
        Object value = localCache.getIfPresent(k);
        if (value != null) {
            log.debug("L1 hit: {}", k);
            return value;
        }
        String json = redisTemplate.opsForValue().get(k);
        if (json != null) {
            Object redisValue = deserialize(json);
            if (redisValue != null) {
                localCache.put(k, redisValue);
            }
            return redisValue;
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        Object value = lookup(key);
        if (value != null) {
            return (T) value;
        }
        String k = keyOf(key);
        try {
            T loaded = valueLoader.call();
            put(key, loaded);
            return loaded;
        } catch (Exception e) {
            throw new ValueRetrievalException(k, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            return;
        }
        String k = keyOf(key);
        localCache.put(k, value);
        long jitterSeconds = Math.max(1, defaultTtl.getSeconds() / 10);
        long ttlSeconds = defaultTtl.getSeconds() + random.nextInt((int) jitterSeconds);
        String json = serialize(value);
        if (json != null) {
            redisTemplate.opsForValue().set(k, json, ttlSeconds, TimeUnit.SECONDS);
        }
    }

    @Override
    public void evict(Object key) {
        String k = keyOf(key);
        localCache.invalidate(k);
        redisTemplate.delete(k);
    }

    @Override
    public void clear() {
        localCache.invalidateAll();
        // 不主动清空 Redis，避免误删其他缓存
    }

    private String keyOf(Object key) {
        return name + ":" + key;
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("cache serialize error", e);
            return null;
        }
    }

    private Object deserialize(String json) {
        try {
            return objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), Object.class);
        } catch (Exception e) {
            log.error("cache deserialize error", e);
            return null;
        }
    }
}
