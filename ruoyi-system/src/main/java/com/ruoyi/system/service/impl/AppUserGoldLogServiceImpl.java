package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserGoldLogMapper;
import com.ruoyi.system.domain.AppUserGoldLog;
import com.ruoyi.system.service.IAppUserGoldLogService;

/**
 * 金币记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserGoldLogServiceImpl implements IAppUserGoldLogService 
{
    @Autowired
    private AppUserGoldLogMapper appUserGoldLogMapper;

    /**
     * 查询金币记录
     * 
     * @param logId 金币记录主键
     * @return 金币记录
     */
    @Override
    public AppUserGoldLog selectAppUserGoldLogByLogId(Long logId)
    {
        return appUserGoldLogMapper.selectAppUserGoldLogByLogId(logId);
    }

    /**
     * 查询金币记录列表
     * 
     * @param appUserGoldLog 金币记录
     * @return 金币记录
     */
    @Override
    public List<AppUserGoldLog> selectAppUserGoldLogList(AppUserGoldLog appUserGoldLog)
    {
        return appUserGoldLogMapper.selectAppUserGoldLogList(appUserGoldLog);
    }

    /**
     * 新增金币记录
     * 
     * @param appUserGoldLog 金币记录
     * @return 结果
     */
    @Override
    public int insertAppUserGoldLog(AppUserGoldLog appUserGoldLog)
    {
        appUserGoldLog.setCreateTime(DateUtils.getNowDate());
        return appUserGoldLogMapper.insertAppUserGoldLog(appUserGoldLog);
    }

    /**
     * 修改金币记录
     * 
     * @param appUserGoldLog 金币记录
     * @return 结果
     */
    @Override
    public int updateAppUserGoldLog(AppUserGoldLog appUserGoldLog)
    {
        return appUserGoldLogMapper.updateAppUserGoldLog(appUserGoldLog);
    }

    /**
     * 批量删除金币记录
     * 
     * @param logIds 需要删除的金币记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserGoldLogByLogIds(Long[] logIds)
    {
        return appUserGoldLogMapper.deleteAppUserGoldLogByLogIds(logIds);
    }

    /**
     * 删除金币记录信息
     * 
     * @param logId 金币记录主键
     * @return 结果
     */
    @Override
    public int deleteAppUserGoldLogByLogId(Long logId)
    {
        return appUserGoldLogMapper.deleteAppUserGoldLogByLogId(logId);
    }
}
