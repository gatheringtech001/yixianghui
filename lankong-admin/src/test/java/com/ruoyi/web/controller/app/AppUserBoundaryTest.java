package com.ruoyi.web.controller.app;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppUserAddress;
import com.ruoyi.system.service.IAppGoodsOrderService;
import com.ruoyi.system.service.IAppUserAddressService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppUserBoundaryTest {
    @Test
    void failedPaymentNotificationMustUseNonSuccessHttpStatus() throws Exception {
        IAppGoodsOrderService orders = mock(IAppGoodsOrderService.class);
        AppUserController controller = userController();
        ReflectionTestUtils.setField(controller, "goodsOrderService", orders);
        org.springframework.test.web.servlet.MockMvc mvc =
                org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        when(orders.wxpayNotify(any())).thenReturn("{\"code\":\"FAIL\"}");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mnp/app_user/wxpay_notify"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is5xxServerError());
        when(orders.wxpayNotify(any())).thenReturn("{\"code\":\"SUCCESS\"}");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mnp/app_user/wxpay_notify"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        when(orders.wxpayRefundNotify(any())).thenThrow(new IllegalStateException("database unavailable"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mnp/app_user/wxpay_refund_notify"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is5xxServerError());
    }

    @Test
    void addressReadMustNotReturnAnotherUsersPersonalData() {
        IAppUserAddressService addresses = mock(IAppUserAddressService.class);
        AppUserController controller = userController();
        ReflectionTestUtils.setField(controller, "userAddressService", addresses);
        AppUserAddress other = new AppUserAddress();
        other.setUserId(8L);
        when(addresses.selectAppUserAddressByAddressId(20L)).thenReturn(other);

        AjaxResult response = controller.address_getInfo(20L);

        assertNotEquals(200, response.get(AjaxResult.CODE_TAG));
        assertNull(response.get(AjaxResult.DATA_TAG));
    }

    @Test
    void missingAddressAndOrderReturnBusinessErrors() {
        AppUserController controller = userController();
        ReflectionTestUtils.setField(controller, "userAddressService", mock(IAppUserAddressService.class));
        ReflectionTestUtils.setField(controller, "goodsOrderService", mock(IAppGoodsOrderService.class));
        AppUserAddress address = new AppUserAddress();
        address.setAddressId(20L);
        AppGoodsOrder order = new AppGoodsOrder();
        order.setOrderId(20L);

        assertNotEquals(200, controller.address_edit(address).get(AjaxResult.CODE_TAG));
        assertNotEquals(200, controller.address_remove(20L).get(AjaxResult.CODE_TAG));
        assertNotEquals(200, controller.pay_goods_order(order).get(AjaxResult.CODE_TAG));
    }

    @Test
    void legacyPaymentRouteUsesOwnedPersistedOrderAndRealPaymentService() {
        IAppGoodsOrderService orders = mock(IAppGoodsOrderService.class);
        AppGoodsOrderController controller = new AppGoodsOrderController() {
            @Override public Long getUserId() { return 7L; }
        };
        ReflectionTestUtils.setField(controller, "appGoodsOrderService", orders);
        AppGoodsOrder request = new AppGoodsOrder();
        request.setOrderId(20L);
        AppGoodsOrder stored = new AppGoodsOrder();
        stored.setOrderId(20L);
        stored.setUserId(8L);
        when(orders.selectAppGoodsOrderByOrderId(20L)).thenReturn(stored);
        assertNotEquals(200, controller.pay(request).get(AjaxResult.CODE_TAG));
        verify(orders, never()).wxpayPrepay(any());

        stored.setUserId(7L);
        AjaxResult expected = AjaxResult.error("payment provider unavailable");
        when(orders.wxpayPrepay(stored)).thenReturn(expected);
        assertSame(expected, controller.pay(request));
        verify(orders).wxpayPrepay(stored);
    }

    private AppUserController userController() {
        return new AppUserController() {
            @Override public Long getUserId() { return 7L; }
        };
    }
}
