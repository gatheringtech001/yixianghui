package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.AppUserAddressMapper;
import com.ruoyi.system.domain.AppUserAddress;
import com.ruoyi.system.service.IAppUserAddressService;

/**
 * 用户地址Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserAddressServiceImpl implements IAppUserAddressService 
{
    @Autowired
    private AppUserAddressMapper appUserAddressMapper;

    /**
     * 查询用户地址
     * 
     * @param addressId 用户地址主键
     * @return 用户地址
     */
    @Override
    public AppUserAddress selectAppUserAddressByAddressId(Long addressId)
    {
        return appUserAddressMapper.selectAppUserAddressByAddressId(addressId);
    }

    /**
     * 查询用户地址列表
     * 
     * @param appUserAddress 用户地址
     * @return 用户地址
     */
    @Override
    public List<AppUserAddress> selectAppUserAddressList(AppUserAddress appUserAddress)
    {
        return appUserAddressMapper.selectAppUserAddressList(appUserAddress);
    }

    /**
     * 新增用户地址
     * 
     * @param appUserAddress 用户地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAppUserAddress(AppUserAddress appUserAddress)
    {
        if (isDefaultAddress(appUserAddress.getIsDefault())) {
            appUserAddressMapper.clearDefaultByUserId(appUserAddress.getUserId(), null);
        }
        return appUserAddressMapper.insertAppUserAddress(appUserAddress);
    }

    /**
     * 修改用户地址
     * 
     * @param appUserAddress 用户地址
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAppUserAddress(AppUserAddress appUserAddress)
    {
        if (appUserAddress.getUserId() == null && appUserAddress.getAddressId() != null) {
            AppUserAddress existing = appUserAddressMapper.selectAppUserAddressByAddressId(appUserAddress.getAddressId());
            if (existing != null) {
                appUserAddress.setUserId(existing.getUserId());
            }
        }
        if (isDefaultAddress(appUserAddress.getIsDefault())) {
            appUserAddressMapper.clearDefaultByUserId(appUserAddress.getUserId(), appUserAddress.getAddressId());
        }
        return appUserAddressMapper.updateAppUserAddress(appUserAddress);
    }

    private boolean isDefaultAddress(Integer isDefault)
    {
        return isDefault != null && isDefault.intValue() == 1;
    }

    /**
     * 批量删除用户地址
     * 
     * @param addressIds 需要删除的用户地址主键
     * @return 结果
     */
    @Override
    public int deleteAppUserAddressByAddressIds(Long[] addressIds)
    {
        return appUserAddressMapper.deleteAppUserAddressByAddressIds(addressIds);
    }

    /**
     * 删除用户地址信息
     * 
     * @param addressId 用户地址主键
     * @return 结果
     */
    @Override
    public int deleteAppUserAddressByAddressId(Long addressId)
    {
        return appUserAddressMapper.deleteAppUserAddressByAddressId(addressId);
    }
}
