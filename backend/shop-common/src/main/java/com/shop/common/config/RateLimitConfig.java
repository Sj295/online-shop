package com.shop.common.config;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.limit.RateLimiter;
import com.shop.common.result.ResultCode;
import com.shop.common.util.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        log.info("RateLimitConfig loaded");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering order create rate limit interceptor");
        registry.addInterceptor(new OrderCreateRateLimitInterceptor(rateLimiter))
                .addPathPatterns("/api/order/create")
                .order(1);
    }

    @RequiredArgsConstructor
    public static class OrderCreateRateLimitInterceptor implements HandlerInterceptor {

        private final RateLimiter rateLimiter;

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            String ip = getClientIp(request);
            if (!rateLimiter.allow("shop:rate:order:ip:" + ip, 200, 60)) {
                log.warn("IP rate limit hit: {}", ip);
                WebUtil.writeError(response, ResultCode.RATE_LIMIT);
                return false;
            }

            try {
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    if (!rateLimiter.allow("shop:rate:order:user:" + userId, 30, 60)) {
                        log.warn("User rate limit hit: {}", userId);
                        WebUtil.writeError(response, ResultCode.RATE_LIMIT);
                        return false;
                    }
                }
            } catch (Exception ignored) {
                // 未登录或异常时不做用户维度限流
            }
            return true;
        }

        private String getClientIp(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip : "unknown";
        }
    }
}
