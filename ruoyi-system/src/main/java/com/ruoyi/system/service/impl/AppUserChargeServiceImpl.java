package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserChargeMapper;
import com.ruoyi.system.domain.AppUserCharge;
import com.ruoyi.system.service.IAppUserChargeService;

/**
 * 用户充值Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserChargeServiceImpl implements IAppUserChargeService 
{
    @Autowired
    private AppUserChargeMapper appUserChargeMapper;

    /**
     * 查询用户充值
     * 
     * @param chargeId 用户充值主键
     * @return 用户充值
     */
    @Override
    public AppUserCharge selectAppUserChargeByChargeId(Long chargeId)
    {
        return appUserChargeMapper.selectAppUserChargeByChargeId(chargeId);
    }

    /**
     * 查询用户充值列表
     * 
     * @param appUserCharge 用户充值
     * @return 用户充值
     */
    @Override
    public List<AppUserCharge> selectAppUserChargeList(AppUserCharge appUserCharge)
    {
        return appUserChargeMapper.selectAppUserChargeList(appUserCharge);
    }

    /**
     * 新增用户充值
     * 
     * @param appUserCharge 用户充值
     * @return 结果
     */
    @Override
    public int insertAppUserCharge(AppUserCharge appUserCharge)
    {
        appUserCharge.setCreateTime(DateUtils.getNowDate());
        return appUserChargeMapper.insertAppUserCharge(appUserCharge);
    }

    /**
     * 修改用户充值
     * 
     * @param appUserCharge 用户充值
     * @return 结果
     */
    @Override
    public int updateAppUserCharge(AppUserCharge appUserCharge)
    {
        return appUserChargeMapper.updateAppUserCharge(appUserCharge);
    }

    /**
     * 批量删除用户充值
     * 
     * @param chargeIds 需要删除的用户充值主键
     * @return 结果
     */
    @Override
    public int deleteAppUserChargeByChargeIds(Long[] chargeIds)
    {
        return appUserChargeMapper.deleteAppUserChargeByChargeIds(chargeIds);
    }

    /**
     * 删除用户充值信息
     * 
     * @param chargeId 用户充值主键
     * @return 结果
     */
    @Override
    public int deleteAppUserChargeByChargeId(Long chargeId)
    {
        return appUserChargeMapper.deleteAppUserChargeByChargeId(chargeId);
    }
}
