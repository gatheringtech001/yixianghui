package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppArticleCategoryMapper;
import com.ruoyi.system.domain.AppArticleCategory;
import com.ruoyi.system.service.IAppArticleCategoryService;

/**
 * 内容分类Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppArticleCategoryServiceImpl implements IAppArticleCategoryService 
{
    @Autowired
    private AppArticleCategoryMapper appArticleCategoryMapper;

    /**
     * 查询内容分类
     * 
     * @param categoryId 内容分类主键
     * @return 内容分类
     */
    @Override
    public AppArticleCategory selectAppArticleCategoryByCategoryId(Long categoryId)
    {
        return appArticleCategoryMapper.selectAppArticleCategoryByCategoryId(categoryId);
    }

    /**
     * 查询内容分类列表
     * 
     * @param appArticleCategory 内容分类
     * @return 内容分类
     */
    @Override
    public List<AppArticleCategory> selectAppArticleCategoryList(AppArticleCategory appArticleCategory)
    {
        return appArticleCategoryMapper.selectAppArticleCategoryList(appArticleCategory);
    }

    /**
     * 新增内容分类
     * 
     * @param appArticleCategory 内容分类
     * @return 结果
     */
    @Override
    public int insertAppArticleCategory(AppArticleCategory appArticleCategory)
    {
        return appArticleCategoryMapper.insertAppArticleCategory(appArticleCategory);
    }

    /**
     * 修改内容分类
     * 
     * @param appArticleCategory 内容分类
     * @return 结果
     */
    @Override
    public int updateAppArticleCategory(AppArticleCategory appArticleCategory)
    {
        return appArticleCategoryMapper.updateAppArticleCategory(appArticleCategory);
    }

    /**
     * 批量删除内容分类
     * 
     * @param categoryIds 需要删除的内容分类主键
     * @return 结果
     */
    @Override
    public int deleteAppArticleCategoryByCategoryIds(Long[] categoryIds)
    {
        return appArticleCategoryMapper.deleteAppArticleCategoryByCategoryIds(categoryIds);
    }

    /**
     * 删除内容分类信息
     * 
     * @param categoryId 内容分类主键
     * @return 结果
     */
    @Override
    public int deleteAppArticleCategoryByCategoryId(Long categoryId)
    {
        return appArticleCategoryMapper.deleteAppArticleCategoryByCategoryId(categoryId);
    }
}
