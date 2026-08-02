package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppArticleCategory;

/**
 * 内容分类Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppArticleCategoryService 
{
    /**
     * 查询内容分类
     * 
     * @param categoryId 内容分类主键
     * @return 内容分类
     */
    public AppArticleCategory selectAppArticleCategoryByCategoryId(Long categoryId);

    /**
     * 查询内容分类列表
     * 
     * @param appArticleCategory 内容分类
     * @return 内容分类集合
     */
    public List<AppArticleCategory> selectAppArticleCategoryList(AppArticleCategory appArticleCategory);

    /**
     * 新增内容分类
     * 
     * @param appArticleCategory 内容分类
     * @return 结果
     */
    public int insertAppArticleCategory(AppArticleCategory appArticleCategory);

    /**
     * 修改内容分类
     * 
     * @param appArticleCategory 内容分类
     * @return 结果
     */
    public int updateAppArticleCategory(AppArticleCategory appArticleCategory);

    /**
     * 批量删除内容分类
     * 
     * @param categoryIds 需要删除的内容分类主键集合
     * @return 结果
     */
    public int deleteAppArticleCategoryByCategoryIds(Long[] categoryIds);

    /**
     * 删除内容分类信息
     * 
     * @param categoryId 内容分类主键
     * @return 结果
     */
    public int deleteAppArticleCategoryByCategoryId(Long categoryId);
}
