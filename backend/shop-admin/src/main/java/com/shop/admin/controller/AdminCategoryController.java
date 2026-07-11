package com.shop.admin.controller;

import com.shop.common.dto.CategoryDTO;
import com.shop.common.entity.Category;
import com.shop.common.result.Result;
import com.shop.common.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.list());
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CategoryDTO dto) {
        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        category.setStatus(1);
        categoryService.save(category);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        BeanUtils.copyProperties(dto, category);
        category.setId(id);
        categoryService.updateById(category);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return Result.success();
    }
}
