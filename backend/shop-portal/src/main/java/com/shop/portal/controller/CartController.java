package com.shop.portal.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.dto.CartAddDTO;
import com.shop.common.dto.CartUpdateDTO;
import com.shop.common.result.Result;
import com.shop.common.service.CartItemService;
import com.shop.common.vo.CartItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartItemService cartItemService;

    public CartController(CartItemService cartItemService) {
        this.cartItemService = cartItemService;
    }

    @GetMapping("/list")
    public Result<List<CartItemVO>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(cartItemService.listCart(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartAddDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long skuId = dto.getSkuId() == null ? 0L : dto.getSkuId();
        Integer quantity = dto.getQuantity() == null ? 1 : dto.getQuantity();
        cartItemService.addToCart(userId, dto.getProductId(), skuId, quantity);
        return Result.success();
    }

    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody CartUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartItemService.updateQuantity(userId, dto.getCartItemId(), dto.getQuantity());
        if (dto.getSelected() != null) {
            cartItemService.selectItem(userId, dto.getCartItemId(), dto.getSelected());
        }
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        cartItemService.deleteItem(userId, id);
        return Result.success();
    }
}
