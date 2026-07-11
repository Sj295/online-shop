package com.shop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pms_product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;
    private String name;
    private String subtitle;
    private String description;
    private String mainImage;
    private String subImages;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer saleCount;
    private Integer status;
    private Integer isHot;
    private Integer isNew;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
