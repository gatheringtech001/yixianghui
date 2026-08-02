package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppUserScoreLog;

/**
 * 积分记录Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserScoreLogService 
{
    /**
     * 查询积分记录
     * 
     * @param logId 积分记录主键
     * @return 积分记录
     */
    public AppUserScoreLog selectAppUserScoreLogByLogId(Long logId);

    /**
     * 查询积分记录列表
     * 
     * @param appUserScoreLog 积分记录
     * @return 积分记录集合
     */
    public List<AppUserScoreLog> selectAppUserScoreLogList(AppUserScoreLog appUserScoreLog);

    /**
     * 新增积分记录
     * 
     * @param appUserScoreLog 积分记录
     * @return 结果
     */
    public int insertAppUserScoreLog(AppUserScoreLog appUserScoreLog);

    /**
     * 修改积分记录
     * 
     * @param appUserScoreLog 积分记录
     * @return 结果
     */
    public int updateAppUserScoreLog(AppUserScoreLog appUserScoreLog);

    /**
     * 批量删除积分记录
     * 
     * @param logIds 需要删除的积分记录主键集合
     * @return 结果
     */
    public int deleteAppUserScoreLogByLogIds(Long[] logIds);

    /**
     * 删除积分记录信息
     * 
     * @param logId 积分记录主键
     * @return 结果
     */
    public int deleteAppUserScoreLogByLogId(Long logId);
}
