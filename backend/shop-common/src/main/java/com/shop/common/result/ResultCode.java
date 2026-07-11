package com.shop.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "success"),
    ERROR(500, "server error"),
    PARAM_ERROR(400, "parameter error"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    BUSINESS_ERROR(1000, "business error"),
    STOCK_NOT_ENOUGH(1001, "stock not enough"),
    ORDER_NOT_FOUND(1002, "order not found"),
    PRODUCT_NOT_FOUND(1003, "product not found"),
    CART_EMPTY(1004, "cart is empty"),
    PAYMENT_FAILED(1005, "payment failed");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
