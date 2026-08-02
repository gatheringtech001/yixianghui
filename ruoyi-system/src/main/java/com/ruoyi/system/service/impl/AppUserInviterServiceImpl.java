package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserInviterMapper;
import com.ruoyi.system.domain.AppUserInviter;
import com.ruoyi.system.service.IAppUserInviterService;

/**
 * 邀请记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserInviterServiceImpl implements IAppUserInviterService 
{
    @Autowired
    private AppUserInviterMapper appUserInviterMapper;

    /**
     * 查询邀请记录
     * 
     * @param inviterId 邀请记录主键
     * @return 邀请记录
     */
    @Override
    public AppUserInviter selectAppUserInviterByInviterId(Long inviterId)
    {
        return appUserInviterMapper.selectAppUserInviterByInviterId(inviterId);
    }

    /**
     * 查询邀请记录列表
     * 
     * @param appUserInviter 邀请记录
     * @return 邀请记录
     */
    @Override
    public List<AppUserInviter> selectAppUserInviterList(AppUserInviter appUserInviter)
    {
        return appUserInviterMapper.selectAppUserInviterList(appUserInviter);
    }

    /**
     * 新增邀请记录
     * 
     * @param appUserInviter 邀请记录
     * @return 结果
     */
    @Override
    public int insertAppUserInviter(AppUserInviter appUserInviter)
    {
        appUserInviter.setCreateTime(DateUtils.getNowDate());
        return appUserInviterMapper.insertAppUserInviter(appUserInviter);
    }

    /**
     * 修改邀请记录
     * 
     * @param appUserInviter 邀请记录
     * @return 结果
     */
    @Override
    public int updateAppUserInviter(AppUserInviter appUserInviter)
    {
        return appUserInviterMapper.updateAppUserInviter(appUserInviter);
    }

    /**
     * 批量删除邀请记录
     * 
     * @param inviterIds 需要删除的邀请记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserInviterByInviterIds(Long[] inviterIds)
    {
        return appUserInviterMapper.deleteAppUserInviterByInviterIds(inviterIds);
    }

    /**
     * 删除邀请记录信息
     * 
     * @param inviterId 邀请记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserInviterByInviterId(Long inviterId)
    {
        return appUserInviterMapper.deleteAppUserInviterByInviterId(inviterId);
    }
}
