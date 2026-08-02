package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsCategory;

/**
 * 商品分类Service接口
 * 
 * @author lankong
 * @date 2025-03-31
 */
public interface IAppGoodsCategoryService 
{
    /**
     * 查询商品分类
     * 
     * @param categoryId 商品分类主键
     * @return 商品分类
     */
    public AppGoodsCategory selectAppGoodsCategoryByCategoryId(Long categoryId);

    /**
     * 查询商品分类列表
     * 
     * @param appGoodsCategory 商品分类
     * @return 商品分类集合
     */
    public List<AppGoodsCategory> selectAppGoodsCategoryList(AppGoodsCategory appGoodsCategory);

    /**
     * 新增商品分类
     * 
     * @param appGoodsCategory 商品分类
     * @return 结果
     */
    public int insertAppGoodsCategory(AppGoodsCategory appGoodsCategory);

    /**
     * 修改商品分类
     * 
     * @param appGoodsCategory 商品分类
     * @return 结果
     */
    public int updateAppGoodsCategory(AppGoodsCategory appGoodsCategory);

    /**
     * 批量删除商品分类
     * 
     * @param categoryIds 需要删除的商品分类主键集合
     * @return 结果
     */
    public int deleteAppGoodsCategoryByCategoryIds(Long[] categoryIds);

    /**
     * 删除商品分类信息
     * 
     * @param categoryId 商品分类主键
     * @return 结果
     */
    public int deleteAppGoodsCategoryByCategoryId(Long categoryId);

    /**
     * 获取所有分类ID（包含子分类）
     * @param categoryId
     * @return
     */
    String selectAppGoodsCategoryAllIdsById(Long categoryId);

    /**
     * 获取缓存分类
     * @param categoryId
     * @return
     */
    public AppGoodsCategory getCacheAppGoodsCategoryById(Long categoryId);
}
