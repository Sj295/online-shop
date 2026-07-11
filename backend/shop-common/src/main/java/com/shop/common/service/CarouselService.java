package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Carousel;

import java.util.List;

public interface CarouselService extends IService<Carousel> {

    List<Carousel> listEnabled();
}
