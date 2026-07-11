package com.shop.portal.controller;

import com.shop.common.entity.Category;
import com.shop.common.result.Result;
import com.shop.common.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
