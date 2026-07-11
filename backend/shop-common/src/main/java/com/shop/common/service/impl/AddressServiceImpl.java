package com.shop.common.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shop.common.entity.Address;
import com.shop.common.exception.BusinessException;
import com.shop.common.mapper.AddressMapper;
import com.shop.common.result.ResultCode;
import com.shop.common.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Override
    public List<Address> listByUserId(Long userId) {
        return lambdaQuery().eq(Address::getUserId, userId).orderByDesc(Address::getIsDefault).list();
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        return lambdaQuery().eq(Address::getUserId, userId).eq(Address::getIsDefault, 1).one();
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long addressId) {
        Address address = getById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR.getCode(), "地址不存在");
        }
        lambdaUpdate().eq(Address::getUserId, userId).set(Address::getIsDefault, 0).update();
        lambdaUpdate().eq(Address::getId, addressId).set(Address::getIsDefault, 1).update();
    }
}
