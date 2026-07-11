package com.shop.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderCreateDTO {

    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    private String remark;
}
