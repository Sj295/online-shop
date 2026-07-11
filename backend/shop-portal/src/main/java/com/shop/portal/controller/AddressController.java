package com.shop.portal.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.shop.common.dto.AddressDTO;
import com.shop.common.entity.Address;
import com.shop.common.result.Result;
import com.shop.common.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/list")
    public Result<List<Address>> list() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(addressService.listByUserId(userId));
    }

    @GetMapping("/default")
    public Result<Address> getDefault() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(addressService.getDefaultAddress(userId));
    }

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Address address = new Address();
        BeanUtils.copyProperties(dto, address);
        address.setUserId(userId);
        addressService.save(address);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Address address = addressService.getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            return Result.error("地址不存在");
        }
        BeanUtils.copyProperties(dto, address);
        address.setId(id);
        address.setUserId(userId);
        addressService.updateById(address);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Address address = addressService.getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            return Result.error("地址不存在");
        }
        addressService.removeById(id);
        return Result.success();
    }

    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        addressService.setDefault(userId, id);
        return Result.success();
    }
}
