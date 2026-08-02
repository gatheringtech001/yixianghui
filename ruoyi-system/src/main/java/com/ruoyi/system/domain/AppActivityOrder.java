package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 活动预约对象 app_activity_order
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppActivityOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 预约id */
    private Long orderId;

    /** 所属用户 */
    @Excel(name = "所属用户")
    private Long userId;

    /** 活动id */
    @Excel(name = "活动id")
    private Long activityId;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 报名人姓名 */
    @Excel(name = "报名人姓名")
    private String signName;

    /** 报名人电话 */
    @Excel(name = "报名人电话")
    private String signMobile;

    /** 预约人数 */
    @Excel(name = "预约人数")
    private Integer signCount;

    /** 应付金额 */
    @Excel(name = "应付金额")
    private BigDecimal moneyPayable;

    /** 支付状态 0待支付 1已支付 */
    @Excel(name = "支付状态")
    private String payStatus;

    /** 实付金额 */
    @Excel(name = "实付金额")
    private BigDecimal payMoney;

    /** 支付方式 */
    @Excel(name = "支付方式")
    private String payType;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "支付时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 排序顺序 */
    @Excel(name = "排序顺序")
    private Integer orderNum;

    /** 报名状态 */
    @Excel(name = "报名状态")
    private String status;

    /** 活动信息 */
    private transient AppActivity activityInfo;

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

    public void setActivityId(Long activityId) 
    {
        this.activityId = activityId;
    }

    public Long getActivityId() 
    {
        return activityId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setSignName(String signName) 
    {
        this.signName = signName;
    }

    public String getSignName() 
    {
        return signName;
    }

    public void setSignMobile(String signMobile) 
    {
        this.signMobile = signMobile;
    }

    public String getSignMobile() 
    {
        return signMobile;
    }

    public void setSignCount(Integer signCount) 
    {
        this.signCount = signCount;
    }

    public Integer getSignCount() 
    {
        return signCount;
    }

    public BigDecimal getMoneyPayable() {
        return moneyPayable;
    }

    public void setMoneyPayable(BigDecimal moneyPayable) {
        this.moneyPayable = moneyPayable;
    }

    public String getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(String payStatus) {
        this.payStatus = payStatus;
    }

    public BigDecimal getPayMoney() {
        return payMoney;
    }

    public void setPayMoney(BigDecimal payMoney) {
        this.payMoney = payMoney;
    }

    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

    public Date getPayTime() {
        return payTime;
    }

    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }

    public void setOrderNum(Integer orderNum) 
    {
        this.orderNum = orderNum;
    }

    public Integer getOrderNum() 
    {
        return orderNum;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
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
            .append("orderId", getOrderId())
            .append("userId", getUserId())
            .append("activityId", getActivityId())
            .append("orderNo", getOrderNo())
            .append("signName", getSignName())
            .append("signMobile", getSignMobile())
            .append("signCount", getSignCount())
            .append("moneyPayable", getMoneyPayable())
            .append("payStatus", getPayStatus())
            .append("payMoney", getPayMoney())
            .append("payType", getPayType())
            .append("payTime", getPayTime())
            .append("remark", getRemark())
            .append("orderNum", getOrderNum())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
                .append("activityInfo", getActivityInfo())
            .toString();
    }
}
