package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppActivity;

/**
 * 活动Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppActivityService 
{
    /**
     * 查询活动
     * 
     * @param activityId 活动主键
     * @return 活动
     */
    public AppActivity selectAppActivityByActivityId(Long activityId);

    /**
     * 查询活动列表
     * 
     * @param appActivity 活动
     * @return 活动集合
     */
    public List<AppActivity> selectAppActivityList(AppActivity appActivity);

    /**
     * 新增活动
     * 
     * @param appActivity 活动
     * @return 结果
     */
    public int insertAppActivity(AppActivity appActivity);

    /**
     * 修改活动
     * 
     * @param appActivity 活动
     * @return 结果
     */
    public int updateAppActivity(AppActivity appActivity);

    /**
     * 批量删除活动
     * 
     * @param activityIds 需要删除的活动主键集合
     * @return 结果
     */
    public int deleteAppActivityByActivityIds(Long[] activityIds);

    /**
     * 删除活动信息
     * 
     * @param activityId 活动主键
     * @return 结果
     */
    public int deleteAppActivityByActivityId(Long activityId);
}
