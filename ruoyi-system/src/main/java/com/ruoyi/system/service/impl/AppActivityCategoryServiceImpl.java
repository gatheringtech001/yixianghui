package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.system.domain.AppActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppActivityCategoryMapper;
import com.ruoyi.system.domain.AppActivityCategory;
import com.ruoyi.system.service.IAppActivityCategoryService;

/**
 * 活动分类Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppActivityCategoryServiceImpl implements IAppActivityCategoryService 
{
    @Autowired
    private AppActivityCategoryMapper appActivityCategoryMapper;

    /**
     * 查询活动分类
     * 
     * @param categoryId 活动分类主键
     * @return 活动分类
     */
    @Override
    public AppActivityCategory selectAppActivityCategoryByCategoryId(Long categoryId)
    {
        return appActivityCategoryMapper.selectAppActivityCategoryByCategoryId(categoryId);
    }

    /**
     * 查询活动分类列表
     * 
     * @param appActivityCategory 活动分类
     * @return 活动分类
     */
    @Override
    public List<AppActivityCategory> selectAppActivityCategoryList(AppActivityCategory appActivityCategory)
    {
        return appActivityCategoryMapper.selectAppActivityCategoryList(appActivityCategory);
    }

    /**
     * 新增活动分类
     * 
     * @param appActivityCategory 活动分类
     * @return 结果
     */
    @Override
    public int insertAppActivityCategory(AppActivityCategory appActivityCategory)
    {
        return appActivityCategoryMapper.insertAppActivityCategory(appActivityCategory);
    }

    /**
     * 修改活动分类
     * 
     * @param appActivityCategory 活动分类
     * @return 结果
     */
    @Override
    public int updateAppActivityCategory(AppActivityCategory appActivityCategory)
    {
        return appActivityCategoryMapper.updateAppActivityCategory(appActivityCategory);
    }

    /**
     * 批量删除活动分类
     * 
     * @param categoryIds 需要删除的活动分类主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityCategoryByCategoryIds(Long[] categoryIds)
    {
        return appActivityCategoryMapper.deleteAppActivityCategoryByCategoryIds(categoryIds);
    }

    /**
     * 删除活动分类信息
     * 
     * @param categoryId 活动分类主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityCategoryByCategoryId(Long categoryId)
    {
        return appActivityCategoryMapper.deleteAppActivityCategoryByCategoryId(categoryId);
    }

    /**
     * 通过ID获取分类名称（优先缓存）
     * @param categoryId
     * @return
     */
    @Override
    public String selectAppActivityCategoryCacheNameByCategoryId(Long categoryId) {
        // todo 先进行缓存查询，再进行数据查询
        AppActivityCategory info = selectAppActivityCategoryByCategoryId(categoryId);
        if (info == null) {
            return null;
        }
        return info.getCategoryName();
    }
}
