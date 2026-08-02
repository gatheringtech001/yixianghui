package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户会员卡对象 app_user_card
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserCard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户会员id */
    private Long recordId;

    /** 用户id */
    @Excel(name = "用户id")
    private Long userId;

    /** 卡id */
    @Excel(name = "卡id")
    private Long cardId;

    /** 激活类型 */
    @Excel(name = "激活类型")
    private String activeType;

    /** 有效期开始时间（type=1） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期开始时间", dateFormat = "yyyy-MM-dd")
    private Date enableStartTime;

    /** 有效期结束时间（type=1） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "有效期结束时间", dateFormat = "yyyy-MM-dd")
    private Date enableEndTime;

    /** 可用天数（type=2） */
    @Excel(name = "可用天数")
    private Long enableDayCount;

    /** 激活码 */
    @Excel(name = "激活码")
    private String activeCode;

    /** 激活时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "激活时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date activeTime;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    private transient AppCard cardInfo;

    public void setRecordId(Long recordId) 
    {
        this.recordId = recordId;
    }

    public Long getRecordId() 
    {
        return recordId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCardId(Long cardId) 
    {
        this.cardId = cardId;
    }

    public Long getCardId() 
    {
        return cardId;
    }

    public void setActiveType(String activeType) 
    {
        this.activeType = activeType;
    }

    public String getActiveType() 
    {
        return activeType;
    }

    public void setEnableStartTime(Date enableStartTime) 
    {
        this.enableStartTime = enableStartTime;
    }

    public Date getEnableStartTime() 
    {
        return enableStartTime;
    }

    public void setEnableEndTime(Date enableEndTime) 
    {
        this.enableEndTime = enableEndTime;
    }

    public Date getEnableEndTime() 
    {
        return enableEndTime;
    }

    public void setEnableDayCount(Long enableDayCount) 
    {
        this.enableDayCount = enableDayCount;
    }

    public Long getEnableDayCount() 
    {
        return enableDayCount;
    }

    public void setActiveCode(String activeCode) 
    {
        this.activeCode = activeCode;
    }

    public String getActiveCode() 
    {
        return activeCode;
    }

    public void setActiveTime(Date activeTime) 
    {
        this.activeTime = activeTime;
    }

    public Date getActiveTime() 
    {
        return activeTime;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public AppCard getCardInfo() {
        return cardInfo;
    }

    public void setCardInfo(AppCard cardInfo) {
        this.cardInfo = cardInfo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("recordId", getRecordId())
            .append("userId", getUserId())
            .append("cardId", getCardId())
            .append("activeType", getActiveType())
            .append("enableStartTime", getEnableStartTime())
            .append("enableEndTime", getEnableEndTime())
            .append("enableDayCount", getEnableDayCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("activeCode", getActiveCode())
            .append("activeTime", getActiveTime())
            .append("status", getStatus())
                .append("cardInfo", getCardInfo())
            .toString();
    }
}
