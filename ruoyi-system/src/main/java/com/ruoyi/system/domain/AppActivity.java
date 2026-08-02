package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 活动对象 app_activity
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppActivity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 所属分类 */
    @Excel(name = "所属分类")
    private Long categoryId;

    /** 归属站点 */
    @Excel(name = "归属站点")
    private Long deptId;

    /** 祖级分类 */
    @Excel(name = "祖级分类")
    private String categoryIds;

    /** 活动标题 */
    @Excel(name = "活动标题")
    private String activityName;

    /** 活动地点 */
    @Excel(name = "活动地点")
    private String address;

    /** 封面图片 */
    @Excel(name = "封面图片")
    private String activityCover;

    /** 活动简介 */
    @Excel(name = "活动简介")
    private String description;

    /** 活动标签 */
    @Excel(name = "活动标签")
    private String tags;

    /** 是否置顶 */
    @Excel(name = "是否置顶")
    private Integer isTop;

    /** 是否热门 */
    @Excel(name = "是否热门")
    private Integer isHot;

    /** 活动内容 */
    @Excel(name = "活动内容")
    private String content;

    /** 阅读次数 */
    @Excel(name = "阅读次数")
    private Long viewCount;

    /** 活动状态 */
    @Excel(name = "活动状态")
    private String status;

    /** 已报名人数 */
    @Excel(name = "已报名人数")
    private String signCount;
    /** 报名人数限制 */
    @Excel(name = "报名人数限制")
    private String maxCount;
    /** 报名截止时间 */
    @Excel(name = "报名截止时间")
    private String signEndTime;
    /** 活动时间 */
    @Excel(name = "活动时间")
    private String activityTime;

    /** 活动结束时间（仅用于结束判断，不在小程序展示） */
    @Excel(name = "结束时间")
    private String activityEndTime;

    /**
     * 活动列表时间筛选（仅查询用，不落库）
     * active：活动未结束；ended：活动已结束（按 activityEndTime）
     */
    private transient String signFilter;

    /** 是否免费 1免费 0付费 */
    @Excel(name = "是否免费")
    private Integer isFree;

    /** 原价 */
    @Excel(name = "原价")
    private java.math.BigDecimal price;

    /** 实付价 */
    @Excel(name = "实付价")
    private java.math.BigDecimal vipPrice;

    /** 分类名称 */
    @Excel(name = "分类名称")
    private transient String categoryName;


    public void setActivityId(Long activityId) 
    {
        this.activityId = activityId;
    }

    public Long getActivityId() 
    {
        return activityId;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setCategoryIds(String categoryIds) 
    {
        this.categoryIds = categoryIds;
    }

    public String getCategoryIds() 
    {
        return categoryIds;
    }

    public void setActivityName(String activityName) 
    {
        this.activityName = activityName;
    }

    public String getActivityName() 
    {
        return activityName;
    }

    public void setAddress(String address) 
    {
        this.address = address;
    }

    public String getAddress() 
    {
        return address;
    }

    public void setActivityCover(String activityCover) 
    {
        this.activityCover = activityCover;
    }

    public String getActivityCover() 
    {
        return activityCover;
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

    public String getSignCount() {
        return signCount;
    }

    public void setSignCount(String signCount) {
        this.signCount = signCount;
    }

    public String getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(String maxCount) {
        this.maxCount = maxCount;
    }

    public String getSignEndTime() {
        return signEndTime;
    }

    public void setSignEndTime(String signEndTime) {
        this.signEndTime = signEndTime;
    }

    public String getSignFilter() {
        return signFilter;
    }

    public void setSignFilter(String signFilter) {
        this.signFilter = signFilter;
    }

    public String getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(String activityTime) {
        this.activityTime = activityTime;
    }

    public String getActivityEndTime() {
        return activityEndTime;
    }

    public void setActivityEndTime(String activityEndTime) {
        this.activityEndTime = activityEndTime;
    }

    public Integer getIsFree() {
        return isFree;
    }

    public void setIsFree(Integer isFree) {
        this.isFree = isFree;
    }

    public java.math.BigDecimal getPrice() {
        return price;
    }

    public void setPrice(java.math.BigDecimal price) {
        this.price = price;
    }

    public java.math.BigDecimal getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(java.math.BigDecimal vipPrice) {
        this.vipPrice = vipPrice;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("activityId", getActivityId())
            .append("categoryId", getCategoryId())
            .append("deptId", getDeptId())
            .append("categoryIds", getCategoryIds())
            .append("activityName", getActivityName())
            .append("address", getAddress())
            .append("activityCover", getActivityCover())
            .append("description", getDescription())
            .append("tags", getTags())
            .append("isTop", getIsTop())
            .append("isHot", getIsHot())
            .append("content", getContent())
            .append("viewCount", getViewCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
                .append("signCount", getSignCount())
                .append("maxCount", getMaxCount())
                .append("signEndTime", getSignEndTime())
                .append("activityTime", getActivityTime())
                .append("activityEndTime", getActivityEndTime())
                .append("isFree", getIsFree())
                .append("price", getPrice())
                .append("vipPrice", getVipPrice())
                .append("categoryName", getCategoryName())
            .toString();
    }
}
