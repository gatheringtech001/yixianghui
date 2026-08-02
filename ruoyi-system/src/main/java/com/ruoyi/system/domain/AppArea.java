package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 行政区域对象 app_area
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppArea extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 区域ID */
    private Long areaId;

    /** 省份 */
    @Excel(name = "省份")
    private String province;

    /** 州市 */
    @Excel(name = "州市")
    private String city;

    /** 区县 */
    @Excel(name = "区县")
    private String county;

    /** 邮政编码 */
    @Excel(name = "邮政编码")
    private String areaCode;

    /** 拼音 */
    @Excel(name = "拼音")
    private String pinyin;

    /** 等级 */
    @Excel(name = "等级")
    private Integer areaLevel;

    /** 经度 */
    @Excel(name = "经度")
    private BigDecimal lng;

    /** 纬度 */
    @Excel(name = "纬度")
    private BigDecimal lat;

    /** 地图数据 */
    @Excel(name = "地图数据")
    private String mapData;

    /** 是否地图核心 */
    @Excel(name = "是否地图核心")
    private Integer mapMaster;

    public void setAreaId(Long areaId) 
    {
        this.areaId = areaId;
    }

    public Long getAreaId() 
    {
        return areaId;
    }

    public void setProvince(String province) 
    {
        this.province = province;
    }

    public String getProvince() 
    {
        return province;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setCounty(String county) 
    {
        this.county = county;
    }

    public String getCounty() 
    {
        return county;
    }

    public void setAreaCode(String areaCode) 
    {
        this.areaCode = areaCode;
    }

    public String getAreaCode() 
    {
        return areaCode;
    }

    public void setPinyin(String pinyin) 
    {
        this.pinyin = pinyin;
    }

    public String getPinyin() 
    {
        return pinyin;
    }

    public void setAreaLevel(Integer areaLevel) 
    {
        this.areaLevel = areaLevel;
    }

    public Integer getAreaLevel() 
    {
        return areaLevel;
    }

    public void setLng(BigDecimal lng) 
    {
        this.lng = lng;
    }

    public BigDecimal getLng() 
    {
        return lng;
    }

    public void setLat(BigDecimal lat) 
    {
        this.lat = lat;
    }

    public BigDecimal getLat() 
    {
        return lat;
    }

    public void setMapData(String mapData) 
    {
        this.mapData = mapData;
    }

    public String getMapData() 
    {
        return mapData;
    }

    public void setMapMaster(Integer mapMaster) 
    {
        this.mapMaster = mapMaster;
    }

    public Integer getMapMaster() 
    {
        return mapMaster;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("areaId", getAreaId())
            .append("province", getProvince())
            .append("city", getCity())
            .append("county", getCounty())
            .append("areaCode", getAreaCode())
            .append("pinyin", getPinyin())
            .append("areaLevel", getAreaLevel())
            .append("lng", getLng())
            .append("lat", getLat())
            .append("mapData", getMapData())
            .append("mapMaster", getMapMaster())
            .toString();
    }
}
