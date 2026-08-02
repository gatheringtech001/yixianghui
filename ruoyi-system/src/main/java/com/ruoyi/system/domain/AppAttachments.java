package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 附件对象 app_attachments
 * 
 * @author lankong
 * @date 2025-07-20
 */
public class AppAttachments extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 附件id */
    private Long attachmentId;

    /** 文件路径 */
    @Excel(name = "文件路径")
    private String filePath;

    /** 关联业务id */
    @Excel(name = "关联业务id")
    private Long bussId;

    /** 业务分类；‘001’：退款证明图片 */
    @Excel(name = "业务分类；‘001’：退款证明图片")
    private String bussType;

    /** ‘0’：删除 ‘1’：正常 */
    @Excel(name = "‘0’：删除 ‘1’：正常")
    private String attStatus;

    public void setAttachmentId(Long attachmentId) 
    {
        this.attachmentId = attachmentId;
    }

    public Long getAttachmentId() 
    {
        return attachmentId;
    }

    public void setFilePath(String filePath) 
    {
        this.filePath = filePath;
    }

    public String getFilePath() 
    {
        return filePath;
    }

    public void setBussId(Long bussId) 
    {
        this.bussId = bussId;
    }

    public Long getBussId() 
    {
        return bussId;
    }

    public void setBussType(String bussType) 
    {
        this.bussType = bussType;
    }

    public String getBussType() 
    {
        return bussType;
    }

    public void setAttStatus(String attStatus) 
    {
        this.attStatus = attStatus;
    }

    public String getAttStatus() 
    {
        return attStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("attachmentId", getAttachmentId())
            .append("filePath", getFilePath())
            .append("bussId", getBussId())
            .append("bussType", getBussType())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .append("attStatus", getAttStatus())
            .toString();
    }
}
