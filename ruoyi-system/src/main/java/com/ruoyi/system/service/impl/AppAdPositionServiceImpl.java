package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.AppAdContent;
import com.ruoyi.system.mapper.AppAdPositionMapper;
import com.ruoyi.system.domain.AppAdPosition;
import com.ruoyi.system.service.IAppAdPositionService;

/**
 * 广告管理Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppAdPositionServiceImpl implements IAppAdPositionService 
{
    @Autowired
    private AppAdPositionMapper appAdPositionMapper;

    /**
     * 查询广告管理
     * 
     * @param positionId 广告管理主键
     * @return 广告管理
     */
    @Override
    public AppAdPosition selectAppAdPositionByPositionId(Long positionId)
    {
        return appAdPositionMapper.selectAppAdPositionByPositionId(positionId);
    }

    /**
     * 查询广告管理列表
     * 
     * @param appAdPosition 广告管理
     * @return 广告管理
     */
    @Override
    public List<AppAdPosition> selectAppAdPositionList(AppAdPosition appAdPosition)
    {
        return appAdPositionMapper.selectAppAdPositionList(appAdPosition);
    }

    /**
     * 新增广告管理
     * 
     * @param appAdPosition 广告管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertAppAdPosition(AppAdPosition appAdPosition)
    {
        appAdPosition.setCreateTime(DateUtils.getNowDate());
        int rows = appAdPositionMapper.insertAppAdPosition(appAdPosition);
        insertAppAdContent(appAdPosition);
        return rows;
    }

    /**
     * 修改广告管理
     * 
     * @param appAdPosition 广告管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateAppAdPosition(AppAdPosition appAdPosition)
    {
        appAdPositionMapper.deleteAppAdContentByPositionId(appAdPosition.getPositionId());
        insertAppAdContent(appAdPosition);
        return appAdPositionMapper.updateAppAdPosition(appAdPosition);
    }

    /**
     * 批量删除广告管理
     * 
     * @param positionIds 需要删除的广告管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteAppAdPositionByPositionIds(Long[] positionIds)
    {
        appAdPositionMapper.deleteAppAdContentByPositionIds(positionIds);
        return appAdPositionMapper.deleteAppAdPositionByPositionIds(positionIds);
    }

    /**
     * 删除广告管理信息
     * 
     * @param positionId 广告管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteAppAdPositionByPositionId(Long positionId)
    {
        appAdPositionMapper.deleteAppAdContentByPositionId(positionId);
        return appAdPositionMapper.deleteAppAdPositionByPositionId(positionId);
    }

    /**
     * 新增广告内容信息
     * 
     * @param appAdPosition 广告管理对象
     */
    public void insertAppAdContent(AppAdPosition appAdPosition)
    {
        List<AppAdContent> appAdContentList = appAdPosition.getAppAdContentList();
        Long positionId = appAdPosition.getPositionId();
        if (StringUtils.isNotNull(appAdContentList))
        {
            List<AppAdContent> list = new ArrayList<AppAdContent>();
            for (AppAdContent appAdContent : appAdContentList)
            {
                appAdContent.setPositionId(positionId);
                list.add(appAdContent);
            }
            if (list.size() > 0)
            {
                appAdPositionMapper.batchAppAdContent(list);
            }
        }
    }
}
