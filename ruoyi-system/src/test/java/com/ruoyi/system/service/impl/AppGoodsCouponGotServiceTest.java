package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoodsCoupon;
import com.ruoyi.system.domain.AppGoodsCouponGot;
import com.ruoyi.system.mapper.AppGoodsCouponGotMapper;
import com.ruoyi.system.mapper.AppGoodsCouponMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppGoodsCouponGotServiceTest {
    private AppGoodsCouponGotServiceImpl service;
    private AppGoodsCouponMapper couponMapper;
    private AppGoodsCouponGotMapper gotMapper;

    @BeforeEach
    void setUp() {
        service = new AppGoodsCouponGotServiceImpl();
        couponMapper = mock(AppGoodsCouponMapper.class);
        gotMapper = mock(AppGoodsCouponGotMapper.class);
        ReflectionTestUtils.setField(service, "appGoodsCouponMapper", couponMapper);
        ReflectionTestUtils.setField(service, "appGoodsCouponGotMapper", gotMapper);
    }

    @Test
    void rejectsMismatchedSourceMiniProgram() {
        AppGoodsCoupon coupon = activeCoupon();
        coupon.setSourceAppId("wx_expected");
        when(couponMapper.selectByChannelCodeForUpdate("partner_a")).thenReturn(coupon);

        assertThrows(ServiceException.class, () ->
                service.claimDistributionCoupon(7L, "partner_a", "wx_other"));
        verify(gotMapper, never()).insertAppGoodsCouponGot(any());
    }

    @Test
    void createsOnlyOneClaimForCurrentUser() {
        AppGoodsCoupon coupon = activeCoupon();
        when(couponMapper.selectByChannelCodeForUpdate("partner_a")).thenReturn(coupon);
        when(gotMapper.selectByUserAndCoupon(7L, 3L)).thenReturn(null);
        when(gotMapper.insertAppGoodsCouponGot(any())).thenReturn(1);

        AppGoodsCouponGot result = service.claimDistributionCoupon(7L, "partner_a", "");

        assertEquals(7L, result.getUserId());
        assertEquals("partner_a", result.getChannelCode());
        assertEquals(0, result.getIsUsed());
        verify(couponMapper).incrementGotCount(3L);
    }

    private AppGoodsCoupon activeCoupon() {
        AppGoodsCoupon coupon = new AppGoodsCoupon();
        coupon.setCouponId(3L);
        coupon.setStatus("1");
        coupon.setCouponTotal(100L);
        coupon.setCouponGotCount(0L);
        return coupon;
    }
}
