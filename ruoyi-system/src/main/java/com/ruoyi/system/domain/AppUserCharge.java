package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户充值对象 app_user_charge
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserCharge extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 充值id */
    private Long chargeId;

    /** 充值单号 */
    @Excel(name = "充值单号")
    private String orderNo;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 支付方式 */
    @Excel(name = "支付方式")
    private String payMethod;

    /** 统一支付单号 */
    @Excel(name = "统一支付单号")
    private String payNo;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal money;

    /** 是否已经发放奖励 */
    private Integer isAward;

    /** 充值状态 */
    @Excel(name = "充值状态")
    private String status;

    public void setChargeId(Long chargeId) 
    {
        this.chargeId = chargeId;
    }

    public Long getChargeId() 
    {
        return chargeId;
    }

    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setPayMethod(String payMethod) 
    {
        this.payMethod = payMethod;
    }

    public String getPayMethod() 
    {
        return payMethod;
    }

    public void setPayNo(String payNo) 
    {
        this.payNo = payNo;
    }

    public String getPayNo() 
    {
        return payNo;
    }

    public void setMoney(BigDecimal money) 
    {
        this.money = money;
    }

    public BigDecimal getMoney() 
    {
        return money;
    }

    public void setIsAward(Integer isAward) 
    {
        this.isAward = isAward;
    }

    public Integer getIsAward() 
    {
        return isAward;
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
            .append("chargeId", getChargeId())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("payMethod", getPayMethod())
            .append("payNo", getPayNo())
            .append("money", getMoney())
            .append("isAward", getIsAward())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
