package com.ruoyi.system.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 第三方授权表 sys_auth_user
 * 
 * @author ruoyi
 */
public class SysAuthUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 授权ID */
    private Long authId;

    /** 第三方平台用户唯一ID */
    private String uuid;

    /** 系统用户ID */
    private Long userId;

    /** 登录账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 头像地址 */
    private String avatar;

    /** 用户邮箱 */
    private String email;

    /** 用户来源 */
    private String source;

    /** 微信开放平台 UnionID，仅用于身份映射 */
    private String unionId;

    /** 微信应用 AppID */
    private String appId;

    public Long getAuthId()
    {
        return authId;
    }

    public void setAuthId(Long authId)
    {
        this.authId = authId;
    }

    @JsonIgnore
    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    @JsonIgnore
    public String getUnionId()
    {
        return unionId;
    }

    public void setUnionId(String unionId)
    {
        this.unionId = unionId;
    }

    @JsonIgnore
    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }
}
