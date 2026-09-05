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
    @Test
    void usesGeneratedDatabaseIdentityForMerchantNumber() {
        AppActivityOrderServiceImpl service = new AppActivityOrderServiceImpl();
        AppActivityMapper activities = mock(AppActivityMapper.class);
        AppActivityOrderMapper orders = mock(AppActivityOrderMapper.class);
        ReflectionTestUtils.setField(service, "appActivityMapper", activities);
        ReflectionTestUtils.setField(service, "appActivityOrderMapper", orders);
        AppActivity activity = new AppActivity();
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
