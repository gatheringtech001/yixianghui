package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppPayRefundLogMapper;
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.service.IAppPayRefundLogService;

/**
 * 退款记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-24
 */
@Service
public class AppPayRefundLogServiceImpl implements IAppPayRefundLogService 
{
    @Autowired
    private AppPayRefundLogMapper appPayRefundLogMapper;

    /**
     * 查询退款记录
     * 
     * @param logId 退款记录主键
     * @return 退款记录
     */
    @Override
    public AppPayRefundLog selectAppPayRefundLogByLogId(Long logId)
    {
        return appPayRefundLogMapper.selectAppPayRefundLogByLogId(logId);
    }

    /**
     * 查询退款记录列表
     * 
     * @param appPayRefundLog 退款记录
     * @return 退款记录
     */
    @Override
    public List<AppPayRefundLog> selectAppPayRefundLogList(AppPayRefundLog appPayRefundLog)
    {
        return appPayRefundLogMapper.selectAppPayRefundLogList(appPayRefundLog);
    }

    /**
     * 新增退款记录
     * 
     * @param appPayRefundLog 退款记录
     * @return 结果
     */
    @Override
    public int insertAppPayRefundLog(AppPayRefundLog appPayRefundLog)
    {
        appPayRefundLog.setCreateTime(DateUtils.getNowDate());
        return appPayRefundLogMapper.insertAppPayRefundLog(appPayRefundLog);
    }

    /**
     * 修改退款记录
     * 
     * @param appPayRefundLog 退款记录
     * @return 结果
     */
    @Override
    public int updateAppPayRefundLog(AppPayRefundLog appPayRefundLog)
    {
        appPayRefundLog.setUpdateTime(DateUtils.getNowDate());
        return appPayRefundLogMapper.updateAppPayRefundLog(appPayRefundLog);
    }

    /**
     * 批量删除退款记录
     * 
     * @param logIds 需要删除的退款记录主键
     * @return 结果
     */
    @Override
    public int deleteAppPayRefundLogByLogIds(Long[] logIds)
    {
        return appPayRefundLogMapper.deleteAppPayRefundLogByLogIds(logIds);
    }

    /**
     * 删除退款记录信息
     * 
     * @param logId 退款记录主键
     * @return 结果
     */
    @Override
    public int deleteAppPayRefundLogByLogId(Long logId)
    {
        return appPayRefundLogMapper.deleteAppPayRefundLogByLogId(logId);
    }
}
