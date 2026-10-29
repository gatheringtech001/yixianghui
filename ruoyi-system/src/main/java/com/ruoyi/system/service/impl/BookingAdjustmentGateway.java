package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Map;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.service.IAppUserInfoService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** External IO only. The caller must commit an approved intent before sending a refund. */
@Service
public class BookingAdjustmentGateway {
    private final JsapiService payments;
    private final WechatPrepayService prepay;
    private final IAppUserInfoService users;
    @Value("${wx.pay.appId}") private String appId;
    @Value("${wx.pay.merchantId}") private String merchantId;
    @Value("${wx.pay.payNotifyUrl}") private String notifyUrl;

    public BookingAdjustmentGateway(Config config, WechatPrepayService prepay, IAppUserInfoService users) {
        this.prepay = prepay; this.users = users;
        com.wechat.pay.java.core.http.HttpClient client = new com.wechat.pay.java.core.http.DefaultHttpClientBuilder().config(config)
            .connectTimeoutMs(3000).readTimeoutMs(6000).writeTimeoutMs(3000).disableRetryOnConnectionFailure().build();
        this.payments = new JsapiService.Builder().httpClient(client).build();
    }

    public PrepayWithRequestPaymentResponse prepay(Map<String, Object> change) {
        AppUserInfo user = users.selectAppUserInfoByUserId(number(change, "user_id"));
        if (user == null || user.getWeixinOpenid() == null || user.getWeixinOpenid().isEmpty())
            throw new ServiceException("请重新登录微信后支付");
        PrepayRequest request = new PrepayRequest();
        request.setAppid(appId); request.setMchid(merchantId);
        request.setOutTradeNo(String.valueOf(change.get("pay_no")));
        request.setDescription("逸享荟活动报名补差价"); request.setNotifyUrl(notifyUrl);
        request.setTimeExpire(((java.util.Date) change.get("expires_at")).toInstant()
            .atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        Amount amount = new Amount(); amount.setCurrency("CNY");
        amount.setTotal(Math.toIntExact(number(change, "amount_fen"))); request.setAmount(amount);
        Payer payer = new Payer(); payer.setOpenid(user.getWeixinOpenid()); request.setPayer(payer);
        return prepay.create(request);
    }

    public Transaction queryPayment(String number) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(merchantId); request.setOutTradeNo(number);
        return payments.queryOrderByOutTradeNo(request);
    }

    public void closePayment(String number) {
        CloseOrderRequest request = new CloseOrderRequest(); request.setMchid(merchantId); request.setOutTradeNo(number);
        payments.closeOrder(request);
    }

    public void validatePayment(Map<String, Object> change, Transaction result) {
        if (result == null || result.getTradeState() != Transaction.TradeStateEnum.SUCCESS
            || !java.util.Objects.equals(appId, result.getAppid()) || !java.util.Objects.equals(merchantId, result.getMchid())
            || !java.util.Objects.equals(change.get("pay_no"), result.getOutTradeNo()) || result.getAmount() == null
            || !"CNY".equals(result.getAmount().getCurrency()) || result.getAmount().getTotal() == null
            || number(change, "amount_fen") != result.getAmount().getTotal().longValue())
            throw new ServiceException("微信补款凭证与本次变更不一致");
    }

    static long number(Map<String, Object> row, String name) {
        Object value = row.get(name);
        if (value == null) throw new ServiceException("订单数据缺失，请联系客服");
        return new BigDecimal(value.toString()).longValueExact();
    }
}
