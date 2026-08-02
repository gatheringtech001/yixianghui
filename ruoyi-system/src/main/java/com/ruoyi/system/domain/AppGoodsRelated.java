package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品详情区块对象 app_goods_related
 * 
 * @author lankong
 * @date 2025-11-27
 */
public class AppGoodsRelated extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long goodsId;

    /** 区块ID */
    @Excel(name = "区块ID")
    private String sectionId;

    /** 区块名称 */
    @Excel(name = "区块名称")
    private String sectionName;

    /** 详情内容 */
    @Excel(name = "详情内容")
    private String content;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** 展开阈值 */
    @Excel(name = "展开阈值")
    private Long minContentLength;

    public void setId(Long id) 
    {
        this.id = id;
    }

    public Long getId() 
    {
        return id;
    }

    public void setGoodsId(Long goodsId) 
    {
        this.goodsId = goodsId;
    }

    public Long getGoodsId() 
    {
        return goodsId;
    }

    public void setSectionId(String sectionId) 
    {
        this.sectionId = sectionId;
    }

    public String getSectionId() 
    {
        return sectionId;
    }

    public void setSectionName(String sectionName) 
    {
        this.sectionName = sectionName;
    }

    public String getSectionName() 
    {
        return sectionName;
    }

    public void setContent(String content) 
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setMinContentLength(Long minContentLength) 
    {
        this.minContentLength = minContentLength;
    }

    public Long getMinContentLength() 
    {
        return minContentLength;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("goodsId", getGoodsId())
            .append("sectionId", getSectionId())
            .append("sectionName", getSectionName())
            .append("content", getContent())
            .append("sortOrder", getSortOrder())
            .append("minContentLength", getMinContentLength())
            .append("createTime", getCreateTime())
            .toString();
    }
}
