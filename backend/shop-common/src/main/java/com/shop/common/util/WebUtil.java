package com.shop.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.result.Result;
import com.shop.common.result.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
public class WebUtil {

    private WebUtil() {
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void writeError(HttpServletResponse response, ResultCode resultCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setStatus(200);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(OBJECT_MAPPER.writeValueAsString(Result.error(resultCode)));
            writer.flush();
        }
    }
}
