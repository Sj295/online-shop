package com.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreateTask implements Serializable {

    private String orderNo;
    private Long userId;
    private Long addressId;
    private String remark;
    private List<OrderCreateItem> items;
    private long createTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderCreateItem implements Serializable {
        private Long productId;
        private Long skuId;
        private Integer quantity;
    }
}
