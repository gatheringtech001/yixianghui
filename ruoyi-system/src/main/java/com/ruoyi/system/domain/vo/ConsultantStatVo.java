package com.ruoyi.system.domain.vo;

import java.math.BigDecimal;

/**
 * 顾问中心统计数据
 */
public class ConsultantStatVo
{
    private BigDecimal withdrawnAmount = BigDecimal.ZERO;
    private BigDecimal pendingAmount = BigDecimal.ZERO;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private Long customerCount = 0L;
    private Long inviteCount = 0L;

    public BigDecimal getWithdrawnAmount()
    {
        return withdrawnAmount;
    }

    public void setWithdrawnAmount(BigDecimal withdrawnAmount)
    {
        this.withdrawnAmount = withdrawnAmount;
    }

    public BigDecimal getPendingAmount()
    {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount)
    {
        this.pendingAmount = pendingAmount;
    }

    public BigDecimal getTotalIncome()
    {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome)
    {
        this.totalIncome = totalIncome;
    }

    public Long getCustomerCount()
    {
        return customerCount;
    }

    public void setCustomerCount(Long customerCount)
    {
        this.customerCount = customerCount;
    }

    public Long getInviteCount()
    {
        return inviteCount;
    }

    public void setInviteCount(Long inviteCount)
    {
        this.inviteCount = inviteCount;
    }
}
