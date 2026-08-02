package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppAdContentMapper;
import com.ruoyi.system.domain.AppAdContent;
import com.ruoyi.system.service.IAppAdContentService;

/**
 * 广告内容Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppAdContentServiceImpl implements IAppAdContentService 
{
    @Autowired
    private AppAdContentMapper appAdContentMapper;

    /**
     * 查询广告内容
     * 
     * @param contentId 广告内容主键
     * @return 广告内容
     */
    @Override
    public AppAdContent selectAppAdContentByContentId(Long contentId)
    {
        return appAdContentMapper.selectAppAdContentByContentId(contentId);
    }

    /**
     * 查询广告内容列表
     * 
     * @param appAdContent 广告内容
     * @return 广告内容
     */
    @Override
    public List<AppAdContent> selectAppAdContentList(AppAdContent appAdContent)
    {
        return appAdContentMapper.selectAppAdContentList(appAdContent);
    }

    /**
     * 新增广告内容
     * 
     * @param appAdContent 广告内容
     * @return 结果
     */
    @Override
    public int insertAppAdContent(AppAdContent appAdContent)
    {
        return appAdContentMapper.insertAppAdContent(appAdContent);
    }

    /**
     * 修改广告内容
     * 
     * @param appAdContent 广告内容
     * @return 结果
     */
    @Override
    public int updateAppAdContent(AppAdContent appAdContent)
    {
        return appAdContentMapper.updateAppAdContent(appAdContent);
    }

    /**
     * 批量删除广告内容
     * 
     * @param contentIds 需要删除的广告内容主键
     * @return 结果
     */
    @Override
    public int deleteAppAdContentByContentIds(Long[] contentIds)
    {
        return appAdContentMapper.deleteAppAdContentByContentIds(contentIds);
    }

    /**
     * 删除广告内容信息
     * 
     * @param contentId 广告内容主键
     * @return 结果
     */
    @Override
    public int deleteAppAdContentByContentId(Long contentId)
    {
        return appAdContentMapper.deleteAppAdContentByContentId(contentId);
    }

    @Override
    public List<AppAdContent> selectAppAdContentListByPositionId(Long positionId)
        {
            return appAdContentMapper.selectAppAdContentListByPositionId(positionId);
        }
}
