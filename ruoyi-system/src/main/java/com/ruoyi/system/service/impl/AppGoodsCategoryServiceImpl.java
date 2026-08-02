package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCategoryMapper;
import com.ruoyi.system.domain.AppGoodsCategory;
import com.ruoyi.system.service.IAppGoodsCategoryService;

/**
 * 商品分类Service业务层处理
 * 
 * @author lankong
 * @date 2025-03-31
 */
@Service
public class AppGoodsCategoryServiceImpl implements IAppGoodsCategoryService 
{
    @Autowired
    private AppGoodsCategoryMapper appGoodsCategoryMapper;

    /**
     * 查询商品分类
     * 
     * @param categoryId 商品分类主键
     * @return 商品分类
     */
    @Override
    public AppGoodsCategory selectAppGoodsCategoryByCategoryId(Long categoryId)
    {
        return appGoodsCategoryMapper.selectAppGoodsCategoryByCategoryId(categoryId);
    }

    /**
     * 查询商品分类列表
     * 
     * @param appGoodsCategory 商品分类
     * @return 商品分类
     */
    @Override
    public List<AppGoodsCategory> selectAppGoodsCategoryList(AppGoodsCategory appGoodsCategory)
    {
        return appGoodsCategoryMapper.selectAppGoodsCategoryList(appGoodsCategory);
    }

    /**
     * 新增商品分类
     * 
     * @param appGoodsCategory 商品分类
     * @return 结果
     */
    @Override
    public int insertAppGoodsCategory(AppGoodsCategory appGoodsCategory)
    {
        return appGoodsCategoryMapper.insertAppGoodsCategory(appGoodsCategory);
    }

    /**
     * 修改商品分类
     * 
     * @param appGoodsCategory 商品分类
     * @return 结果
     */
    @Override
    public int updateAppGoodsCategory(AppGoodsCategory appGoodsCategory)
    {
        return appGoodsCategoryMapper.updateAppGoodsCategory(appGoodsCategory);
    }

    /**
     * 批量删除商品分类
     * 
     * @param categoryIds 需要删除的商品分类主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCategoryByCategoryIds(Long[] categoryIds)
    {
        return appGoodsCategoryMapper.deleteAppGoodsCategoryByCategoryIds(categoryIds);
    }

    /**
     * 删除商品分类信息
     * 
     * @param categoryId 商品分类主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCategoryByCategoryId(Long categoryId)
    {
        return appGoodsCategoryMapper.deleteAppGoodsCategoryByCategoryId(categoryId);
    }

    /**
     * 获取所有分类ID（包含子分类）
     * @param categoryId
     * @return
     */
    @Override
    public String selectAppGoodsCategoryAllIdsById(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        String allIds = "";
        allIds += "" + categoryId;
        AppGoodsCategory categoryWhere = new AppGoodsCategory();
        categoryWhere.setParentId(categoryId);
//        categoryWhere.setStatus("");
        List<AppGoodsCategory> categoryList = appGoodsCategoryMapper.selectAppGoodsCategoryList(categoryWhere);
        for (int i = 0; i < categoryList.size(); i++) {
            allIds += "," + categoryList.get(i).getCategoryId();
            categoryWhere.setParentId(categoryList.get(i).getCategoryId());
            List<AppGoodsCategory> categoryChildList = appGoodsCategoryMapper.selectAppGoodsCategoryList(categoryWhere);
            for (int j = 0; j < categoryChildList.size(); j++) {
                allIds += "," + categoryChildList.get(j).getCategoryId();
            }
        }
        return allIds;
    }

    @Override
    public AppGoodsCategory getCacheAppGoodsCategoryById(Long categoryId) {
        AppGoodsCategory appGoodsCategory = SpringUtils.getBean(RedisCache.class).getCacheObject(CacheConstants.APP_GOODS_CATEGROY +"id:" + categoryId);
        if (appGoodsCategory == null) {
            if(0==categoryId){
                appGoodsCategory = new AppGoodsCategory();
                appGoodsCategory.setCategoryId(0L);
                appGoodsCategory.setCategoryName("上海智享居");
                SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.APP_GOODS_CATEGROY + "id:" + categoryId, appGoodsCategory);
            }else {
                appGoodsCategory = appGoodsCategoryMapper.selectAppGoodsCategoryByCategoryId(categoryId);
                if (appGoodsCategory != null) {
                    SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.APP_GOODS_CATEGROY + "id:" + categoryId, appGoodsCategory);
                }
            }
        }
        return appGoodsCategory;
    }
}
