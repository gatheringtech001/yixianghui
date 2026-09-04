package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 优惠券领取记录对象 app_goods_coupon_got
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsCouponGot extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 领取id */
    private Long gotId;

    /** 优惠券id */
    @Excel(name = "优惠券id")
    private Long couponId;

    /** 领取用户 */
    @Excel(name = "领取用户")
    private Long userId;

    /** 使用的订单 */
    @Excel(name = "使用的订单")
    private Long orderId;

    /** 折扣金额 */
    @Excel(name = "折扣金额")
    private BigDecimal discountPrice;

    /** 领取方式 */
    @Excel(name = "领取方式")
    private String getMethod;

    /** 领取渠道 */
    private String channelCode;

    /** 是否已使用 */
    @Excel(name = "是否已使用")
    private Integer isUsed;

    /** 券状态 */
    @Excel(name = "券状态")
    private String status;

    private transient AppGoodsCoupon couponInfo;

    public void setGotId(Long gotId) 
    {
        this.gotId = gotId;
    }

    public Long getGotId() 
    {
        return gotId;
    }

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

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setDiscountPrice(BigDecimal discountPrice) 
    {
        this.discountPrice = discountPrice;
    }

    public BigDecimal getDiscountPrice() 
    {
        return discountPrice;
    }

    public void setGetMethod(String getMethod) 
    {
        this.getMethod = getMethod;
    }

    public String getGetMethod() 
    {
        return getMethod;
    }

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }

    public void setIsUsed(Integer isUsed) 
    {
        this.isUsed = isUsed;
    }

    public Integer getIsUsed() 
    {
        return isUsed;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public AppGoodsCoupon getCouponInfo() {
        return couponInfo;
    }

    public void setCouponInfo(AppGoodsCoupon couponInfo) {
        this.couponInfo = couponInfo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("gotId", getGotId())
            .append("couponId", getCouponId())
            .append("userId", getUserId())
            .append("orderId", getOrderId())
            .append("discountPrice", getDiscountPrice())
            .append("getMethod", getGetMethod())
            .append("isUsed", getIsUsed())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
                .append("couponInfo", getCouponInfo())
            .toString();
    }
}
