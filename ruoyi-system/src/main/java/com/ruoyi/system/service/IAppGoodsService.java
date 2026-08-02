package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.vo.AppGoodsVo;

/**
 * 商品Service接口
 * 
 * @author lankong
 * @date 2025-03-31
 */
public interface IAppGoodsService 
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
     * 批量删除商品
     * 
     * @param goodsIds 需要删除的商品主键集合
     * @return 结果
     */
    public int deleteAppGoodsByGoodsIds(Long[] goodsIds);

    /**
     * 删除商品信息
     * 
     * @param goodsId 商品主键
     * @return 结果
     */
    public int deleteAppGoodsByGoodsId(Long goodsId);

    /**
     * 获取缓存商品信息
     * @param goodsId
     * @return
     */
    public AppGoods getCacheAppGoodsById(Long goodsId);
}
