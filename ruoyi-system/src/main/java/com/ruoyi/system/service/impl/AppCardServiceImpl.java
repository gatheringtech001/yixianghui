package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppCardMapper;
import com.ruoyi.system.domain.AppCard;
import com.ruoyi.system.service.IAppCardService;

/**
 * 会员卡Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppCardServiceImpl implements IAppCardService 
{
    @Autowired
    private AppCardMapper appCardMapper;

    /**
     * 查询会员卡
     * 
     * @param cardId 会员卡主键
     * @return 会员卡
     */
    @Override
    public AppCard selectAppCardByCardId(Long cardId)
    {
        return appCardMapper.selectAppCardByCardId(cardId);
    }

    /**
     * 查询会员卡列表
     * 
     * @param appCard 会员卡
     * @return 会员卡
     */
    @Override
    public List<AppCard> selectAppCardList(AppCard appCard)
    {
        return appCardMapper.selectAppCardList(appCard);
    }

    /**
     * 新增会员卡
     * 
     * @param appCard 会员卡
     * @return 结果
     */
    @Override
    public int insertAppCard(AppCard appCard)
    {
        appCard.setCreateTime(DateUtils.getNowDate());
        return appCardMapper.insertAppCard(appCard);
    }

    /**
     * 修改会员卡
     * 
     * @param appCard 会员卡
     * @return 结果
     */
    @Override
    public int updateAppCard(AppCard appCard)
    {
        appCard.setUpdateTime(DateUtils.getNowDate());
        return appCardMapper.updateAppCard(appCard);
    }

    /**
     * 批量删除会员卡
     * 
     * @param cardIds 需要删除的会员卡主键
     * @return 结果
     */
    @Override
    public int deleteAppCardByCardIds(Long[] cardIds)
    {
        return appCardMapper.deleteAppCardByCardIds(cardIds);
    }

    /**
     * 删除会员卡信息
     * 
     * @param cardId 会员卡主键
     * @return 结果
     */
    @Override
    public int deleteAppCardByCardId(Long cardId)
    {
        return appCardMapper.deleteAppCardByCardId(cardId);
    }
}
