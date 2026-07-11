package com.shop.portal.controller;

import com.shop.common.entity.Carousel;
import com.shop.common.entity.Category;
import com.shop.common.entity.Product;
import com.shop.common.result.Result;
import com.shop.common.service.CarouselService;
import com.shop.common.service.CategoryService;
import com.shop.common.service.ProductService;
import com.shop.common.util.RedisKeyUtil;
import com.shop.common.util.RedisUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CarouselService carouselService;
    private final RedisUtil redisUtil;

    public HomeController(ProductService productService, CategoryService categoryService,
                          CarouselService carouselService, RedisUtil redisUtil) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.carouselService = carouselService;
        this.redisUtil = redisUtil;
    }

    @GetMapping("/index")
    public Result<HomeData> index() {
        List<Carousel> carousels = carouselService.listEnabled();
        List<Category> categories = categoryService.listEnabled();
        List<Product> hotProducts = productService.listHot(6);
        List<Product> newProducts = productService.listNew(6);

        HomeData data = new HomeData();
        data.setCarousels(carousels);
        data.setCategories(categories);
        data.setHotProducts(hotProducts);
        data.setNewProducts(newProducts);
        return Result.success(data);
    }

    public static class HomeData {
        private List<Carousel> carousels;
        private List<Category> categories;
        private List<Product> hotProducts;
        private List<Product> newProducts;

        public List<Carousel> getCarousels() { return carousels; }
        public void setCarousels(List<Carousel> carousels) { this.carousels = carousels; }
        public List<Category> getCategories() { return categories; }
        public void setCategories(List<Category> categories) { this.categories = categories; }
        public List<Product> getHotProducts() { return hotProducts; }
        public void setHotProducts(List<Product> hotProducts) { this.hotProducts = hotProducts; }
        public List<Product> getNewProducts() { return newProducts; }
        public void setNewProducts(List<Product> newProducts) { this.newProducts = newProducts; }
    }
}
