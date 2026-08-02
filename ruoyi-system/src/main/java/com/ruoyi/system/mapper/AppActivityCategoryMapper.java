package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppActivityCategory;

/**
 * 活动分类Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppActivityCategoryMapper 
{
    /**
     * 查询活动分类
     * 
     * @param categoryId 活动分类主键
     * @return 活动分类
     */
    public AppActivityCategory selectAppActivityCategoryByCategoryId(Long categoryId);

    /**
     * 查询活动分类列表
     * 
     * @param appActivityCategory 活动分类
     * @return 活动分类集合
     */
    public List<AppActivityCategory> selectAppActivityCategoryList(AppActivityCategory appActivityCategory);

    /**
     * 新增活动分类
     * 
     * @param appActivityCategory 活动分类
     * @return 结果
     */
    public int insertAppActivityCategory(AppActivityCategory appActivityCategory);

    /**
     * 修改活动分类
     * 
     * @param appActivityCategory 活动分类
     * @return 结果
     */
    public int updateAppActivityCategory(AppActivityCategory appActivityCategory);

    /**
     * 删除活动分类
     * 
     * @param categoryId 活动分类主键
     * @return 结果
     */
    public int deleteAppActivityCategoryByCategoryId(Long categoryId);

    /**
     * 批量删除活动分类
     * 
     * @param categoryIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppActivityCategoryByCategoryIds(Long[] categoryIds);
}
