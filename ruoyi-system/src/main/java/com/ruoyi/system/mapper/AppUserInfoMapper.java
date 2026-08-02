package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppUserInfo;

/**
 * 用户信息Mapper接口
 * 
 * @author lankong
 * @date 2025-06-03
 */
public interface AppUserInfoMapper 
{
    /**
     * 查询用户信息
     * 
     * @param userId 用户信息主键
     * @return 用户信息
     */
    public AppUserInfo selectAppUserInfoByUserId(Long userId);

    /**
     * 查询用户信息列表
     * 
     * @param appUserInfo 用户信息
     * @return 用户信息集合
     */
    public List<AppUserInfo> selectAppUserInfoList(AppUserInfo appUserInfo);

    /**
     * 新增用户信息
     * 
     * @param appUserInfo 用户信息
     * @return 结果
     */
    public int insertAppUserInfo(AppUserInfo appUserInfo);

    /**
     * 修改用户信息
     * 
     * @param appUserInfo 用户信息
     * @return 结果
     */
    public int updateAppUserInfo(AppUserInfo appUserInfo);

    /**
     * 删除用户信息
     * 
     * @param userId 用户信息主键
     * @return 结果
     */
    public int deleteAppUserInfoByUserId(Long userId);

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserInfoByUserIds(Long[] userIds);
}
