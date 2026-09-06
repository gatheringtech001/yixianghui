package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

/** Money crosses the web boundary as decimal text, never as floating point. */
public final class GoodsRefundRequest {
    public final String actor, decision, note;
    public final long afterId;
    public final Long refundFen;
    public final boolean returnReceived;
    public String operationId, expected;
    private final Map<String, Object> input;

    private GoodsRefundRequest(String actor, Map<String, Object> input) {
        this.actor = actor; this.input = new TreeMap<>(input);
        afterId = TalentWorkflowAccess.id(input.get("afterId"));
        boolean existing = Arrays.asList("sync-refund", "retry-refund").contains(input.get("kind"));
        decision = existing ? ("sync-refund".equals(input.get("kind")) ? "sync" : "retry") : TalentWorkflowAccess.text(input.get("decision"), 10);
        if (!existing && !"review-refund".equals(input.get("kind"))) fail("不支持的售后操作");
        if (!Arrays.asList("approve", "reject", "sync", "retry").contains(decision)) fail("审核结果无效");
        note = TalentWorkflowAccess.text(input.get("note"), 255);
        if (note.isEmpty()) fail("请填写审核说明或拒绝原因");
        if (input.get("returnReceived") != null && !(input.get("returnReceived") instanceof Boolean)) fail("收货确认无效");
        returnReceived = Boolean.TRUE.equals(input.get("returnReceived"));
        refundFen = "approve".equals(decision) ? cents(input.get("refundAmount")) : null;
        if (!"approve".equals(decision) && input.get("refundAmount") != null) fail("此操作不应包含退款金额");
        Set<String> keys = new HashSet<>(Arrays.asList("kind", "afterId", "note"));
        if (!existing) keys.addAll(Arrays.asList("decision", "refundAmount", "returnReceived"));
        if (!keys.containsAll(input.keySet())) fail("售后操作包含未知字段");
    }
    public static GoodsRefundRequest from(String actor, Map<String, Object> input) {
        return new GoodsRefundRequest(actor, input);
    }
    public GoodsRefundRequest confirmation(Object operation, Object fingerprint) {
        operationId = TalentWorkflowAccess.text(operation, 36);
        expected = TalentWorkflowAccess.text(fingerprint, 64);
        if (!operationId.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}")
                || !expected.matches("[a-f0-9]{64}")) fail("确认编号或快照无效");
        return this;
    }
    public String json() { return JSON.toJSONString(input); }
    public String inputHash() { return hash(expected + ":" + json()); }
    public String kind() { return String.valueOf(input.get("kind")); }
    public static long cents(Object amount) {
        if (!(amount instanceof String) || !((String) amount).matches("(0|[1-9][0-9]{0,5})\\.[0-9]{2}")) {
            fail("退款金额须为两位小数，且不超过 999999.99 元");
        }
        long cents = new BigDecimal((String) amount).movePointRight(2).longValueExact();
        if (cents <= 0) fail("退款金额必须大于零");
        return cents;
    }
    static String yuan(long cents) { return BigDecimal.valueOf(cents, 2).toPlainString(); }
    static String hash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private static void fail(String message) { throw new TalentCenterApiException(400, message); }
}
