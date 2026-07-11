package com.shop.common.util;

public class RedisKeyUtil {

    private static final String PREFIX = "shop:";

    public static final String HOT_PRODUCTS = PREFIX + "hot_products";
    public static final String NEW_PRODUCTS = PREFIX + "new_products";
    public static final String CATEGORIES = PREFIX + "categories";
    public static final String CAROUSELS = PREFIX + "carousels";

    public static String productKey(Long productId) {
        return PREFIX + "product:" + productId;
    }

    public static String cartKey(Long userId) {
        return PREFIX + "cart:" + userId;
    }

    public static String orderLockKey(String orderNo) {
        return PREFIX + "lock:order:" + orderNo;
    }

    public static String cartLockKey(Long userId) {
        return PREFIX + "lock:cart:" + userId;
    }

    public static String stockLockKey(Long productId) {
        return PREFIX + "lock:stock:" + productId;
    }

    public static String idempotentKey(String key) {
        return PREFIX + "idempotent:" + key;
    }

    public static String productCacheKey(Long productId) {
        return PREFIX + "cache:product:" + productId;
    }

    public static String skuCacheKey(Long productId) {
        return PREFIX + "cache:sku:" + productId;
    }

    public static String addressCacheKey(Long addressId) {
        return PREFIX + "cache:address:" + addressId;
    }

    public static String productCacheLockKey(Long productId) {
        return PREFIX + "lock:cache:product:" + productId;
    }

    public static String productStockKey(Long productId) {
        return PREFIX + "stock:product:" + productId;
    }

    public static String skuStockKey(Long skuId) {
        return PREFIX + "stock:sku:" + skuId;
    }

    public static String orderCreateQueueKey() {
        return PREFIX + "queue:order:create";
    }

    public static String orderCreateStatusKey(String orderNo) {
        return PREFIX + "order:create:status:" + orderNo;
    }

    public static String orderCreateTaskKey(String orderNo) {
        return PREFIX + "order:create:task:" + orderNo;
    }

    public static String orderCreateLockKey(Long userId, Long productId) {
        return PREFIX + "lock:order:create:" + userId + ":" + productId;
    }
}
