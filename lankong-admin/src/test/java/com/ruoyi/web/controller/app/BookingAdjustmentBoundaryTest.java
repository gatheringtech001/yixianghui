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
    @Configuration @EnableMethodSecurity @Import({BookingAdjustmentController.class,AppGoodsOrderAfterController.class})
    static class Config {
        @Bean BookingAdjustmentService changes() { return mock(BookingAdjustmentService.class); }
        @Bean com.ruoyi.system.service.IAppGoodsOrderAfterService afters() { return mock(com.ruoyi.system.service.IAppGoodsOrderAfterService.class); }
        @Bean com.ruoyi.system.service.IAppGoodsOrderService orders() { return mock(com.ruoyi.system.service.IAppGoodsOrderService.class); }
        @Bean com.ruoyi.system.service.IAppGoodsService goods() { return mock(com.ruoyi.system.service.IAppGoodsService.class); }
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
            AppGoodsOrderAfterController afters=context.getBean(AppGoodsOrderAfterController.class);
            for(String decision:Arrays.asList("approve","reject","sync","retry")) {
                com.ruoyi.system.domain.AppGoodsOrderAfter input=new com.ruoyi.system.domain.AppGoodsOrderAfter();
                input.setAfterId(1L);input.setRefundAction(decision);
                assertThrows(AccessDeniedException.class, () -> afters.refundPrepay(input));
            }
            assertThrows(AccessDeniedException.class, () -> afters.list(new com.ruoyi.system.domain.AppGoodsOrderAfter()));
            verifyNoInteractions(service);
            controller.latest("3",1); verify(service).latest("3",1,5);
        }
    }
    @Test void reviewerIdentityAlwaysComesFromTheAuthenticatedAdministrator() {
        login(99,"system:app_goods_order_after:edit");
        try(AnnotationConfigApplicationContext context=new AnnotationConfigApplicationContext(Config.class)) {
            AppGoodsOrderAfterController controller=context.getBean(AppGoodsOrderAfterController.class);
            com.ruoyi.system.domain.AppGoodsOrderAfter input=new com.ruoyi.system.domain.AppGoodsOrderAfter();input.setAfterId(7L);input.setStatus("1");input.setUpdateBy("5");
            controller.refundPrepay(input);
            assertEquals("99",input.getUpdateBy());
            verify(context.getBean(com.ruoyi.system.service.IAppGoodsOrderService.class)).wxpayRefund(input);
        }
    }
    @Test void noIndependentActivityApprovalRouteRemains() {
        for(java.lang.reflect.Method method:BookingAdjustmentController.class.getDeclaredMethods()) {
            org.springframework.web.bind.annotation.PostMapping mapping=method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if(mapping!=null) for(String path:mapping.value()) assertFalse(path.startsWith("/system/"));
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
