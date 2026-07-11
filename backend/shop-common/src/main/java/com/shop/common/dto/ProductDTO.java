package com.shop.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String subtitle;
    private String description;
    private String mainImage;
    private String subImages;

    @NotNull(message = "售价不能为空")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private Integer isHot;
    private Integer isNew;
    private Integer sort;
}
