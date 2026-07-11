package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.OrderItem;
import com.shop.common.mapper.OrderItemMapper;
import com.shop.common.service.OrderItemService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {

    @Override
    public void saveBatchItems(List<OrderItem> items) {
        saveBatch(items);
    }

    @Override
    public List<OrderItem> listByOrderId(Long orderId) {
        return lambdaQuery().eq(OrderItem::getOrderId, orderId).list();
    }
}
