package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listEnabled();
}
