package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邀请记录对象 app_user_inviter
 * 
 * @author lankong
 * @date 2025-04-06
 */
public class AppUserInviter extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 邀请id */
    private Long inviterId;

    /** 邀请人id */
    @Excel(name = "邀请人id")
    private Long userId;

    /** 新用户id */
    @Excel(name = "新用户id")
    private Long newUserId;

    /** 邀请码 */
    @Excel(name = "邀请码")
    private String inviterCode;

    /** 累计佣金 */
    @Excel(name = "累计佣金")
    private BigDecimal totalAward;

    /** 邀请状态 */
    @Excel(name = "邀请状态")
    private String status;

    /** 邀请人昵称（展示） */
    private transient String inviterNickName;

    /** 邀请人手机号（展示） */
    private transient String inviterPhonenumber;

    /** 邀请人头像（展示） */
    private transient String inviterAvatar;

    /** 被邀请人昵称（展示） */
    private transient String newUserNickName;

    /** 被邀请人手机号（展示） */
    private transient String newUserPhonenumber;

    /** 被邀请人头像（展示） */
    private transient String newUserAvatar;

    public void setInviterId(Long inviterId) 
    {
        this.inviterId = inviterId;
    }

    public Long getInviterId() 
    {
        return inviterId;
    }

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setNewUserId(Long newUserId) 
    {
        this.newUserId = newUserId;
    }

    public Long getNewUserId() 
    {
        return newUserId;
    }

    public void setInviterCode(String inviterCode) 
    {
        this.inviterCode = inviterCode;
    }

    public String getInviterCode() 
    {
        return inviterCode;
    }

    public void setTotalAward(BigDecimal totalAward) 
    {
        this.totalAward = totalAward;
    }

    public BigDecimal getTotalAward() 
    {
        return totalAward;
    }

    public void setStatus(String status) 
    {
        this.status = status;
    }

    public String getStatus() 
    {
        return status;
    }

    public String getInviterNickName()
    {
        return inviterNickName;
    }

    public void setInviterNickName(String inviterNickName)
    {
        this.inviterNickName = inviterNickName;
    }

    public String getInviterPhonenumber()
    {
        return inviterPhonenumber;
    }

    public void setInviterPhonenumber(String inviterPhonenumber)
    {
        this.inviterPhonenumber = inviterPhonenumber;
    }

    public String getInviterAvatar()
    {
        return inviterAvatar;
    }

    public void setInviterAvatar(String inviterAvatar)
    {
        this.inviterAvatar = inviterAvatar;
    }

    public String getNewUserNickName()
    {
        return newUserNickName;
    }

    public void setNewUserNickName(String newUserNickName)
    {
        this.newUserNickName = newUserNickName;
    }

    public String getNewUserPhonenumber()
    {
        return newUserPhonenumber;
    }

    public void setNewUserPhonenumber(String newUserPhonenumber)
    {
        this.newUserPhonenumber = newUserPhonenumber;
    }

    public String getNewUserAvatar()
    {
        return newUserAvatar;
    }

    public void setNewUserAvatar(String newUserAvatar)
    {
        this.newUserAvatar = newUserAvatar;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("inviterId", getInviterId())
            .append("userId", getUserId())
            .append("newUserId", getNewUserId())
            .append("inviterCode", getInviterCode())
            .append("totalAward", getTotalAward())
            .append("remark", getRemark())
            .append("createTime", getCreateTime())
            .append("status", getStatus())
            .toString();
    }
}
