package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 快递公司对象 app_express
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppExpress extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 快递id */
    private Long expressId;

    /** 快递名称 */
    @Excel(name = "快递名称")
    private String expressName;

    /** 英文名称 */
    @Excel(name = "英文名称")
    private String simpleName;

    /** 快递图片 */
    @Excel(name = "快递图片")
    private String expressImage;

    /** 服务号码 */
    @Excel(name = "服务号码")
    private String expressPhone;

    /** 官方网址 */
    @Excel(name = "官方网址")
    private String expressUrl;

    /** 说明 */
    @Excel(name = "说明")
    private String description;

    /** 首重价格 */
    @Excel(name = "首重价格")
    private BigDecimal firstPrice;

    /** 续重价格 */
    @Excel(name = "续重价格")
    private BigDecimal secondPrice;

    /** 快递状态 */
    @Excel(name = "快递状态")
    private String status;

    public void setExpressId(Long expressId) 
    {
        this.expressId = expressId;
    }

    public Long getExpressId() 
    {
        return expressId;
    }

    public void setExpressName(String expressName) 
    {
        this.expressName = expressName;
    }

    public String getExpressName() 
    {
        return expressName;
    }

    public void setSimpleName(String simpleName) 
    {
        this.simpleName = simpleName;
    }

    public String getSimpleName() 
    {
        return simpleName;
    }

    public void setExpressImage(String expressImage) 
    {
        this.expressImage = expressImage;
    }

    public String getExpressImage() 
    {
        return expressImage;
    }

    public void setExpressPhone(String expressPhone) 
    {
        this.expressPhone = expressPhone;
    }

    public String getExpressPhone() 
    {
        return expressPhone;
    }

    public void setExpressUrl(String expressUrl) 
    {
        this.expressUrl = expressUrl;
    }

    public String getExpressUrl() 
    {
        return expressUrl;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public void setFirstPrice(BigDecimal firstPrice) 
    {
        this.firstPrice = firstPrice;
    }

    public BigDecimal getFirstPrice() 
    {
        return firstPrice;
    }

    public void setSecondPrice(BigDecimal secondPrice) 
    {
        this.secondPrice = secondPrice;
    }

    public BigDecimal getSecondPrice() 
    {
        return secondPrice;
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
            .append("expressId", getExpressId())
            .append("expressName", getExpressName())
            .append("simpleName", getSimpleName())
            .append("expressImage", getExpressImage())
            .append("expressPhone", getExpressPhone())
            .append("expressUrl", getExpressUrl())
            .append("description", getDescription())
            .append("firstPrice", getFirstPrice())
            .append("secondPrice", getSecondPrice())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
