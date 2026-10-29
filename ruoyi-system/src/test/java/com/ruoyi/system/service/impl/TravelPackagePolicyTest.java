package com.ruoyi.system.service.impl;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppGoodsSkuOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class TravelPackagePolicyTest {
    @Test
    void acceptsOptionalFieldsAndLegacyMetadataEnvelopes() {
        for (String json : Arrays.asList(null, "", "  ", "{}", "{\"source\":\"legacy\"}",
                "{\"travelPolicy\":{\"version\":1}}")) {
            TravelPackagePolicy policy = parse(json);
            assertDoesNotThrow(() -> policy.validate(order("2026-01-01", "2026-01-08", null, 1)));
            assertFalse(policy.hasIncludedMeals());
        }
        assertDoesNotThrow(() -> TravelPackagePolicy.fromOptions(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not-json", "null", "[]", "{} {}", "{", "{\"travelPolicy\":null}",
        "{\"travelPolicy\":[]}", "{\"travelPolicy\":{}}",
        "{\"travelPolicy\":{\"version\":2}}", "{\"travelPolicy\":{\"version\":\"1\"}}",
        "{\"travelPolicy\":{\"version\":1.0}}", "{\"travelPolicy\":{\"version\":null}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":0}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":-1}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":\"2\"}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":2.0}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":null}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":2147483648}}",
        "{\"travelPolicy\":{\"version\":1,\"includedMeals\":false}}",
        "{\"travelPolicy\":{\"version\":1,\"includedMeals\":null}}",
        "{\"travelPolicy\":{\"version\":1,\"includedMeals\":\" \"}}",
        "{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-02-29\"}}",
        "{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-1-01\"}}",
        "{\"travelPolicy\":{\"version\":1,\"stayEnd\":null}}",
        "{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-01-02\",\"stayEnd\":\"2026-01-01\"}}",
        "{\"travelPolicy\":{\"version\":1,\"excludedStayRanges\":null}}",
        "{\"travelPolicy\":{\"version\":1,\"excludedStayRanges\":{}}}",
        "{\"travelPolicy\":{\"version\":1,\"excludedStayRanges\":[{}]}}",
        "{\"travelPolicy\":{\"version\":1,\"excludedStayRanges\":[null]}}",
        "{\"travelPolicy\":{\"version\":1,\"excludedStayRanges\":[{\"start\":\"2026-01-02\",\"end\":\"2026-01-01\"}]}}",
        "{\"travelPolicy\":{\"version\":1,\"capcity\":2}}",
        "{\"travelPolicy\":{\"version\":1,\"capacity\":2,\"capacity\":20}}",
        "{\"travelPolicy\":{\"version\":1},\"travelPolicy\":{\"version\":1}}"
    })
    void rejectsMalformedPresentMetadata(String json) {
        assertThrows(ServiceException.class, () -> parse(json));
    }

    @Test
    void rejectsAmbiguousAndDisabledPolicyRows() {
        AppGoodsSkuOption option = option("{\"travelPolicy\":{\"version\":1}}");
        assertThrows(ServiceException.class, () -> TravelPackagePolicy.fromOptions(Arrays.asList(option, option)));
        assertThrows(ServiceException.class, () -> TravelPackagePolicy.fromOptions(Arrays.asList(option, option(null))));
        option.setStatus("0");
        assertThrows(ServiceException.class, () -> TravelPackagePolicy.fromOptions(Collections.singletonList(option)));
    }

    @Test
    void onlyPublic303SeqZeroCarriesPolicyNotNamesDescriptionsOrCombinationQuantities() {
        AppGoodsSkuOption option = option("broken");
        option.setSkuSeqNo(1);
        assertDoesNotThrow(() -> TravelPackagePolicy.fromOptions(Collections.singletonList(option)));
        option.setSkuSeqNo(0);
        option.setOptionType("304");
        assertDoesNotThrow(() -> TravelPackagePolicy.fromOptions(Collections.singletonList(option)));
    }

    @Test
    void chargesFullRoomForOnePersonAndAllowsCapacityTimesRoomsWithoutOverflow() {
        TravelPackagePolicy policy = parse("{\"travelPolicy\":{\"version\":1,\"capacity\":2}}");
        assertDoesNotThrow(() -> policy.validate(order("2026-01-01", "2026-01-08", 1, 1)));
        assertDoesNotThrow(() -> policy.validate(order("2026-01-01", "2026-01-08", 4, 2)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2026-01-01", "2026-01-08", 5, 2)));
        assertDoesNotThrow(() -> policy.validate(order("2026-01-01", "2026-01-08", 1, Long.MAX_VALUE)));
        for (Integer people : Arrays.asList(null, 0, -1)) {
            assertThrows(ServiceException.class, () -> policy.validate(order("2026-01-01", "2026-01-08", people, 1)));
        }
    }

    @Test
    void validatesEveryNightUsingHalfOpenOrderAndInclusivePolicyBounds() {
        TravelPackagePolicy policy = parse("{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-01-01\","
                + "\"stayEnd\":\"2026-12-31\",\"excludedStayRanges\":[{\"start\":\"2026-07-10\",\"end\":\"2026-08-20\"}]}}");
        assertDoesNotThrow(() -> policy.validate(order("2026-01-01", "2026-01-08", 1, 1)));
        assertDoesNotThrow(() -> policy.validate(order("2026-12-25", "2027-01-01", 1, 1)));
        assertDoesNotThrow(() -> policy.validate(order("2026-07-03", "2026-07-10", 1, 1)));
        assertDoesNotThrow(() -> policy.validate(order("2026-08-21", "2026-08-28", 1, 1)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2025-12-31", "2026-01-07", 1, 1)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2026-12-26", "2027-01-02", 1, 1)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2026-07-04", "2026-07-11", 1, 1)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2026-08-20", "2026-08-27", 1, 1)));
        assertThrows(ServiceException.class, () -> policy.validate(order("2026-07-01", "2026-09-01", 1, 1)));
    }

    @Test
    void permitsIndependentOptionalDateBoundsAndEmptyExclusions() {
        assertDoesNotThrow(() -> parse("{\"travelPolicy\":{\"version\":1,\"stayEnd\":\"2026-01-07\",\"excludedStayRanges\":[]}}")
                .validate(order("2026-01-01", "2026-01-08", 1, 1)));
        assertThrows(ServiceException.class, () -> parse("{\"travelPolicy\":{\"version\":1,\"stayStart\":\"2026-01-02\"}}")
                .validate(order("2026-01-01", "2026-01-08", 1, 1)));
    }

    @Test
    void requiresActualOrderDatesAndMatchingNights() {
        TravelPackagePolicy policy = parse(null);
        AppGoodsOrder order = order("2026-01-01", "2026-01-08", 1, 1);
        order.setInterCount(1);
        assertThrows(ServiceException.class, () -> policy.validate(order));
        order.setCheckOutDate(order.getCheckInDate());
        assertThrows(ServiceException.class, () -> policy.validate(order));
        order.setCheckInDate(null);
        assertThrows(ServiceException.class, () -> policy.validate(order));
    }

    private TravelPackagePolicy parse(String json) {
        return TravelPackagePolicy.fromOptions(Collections.singletonList(option(json)));
    }

    static AppGoodsSkuOption option(String json) {
        AppGoodsSkuOption option = new AppGoodsSkuOption();
        option.setOptionType("303");
        option.setSkuSeqNo(0);
        option.setOptionParam(json);
        option.setStatus("1");
        return option;
    }

    static AppGoodsOrder order(String start, String end, Integer people, long rooms) {
        AppGoodsOrder order = new AppGoodsOrder();
        order.setCheckInDate(Date.valueOf(start));
        order.setCheckOutDate(Date.valueOf(end));
        order.setInterCount((int) java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.parse(start), java.time.LocalDate.parse(end)));
        order.setSelfGoodsCount(people);
        order.setGoodsCount(rooms);
        return order;
    }
}
