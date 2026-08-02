package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserMoneyLogMapper;
import com.ruoyi.system.domain.AppUserMoneyLog;
import com.ruoyi.system.service.IAppUserMoneyLogService;

/**
 * 钱包记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserMoneyLogServiceImpl implements IAppUserMoneyLogService 
{
    @Autowired
    private AppUserMoneyLogMapper appUserMoneyLogMapper;

    /**
     * 查询钱包记录
     * 
     * @param logId 钱包记录主键
     * @return 钱包记录
     */
    @Override
    public AppUserMoneyLog selectAppUserMoneyLogByLogId(Long logId)
    {
        return appUserMoneyLogMapper.selectAppUserMoneyLogByLogId(logId);
    }

    /**
     * 查询钱包记录列表
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 钱包记录
     */
    @Override
    public List<AppUserMoneyLog> selectAppUserMoneyLogList(AppUserMoneyLog appUserMoneyLog)
    {
        return appUserMoneyLogMapper.selectAppUserMoneyLogList(appUserMoneyLog);
    }

    /**
     * 新增钱包记录
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 结果
     */
    @Override
    public int insertAppUserMoneyLog(AppUserMoneyLog appUserMoneyLog)
    {
        appUserMoneyLog.setCreateTime(DateUtils.getNowDate());
        return appUserMoneyLogMapper.insertAppUserMoneyLog(appUserMoneyLog);
    }

    /**
     * 修改钱包记录
     * 
     * @param appUserMoneyLog 钱包记录
     * @return 结果
     */
    @Override
    public int updateAppUserMoneyLog(AppUserMoneyLog appUserMoneyLog)
    {
        return appUserMoneyLogMapper.updateAppUserMoneyLog(appUserMoneyLog);
    }

    /**
     * 批量删除钱包记录
     * 
     * @param logIds 需要删除的钱包记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserMoneyLogByLogIds(Long[] logIds)
    {
        return appUserMoneyLogMapper.deleteAppUserMoneyLogByLogIds(logIds);
    }

    /**
     * 删除钱包记录信息
     * 
     * @param logId 钱包记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserMoneyLogByLogId(Long logId)
    {
        return appUserMoneyLogMapper.deleteAppUserMoneyLogByLogId(logId);
    }
}
