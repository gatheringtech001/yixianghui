package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单详细对象 app_goods_order_detail
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsOrderDetail extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 详单id */
    private Long detailId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 所属订单 */
    @Excel(name = "所属订单")
    private Long orderId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long goodsId;

    /** 商品数量 */
    @Excel(name = "商品数量")
    private Long goodsCount;

    /** 商品小计金额 */
    @Excel(name = "商品小计金额")
    private BigDecimal goodsMoney;

    /** 折扣金额 */
    @Excel(name = "折扣金额")
    private BigDecimal discountMoney;

    /** 是否sku商品 */
    @Excel(name = "是否sku商品")
    private Integer isSku;

    /** sku数据id */
    @Excel(name = "sku数据id")
    private Long skuDataId;

    /** sku数据描述 */
    @Excel(name = "sku数据描述")
    private String skuDataValues;

    /** 退货id */
    @Excel(name = "退货id")
    private Long goodsBackId;

    /** 退款金额 */
    @Excel(name = "退款金额")
    private BigDecimal refundMoney;

    /** 是否已评论 */
    @Excel(name = "是否已评论")
    private Integer isComment;

    /** 详单状态 */
    @Excel(name = "详单状态")
    private String status;

    @Excel(name = "预定开始日期")
    private Date orderStartDate;

    @Excel(name = "预定结束日期")
    private Date orderEndDate;

    private Long skuId;
    private Long selfSkuId;
    private Integer skuSeqNo;

    private Integer selfGoodsCount;

    private Integer interCount;

    public void setDetailId(Long detailId) 
    {
        this.detailId = detailId;
    }

    public Long getDetailId() 
    {
        return detailId;
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

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setGoodsCount(Long goodsCount) 
    {
        this.goodsCount = goodsCount;
    }

    public Long getGoodsCount() 
    {
        return goodsCount;
    }

    public void setGoodsMoney(BigDecimal goodsMoney) 
    {
        this.goodsMoney = goodsMoney;
    }

    public BigDecimal getGoodsMoney() 
    {
        return goodsMoney;
    }

    public void setDiscountMoney(BigDecimal discountMoney) 
    {
        this.discountMoney = discountMoney;
    }

    public BigDecimal getDiscountMoney() 
    {
        return discountMoney;
    }

    public void setIsSku(Integer isSku) 
    {
        this.isSku = isSku;
    }

    public Integer getIsSku() 
    {
        return isSku;
    }

    public void setSkuDataId(Long skuDataId) 
    {
        this.skuDataId = skuDataId;
    }

    public Long getSkuDataId() 
    {
        return skuDataId;
    }

    public void setSkuDataValues(String skuDataValues) 
    {
        this.skuDataValues = skuDataValues;
    }

    public String getSkuDataValues() 
    {
        return skuDataValues;
    }

    public void setGoodsBackId(Long goodsBackId) 
    {
        this.goodsBackId = goodsBackId;
    }

    public Long getGoodsBackId() 
    {
        return goodsBackId;
    }

    public void setRefundMoney(BigDecimal refundMoney) 
    {
        this.refundMoney = refundMoney;
    }

    public BigDecimal getRefundMoney() 
    {
        return refundMoney;
    }

    public void setIsComment(Integer isComment) 
    {
        this.isComment = isComment;
    }

    public Integer getIsComment() 
    {
        return isComment;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public Date getOrderStartDate() {
        return orderStartDate;
    }

    public void setOrderStartDate(Date orderStartDate) {
        this.orderStartDate = orderStartDate;
    }

    public Date getOrderEndDate() {
        return orderEndDate;
    }

    public void setOrderEndDate(Date orderEndDate) {
        this.orderEndDate = orderEndDate;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSelfSkuId() {
        return selfSkuId;
    }

    public void setSelfSkuId(Long selfSkuId) {
        this.selfSkuId = selfSkuId;
    }

    public Integer getSkuSeqNo() {
        return skuSeqNo;
    }

    public void setSkuSeqNo(Integer skuSeqNo) {
        this.skuSeqNo = skuSeqNo;
    }

    public Integer getSelfGoodsCount() {
        return selfGoodsCount;
    }

    public void setSelfGoodsCount(Integer selfGoodsCount) {
        this.selfGoodsCount = selfGoodsCount;
    }

    public Integer getInterCount() {
        return interCount;
    }

    public void setInterCount(Integer interCount) {
        this.interCount = interCount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("detailId", getDetailId())
            .append("userId", getUserId())
            .append("orderId", getOrderId())
            .append("goodsId", getGoodsId())
            .append("goodsCount", getGoodsCount())
            .append("goodsMoney", getGoodsMoney())
            .append("discountMoney", getDiscountMoney())
            .append("isSku", getIsSku())
            .append("skuDataId", getSkuDataId())
            .append("skuDataValues", getSkuDataValues())
            .append("goodsBackId", getGoodsBackId())
            .append("refundMoney", getRefundMoney())
            .append("remark", getRemark())
            .append("isComment", getIsComment())
            .append("status", getStatus())
            .append("orderStartDate", getOrderStartDate())
            .append("orderEndDate", getOrderEndDate())
            .toString();
    }
}
