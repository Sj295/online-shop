package com.shop.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shop.common.entity.OrderItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderItemMapper extends BaseMapper<OrderItem> {

    int insertBatch(@Param("list") List<OrderItem> items);
}
