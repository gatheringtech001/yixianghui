package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 单页文章对象 app_single_page
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppSinglePage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 页面id */
    private Long pageId;

    /** 页面标题 */
    @Excel(name = "页面标题")
    private String pageName;

    /** 页面标识 */
    @Excel(name = "页面标识")
    private String pageKey;

    /** 封面图片 */
    @Excel(name = "封面图片")
    private String pageCover;

    /** 页面描述 */
    private String description;

    /** 页面关键字 */
    @Excel(name = "页面关键字")
    private String keywords;

    /** 是否热门 */
    @Excel(name = "是否热门")
    private Integer isHot;

    /** 页面内容 */
    private String content;

    /** 阅读次数 */
    @Excel(name = "阅读次数")
    private Long viewCount;

    /** 页面状态 */
    @Excel(name = "页面状态")
    private String status;

    public void setPageId(Long pageId) 
    {
        this.pageId = pageId;
    }

    public Long getPageId() 
    {
        return pageId;
    }

    public void setPageName(String pageName) 
    {
        this.pageName = pageName;
    }

    public String getPageName() 
    {
        return pageName;
    }

    public void setPageKey(String pageKey) 
    {
        this.pageKey = pageKey;
    }

    public String getPageKey() 
    {
        return pageKey;
    }

    public void setPageCover(String pageCover) 
    {
        this.pageCover = pageCover;
    }

    public String getPageCover() 
    {
        return pageCover;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setKeywords(String keywords) 
    {
        this.keywords = keywords;
    }

    public String getKeywords() 
    {
        return keywords;
    }

    public void setIsHot(Integer isHot) 
    {
        this.isHot = isHot;
    }

    public Integer getIsHot() 
    {
        return isHot;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setViewCount(Long viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Long getViewCount() 
    {
        return viewCount;
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
            .append("pageId", getPageId())
            .append("pageName", getPageName())
            .append("pageKey", getPageKey())
            .append("pageCover", getPageCover())
            .append("description", getDescription())
            .append("keywords", getKeywords())
            .append("isHot", getIsHot())
            .append("content", getContent())
            .append("viewCount", getViewCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
