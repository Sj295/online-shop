package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.Category;
import com.shop.common.mapper.CategoryMapper;
import com.shop.common.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> listEnabled() {
        return lambdaQuery().eq(Category::getStatus, 1).orderByAsc(Category::getSort).list();
    }
}
