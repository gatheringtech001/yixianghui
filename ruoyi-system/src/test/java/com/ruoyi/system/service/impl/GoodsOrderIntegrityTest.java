package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GoodsOrderIntegrityTest {
    private AppGoodsOrderServiceImpl service;
    private AppGoodsMapper goodsMapper;
    private AppGoodsOrderMapper orders;
    private AppGoodsSkuMapper skus;
    private AppGoodsCouponGotMapper coupons;
    private AppGoods goods;

    @BeforeEach
    void setup() {
        service = new AppGoodsOrderServiceImpl();
        goodsMapper = mock(AppGoodsMapper.class);
        orders = mock(AppGoodsOrderMapper.class);
        skus = mock(AppGoodsSkuMapper.class);
        coupons = mock(AppGoodsCouponGotMapper.class);
        ReflectionTestUtils.setField(service, "appGoodsMapper", goodsMapper);
        ReflectionTestUtils.setField(service, "appGoodsOrderMapper", orders);
        ReflectionTestUtils.setField(service, "appGoodsSkuMapper", skus);
        ReflectionTestUtils.setField(service, "couponGotMapper", coupons);
        ReflectionTestUtils.setField(service, "couponMapper", mock(AppGoodsCouponMapper.class));
        AppGoodsOrderDetailMapper details = mock(AppGoodsOrderDetailMapper.class);
        when(details.insertAppGoodsOrderDetail(any())).thenReturn(1);
        ReflectionTestUtils.setField(service, "orderDetailMapper", details);
        ReflectionTestUtils.setField(service, "appGoodsSkuOptionMapper", mock(AppGoodsSkuOptionMapper.class));
        goods = new AppGoods();
        goods.setGoodsId(31L);
        goods.setGoodsType("online");
        goods.setStatus("1");
        goods.setIsSku(0);
        goods.setVipPrice(new BigDecimal("100.00"));
        goods.setPrice(new BigDecimal("100.00"));
        when(goodsMapper.selectAppGoodsByGoodsId(31L)).thenReturn(goods);
        when(goodsMapper.reserveStock(eq(31L), anyLong())).thenReturn(1);
        when(orders.updateAppGoodsOrder(any())).thenReturn(1);
        AtomicLong nextId = new AtomicLong(100);
        when(orders.insertAppGoodsOrder(any())).thenAnswer(call -> {
            AppGoodsOrder order = call.getArgument(0);
            assertNull(order.getOrderId(), "client must not select database primary key");
            order.setOrderId(nextId.incrementAndGet());
            return 1;
        });
    }

    @Test
    void createIgnoresForgedPaymentFulfillmentAndAttributionFields() {
        AppGoodsOrder request = request();
        request.setOrderId(1L);
        request.setPayStatus("1");
        request.setStatus("1");
        request.setPayMoney(new BigDecimal("0.01"));
        request.setMoneyExpress(new BigDecimal("-100"));
        request.setDistributionChannelCode("forged");
        request.setFeishuRecordId("forged");
        request.setSendExpressNo("forged");
        request.setIsChecked("1");
        AppGoodsOrder created = service.insertAppGoodsOrder(request);
        assertEquals("0", created.getPayStatus());
        assertEquals("0", created.getStatus());
        assertEquals(new BigDecimal("100.00"), created.getMoneyPayable());
        assertEquals(new BigDecimal("100.00"), created.getMoneyTotal());
        assertNull(created.getDistributionChannelCode());
        assertNull(created.getFeishuRecordId());
        assertNull(created.getSendExpressNo());
        assertNotEquals("1", created.getIsChecked());
    }

    @Test
    void failedInsertMustThrowSoReservedStockCanRollBack() {
        doReturn(0).when(orders).insertAppGoodsOrder(any());
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request()));
    }

    @Test
    void orderNumberUsesUniqueOrderIdentity() {
        AppGoodsOrder first = service.insertAppGoodsOrder(request());
        AppGoodsOrder second = service.insertAppGoodsOrder(request());
        assertNotEquals(first.getOrderNo(), second.getOrderNo());
        assertTrue(first.getOrderNo().matches("[A-Za-z0-9_\\-|*]{6,32}"));
    }

    @Test
    void nonTravelDatesCannotReduceGoodsPriceToZero() {
        AppGoodsOrder request = request();
        request.setCheckInDate(Date.valueOf("2027-01-01"));
        request.setCheckOutDate(Date.valueOf("2027-01-01"));
        assertEquals(new BigDecimal("100.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @Test
    void onlineGoodsChargeTheSameRegularPriceShownByCheckout() {
        goods.setVipPrice(new BigDecimal("80.00"));
        assertEquals(new BigDecimal("100.00"), service.insertAppGoodsOrder(request()).getMoneyPayable());
    }

    @Test
    void rejectsNonPositiveQuantityBeforeReservingStock() {
        AppGoodsOrder request = request();
        request.setGoodsCount(-1L);
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
        verify(goodsMapper, never()).reserveStock(anyLong(), anyLong());
    }

    @Test
    void rejectsUnpublishedGoodsAtServiceBoundary() {
        goods.setStatus("0");
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request()));
        verify(orders, never()).insertAppGoodsOrder(any());
    }

    @Test
    void rejectsRoomBelongingToAnotherProduct() {
        AppGoodsOrder request = travelRequest();
        when(skus.selectAppGoodsSkuBySkuId(201L)).thenReturn(sku(32L, "200"));
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
        verify(orders, never()).insertAppGoodsOrder(any());
    }

    @Test
    void rejectsNegativeMealCountAndForeignMealSku() {
        AppGoodsOrder request = travelRequest();
        request.setSelfSkuId(202L);
        request.setSelfGoodsCount(-1);
        when(skus.selectAppGoodsSkuBySkuId(202L)).thenReturn(sku(31L, "201"));
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
        request.setSelfGoodsCount(1);
        when(skus.selectAppGoodsSkuBySkuId(202L)).thenReturn(sku(32L, "201"));
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
    }

    @Test
    void rejectsMismatchedCalendarNights() {
        AppGoodsOrder request = travelRequest();
        request.setInterCount(8);
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
    }

    @Test
    void acceptsOwnedRoomAndComputesTotalFromServerPrice() {
        AppGoodsOrder created = service.insertAppGoodsOrder(travelRequest());
        assertEquals(new BigDecimal("700.00"), created.getMoneyPayable());
    }

    @Test
    void cancelledOrderCannotStartPaymentAgain() {
        AppGoodsOrder stored = request();
        stored.setOrderId(1L);
        stored.setStatus("2");
        stored.setPayStatus("2");
        when(orders.selectAppGoodsOrderByOrderIdForUpdate(1L)).thenReturn(stored);
        assertEquals("订单已关闭或已支付", service.wxpayPrepay(stored).get("msg"));
    }

    @Test
    void fullyCoveredCouponCreatesConfirmedZeroPaymentOrder() {
        AppGoodsOrder request = request();
        request.setCouponGotIds("88");
        AppGoodsCouponGot got = new AppGoodsCouponGot();
        got.setGotId(88L);
        got.setCouponId(9L);
        got.setUserId(7L);
        got.setIsUsed(0);
        got.setStatus("1");
        AppGoodsCoupon coupon = new AppGoodsCoupon();
        coupon.setStatus("1");
        coupon.setDiscountType("1");
        coupon.setDiscountPrice(new BigDecimal("100.00"));
        AppGoodsCouponMapper couponMapper = mock(AppGoodsCouponMapper.class);
        ReflectionTestUtils.setField(service, "couponMapper", couponMapper);
        when(coupons.selectForUpdate(88L)).thenReturn(got);
        when(couponMapper.selectAppGoodsCouponByCouponId(9L)).thenReturn(coupon);
        when(coupons.markUsed(eq(88L), anyLong(), any())).thenReturn(1);
        AppGoodsOrder created = service.insertAppGoodsOrder(request);
        assertEquals(new BigDecimal("0.00"), created.getMoneyPayable());
        assertEquals("1", created.getPayStatus());
        assertEquals("1", created.getStatus());
        verify(coupons).markUsed(88L, created.getOrderId(), new BigDecimal("100.00"));
    }

    @Test
    void cancellationReleasesNonChannelCouponToo() {
        AppGoodsOrder order = request();
        order.setOrderId(101L);
        order.setCouponGotIds("88");
        AppGoodsCouponGot got = new AppGoodsCouponGot();
        got.setCouponId(9L);
        when(coupons.selectAppGoodsCouponGotByGotId(88L)).thenReturn(got);
        when(coupons.releaseByOrderId(101L)).thenReturn(1);
        service.releaseCouponIfNeeded(order);
        verify(coupons).releaseByOrderId(101L);
    }

    private AppGoodsOrder travelRequest() {
        goods.setGoodsType("hotel");
        goods.setIsSku(1);
        AppGoodsOrder order = request();
        order.setSkuId(201L);
        order.setCheckInDate(Date.valueOf("2027-01-01"));
        order.setCheckOutDate(Date.valueOf("2027-01-08"));
        order.setInterCount(7);
        when(skus.selectAppGoodsSkuBySkuId(201L)).thenReturn(sku(31L, "200"));
        return order;
    }

    private AppGoodsSku sku(Long goodsId, String type) {
        AppGoodsSku sku = new AppGoodsSku();
        sku.setGoodsId(goodsId);
        sku.setStatus("1");
        sku.setSkuType(type);
        sku.setPrice(100.0);
        return sku;
    }

    private AppGoodsOrder request() {
        AppGoodsOrder order = new AppGoodsOrder();
        order.setGoodsId(31L);
        order.setUserId(7L);
        order.setGoodsCount(1L);
        order.setGoodsList(Collections.singletonList(goods));
        return order;
    }
}
