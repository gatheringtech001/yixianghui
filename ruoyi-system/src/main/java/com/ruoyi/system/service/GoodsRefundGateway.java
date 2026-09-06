package com.ruoyi.system.service;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.http.DefaultHttpClientBuilder;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.*;

@Service
public class GoodsRefundGateway {
    private final RefundService service;
    private final String notifyUrl;
    public GoodsRefundGateway(Config config, @Value("${wx.pay.refundNotifyUrl}") String notifyUrl) {
        this.notifyUrl = notifyUrl;
        service = new RefundService.Builder().httpClient(new DefaultHttpClientBuilder().config(config)
            .connectTimeoutMs(3000).readTimeoutMs(6000).writeTimeoutMs(3000)
            .disableRetryOnConnectionFailure().build()).build();
    }
    public Refund create(GoodsRefundSnapshot snapshot, GoodsRefundRequest input) {
        return service.create(request(snapshot, input, notifyUrl));
    }
    static CreateRequest request(GoodsRefundSnapshot snapshot, GoodsRefundRequest input, String notifyUrl) {
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(snapshot.text("order_no")); request.setOutRefundNo(snapshot.reference());
        request.setNotifyUrl(notifyUrl);
        String reason = "retry".equals(input.decision) ? snapshot.text("remark") : input.note;
        request.setReason(reason.substring(0, reason.offsetByCodePoints(0, Math.min(80, reason.codePointCount(0, reason.length())))));
        AmountReq amount = new AmountReq(); amount.setCurrency("CNY");
        amount.setTotal(snapshot.paidFen()); amount.setRefund("retry".equals(input.decision)
            ? GoodsRefundSnapshot.integral(Objects.requireNonNull(snapshot.ownLog()).get("refund_money")) : Objects.requireNonNull(input.refundFen));
        request.setAmount(amount);
        return request;
    }
    public Refund query(String reference) {
        QueryByOutRefundNoRequest request = new QueryByOutRefundNoRequest(); request.setOutRefundNo(reference);
        return service.queryByOutRefundNo(request);
    }
    static Refund evidence(RefundNotification notification) {
        Refund refund = new Refund(); refund.setOutRefundNo(notification.getOutRefundNo());
        refund.setOutTradeNo(notification.getOutTradeNo()); refund.setRefundId(notification.getRefundId());
        refund.setTransactionId(notification.getTransactionId()); refund.setAmount(notification.getAmount());
        refund.setStatus(notification.getRefundStatus()); return refund;
    }
}
