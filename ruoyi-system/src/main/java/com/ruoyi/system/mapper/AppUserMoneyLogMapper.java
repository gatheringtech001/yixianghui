package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppUserMoneyLog;

/**
 * 钱包记录Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppUserMoneyLogMapper 
{
    /**
     * 查询钱包记录
     * 
     * @param logId 钱包记录主键
     * @return 钱包记录
     */
    public AppUserMoneyLog selectAppUserMoneyLogByLogId(Long logId);

    /**
     * 查询钱包记录列表
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 钱包记录集合
     */
    public List<AppUserMoneyLog> selectAppUserMoneyLogList(AppUserMoneyLog appUserMoneyLog);

    /**
     * 新增钱包记录
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 结果
     */
    public int insertAppUserMoneyLog(AppUserMoneyLog appUserMoneyLog);

    /**
     * 修改钱包记录
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 结果
     */
    public int updateAppUserMoneyLog(AppUserMoneyLog appUserMoneyLog);

    /**
     * 删除钱包记录
     * 
     * @param logId 钱包记录主键
     * @return 结果
     */
    public int deleteAppUserMoneyLogByLogId(Long logId);

    /**
     * 批量删除钱包记录
     * 
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserMoneyLogByLogIds(Long[] logIds);
}
