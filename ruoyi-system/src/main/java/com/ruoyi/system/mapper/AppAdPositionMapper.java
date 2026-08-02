package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppAdPosition;
import com.ruoyi.system.domain.AppAdContent;

/**
 * 广告管理Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppAdPositionMapper 
{
    /**
     * 查询广告管理
     * 
     * @param positionId 广告管理主键
     * @return 广告管理
     */
    public AppAdPosition selectAppAdPositionByPositionId(Long positionId);

    /**
     * 查询广告管理列表
     * 
     * @param appAdPosition 广告管理
     * @return 广告管理集合
     */
    public List<AppAdPosition> selectAppAdPositionList(AppAdPosition appAdPosition);

    /**
     * 新增广告管理
     * 
     * @param appAdPosition 广告管理
     * @return 结果
     */
    public int insertAppAdPosition(AppAdPosition appAdPosition);

    /**
     * 修改广告管理
     * 
     * @param appAdPosition 广告管理
     * @return 结果
     */
    public int updateAppAdPosition(AppAdPosition appAdPosition);

    /**
     * 删除广告管理
     * 
     * @param positionId 广告管理主键
     * @return 结果
     */
    public int deleteAppAdPositionByPositionId(Long positionId);

    /**
     * 批量删除广告管理
     * 
     * @param positionIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppAdPositionByPositionIds(Long[] positionIds);

    /**
     * 批量删除广告内容
     * 
     * @param positionIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppAdContentByPositionIds(Long[] positionIds);
    
    /**
     * 批量新增广告内容
     * 
     * @param appAdContentList 广告内容列表
     * @return 结果
     */
    public int batchAppAdContent(List<AppAdContent> appAdContentList);
    

    /**
     * 通过广告管理主键删除广告内容信息
     * 
     * @param positionId 广告管理ID
     * @return 结果
     */
    public int deleteAppAdContentByPositionId(Long positionId);
}
