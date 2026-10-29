package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.domain.AppActivityOrder;
import com.ruoyi.system.mapper.AppActivityMapper;
import com.ruoyi.system.mapper.AppActivityOrderMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActivityOrderIntegrityTest {
    @Test void unpaidCancellationAndTimeoutKeepOrderWhenWechatCannotConfirmClosure() {
        AppActivityOrderServiceImpl service=new AppActivityOrderServiceImpl();
        AppActivityMapper activities=mock(AppActivityMapper.class); AppActivityOrderMapper orders=mock(AppActivityOrderMapper.class);
        BookingAdjustmentService changes=mock(BookingAdjustmentService.class);
        ReflectionTestUtils.setField(service,"appActivityMapper",activities);ReflectionTestUtils.setField(service,"appActivityOrderMapper",orders);ReflectionTestUtils.setField(service,"bookingAdjustments",changes);
        AppActivityOrder order=new AppActivityOrder();order.setOrderId(1L);order.setUserId(7L);order.setActivityId(3L);order.setPayStatus("0");order.setStatus("0");order.setOrderNo("3000000000000000000001");
        when(orders.selectAppActivityOrderByOrderId(1L)).thenReturn(order); when(orders.selectAppActivityOrderByOrderIdForUpdate(1L)).thenReturn(order);
        when(orders.selectAppActivityOrderList(any())).thenReturn(java.util.Collections.singletonList(order));
        doThrow(new com.ruoyi.common.exception.ServiceException("微信结果未知")).when(changes).closePendingPaymentBeforeReprice(order.getOrderNo());
        assertThrows(com.ruoyi.common.exception.ServiceException.class, () -> service.cancelActivityOrder(1L,7L));
        assertEquals(0,service.closeExpiredUnpaidOrders(30));
        verify(orders,never()).updateAppActivityOrder(any()); verify(activities,never()).decreaseSignCount(anyLong(),anyInt());
    }
    @Test void freeSignupRejectsMultiplePeopleBeforeWritingAnything() {
        AppActivityOrderServiceImpl service=new AppActivityOrderServiceImpl();
        AppActivityMapper activities=mock(AppActivityMapper.class); AppActivityOrderMapper orders=mock(AppActivityOrderMapper.class);
        ReflectionTestUtils.setField(service,"appActivityMapper",activities); ReflectionTestUtils.setField(service,"appActivityOrderMapper",orders);
        AppActivity activity=new AppActivity();activity.setActivityId(3L);activity.setStatus("1");activity.setIsFree(1);
        when(activities.selectAppActivityByActivityIdForUpdate(3L)).thenReturn(activity);
        AppActivityOrder input=new AppActivityOrder();input.setActivityId(3L);input.setUserId(7L);input.setSignCount(2);
        assertThrows(com.ruoyi.common.exception.ServiceException.class, () -> service.signupActivity(input));
        verifyNoInteractions(orders); verify(activities,never()).increaseSignCount(anyLong(),anyInt());
    }
    @Test void paidCancellationOnlySubmitsReviewAndNeverReleasesSeats() {
        AppActivityOrderServiceImpl service=new AppActivityOrderServiceImpl();
        AppActivityMapper activities=mock(AppActivityMapper.class);AppActivityOrderMapper orders=mock(AppActivityOrderMapper.class);
        BookingAdjustmentService changes=mock(BookingAdjustmentService.class);
        ReflectionTestUtils.setField(service,"appActivityMapper",activities);ReflectionTestUtils.setField(service,"appActivityOrderMapper",orders);ReflectionTestUtils.setField(service,"bookingAdjustments",changes);
        AppActivityOrder order=new AppActivityOrder();order.setOrderId(1L);order.setUserId(7L);order.setActivityId(3L);order.setPayStatus("1");order.setStatus("1");order.setSignCount(2);order.setPayMoney(new BigDecimal("20.00"));
        when(orders.selectAppActivityOrderByOrderId(1L)).thenReturn(order);when(orders.selectAppActivityOrderByOrderIdForUpdate(1L)).thenReturn(order);
        assertEquals(1,service.cancelActivityOrder(1L,7L));verify(changes).submitActivity(1,7,0);
        verify(activities,never()).decreaseSignCount(anyLong(),anyInt());verify(orders,never()).updateAppActivityOrder(any());
    }
    @Test
    void usesGeneratedDatabaseIdentityForMerchantNumber() {
        AppActivityOrderServiceImpl service = new AppActivityOrderServiceImpl();
        AppActivityMapper activities = mock(AppActivityMapper.class);
        AppActivityOrderMapper orders = mock(AppActivityOrderMapper.class);
        ReflectionTestUtils.setField(service, "appActivityMapper", activities);
        ReflectionTestUtils.setField(service, "appActivityOrderMapper", orders);
        ReflectionTestUtils.setField(service, "bookingAdjustments", mock(BookingAdjustmentService.class));
        AppActivity activity = new AppActivity();
        activity.setActivityId(3L);
        activity.setStatus("1");
        activity.setIsFree(0);
        activity.setPrice(new BigDecimal("10"));
        when(activities.selectAppActivityByActivityIdForUpdate(3L)).thenReturn(activity);
        when(orders.insertAppActivityOrder(any())).thenAnswer(call -> {
            ((AppActivityOrder) call.getArgument(0)).setOrderId(123L);
            return 1;
        });
        AppActivityOrder request = new AppActivityOrder();
        request.setActivityId(3L);
        request.setUserId(7L);
        request.setSignName("test");
        request.setSignMobile("13800000000");
        request.setSignCount(1);
        assertTrue(service.createPendingActivityOrder(request).getOrderNo().matches("[A-Za-z0-9_\\-|*]{6,32}"));
        verify(activities).selectAppActivityByActivityIdForUpdate(3L);
    }
}
