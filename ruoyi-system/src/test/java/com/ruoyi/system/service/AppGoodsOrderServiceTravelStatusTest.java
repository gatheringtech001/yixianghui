package com.ruoyi.system.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.mapper.AppGoodsMapper;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import com.ruoyi.system.service.impl.AppGoodsOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppGoodsOrderServiceTravelStatusTest
{
    private AppGoodsOrderMapper orderMapper;
    private AppGoodsMapper goodsMapper;
    private AppGoodsOrderServiceImpl service;

    @BeforeEach
    void setUp()
    {
        orderMapper = mock(AppGoodsOrderMapper.class);
        goodsMapper = mock(AppGoodsMapper.class);
        service = new AppGoodsOrderServiceImpl();
        ReflectionTestUtils.setField(service, "appGoodsOrderMapper", orderMapper);
        ReflectionTestUtils.setField(service, "appGoodsMapper", goodsMapper);
    }

    @Test
    void advancesTravelOrderThroughControlledEndpoint()
    {
        AppGoodsOrder order = order(10L, 20L, TravelOrderStatusPolicy.CONFIRMED);
        AppGoods goods = new AppGoods();
        goods.setGoodsType("hotel");
        when(orderMapper.selectAppGoodsOrderByOrderId(10L)).thenReturn(order);
        when(goodsMapper.selectAppGoodsByGoodsId(20L)).thenReturn(goods);
        when(orderMapper.updateAppGoodsOrder(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        assertEquals(1, service.updateTravelStatus(10L, TravelOrderStatusPolicy.CHECKED_IN));

        ArgumentCaptor<AppGoodsOrder> update = ArgumentCaptor.forClass(AppGoodsOrder.class);
        verify(orderMapper).updateAppGoodsOrder(update.capture());
        assertEquals(TravelOrderStatusPolicy.CHECKED_IN, update.getValue().getTravelStatus());
    }

    @Test
    void rejectsNonTravelOrder()
    {
        AppGoodsOrder order = order(10L, 20L, null);
        AppGoods goods = new AppGoods();
        goods.setGoodsType("education");
        when(orderMapper.selectAppGoodsOrderByOrderId(10L)).thenReturn(order);
        when(goodsMapper.selectAppGoodsByGoodsId(20L)).thenReturn(goods);

        assertThrows(ServiceException.class,
                () -> service.updateTravelStatus(10L, TravelOrderStatusPolicy.CONFIRMED));
        verify(orderMapper, never()).updateAppGoodsOrder(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void genericEditCannotBypassTravelStateMachine()
    {
        AppGoodsOrder current = order(10L, 20L, TravelOrderStatusPolicy.CHECKED_IN);
        current.setStatus("1");
        when(orderMapper.selectAppGoodsOrderByOrderId(10L)).thenReturn(current);
        when(orderMapper.updateAppGoodsOrder(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        AppGoodsOrder request = order(10L, 20L, TravelOrderStatusPolicy.SETTLED);
        request.setStatus("1");

        service.updateAppGoodsOrder(request);

        ArgumentCaptor<AppGoodsOrder> update = ArgumentCaptor.forClass(AppGoodsOrder.class);
        verify(orderMapper).updateAppGoodsOrder(update.capture());
        assertNull(update.getValue().getTravelStatus());
    }

    @Test
    void cancellingUnpaidOrderAlsoCancelsTravelFulfillment()
    {
        AppGoodsOrder current = order(10L, 20L, TravelOrderStatusPolicy.PENDING_CONFIRMATION);
        current.setStatus("0");
        current.setPayStatus("0");
        when(orderMapper.selectAppGoodsOrderByOrderId(10L)).thenReturn(current);
        when(orderMapper.updateAppGoodsOrder(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        AppGoodsOrder request = order(10L, 20L, null);
        request.setStatus("2");

        service.updateAppGoodsOrder(request);

        ArgumentCaptor<AppGoodsOrder> update = ArgumentCaptor.forClass(AppGoodsOrder.class);
        verify(orderMapper).updateAppGoodsOrder(update.capture());
        assertEquals(TravelOrderStatusPolicy.CANCELLED, update.getValue().getTravelStatus());
    }

    private AppGoodsOrder order(Long orderId, Long goodsId, String travelStatus)
    {
        AppGoodsOrder order = new AppGoodsOrder();
        order.setOrderId(orderId);
        order.setGoodsId(goodsId);
        order.setTravelStatus(travelStatus);
        return order;
    }
}
