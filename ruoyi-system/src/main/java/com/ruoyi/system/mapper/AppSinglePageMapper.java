package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppSinglePage;

/**
 * 单页文章Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppSinglePageMapper 
{
    /**
     * 查询单页文章
     * 
     * @param pageId 单页文章主键
     * @return 单页文章
     */
    public AppSinglePage selectAppSinglePageByPageId(Long pageId);

    /**
     * 查询单页文章列表
     * 
     * @param appSinglePage 单页文章
     * @return 单页文章集合
     */
    public List<AppSinglePage> selectAppSinglePageList(AppSinglePage appSinglePage);

    /**
     * 新增单页文章
     * 
     * @param appSinglePage 单页文章
     * @return 结果
     */
    public int insertAppSinglePage(AppSinglePage appSinglePage);

    /**
     * 修改单页文章
     * 
     * @param appSinglePage 单页文章
     * @return 结果
     */
    public int updateAppSinglePage(AppSinglePage appSinglePage);

    /**
     * 删除单页文章
     * 
     * @param pageId 单页文章主键
     * @return 结果
     */
    public int deleteAppSinglePageByPageId(Long pageId);

    /**
     * 批量删除单页文章
     * 
     * @param pageIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppSinglePageByPageIds(Long[] pageIds);
}
