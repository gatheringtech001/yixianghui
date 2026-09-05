package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品对象 app_goods
 * 
 * @author lankong
 * @date 2025-03-31
 */
public class AppGoods extends BaseEntity
{
    private Long orderQuantity;
    public Long getOrderQuantity() { return orderQuantity; }
    public void setOrderQuantity(Long value) { orderQuantity = value; }
    private static final long serialVersionUID = 1L;

    /** 商品id */
    private Long goodsId;

    /** 所属分类 */
    @Excel(name = "所属分类")
    private Long categoryId;

    /** 祖级分类 */
    @Excel(name = "祖级分类")
    private String categoryIds;

    /** 所属分站 */
    @Excel(name = "所属分站")
    private Long deptId;

    /** 商品名称 */
    @Excel(name = "商品名称")
    private String goodsName;

    /** 封面图片 */
    @Excel(name = "封面图片")
    private String goodsCover;

    /** 轮播图 */
    @Excel(name = "轮播图")
    private String goodsImages;

    /** 商品简介 */
    @Excel(name = "商品简介")
    private String description;

    /** 商品标签 */
    @Excel(name = "商品标签")
    private String tags;

    /** 价格 */
    @Excel(name = "价格")
    private BigDecimal price;

    /** 会员价格 */
    @Excel(name = "会员价格")
    private BigDecimal vipPrice;

    /** 单位 */
    @Excel(name = "单位")
    private String unit;

    /** 规格说明 */
    @Excel(name = "规格说明")
    private String specifications;

    /** 库存 */
    @Excel(name = "库存")
    private Long stock;

    /** 商品类型 */
    @Excel(name = "商品类型")
    private String goodsType;

    /** 是否置顶 */
    @Excel(name = "是否置顶")
    private Integer isTop;

    /** 是否热门 */
    @Excel(name = "是否热门")
    private Integer isHot;

    /** 分类属性id集合 */
    @Excel(name = "分类属性id集合")
    private String attrIds;

    /** 分类属性值集合 */
    @Excel(name = "分类属性值集合")
    private String attrValues;

    /** 是否sku价 */
    @Excel(name = "是否sku价")
    private Integer isSku;

    /** 推荐奖励类型（0不奖励 1按照系统设置奖励 2商品单独设置奖励比例 3商品单独设置奖励金额） */
    @Excel(name = "推荐奖励类型", readConverterExp = "0=不奖励,1=按照系统设置奖励,2=商品单独设置奖励比例,3=商品单独设置奖励金额")
    private String awardType;

    /** 父级奖励比例（%或元） */
    @Excel(name = "父级奖励比例", readConverterExp = "%=或元")
    private BigDecimal awardParentRatio;

    /** 祖级奖励比例（%或元） */
    @Excel(name = "祖级奖励比例", readConverterExp = "%=或元")
    private BigDecimal awardGrandParentRatio;

    /** 奖励金币 */
    @Excel(name = "奖励金币")
    private BigDecimal awardGolden;

    /** 商品内容 */
    private String content;

    /** 运费 */
    @Excel(name = "运费")
    private BigDecimal expressFee;

    /** 重量(克) */
    @Excel(name = "重量(克)")
    private Long weight;

    /** 阅读次数 */
    @Excel(name = "阅读次数")
    private Long viewCount;

    /** 销量 */
    @Excel(name = "销量")
    private Long saleCount;

    /** 商品状态 */
    @Excel(name = "商品状态")
    private String status;
    private String deptName;

    private String deptIds;

    private List<AppGoodsSku> optionList = new ArrayList();
    private List<AppGoodsRelated> features = new ArrayList();
    /** 老年教育扩展信息（goodsType=education 时使用） */
    private AppGoodsEducationExt educationExt;

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setCategoryId(Long categoryId) 
    {
        this.categoryId = categoryId;
    }

    public Long getCategoryId() 
    {
        return categoryId;
    }

    public void setCategoryIds(String categoryIds) 
    {
        this.categoryIds = categoryIds;
    }

    public String getCategoryIds() 
    {
        return categoryIds;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setGoodsName(String goodsName) 
    {
        this.goodsName = goodsName;
    }

    public String getGoodsName() 
    {
        return goodsName;
    }

    public void setGoodsCover(String goodsCover) 
    {
        this.goodsCover = goodsCover;
    }

    public String getGoodsCover() 
    {
        return goodsCover;
    }

    public void setGoodsImages(String goodsImages) 
    {
        this.goodsImages = goodsImages;
    }

    public String getGoodsImages() 
    {
        return goodsImages;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setTags(String tags) 
    {
        this.tags = tags;
    }

    public String getTags() 
    {
        return tags;
    }

    public void setPrice(BigDecimal price) 
    {
        this.price = price;
    }

    public BigDecimal getPrice() 
    {
        return price;
    }

    public BigDecimal getVipPrice() {
        return vipPrice;
    }

    public void setVipPrice(BigDecimal vipPrice) {
        this.vipPrice = vipPrice;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public String getUnit() 
    {
        return unit;
    }

    public void setSpecifications(String specifications) 
    {
        this.specifications = specifications;
    }

    public String getSpecifications() 
    {
        return specifications;
    }

    public void setStock(Long stock) 
    {
        this.stock = stock;
    }

    public Long getStock() 
    {
        return stock;
    }

    public void setGoodsType(String goodsType) 
    {
        this.goodsType = goodsType;
    }

    public String getGoodsType() 
    {
        return goodsType;
    }

    public void setIsTop(Integer isTop) 
    {
        this.isTop = isTop;
    }

    public Integer getIsTop() 
    {
        return isTop;
    }

    public void setIsHot(Integer isHot) 
    {
        this.isHot = isHot;
    }

    public Integer getIsHot() 
    {
        return isHot;
    }

    public void setAttrIds(String attrIds) 
    {
        this.attrIds = attrIds;
    }

    public String getAttrIds() 
    {
        return attrIds;
    }

    public void setAttrValues(String attrValues) 
    {
        this.attrValues = attrValues;
    }

    public String getAttrValues() 
    {
        return attrValues;
    }

    public void setIsSku(Integer isSku) 
    {
        this.isSku = isSku;
    }

    public Integer getIsSku() 
    {
        return isSku;
    }

    public void setAwardType(String awardType) 
    {
        this.awardType = awardType;
    }

    public String getAwardType() 
    {
        return awardType;
    }

    public void setAwardParentRatio(BigDecimal awardParentRatio) 
    {
        this.awardParentRatio = awardParentRatio;
    }

    public BigDecimal getAwardParentRatio() 
    {
        return awardParentRatio;
    }

    public void setAwardGrandParentRatio(BigDecimal awardGrandParentRatio) 
    {
        this.awardGrandParentRatio = awardGrandParentRatio;
    }

    public BigDecimal getAwardGrandParentRatio() 
    {
        return awardGrandParentRatio;
    }

    public BigDecimal getAwardGolden() {
        return awardGolden;
    }

    public void setAwardGolden(BigDecimal awardGolden) {
        this.awardGolden = awardGolden;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setExpressFee(BigDecimal expressFee) 
    {
        this.expressFee = expressFee;
    }

    public BigDecimal getExpressFee() 
    {
        return expressFee;
    }

    public void setWeight(Long weight) 
    {
        this.weight = weight;
    }

    public Long getWeight() 
    {
        return weight;
    }

    public void setViewCount(Long viewCount) 
    {
        this.viewCount = viewCount;
    }

    public Long getViewCount() 
    {
        return viewCount;
    }

    public void setSaleCount(Long saleCount) 
    {
        this.saleCount = saleCount;
    }

    public Long getSaleCount() 
    {
        return saleCount;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }


    public String getDeptIds() {
        return deptIds;
    }

    public void setDeptIds(String deptIds) {
        this.deptIds = deptIds;
    }

    public List<AppGoodsSku> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<AppGoodsSku> optionList) {
        this.optionList = optionList;
    }

    public List<AppGoodsRelated> getFeatures() {
        return features;
    }

    public void setFeatures(List<AppGoodsRelated> features) {
        this.features = features;
    }

    public AppGoodsEducationExt getEducationExt() {
        return educationExt;
    }

    public void setEducationExt(AppGoodsEducationExt educationExt) {
        this.educationExt = educationExt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("goodsId", getGoodsId())
            .append("categoryId", getCategoryId())
            .append("categoryIds", getCategoryIds())
            .append("deptId", getDeptId())
            .append("goodsName", getGoodsName())
            .append("goodsCover", getGoodsCover())
            .append("goodsImages", getGoodsImages())
            .append("description", getDescription())
            .append("tags", getTags())
            .append("price", getPrice())
                .append("vipPrice", getVipPrice())
            .append("unit", getUnit())
            .append("specifications", getSpecifications())
            .append("stock", getStock())
            .append("goodsType", getGoodsType())
            .append("isTop", getIsTop())
            .append("isHot", getIsHot())
            .append("attrIds", getAttrIds())
            .append("attrValues", getAttrValues())
            .append("isSku", getIsSku())
            .append("awardType", getAwardType())
            .append("awardParentRatio", getAwardParentRatio())
            .append("awardGrandParentRatio", getAwardGrandParentRatio())
                .append("awardGolden", getAwardGolden())
            .append("content", getContent())
            .append("expressFee", getExpressFee())
            .append("weight", getWeight())
            .append("viewCount", getViewCount())
            .append("saleCount", getSaleCount())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
