package com.ruoyi.system.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 旅居订单履约状态，编码与飞书“订单状态”一一对应。 */
public final class TravelOrderStatusPolicy
{
    public static final String PENDING_CONFIRMATION = "0";
    public static final String CONFIRMED = "1";
    public static final String CANCELLED = "2";
    public static final String CHECKED_IN = "3";
    public static final String CHECKED_OUT = "4";
    public static final String SETTLED = "5";
    public static final String REFUNDING = "6";
    public static final String REFUNDED = "7";

    private static final Map<String, String> LABELS;
    private static final Map<String, List<String>> MANUAL_TRANSITIONS;

    static
    {
        Map<String, String> labels = new HashMap<>();
        labels.put(PENDING_CONFIRMATION, "待确认");
        labels.put(CONFIRMED, "已确认");
        labels.put(CANCELLED, "已取消");
        labels.put(CHECKED_IN, "已入住");
        labels.put(CHECKED_OUT, "已离店");
        labels.put(SETTLED, "已结算");
        labels.put(REFUNDING, "退款中");
        labels.put(REFUNDED, "已退款");
        LABELS = Collections.unmodifiableMap(labels);

        Map<String, List<String>> transitions = new HashMap<>();
        transitions.put(CONFIRMED, Collections.singletonList(CHECKED_IN));
        transitions.put(CHECKED_IN, Collections.singletonList(CHECKED_OUT));
        transitions.put(CHECKED_OUT, Collections.singletonList(SETTLED));
        MANUAL_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    private TravelOrderStatusPolicy() { }

    public static String label(String status)
    {
        String label = LABELS.get(status);
        if (label == null) throw new IllegalArgumentException("未知旅居订单状态: " + status);
        return label;
    }

    public static boolean canManuallyTransition(String current, String target)
    {
        return MANUAL_TRANSITIONS.getOrDefault(current, Collections.emptyList()).contains(target);
    }

    public static void requireManualTransition(String current, String target)
    {
        label(target);
        if (!canManuallyTransition(current, target))
            throw new IllegalArgumentException("旅居订单状态不能从" + label(current) + "变更为" + label(target));
    }
}
