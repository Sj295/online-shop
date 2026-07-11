package com.shop.common.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shop.common.entity.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    List<Address> listByUserId(Long userId);

    Address getDefaultAddress(Long userId);

    void setDefault(Long userId, Long addressId);
}
