package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 钱包记录对象 app_user_money_log
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserMoneyLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录id */
    private Long logId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 金额 */
    @Excel(name = "金额")
    private BigDecimal money;

    /** 余额 */
    @Excel(name = "余额")
    private BigDecimal balance;

    /** 交易类型 */
    @Excel(name = "交易类型")
    private Long tradeType;

    /** 交易标题 */
    @Excel(name = "交易标题")
    private String tradeTitle;

    /** 交易说明 */
    @Excel(name = "交易说明")
    private String tradeDetail;

    /** 交易数据 */
    @Excel(name = "交易数据")
    private String tradeData;

    /** 支付日志id */
    @Excel(name = "支付日志id")
    private Long payLogId;

    /** 退款日志id */
    @Excel(name = "退款日志id")
    private Long payRefundLogId;

    /** 记录状态 */
    @Excel(name = "记录状态")
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

    public void setMoney(BigDecimal money) 
    {
        this.money = money;
    }

    public BigDecimal getMoney() 
    {
        return money;
    }

    public void setBalance(BigDecimal balance) 
    {
        this.balance = balance;
    }

    public BigDecimal getBalance() 
    {
        return balance;
    }

    public void setTradeType(Long tradeType) 
    {
        this.tradeType = tradeType;
    }

    public Long getTradeType() 
    {
        return tradeType;
    }

    public void setTradeTitle(String tradeTitle) 
    {
        this.tradeTitle = tradeTitle;
    }

    public String getTradeTitle() 
    {
        return tradeTitle;
    }

    public void setTradeDetail(String tradeDetail) 
    {
        this.tradeDetail = tradeDetail;
    }

    public String getTradeDetail() 
    {
        return tradeDetail;
    }

    public void setTradeData(String tradeData) 
    {
        this.tradeData = tradeData;
    }

    public String getTradeData() 
    {
        return tradeData;
    }

    public void setPayLogId(Long payLogId) 
    {
        this.payLogId = payLogId;
    }

    public Long getPayLogId() 
    {
        return payLogId;
    }

    public void setPayRefundLogId(Long payRefundLogId) 
    {
        this.payRefundLogId = payRefundLogId;
    }

    public Long getPayRefundLogId() 
    {
        return payRefundLogId;
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
            .append("money", getMoney())
            .append("balance", getBalance())
            .append("tradeType", getTradeType())
            .append("tradeTitle", getTradeTitle())
            .append("tradeDetail", getTradeDetail())
            .append("tradeData", getTradeData())
            .append("payLogId", getPayLogId())
            .append("payRefundLogId", getPayRefundLogId())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
