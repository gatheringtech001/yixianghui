package com.ruoyi.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商品属性对象 app_goods_sku
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsSku extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 类别id */
    private Long skuId;

    /** 所属商品 */
    @Excel(name = "所属商品")
    private Long goodsId;

    /** 属性名称 */
    @Excel(name = "属性名称")
    private String skuName;

    /** 属性状态 */
    @Excel(name = "属性状态")
    private String status;

    @Excel(name = "套餐分类")
    private String skuType;

    @Excel(name = "SKU编码")
    private String skuCode;

    @Excel(name = "父SKU_ID")
    private String parSkuId;

    @Excel(name = "排序")
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date invalidTime;

    @Excel(name = "库存")
    private Integer stock;

    @Excel(name = "销量")
    private Integer saleNum;
    @Excel(name = "库存单位")
    private String stockUnit;

    @Excel(name = "价格")
    private Double price;

    @Excel(name = "销售价")
    private Double salePrice;

    private String goodsName;

    private String tmpSkuId;
    private String tmpParSkuId;

    private transient List<AppGoodsSkuOption> options = new ArrayList();

    public void setSkuId(Long skuId) 
    {
        this.skuId = skuId;
    }

    public Long getSkuId() 
    {
        return skuId;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setSkuName(String skuName) 
    {
        this.skuName = skuName;
    }

    public String getSkuName() 
    {
        return skuName;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public List<AppGoodsSkuOption> getOptions() {
        return options;
    }

    public void setOptions(List<AppGoodsSkuOption> options) {
        this.options = options;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getSkuType() {
        return skuType;
    }

    public void setSkuType(String skuType) {
        this.skuType = skuType;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getParSkuId() {
        return parSkuId;
    }

    public void setParSkuId(String parSkuId) {
        this.parSkuId = parSkuId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Date getValidTime() {
        return validTime;
    }

    public void setValidTime(Date validTime) {
        this.validTime = validTime;
    }

    public Date getInvalidTime() {
        return invalidTime;
    }

    public void setInvalidTime(Date invalidTime) {
        this.invalidTime = invalidTime;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getSaleNum() {
        return saleNum;
    }

    public void setSaleNum(Integer saleNum) {
        this.saleNum = saleNum;
    }

    public String getStockUnit() {
        return stockUnit;
    }

    public void setStockUnit(String stockUnit) {
        this.stockUnit = stockUnit;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public String getTmpSkuId() {
        return tmpSkuId;
    }

    public void setTmpSkuId(String tmpSkuId) {
        this.tmpSkuId = tmpSkuId;
    }

    public String getTmpParSkuId() {
        return tmpParSkuId;
    }

    public void setTmpParSkuId(String tmpParSkuId) {
        this.tmpParSkuId = tmpParSkuId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("skuId", getSkuId())
            .append("goodsId", getGoodsId())
            .append("skuName", getSkuName())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .append("skuType", getSkuType())
            .append("skuCode", getSkuCode())
            .append("parSkuId", getParSkuId())
            .append("sortOrder", getSortOrder())
            .append("validTime", getValidTime())
            .append("invalidTime", getInvalidTime())
            .append("stock", getStock())
            .append("stockUnit", getStockUnit())
            .append("price", getPrice())
            .append("salePrice", getSalePrice())
            .toString();
    }
}
