package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppPayLog;

/**
 * 支付记录Service接口
 * 
 * @author lankong
 * @date 2025-05-24
 */
public interface IAppPayLogService 
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
     * 批量删除支付记录
     * 
     * @param logIds 需要删除的支付记录主键集合
     * @return 结果
     */
    public int deleteAppPayLogByLogIds(Long[] logIds);

    /**
     * 删除支付记录信息
     * 
     * @param logId 支付记录主键
     * @return 结果
     */
    public int deleteAppPayLogByLogId(Long logId);
    /**
     * 根据商户订单号查询支付记录
     * @param agentPayNo
     * @return
     */
    public AppPayLog selectAppPayLogByAgentPayNo(String agentPayNo);

    /**
     * 根据微信支付订单号查询支付记录
     * @param payNo
     * @return
     */
    public AppPayLog selectAppPayLogByPayNo(String payNo);
}
