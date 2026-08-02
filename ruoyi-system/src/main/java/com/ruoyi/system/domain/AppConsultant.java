package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 康养顾问对象 app_consultant
 * 
 * @author lankong
 * @date 2025-05-14
 */
public class AppConsultant extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 顾问ID */
    private Long consultantId;

    /** 顾问编号 */
    @Excel(name = "顾问编号")
    private String consultantNo;

    /** 所属站点 */
    @Excel(name = "所属站点")
    private Long deptId;

    /** 顾问姓名 */
    @Excel(name = "顾问姓名")
    private String consultantName;

    /** 顾问电话 */
    @Excel(name = "顾问电话")
    private String mobile;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idcard;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 状态 */
    @Excel(name = "状态")
    private String status;

    /** 关联用户ID */
    @Excel(name = "关联用户ID")
    private Long userId;

    /** 所属站点 */
    @Excel(name = "所属站点")
    private transient String deptName;

    /** 关联用户昵称（展示用） */
    private transient String userNickName;

    public void setConsultantId(Long consultantId) 
    {
        this.consultantId = consultantId;
    }

    public Long getConsultantId() 
    {
        return consultantId;
    }

    public void setConsultantNo(String consultantNo) 
    {
        this.consultantNo = consultantNo;
    }

    public String getConsultantNo() 
    {
        return consultantNo;
    }

    public void setDeptId(Long deptId) 
    {
        this.deptId = deptId;
    }

    public Long getDeptId() 
    {
        return deptId;
    }

    public void setConsultantName(String consultantName) 
    {
        this.consultantName = consultantName;
    }

    public String getConsultantName() 
    {
        return consultantName;
    }

    public void setMobile(String mobile) 
    {
        this.mobile = mobile;
    }

    public String getMobile() 
    {
        return mobile;
    }

    public void setIdcard(String idcard) 
    {
        this.idcard = idcard;
    }

    public String getIdcard() 
    {
        return idcard;
    }

    @Override
    public String getRemark() {
        return remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getUserNickName() {
        return userNickName;
    }

    public void setUserNickName(String userNickName) {
        this.userNickName = userNickName;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("consultantId", getConsultantId())
            .append("consultantNo", getConsultantNo())
            .append("deptId", getDeptId())
            .append("consultantName", getConsultantName())
            .append("mobile", getMobile())
            .append("idcard", getIdcard())
                .append("remark", getRemark())
            .append("status", getStatus())
            .append("createTime", getCreateTime())
            .append("userId", getUserId())
            .append("updateTime", getUpdateTime())
                .append("deptName", getDeptName())
            .toString();
    }
}
