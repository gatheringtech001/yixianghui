package com.ruoyi.web.controller.app;

import java.util.*;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.PermissionService;
import com.ruoyi.system.service.impl.BookingAdjustmentService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingAdjustmentBoundaryTest {
    @Configuration @EnableMethodSecurity @Import(BookingAdjustmentController.class)
    static class Config {
        @Bean BookingAdjustmentService changes() { return mock(BookingAdjustmentService.class); }
        @Bean(name="ss") PermissionService permissions() { return new PermissionService(); }
    }
    @BeforeEach void request() {
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(
            new org.springframework.web.context.request.ServletRequestAttributes(new org.springframework.mock.web.MockHttpServletRequest()));
    }
    @AfterEach void clear() {
        SecurityContextHolder.clearContext();
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }
    @Test void miniProgramUserCannotApproveRejectRetryOrListOtherUsersRefunds() {
        login(5,"system:mnp:user");
        try (AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext(Config.class)) {
            BookingAdjustmentController controller=context.getBean(BookingAdjustmentController.class);
            BookingAdjustmentService service=context.getBean(BookingAdjustmentService.class);
            clearInvocations(service); // Spring invokes the mocked schema startup check during bean creation.
            for(String decision:Arrays.asList("approve","reject","sync","retry")) {
                Map<String,String> input=new HashMap<>(); input.put("decision",decision);input.put("note","测试");
                assertThrows(AccessDeniedException.class, () -> controller.review(1,input));
            }
            assertThrows(AccessDeniedException.class, () -> controller.list(null));
            verifyNoInteractions(service);
            controller.latest("3",1); verify(service).latest("3",1,5);
        }
    }
    @Test void reviewerIdentityAlwaysComesFromTheAuthenticatedAdministrator() {
        login(99,"system:app_pay_refund_log:edit");
        try(AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext(Config.class)) {
            BookingAdjustmentController controller=context.getBean(BookingAdjustmentController.class);
            Map<String,String> input=new HashMap<>();input.put("decision","approve");input.put("note","核对通过");input.put("reviewerId","5");
            controller.review(7,input);
            verify(context.getBean(BookingAdjustmentService.class)).review(7,99,"approve","核对通过");
        }
    }
    @Test void refundReceiptsCannotBeManuallyForgedOrDeleted() {
        AppPayRefundLogController controller=new AppPayRefundLogController();
        assertEquals(500,controller.add(new com.ruoyi.system.domain.AppPayRefundLog()).get("code"));
        assertEquals(500,controller.edit(new com.ruoyi.system.domain.AppPayRefundLog()).get("code"));
        assertEquals(500,controller.remove(new Long[]{1L}).get("code"));
    }
    private void login(long id,String permission) {
        LoginUser user=new LoginUser(id,1L,new SysUser(),Collections.singleton(permission));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,Collections.emptyList()));
    }
}
