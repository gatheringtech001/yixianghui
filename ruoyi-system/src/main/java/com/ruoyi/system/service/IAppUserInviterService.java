package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppUserInviter;

/**
 * 邀请记录Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserInviterService 
{
    /**
     * 查询邀请记录
     * 
     * @param inviterId 邀请记录主键
     * @return 邀请记录
     */
    public AppUserInviter selectAppUserInviterByInviterId(Long inviterId);

    /**
     * 查询邀请记录列表
     * 
     * @param appUserInviter 邀请记录
     * @return 邀请记录集合
     */
    public List<AppUserInviter> selectAppUserInviterList(AppUserInviter appUserInviter);

    /**
     * 新增邀请记录
     * 
     * @param appUserInviter 邀请记录
     * @return 结果
     */
    public int insertAppUserInviter(AppUserInviter appUserInviter);

    /**
     * 修改邀请记录
     * 
     * @param appUserInviter 邀请记录
     * @return 结果
     */
    public int updateAppUserInviter(AppUserInviter appUserInviter);

    /**
     * 批量删除邀请记录
     * 
     * @param inviterIds 需要删除的邀请记录主键集合
     * @return 结果
     */
    public int deleteAppUserInviterByInviterIds(Long[] inviterIds);

    /**
     * 删除邀请记录信息
     * 
     * @param inviterId 邀请记录主键
     * @return 结果
     */
    public int deleteAppUserInviterByInviterId(Long inviterId);
}
