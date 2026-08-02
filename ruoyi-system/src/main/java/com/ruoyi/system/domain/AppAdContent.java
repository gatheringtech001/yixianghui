package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 广告内容对象 app_ad_content
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppAdContent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 广告内容id */
    private Long contentId;

    /** 所属广告位 */
    @Excel(name = "所属广告位")
    private Long positionId;

    /** 广告标题 */
    @Excel(name = "广告标题")
    private String adName;

    /** 广告介绍 */
    @Excel(name = "广告介绍")
    private String description;

    /** 广告图片 */
    @Excel(name = "广告图片")
    private String adImage;

    /** 富文本 */
    @Excel(name = "富文本")
    private String adContent;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date endTime;

    /** 广告链接 */
    @Excel(name = "广告链接")
    private String linkUrl;

    /** 排序 */
    @Excel(name = "排序")
    private Integer orderNum;

    /** 广告状态 */
    @Excel(name = "广告状态")
    private String status;

    public void setContentId(Long contentId) 
    {
        this.contentId = contentId;
    }

    public Long getContentId() 
    {
        return contentId;
    }

    public void setPositionId(Long positionId) 
    {
        this.positionId = positionId;
    }

    public Long getPositionId() 
    {
        return positionId;
    }

    public void setAdName(String adName) 
    {
        this.adName = adName;
    }

    public String getAdName() 
    {
        return adName;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setAdImage(String adImage) 
    {
        this.adImage = adImage;
    }

    public String getAdImage() 
    {
        return adImage;
    }

    public void setAdContent(String adContent) 
    {
        this.adContent = adContent;
    }

    public String getAdContent() 
    {
        return adContent;
    }

    public void setStartTime(Date startTime) 
    {
        this.startTime = startTime;
    }

    public Date getStartTime() 
    {
        return startTime;
    }

    public void setEndTime(Date endTime) 
    {
        this.endTime = endTime;
    }

    public Date getEndTime() 
    {
        return endTime;
    }

    public void setLinkUrl(String linkUrl) 
    {
        this.linkUrl = linkUrl;
    }

    public String getLinkUrl() 
    {
        return linkUrl;
    }

    public void setOrderNum(Integer orderNum) 
    {
        this.orderNum = orderNum;
    }

    public Integer getOrderNum() 
    {
        return orderNum;
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
            .append("contentId", getContentId())
            .append("positionId", getPositionId())
            .append("adName", getAdName())
            .append("description", getDescription())
            .append("adImage", getAdImage())
            .append("adContent", getAdContent())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("linkUrl", getLinkUrl())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .toString();
    }
}
