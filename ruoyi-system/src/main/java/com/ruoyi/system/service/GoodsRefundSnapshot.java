package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

public final class GoodsRefundSnapshot {
    final Map<String, Object> row;
    final List<Map<String, Object>> logs;
    GoodsRefundSnapshot(Map<String, Object> row, List<Map<String, Object>> logs) { this.row = row; this.logs = logs; }
    public long afterId() { return TalentWorkflowAccess.id(row.get("after_id")); }
    public long orderId() { return TalentWorkflowAccess.id(row.get("order_id")); }
    public String text(String key) { return Objects.toString(row.get(key), ""); }
    public String reference() { return "YXHAF" + afterId(); }
    public String fingerprint() {
        Map<String, Object> data = new TreeMap<>(row);
        List<Map<String, Object>> ordered = new ArrayList<>();
        for (Map<String, Object> log : logs) ordered.add(new TreeMap<>(log));
        data.put("refundLogs", ordered);
        return GoodsRefundRequest.hash(JSON.toJSONString(data));
    }
    Map<String, Object> ownLog() {
        for (Map<String, Object> log : logs) if (reference().equals(log.get("agent_refund_no"))) return log;
        return null;
    }
    String reviewBlockedReason() {
        if (!"0".equals(text("status"))) return "此申请已审核，不能重复处理";
        for (Map<String, Object> log : logs)
            if (Arrays.asList("0", "3").contains(String.valueOf(log.get("status")))) return "订单有处理中或待核对的退款，不能重复审核";
        return null;
    }
    public String approvalBlockedReason() {
        String blocked = reviewBlockedReason();
        if (blocked != null) return blocked;
        if (ownLog() != null) return "此申请已有退款记录，请同步结果，不能重复发起";
        if (!Arrays.asList("1", "2").contains(text("after_type"))) return "此单不是退款申请，不能发起退款";
        if (!"1".equals(text("payment_status")) || !"1".equals(text("pay_status"))) return "缺少已支付凭证，不能退款";
        if (!"1".equals(text("payment_count")) || !"wxpay".equals(text("payment_method"))) return "微信支付记录不唯一或支付渠道不匹配";
        if (!text("payment_order_id").equals(text("order_id")) || !text("payment_user_id").equals(text("user_id"))
                || !text("user_id").equals(text("order_user_id")) || text("order_no").isEmpty()
                || !text("out_order_no").equals(text("order_no"))) return "售后、订单与支付凭证不匹配，请核对数据";
        if ("feishu_history".equals(text("order_origin"))) return "历史导入订单不能发起微信退款";
        if (refundableFen() <= 0) return "申请金额或剩余可退金额无效，请核对数据";
        return null;
    }
    long paidFen() { return integral(row.get("payment_fen")); }
    long refundableFen() {
        long requested = moneyFen(row.get("app_refund_money")), goods = moneyFen(row.get("goods_money"));
        if (requested <= 0 || goods <= 0 || paidFen() <= 0) return 0;
        long reserved = 0;
        for (Map<String, Object> log : logs) {
            if (!"2".equals(String.valueOf(log.get("status")))) {
                long amount = integral(log.get("refund_money"));
                if (amount <= 0) return 0;
                reserved += amount;
            }
        }
        return Math.max(0, Math.min(99_999_999L, Math.min(requested, Math.min(goods, paidFen() - reserved))));
    }
    public String refundableAmount() { return GoodsRefundRequest.yuan(refundableFen()); }
    public void validate(GoodsRefundRequest request) {
        if ("retry".equals(request.decision)) {
            if (!canRetry()) throw new TalentCenterApiException(409, "只能继续未确认受理的原退款，不能新建或修改退款金额");
            return;
        }
        if ("sync".equals(request.decision)) {
            if (ownLog() == null) throw new TalentCenterApiException(409, "未找到本次审核的退款记录");
            return;
        }
        String blocked = "approve".equals(request.decision) ? approvalBlockedReason() : reviewBlockedReason();
        if (blocked != null) throw new TalentCenterApiException(409, blocked);
        if (!"approve".equals(request.decision)) return;
        if (request.refundFen == null || request.refundFen > refundableFen())
            throw new TalentCenterApiException(400, "退款金额不能超过申请金额、商品金额及剩余实付金额");
        if ("1".equals(text("after_type")) && !request.returnReceived)
            throw new TalentCenterApiException(400, "退货退款须先确认已收货并核验无误");
    }
    private boolean canRetry() {
        Map<String, Object> log = ownLog();
        return "1".equals(text("status")) && log != null && "0".equals(log.get("status"))
            && Objects.toString(log.get("pay_no"), "").isEmpty() && "1".equals(text("payment_status"))
            && "1".equals(text("pay_status")) && "1".equals(text("payment_count")) && "wxpay".equals(text("payment_method"))
            && text("payment_order_id").equals(text("order_id")) && text("payment_user_id").equals(text("user_id"))
            && text("user_id").equals(text("order_user_id")) && !text("order_no").isEmpty()
            && text("out_order_no").equals(text("order_no")) && integral(log.get("refund_money")) > 0
            && integral(log.get("refund_money")) <= paidFen();
    }
    public Map<String, Object> review() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("afterId", String.valueOf(afterId())); data.put("status", state());
        data.put("statusLabel", stateLabel()); data.put("afterType", text("after_type"));
        data.put("requestedAmount", money(row.get("app_refund_money")));
        data.put("approvedAmount", money(row.get("refund_money")));
        data.put("maxRefundAmount", refundableAmount());
        data.put("canApprove", approvalBlockedReason() == null); data.put("canReject", reviewBlockedReason() == null);
        data.put("canSync", ownLog() != null && !"rejected".equals(state()));
        data.put("canRetry", canRetry());
        data.put("blockedReason", approvalBlockedReason());
        return data;
    }
    public String state() {
        if ("2".equals(text("status"))) return "rejected";
        if ("6".equals(text("status"))) return "succeeded";
        Map<String, Object> log = ownLog();
        if (log != null) {
            if (Arrays.asList("2", "3").contains(String.valueOf(log.get("status")))) return "failed";
            if (Objects.toString(log.get("pay_no"), "").isEmpty()) return "uncertain";
            return "processing";
        }
        if ("0".equals(text("status"))) return "awaiting_review";
        return "5".equals(text("status")) ? "failed" : "processing";
    }
    public String stateLabel() {
        switch (state()) {
            case "awaiting_review": return "待审核";
            case "succeeded": return "退款完成";
            case "rejected": return "已拒绝";
            case "failed": return "退款异常";
            case "uncertain": return "退款结果待核对";
            default: return "微信退款处理中";
        }
    }
    Map<String, Object> result() {
        String state = state(); Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "failed".equals(state) ? "failed" : Arrays.asList("succeeded", "rejected").contains(state) ? "completed" : "pending");
        result.put("refundStatus", state);
        result.put("message", "uncertain".equals(state) ? "退款结果尚未确认，请同步结果，勿重复发起退款" : stateLabel());
        return result;
    }
    List<Map<String, String>> changes(GoodsRefundRequest request) {
        List<Map<String, String>> changes = new ArrayList<>();
        changes.add(change("订单编号", text("order_no"), text("order_no")));
        changes.add(change("商品", text("goods_name"), text("goods_name")));
        changes.add(change("处理结果", stateLabel(), "approve".equals(request.decision) ? "通过并发起微信退款" : "reject".equals(request.decision) ? "拒绝申请，不退款" : "retry".equals(request.decision) ? "继续原退款单，不更改金额" : "查询微信并同步真实退款状态"));
        if ("retry".equals(request.decision)) changes.add(change("原退款金额", GoodsRefundRequest.yuan(integral(Objects.requireNonNull(ownLog()).get("refund_money"))) + " 元", GoodsRefundRequest.yuan(integral(ownLog().get("refund_money"))) + " 元"));
        if (request.refundFen != null) changes.add(change("退款金额", money(row.get("app_refund_money")) + " 元（申请）", GoodsRefundRequest.yuan(request.refundFen) + " 元"));
        if ("1".equals(text("after_type")) && "approve".equals(request.decision)) changes.add(change("退货核验", "待核对", "已收货并核验无误"));
        return changes;
    }
    private static Map<String, String> change(String label, String before, String after) {
        Map<String, String> value = new LinkedHashMap<>(); value.put("label", label); value.put("before", before); value.put("after", after); return value;
    }
    static String money(Object value) {
        if (value == null) return null;
        try { return new BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString(); }
        catch (ArithmeticException | NumberFormatException error) { return null; }
    }
    private static long moneyFen(Object value) {
        if (value == null) return 0;
        try { return new BigDecimal(value.toString()).movePointRight(2).longValueExact(); }
        catch (ArithmeticException | NumberFormatException error) { return 0; }
    }
    static long integral(Object value) {
        if (value == null) return 0;
        try { return new BigDecimal(value.toString()).longValueExact(); }
        catch (ArithmeticException | NumberFormatException error) { return 0; }
    }
}
