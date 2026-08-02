package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppAreaMapper;
import com.ruoyi.system.domain.AppArea;
import com.ruoyi.system.service.IAppAreaService;

/**
 * 行政区域Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppAreaServiceImpl implements IAppAreaService 
{
    @Autowired
    private AppAreaMapper appAreaMapper;

    /**
     * 查询行政区域
     * 
     * @param areaId 行政区域主键
     * @return 行政区域
     */
    @Override
    public AppArea selectAppAreaByAreaId(Long areaId)
    {
        return appAreaMapper.selectAppAreaByAreaId(areaId);
    }

    /**
     * 查询行政区域列表
     * 
     * @param appArea 行政区域
     * @return 行政区域
     */
    @Override
    public List<AppArea> selectAppAreaList(AppArea appArea)
    {
        return appAreaMapper.selectAppAreaList(appArea);
    }

    /**
     * 新增行政区域
     * 
     * @param appArea 行政区域
     * @return 结果
     */
    @Override
    public int insertAppArea(AppArea appArea)
    {
        return appAreaMapper.insertAppArea(appArea);
    }

    /**
     * 修改行政区域
     * 
     * @param appArea 行政区域
     * @return 结果
     */
    @Override
    public int updateAppArea(AppArea appArea)
    {
        return appAreaMapper.updateAppArea(appArea);
    }

    /**
     * 批量删除行政区域
     * 
     * @param areaIds 需要删除的行政区域主键
     * @return 结果
     */
    @Override
    public int deleteAppAreaByAreaIds(Long[] areaIds)
    {
        return appAreaMapper.deleteAppAreaByAreaIds(areaIds);
    }

    /**
     * 删除行政区域信息
     * 
     * @param areaId 行政区域主键
     * @return 结果
     */
    @Override
    public int deleteAppAreaByAreaId(Long areaId)
    {
        return appAreaMapper.deleteAppAreaByAreaId(areaId);
    }
}
