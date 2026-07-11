package com.shop.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shop.common.cache.CacheNames;
import com.shop.common.cache.TwoLevelCache;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

@Slf4j
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @PostConstruct
    public void init() {
        log.info("CacheConfig loaded");
    }

    @Bean
    public CacheManager cacheManager(StringRedisTemplate redisTemplate) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        TwoLevelCache productCache = buildCache(CacheNames.PRODUCT, redisTemplate, objectMapper, Duration.ofMinutes(10));
        TwoLevelCache skuCache = buildCache(CacheNames.SKU, redisTemplate, objectMapper, Duration.ofMinutes(10));
        TwoLevelCache addressCache = buildCache(CacheNames.ADDRESS, redisTemplate, objectMapper, Duration.ofMinutes(30));

        return new CacheManager() {
            @Override
            public TwoLevelCache getCache(String name) {
                return switch (name) {
                    case CacheNames.PRODUCT -> productCache;
                    case CacheNames.SKU -> skuCache;
                    case CacheNames.ADDRESS -> addressCache;
                    default -> null;
                };
            }

            @Override
            public Collection<String> getCacheNames() {
                return List.of(CacheNames.PRODUCT, CacheNames.SKU, CacheNames.ADDRESS);
            }
        };
    }

    private TwoLevelCache buildCache(String name, StringRedisTemplate redisTemplate,
                                     ObjectMapper objectMapper, Duration ttl) {
        com.github.benmanes.caffeine.cache.Cache<String, Object> local = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(ttl.dividedBy(2))
                .build();
        return new TwoLevelCache(name, local, redisTemplate, objectMapper, ttl);
    }
}
