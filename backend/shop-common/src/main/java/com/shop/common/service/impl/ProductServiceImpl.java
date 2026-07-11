package com.shop.common.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.bloom.BloomFilterService;
import com.shop.common.cache.CacheNames;
import com.shop.common.entity.Product;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.ProductMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.ProductService;
import com.shop.common.util.RedisKeyUtil;
import com.shop.common.util.RedissonLockUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final BloomFilterService bloomFilterService;
    private final CacheManager cacheManager;
    private final RedissonLockUtil lockUtil;

    public ProductServiceImpl(BloomFilterService bloomFilterService,
                              CacheManager cacheManager,
                              RedissonLockUtil lockUtil) {
        this.bloomFilterService = bloomFilterService;
        this.cacheManager = cacheManager;
        this.lockUtil = lockUtil;
    }

    @Override
    @CircuitBreaker(name = "productDetail", fallbackMethod = "getDetailFallback")
    public Product getDetail(Long id) {
        if (!bloomFilterService.mightContain(id)) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }

        Cache cache = cacheManager.getCache(CacheNames.PRODUCT);
        Product cached = cache != null ? cache.get(id, Product.class) : null;
        if (cached != null) {
            return cached;
        }

        String lockKey = RedisKeyUtil.productCacheLockKey(id);
        boolean locked = lockUtil.tryLock(lockKey, 200, 5, TimeUnit.MILLISECONDS);
        try {
            cached = cache != null ? cache.get(id, Product.class) : null;
            if (cached != null) {
                return cached;
            }
            Product product = lambdaQuery().eq(Product::getId, id).eq(Product::getStatus, 1).one();
            if (product == null) {
                throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
            }
            if (cache != null) {
                cache.put(id, product);
            }
            return product;
        } finally {
            if (locked) {
                lockUtil.unlock(lockKey);
            }
        }
    }

    public Product getDetailFallback(Long id, Throwable t) {
        throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
    }

    @Override
    public IPage<Product> pageByCategory(Long categoryId, Integer page, Integer size) {
        return lambdaQuery()
                .eq(categoryId != null && categoryId > 0, Product::getCategoryId, categoryId)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getIsHot)
                .orderByDesc(Product::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    public IPage<Product> search(String keyword, Integer page, Integer size) {
        return lambdaQuery()
                .like(keyword != null && !keyword.isEmpty(), Product::getName, keyword)
                .or()
                .like(keyword != null && !keyword.isEmpty(), Product::getSubtitle, keyword)
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    public List<Product> listHot(Integer limit) {
        return lambdaQuery().eq(Product::getStatus, 1).eq(Product::getIsHot, 1)
                .orderByDesc(Product::getSaleCount).last("LIMIT " + limit).list();
    }

    @Override
    public List<Product> listNew(Integer limit) {
        return lambdaQuery().eq(Product::getStatus, 1).eq(Product::getIsNew, 1)
                .orderByDesc(Product::getCreateTime).last("LIMIT " + limit).list();
    }

    @Override
    public List<Product> listByCategory(Long categoryId) {
        return lambdaQuery().eq(Product::getCategoryId, categoryId).eq(Product::getStatus, 1)
                .orderByDesc(Product::getSort).list();
    }

    @Override
    public void increaseStock(Long productId, Integer quantity) {
        lambdaUpdate().eq(Product::getId, productId).setSql("stock = stock + " + quantity).update();
    }

    @Override
    public void decreaseStock(Long productId, Integer quantity) {
        boolean success = lambdaUpdate().eq(Product::getId, productId).ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity + ", sale_count = sale_count + " + quantity).update();
        if (!success) {
            throw new BusinessException(ResultCode.STOCK_NOT_ENOUGH);
        }
    }
}
