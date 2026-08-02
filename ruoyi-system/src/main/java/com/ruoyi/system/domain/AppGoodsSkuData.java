package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 型号信息对象 app_goods_sku_data
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsSkuData extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 数据id */
    private Long dataId;

    /** 所属商品 */
    @Excel(name = "所属商品")
    private Long goodsId;

    /** 属性id集合 */
    @Excel(name = "属性id集合")
    private String skuIds;

    /** 选项组合id */
    @Excel(name = "选项组合id")
    private String optionIds;

    /** 选项组合名称 */
    @Excel(name = "选项组合名称")
    private String dataValues;

    /** 商品价格 */
    @Excel(name = "商品价格")
    private BigDecimal dataPrice;

    /** 选项图片 */
    @Excel(name = "选项图片")
    private String dataImage;

    /** 选项库存 */
    @Excel(name = "选项库存")
    private Long dataStock;

    /** 数据状态 */
    @Excel(name = "数据状态")
    private String status;

    public void setDataId(Long dataId) 
    {
        this.dataId = dataId;
    }

    public Long getDataId() 
    {
        return dataId;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setSkuIds(String skuIds) 
    {
        this.skuIds = skuIds;
    }

    public String getSkuIds() 
    {
        return skuIds;
    }

    public void setOptionIds(String optionIds) 
    {
        this.optionIds = optionIds;
    }

    public String getOptionIds() 
    {
        return optionIds;
    }

    public void setDataValues(String dataValues) 
    {
        this.dataValues = dataValues;
    }

    public String getDataValues() 
    {
        return dataValues;
    }

    public void setDataPrice(BigDecimal dataPrice) 
    {
        this.dataPrice = dataPrice;
    }

    public BigDecimal getDataPrice() 
    {
        return dataPrice;
    }

    public void setDataImage(String dataImage) 
    {
        this.dataImage = dataImage;
    }

    public String getDataImage() 
    {
        return dataImage;
    }

    public void setDataStock(Long dataStock) 
    {
        this.dataStock = dataStock;
    }

    public Long getDataStock() 
    {
        return dataStock;
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
            .append("dataId", getDataId())
            .append("goodsId", getGoodsId())
            .append("skuIds", getSkuIds())
            .append("optionIds", getOptionIds())
            .append("dataValues", getDataValues())
            .append("dataPrice", getDataPrice())
            .append("dataImage", getDataImage())
            .append("dataStock", getDataStock())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
