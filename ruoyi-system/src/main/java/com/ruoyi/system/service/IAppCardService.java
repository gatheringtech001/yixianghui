package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppCard;

/**
 * 会员卡Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppCardService 
{
    /**
     * 查询会员卡
     * 
     * @param cardId 会员卡主键
     * @return 会员卡
     */
    public AppCard selectAppCardByCardId(Long cardId);

    /**
     * 查询会员卡列表
     * 
     * @param appCard 会员卡
     * @return 会员卡集合
     */
    public List<AppCard> selectAppCardList(AppCard appCard);

    /**
     * 新增会员卡
     * 
     * @param appCard 会员卡
     * @return 结果
     */
    public int insertAppCard(AppCard appCard);

    /**
     * 修改会员卡
     * 
     * @param appCard 会员卡
     * @return 结果
     */
    public int updateAppCard(AppCard appCard);

    /**
     * 批量删除会员卡
     * 
     * @param cardIds 需要删除的会员卡主键集合
     * @return 结果
     */
    public int deleteAppCardByCardIds(Long[] cardIds);

    /**
     * 删除会员卡信息
     * 
     * @param cardId 会员卡主键
     * @return 结果
     */
    public int deleteAppCardByCardId(Long cardId);
}
