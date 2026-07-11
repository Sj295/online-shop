package com.shop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("oms_cart_item")
public class CartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long productId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
