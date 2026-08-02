package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 图文内容对象 app_article
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppArticle extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 内容ID */
    private Long articleId;

    /** 所属分类 */
    @Excel(name = "所属分类")
    private Long categoryId;

    /** 祖级分类 */
    @Excel(name = "祖级分类")
    private String categoryIds;

    /** 内容标题 */
    @Excel(name = "内容标题")
    private String articleName;

    /** 内容作者 */
    @Excel(name = "内容作者")
    private String author;

    /** 封面图片 */
    @Excel(name = "封面图片")
    private String articleCover;

    /** 内容简介 */
    @Excel(name = "内容简介")
    private String description;

    /** 内容标签 */
    @Excel(name = "内容标签")
    private String tags;

    /** 是否置顶 */
    @Excel(name = "是否置顶")
    private Integer isTop;

    /** 是否热门 */
    @Excel(name = "是否热门")
    private Integer isHot;

    /** 详细内容 */
    @Excel(name = "详细内容")
    private String content;

    /** 阅读次数 */
    @Excel(name = "阅读次数")
    private Long viewCount;

    /** 内容状态 */
    @Excel(name = "内容状态")
    private String status;

    public void setArticleId(Long articleId) 
    {
        this.articleId = articleId;
    }

    public Long getArticleId() 
    {
        return articleId;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setCategoryIds(String categoryIds) 
    {
        this.categoryIds = categoryIds;
    }

    public String getCategoryIds() 
    {
        return categoryIds;
    }

    public void setArticleName(String articleName) 
    {
        this.articleName = articleName;
    }

    public String getArticleName() 
    {
        return articleName;
    }

    public void setAuthor(String author) 
    {
        this.author = author;
    }

    public String getAuthor() 
    {
        return author;
    }

    public void setArticleCover(String articleCover) 
    {
        this.articleCover = articleCover;
    }

    public String getArticleCover() 
    {
        return articleCover;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setTags(String tags) 
    {
        this.tags = tags;
    }

    public String getTags() 
    {
        return tags;
    }

    public void setIsTop(Integer isTop) 
    {
        this.isTop = isTop;
    }

    public Integer getIsTop() 
    {
        return isTop;
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
            .append("articleId", getArticleId())
            .append("categoryId", getCategoryId())
            .append("categoryIds", getCategoryIds())
            .append("articleName", getArticleName())
            .append("author", getAuthor())
            .append("articleCover", getArticleCover())
            .append("description", getDescription())
            .append("tags", getTags())
            .append("isTop", getIsTop())
            .append("isHot", getIsHot())
            .append("content", getContent())
            .append("viewCount", getViewCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
