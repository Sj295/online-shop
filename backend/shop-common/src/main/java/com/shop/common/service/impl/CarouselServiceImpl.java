package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.Carousel;
import com.shop.common.mapper.CarouselMapper;
import com.shop.common.service.CarouselService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselServiceImpl extends ServiceImpl<CarouselMapper, Carousel> implements CarouselService {

    @Override
    public List<Carousel> listEnabled() {
        return lambdaQuery().eq(Carousel::getStatus, 1).orderByAsc(Carousel::getSort).list();
    }
}
