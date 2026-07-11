package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.CartItem;
import com.shop.common.entity.Product;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.CartItemMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.CartItemService;
import com.shop.common.service.ProductService;
import com.shop.common.vo.CartItemVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartItemServiceImpl extends ServiceImpl<CartItemMapper, CartItem> implements CartItemService {

    private final ProductService productService;

    public CartItemServiceImpl(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public List<CartItemVO> listCart(Long userId) {
        List<CartItem> items = lambdaQuery().eq(CartItem::getUserId, userId).orderByDesc(CartItem::getCreateTime).list();
        return items.stream().map(item -> {
            Product product = productService.getById(item.getProductId());
            CartItemVO vo = new CartItemVO();
            BeanUtils.copyProperties(item, vo);
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getMainImage());
                vo.setPrice(product.getPrice());
                vo.setSubtitle(product.getSubtitle());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Long skuId, Integer quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Product product = productService.getDetail(productId);
        if (product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        CartItem exist = lambdaQuery().eq(CartItem::getUserId, userId)
                .eq(CartItem::getProductId, productId)
                .eq(CartItem::getSkuId, skuId).one();
        if (exist != null) {
            int newQuantity = exist.getQuantity() + quantity;
            if (product.getStock() < newQuantity) {
                throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
            }
            lambdaUpdate().eq(CartItem::getId, exist.getId())
                    .set(CartItem::getQuantity, newQuantity).update();
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setSkuId(skuId);
            item.setQuantity(quantity);
            item.setSelected(1);
            save(item);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        if (quantity <= 0) {
            deleteItem(userId, cartItemId);
            return;
        }
        CartItem item = getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "购物车项不存在");
        }
        Product product = productService.getById(item.getProductId());
        if (product != null && product.getStock() < quantity) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
        lambdaUpdate().eq(CartItem::getId, cartItemId).set(CartItem::getQuantity, quantity).update();
    }

    @Override
    public void deleteItem(Long userId, Long cartItemId) {
        CartItem item = getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "购物车项不存在");
        }
        removeById(cartItemId);
    }

    @Override
    public void selectItem(Long userId, Long cartItemId, Integer selected) {
        CartItem item = getById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "购物车项不存在");
        }
        lambdaUpdate().eq(CartItem::getId, cartItemId).set(CartItem::getSelected, selected).update();
    }

    @Override
    public void clearSelected(Long userId) {
        lambdaUpdate().eq(CartItem::getUserId, userId).eq(CartItem::getSelected, 1).remove();
    }
}
