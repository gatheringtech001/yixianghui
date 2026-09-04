package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppGoodsOrderCouponTest {
    @Test
    void calculatesNinetyPercentAsTenPercentDiscount() {
        assertEquals(new BigDecimal("10.00"),
                AppGoodsOrderServiceImpl.calculatePercentageDiscount(
                        new BigDecimal("100.00"), new BigDecimal("90")));
    }

    @Test
    void rejectsInvalidPercentage() {
        assertThrows(ServiceException.class, () ->
                AppGoodsOrderServiceImpl.calculatePercentageDiscount(
                        new BigDecimal("100.00"), new BigDecimal("101")));
    }
}
