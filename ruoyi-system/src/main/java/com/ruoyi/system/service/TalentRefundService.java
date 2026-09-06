package com.ruoyi.system.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentRefundService {
    private final TalentWorkflowAccess access;
    private final GoodsRefundReviewService refunds;
    public TalentRefundService(TalentWorkflowAccess access, GoodsRefundReviewService refunds) { this.access = access; this.refunds = refunds; }
    public Map<String, Object> preview(String actor, String scope, Map<String, Object> input) {
        access.admin(actor, scope); return refunds.preview(GoodsRefundRequest.from(actor, input));
    }
    public Map<String, Object> execute(String actor, String scope, Map<String, Object> request) {
        access.admin(actor, scope);
        GoodsRefundRequest input = GoodsRefundRequest.from(actor, input(request));
        return refunds.execute(input.confirmation(request.get("operationId"), request.get("fingerprint")));
    }
    @SuppressWarnings("unchecked")
    public static Map<String, Object> input(Map<String, Object> request) {
        if (!(request.get("input") instanceof Map)) throw new TalentCenterApiException(400, "确认内容无效");
        return (Map<String, Object>)request.get("input");
    }
    public static boolean handles(Map<String, Object> input) {
        return "review-refund".equals(input.get("kind")) || "sync-refund".equals(input.get("kind")) || "retry-refund".equals(input.get("kind"));
    }
}
