package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppUserAddress;

/**
 * 用户地址Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserAddressService 
{
    /**
     * 查询用户地址
     * 
     * @param addressId 用户地址主键
     * @return 用户地址
     */
    public AppUserAddress selectAppUserAddressByAddressId(Long addressId);

    /**
     * 查询用户地址列表
     * 
     * @param appUserAddress 用户地址
     * @return 用户地址集合
     */
    public List<AppUserAddress> selectAppUserAddressList(AppUserAddress appUserAddress);

    /**
     * 新增用户地址
     * 
     * @param appUserAddress 用户地址
     * @return 结果
     */
    public int insertAppUserAddress(AppUserAddress appUserAddress);

    /**
     * 修改用户地址
     * 
     * @param appUserAddress 用户地址
     * @return 结果
     */
    public int updateAppUserAddress(AppUserAddress appUserAddress);

    /**
     * 批量删除用户地址
     * 
     * @param addressIds 需要删除的用户地址主键集合
     * @return 结果
     */
    public int deleteAppUserAddressByAddressIds(Long[] addressIds);

    /**
     * 删除用户地址信息
     * 
     * @param addressId 用户地址主键
     * @return 结果
     */
    public int deleteAppUserAddressByAddressId(Long addressId);
}
