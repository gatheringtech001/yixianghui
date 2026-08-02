package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 支付记录对象 app_pay_log
 * 
 * @author lankong
 * @date 2025-05-24
 */
public class AppPayLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键id */
    private Long logId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 业务订单id */
    @Excel(name = "业务订单id")
    private Long orderId;

    /** 订单类型 */
    @Excel(name = "订单类型")
    private String orderType;

    /** 唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号 */
    @Excel(name = "唯一识别单号：10开通为充值；20开头为商品；命名方式为：标识+日期+业务单号")
    private String payNo;

    /** 支付名称 */
    @Excel(name = "支付名称")
    private String payName;

    /** 支付说明 */
    @Excel(name = "支付说明")
    private String payDescription;

    /** 支付金额 */
    @Excel(name = "支付金额")
    private BigDecimal payMoney;

    /** 支付方式 */
    @Excel(name = "支付方式")
    private String payMethod;

    /** 支付机构名称 */
    @Excel(name = "支付机构名称")
    private String agentName;

    /** 机构订单号 */
    @Excel(name = "机构订单号")
    private String agentPayNo;

    /** 回调内容 */
    @Excel(name = "回调内容")
    private String notifyContent;

    /** 支付状态 */
    @Excel(name = "支付状态")
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

    public void setPayName(String payName) 
    {
        this.payName = payName;
    }

    public String getPayName() 
    {
        return payName;
    }

    public void setPayDescription(String payDescription) 
    {
        this.payDescription = payDescription;
    }

    public String getPayDescription() 
    {
        return payDescription;
    }

    public void setPayMoney(BigDecimal payMoney) 
    {
        this.payMoney = payMoney;
    }

    public BigDecimal getPayMoney() 
    {
        return payMoney;
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
            .append("payName", getPayName())
            .append("payDescription", getPayDescription())
            .append("payMoney", getPayMoney())
            .append("payMethod", getPayMethod())
            .append("agentName", getAgentName())
            .append("agentPayNo", getAgentPayNo())
            .append("notifyContent", getNotifyContent())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
