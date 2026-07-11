package com.shop.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pms_carousel")
public class Carousel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String image;
    private String link;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
