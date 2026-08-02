package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品收藏对象 app_goods_collect
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsCollect extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 收藏id */
    private Long collectId;

    /** 所属用户 */
    @Excel(name = "所属用户")
    private Long userId;

    /** 收藏类型 goods/activity */
    @Excel(name = "收藏类型")
    private String collectType;

    /** 商品id */
    @Excel(name = "商品id")
    private Long goodsId;

    /** 活动id */
    @Excel(name = "活动id")
    private Long activityId;

    /** 收藏状态 */
    @Excel(name = "收藏状态")
    private String status;

    private String userName;

    private String goodsName;

    private transient AppGoods goodsInfo;

    private transient AppActivity activityInfo;

    public void setCollectId(Long collectId) 
    {
        this.collectId = collectId;
    }

    public Long getCollectId() 
    {
        return collectId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public String getCollectType()
    {
        return collectType;
    }

    public void setCollectType(String collectType)
    {
        this.collectType = collectType;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public Long getActivityId()
    {
        return activityId;
    }

    public void setActivityId(Long activityId)
    {
        this.activityId = activityId;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public AppGoods getGoodsInfo() {
        return goodsInfo;
    }

    public void setGoodsInfo(AppGoods goodsInfo) {
        this.goodsInfo = goodsInfo;
    }

    public AppActivity getActivityInfo() {
        return activityInfo;
    }

    public void setActivityInfo(AppActivity activityInfo) {
        this.activityInfo = activityInfo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("collectId", getCollectId())
            .append("userId", getUserId())
            .append("collectType", getCollectType())
            .append("goodsId", getGoodsId())
            .append("activityId", getActivityId())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .append("goodsInfo", getGoodsInfo())
            .append("activityInfo", getActivityInfo())
            .toString();
    }
}
