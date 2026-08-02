package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.List;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysAuthUser;
import com.ruoyi.system.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserInfoMapper;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.service.IAppUserInfoService;

/**
 * 用户信息Service业务层处理
 * 
 * @author lankong
 * @date 2025-06-03
 */
@Service
public class AppUserInfoServiceImpl implements IAppUserInfoService 
{
    @Autowired
    private AppUserInfoMapper appUserInfoMapper;
    @Autowired
    private SysUserMapper userMapper;

    /**
     * 查询用户信息
     * 
     * @param userId 用户信息主键
     * @return 用户信息
     */
    @Override
    public AppUserInfo selectAppUserInfoByUserId(Long userId)
    {
        AppUserInfo userInfo = appUserInfoMapper.selectAppUserInfoByUserId(userId);
        if (userInfo == null) {
            userInfo = new AppUserInfo();
            userInfo.setUserId(userId);
            userInfo.setGolden(0L);
            userInfo.setMoney(new BigDecimal(0));
            userInfo.setScore(0L);
            userInfo.setFansCount(0L);
            List<SysAuthUser> authUserList = userMapper.selectAuthUserListByUserId(userId);
            if (authUserList.size() > 0) {
                userInfo.setWeixinOpenid(authUserList.get(0).getUuid().replace("wechat_mnp", ""));
            }
            appUserInfoMapper.insertAppUserInfo(userInfo);
        }
        if (StringUtils.isEmpty(userInfo.getWeixinOpenid())) {
            List<SysAuthUser> authUserList = userMapper.selectAuthUserListByUserId(userId);
            if (authUserList.size() > 0) {
                userInfo.setWeixinOpenid(authUserList.get(0).getUuid().replace("wechat_mnp", ""));
                appUserInfoMapper.updateAppUserInfo(userInfo);
            }
        }
        return userInfo;
    }

    /**
     * 查询用户信息列表
     * 
     * @param appUserInfo 用户信息
     * @return 用户信息
     */
    @Override
    public List<AppUserInfo> selectAppUserInfoList(AppUserInfo appUserInfo)
    {
        return appUserInfoMapper.selectAppUserInfoList(appUserInfo);
    }

    /**
     * 新增用户信息
     * 
     * @param appUserInfo 用户信息
     * @return 结果
     */
    @Override
    public int insertAppUserInfo(AppUserInfo appUserInfo)
    {
        return appUserInfoMapper.insertAppUserInfo(appUserInfo);
    }

    /**
     * 修改用户信息
     * 
     * @param appUserInfo 用户信息
     * @return 结果
     */
    @Override
    public int updateAppUserInfo(AppUserInfo appUserInfo)
    {
        return appUserInfoMapper.updateAppUserInfo(appUserInfo);
    }

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户信息主键
     * @return 结果
     */
    @Override
    public int deleteAppUserInfoByUserIds(Long[] userIds)
    {
        return appUserInfoMapper.deleteAppUserInfoByUserIds(userIds);
    }

    /**
     * 删除用户信息信息
     * 
     * @param userId 用户信息主键
     * @return 结果
     */
    @Override
    public int deleteAppUserInfoByUserId(Long userId)
    {
        return appUserInfoMapper.deleteAppUserInfoByUserId(userId);
    }

    /**
     * 初始化用户信息
     * @param userId 用户信息主键
     * @return
     */
    @Override
    public AppUserInfo initUserInfo(Long userId) {
        AppUserInfo userInfo = new AppUserInfo();
        userInfo.setUserId(userId);
        appUserInfoMapper.insertAppUserInfo(userInfo);
        return userInfo;
    }
}
