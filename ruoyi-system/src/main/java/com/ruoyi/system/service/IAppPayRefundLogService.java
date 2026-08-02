package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppPayRefundLog;

/**
 * 退款记录Service接口
 * 
 * @author lankong
 * @date 2025-05-24
 */
public interface IAppPayRefundLogService 
{
    /**
     * 查询退款记录
     * 
     * @param logId 退款记录主键
     * @return 退款记录
     */
    public AppPayRefundLog selectAppPayRefundLogByLogId(Long logId);

    /**
     * 查询退款记录列表
     * 
     * @param appPayRefundLog 退款记录
     * @return 退款记录集合
     */
    public List<AppPayRefundLog> selectAppPayRefundLogList(AppPayRefundLog appPayRefundLog);

    /**
     * 新增退款记录
     * 
     * @param appPayRefundLog 退款记录
     * @return 结果
     */
    public int insertAppPayRefundLog(AppPayRefundLog appPayRefundLog);

    /**
     * 修改退款记录
     * 
     * @param appPayRefundLog 退款记录
     * @return 结果
     */
    public int updateAppPayRefundLog(AppPayRefundLog appPayRefundLog);

    /**
     * 批量删除退款记录
     * 
     * @param logIds 需要删除的退款记录主键集合
     * @return 结果
     */
    public int deleteAppPayRefundLogByLogIds(Long[] logIds);

    /**
     * 删除退款记录信息
     * 
     * @param logId 退款记录主键
     * @return 结果
     */
    public int deleteAppPayRefundLogByLogId(Long logId);
}
