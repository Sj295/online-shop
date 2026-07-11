package com.shop.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.dto.OrderShipDTO;
import com.shop.common.entity.Order;
import com.shop.common.result.Result;
import com.shop.common.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/list")
    public Result<IPage<Order>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(orderService.lambdaQuery()
                .eq(status != null, Order::getStatus, status)
                .orderByDesc(Order::getCreateTime)
                .page(new Page<>(page, size)));
    }

    @PostMapping("/ship")
    public Result<Void> ship(@RequestBody OrderShipDTO dto) {
        Order order = orderService.lambdaQuery().eq(Order::getOrderNo, dto.getOrderNo()).one();
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (order.getStatus() != 1) {
            return Result.error("订单状态异常");
        }
        orderService.lambdaUpdate().eq(Order::getId, order.getId())
                .set(Order::getStatus, 2)
                .set(Order::getDeliverTime, LocalDateTime.now())
                .update();
        return Result.success();
    }
}
