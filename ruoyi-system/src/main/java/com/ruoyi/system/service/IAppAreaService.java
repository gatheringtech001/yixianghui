package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppArea;

/**
 * 行政区域Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppAreaService 
{
    /**
     * 查询行政区域
     * 
     * @param areaId 行政区域主键
     * @return 行政区域
     */
    public AppArea selectAppAreaByAreaId(Long areaId);

    /**
     * 查询行政区域列表
     * 
     * @param appArea 行政区域
     * @return 行政区域集合
     */
    public List<AppArea> selectAppAreaList(AppArea appArea);

    /**
     * 新增行政区域
     * 
     * @param appArea 行政区域
     * @return 结果
     */
    public int insertAppArea(AppArea appArea);

    /**
     * 修改行政区域
     * 
     * @param appArea 行政区域
     * @return 结果
     */
    public int updateAppArea(AppArea appArea);

    /**
     * 批量删除行政区域
     * 
     * @param areaIds 需要删除的行政区域主键集合
     * @return 结果
     */
    public int deleteAppAreaByAreaIds(Long[] areaIds);

    /**
     * 删除行政区域信息
     * 
     * @param areaId 行政区域主键
     * @return 结果
     */
    public int deleteAppAreaByAreaId(Long areaId);
}
