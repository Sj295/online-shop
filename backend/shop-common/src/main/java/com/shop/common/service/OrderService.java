package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Order;
import com.shop.common.vo.OrderVO;

import java.util.List;

public interface OrderService extends IService<Order> {

    String createOrder(Long userId, Long addressId, String remark);

    void payOrder(Long userId, String orderNo);

    void cancelOrder(Long userId, String orderNo);

    OrderVO getOrderDetail(Long userId, String orderNo);

    List<OrderVO> listOrders(Long userId, Integer status);
}
