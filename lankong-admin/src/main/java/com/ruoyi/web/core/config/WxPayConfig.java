package com.ruoyi.web.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wx.pay")
public class WxPayConfig {
    private String appId;
    /**
     * 商户号
     */
    private String merchantId;
    /**
     * 商户API私钥
     */
    private String privateKey;
    /**
     * 商户证书序列号
     */
    private String merchantSerialNumber;
    /**
     * 商户APIv3密钥
     */
    private String apiV3Key;
    /**
     * 支付通知地址
     */
    private String payNotifyUrl;
    /**
     * 退款通知地址
     */
    private String refundNotifyUrl;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getMerchantSerialNumber() {
        return merchantSerialNumber;
    }

    public void setMerchantSerialNumber(String merchantSerialNumber) {
        this.merchantSerialNumber = merchantSerialNumber;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String apiV3Key) {
        this.apiV3Key = apiV3Key;
    }

    public String getPayNotifyUrl() {
        return payNotifyUrl;
    }

    public void setPayNotifyUrl(String payNotifyUrl) {
        this.payNotifyUrl = payNotifyUrl;
    }

    public String getRefundNotifyUrl() {
        return refundNotifyUrl;
    }

    public void setRefundNotifyUrl(String refundNotifyUrl) {
        this.refundNotifyUrl = refundNotifyUrl;
    }
}
