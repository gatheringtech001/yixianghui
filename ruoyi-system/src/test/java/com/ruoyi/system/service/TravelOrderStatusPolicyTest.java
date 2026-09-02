package com.ruoyi.system.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelOrderStatusPolicyTest
{
    @Test
    void exposesEveryFeishuTravelOrderStatus()
    {
        assertEquals("待确认", TravelOrderStatusPolicy.label("0"));
        assertEquals("已确认", TravelOrderStatusPolicy.label("1"));
        assertEquals("已取消", TravelOrderStatusPolicy.label("2"));
        assertEquals("已入住", TravelOrderStatusPolicy.label("3"));
        assertEquals("已离店", TravelOrderStatusPolicy.label("4"));
        assertEquals("已结算", TravelOrderStatusPolicy.label("5"));
        assertEquals("退款中", TravelOrderStatusPolicy.label("6"));
        assertEquals("已退款", TravelOrderStatusPolicy.label("7"));
    }

    @Test
    void allowsOnlyForwardFulfillmentTransitions()
    {
        assertTrue(TravelOrderStatusPolicy.canManuallyTransition("1", "3"));
        assertTrue(TravelOrderStatusPolicy.canManuallyTransition("3", "4"));
        assertTrue(TravelOrderStatusPolicy.canManuallyTransition("4", "5"));

        assertThrows(IllegalArgumentException.class,
                () -> TravelOrderStatusPolicy.requireManualTransition("4", "3"));
        assertThrows(IllegalArgumentException.class,
                () -> TravelOrderStatusPolicy.requireManualTransition("6", "5"));
        assertThrows(IllegalArgumentException.class,
                () -> TravelOrderStatusPolicy.requireManualTransition("0", "1"));
    }
}
