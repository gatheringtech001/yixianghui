package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品订单对象 app_goods_order
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long goodsId;

    /** 所属分站 */
    @Excel(name = "所属分站Id")
    private Long deptId;

    /** 收货地址 */
    @Excel(name = "收货地址")
    private Long addressId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 商品合计金额 */
    @Excel(name = "商品合计金额")
    private BigDecimal moneyTotal;

    /** 折扣金额 */
    @Excel(name = "折扣金额")
    private BigDecimal moneyDiscount;

    /** 商品应付金额 */
    @Excel(name = "商品应付金额")
    private BigDecimal moneyPayable;

    /** 快递费 */
    @Excel(name = "快递费")
    private BigDecimal moneyExpress;

    /** 是否已支付 */
    @Excel(name = "是否已支付")
    private String payStatus;

    /** 支付金额 */
    @Excel(name = "支付金额")
    private BigDecimal payMoney;

    /** 支付方式 */
    @Excel(name = "支付方式")
    private String payType;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date payTime;

    /** 商品总数量 */
    @Excel(name = "商品总数量")
    private Long goodsCount;

    /** 使用优惠券的id集合，逗号分割 */
    @Excel(name = "使用优惠券的id集合，逗号分割")
    private String couponGotIds;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "发货时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date sendTime;

    /** 发货快递名称 */
    @Excel(name = "发货快递名称")
    private String sendExpressName;

    /** 发货快递标识 */
    @Excel(name = "发货快递标识")
    private String sendExpressSimple;

    /** 发货快递单号 */
    @Excel(name = "发货快递单号")
    private String sendExpressNo;

    /** 核销码 */
    @Excel(name = "核销码")
    private String checkNum;

    /** 是否已核销 */
    @Excel(name = "是否已核销")
    private String isChecked;

    /** 收货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "收货时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date receiveTime;

    /** 是否已经发放奖励 */
    @Excel(name = "是否已经发放奖励")
    private Integer isAward;

    /** 是否已评论 */
    @Excel(name = "是否已评论")
    private Integer isComment;

    /** 是否申请取消：0否，1审核中，2同意取消 */
    @Excel(name = "是否申请取消：0否，1审核中，2同意取消")
    private Integer isApplyCancel;

    /** 商户是否已结算 */
    @Excel(name = "商户是否已结算")
    private Integer isCash;

    /** 订单状态 */
    @Excel(name = "订单状态")
    private String status;

    /** 旅居履约状态：0待确认 1已确认 2已取消 3已入住 4已离店 5已结算 6退款中 7已退款 */
    @Excel(name = "旅居状态", readConverterExp = "0=待确认,1=已确认,2=已取消,3=已入住,4=已离店,5=已结算,6=退款中,7=已退款")
    private String travelStatus;

    /** 退款前旅居履约状态，用于退款失败或拒绝时恢复 */
    private String travelStatusBeforeRefund;

    @Excel(name = "所属分站")
    private String deptName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预定开始日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "预定结束日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date checkOutDate;

    @Excel(name = "联系人姓名")
    private String contactName;

    @Excel(name = "联系人电话")
    private String contactPhone;

    /** 多规格商品订单获取时使用 */
    private transient Long skuDataId;

    private Long skuId;
    private Long selfSkuId;
    private Integer skuSeqNo;

    private Integer selfGoodsCount;

    private Integer interCount;

    private Integer selComboIndex;

    private transient AppUserAddress addressInfo;

    private transient List<AppGoodsOrderDetail> orderDetailList;

    private transient List<AppGoodsOrderAfter> orderAfterList;

    private transient List<AppGoods> goodsList;

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setAddressId(Long addressId) 
    {
        this.addressId = addressId;
    }

    public Long getAddressId() 
    {
        return addressId;
    }

    public void setOrderNo(String orderNo) 
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo() 
    {
        return orderNo;
    }

    public void setMoneyTotal(BigDecimal moneyTotal) 
    {
        this.moneyTotal = moneyTotal;
    }

    public BigDecimal getMoneyTotal() 
    {
        return moneyTotal;
    }

    public void setMoneyDiscount(BigDecimal moneyDiscount) 
    {
        this.moneyDiscount = moneyDiscount;
    }

    public BigDecimal getMoneyDiscount() 
    {
        return moneyDiscount;
    }

    public void setMoneyPayable(BigDecimal moneyPayable) 
    {
        this.moneyPayable = moneyPayable;
    }

    public BigDecimal getMoneyPayable() 
    {
        return moneyPayable;
    }

    public void setMoneyExpress(BigDecimal moneyExpress) 
    {
        this.moneyExpress = moneyExpress;
    }

    public BigDecimal getMoneyExpress() 
    {
        return moneyExpress;
    }

    public void setPayStatus(String payStatus) 
    {
        this.payStatus = payStatus;
    }

    public String getPayStatus() 
    {
        return payStatus;
    }

    public void setPayMoney(BigDecimal payMoney) 
    {
        this.payMoney = payMoney;
    }

    public BigDecimal getPayMoney() 
    {
        return payMoney;
    }

    public void setPayType(String payType) 
    {
        this.payType = payType;
    }

    public String getPayType() 
    {
        return payType;
    }

    public void setPayTime(Date payTime) 
    {
        this.payTime = payTime;
    }

    public Date getPayTime() 
    {
        return payTime;
    }

    public void setGoodsCount(Long goodsCount) 
    {
        this.goodsCount = goodsCount;
    }

    public Long getGoodsCount() 
    {
        return goodsCount;
    }

    public void setCouponGotIds(String couponGotIds) 
    {
        this.couponGotIds = couponGotIds;
    }

    public String getCouponGotIds() 
    {
        return couponGotIds;
    }

    public void setSendTime(Date sendTime) 
    {
        this.sendTime = sendTime;
    }

    public Date getSendTime() 
    {
        return sendTime;
    }

    public void setSendExpressName(String sendExpressName) 
    {
        this.sendExpressName = sendExpressName;
    }

    public String getSendExpressName() 
    {
        return sendExpressName;
    }

    public void setSendExpressSimple(String sendExpressSimple) 
    {
        this.sendExpressSimple = sendExpressSimple;
    }

    public String getSendExpressSimple() 
    {
        return sendExpressSimple;
    }

    public void setSendExpressNo(String sendExpressNo) 
    {
        this.sendExpressNo = sendExpressNo;
    }

    public String getSendExpressNo() 
    {
        return sendExpressNo;
    }

    public void setCheckNum(String checkNum) 
    {
        this.checkNum = checkNum;
    }

    public String getCheckNum() 
    {
        return checkNum;
    }

    public void setIsChecked(String isChecked) 
    {
        this.isChecked = isChecked;
    }

    public String getIsChecked() 
    {
        return isChecked;
    }

    public void setReceiveTime(Date receiveTime) 
    {
        this.receiveTime = receiveTime;
    }

    public Date getReceiveTime() 
    {
        return receiveTime;
    }

    public void setIsAward(Integer isAward) 
    {
        this.isAward = isAward;
    }

    public Integer getIsAward() 
    {
        return isAward;
    }

    public void setIsComment(Integer isComment) 
    {
        this.isComment = isComment;
    }

    public Integer getIsComment() 
    {
        return isComment;
    }

    public void setIsApplyCancel(Integer isApplyCancel) 
    {
        this.isApplyCancel = isApplyCancel;
    }

    public Integer getIsApplyCancel() 
    {
        return isApplyCancel;
    }

    public void setIsCash(Integer isCash) 
    {
        this.isCash = isCash;
    }

    public Integer getIsCash() 
    {
        return isCash;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getTravelStatus()
    {
        return travelStatus;
    }

    public void setTravelStatus(String travelStatus)
    {
        this.travelStatus = travelStatus;
    }

    public String getTravelStatusBeforeRefund()
    {
        return travelStatusBeforeRefund;
    }

    public void setTravelStatusBeforeRefund(String travelStatusBeforeRefund)
    {
        this.travelStatusBeforeRefund = travelStatusBeforeRefund;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Date getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(Date checkInDate) {
        this.checkInDate = checkInDate;
    }

    public Date getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(Date checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public AppUserAddress getAddressInfo() {
        return addressInfo;
    }

    public void setAddressInfo(AppUserAddress addressInfo) {
        this.addressInfo = addressInfo;
    }

    public List<AppGoodsOrderDetail> getOrderDetailList() {
        return orderDetailList;
    }

    public void setOrderDetailList(List<AppGoodsOrderDetail> orderDetailList) {
        this.orderDetailList = orderDetailList;
    }

    public List<AppGoodsOrderAfter> getOrderAfterList() {
        return orderAfterList;
    }

    public void setOrderAfterList(List<AppGoodsOrderAfter> orderAfterList) {
        this.orderAfterList = orderAfterList;
    }

    public List<AppGoods> getGoodsList() {
        return goodsList;
    }

    public void setGoodsList(List<AppGoods> goodsList) {
        this.goodsList = goodsList;
    }

    public Long getSkuDataId() {
        return skuDataId;
    }

    public void setSkuDataId(Long skuDataId) {
        this.skuDataId = skuDataId;
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

    public Integer getSelComboIndex() {
        return selComboIndex;
    }

    public void setSelComboIndex(Integer selComboIndex) {
        this.selComboIndex = selComboIndex;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("userId", getUserId())
            .append("goodsId", getGoodsId())
            .append("deptId", getDeptId())
            .append("addressId", getAddressId())
            .append("orderNo", getOrderNo())
            .append("moneyTotal", getMoneyTotal())
            .append("moneyDiscount", getMoneyDiscount())
            .append("moneyPayable", getMoneyPayable())
            .append("moneyExpress", getMoneyExpress())
            .append("payStatus", getPayStatus())
            .append("payMoney", getPayMoney())
            .append("payType", getPayType())
            .append("payTime", getPayTime())
            .append("goodsCount", getGoodsCount())
            .append("couponGotIds", getCouponGotIds())
            .append("remark", getRemark())
            .append("sendTime", getSendTime())
            .append("sendExpressName", getSendExpressName())
            .append("sendExpressSimple", getSendExpressSimple())
            .append("sendExpressNo", getSendExpressNo())
            .append("checkNum", getCheckNum())
            .append("isChecked", getIsChecked())
            .append("receiveTime", getReceiveTime())
            .append("isAward", getIsAward())
            .append("isComment", getIsComment())
            .append("isApplyCancel", getIsApplyCancel())
            .append("isCash", getIsCash())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
                .append("skuDataId", getSkuDataId())
                .append("deptName", getDeptName())
                .append("contactName", getContactName())
                .append("contactPhone", getContactPhone())
                .append("addressInfo", getAddressInfo())
                .append("orderDetailInfo", getOrderDetailList())
                .append("orderAfterInfo", getOrderAfterList())
                .append("goodsList", getGoodsList())
            .toString();
    }
}
