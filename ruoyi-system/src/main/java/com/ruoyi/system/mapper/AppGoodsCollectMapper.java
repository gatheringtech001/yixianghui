package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsCollect;

/**
 * 商品收藏Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsCollectMapper 
{
    /**
     * 查询商品收藏
     * 
     * @param collectId 商品收藏主键
     * @return 商品收藏
     */
    public AppGoodsCollect selectAppGoodsCollectByCollectId(Long collectId);

    /**
     * 查询商品收藏列表
     * 
     * @param appGoodsCollect 商品收藏
     * @return 商品收藏集合
     */
    public List<AppGoodsCollect> selectAppGoodsCollectList(AppGoodsCollect appGoodsCollect);

    /**
     * 新增商品收藏
     * 
     * @param appGoodsCollect 商品收藏
     * @return 结果
     */
    public int insertAppGoodsCollect(AppGoodsCollect appGoodsCollect);

    /**
     * 修改商品收藏
     * 
     * @param appGoodsCollect 商品收藏
     * @return 结果
     */
    public int updateAppGoodsCollect(AppGoodsCollect appGoodsCollect);

    /**
     * 删除商品收藏
     * 
     * @param collectId 商品收藏主键
     * @return 结果
     */
    public int deleteAppGoodsCollectByCollectId(Long collectId);

    /**
     * 批量删除商品收藏
     * 
     * @param collectIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsCollectByCollectIds(Long[] collectIds);
}
