package com.ruoyi.web.core.config;

import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.util.PemUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.Resource;
import java.io.IOException;

@Configuration
public class WxPayAutoCertificateConfig {
    @Resource
    private WxPayConfig wxPayConfig;
    /**
     * 初始化商户配置
     * @return
     */
    @Bean
    public RSAAutoCertificateConfig rsaAutoCertificateConfig() throws IOException {
        RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wxPayConfig.getMerchantId())
                .privateKey(PemUtil.loadPrivateKeyFromPath(wxPayConfig.getPrivateKey()))
                .merchantSerialNumber(wxPayConfig.getMerchantSerialNumber())
                .apiV3Key(wxPayConfig.getApiV3Key())
                .build();
        return config;
    }
}
