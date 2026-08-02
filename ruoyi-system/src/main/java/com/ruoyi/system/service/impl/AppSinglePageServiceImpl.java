package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppSinglePageMapper;
import com.ruoyi.system.domain.AppSinglePage;
import com.ruoyi.system.service.IAppSinglePageService;

/**
 * 单页文章Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppSinglePageServiceImpl implements IAppSinglePageService 
{
    @Autowired
    private AppSinglePageMapper appSinglePageMapper;

    /**
     * 查询单页文章
     * 
     * @param pageId 单页文章主键
     * @return 单页文章
     */
    @Override
    public AppSinglePage selectAppSinglePageByPageId(Long pageId)
    {
        return appSinglePageMapper.selectAppSinglePageByPageId(pageId);
    }

    /**
     * 查询单页文章列表
     * 
     * @param appSinglePage 单页文章
     * @return 单页文章
     */
    @Override
    public List<AppSinglePage> selectAppSinglePageList(AppSinglePage appSinglePage)
    {
        return appSinglePageMapper.selectAppSinglePageList(appSinglePage);
    }

    /**
     * 新增单页文章
     * 
     * @param appSinglePage 单页文章
     * @return 结果
     */
    @Override
    public int insertAppSinglePage(AppSinglePage appSinglePage)
    {
        appSinglePage.setCreateTime(DateUtils.getNowDate());
        return appSinglePageMapper.insertAppSinglePage(appSinglePage);
    }

    /**
     * 修改单页文章
     * 
     * @param appSinglePage 单页文章
     * @return 结果
     */
    @Override
    public int updateAppSinglePage(AppSinglePage appSinglePage)
    {
        appSinglePage.setUpdateTime(DateUtils.getNowDate());
        return appSinglePageMapper.updateAppSinglePage(appSinglePage);
    }

    /**
     * 批量删除单页文章
     * 
     * @param pageIds 需要删除的单页文章主键
     * @return 结果
     */
    @Override
    public int deleteAppSinglePageByPageIds(Long[] pageIds)
    {
        return appSinglePageMapper.deleteAppSinglePageByPageIds(pageIds);
    }

    /**
     * 删除单页文章信息
     * 
     * @param pageId 单页文章主键
     * @return 结果
     */
    @Override
    public int deleteAppSinglePageByPageId(Long pageId)
    {
        return appSinglePageMapper.deleteAppSinglePageByPageId(pageId);
    }
}
