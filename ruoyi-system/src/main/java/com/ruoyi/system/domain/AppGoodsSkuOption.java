package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 属性选项对象 app_goods_sku_option
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppGoodsSkuOption extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 选项id */
    private Long optionId;

    /** 所属商品 */
    @Excel(name = "所属商品")
    private Long goodsId;

    /** 所属属性 */
    @Excel(name = "所属属性")
    private Long skuId;

    /** 选项名称 */
    @Excel(name = "选项名称")
    private String optionName;

    /** 选项参数 */
    @Excel(name = "选项参数")
    private String optionParam;

    /** 选项状态 */
    @Excel(name = "选项状态")
    private String status;
    @Excel(name = "套餐分类")
    private String optionType;

    @Excel(name = "选项值")
    private String optionValue;

    @Excel(name = "选项值单位")
    private String optionValueUnit;

    @Excel(name = "序号")
    private Integer optionSort;

    @Excel(name = "规格编号")
    private Integer skuSeqNo;

    public void setOptionId(Long optionId) 
    {
        this.optionId = optionId;
    }

    public Long getOptionId() 
    {
        return optionId;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setSkuId(Long skuId) 
    {
        this.skuId = skuId;
    }

    public Long getSkuId() 
    {
        return skuId;
    }

    public void setOptionName(String optionName) 
    {
        this.optionName = optionName;
    }

    public String getOptionName() 
    {
        return optionName;
    }

    public String getOptionParam() {
        return optionParam;
    }

    public void setOptionParam(String optionParam) {
        this.optionParam = optionParam;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getOptionType() {
        return optionType;
    }

    public void setOptionType(String optionType) {
        this.optionType = optionType;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public void setOptionValue(String optionValue) {
        this.optionValue = optionValue;
    }

    public String getOptionValueUnit() {
        return optionValueUnit;
    }

    public void setOptionValueUnit(String optionValueUnit) {
        this.optionValueUnit = optionValueUnit;
    }

    public Integer getOptionSort() {
        return optionSort;
    }

    public void setOptionSort(Integer optionSort) {
        this.optionSort = optionSort;
    }

    public Integer getSkuSeqNo() {
        return skuSeqNo;
    }

    public void setSkuSeqNo(Integer skuSeqNo) {
        this.skuSeqNo = skuSeqNo;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("optionId", getOptionId())
            .append("goodsId", getGoodsId())
            .append("skuId", getSkuId())
            .append("optionName", getOptionName())
                .append("optionParam", getOptionParam())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
                .append("optionType", getOptionType())
                .append("optionValue", getOptionValue())
                .append("optionValueUnit", getOptionValueUnit())
                .append("optionSort", getOptionSort())
            .toString();
    }
}
