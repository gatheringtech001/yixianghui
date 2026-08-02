package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoods;

/**
 * 商品Mapper接口
 * 
 * @author lankong
 * @date 2025-03-31
 */
public interface AppGoodsMapper 
{
    /**
     * 查询商品
     * 
     * @param goodsId 商品主键
     * @return 商品
     */
    public AppGoods selectAppGoodsByGoodsId(Long goodsId);

    /**
     * 查询商品列表
     * 
     * @param appGoods 商品
     * @return 商品集合
     */
    public List<AppGoods> selectAppGoodsList(AppGoods appGoods);

    /**
     * 新增商品
     * 
     * @param appGoods 商品
     * @return 结果
     */
    public int insertAppGoods(AppGoods appGoods);

    /**
     * 修改商品
     * 
     * @param appGoods 商品
     * @return 结果
     */
    public int updateAppGoods(AppGoods appGoods);

    /**
     * 删除商品
     * 
     * @param goodsId 商品主键
     * @return 结果
     */
    public int deleteAppGoodsByGoodsId(Long goodsId);

    /**
     * 批量删除商品
     * 
     * @param goodsIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsByGoodsIds(Long[] goodsIds);

    /**
     * 扣减库存并增加销量（库存充足时）
     */
    public int reserveStock(@org.apache.ibatis.annotations.Param("goodsId") Long goodsId, @org.apache.ibatis.annotations.Param("count") Long count);

    /**
     * 释放预占库存并回退销量
     */
    public int releaseStock(@org.apache.ibatis.annotations.Param("goodsId") Long goodsId, @org.apache.ibatis.annotations.Param("count") Long count);
}
