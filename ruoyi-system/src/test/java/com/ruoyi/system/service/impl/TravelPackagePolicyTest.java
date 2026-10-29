package com.ruoyi.system.service.impl;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void eligibilityNoticeRequiresStrictBooleanConfirmationAfterJacksonDeserialization() throws Exception {
        TravelPackagePolicy policy = parse("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":\"请确认符合入住资格\"}}");
        ObjectMapper json = new ObjectMapper();
        for (String value : Arrays.asList("true", "false", "\"true\"", "1", "null")) {
            AppGoodsOrder request = json.readValue("{\"params\":{\"travelEligibilityConfirmed\":" + value + "}}", AppGoodsOrder.class);
            request.setCheckInDate(Date.valueOf("2026-01-01"));
            request.setCheckOutDate(Date.valueOf("2026-01-08"));
            request.setInterCount(7);
            assertTrue(request.getParams().containsKey("travelEligibilityConfirmed"), "Jackson must retain BaseEntity.params");
            Object confirmation = request.getParams().get("travelEligibilityConfirmed");
            if ("true".equals(value) || "false".equals(value)) {
                assertEquals(Boolean.class, confirmation.getClass());
            } else if ("\"true\"".equals(value)) {
                assertEquals(String.class, confirmation.getClass());
            } else if ("1".equals(value)) {
                assertEquals(Integer.class, confirmation.getClass());
            } else {
                assertNull(confirmation);
            }
            if ("true".equals(value)) {
                assertDoesNotThrow(() -> policy.validate(request));
            } else {
                assertTrue(assertThrows(ServiceException.class, () -> policy.validate(request))
                        .getMessage().contains("确认入住资格"));
            }
        }
        for (String payload : Arrays.asList("{}", "{\"params\":null}", "{\"params\":{}}")) {
            AppGoodsOrder request = json.readValue(payload, AppGoodsOrder.class);
            request.setCheckInDate(Date.valueOf("2026-01-01"));
            request.setCheckOutDate(Date.valueOf("2026-01-08"));
            request.setInterCount(7);
            assertTrue(assertThrows(ServiceException.class, () -> policy.validate(request))
                    .getMessage().contains("确认入住资格"));
        }
    }

    @Test
    void acceptsEligibilityNoticeWithRawLengthAtMost100AndNonblankTrimmedText() {
        String hundred = String.join("", Collections.nCopies(100, "资"));
        for (String text : Arrays.asList("资格声明", "  资格声明  ", hundred, " " + hundred.substring(1))) {
            TravelPackagePolicy policy = parse("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":\"" + text + "\"}}");
            AppGoodsOrder request = order("2026-01-01", "2026-01-08", 1, 1);
            request.getParams().put("travelEligibilityConfirmed", Boolean.TRUE);
            assertDoesNotThrow(() -> policy.validate(request));
        }
        for (String text : Arrays.asList(hundred + "资", " " + hundred, hundred + " ", "  " + hundred + "  ")) {
            assertThrows(ServiceException.class, () -> parse("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":\"" + text + "\"}}"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "false", "1", "[]", "{}", "\"\"", "\"   \"", "\"\\t\\n\""})
    void rejectsNontextOrBlankEligibilityNotice(String value) {
        assertThrows(ServiceException.class, () -> parse("{\"travelPolicy\":{\"version\":1,\"eligibilityNotice\":" + value + "}}"));
    }

    @Test
    void absentNoticeDoesNotRequireOrInterpretConfirmation() {
        for (String metadata : Arrays.asList(null, "{}", "{\"travelPolicy\":{\"version\":1,\"capacity\":2}}")) {
            TravelPackagePolicy policy = parse(metadata);
            AppGoodsOrder request = order("2026-01-01", "2026-01-08", 1, 1);
            assertDoesNotThrow(() -> policy.validate(request));
            for (Object value : Arrays.asList(null, Boolean.FALSE, "true", 1)) {
                request.getParams().put("travelEligibilityConfirmed", value);
                assertDoesNotThrow(() -> policy.validate(request));
            }
        }
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
