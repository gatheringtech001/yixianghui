package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户提现对象 app_user_cash
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserCash extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 提现id */
    private Long cashId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 提现类型 */
    @Excel(name = "提现类型")
    private String cashType;

    /** 支付机构单号 */
    @Excel(name = "支付机构单号")
    private String payNo;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal money;

    /** 手续费 */
    @Excel(name = "手续费")
    private BigDecimal cashFee;

    /** 用户银行id */
    @Excel(name = "用户银行id")
    private String bankId;

    /** 用户支付宝id */
    @Excel(name = "用户支付宝id")
    private String alipayId;

    /** 用户微信id */
    @Excel(name = "用户微信id")
    private String weixinId;

    /** 提现回执 */
    @Excel(name = "提现回执")
    private String replyContent;

    /** 提现状态 */
    @Excel(name = "提现状态")
    private String status;

    public void setCashId(Long cashId) 
    {
        this.cashId = cashId;
    }

    public Long getCashId() 
    {
        return cashId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCashType(String cashType) 
    {
        this.cashType = cashType;
    }

    public String getCashType() 
    {
        return cashType;
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

    public void setCashFee(BigDecimal cashFee) 
    {
        this.cashFee = cashFee;
    }

    public BigDecimal getCashFee() 
    {
        return cashFee;
    }

    public void setBankId(String bankId) 
    {
        this.bankId = bankId;
    }

    public String getBankId() 
    {
        return bankId;
    }

    public void setAlipayId(String alipayId) 
    {
        this.alipayId = alipayId;
    }

    public String getAlipayId() 
    {
        return alipayId;
    }

    public void setWeixinId(String weixinId) 
    {
        this.weixinId = weixinId;
    }

    public String getWeixinId() 
    {
        return weixinId;
    }

    public void setReplyContent(String replyContent) 
    {
        this.replyContent = replyContent;
    }

    public String getReplyContent() 
    {
        return replyContent;
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
            .append("cashId", getCashId())
            .append("userId", getUserId())
            .append("cashType", getCashType())
            .append("payNo", getPayNo())
            .append("money", getMoney())
            .append("cashFee", getCashFee())
            .append("bankId", getBankId())
            .append("alipayId", getAlipayId())
            .append("weixinId", getWeixinId())
            .append("replyContent", getReplyContent())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
