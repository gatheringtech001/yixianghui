package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppAdPosition;

/**
 * 广告管理Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppAdPositionService 
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
     * 批量删除广告管理
     * 
     * @param positionIds 需要删除的广告管理主键集合
     * @return 结果
     */
    public int deleteAppAdPositionByPositionIds(Long[] positionIds);

    /**
     * 删除广告管理信息
     * 
     * @param positionId 广告管理主键
     * @return 结果
     */
    public int deleteAppAdPositionByPositionId(Long positionId);
}
