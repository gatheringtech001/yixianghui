package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AppUserInviter;
import com.ruoyi.system.domain.vo.ConsultantInviteVo;

/**
 * 邀请记录Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppUserInviterMapper 
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
     * 删除邀请记录
     * 
     * @param inviterId 邀请记录主键
     * @return 结果
     */
    public int deleteAppUserInviterByInviterId(Long inviterId);

    /**
     * 批量删除邀请记录
     * 
     * @param inviterIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserInviterByInviterIds(Long[] inviterIds);

    Long countByInviterUserId(@Param("userId") Long userId);

    List<ConsultantInviteVo> selectInviteUserList(@Param("userId") Long userId);
}
