package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户银行卡对象 app_user_bank
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserBank extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 银行卡id */
    private Long bankId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 类型 */
    @Excel(name = "类型")
    private String bankType;

    /** 用户银行标识 */
    @Excel(name = "用户银行标识")
    private String bankName;

    /** 用户银行名称 */
    @Excel(name = "用户银行名称")
    private String bankTitle;

    /** 用户银行分行 */
    @Excel(name = "用户银行分行")
    private String bankSubbranch;

    /** 用户银行户名 */
    @Excel(name = "用户银行户名")
    private String bankAccountName;

    /** 用户银行卡号 */
    @Excel(name = "用户银行卡号")
    private String bankAccountNum;

    /** 扩展数据 */
    @Excel(name = "扩展数据")
    private String extendData;

    /** 银行卡状态 */
    @Excel(name = "银行卡状态")
    private String status;

    public void setBankId(Long bankId) 
    {
        this.bankId = bankId;
    }

    public Long getBankId() 
    {
        return bankId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setBankType(String bankType) 
    {
        this.bankType = bankType;
    }

    public String getBankType() 
    {
        return bankType;
    }

    public void setBankName(String bankName) 
    {
        this.bankName = bankName;
    }

    public String getBankName() 
    {
        return bankName;
    }

    public void setBankTitle(String bankTitle) 
    {
        this.bankTitle = bankTitle;
    }

    public String getBankTitle() 
    {
        return bankTitle;
    }

    public void setBankSubbranch(String bankSubbranch) 
    {
        this.bankSubbranch = bankSubbranch;
    }

    public String getBankSubbranch() 
    {
        return bankSubbranch;
    }

    public void setBankAccountName(String bankAccountName) 
    {
        this.bankAccountName = bankAccountName;
    }

    public String getBankAccountName() 
    {
        return bankAccountName;
    }

    public void setBankAccountNum(String bankAccountNum) 
    {
        this.bankAccountNum = bankAccountNum;
    }

    public String getBankAccountNum() 
    {
        return bankAccountNum;
    }

    public void setExtendData(String extendData) 
    {
        this.extendData = extendData;
    }

    public String getExtendData() 
    {
        return extendData;
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
            .append("bankId", getBankId())
            .append("userId", getUserId())
            .append("bankType", getBankType())
            .append("bankName", getBankName())
            .append("bankTitle", getBankTitle())
            .append("bankSubbranch", getBankSubbranch())
            .append("bankAccountName", getBankAccountName())
            .append("bankAccountNum", getBankAccountNum())
            .append("extendData", getExtendData())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
