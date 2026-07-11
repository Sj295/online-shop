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
}
