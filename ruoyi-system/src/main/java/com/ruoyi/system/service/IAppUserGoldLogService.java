package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppUserGoldLog;

/**
 * 金币记录Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppUserGoldLogService 
{
    /**
     * 查询金币记录
     * 
     * @param logId 金币记录主键
     * @return 金币记录
     */
    public AppUserGoldLog selectAppUserGoldLogByLogId(Long logId);

    /**
     * 查询金币记录列表
     * 
     * @param appUserGoldLog 金币记录
     * @return 金币记录集合
     */
    public List<AppUserGoldLog> selectAppUserGoldLogList(AppUserGoldLog appUserGoldLog);

    /**
     * 新增金币记录
     * 
     * @param appUserGoldLog 金币记录
     * @return 结果
     */
    public int insertAppUserGoldLog(AppUserGoldLog appUserGoldLog);

    /**
     * 修改金币记录
     * 
     * @param appUserGoldLog 金币记录
     * @return 结果
     */
    public int updateAppUserGoldLog(AppUserGoldLog appUserGoldLog);

    /**
     * 批量删除金币记录
     * 
     * @param logIds 需要删除的金币记录主键集合
     * @return 结果
     */
    public int deleteAppUserGoldLogByLogIds(Long[] logIds);

    /**
     * 删除金币记录信息
     * 
     * @param logId 金币记录主键
     * @return 结果
     */
    public int deleteAppUserGoldLogByLogId(Long logId);
}
