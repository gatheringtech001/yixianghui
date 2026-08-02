package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 退款记录对象 app_pay_refund_log
 * 
 * @author lankong
 * @date 2025-05-24
 */
public class AppPayRefundLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志id */
    private Long logId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 订单id */
    @Excel(name = "订单id")
    private Long orderId;

    /** 订单类型 */
    @Excel(name = "订单类型")
    private String orderType;

    /** 唯一识别单号 */
    @Excel(name = "唯一识别单号")
    private String payNo;

    /** 支付方式 */
    @Excel(name = "支付方式")
    private String payMethod;

    /** 支付机构名称 */
    @Excel(name = "支付机构名称")
    private String agentName;

    /** 支付单号 */
    @Excel(name = "支付单号")
    private String agentPayNo;

    /** 退款单号 */
    @Excel(name = "退款单号")
    private String agentRefundNo;

    /** 退款金额 */
    @Excel(name = "退款金额")
    private BigDecimal refundMoney;

    /** 回调内容 */
    @Excel(name = "回调内容")
    private String notifyContent;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    public void setLogId(Long logId) 
    {
        this.logId = logId;
    }

    public Long getLogId() 
    {
        return logId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setOrderType(String orderType) 
    {
        this.orderType = orderType;
    }

    public String getOrderType() 
    {
        return orderType;
    }

    public void setPayNo(String payNo) 
    {
        this.payNo = payNo;
    }

    public String getPayNo() 
    {
        return payNo;
    }

    public void setPayMethod(String payMethod) 
    {
        this.payMethod = payMethod;
    }

    public String getPayMethod() 
    {
        return payMethod;
    }

    public void setAgentName(String agentName) 
    {
        this.agentName = agentName;
    }

    public String getAgentName() 
    {
        return agentName;
    }

    public void setAgentPayNo(String agentPayNo) 
    {
        this.agentPayNo = agentPayNo;
    }

    public String getAgentPayNo() 
    {
        return agentPayNo;
    }

    public void setAgentRefundNo(String agentRefundNo) 
    {
        this.agentRefundNo = agentRefundNo;
    }

    public String getAgentRefundNo() 
    {
        return agentRefundNo;
    }

    public void setRefundMoney(BigDecimal refundMoney) 
    {
        this.refundMoney = refundMoney;
    }

    public BigDecimal getRefundMoney() 
    {
        return refundMoney;
    }

    public void setNotifyContent(String notifyContent) 
    {
        this.notifyContent = notifyContent;
    }

    public String getNotifyContent() 
    {
        return notifyContent;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("userId", getUserId())
            .append("orderId", getOrderId())
            .append("orderType", getOrderType())
            .append("payNo", getPayNo())
            .append("payMethod", getPayMethod())
            .append("agentName", getAgentName())
            .append("agentPayNo", getAgentPayNo())
            .append("agentRefundNo", getAgentRefundNo())
            .append("refundMoney", getRefundMoney())
            .append("notifyContent", getNotifyContent())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
