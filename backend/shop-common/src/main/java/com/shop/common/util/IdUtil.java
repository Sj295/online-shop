package com.shop.common.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IdUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public static String generateOrderNo() {
        return LocalDateTime.now().format(FORMATTER) + IdWorker.getId();
    }
}
