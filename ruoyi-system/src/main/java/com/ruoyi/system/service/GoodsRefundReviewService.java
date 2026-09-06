package com.ruoyi.system.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppGoodsOrderAfter;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;

@Service
public class GoodsRefundReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(GoodsRefundReviewService.class);
    private final GoodsRefundStore store;
    private final GoodsRefundGateway gateway;
    private final IAppGoodsOrderService orders;
    public GoodsRefundReviewService(GoodsRefundStore store, GoodsRefundGateway gateway, @Lazy IAppGoodsOrderService orders) {
        this.store = store; this.gateway = gateway; this.orders = orders;
    }
    public GoodsRefundSnapshot detail(long id) { return store.read(id); }
    public Map<String, Object> preview(GoodsRefundRequest request) {
        GoodsRefundSnapshot snapshot = store.read(request.afterId); snapshot.validate(request);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", "approve".equals(request.decision) ? "确认通过并退款" : "reject".equals(request.decision) ? "确认拒绝退款申请" : "retry".equals(request.decision) ? "继续原退款" : "同步微信退款结果");
        result.put("fingerprint", snapshot.fingerprint()); result.put("changes", snapshot.changes(request)); return result;
    }
    public Map<String, Object> execute(GoodsRefundRequest request) {
        GoodsRefundStore.Prepared prepared = store.prepare(request);
        if (!prepared.send) return store.read(request.afterId).result();
        Map<String, Object> result;
        // Reservation commits before external IO. A timeout cannot roll back the payment intent.
        try {
            Refund evidence = "sync".equals(request.decision) ? gateway.query(prepared.snapshot.reference())
                : gateway.create(prepared.snapshot, request);
            if (evidence == null) throw new TalentCenterApiException(502, "微信未返回退款凭证");
            result = store.record(evidence, orders::completeReviewedRefund);
        } catch (RuntimeException error) {
            LOG.warn("Refund result unconfirmed afterId={}, operation={}, cause={}", request.afterId, request.operationId, error.getClass().getSimpleName());
            result = store.read(request.afterId).result();
            if (!"completed".equals(result.get("status"))) {
                result.put("status", "pending"); result.put("refundStatus", "uncertain");
                result.put("message", error instanceof TalentCenterApiException ? error.getMessage()
                    : "微信退款结果尚未确认，请同步结果，勿重复发起退款");
            }
        }
        store.saveResult(request.operationId, result);
        return result;
    }
    public void notification(RefundNotification notification) {
        store.record(GoodsRefundGateway.evidence(notification), orders::completeReviewedRefund);
    }
    public Map<String, Object> refresh(long afterId) {
        GoodsRefundSnapshot snapshot = store.read(afterId);
        return store.record(gateway.query(snapshot.reference()), orders::completeReviewedRefund);
    }
    public AjaxResult nativeReview(AppGoodsOrderAfter after) {
        if (after == null || after.getAfterId() == null) return AjaxResult.error("售后单无效");
        if (!Arrays.asList("1", "2").contains(after.getStatus())) return AjaxResult.error("审核结果无效");
        try {
            Map<String, Object> input = new LinkedHashMap<>();
            input.put("kind", "review-refund"); input.put("afterId", after.getAfterId().toString());
            input.put("decision", "1".equals(after.getStatus()) ? "approve" : "reject");
            input.put("refundAmount", "1".equals(after.getStatus()) ? GoodsRefundSnapshot.money(after.getRefundMoney()) : null);
            input.put("returnReceived", Boolean.TRUE.equals(after.getReturnReceived()));
            input.put("note", after.getRemark() == null || after.getRemark().trim().isEmpty() ? "管理员核对通过" : after.getRemark());
            if ("2".equals(after.getStatus()) && (after.getRemark() == null || after.getRemark().trim().isEmpty())) return AjaxResult.error("请填写拒绝原因");
            GoodsRefundRequest command = GoodsRefundRequest.from("backend:" + after.getUpdateBy(), input);
            command.confirmation(UUID.randomUUID().toString(), preview(command).get("fingerprint"));
            Map<String, Object> result = execute(command);
            return "uncertain".equals(result.get("refundStatus")) || "failed".equals(result.get("status"))
                ? AjaxResult.error(String.valueOf(result.get("message"))) : AjaxResult.success(String.valueOf(result.get("message")), result);
        } catch (TalentCenterApiException error) { return AjaxResult.error(error.getMessage()); }
    }
}
