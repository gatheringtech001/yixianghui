package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsSku;

/**
 * 商品属性Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsSkuService 
{
    /**
     * 查询商品属性
     * 
     * @param skuId 商品属性主键
     * @return 商品属性
     */
    public AppGoodsSku selectAppGoodsSkuBySkuId(Long skuId);

    /**
     * 查询商品属性列表
     * 
     * @param appGoodsSku 商品属性
     * @return 商品属性集合
     */
    public List<AppGoodsSku> selectAppGoodsSkuList(AppGoodsSku appGoodsSku);

    /**
     * 新增商品属性
     * 
     * @param appGoodsSku 商品属性
     * @return 结果
     */
    public int insertAppGoodsSku(AppGoodsSku appGoodsSku);

    /**
     * 修改商品属性
     * 
     * @param appGoodsSku 商品属性
     * @return 结果
     */
    public int updateAppGoodsSku(AppGoodsSku appGoodsSku);

    /**
     * 批量删除商品属性
     * 
     * @param skuIds 需要删除的商品属性主键集合
     * @return 结果
     */
    public int deleteAppGoodsSkuBySkuIds(Long[] skuIds);

    /**
     * 删除商品属性信息
     * 
     * @param skuId 商品属性主键
     * @return 结果
     */
    public int deleteAppGoodsSkuBySkuId(Long skuId);

    List<AppGoodsSku> selectAppGoodsSkuListByGoodsId(Long goodsId);
}
