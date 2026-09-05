package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import com.ruoyi.system.service.IAppPayLogService;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerifiedPaymentTest {
    private AppGoodsOrderServiceImpl service;
    private IAppPayLogService logs;
    private AppGoodsOrderMapper orders;
    private AppGoodsOrder order;
    private Transaction transaction;

    @BeforeEach
    void setup() {
        service = new AppGoodsOrderServiceImpl();
        logs = mock(IAppPayLogService.class);
        orders = mock(AppGoodsOrderMapper.class);
        ReflectionTestUtils.setField(service, "payLogService", logs);
        ReflectionTestUtils.setField(service, "appGoodsOrderMapper", orders);
        ReflectionTestUtils.setField(service, "appId", "test-app");
        ReflectionTestUtils.setField(service, "merchantId", "test-merchant");
        AppPayLog log = new AppPayLog();
        log.setOrderId(1L);
        log.setPayNo("201");
        log.setStatus("0");
        log.setPayMoney(new BigDecimal("10000"));
        when(logs.selectAppPayLogByPayNo("201")).thenReturn(log);
        order = new AppGoodsOrder();
        order.setOrderId(1L);
        order.setPayStatus("0");
        when(orders.selectAppGoodsOrderByOrderIdForUpdate(1L)).thenReturn(order);
        transaction = mock(Transaction.class, RETURNS_DEEP_STUBS);
        when(transaction.getOutTradeNo()).thenReturn("201");
        when(transaction.getAppid()).thenReturn("test-app");
        when(transaction.getMchid()).thenReturn("test-merchant");
        when(transaction.getTradeState()).thenReturn(Transaction.TradeStateEnum.SUCCESS);
        when(transaction.getAmount().getTotal()).thenReturn(10000);
        when(transaction.getAmount().getCurrency()).thenReturn("CNY");
    }

    @Test
    void mismatchedAmountDoesNotAlterOrderOrPaymentLog() {
        when(transaction.getAmount().getTotal()).thenReturn(1);
        assertTrue(service.handleVerifiedPayment(transaction).contains("FAIL"));
        verify(orders, never()).updateAppGoodsOrder(any());
        verify(logs, never()).updateAppPayLog(any());
    }

    @Test
    void failedOrderWriteCannotLeaveSuccessMarker() {
        when(orders.updateAppGoodsOrder(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> service.handleVerifiedPayment(transaction));
        verify(logs, never()).updateAppPayLog(any());
    }

    @Test
    void cancelledOrderDoesNotSilentlyAcknowledgeSuccessfulPayment() {
        order.setPayStatus("2");
        assertTrue(service.handleVerifiedPayment(transaction).contains("FAIL"));
        verify(logs, never()).updateAppPayLog(any());
    }
}
