package com.ruoyi.web.core.config;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.auth.Credential;
import com.wechat.pay.java.core.auth.Validator;
import com.wechat.pay.java.core.cipher.AeadCipher;
import com.wechat.pay.java.core.cipher.PrivacyDecryptor;
import com.wechat.pay.java.core.cipher.PrivacyEncryptor;
import com.wechat.pay.java.core.cipher.Signer;
import com.wechat.pay.java.core.cipher.Verifier;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.util.PemUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @ConditionalOnProperty(name = "wx.pay.enabled", havingValue = "true", matchIfMissing = true)
    public RSAAutoCertificateConfig rsaAutoCertificateConfig() throws IOException {
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(wxPayConfig.getMerchantId())
                .privateKey(PemUtil.loadPrivateKeyFromPath(wxPayConfig.getPrivateKey()))
                .merchantSerialNumber(wxPayConfig.getMerchantSerialNumber())
                .apiV3Key(wxPayConfig.getApiV3Key())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "wx.pay.enabled", havingValue = "false")
    public DisabledWxPayConfig disabledWxPayConfig() {
        return new DisabledWxPayConfig();
    }

    public static final class DisabledWxPayConfig implements Config, NotificationConfig {
        private IllegalStateException disabled() {
            return new IllegalStateException("WeChat Pay is disabled in this environment");
        }

        @Override
        public PrivacyEncryptor createEncryptor() {
            throw disabled();
        }

        @Override
        public PrivacyDecryptor createDecryptor() {
            throw disabled();
        }

        @Override
        public Credential createCredential() {
            throw disabled();
        }

        @Override
        public Validator createValidator() {
            throw disabled();
        }

        @Override
        public Signer createSigner() {
            throw disabled();
        }

        @Override
        public String getSignType() {
            throw disabled();
        }

        @Override
        public String getCipherType() {
            throw disabled();
        }

        @Override
        public Verifier createVerifier() {
            throw disabled();
        }

        @Override
        public AeadCipher createAeadCipher() {
            throw disabled();
        }
    }
}
