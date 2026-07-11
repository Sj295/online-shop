package com.shop.common.stock;

import com.shop.common.bloom.BloomFilterService;
import com.shop.common.entity.Product;
import com.shop.common.entity.Sku;
import com.shop.common.service.ProductService;
import com.shop.common.service.SkuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInitializer implements ApplicationRunner {

    private final ProductService productService;
    private final SkuService skuService;
    private final RedisStockService redisStockService;
    private final BloomFilterService bloomFilterService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing Redis stock counters and product bloom filter...");

        List<Product> products = productService.list();
        if (products == null || products.isEmpty()) {
            log.warn("No products found, skip stock initialization");
            return;
        }

        for (Product product : products) {
            redisStockService.initProductStock(product.getId(), product.getStock());
        }
        bloomFilterService.addProducts(products.stream().map(Product::getId).collect(Collectors.toList()));

        List<Sku> skus = skuService.list();
        if (skus != null) {
            for (Sku sku : skus) {
                redisStockService.initSkuStock(sku.getId(), sku.getStock());
            }
        }

        log.info("Initialized {} products and {} skus in Redis", products.size(), skus == null ? 0 : skus.size());
    }
}
