package com.shop.portal.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.dto.OrderCreateDTO;
import com.shop.common.result.Result;
import com.shop.common.service.OrderService;
import com.shop.common.vo.OrderVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public Result<String> create(@Valid @RequestBody OrderCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        String orderNo = orderService.createOrder(userId, dto.getAddressId(), dto.getRemark());
        return Result.success(orderNo);
    }

    @PostMapping("/pay")
    public Result<Void> pay(@RequestParam String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        orderService.payOrder(userId, orderNo);
        return Result.success();
    }

    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestParam String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        orderService.cancelOrder(userId, orderNo);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<OrderVO>> list(@RequestParam(required = false) Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.listOrders(userId, status));
    }

    @GetMapping("/detail")
    public Result<OrderVO> detail(@RequestParam String orderNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(orderService.getOrderDetail(userId, orderNo));
    }
}
