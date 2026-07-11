package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.OrderItem;

import java.util.List;

public interface OrderItemService extends IService<OrderItem> {

    void saveBatchItems(List<OrderItem> items);

    List<OrderItem> listByOrderId(Long orderId);
}
