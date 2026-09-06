package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import com.ruoyi.system.mapper.AppGoodsOrderAfterMapper;
import com.ruoyi.system.service.IAppGoldService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ReviewedRefundCompletionTest {
    @Test void verifiedCompletionUpdatesTheExactAfterSaleAndRetainsNativeEffects() {
        AppGoodsOrderServiceImpl service = spy(new AppGoodsOrderServiceImpl());
        AppGoodsOrderMapper orders = mock(AppGoodsOrderMapper.class);
        AppGoodsOrderAfterMapper afters = mock(AppGoodsOrderAfterMapper.class);
        IAppGoldService gold = mock(IAppGoldService.class);
        ReflectionTestUtils.setField(service, "appGoodsOrderMapper", orders);
        ReflectionTestUtils.setField(service, "appGoodsOrderAfterMapper", afters);
        ReflectionTestUtils.setField(service, "goldService", gold);
        AppGoodsOrder order = new AppGoodsOrder(); order.setOrderId(20L); order.setOrderNo("TEST20");
        order.setStatus("3"); order.setPayStatus("1"); order.setTravelStatus("6"); order.setUserId(5L);
        AppGoodsOrderAfter after = new AppGoodsOrderAfter(); after.setAfterId(8L); after.setOrderId(20L); after.setStatus("1");
        when(orders.selectAppGoodsOrderByOrderId(20L)).thenReturn(order);
        when(afters.selectAppGoodsOrderAfterByAfterId(8L)).thenReturn(after);
        when(orders.updateAppGoodsOrder(any())).thenAnswer(call -> {
            AppGoodsOrder value = call.getArgument(0); order.setStatus(value.getStatus()); order.setPayStatus(value.getPayStatus()); order.setTravelStatus(value.getTravelStatus()); return 1;
        });
        when(afters.updateAppGoodsOrderAfter(any())).thenAnswer(call -> { AppGoodsOrderAfter value = call.getArgument(0); after.setStatus(value.getStatus()); after.setRefundMoney(value.getRefundMoney()); return 1; });
        doNothing().when(service).releaseEducationStockIfNeeded(any());
        AppPayRefundLog log = new AppPayRefundLog(); log.setOrderId(20L); log.setUserId(5L);
        log.setAgentRefundNo("YXHAF8"); log.setRefundMoney(new BigDecimal("100"));
        service.completeReviewedRefund(log);
        assertEquals("6", after.getStatus()); assertEquals(new BigDecimal("1"), after.getRefundMoney());
        assertEquals("4", order.getPayStatus()); assertEquals("7", order.getTravelStatus());
        verify(service).releaseEducationStockIfNeeded(order);
        verify(gold).reverseOnRefund(eq(5L), any(), eq(20L), eq(new BigDecimal("100")), eq("YXHAF8"));
        verify(afters, never()).selectAppGoodsOrderAfterList(any());
    }
}
