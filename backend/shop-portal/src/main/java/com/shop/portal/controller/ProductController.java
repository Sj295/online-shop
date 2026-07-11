package com.shop.portal.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shop.common.entity.Product;
import com.shop.common.result.Result;
import com.shop.common.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }

    @GetMapping("/list")
    public Result<IPage<Product>> list(
            @RequestParam(required = false, defaultValue = "0") Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size) {
        return Result.success(productService.pageByCategory(categoryId, page, size));
    }

    @GetMapping("/search")
    public Result<IPage<Product>> search(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "12") Integer size) {
        return Result.success(productService.search(keyword, page, size));
    }
}
