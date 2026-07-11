package com.shop.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressDTO {

    @NotBlank(message = "收货人不能为空")
    private String receiverName;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    private String province;
    private String city;
    private String district;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @NotNull(message = "是否默认不能为空")
    private Integer isDefault;
}
