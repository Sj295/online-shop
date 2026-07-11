package com.shop.portal.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.dto.UserLoginDTO;
import com.shop.common.dto.UserRegisterDTO;
import com.shop.common.entity.User;
import com.shop.common.result.Result;
import com.shop.common.service.UserService;
import com.shop.common.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenValue());
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody UserRegisterDTO dto) {
        User user = userService.register(dto.getUsername(), dto.getPassword(), dto.getNickname());
        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }

    @GetMapping("/info")
    public Result<UserVO> info() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getById(userId);
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }
}
