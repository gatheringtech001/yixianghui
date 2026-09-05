package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AppActivity;

/**
 * 活动Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppActivityMapper 
{
    /**
     * 查询活动
     * 
     * @param activityId 活动主键
     * @return 活动
     */
    public AppActivity selectAppActivityByActivityId(Long activityId);

    AppActivity selectAppActivityByActivityIdForUpdate(Long activityId);

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
     * 删除活动
     * 
     * @param activityId 活动主键
     * @return 结果
     */
    public int deleteAppActivityByActivityId(Long activityId);

    /**
     * 批量删除活动
     * 
     * @param activityIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppActivityByActivityIds(Long[] activityIds);

    /**
     * 增加活动已报名人数
     */
    public int increaseSignCount(@Param("activityId") Long activityId,
                                 @Param("count") int count);

    /**
     * 减少活动已报名人数
     */
    public int decreaseSignCount(@Param("activityId") Long activityId,
                                 @Param("count") int count);
}
