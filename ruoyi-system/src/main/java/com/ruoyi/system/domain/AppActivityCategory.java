package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.TreeEntity;

/**
 * 活动分类对象 app_activity_category
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppActivityCategory extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 分类id */
    private Long categoryId;

    /** 祖级id集合 */
    @Excel(name = "祖级id集合")
    private String parentIds;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private String categoryName;

    /** 分类图标 */
    @Excel(name = "分类图标")
    private String categoryIcon;

    /** 分类状态 */
    @Excel(name = "分类状态")
    private String status;

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setParentIds(String parentIds) 
    {
        this.parentIds = parentIds;
    }

    public String getParentIds() 
    {
        return parentIds;
    }

    public void setCategoryName(String categoryName) 
    {
        this.categoryName = categoryName;
    }

    public String getCategoryName() 
    {
        return categoryName;
    }

    public void setCategoryIcon(String categoryIcon) 
    {
        this.categoryIcon = categoryIcon;
    }

    public String getCategoryIcon() 
    {
        return categoryIcon;
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
            .append("categoryId", getCategoryId())
            .append("parentId", getParentId())
            .append("parentIds", getParentIds())
            .append("categoryName", getCategoryName())
            .append("categoryIcon", getCategoryIcon())
            .append("remark", getRemark())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .toString();
    }
}
