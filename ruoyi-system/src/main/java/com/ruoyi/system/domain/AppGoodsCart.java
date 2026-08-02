package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户购物车对象 app_goods_cart
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsCart extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 购物车id */
    private Long cartId;

    /** 所属用户 */
    @Excel(name = "所属用户")
    private Long userId;

    /** 商品id */
    @Excel(name = "商品id")
    private Long goodsId;

    /** 是否sku */
    @Excel(name = "是否sku")
    private Long isSku;

    /** 型号信息 */
    @Excel(name = "型号信息")
    private Long dataId;

    /** 型号组合名 */
    @Excel(name = "型号组合名")
    private String dataValues;

    /** 商品数量 */
    @Excel(name = "商品数量")
    private Integer goodsCount;

    /** 可用状态 */
    @Excel(name = "可用状态")
    private String status;

    private String userName;

    private String goodsName;

    private transient AppGoods goodsInfo;

    public void setCartId(Long cartId) 
    {
        this.cartId = cartId;
    }

    public Long getCartId() 
    {
        return cartId;
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

    public void setIsSku(Long isSku) 
    {
        this.isSku = isSku;
    }

    public Long getIsSku() 
    {
        return isSku;
    }

    public void setDataId(Long dataId) 
    {
        this.dataId = dataId;
    }

    public Long getDataId() 
    {
        return dataId;
    }

    public void setDataValues(String dataValues) 
    {
        this.dataValues = dataValues;
    }

    public String getDataValues() 
    {
        return dataValues;
    }

    public void setGoodsCount(Integer goodsCount) 
    {
        this.goodsCount = goodsCount;
    }

    public Integer getGoodsCount() 
    {
        return goodsCount;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public AppGoods getGoodsInfo() {
        return goodsInfo;
    }

    public void setGoodsInfo(AppGoods goodsInfo) {
        this.goodsInfo = goodsInfo;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("cartId", getCartId())
            .append("userId", getUserId())
            .append("goodsId", getGoodsId())
            .append("isSku", getIsSku())
            .append("dataId", getDataId())
            .append("dataValues", getDataValues())
            .append("goodsCount", getGoodsCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
                .append("goodsInfo", getGoodsInfo())
            .toString();
    }
}
