package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppUserCharge;

/**
 * 用户充值Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserChargeService 
{
    /**
     * 查询用户充值
     * 
     * @param chargeId 用户充值主键
     * @return 用户充值
     */
    public AppUserCharge selectAppUserChargeByChargeId(Long chargeId);

    /**
     * 查询用户充值列表
     * 
     * @param appUserCharge 用户充值
     * @return 用户充值集合
     */
    public List<AppUserCharge> selectAppUserChargeList(AppUserCharge appUserCharge);

    /**
     * 新增用户充值
     * 
     * @param appUserCharge 用户充值
     * @return 结果
     */
    public int insertAppUserCharge(AppUserCharge appUserCharge);

    /**
     * 修改用户充值
     * 
     * @param appUserCharge 用户充值
     * @return 结果
     */
    public int updateAppUserCharge(AppUserCharge appUserCharge);

    /**
     * 批量删除用户充值
     * 
     * @param chargeIds 需要删除的用户充值主键集合
     * @return 结果
     */
    public int deleteAppUserChargeByChargeIds(Long[] chargeIds);

    /**
     * 删除用户充值信息
     * 
     * @param chargeId 用户充值主键
     * @return 结果
     */
    public int deleteAppUserChargeByChargeId(Long chargeId);
}
