package com.ruoyi.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户信息对象 app_user_info
 * 
 * @author lankong
 * @date 2025-06-03
 */
public class AppUserInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 应用ID */
    @Excel(name = "应用ID")
    private Long appId;

    /** 所属分组 */
    @Excel(name = "所属分组")
    private Long groupId;

    /** 推荐人 */
    @Excel(name = "推荐人")
    private Long introId;

    /** 推荐码地址 */
    @Excel(name = "推荐码地址")
    private String qrcodeUrl;

    /** 金币数量 */
    @Excel(name = "金币数量")
    private Long golden;

    /** 积分数量 */
    @Excel(name = "积分数量")
    private Long score;

    /** 钱包 */
    @Excel(name = "钱包")
    private BigDecimal money;

    /** 粉丝数量 */
    @Excel(name = "粉丝数量")
    private Long fansCount;

    /** 真实姓名 */
    @Excel(name = "真实姓名")
    private String trueName;

    /** 身份证号 */
    @Excel(name = "身份证号")
    private String idcard;

    /** 提现银行 */
    @Excel(name = "提现银行")
    private String bankTitle;

    /** 银行户名 */
    @Excel(name = "银行户名")
    private String bankUsername;

    /** 银行卡号 */
    @Excel(name = "银行卡号")
    private String bankAccount;

    /** 用户所在城市 */
    @Excel(name = "用户所在城市")
    private String city;

    /** 微信号 */
    @Excel(name = "微信号")
    private String wexinAccount;

    /** 微信openid */
    @Excel(name = "微信openid")
    private String weixinOpenid;

    /** 支付宝账号 */
    @Excel(name = "支付宝账号")
    private String alipayAccount;

    public void setUserId(Long userId) 
    {
        this.userId = userId;
    }

    public Long getUserId() 
    {
        return userId;
    }

    public void setAppId(Long appId) 
    {
        this.appId = appId;
    }

    public Long getAppId() 
    {
        return appId;
    }

    public void setGroupId(Long groupId) 
    {
        this.groupId = groupId;
    }

    public Long getGroupId() 
    {
        return groupId;
    }

    public void setIntroId(Long introId) 
    {
        this.introId = introId;
    }

    public Long getIntroId() 
    {
        return introId;
    }

    public void setQrcodeUrl(String qrcodeUrl) 
    {
        this.qrcodeUrl = qrcodeUrl;
    }

    public String getQrcodeUrl() 
    {
        return qrcodeUrl;
    }

    public void setGolden(Long golden) 
    {
        this.golden = golden;
    }

    public Long getGolden() 
    {
        return golden;
    }

    public void setScore(Long score) 
    {
        this.score = score;
    }

    public Long getScore() 
    {
        return score;
    }

    public void setMoney(BigDecimal money) 
    {
        this.money = money;
    }

    public BigDecimal getMoney() 
    {
        return money;
    }

    public void setFansCount(Long fansCount) 
    {
        this.fansCount = fansCount;
    }

    public Long getFansCount() 
    {
        return fansCount;
    }

    public void setTrueName(String trueName) 
    {
        this.trueName = trueName;
    }

    public String getTrueName() 
    {
        return trueName;
    }

    public void setIdcard(String idcard) 
    {
        this.idcard = idcard;
    }

    public String getIdcard() 
    {
        return idcard;
    }

    public void setBankTitle(String bankTitle) 
    {
        this.bankTitle = bankTitle;
    }

    public String getBankTitle() 
    {
        return bankTitle;
    }

    public void setBankUsername(String bankUsername) 
    {
        this.bankUsername = bankUsername;
    }

    public String getBankUsername() 
    {
        return bankUsername;
    }

    public void setBankAccount(String bankAccount) 
    {
        this.bankAccount = bankAccount;
    }

    public String getBankAccount() 
    {
        return bankAccount;
    }

    public void setCity(String city) 
    {
        this.city = city;
    }

    public String getCity() 
    {
        return city;
    }

    public void setWexinAccount(String wexinAccount) 
    {
        this.wexinAccount = wexinAccount;
    }

    public String getWexinAccount() 
    {
        return wexinAccount;
    }

    public void setWeixinOpenid(String weixinOpenid) 
    {
        this.weixinOpenid = weixinOpenid;
    }

    public String getWeixinOpenid() 
    {
        return weixinOpenid;
    }

    public void setAlipayAccount(String alipayAccount) 
    {
        this.alipayAccount = alipayAccount;
    }

    public String getAlipayAccount() 
    {
        return alipayAccount;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("appId", getAppId())
            .append("groupId", getGroupId())
            .append("introId", getIntroId())
            .append("qrcodeUrl", getQrcodeUrl())
            .append("golden", getGolden())
            .append("score", getScore())
            .append("money", getMoney())
            .append("fansCount", getFansCount())
            .append("trueName", getTrueName())
            .append("idcard", getIdcard())
            .append("bankTitle", getBankTitle())
            .append("bankUsername", getBankUsername())
            .append("bankAccount", getBankAccount())
            .append("city", getCity())
            .append("wexinAccount", getWexinAccount())
            .append("weixinOpenid", getWeixinOpenid())
            .append("alipayAccount", getAlipayAccount())
            .toString();
    }
}
