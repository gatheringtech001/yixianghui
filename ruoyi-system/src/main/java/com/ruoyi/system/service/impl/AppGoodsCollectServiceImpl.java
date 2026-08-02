package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCollectMapper;
import com.ruoyi.system.domain.AppGoodsCollect;
import com.ruoyi.system.service.IAppGoodsCollectService;

/**
 * 商品收藏Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCollectServiceImpl implements IAppGoodsCollectService 
{
    @Autowired
    private AppGoodsCollectMapper appGoodsCollectMapper;

    /**
     * 查询商品收藏
     * 
     * @param collectId 商品收藏主键
     * @return 商品收藏
     */
    @Override
    public AppGoodsCollect selectAppGoodsCollectByCollectId(Long collectId)
    {
        return appGoodsCollectMapper.selectAppGoodsCollectByCollectId(collectId);
    }

    /**
     * 查询商品收藏列表
     * 
     * @param appGoodsCollect 商品收藏
     * @return 商品收藏
     */
    @Override
    public List<AppGoodsCollect> selectAppGoodsCollectList(AppGoodsCollect appGoodsCollect)
    {
        return appGoodsCollectMapper.selectAppGoodsCollectList(appGoodsCollect);
    }

    /**
     * 新增商品收藏
     * 
     * @param appGoodsCollect 商品收藏
     * @return 结果
     */
    @Override
    public int insertAppGoodsCollect(AppGoodsCollect appGoodsCollect)
    {
        appGoodsCollect.setCreateTime(DateUtils.getNowDate());
        return appGoodsCollectMapper.insertAppGoodsCollect(appGoodsCollect);
    }

    /**
     * 修改商品收藏
     * 
     * @param appGoodsCollect 商品收藏
     * @return 结果
     */
    @Override
    public int updateAppGoodsCollect(AppGoodsCollect appGoodsCollect)
    {
        appGoodsCollect.setUpdateTime(DateUtils.getNowDate());
        return appGoodsCollectMapper.updateAppGoodsCollect(appGoodsCollect);
    }

    /**
     * 批量删除商品收藏
     * 
     * @param collectIds 需要删除的商品收藏主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCollectByCollectIds(Long[] collectIds)
    {
        return appGoodsCollectMapper.deleteAppGoodsCollectByCollectIds(collectIds);
    }

    /**
     * 删除商品收藏信息
     * 
     * @param collectId 商品收藏主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCollectByCollectId(Long collectId)
    {
        return appGoodsCollectMapper.deleteAppGoodsCollectByCollectId(collectId);
    }
}
