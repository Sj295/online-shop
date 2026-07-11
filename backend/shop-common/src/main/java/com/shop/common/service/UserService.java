package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.User;

public interface UserService extends IService<User> {

    User login(String username, String password);

    User register(String username, String password, String nickname);

    User getCurrentUser();
}
