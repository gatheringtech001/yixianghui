package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppAdContent;

/**
 * 广告内容Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppAdContentService 
{
    /**
     * 查询广告内容
     * 
     * @param contentId 广告内容主键
     * @return 广告内容
     */
    public AppAdContent selectAppAdContentByContentId(Long contentId);

    /**
     * 查询广告内容列表
     * 
     * @param appAdContent 广告内容
     * @return 广告内容集合
     */
    public List<AppAdContent> selectAppAdContentList(AppAdContent appAdContent);

    /**
     * 新增广告内容
     * 
     * @param appAdContent 广告内容
     * @return 结果
     */
    public int insertAppAdContent(AppAdContent appAdContent);

    /**
     * 修改广告内容
     * 
     * @param appAdContent 广告内容
     * @return 结果
     */
    public int updateAppAdContent(AppAdContent appAdContent);

    /**
     * 批量删除广告内容
     * 
     * @param contentIds 需要删除的广告内容主键集合
     * @return 结果
     */
    public int deleteAppAdContentByContentIds(Long[] contentIds);

    /**
     * 删除广告内容信息
     * 
     * @param contentId 广告内容主键
     * @return 结果
     */
    public int deleteAppAdContentByContentId(Long contentId);

    public List<AppAdContent> selectAppAdContentListByPositionId(Long positionId);
}
