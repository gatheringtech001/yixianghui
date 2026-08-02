package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserScoreLogMapper;
import com.ruoyi.system.domain.AppUserScoreLog;
import com.ruoyi.system.service.IAppUserScoreLogService;

/**
 * 积分记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserScoreLogServiceImpl implements IAppUserScoreLogService 
{
    @Autowired
    private AppUserScoreLogMapper appUserScoreLogMapper;

    /**
     * 查询积分记录
     * 
     * @param logId 积分记录主键
     * @return 积分记录
     */
    @Override
    public AppUserScoreLog selectAppUserScoreLogByLogId(Long logId)
    {
        return appUserScoreLogMapper.selectAppUserScoreLogByLogId(logId);
    }

    /**
     * 查询积分记录列表
     * 
     * @param appUserScoreLog 积分记录
     * @return 积分记录
     */
    @Override
    public List<AppUserScoreLog> selectAppUserScoreLogList(AppUserScoreLog appUserScoreLog)
    {
        return appUserScoreLogMapper.selectAppUserScoreLogList(appUserScoreLog);
    }

    /**
     * 新增积分记录
     * 
     * @param appUserScoreLog 积分记录
     * @return 结果
     */
    @Override
    public int insertAppUserScoreLog(AppUserScoreLog appUserScoreLog)
    {
        appUserScoreLog.setCreateTime(DateUtils.getNowDate());
        return appUserScoreLogMapper.insertAppUserScoreLog(appUserScoreLog);
    }

    /**
     * 修改积分记录
     * 
     * @param appUserScoreLog 积分记录
     * @return 结果
     */
    @Override
    public int updateAppUserScoreLog(AppUserScoreLog appUserScoreLog)
    {
        return appUserScoreLogMapper.updateAppUserScoreLog(appUserScoreLog);
    }

    /**
     * 批量删除积分记录
     * 
     * @param logIds 需要删除的积分记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserScoreLogByLogIds(Long[] logIds)
    {
        return appUserScoreLogMapper.deleteAppUserScoreLogByLogIds(logIds);
    }

    /**
     * 删除积分记录信息
     * 
     * @param logId 积分记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserScoreLogByLogId(Long logId)
    {
        return appUserScoreLogMapper.deleteAppUserScoreLogByLogId(logId);
    }
}
