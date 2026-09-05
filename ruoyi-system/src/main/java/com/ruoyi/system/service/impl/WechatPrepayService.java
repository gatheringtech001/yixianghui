package com.ruoyi.system.service.impl;

import com.ruoyi.common.exception.ServiceException;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WechatPrepayService {
    @Autowired private Config wxPayConfigRuntime;

    public PrepayWithRequestPaymentResponse create(PrepayRequest request) {
        if (!MerchantOrderNumbers.valid(request.getOutTradeNo())) throw new ServiceException("支付单号格式无效");
        if (request.getAmount() == null || request.getAmount().getTotal() == null || request.getAmount().getTotal() <= 0) {
            throw new ServiceException("支付金额无效");
        }
        try {
            return send(request);
        } catch (com.wechat.pay.java.core.exception.ServiceException error) {
            String code = error.getErrorCode();
            if (code == null || !code.matches("[A-Z0-9_]{1,64}")) code = "UNKNOWN";
            log.error("微信预支付拒绝 orderNo={}, status={}, code={}", request.getOutTradeNo(), error.getHttpStatusCode(), code);
            throw new ServiceException("微信预支付失败（" + code + "），请联系客服");
        } catch (HttpException | MalformedMessageException error) {
            log.error("微信预支付通信失败 orderNo={}, type={}", request.getOutTradeNo(), error.getClass().getSimpleName());
            throw new ServiceException("连接微信支付失败，请稍后重试");
        }
    }

    protected PrepayWithRequestPaymentResponse send(PrepayRequest request) {
        return new JsapiServiceExtension.Builder().config(wxPayConfigRuntime).signType("RSA")
                .build().prepayWithRequestPayment(request);
    }
}
