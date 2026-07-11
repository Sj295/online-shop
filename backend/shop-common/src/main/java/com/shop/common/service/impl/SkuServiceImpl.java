package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.Sku;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.SkuMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.SkuService;
import org.springframework.stereotype.Service;

@Service
public class SkuServiceImpl extends ServiceImpl<SkuMapper, Sku> implements SkuService {

    @Override
    public Sku getDefaultByProductId(Long productId) {
        return lambdaQuery().eq(Sku::getProductId, productId).eq(Sku::getStatus, 1).one();
    }

    @Override
    public void decreaseStock(Long skuId, Integer quantity) {
        boolean success = lambdaUpdate().eq(Sku::getId, skuId).ge(Sku::getStock, quantity)
                .setSql("stock = stock - " + quantity + ", sale_count = sale_count + " + quantity).update();
        if (!success) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
    }
}
