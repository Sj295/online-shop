package com.shop.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartUpdateDTO {

    @NotNull(message = "购物车ID不能为空")
    private Long cartItemId;

    @Min(value = 0, message = "数量不能为负数")
    private Integer quantity;

    private Integer selected;
}
