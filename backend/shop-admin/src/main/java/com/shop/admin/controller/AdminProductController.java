package com.shop.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shop.common.dto.ProductDTO;
import com.shop.common.entity.Product;
import com.shop.common.entity.Sku;
import com.shop.common.result.Result;
import com.shop.common.service.ProductService;
import com.shop.common.service.SkuService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/product")
public class AdminProductController {

    private final ProductService productService;
    private final SkuService skuService;

    public AdminProductController(ProductService productService, SkuService skuService) {
        this.productService = productService;
        this.skuService = skuService;
    }

    @GetMapping("/list")
    public Result<IPage<Product>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(productService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size)));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody ProductDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(1);
        product.setSaleCount(0);
        product.setCreateTime(LocalDateTime.now());
        productService.save(product);

        Sku sku = new Sku();
        sku.setProductId(product.getId());
        sku.setSkuCode("SKU-" + String.format("%05d", product.getId()));
        sku.setPrice(product.getPrice());
        sku.setStock(product.getStock());
        sku.setStatus(1);
        sku.setSkuSpecs("{\"规格\":\"默认\"}");
        skuService.save(sku);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        BeanUtils.copyProperties(dto, product);
        product.setId(id);
        productService.updateById(product);
        return Result.success();
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        product.setStatus(status);
        productService.updateById(product);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.success();
    }
}
