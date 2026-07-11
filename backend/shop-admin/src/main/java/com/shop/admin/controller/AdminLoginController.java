package com.shop.admin.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.dto.UserLoginDTO;
import com.shop.common.entity.User;
import com.shop.common.exception.BusinessException;
import com.shop.common.result.Result;
import com.shop.common.result.ResultCode;
import com.shop.common.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminLoginController {

    private final UserService userService;

    public AdminLoginController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        if (user.getRole() == null || user.getRole() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
