package com.ruoyi.system.domain;

/**
 * 小程序用户资料更新（昵称、头像、微信手机号授权码）
 */
public class AppUserMnpUpdateBody
{
    private String nickName;

    private String avatar;

    /** 微信 getPhoneNumber 返回的 code，建议改走 wx_profile_auth 接口 */
    private String phoneCode;

    /** 现居住地址 */
    private String liveAddress;

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

    public String getPhoneCode()
    {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode)
    {
        this.phoneCode = phoneCode;
    }

    public String getLiveAddress()
    {
        return liveAddress;
    }

    public void setLiveAddress(String liveAddress)
    {
        this.liveAddress = liveAddress;
    }
}
