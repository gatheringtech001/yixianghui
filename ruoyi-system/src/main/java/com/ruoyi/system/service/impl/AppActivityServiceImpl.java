package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppActivityMapper;
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.service.IAppActivityService;

/**
 * 活动Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppActivityServiceImpl implements IAppActivityService 
{
    @Autowired
    private AppActivityMapper appActivityMapper;

    /**
     * 查询活动
     * 
     * @param activityId 活动主键
     * @return 活动
     */
    @Override
    public AppActivity selectAppActivityByActivityId(Long activityId)
    {
        return appActivityMapper.selectAppActivityByActivityId(activityId);
    }

    /**
     * 查询活动列表
     * 
     * @param appActivity 活动
     * @return 活动
     */
    @Override
    public List<AppActivity> selectAppActivityList(AppActivity appActivity)
    {
        return appActivityMapper.selectAppActivityList(appActivity);
    }

    /**
     * 新增活动
     * 
     * @param appActivity 活动
     * @return 结果
     */
    @Override
    public int insertAppActivity(AppActivity appActivity)
    {
        appActivity.setCreateTime(DateUtils.getNowDate());
        return appActivityMapper.insertAppActivity(appActivity);
    }

    /**
     * 修改活动
     * 
     * @param appActivity 活动
     * @return 结果
     */
    @Override
    public int updateAppActivity(AppActivity appActivity)
    {
        appActivity.setUpdateTime(DateUtils.getNowDate());
        return appActivityMapper.updateAppActivity(appActivity);
    }

    /**
     * 批量删除活动
     * 
     * @param activityIds 需要删除的活动主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityByActivityIds(Long[] activityIds)
    {
        return appActivityMapper.deleteAppActivityByActivityIds(activityIds);
    }

    /**
     * 删除活动信息
     * 
     * @param activityId 活动主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityByActivityId(Long activityId)
    {
        return appActivityMapper.deleteAppActivityByActivityId(activityId);
    }
}
