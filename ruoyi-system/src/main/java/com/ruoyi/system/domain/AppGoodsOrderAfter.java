package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.List;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.entity.SysDictData;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单商品售后对象 app_goods_order_after
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsOrderAfter extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 退换id */
    private Long afterId;

    /** 售后类型 */
    @Excel(name = "售后类型")
    private String afterType;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 所属订单 */
    @Excel(name = "所属订单")
    private Long orderId;

    /** 退款订单编号 */
    @Excel(name = "退款订单编号")
    private String outOrderNo;

    /** 商品小计金额 */
    @Excel(name = "商品小计金额")
    private BigDecimal orderMoney;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long goodsId;

    /** 商品数量 */
    @Excel(name = "商品数量")
    private Long goodsCount;

    /** 商品售价 */
    @Excel(name = "商品售价")
    private BigDecimal goodsMoney;

    /** 快递金额 */
    @Excel(name = "快递金额")
    private BigDecimal expressMoney;

    /** 退款金额 */
    @Excel(name = "退款金额")
    private BigDecimal refundMoney;

    /** 快递名称 */
    @Excel(name = "快递名称")
    private String backExpressName;

    /** 快递单号 */
    @Excel(name = "快递单号")
    private String backExpressNo;

    /** 重发快递名称 */
    @Excel(name = "重发快递名称")
    private String resendExpressName;

    /** 重发快递单号 */
    @Excel(name = "重发快递单号")
    private String resendExpressNo;

    /** 售后状态 */
    @Excel(name = "售后状态")
    private String status;
    @Excel(name = "申请原因")
    private Long refundReason; // 关联数字字典
    @Excel(name = "申请原因详细描述")
    private String reasonDescription;//申请原因详细描述
    @Excel(name = "申请退款金额")
    private BigDecimal appRefundMoney;

    private String goodsName;

    private List<JSONObject> fileList;


    public void setAfterId(Long afterId) 
    {
        this.afterId = afterId;
    }

    public Long getAfterId() 
    {
        return afterId;
    }

    public void setAfterType(String afterType) 
    {
        this.afterType = afterType;
    }

    public String getAfterType() 
    {
        return afterType;
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

    public void setOutOrderNo(String outOrderNo) 
    {
        this.outOrderNo = outOrderNo;
    }

    public String getOutOrderNo() 
    {
        return outOrderNo;
    }

    public void setOrderMoney(BigDecimal orderMoney) 
    {
        this.orderMoney = orderMoney;
    }

    public BigDecimal getOrderMoney() 
    {
        return orderMoney;
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

    public void setExpressMoney(BigDecimal expressMoney) 
    {
        this.expressMoney = expressMoney;
    }

    public BigDecimal getExpressMoney() 
    {
        return expressMoney;
    }

    public void setRefundMoney(BigDecimal refundMoney) 
    {
        this.refundMoney = refundMoney;
    }

    public BigDecimal getRefundMoney() 
    {
        return refundMoney;
    }

    public void setBackExpressName(String backExpressName) 
    {
        this.backExpressName = backExpressName;
    }

    public String getBackExpressName() 
    {
        return backExpressName;
    }

    public void setBackExpressNo(String backExpressNo) 
    {
        this.backExpressNo = backExpressNo;
    }

    public String getBackExpressNo() 
    {
        return backExpressNo;
    }

    public void setResendExpressName(String resendExpressName) 
    {
        this.resendExpressName = resendExpressName;
    }

    public String getResendExpressName() 
    {
        return resendExpressName;
    }

    public void setResendExpressNo(String resendExpressNo) 
    {
        this.resendExpressNo = resendExpressNo;
    }

    public String getResendExpressNo() 
    {
        return resendExpressNo;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setRefundReason(Long refundReason)
    {
        this.refundReason = refundReason;
    }

    public Long getRefundReason()
    {
        return refundReason;
    }

    public void setReasonDescription(String reasonDescription)
    {
        this.reasonDescription = reasonDescription;
    }

    public String getReasonDescription()
    {
        return reasonDescription;
    }

    public void setAppRefundMoney(BigDecimal appRefundMoney)
    {
        this.appRefundMoney = appRefundMoney;
    }


    public BigDecimal getAppRefundMoney()
    {
        return appRefundMoney;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public List<JSONObject> getFileList() {
        return fileList;
    }

    public void setFileList(List<JSONObject> fileList) {
        this.fileList = fileList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("afterId", getAfterId())
            .append("afterType", getAfterType())
            .append("userId", getUserId())
            .append("orderId", getOrderId())
            .append("outOrderNo", getOutOrderNo())
            .append("orderMoney", getOrderMoney())
            .append("goodsId", getGoodsId())
            .append("goodsCount", getGoodsCount())
            .append("goodsMoney", getGoodsMoney())
            .append("expressMoney", getExpressMoney())
            .append("refundMoney", getRefundMoney())
            .append("backExpressName", getBackExpressName())
            .append("backExpressNo", getBackExpressNo())
            .append("resendExpressName", getResendExpressName())
            .append("resendExpressNo", getResendExpressNo())
            .append("remark", getRemark())
            .append("status", getStatus())
            .append("refundReason", getRefundReason())
            .append("reasonDescription", getReasonDescription())
            .append("appRefundMoney", getAppRefundMoney())
            .toString();
    }
}
