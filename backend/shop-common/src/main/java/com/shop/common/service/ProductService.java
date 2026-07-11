package com.shop.common.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Product;

import java.util.List;

public interface ProductService extends IService<Product> {

    Product getDetail(Long id);

    IPage<Product> pageByCategory(Long categoryId, Integer page, Integer size);

    IPage<Product> search(String keyword, Integer page, Integer size);

    List<Product> listHot(Integer limit);

    List<Product> listNew(Integer limit);

    List<Product> listByCategory(Long categoryId);

    void increaseStock(Long productId, Integer quantity);

    void decreaseStock(Long productId, Integer quantity);
}
