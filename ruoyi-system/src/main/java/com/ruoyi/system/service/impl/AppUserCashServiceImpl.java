package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserCashMapper;
import com.ruoyi.system.domain.AppUserCash;
import com.ruoyi.system.service.IAppUserCashService;

/**
 * 用户提现Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserCashServiceImpl implements IAppUserCashService 
{
    @Autowired
    private AppUserCashMapper appUserCashMapper;

    /**
     * 查询用户提现
     * 
     * @param cashId 用户提现主键
     * @return 用户提现
     */
    @Override
    public AppUserCash selectAppUserCashByCashId(Long cashId)
    {
        return appUserCashMapper.selectAppUserCashByCashId(cashId);
    }

    /**
     * 查询用户提现列表
     * 
     * @param appUserCash 用户提现
     * @return 用户提现
     */
    @Override
    public List<AppUserCash> selectAppUserCashList(AppUserCash appUserCash)
    {
        return appUserCashMapper.selectAppUserCashList(appUserCash);
    }

    /**
     * 新增用户提现
     * 
     * @param appUserCash 用户提现
     * @return 结果
     */
    @Override
    public int insertAppUserCash(AppUserCash appUserCash)
    {
        appUserCash.setCreateTime(DateUtils.getNowDate());
        return appUserCashMapper.insertAppUserCash(appUserCash);
    }

    /**
     * 修改用户提现
     * 
     * @param appUserCash 用户提现
     * @return 结果
     */
    @Override
    public int updateAppUserCash(AppUserCash appUserCash)
    {
        return appUserCashMapper.updateAppUserCash(appUserCash);
    }

    /**
     * 批量删除用户提现
     * 
     * @param cashIds 需要删除的用户提现主键
     * @return 结果
     */
    @Override
    public int deleteAppUserCashByCashIds(Long[] cashIds)
    {
        return appUserCashMapper.deleteAppUserCashByCashIds(cashIds);
    }

    /**
     * 删除用户提现信息
     * 
     * @param cashId 用户提现主键
     * @return 结果
     */
    @Override
    public int deleteAppUserCashByCashId(Long cashId)
    {
        return appUserCashMapper.deleteAppUserCashByCashId(cashId);
    }
}
