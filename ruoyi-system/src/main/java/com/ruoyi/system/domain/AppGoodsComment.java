package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品评价对象 app_goods_comment
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsComment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 评价id */
    private Long commentId;

    /** 所属用户 */
    @Excel(name = "所属用户")
    private Long userId;

    /** 所属商品 */
    @Excel(name = "所属商品")
    private Long goodsId;

    /** 所属订单 */
    @Excel(name = "所属订单")
    private Long orderId;

    /** 所属详单 */
    @Excel(name = "所属详单")
    private Long detailId;

    /** 评价标签 */
    @Excel(name = "评价标签")
    private String commentTags;

    /** 评价内容 */
    @Excel(name = "评价内容")
    private String commentContent;

    /** 评论图片 */
    @Excel(name = "评论图片")
    private String commentImages;

    /** 商品评分 */
    @Excel(name = "商品评分")
    private BigDecimal goodsStar;

    /** 物流评分 */
    @Excel(name = "物流评分")
    private BigDecimal expressStar;

    /** 服务评分 */
    @Excel(name = "服务评分")
    private BigDecimal waiterStar;

    /** 评价状态 */
    @Excel(name = "评价状态")
    private String status;

    private String userName;

    private String goodsName;

    private String appGoodsOrderNo;

    public void setCommentId(Long commentId) 
    {
        this.commentId = commentId;
    }

    public Long getCommentId() 
    {
        return commentId;
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

    public void setOrderId(Long orderId) 
    {
        this.orderId = orderId;
    }

    public Long getOrderId() 
    {
        return orderId;
    }

    public void setDetailId(Long detailId) 
    {
        this.detailId = detailId;
    }

    public Long getDetailId() 
    {
        return detailId;
    }

    public void setCommentTags(String commentTags) 
    {
        this.commentTags = commentTags;
    }

    public String getCommentTags() 
    {
        return commentTags;
    }

    public void setCommentContent(String commentContent) 
    {
        this.commentContent = commentContent;
    }

    public String getCommentContent() 
    {
        return commentContent;
    }

    public void setCommentImages(String commentImages) 
    {
        this.commentImages = commentImages;
    }

    public String getCommentImages() 
    {
        return commentImages;
    }

    public void setGoodsStar(BigDecimal goodsStar) 
    {
        this.goodsStar = goodsStar;
    }

    public BigDecimal getGoodsStar() 
    {
        return goodsStar;
    }

    public void setExpressStar(BigDecimal expressStar) 
    {
        this.expressStar = expressStar;
    }

    public BigDecimal getExpressStar() 
    {
        return expressStar;
    }

    public void setWaiterStar(BigDecimal waiterStar) 
    {
        this.waiterStar = waiterStar;
    }

    public BigDecimal getWaiterStar() 
    {
        return waiterStar;
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

    public String getAppGoodsOrderNo() {
        return appGoodsOrderNo;
    }

    public void setAppGoodsOrderNo(String appGoodsOrderNo) {
        this.appGoodsOrderNo = appGoodsOrderNo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("commentId", getCommentId())
            .append("userId", getUserId())
            .append("goodsId", getGoodsId())
            .append("orderId", getOrderId())
            .append("detailId", getDetailId())
            .append("commentTags", getCommentTags())
            .append("commentContent", getCommentContent())
            .append("commentImages", getCommentImages())
            .append("goodsStar", getGoodsStar())
            .append("expressStar", getExpressStar())
            .append("waiterStar", getWaiterStar())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
