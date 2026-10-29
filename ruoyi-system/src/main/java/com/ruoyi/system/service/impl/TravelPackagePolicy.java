package com.ruoyi.system.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppGoodsSkuOption;

/** Fixed-package metadata only: no database, clock, Spring context or pricing inference. */
final class TravelPackagePolicy {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private Integer capacity;
    private boolean includedMealsDeclared;
    private LocalDate stayStart;
    private LocalDate stayEnd;
    private final List<LocalDate[]> excluded = new ArrayList<>();

    private TravelPackagePolicy() { }

    /** Blank legacy params and JSON envelopes without travelPolicy impose no new restrictions. */
    static TravelPackagePolicy fromOptions(List<AppGoodsSkuOption> options) {
        TravelPackagePolicy result = new TravelPackagePolicy();
        boolean found = false;
        int publicRows = 0;
        if (options == null) return result;
        for (AppGoodsSkuOption option : options) {
            if (option == null || !"303".equals(option.getOptionType())
                    || !Integer.valueOf(0).equals(option.getSkuSeqNo())) continue;
            publicRows++;
            String param = option.getOptionParam();
            if (param == null || param.trim().isEmpty()) continue;
            JsonNode root;
            try {
                root = JSON.readTree(param);
            } catch (IOException | RuntimeException ex) {
                throw invalid();
            }
            if (root == null || !root.isObject()) throw invalid();
            if (!root.has("travelPolicy")) continue;
            // Never ignore a duplicate/disabled policy and accidentally remove its restrictions.
            String status = option.getStatus() == null ? "" : option.getStatus().trim();
            if (found || "0".equals(status) || "停用".equals(status)) throw invalid();
            found = true;
            JsonNode policy = root.get("travelPolicy");
            requireKeys(policy, "version", "capacity", "includedMeals", "stayStart", "stayEnd", "excludedStayRanges");
            if (!policy.has("version") || !policy.get("version").isIntegralNumber()
                    || !policy.get("version").canConvertToInt() || policy.get("version").intValue() != 1) throw invalid();
            if (policy.has("capacity")) {
                JsonNode value = policy.get("capacity");
                if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() <= 0) throw invalid();
                result.capacity = value.intValue();
            }
            if (policy.has("includedMeals")) {
                JsonNode value = policy.get("includedMeals");
                if (!value.isTextual() || value.textValue().trim().isEmpty()) throw invalid();
                // Authoritative description, including "不含餐"; never interpret it as a free add-on.
                result.includedMealsDeclared = true;
            }
            if (policy.has("stayStart")) result.stayStart = date(policy.get("stayStart"));
            if (policy.has("stayEnd")) result.stayEnd = date(policy.get("stayEnd"));
            if (result.stayStart != null && result.stayEnd != null && result.stayStart.isAfter(result.stayEnd)) throw invalid();
            if (policy.has("excludedStayRanges")) {
                JsonNode ranges = policy.get("excludedStayRanges");
                if (!ranges.isArray()) throw invalid();
                for (JsonNode range : ranges) {
                    requireKeys(range, "start", "end");
                    if (!range.has("start") || !range.has("end")) throw invalid();
                    LocalDate start = date(range.get("start"));
                    LocalDate end = date(range.get("end"));
                    if (start.isAfter(end)) throw invalid();
                    result.excluded.add(new LocalDate[] {start, end});
                }
            }
        }
        if (found && publicRows != 1) throw invalid();
        return result;
    }

    void validate(AppGoodsOrder order) {
        if (order == null || order.getCheckInDate() == null || order.getCheckOutDate() == null) {
            throw new ServiceException("请选择入住和退房日期");
        }
        // Same local-date semantics as NewGoodsOrderPolicy; never trust a client-supplied night count.
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = java.time.Instant.ofEpochMilli(order.getCheckInDate().getTime()).atZone(zone).toLocalDate();
        LocalDate end = java.time.Instant.ofEpochMilli(order.getCheckOutDate().getTime()).atZone(zone).toLocalDate();
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights <= 0 || nights > Integer.MAX_VALUE || order.getInterCount() == null
                || order.getInterCount().longValue() != nights) throw new ServiceException("入住日期与晚数不一致");
        // [check-in, check-out): the checkout date is not an occupied night.
        LocalDate lastNight = end.minusDays(1);
        if ((stayStart != null && start.isBefore(stayStart)) || (stayEnd != null && lastNight.isAfter(stayEnd))) {
            throw new ServiceException("入住日期不在套餐适用范围内");
        }
        for (LocalDate[] range : excluded) {
            if (!lastNight.isBefore(range[0]) && !start.isAfter(range[1])) {
                throw new ServiceException("入住日期包含套餐不可用日期");
            }
        }
        if (capacity != null) {
            Integer people = order.getSelfGoodsCount();
            Long rooms = order.getGoodsCount();
            // ceil(people / capacity) <= rooms avoids multiplication overflow for large room counts.
            if (people == null || people <= 0 || rooms == null || rooms <= 0
                    || ((long) people + capacity - 1) / capacity > rooms) {
                throw new ServiceException("入住人数无效或超过房型容量");
            }
        }
    }

    boolean hasIncludedMeals() {
        return includedMealsDeclared;
    }

    private static void requireKeys(JsonNode object, String... allowed) {
        if (object == null || !object.isObject()) throw invalid();
        Set<String> keys = new HashSet<>(Arrays.asList(allowed));
        Iterator<String> fields = object.fieldNames();
        while (fields.hasNext()) if (!keys.contains(fields.next())) throw invalid();
    }

    private static LocalDate date(JsonNode value) {
        if (value == null || !value.isTextual() || !value.textValue().matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalid();
        try {
            return LocalDate.parse(value.textValue(), DATE);
        } catch (RuntimeException ex) {
            throw invalid();
        }
    }

    private static ServiceException invalid() {
        return new ServiceException("套餐入住规则配置无效，请联系客服核对");
    }
}
