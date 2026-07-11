package com.shop.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemVO {

    private Long id;
    private Long userId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;

    private String productName;
    private String subtitle;
    private String productImage;
    private BigDecimal price;
}
