package com.shop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("oms_order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long productId;
    private Long skuId;
    private String productName;
    private String productImage;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalAmount;
    private LocalDateTime createTime;
}
