package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.CartItem;
import com.shop.common.vo.CartItemVO;

import java.util.List;

public interface CartItemService extends IService<CartItem> {

    List<CartItemVO> listCart(Long userId);

    void addToCart(Long userId, Long productId, Long skuId, Integer quantity);

    void updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void deleteItem(Long userId, Long cartItemId);

    void selectItem(Long userId, Long cartItemId, Integer selected);

    void clearSelected(Long userId);
}
