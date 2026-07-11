package com.shop.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.entity.User;
import com.shop.common.result.Result;
import com.shop.common.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<IPage<User>> list(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return Result.success(userService.page(new Page<>(page, size)));
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }
}
