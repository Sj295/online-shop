package com.shop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pms_sku")
public class Sku {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private String skuCode;
    private String skuSpecs;
    private BigDecimal price;
    private Integer stock;
    private Integer saleCount;
    private Integer status;

    @Version
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
