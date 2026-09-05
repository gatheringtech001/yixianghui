package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IAppPayLogService;
import com.ruoyi.system.service.IAppUserInfoService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WechatPrepayContractTest {
    @Test void generatedNumbersMeetWechatBoundsAndAreStable() {
        for (long id : new long[]{1, 448, 9999, Long.MAX_VALUE}) {
            String number = MerchantOrderNumbers.create("20", id);
            assertTrue(number.matches("[A-Za-z0-9_\\-|*]{6,32}"));
            assertEquals(number, MerchantOrderNumbers.create("20", id));
            assertNotEquals(number, MerchantOrderNumbers.create("30", id));
        }
        assertFalse(MerchantOrderNumbers.valid("20448"));
        assertEquals("2020260905000000262", MerchantOrderNumbers.forPayment("2020260905000000262", "20", 448L));
        assertEquals(MerchantOrderNumbers.create("20", 448L), MerchantOrderNumbers.forPayment("20448", "20", 448L));
        assertThrows(ServiceException.class, () -> MerchantOrderNumbers.forPayment("bad", "20", 448L));
    }

    @Test void gatewayRejectsBadNumberBeforeContactingWechat() {
        WechatPrepayService gateway = new WechatPrepayService() {
            @Override protected PrepayWithRequestPaymentResponse send(PrepayRequest r) { fail("Must reject before network"); return null; }
        };
        PrepayRequest request = new PrepayRequest(); request.setOutTradeNo("20448");
        assertThrows(ServiceException.class, () -> gateway.create(request));
    }

    @Test void sdkRejectionBecomesSafeActionableApplicationError() {
        WechatPrepayService gateway = new WechatPrepayService() {
            @Override protected PrepayWithRequestPaymentResponse send(PrepayRequest r) {
                throw new com.wechat.pay.java.core.exception.ServiceException(null, 400,
                        "{\"code\":\"PARAM_ERROR\",\"message\":\"sensitive request details\"}");
            }
        };
        PrepayRequest request = new PrepayRequest(); request.setOutTradeNo("20000000000000000448");
        Amount amount = new Amount(); amount.setTotal(1); request.setAmount(amount);
        ServiceException error = assertThrows(ServiceException.class, () -> gateway.create(request));
        assertTrue(error.getMessage().contains("PARAM_ERROR"));
        assertFalse(error.getMessage().contains("sensitive"));
    }

    @Test void discountedMultiItemOrderPaysOneFenAndRetriesWithTheSameNumber() {
        AppGoodsOrderServiceImpl service = new AppGoodsOrderServiceImpl();
        AppGoodsOrderMapper orders = mock(AppGoodsOrderMapper.class);
        IAppPayLogService logs = mock(IAppPayLogService.class);
        IAppUserInfoService users = mock(IAppUserInfoService.class);
        AppGoodsOrder order = pendingGoods();
        AppGoodsOrderDetail first = new AppGoodsOrderDetail(); first.setGoodsId(262L);
        AppGoodsOrderDetail second = new AppGoodsOrderDetail(); second.setGoodsId(260L);
        order.setOrderDetailList(Arrays.asList(first, second));
        when(orders.selectAppGoodsOrderByOrderIdForUpdate(448L)).thenReturn(order);
        when(orders.updateAppGoodsOrder(any())).thenReturn(1);
        when(logs.insertAppPayLog(any())).thenReturn(1);
        when(users.selectAppUserInfoByUserId(7L)).thenReturn(payer());
        AtomicReference<PrepayRequest> captured = new AtomicReference<>();
        wire(service, "appGoodsOrderMapper", orders); wire(service, "payLogService", logs);
        wire(service, "userInfoService", users); wire(service, "wechatPrepayService", capturing(captured));
        assertEquals(200, service.wxpayPrepay(order).get("code"));
        String number = captured.get().getOutTradeNo();
        assertEquals(MerchantOrderNumbers.create("20",448L), number);
        assertEquals(Integer.valueOf(1), captured.get().getAmount().getTotal());
        assertNull(captured.get().getDetail(), "Platform coupon must not conflict with optional WeChat item marketing amounts");
        assertEquals(2, order.getOrderDetailList().size());
        assertEquals(200, service.wxpayPrepay(order).get("code"));
        assertEquals(number, captured.get().getOutTradeNo());
        assertEquals("0", order.getPayStatus());
    }

    @Test void shortNumberWithExistingPaymentRecordCannotBeRenumbered() {
        AppGoodsOrderServiceImpl service = new AppGoodsOrderServiceImpl();
        AppGoodsOrderMapper orders = mock(AppGoodsOrderMapper.class);
        IAppPayLogService logs = mock(IAppPayLogService.class);
        AppGoodsOrder order = pendingGoods();
        when(orders.selectAppGoodsOrderByOrderIdForUpdate(448L)).thenReturn(order);
        when(logs.selectAppPayLogByPayNo("20448")).thenReturn(new AppPayLog());
        wire(service, "appGoodsOrderMapper", orders); wire(service, "payLogService", logs);
        assertThrows(ServiceException.class, () -> service.wxpayPrepay(order));
        verify(orders, never()).updateAppGoodsOrder(any());
    }

    @Test void legacyActivityNumberIsCorrectedAndWechatGetsExactTotal() {
        AppActivityOrderServiceImpl service = new AppActivityOrderServiceImpl();
        AppActivityOrderMapper orders = mock(AppActivityOrderMapper.class);
        AppActivityMapper activities = mock(AppActivityMapper.class);
        IAppUserInfoService users = mock(IAppUserInfoService.class);
        AppActivityOrder order = new AppActivityOrder();
        order.setOrderId(12L); order.setOrderNo("3012"); order.setActivityId(3L); order.setUserId(7L);
        order.setStatus("0"); order.setPayStatus("0"); order.setSignCount(3);
        order.setMoneyPayable(new BigDecimal("0.01")); order.setCreateTime(new Date());
        when(orders.selectAppActivityOrderByOrderId(12L)).thenReturn(order);
        when(orders.updateAppActivityOrder(any())).thenReturn(1);
        when(activities.selectAppActivityByActivityId(3L)).thenReturn(new AppActivity());
        when(users.selectAppUserInfoByUserId(7L)).thenReturn(payer());
        AtomicReference<PrepayRequest> captured = new AtomicReference<>();
        wire(service,"appActivityOrderMapper",orders); wire(service,"appActivityMapper",activities);
        wire(service,"userInfoService",users); wire(service,"payLogService",mock(IAppPayLogService.class));
        wire(service,"wechatPrepayService",capturing(captured));
        assertEquals(200,service.wxpayPrepay(order).get("code"));
        assertEquals(MerchantOrderNumbers.create("30",12L),captured.get().getOutTradeNo());
        assertEquals(Integer.valueOf(1),captured.get().getAmount().getTotal());
        assertNull(captured.get().getDetail());
    }

    private AppGoodsOrder pendingGoods() {
        AppGoodsOrder order = new AppGoodsOrder(); order.setOrderId(448L); order.setOrderNo("20448");
        order.setUserId(7L); order.setStatus("0"); order.setPayStatus("0");
        order.setMoneyPayable(new BigDecimal("0.01")); order.setCreateTime(new Date()); return order;
    }
    private AppUserInfo payer() { AppUserInfo user = new AppUserInfo(); user.setWeixinOpenid("local-test-payer"); return user; }
    private WechatPrepayService capturing(AtomicReference<PrepayRequest> captured) {
        return new WechatPrepayService() {
            @Override protected PrepayWithRequestPaymentResponse send(PrepayRequest r) {
                captured.set(r); PrepayWithRequestPaymentResponse response = new PrepayWithRequestPaymentResponse();
                response.setPackageVal("prepay_id=local-test-only"); return response;
            }
        };
    }
    private void wire(Object target,String field,Object value) { ReflectionTestUtils.setField(target,field,value); }
}
