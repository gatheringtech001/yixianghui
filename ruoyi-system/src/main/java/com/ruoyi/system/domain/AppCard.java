package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 会员卡对象 app_card
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppCard extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 卡id */
    private Long cardId;

    /** 卡名称 */
    @Excel(name = "卡名称")
    private String cardName;

    /** 卡片标识 */
    @Excel(name = "卡片标识")
    private String cardKey;

    /** 卡片图片 */
    @Excel(name = "卡片图片")
    private String cardImage;

    /** 卡介绍 */
    @Excel(name = "卡介绍")
    private String description;

    /** 卡价格 */
    @Excel(name = "卡价格")
    private String price;

    /** 卡有效期 */
    @Excel(name = "卡有效期")
    private String expiration;

    /** 详细说明 */
    @Excel(name = "详细说明")
    private String content;

    /** 折扣类型 */
    @Excel(name = "折扣类型")
    private String discountType;

    /** 卡状态 */
    @Excel(name = "卡状态")
    private String status;

    public void setCardId(Long cardId) 
    {
        this.cardId = cardId;
    }

    public Long getCardId() 
    {
        return cardId;
    }

    public void setCardName(String cardName) 
    {
        this.cardName = cardName;
    }

    public String getCardName() 
    {
        return cardName;
    }

    public void setCardKey(String cardKey) 
    {
        this.cardKey = cardKey;
    }

    public String getCardKey() 
    {
        return cardKey;
    }

    public void setCardImage(String cardImage) 
    {
        this.cardImage = cardImage;
    }

    public String getCardImage() 
    {
        return cardImage;
    }

    public void setDescription(String description) 
    {
        this.description = description;
    }

    public String getDescription() 
    {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getExpiration() {
        return expiration;
    }

    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getContent() 
    {
        return content;
    }

    public void setDiscountType(String discountType) 
    {
        this.discountType = discountType;
    }

    public String getDiscountType() 
    {
        return discountType;
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
            .append("cardId", getCardId())
            .append("cardName", getCardName())
            .append("cardKey", getCardKey())
            .append("cardImage", getCardImage())
            .append("description", getDescription())
                .append("price", getPrice())
                .append("expiration", getExpiration())
            .append("content", getContent())
            .append("discountType", getDiscountType())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("status", getStatus())
            .toString();
    }
}
