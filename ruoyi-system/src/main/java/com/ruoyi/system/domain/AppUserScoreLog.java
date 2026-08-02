package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 积分记录对象 app_user_score_log
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserScoreLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录id */
    private Long logId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 积分数量 */
    @Excel(name = "积分数量")
    private Long score;

    /** 积分余额 */
    @Excel(name = "积分余额")
    private Long balance;

    /** 变化类型 */
    @Excel(name = "变化类型")
    private Integer tradeType;

    /** 交易标题 */
    @Excel(name = "交易标题")
    private String tradeTitle;

    /** 交易说明 */
    @Excel(name = "交易说明")
    private String tradeDetail;

    /** 交易数据 */
    @Excel(name = "交易数据")
    private String tradeData;

    /** 业务类型 */
    @Excel(name = "业务类型")
    private String businessType;

    /** 业务id */
    @Excel(name = "业务id")
    private Long businessId;

    /** 是否有效 */
    @Excel(name = "是否有效")
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

    public void setScore(Long score) 
    {
        this.score = score;
    }

    public Long getScore() 
    {
        return score;
    }

    public void setBalance(Long balance) 
    {
        this.balance = balance;
    }

    public Long getBalance() 
    {
        return balance;
    }

    public void setTradeType(Integer tradeType) 
    {
        this.tradeType = tradeType;
    }

    public Integer getTradeType() 
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

    public void setBusinessType(String businessType) 
    {
        this.businessType = businessType;
    }

    public String getBusinessType() 
    {
        return businessType;
    }

    public void setBusinessId(Long businessId) 
    {
        this.businessId = businessId;
    }

    public Long getBusinessId() 
    {
        return businessId;
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
            .append("score", getScore())
            .append("balance", getBalance())
            .append("tradeType", getTradeType())
            .append("tradeTitle", getTradeTitle())
            .append("tradeDetail", getTradeDetail())
            .append("tradeData", getTradeData())
            .append("businessType", getBusinessType())
            .append("businessId", getBusinessId())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
