package com.shop.common.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class HomeVO {

    private List<? extends Object> carousels;
    private List<? extends Object> hotProducts;
    private List<? extends Object> newProducts;
    private List<? extends Object> categories;
}
