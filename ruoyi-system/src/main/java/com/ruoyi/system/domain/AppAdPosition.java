package com.ruoyi.system.domain;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 广告管理对象 app_ad_position
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppAdPosition extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 广告位ID */
    private Long positionId;

    /** 位置名称 */
    @Excel(name = "位置名称")
    private String positionName;

    /** 位置编号 */
    @Excel(name = "位置编号")
    private String positionCode;

    /** 位置状态 */
    @Excel(name = "位置状态")
    private String status;

    /** 广告内容信息 */
    private List<AppAdContent> appAdContentList;

    public void setPositionId(Long positionId) 
    {
        this.positionId = positionId;
    }

    public Long getPositionId() 
    {
        return positionId;
    }

    public void setPositionName(String positionName) 
    {
        this.positionName = positionName;
    }

    public String getPositionName() 
    {
        return positionName;
    }

    public void setPositionCode(String positionCode) 
    {
        this.positionCode = positionCode;
    }

    public String getPositionCode() 
    {
        return positionCode;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public List<AppAdContent> getAppAdContentList()
    {
        return appAdContentList;
    }

    public void setAppAdContentList(List<AppAdContent> appAdContentList)
    {
        this.appAdContentList = appAdContentList;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("positionId", getPositionId())
            .append("positionName", getPositionName())
            .append("positionCode", getPositionCode())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .append("appAdContentList", getAppAdContentList())
            .toString();
    }
}
