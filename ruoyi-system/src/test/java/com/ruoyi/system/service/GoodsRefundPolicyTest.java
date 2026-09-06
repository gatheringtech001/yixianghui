package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import static org.junit.jupiter.api.Assertions.*;

class GoodsRefundPolicyTest {
    @Test void amountsMustBeExactPositiveCents() {
        assertEquals(1L, GoodsRefundRequest.cents("0.01"));
        for (String value : Arrays.asList("0", "0.00", "-1.00", "1.001", "1e2", "NaN", "1000000.00"))
            assertThrows(TalentCenterApiException.class, () -> GoodsRefundRequest.cents(value));
    }
    @Test void refundIsBoundedByRequestGoodsAndRealPayment() {
        GoodsRefundSnapshot snapshot = snapshot();
        assertEquals("1.00", snapshot.refundableAmount());
        snapshot.validate(request("approve", "1.00", false));
        assertThrows(TalentCenterApiException.class, () -> snapshot.validate(request("approve", "1.01", false)));
    }
    @Test void unpaidOrMismatchedPaymentCannotBeApproved() {
        for (String field : Arrays.asList("payment_status", "payment_order_id", "payment_user_id", "out_order_no")) {
            Map<String, Object> row = row(); row.put(field, "invalid");
            GoodsRefundSnapshot snapshot = new GoodsRefundSnapshot(row, Collections.emptyList());
            assertNotNull(snapshot.approvalBlockedReason());
            assertThrows(TalentCenterApiException.class, () -> snapshot.validate(request("approve", "1.00", false)));
        }
    }
    @Test void returnedGoodsRequireExplicitReceiptAndExchangeNeverIssuesMoney() {
        Map<String, Object> row = row(); row.put("after_type", "1");
        GoodsRefundSnapshot snapshot = new GoodsRefundSnapshot(row, Collections.emptyList());
        assertThrows(TalentCenterApiException.class, () -> snapshot.validate(request("approve", "1.00", false)));
        snapshot.validate(request("approve", "1.00", true));
        row.put("after_type", "3");
        assertThrows(TalentCenterApiException.class, () -> new GoodsRefundSnapshot(row, Collections.emptyList()).validate(request("approve", "1.00", true)));
    }
    @Test void pendingOrUncertainRefundsBlockAnotherApproval() {
        Map<String, Object> log = refund();
        GoodsRefundSnapshot snapshot = new GoodsRefundSnapshot(row(), Collections.singletonList(log));
        assertNotNull(snapshot.approvalBlockedReason());
        assertThrows(TalentCenterApiException.class, () -> snapshot.validate(request("approve", "1.00", false)));
    }
    @Test void rejectionRequiresReasonButDoesNotNeedAValidPayment() {
        Map<String, Object> row = row(); row.put("payment_status", null);
        new GoodsRefundSnapshot(row, Collections.emptyList()).validate(request("reject", null, false));
        Map<String, Object> input = input("reject", null, false); input.put("note", " ");
        assertThrows(TalentCenterApiException.class, () -> GoodsRefundRequest.from("actor", input));
    }
    @Test void stateFingerprintChangesWhenFinancialOrReviewDataChanges() {
        String original = snapshot().fingerprint();
        for (String field : Arrays.asList("app_refund_money", "goods_money", "payment_fen", "status", "remark")) {
            Map<String, Object> row = row(); row.put(field, "changed");
            assertNotEquals(original, new GoodsRefundSnapshot(row, Collections.emptyList()).fingerprint());
        }
    }
    @Test void continuingAnUncertainRefundUsesTheSamePaymentRequest() {
        Map<String, Object> row = row(); row.put("status", "1"); row.put("remark", "原审核意见");
        GoodsRefundSnapshot snapshot = new GoodsRefundSnapshot(row, Collections.singletonList(refund()));
        Map<String, Object> input = new LinkedHashMap<>(); input.put("kind", "retry-refund"); input.put("afterId", "8"); input.put("note", "继续原退款");
        GoodsRefundRequest retry = GoodsRefundRequest.from("actor", input); snapshot.validate(retry);
        com.wechat.pay.java.service.refund.model.CreateRequest request = GoodsRefundGateway.request(snapshot, retry, "https://callback.invalid/");
        assertEquals("YXHAF8", request.getOutRefundNo()); assertEquals("TEST20", request.getOutTradeNo());
        assertEquals(100L, request.getAmount().getRefund()); assertEquals(100L, request.getAmount().getTotal());
        assertEquals("原审核意见", request.getReason());
        input.put("refundAmount", "2.00");
        assertThrows(TalentCenterApiException.class, () -> GoodsRefundRequest.from("actor", input));
    }
    @Test void refundApprovalAndRecoveryAreAdministratorOnly() {
        TalentWorkflowAccess access = org.mockito.Mockito.mock(TalentWorkflowAccess.class);
        GoodsRefundReviewService review = org.mockito.Mockito.mock(GoodsRefundReviewService.class);
        org.mockito.Mockito.doThrow(new TalentCenterApiException(403, "管理员权限 required")).when(access).admin("actor", "self");
        TalentRefundService service = new TalentRefundService(access, review);
        assertThrows(TalentCenterApiException.class, () -> service.preview("actor", "self", input("approve", "1.00", false)));
        assertThrows(TalentCenterApiException.class, () -> service.execute("actor", "self", Collections.emptyMap()));
        org.mockito.Mockito.verifyNoInteractions(review);
    }
    static GoodsRefundSnapshot snapshot() { return new GoodsRefundSnapshot(row(), Collections.emptyList()); }
    static GoodsRefundRequest request(String decision, String amount, boolean received) { return GoodsRefundRequest.from("actor", input(decision, amount, received)); }
    static Map<String, Object> input(String decision, String amount, boolean received) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("kind", "review-refund"); input.put("afterId", "8"); input.put("decision", decision);
        input.put("refundAmount", amount); input.put("returnReceived", received); input.put("note", "已核对申请"); return input;
    }
    static Map<String, Object> row() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("after_id", 8L); row.put("order_id", 20L); row.put("user_id", 5L); row.put("order_user_id", 5L);
        row.put("status", "0"); row.put("after_type", "2"); row.put("out_order_no", "TEST20"); row.put("order_no", "TEST20");
        row.put("app_refund_money", new BigDecimal("1.00")); row.put("goods_money", new BigDecimal("1.00"));
        row.put("payment_fen", new BigDecimal("100")); row.put("payment_status", "1"); row.put("payment_method", "wxpay");
        row.put("payment_order_id", 20L); row.put("payment_user_id", 5L); row.put("payment_count", 1L);
        row.put("pay_status", "1"); row.put("order_origin", "mini_program"); row.put("goods_name", "测试课程"); return row;
    }
    static Map<String, Object> refund() {
        Map<String, Object> log = new LinkedHashMap<>(); log.put("log_id", 1L); log.put("agent_refund_no", "YXHAF8");
        log.put("refund_money", new BigDecimal("100")); log.put("status", "0"); return log;
    }
}
