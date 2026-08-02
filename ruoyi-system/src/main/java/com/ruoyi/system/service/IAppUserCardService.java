package com.ruoyi.system.service;

import java.util.List;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppUserCard;

/**
 * 用户会员卡Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserCardService 
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
     * 批量删除用户会员卡
     * 
     * @param recordIds 需要删除的用户会员卡主键集合
     * @return 结果
     */
    public int deleteAppUserCardByRecordIds(Long[] recordIds);

    /**
     * 删除用户会员卡信息
     * 
     * @param recordId 用户会员卡主键
     * @return 结果
     */
    public int deleteAppUserCardByRecordId(Long recordId);

    /**
     * 根据用户ID获取会员卡信息
     * @param userId
     * @return
     */
    AppUserCard selectAppUserCardByUserId(Long userId);

    /**
     * 用户会员卡支付
     * @param userCard
     * @return
     */
    AjaxResult wxpayPrepay(AppUserCard userCard);

    /**
     * 关闭超时未支付的会员卡开通单（待激活 status=0）
     * @param expireMinutes 超时分钟数
     * @return 关闭数量
     */
    int closeExpiredUnpaidCards(int expireMinutes);

    /**
     * 取消未支付会员卡开通单
     */
    int cancelUnpaidUserCard(Long recordId, Long userId);

    /**
     * 已激活会员卡申请退款并撤销权益（发起微信退款）
     */
    AjaxResult refundUserCard(Long recordId, Long userId);

    /**
     * 会员卡退款成功回调
     */
    void handleRefundSuccess(Long recordId);
}
