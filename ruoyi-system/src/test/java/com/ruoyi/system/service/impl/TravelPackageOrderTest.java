package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TravelPackageOrderTest {
    private AppGoodsOrderServiceImpl service;
    private AppGoodsOrderMapper orders;
    private AppGoodsOrderDetailMapper details;
    private AppGoodsSkuMapper skus;
    private AppGoodsSkuOptionMapper options;
    private AppGoodsSku room;
    private AppGoodsSku pack;
    private AppGoodsSku meal;
    private AppGoodsSkuOption metadata;
    private AppGoodsSkuOption total;
    private AppGoodsOrder request;

    @BeforeEach
    void setup() {
        service = new AppGoodsOrderServiceImpl();
        orders = mock(AppGoodsOrderMapper.class);
        details = mock(AppGoodsOrderDetailMapper.class);
        skus = mock(AppGoodsSkuMapper.class);
        options = mock(AppGoodsSkuOptionMapper.class);
        AppGoodsMapper goodsMapper = mock(AppGoodsMapper.class);
        ReflectionTestUtils.setField(service, "appGoodsMapper", goodsMapper);
        ReflectionTestUtils.setField(service, "appGoodsOrderMapper", orders);
        ReflectionTestUtils.setField(service, "orderDetailMapper", details);
        ReflectionTestUtils.setField(service, "appGoodsSkuMapper", skus);
        ReflectionTestUtils.setField(service, "appGoodsSkuOptionMapper", options);
        ReflectionTestUtils.setField(service, "couponGotMapper", mock(AppGoodsCouponGotMapper.class));
        AppGoods goods = new AppGoods();
        goods.setGoodsId(31L);
        goods.setGoodsType("hotel");
        goods.setStatus("1");
        goods.setIsSku(1);
        when(goodsMapper.selectAppGoodsByGoodsId(31L)).thenReturn(goods);
        room = sku(100L, "200", 0.0);
        pack = sku(101L, "202", 0.0);
        pack.setParSkuId("100");
        pack.setSkuName("8天7晚");
        meal = sku(102L, "201", 30.0);
        when(skus.selectAppGoodsSkuBySkuId(100L)).thenReturn(room);
        when(skus.selectAppGoodsSkuBySkuId(101L)).thenReturn(pack);
        when(skus.selectAppGoodsSkuBySkuId(102L)).thenReturn(meal);
        metadata = TravelPackagePolicyTest.option("{\"travelPolicy\":{\"version\":1,\"capacity\":2,"
                + "\"includedMeals\":\"三餐\",\"stayStart\":\"2026-01-01\",\"stayEnd\":\"2026-12-31\","
                + "\"excludedStayRanges\":[{\"start\":\"2026-07-10\",\"end\":\"2026-08-20\"}]}}");
        metadata.setOptionValue("8");
        metadata.setOptionValueUnit("天");
        total = new AppGoodsSkuOption();
        total.setOptionType("302");
        total.setOptionValue("1000.00");
        total.setSkuSeqNo(1);
        when(options.selectAppGoodsSkuOptionList(any())).thenAnswer(call -> {
            AppGoodsSkuOption query = call.getArgument(0);
            return query.getSkuSeqNo() == null ? Arrays.asList(metadata, total) : Arrays.asList(total);
        });
        when(orders.insertAppGoodsOrder(any())).thenAnswer(call -> {
            ((AppGoodsOrder) call.getArgument(0)).setOrderId(99L);
            return 1;
        });
        when(orders.updateAppGoodsOrder(any())).thenReturn(1);
        when(details.insertAppGoodsOrderDetail(any())).thenReturn(1);
        request = TravelPackagePolicyTest.order("2026-01-01", "2026-01-08", 1, 1);
        request.setGoodsId(31L);
        request.setUserId(7L);
        request.setSkuId(101L);
        request.setSkuSeqNo(1);
        request.setMoneyPayable(new BigDecimal("0.01"));
    }

    @Test
    void zeroNightlyPriceFixedPackageUsesServer302AndFullRoomPriceForOnePerson() {
        AppGoodsOrder created = service.insertAppGoodsOrder(request);
        assertEquals(new BigDecimal("1000.00"), created.getMoneyPayable());
        assertEquals(new BigDecimal("1000.00"), created.getPayMoney());
        assertEquals(Integer.valueOf(7), created.getInterCount());
        assertEquals(request.getCheckInDate(), created.getOrderDetailList().get(0).getOrderStartDate());
        assertEquals(request.getCheckOutDate(), created.getOrderDetailList().get(0).getOrderEndDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"travelEligibilityConfirmed\":false}",
            "{\"travelEligibilityConfirmed\":\"true\"}", "{\"travelEligibilityConfirmed\":1}",
            "{\"travelEligibilityConfirmed\":null}"})
    void jacksonRequestWithoutStrictEligibilityConfirmationCannotWriteOrders(String params) throws Exception {
        metadata.setOptionParam("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":\"请确认符合入住资格\"}}");
        request = jacksonOrder(params);
        reject("确认入住资格");
    }

    @Test
    void jacksonBooleanTrueEligibilityDeclarationAllowsNormalOrderWrites() throws Exception {
        metadata.setOptionParam("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":\"请确认符合入住资格\"}}");
        request = jacksonOrder("{\"travelEligibilityConfirmed\":true,\"dataScope\":\"untrusted\",\"tenantId\":999}");
        assertSame(Boolean.TRUE, request.getParams().get("travelEligibilityConfirmed"));
        AppGoodsOrder created = service.insertAppGoodsOrder(request);
        assertEquals(new BigDecimal("1000.00"), created.getMoneyPayable());
        assertEquals(Collections.singletonMap("travelEligibilityConfirmed", Boolean.TRUE), created.getParams());
        assertNotSame(request.getParams(), created.getParams());
        verify(orders).insertAppGoodsOrder(any());
        verify(orders).updateAppGoodsOrder(any());
        verify(details).insertAppGoodsOrderDetail(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"travelEligibilityConfirmed\":false}",
            "{\"travelEligibilityConfirmed\":true,\"dataScope\":\"untrusted\"}",
            "{\"travelEligibilityConfirmed\":\"true\"}", "{\"travelEligibilityConfirmed\":1}",
            "{\"travelEligibilityConfirmed\":null}"})
    void noEligibilityNoticeKeepsLegacyOrderWritesWithoutConfirmation(String params) throws Exception {
        request = jacksonOrder(params);
        Object confirmation = request.getParams().get("travelEligibilityConfirmed");
        AppGoodsOrder created = service.insertAppGoodsOrder(request);
        assertEquals(new BigDecimal("1000.00"), created.getMoneyPayable());
        assertEquals(confirmation instanceof Boolean
                ? Collections.singletonMap("travelEligibilityConfirmed", confirmation)
                : Collections.emptyMap(), created.getParams());
        verify(orders).insertAppGoodsOrder(any());
        verify(details).insertAppGoodsOrderDetail(any());
    }

    private AppGoodsOrder jacksonOrder(String params) throws Exception {
        return new ObjectMapper().readValue("{\"goodsId\":31,\"userId\":7,\"skuId\":101,\"skuSeqNo\":1,"
                + "\"checkInDate\":\"2026-01-01\",\"checkOutDate\":\"2026-01-08\","
                + "\"goodsCount\":1,\"selfGoodsCount\":1,\"interCount\":7,\"params\":" + params + "}", AppGoodsOrder.class);
    }

    @Test
    void multipleRoomsMultiply302NotOccupancy() {
        request.setSelfGoodsCount(3);
        request.setGoodsCount(2L);
        assertEquals(new BigDecimal("2000.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"三餐", "不含餐"})
    void metadataNeverMakesAPositive201AddonFree(String mealText) {
        metadata.setOptionParam("{\"travelPolicy\":{\"version\":1,\"includedMeals\":\"" + mealText + "\"}}");
        request.setSelfSkuId(102L);
        reject("套餐供餐已确定");
    }

    @Test
    void metadataWithoutMealDeclarationKeepsPositive201Pricing() {
        request.setSelfSkuId(102L);
        meal.setPrice(30.0);
        for (String json : Arrays.asList(
                "{\"travelPolicy\":{\"version\":1,\"capacity\":2}}",
                "{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-01-01\",\"stayEnd\":\"2026-12-31\"}}")) {
            metadata.setOptionParam(json);
            AppGoodsOrder created = service.insertAppGoodsOrder(request);
            assertEquals(new BigDecimal("210.00"), created.getMoneyPayable().subtract(new BigDecimal("1000.00")));
            assertEquals(new BigDecimal("1210.00"), created.getPayMoney());
            assertEquals(Long.valueOf(102L), created.getSelfSkuId());
        }
    }

    @Test
    void metadataAllowsZeroPrice201WithoutInventingMealBenefits() {
        metadata.setOptionParam("{\"travelPolicy\":{\"version\":1,\"includedMeals\":\"不含餐\"}}");
        request.setSelfSkuId(102L);
        meal.setPrice(0.0);
        assertEquals(new BigDecimal("1000.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"周旅居", "半月旅居", "月旅居"})
    void explicitPublic303NightCountOutranksPackageName(String name) {
        pack.setSkuName(name);
        metadata.setOptionValue("7");
        metadata.setOptionValueUnit("晚");
        assertEquals(new BigDecimal("1000.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @Test
    void combination303QuantityCannotOverridePackageNights() {
        pack.setSkuName("周旅居");
        AppGoodsSkuOption quantity = TravelPackagePolicyTest.option(null);
        quantity.setOptionValue("99");
        quantity.setOptionValueUnit("晚");
        quantity.setSkuSeqNo(1);
        metadata.setOptionValue("6");
        metadata.setOptionValueUnit("晚");
        request.setCheckOutDate(Date.valueOf("2026-01-07"));
        request.setInterCount(6);
        doReturn(Arrays.asList(quantity, metadata, total)).when(options).selectAppGoodsSkuOptionList(any());
        assertEquals(new BigDecimal("1000.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @Test
    void legacyNoMetadataRetainsIndependent201MealPricingAndNoNewCapacityLimit() {
        metadata.setOptionParam(null);
        request.setSelfSkuId(102L);
        request.setSelfGoodsCount(3);
        assertEquals(new BigDecimal("1630.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
    }

    @Test
    void rejectsOverCapacityBeforeAnyOrderWrite() {
        request.setSelfGoodsCount(3);
        reject("容量");
    }

    @Test
    void malformedPolicyCannotFallBackToLegacy() {
        metadata.setOptionParam("{\"travelPolicy\":{\"version\":1,\"capacity\":\"2\"}}");
        reject("配置无效");
    }

    @Test
    void missingPeopleWithCapacityCannotBeInterpretedAsOnePerson() {
        request.setSelfGoodsCount(null);
        reject("人数");
    }

    @Test
    void actualOrderDatesRejectExcludedNightEvenWithoutClientNightCount() {
        request.setCheckInDate(Date.valueOf("2026-07-04"));
        request.setCheckOutDate(Date.valueOf("2026-07-11"));
        request.setInterCount(null);
        reject("不可用日期");
    }

    @Test
    void rejectsCalendarTamperingAndPackageDurationMismatch() {
        request.setInterCount(1);
        reject("晚数");
        request.setCheckOutDate(Date.valueOf("2026-01-07"));
        request.setInterCount(null);
        reject("套餐不符");
    }

    @Test
    void zeroPriceParentCanServeFixedCombinationButNotCustomNights() {
        request.setSkuId(100L);
        assertEquals(new BigDecimal("1000.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
        request.setSkuSeqNo(0);
        assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request));
    }

    @Test
    void positivePriceCustomNightPathKeepsLegacyBehavior() {
        room.setPrice(100.0);
        request.setSkuId(100L);
        request.setSkuSeqNo(0);
        request.setSelfSkuId(102L);
        request.setSelfGoodsCount(3);
        assertEquals(new BigDecimal("1330.00"), service.insertAppGoodsOrder(request).getMoneyPayable());
        verifyNoInteractions(options);
    }

    @ParameterizedTest
    @ValueSource(strings = {"200", "202", "302", "", "garbage"})
    void rejectsNon201MealSkuEvenWhenMealsAreIncluded(String wrongType) {
        request.setSelfSkuId(102L);
        meal.setSkuType(wrongType);
        reject("供餐方案");
    }

    @Test
    void stillChecksIncludedMealSkuOwnershipAndStatus() {
        request.setSelfSkuId(102L);
        meal.setStatus("0");
        reject("停用");
        meal.setStatus("1");
        meal.setGoodsId(32L);
        reject("不属于");
    }

    @Test
    void cannotBypassDisabledParentBySubmittingAnEnabledChild() {
        room.setStatus("0");
        reject("停用");
    }

    @Test
    void checksParentOwnershipTypeAndValidity() {
        room.setGoodsId(32L);
        reject("不属于");
        room.setGoodsId(31L);
        room.setSkuType("201");
        reject("房型无效");
        room.setSkuType("200");
        room.setInvalidTime(new java.util.Date(1L));
        reject("有效期");
        room.setInvalidTime(null);
        room.setValidTime(new java.util.Date(Long.MAX_VALUE));
        reject("有效期");
    }

    @Test
    void rejectsInvalidMissingOrNonexistentParentRatherThanTreatingAsStandalone() {
        pack.setParSkuId(null);
        reject("配置无效");
        pack.setParSkuId("not-id");
        reject("配置无效");
        pack.setParSkuId("999");
        reject("不属于");
    }

    private void reject(String message) {
        assertTrue(assertThrows(ServiceException.class, () -> service.insertAppGoodsOrder(request))
                .getMessage().contains(message));
        verify(orders, never()).insertAppGoodsOrder(any());
        verify(orders, never()).updateAppGoodsOrder(any());
        verifyNoInteractions(details);
    }

    private AppGoodsSku sku(long id, String type, double price) {
        AppGoodsSku sku = new AppGoodsSku();
        sku.setSkuId(id);
        sku.setGoodsId(31L);
        sku.setStatus("1");
        sku.setSkuType(type);
        sku.setPrice(price);
        return sku;
    }
}
