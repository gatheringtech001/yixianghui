package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppPayLogMapper;
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.service.IAppPayLogService;

/**
 * 支付记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-24
 */
@Service
public class AppPayLogServiceImpl implements IAppPayLogService 
{
    @Autowired
    private AppPayLogMapper appPayLogMapper;




    /**
     * 查询支付记录
     * 
     * @param logId 支付记录主键
     * @return 支付记录
     */
    @Override
    public AppPayLog selectAppPayLogByLogId(Long logId)
    {
        return appPayLogMapper.selectAppPayLogByLogId(logId);
    }

    /**
     * 查询支付记录列表
     * 
     * @param appPayLog 支付记录
     * @return 支付记录
     */
    @Override
    public List<AppPayLog> selectAppPayLogList(AppPayLog appPayLog)
    {
        return appPayLogMapper.selectAppPayLogList(appPayLog);
    }

    /**
     * 新增支付记录
     * 
     * @param appPayLog 支付记录
     * @return 结果
     */
    @Override
    public int insertAppPayLog(AppPayLog appPayLog)
    {
        appPayLog.setCreateTime(DateUtils.getNowDate());
        return appPayLogMapper.insertAppPayLog(appPayLog);
    }

    /**
     * 修改支付记录
     * 
     * @param appPayLog 支付记录
     * @return 结果
     */
    @Override
    public int updateAppPayLog(AppPayLog appPayLog)
    {
        appPayLog.setUpdateTime(DateUtils.getNowDate());
        return appPayLogMapper.updateAppPayLog(appPayLog);
    }

    /**
     * 批量删除支付记录
     * 
     * @param logIds 需要删除的支付记录主键
     * @return 结果
     */
    @Override
    public int deleteAppPayLogByLogIds(Long[] logIds)
    {
        return appPayLogMapper.deleteAppPayLogByLogIds(logIds);
    }

    /**
     * 删除支付记录信息
     * 
     * @param logId 支付记录主键
     * @return 结果
     */
    @Override
    public int deleteAppPayLogByLogId(Long logId)
    {
        return appPayLogMapper.deleteAppPayLogByLogId(logId);
    }

    @Override
    public AppPayLog selectAppPayLogByAgentPayNo(String agentPayNo) {
        return appPayLogMapper.selectAppPayLogByAgentPayNo(agentPayNo);
    }

    @Override
    public AppPayLog selectAppPayLogByPayNo(String payNo) {
        return appPayLogMapper.selectAppPayLogByPayNo(payNo);
    }
}
