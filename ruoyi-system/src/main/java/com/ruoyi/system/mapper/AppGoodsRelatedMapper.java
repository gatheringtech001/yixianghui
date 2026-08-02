package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.AppGoodsRelated;

/**
 * 商品详情区块Mapper接口
 * 
 * @author lankong
 * @date 2025-11-27
 */
public interface AppGoodsRelatedMapper 
{
    /**
     * 查询商品详情区块
     * 
     * @param id 商品详情区块主键
     * @return 商品详情区块
     */
    public AppGoodsRelated selectAppGoodsRelatedById(Long id);

    /**
     * 查询商品详情区块列表
     * 
     * @param appGoodsRelated 商品详情区块
     * @return 商品详情区块集合
     */
    public List<AppGoodsRelated> selectAppGoodsRelatedList(AppGoodsRelated appGoodsRelated);

    /**
     * 新增商品详情区块
     * 
     * @param appGoodsRelated 商品详情区块
     * @return 结果
     */
    public int insertAppGoodsRelated(AppGoodsRelated appGoodsRelated);

    /**
     * 修改商品详情区块
     * 
     * @param appGoodsRelated 商品详情区块
     * @return 结果
     */
    public int updateAppGoodsRelated(AppGoodsRelated appGoodsRelated);

    /**
     * 删除商品详情区块
     * 
     * @param id 商品详情区块主键
     * @return 结果
     */
    public int deleteAppGoodsRelatedById(Long id);

    /**
     * 批量删除商品详情区块
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsRelatedByIds(Long[] ids);

    public AppGoodsRelated selectProductRelate(Map param);

    public int deleteByProductId(Long productId);
}
