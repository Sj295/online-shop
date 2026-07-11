package com.shop.common.bloom;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BloomFilterService {

    private static final String PRODUCT_BLOOM = "shop:bloom:product";
    private static final long EXPECTED_INSERTIONS = 100_000L;
    private static final double FALSE_PROBABILITY = 0.01;

    private final RBloomFilter<Long> productBloomFilter;

    public BloomFilterService(RedissonClient redissonClient) {
        this.productBloomFilter = redissonClient.getBloomFilter(PRODUCT_BLOOM);
        // 仅当不存在时初始化；已存在则复用
        if (!this.productBloomFilter.isExists()) {
            this.productBloomFilter.tryInit(EXPECTED_INSERTIONS, FALSE_PROBABILITY);
        }
    }

    public boolean mightContain(Long productId) {
        return productBloomFilter.contains(productId);
    }

    public void addProduct(Long productId) {
        productBloomFilter.add(productId);
    }

    public void addProducts(Iterable<Long> productIds) {
        for (Long id : productIds) {
            productBloomFilter.add(id);
        }
    }
}
