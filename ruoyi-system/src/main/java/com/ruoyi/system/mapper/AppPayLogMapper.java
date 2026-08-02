package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppPayLog;

/**
 * 支付记录Mapper接口
 * 
 * @author lankong
 * @date 2025-05-24
 */
public interface AppPayLogMapper 
{
    /**
     * 查询支付记录
     * 
     * @param logId 支付记录主键
     * @return 支付记录
     */
    public AppPayLog selectAppPayLogByLogId(Long logId);

    /**
     * 查询支付记录列表
     * 
     * @param appPayLog 支付记录
     * @return 支付记录集合
     */
    public List<AppPayLog> selectAppPayLogList(AppPayLog appPayLog);

    /**
     * 新增支付记录
     * 
     * @param appPayLog 支付记录
     * @return 结果
     */
    public int insertAppPayLog(AppPayLog appPayLog);

    /**
     * 修改支付记录
     * 
     * @param appPayLog 支付记录
     * @return 结果
     */
    public int updateAppPayLog(AppPayLog appPayLog);

    /**
     * 删除支付记录
     * 
     * @param logId 支付记录主键
     * @return 结果
     */
    public int deleteAppPayLogByLogId(Long logId);

    /**
     * 批量删除支付记录
     * 
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppPayLogByLogIds(Long[] logIds);

    /**
     * 根据商户订单号查询支付记录
     * @param agentPayNo
     * @return
     */
    public AppPayLog selectAppPayLogByAgentPayNo(String agentPayNo);

    /**
     * 根据商户订单号查询支付记录
     * @param payNo
     * @return
     */
    public AppPayLog selectAppPayLogByPayNo(String payNo);
}
