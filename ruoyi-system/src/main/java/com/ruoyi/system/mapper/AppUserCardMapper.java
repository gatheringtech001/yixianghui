package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppUserCard;

/**
 * 用户会员卡Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppUserCardMapper 
{
    /**
     * 查询用户会员卡
     * 
     * @param recordId 用户会员卡主键
     * @return 用户会员卡
     */
    public AppUserCard selectAppUserCardByRecordId(Long recordId);

    /**
     * 查询用户会员卡列表
     * 
     * @param appUserCard 用户会员卡
     * @return 用户会员卡集合
     */
    public List<AppUserCard> selectAppUserCardList(AppUserCard appUserCard);

    /**
     * 新增用户会员卡
     * 
     * @param appUserCard 用户会员卡
     * @return 结果
     */
    public int insertAppUserCard(AppUserCard appUserCard);

    /**
     * 修改用户会员卡
     * 
     * @param appUserCard 用户会员卡
     * @return 结果
     */
    public int updateAppUserCard(AppUserCard appUserCard);

    /**
     * 删除用户会员卡
     * 
     * @param recordId 用户会员卡主键
     * @return 结果
     */
    public int deleteAppUserCardByRecordId(Long recordId);

    /**
     * 批量删除用户会员卡
     * 
     * @param recordIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserCardByRecordIds(Long[] recordIds);

    /**
     * 根据用户ID获取会员卡信息
     * @param userId
     * @return
     */
    AppUserCard selectAppUserCardByUserId(Long userId);
}
