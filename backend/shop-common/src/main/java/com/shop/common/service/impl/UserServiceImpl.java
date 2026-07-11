package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.User;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.UserMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.UserService;
import com.shop.common.util.PasswordUtil;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        User user = lambdaQuery().eq(User::getUsername, username).one();
        if (user == null || !PasswordUtil.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "账号已被禁用");
        }
        return user;
    }

    @Override
    public User register(String username, String password, String nickname) {
        if (lambdaQuery().eq(User::getUsername, username).count() > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setNickname(nickname);
        user.setStatus(1);
        user.setRole(0);
        save(user);
        return user;
    }

    @Override
    public User getCurrentUser() {
        // 由 Sa-Token 在 Controller 层获取
        return null;
    }
}
