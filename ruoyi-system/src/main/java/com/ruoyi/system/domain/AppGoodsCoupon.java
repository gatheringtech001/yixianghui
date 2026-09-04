package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品优惠券对象 app_goods_coupon
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsCoupon extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 优惠券id */
    private Long couponId;

    /** 发布人 */
    @Excel(name = "发布人")
    private Long userId;

    /** 限定分类 */
    @Excel(name = "限定分类")
    private Long categoryId;

    /** 限定商品 */
    @Excel(name = "限定商品")
    private Long goodsId;

    /** 优惠券名称 */
    @Excel(name = "优惠券名称")
    private String couponName;

    /** 优惠券说明 */
    @Excel(name = "优惠券说明")
    private String couponContent;

    /** 优惠券类型 */
    @Excel(name = "优惠券类型")
    private String couponType;

    /** 满多少可用 */
    @Excel(name = "满多少可用")
    private BigDecimal minPrice;

    /** 折扣方式 */
    @Excel(name = "折扣方式")
    private String discountType;

    /** 折扣金额（discount_type=1表示金额，=2表示百分比） */
    @Excel(name = "折扣金额", readConverterExp = "d=iscount_type=1表示金额，=2表示百分比")
    private BigDecimal discountPrice;

    /** 每单可用张数 */
    @Excel(name = "每单可用张数")
    private Integer countPerOrder;

    /** 发放总量 */
    @Excel(name = "发放总量")
    private Long couponTotal;

    /** 每人可领数量 */
    @Excel(name = "每人可领数量")
    private Integer countPerUser;

    /** 已领取数量 */
    @Excel(name = "已领取数量")
    private Long couponGotCount;

    /** 已使用数量 */
    @Excel(name = "已使用数量")
    private Long couponUsedCount;

    /** 可用开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "可用开始时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date enableStartTime;

    /** 可用结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "可用结束时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date enableEndTime;

    /** 领取方式 */
    @Excel(name = "领取方式")
    private String getMethod;

    /** 专属渠道码 */
    private String channelCode;

    /** 来源小程序AppID，为空时不限制来源 */
    private String sourceAppId;

    /** 进场弹窗标题 */
    private String popupTitle;

    private Long visitCount;
    private Long orderCount;

    /** 优惠券状态 */
    @Excel(name = "优惠券状态")
    private String status;

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public String getSourceAppId() { return sourceAppId; }
    public void setSourceAppId(String sourceAppId) { this.sourceAppId = sourceAppId; }
    public String getPopupTitle() { return popupTitle; }
    public void setPopupTitle(String popupTitle) { this.popupTitle = popupTitle; }
    public Long getVisitCount() { return visitCount; }
    public void setVisitCount(Long visitCount) { this.visitCount = visitCount; }
    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public void setCouponId(Long couponId) 
    {
        this.couponId = couponId;
    }

    public Long getCouponId() 
    {
        return couponId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setCouponName(String couponName) 
    {
        this.couponName = couponName;
    }

    public String getCouponName() 
    {
        return couponName;
    }

    public void setCouponContent(String couponContent) 
    {
        this.couponContent = couponContent;
    }

    public String getCouponContent() 
    {
        return couponContent;
    }

    public void setCouponType(String couponType) 
    {
        this.couponType = couponType;
    }

    public String getCouponType() 
    {
        return couponType;
    }

    public void setMinPrice(BigDecimal minPrice) 
    {
        this.minPrice = minPrice;
    }

    public BigDecimal getMinPrice() 
    {
        return minPrice;
    }

    public void setDiscountType(String discountType) 
    {
        this.discountType = discountType;
    }

    public String getDiscountType() 
    {
        return discountType;
    }

    public void setDiscountPrice(BigDecimal discountPrice) 
    {
        this.discountPrice = discountPrice;
    }

    public BigDecimal getDiscountPrice() 
    {
        return discountPrice;
    }

    public void setCountPerOrder(Integer countPerOrder) 
    {
        this.countPerOrder = countPerOrder;
    }

    public Integer getCountPerOrder() 
    {
        return countPerOrder;
    }

    public void setCouponTotal(Long couponTotal) 
    {
        this.couponTotal = couponTotal;
    }

    public Long getCouponTotal() 
    {
        return couponTotal;
    }

    public void setCountPerUser(Integer countPerUser) 
    {
        this.countPerUser = countPerUser;
    }

    public Integer getCountPerUser() 
    {
        return countPerUser;
    }

    public void setCouponGotCount(Long couponGotCount) 
    {
        this.couponGotCount = couponGotCount;
    }

    public Long getCouponGotCount() 
    {
        return couponGotCount;
    }

    public void setCouponUsedCount(Long couponUsedCount) 
    {
        this.couponUsedCount = couponUsedCount;
    }

    public Long getCouponUsedCount() 
    {
        return couponUsedCount;
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

    public void setGetMethod(String getMethod) 
    {
        this.getMethod = getMethod;
    }

    public String getGetMethod() 
    {
        return getMethod;
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
            .append("couponId", getCouponId())
            .append("userId", getUserId())
            .append("categoryId", getCategoryId())
            .append("goodsId", getGoodsId())
            .append("couponName", getCouponName())
            .append("couponContent", getCouponContent())
            .append("couponType", getCouponType())
            .append("minPrice", getMinPrice())
            .append("discountType", getDiscountType())
            .append("discountPrice", getDiscountPrice())
            .append("countPerOrder", getCountPerOrder())
            .append("couponTotal", getCouponTotal())
            .append("countPerUser", getCountPerUser())
            .append("couponGotCount", getCouponGotCount())
            .append("couponUsedCount", getCouponUsedCount())
            .append("enableStartTime", getEnableStartTime())
            .append("enableEndTime", getEnableEndTime())
            .append("getMethod", getGetMethod())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
