package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.TreeEntity;

/**
 * 商品分类对象 app_goods_category
 * 
 * @author lankong
 * @date 2025-03-31
 */
public class AppGoodsCategory extends TreeEntity
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

    /** 是否热门 */
    @Excel(name = "是否热门")
    private String isHot;

    /** 分类链接类型 */
    @Excel(name = "分类链接类型")
    private String linkType;

    /** 分类链接ID */
    @Excel(name = "分类链接ID")
    private String linkId;

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

    public String getIsHot() {
        return isHot;
    }

    public void setIsHot(String isHot) {
        this.isHot = isHot;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getLinkId() {
        return linkId;
    }

    public void setLinkId(String linkId) {
        this.linkId = linkId;
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
                .append("isHot", getIsHot())
                .append("linkType", getLinkType())
                .append("linkId", getLinkId())
            .append("remark", getRemark())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .toString();
    }
}
