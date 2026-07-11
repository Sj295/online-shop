package com.shop.common.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.Product;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.ProductMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.cache.CacheNames;
import com.shop.common.service.ProductService;
import com.shop.common.util.RedisKeyUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final StringRedisTemplate redisTemplate;

    public ProductServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT, key = "#id")
    @CircuitBreaker(name = "productDetail", fallbackMethod = "getDetailFallback")
    public Product getDetail(Long id) {
        Product product = lambdaQuery().eq(Product::getId, id).eq(Product::getStatus, 1).one();
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
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
    @Cacheable(value = CacheNames.PRODUCT, key = "'hot:' + #limit")
    public List<Product> listHot(Integer limit) {
        String key = RedisKeyUtil.HOT_PRODUCTS;
        // 缓存热门商品ID列表，简化处理
        List<Product> products = lambdaQuery().eq(Product::getStatus, 1).eq(Product::getIsHot, 1)
                .orderByDesc(Product::getSaleCount).last("LIMIT " + limit).list();
        return products;
    }

    @Override
    @Cacheable(value = CacheNames.PRODUCT, key = "'new:' + #limit")
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
