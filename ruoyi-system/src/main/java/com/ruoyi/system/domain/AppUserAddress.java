package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户地址对象 app_user_address
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserAddress extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 地址ID */
    private Long addressId;

    /** 所属用户 */
    @Excel(name = "所属用户")
    private Long userId;

    /** 区划编码 */
    @Excel(name = "区划编码")
    private String regionCode;

    /** 区划标签 */
    @Excel(name = "区划标签")
    private String regionLabel;

    /** 区划id组 */
    @Excel(name = "区划id组")
    private String regionIds;

    /** 省份编码 */
    @Excel(name = "省份编码")
    private String provinceCode;

    /** 收货省份 */
    @Excel(name = "收货省份")
    private String provinceName;

    /** 城市编码 */
    @Excel(name = "城市编码")
    private String cityCode;

    /** 收货城市 */
    @Excel(name = "收货城市")
    private String cityName;

    /** 区县编码 */
    @Excel(name = "区县编码")
    private String countyCode;

    /** 收货区县 */
    @Excel(name = "收货区县")
    private String countyName;

    /** 乡镇街道名称 */
    @Excel(name = "乡镇街道名称")
    private String streetName;

    /** 乡镇街道编码 */
    @Excel(name = "乡镇街道编码")
    private String streetCode;

    /** 详细地址 */
    @Excel(name = "详细地址")
    private String addressDetail;

    /** 收货人姓名 */
    @Excel(name = "收货人姓名")
    private String linkPerson;

    /** 收货人手机 */
    @Excel(name = "收货人手机")
    private String linkMobile;

    /** 邮政编码 */
    @Excel(name = "邮政编码")
    private String postCode;

    /** 是否默认 */
    @Excel(name = "是否默认")
    private Integer isDefault;

    /** 地址状态 */
    @Excel(name = "地址状态")
    private String status;

    private String userName;

    public void setAddressId(Long addressId) 
    {
        this.addressId = addressId;
    }

    public Long getAddressId() 
    {
        return addressId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setRegionCode(String regionCode) 
    {
        this.regionCode = regionCode;
    }

    public String getRegionCode() 
    {
        return regionCode;
    }

    public void setRegionLabel(String regionLabel) 
    {
        this.regionLabel = regionLabel;
    }

    public String getRegionLabel() 
    {
        return regionLabel;
    }

    public void setRegionIds(String regionIds) 
    {
        this.regionIds = regionIds;
    }

    public String getRegionIds() 
    {
        return regionIds;
    }

    public void setProvinceCode(String provinceCode) 
    {
        this.provinceCode = provinceCode;
    }

    public String getProvinceCode() 
    {
        return provinceCode;
    }

    public void setProvinceName(String provinceName) 
    {
        this.provinceName = provinceName;
    }

    public String getProvinceName() 
    {
        return provinceName;
    }

    public void setCityCode(String cityCode) 
    {
        this.cityCode = cityCode;
    }

    public String getCityCode() 
    {
        return cityCode;
    }

    public void setCityName(String cityName) 
    {
        this.cityName = cityName;
    }

    public String getCityName() 
    {
        return cityName;
    }

    public void setCountyCode(String countyCode) 
    {
        this.countyCode = countyCode;
    }

    public String getCountyCode() 
    {
        return countyCode;
    }

    public void setCountyName(String countyName) 
    {
        this.countyName = countyName;
    }

    public String getCountyName() 
    {
        return countyName;
    }

    public void setStreetName(String streetName) 
    {
        this.streetName = streetName;
    }

    public String getStreetName() 
    {
        return streetName;
    }

    public void setStreetCode(String streetCode) 
    {
        this.streetCode = streetCode;
    }

    public String getStreetCode() 
    {
        return streetCode;
    }

    public void setAddressDetail(String addressDetail) 
    {
        this.addressDetail = addressDetail;
    }

    public String getAddressDetail() 
    {
        return addressDetail;
    }

    public void setLinkPerson(String linkPerson) 
    {
        this.linkPerson = linkPerson;
    }

    public String getLinkPerson() 
    {
        return linkPerson;
    }

    public void setLinkMobile(String linkMobile) 
    {
        this.linkMobile = linkMobile;
    }

    public String getLinkMobile() 
    {
        return linkMobile;
    }

    public void setPostCode(String postCode) 
    {
        this.postCode = postCode;
    }

    public String getPostCode() 
    {
        return postCode;
    }

    public void setIsDefault(Integer isDefault) 
    {
        this.isDefault = isDefault;
    }

    public Integer getIsDefault() 
    {
        return isDefault;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("addressId", getAddressId())
            .append("userId", getUserId())
            .append("regionCode", getRegionCode())
            .append("regionLabel", getRegionLabel())
            .append("regionIds", getRegionIds())
            .append("provinceCode", getProvinceCode())
            .append("provinceName", getProvinceName())
            .append("cityCode", getCityCode())
            .append("cityName", getCityName())
            .append("countyCode", getCountyCode())
            .append("countyName", getCountyName())
            .append("streetName", getStreetName())
            .append("streetCode", getStreetCode())
            .append("addressDetail", getAddressDetail())
            .append("linkPerson", getLinkPerson())
            .append("linkMobile", getLinkMobile())
            .append("postCode", getPostCode())
            .append("isDefault", getIsDefault())
            .append("status", getStatus())
            .toString();
    }
}
