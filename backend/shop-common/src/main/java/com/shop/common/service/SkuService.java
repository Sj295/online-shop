package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Sku;

public interface SkuService extends IService<Sku> {

    Sku getDefaultByProductId(Long productId);

    void decreaseStock(Long skuId, Integer quantity);
}
