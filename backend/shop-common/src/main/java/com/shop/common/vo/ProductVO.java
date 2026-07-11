package com.shop.common.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String subtitle;
    private String description;
    private String mainImage;
    private String subImages;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer saleCount;
    private Integer isHot;
    private Integer isNew;
}
