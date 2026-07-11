package com.shop.common.limit;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private RedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() throws IOException {
        ClassPathResource resource = new ClassPathResource("scripts/rate_limit.lua");
        String script = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        rateLimitScript = RedisScript.of(script, Long.class);
    }

    public boolean allow(String key, int maxRequests, int windowSeconds) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(key),
                    String.valueOf(windowSeconds),
                    String.valueOf(maxRequests)
            );
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("rate limit execute error, key={}", key, e);
            // 限流组件异常时放行，避免误杀
            return true;
        }
    }
}
