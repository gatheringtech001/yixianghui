package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppPayRefundLog;

/**
 * 退款记录Mapper接口
 * 
 * @author lankong
 * @date 2025-05-24
 */
public interface AppPayRefundLogMapper 
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
     * 删除退款记录
     * 
     * @param logId 退款记录主键
     * @return 结果
     */
    public int deleteAppPayRefundLogByLogId(Long logId);

    /**
     * 批量删除退款记录
     * 
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppPayRefundLogByLogIds(Long[] logIds);

    /**
     * 根据支付单号查询退款记录
     * @param payNo
     * @return
     */
    public AppPayRefundLog selectAppPayRefundLogByPayno(String payNo);

    /**
     * 按商户退款单号查询
     */
    public AppPayRefundLog selectAppPayRefundLogByAgentRefundNo(String agentRefundNo);
}
