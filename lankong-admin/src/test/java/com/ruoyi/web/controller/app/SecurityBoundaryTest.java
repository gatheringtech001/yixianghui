package com.ruoyi.web.controller.app;

import java.util.Collections;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.framework.aspectj.LogAspect;
import com.ruoyi.framework.config.ServerConfig;
import com.ruoyi.system.domain.SysOperLog;
import com.ruoyi.web.controller.common.CommonController;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SecurityBoundaryTest {
    @Configuration
    @EnableMethodSecurity
    @Import(CommonController.class)
    static class Config {
        @Bean ServerConfig serverConfig() { return mock(ServerConfig.class); }
    }

    @Test
    void uploadEndpointsRejectAnonymousCallersBeforeTouchingFiles() {
        SecurityContextHolder.clearContext();
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Config.class)) {
            CommonController controller = context.getBean(CommonController.class);
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.uploadFile(null));
            assertThrows(AuthenticationCredentialsNotFoundException.class, () -> controller.uploadFiles(null));
        }
    }

    @Log(isSaveRequestData = false)
    public void audited() { }

    @Test
    void responseAuditRedactsNestedWechatIdentityAndPaymentCredentials() throws Exception {
        Log settings = getClass().getMethod("audited").getAnnotation(Log.class);
        java.util.Map<String, Object> identity = new java.util.HashMap<>();
        identity.put("openid", "secret-open-id");
        identity.put("unionId", "secret-union-id");
        identity.put("paySign", "secret-payment-signature");
        identity.put("token", "secret-session");
        identity.put("orderId", 123L);
        SysOperLog audit = new SysOperLog();
        new LogAspect().getControllerMethodDescription(null, settings, audit,
                Collections.singletonMap("data", identity));
        assertFalse(audit.getJsonResult().contains("secret-"));
        assertTrue(audit.getJsonResult().contains("123"));
    }
}
