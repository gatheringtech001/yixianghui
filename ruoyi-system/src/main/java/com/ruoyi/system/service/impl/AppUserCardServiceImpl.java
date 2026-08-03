package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.AppCard;
import com.ruoyi.system.domain.AppGoldBizType;
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.mapper.AppPayRefundLogMapper;
import com.ruoyi.system.mapper.AppUserCardMapper;
import com.ruoyi.system.domain.AppUserCard;
import com.ruoyi.system.service.IAppCardService;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.IAppPayLogService;
import com.ruoyi.system.service.IAppUserCardService;
import com.ruoyi.system.service.IAppUserInfoService;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户会员卡Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
@Slf4j
public class AppUserCardServiceImpl implements IAppUserCardService
{
    @Autowired
    private AppUserCardMapper appUserCardMapper;
    @Autowired
    private IAppUserInfoService userInfoService;
    @Autowired
    private Config wxPayConfigRuntime;

    @Autowired
    private IAppPayLogService payLogService;

    @Autowired
    private AppPayRefundLogMapper appPayRefundLogMapper;

    @Autowired
    private IAppCardService cardService;

    /**
     * Gold hook for member card (controlled by gold.scope.card, default false).
     */
    @Autowired
    private IAppGoldService goldService;

    /** 小程序APPID */
    @Value("${wx.pay.appId}")
    private String appId;
    /** 商户号 */
    @Value("${wx.pay.merchantId}")
    private String merchantId;
    /** 商户API私钥路径 */
    @Value("${wx.pay.privateKey}")
    public String privateKey;
    /** 商户证书序列号 */
    @Value("${wx.pay.merchantSerialNumber}")
    public String merchantSerialNumber;
    /** 商户APIV3密钥 */
    @Value("${wx.pay.apiV3Key}")
    public String apiV3Key;
    /** 支付（回调）通知地址 */
    @Value("${wx.pay.payNotifyUrl}")
    public String payNotifyUrl;
    @Value("${wx.pay.refundNotifyUrl}")
    public String refundNotifyUrl;


    /**
     * 查询用户会员卡
     * 
     * @param recordId 用户会员卡主键
     * @return 用户会员卡
     */
    @Override
    public AppUserCard selectAppUserCardByRecordId(Long recordId)
    {
        return appUserCardMapper.selectAppUserCardByRecordId(recordId);
    }

    /**
     * 查询用户会员卡列表
     * 
     * @param appUserCard 用户会员卡
     * @return 用户会员卡
     */
    @Override
    public List<AppUserCard> selectAppUserCardList(AppUserCard appUserCard)
    {
        return appUserCardMapper.selectAppUserCardList(appUserCard);
    }

    /**
     * 新增用户会员卡
     * 
     * @param appUserCard 用户会员卡
     * @return 结果
     */
    @Override
    public int insertAppUserCard(AppUserCard appUserCard)
    {
        appUserCard.setCreateTime(DateUtils.getNowDate());
        return appUserCardMapper.insertAppUserCard(appUserCard);
    }

    /**
     * 修改用户会员卡
     * 
     * @param appUserCard 用户会员卡
     * @return 结果
     */
    @Override
    public int updateAppUserCard(AppUserCard appUserCard)
    {
        appUserCard.setUpdateTime(DateUtils.getNowDate());
        return appUserCardMapper.updateAppUserCard(appUserCard);
    }

    /**
     * 批量删除用户会员卡
     * 
     * @param recordIds 需要删除的用户会员卡主键
     * @return 结果
     */
    @Override
    public int deleteAppUserCardByRecordIds(Long[] recordIds)
    {
        return appUserCardMapper.deleteAppUserCardByRecordIds(recordIds);
    }

    /**
     * 删除用户会员卡信息
     * 
     * @param recordId 用户会员卡主键
     * @return 结果
     */
    @Override
    public int deleteAppUserCardByRecordId(Long recordId)
    {
        return appUserCardMapper.deleteAppUserCardByRecordId(recordId);
    }

    /**
     * 根据用户ID获取会员卡信息
     * @param userId
     * @return
     */
    @Override
    public AppUserCard selectAppUserCardByUserId(Long userId) {
        return appUserCardMapper.selectAppUserCardByUserId(userId);
    }

    /**
     * 用户会员卡支付
     * @param userCard
     * @return
     */
    @Override
    public AjaxResult wxpayPrepay(AppUserCard userCard) {
        AjaxResult rs = AjaxResult.error();
        try {
            AppCard thecard = userCard.getCardInfo();
            //thecard.setPrice("0.01");
            userCard.setCardInfo(thecard);
            AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userCard.getUserId());
            JsapiServiceExtension service =
                    new JsapiServiceExtension.Builder()
                            .config(wxPayConfigRuntime)
                            // 不填默认为RSA
                            .signType("RSA")
                            .build();
            String theTradeNo = "10" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, userCard.getCreateTime());
            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(merchantId);
            request.setDescription("购买会员卡");
            request.setNotifyUrl(payNotifyUrl);
            request.setOutTradeNo(theTradeNo);
            // 支付截止与开卡单创建时间 + 30 分钟对齐
            ZonedDateTime expireAt;
            if (userCard.getCreateTime() != null) {
                expireAt = userCard.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusMinutes(30L);
            } else {
                expireAt = ZonedDateTime.now().plusMinutes(30L);
            }
            ZonedDateTime minExpire = ZonedDateTime.now().plusMinutes(1);
            if (!expireAt.isAfter(minExpire)) {
                return AjaxResult.error("支付已超时，请重新开通");
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            request.setTimeExpire(expireAt.format(formatter));
            request.setGoodsTag(userCard.getCardInfo().getCardName());
            Amount amount = new Amount();
            amount.setTotal(new BigDecimal(userCard.getCardInfo().getPrice()).multiply(new BigDecimal(100)).intValue());
            amount.setCurrency("CNY");
            request.setAmount(amount);
            Payer payer = new Payer();
            payer.setOpenid(userInfo.getWeixinOpenid());
            request.setPayer(payer);
            Detail detail = new Detail();
            detail.setCostPrice(new BigDecimal(userCard.getCardInfo().getPrice()).multiply(new BigDecimal(100)).intValue());
            List<com.wechat.pay.java.service.payments.jsapi.model.GoodsDetail> goodsDetailList = new ArrayList<>();
            GoodsDetail goodsDetail = new GoodsDetail();
            goodsDetail.setGoodsName(userCard.getCardInfo().getCardName());
            goodsDetail.setMerchantGoodsId(userCard.getCardInfo().getCardId().toString());
            goodsDetail.setQuantity(1);
            goodsDetail.setUnitPrice(new BigDecimal(userCard.getCardInfo().getPrice()).multiply(new BigDecimal(100)).intValue());
            goodsDetailList.add(goodsDetail);
            detail.setGoodsDetail(goodsDetailList);
            request.setDetail(detail);
            SceneInfo sceneInfo = new SceneInfo();
            sceneInfo.setPayerClientIp(IpUtils.getHostIp());
            request.setSceneInfo(sceneInfo);
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            rs = AjaxResult.success(response);
            AppPayLog existPayLog = payLogService.selectAppPayLogByPayNo(theTradeNo);
            if (existPayLog == null) {
                AppPayLog payLog = new AppPayLog();
                payLog.setOrderId(userCard.getRecordId());
                payLog.setPayNo(theTradeNo);
                payLog.setPayName("微信支付");
                payLog.setPayDescription(request.getDescription());
                payLog.setPayMoney(new BigDecimal(userCard.getCardInfo().getPrice()).multiply(new BigDecimal(100)));
                payLog.setPayMethod("wxpay");
                payLog.setStatus("0");
                payLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=",""));
                payLog.setAgentName("微信支付");
                payLog.setCreateTime(DateUtils.getNowDate());
                payLog.setOrderType("1");
                payLog.setUserId(userCard.getUserId());
                payLogService.insertAppPayLog(payLog);
            } else {
                existPayLog.setUpdateTime(DateUtils.getNowDate());
                existPayLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=",""));
                existPayLog.setPayMoney(new BigDecimal(userCard.getCardInfo().getPrice()).multiply(new BigDecimal(100)));
                existPayLog.setPayDescription(request.getDescription());
                if (!"0".equals(existPayLog.getStatus())) {
                    existPayLog.setStatus("0");
                }
                payLogService.updateAppPayLog(existPayLog);
            }
        }catch (HttpException e) { // 发送HTTP请求失败
            log.error("微信下单发送HTTP请求失败，错误信息：{}", e.getMessage());
        } catch (com.ruoyi.common.exception.ServiceException e) { // 服务返回状态小于200或大于等于300，例如500
            log.error("微信下单服务状态错误，错误信息：{}", e.getMessage());
            throw new com.ruoyi.common.exception.ServiceException("下单失败");
        } catch (MalformedMessageException e) { // 服务返回成功，返回体类型不合法，或者解析返回体失败
            log.error("服务返回成功，返回体类型不合法，或者解析返回体失败，错误信息：{}", e.getMessage());
            throw new ServiceException("下单失败");
        }
        return rs;
    }

    @Override
    public int closeExpiredUnpaidCards(int expireMinutes) {
        int minutes = expireMinutes > 0 ? expireMinutes : 30;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minutes);
        Date expireBefore = calendar.getTime();

        AppUserCard query = new AppUserCard();
        query.setStatus("0");
        query.getParams().put("expireBefore", expireBefore);
        List<AppUserCard> list = appUserCardMapper.selectAppUserCardList(query);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int closed = 0;
        Date now = DateUtils.getNowDate();
        for (AppUserCard card : list) {
            if (card == null || card.getRecordId() == null) {
                continue;
            }
            AppUserCard latest = appUserCardMapper.selectAppUserCardByRecordId(card.getRecordId());
            if (latest == null || !"0".equals(StringUtils.defaultIfBlank(latest.getStatus(), ""))) {
                continue;
            }
            closeCardPayLog(latest, now);
            AppUserCard up = new AppUserCard();
            up.setRecordId(latest.getRecordId());
            up.setStatus("3");
            up.setUpdateTime(now);
            if (appUserCardMapper.updateAppUserCard(up) > 0) {
                closed++;
            }
        }
        if (closed > 0) {
            log.info("超时关闭未支付会员卡开通单 {} 笔，阈值={}分钟", closed, minutes);
        }
        return closed;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelUnpaidUserCard(Long recordId, Long userId) {
        AppUserCard card = appUserCardMapper.selectAppUserCardByRecordId(recordId);
        if (card == null) {
            throw new ServiceException("开通记录不存在");
        }
        if (userId == null || card.getUserId() == null || card.getUserId().longValue() != userId.longValue()) {
            throw new ServiceException("非法操作");
        }
        if (!"0".equals(StringUtils.defaultIfBlank(card.getStatus(), ""))) {
            throw new ServiceException("仅待支付开通单可取消");
        }
        Date now = DateUtils.getNowDate();
        closeCardPayLog(card, now);
        AppUserCard up = new AppUserCard();
        up.setRecordId(recordId);
        up.setStatus("3");
        up.setUpdateTime(now);
        return appUserCardMapper.updateAppUserCard(up);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult refundUserCard(Long recordId, Long userId) {
        AppUserCard card = appUserCardMapper.selectAppUserCardByRecordId(recordId);
        if (card == null) {
            throw new ServiceException("开通记录不存在");
        }
        if (userId == null || card.getUserId() == null || card.getUserId().longValue() != userId.longValue()) {
            throw new ServiceException("非法操作");
        }
        if (!"1".equals(StringUtils.defaultIfBlank(card.getStatus(), ""))) {
            throw new ServiceException("仅生效中的会员卡可退款");
        }
        if (card.getCreateTime() == null) {
            throw new ServiceException("支付单号缺失，无法退款");
        }
        AppCard cardDef = cardService.selectAppCardByCardId(card.getCardId());
        if (cardDef == null || StringUtils.isEmpty(cardDef.getPrice())) {
            throw new ServiceException("会员卡价格信息缺失");
        }
        BigDecimal price = new BigDecimal(cardDef.getPrice());
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            // 免费卡直接失效
            AppUserCard up = new AppUserCard();
            up.setRecordId(recordId);
            up.setStatus("3");
            up.setUpdateTime(DateUtils.getNowDate());
            appUserCardMapper.updateAppUserCard(up);
            return AjaxResult.success("已取消");
        }
        String outTradeNo = "10" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, card.getCreateTime());
        try {
            RefundService service = new RefundService.Builder().config(wxPayConfigRuntime).build();
            String outRefundNo = "RF10" + DateUtils.dateTimeNow() + recordId;
            CreateRequest refundRequest = new CreateRequest();
            refundRequest.setOutTradeNo(outTradeNo);
            refundRequest.setOutRefundNo(outRefundNo);
            refundRequest.setReason("会员卡退款");
            refundRequest.setNotifyUrl(refundNotifyUrl);
            AmountReq refundAmount = new AmountReq();
            long fen = price.multiply(new BigDecimal(100)).longValue();
            refundAmount.setRefund(fen);
            refundAmount.setTotal(fen);
            refundAmount.setCurrency("CNY");
            refundRequest.setAmount(refundAmount);
            Refund response = service.create(refundRequest);

            AppPayRefundLog refundLog = new AppPayRefundLog();
            refundLog.setOrderId(recordId);
            refundLog.setUserId(userId);
            refundLog.setOrderType("1");
            refundLog.setPayNo(response.getRefundId());
            refundLog.setPayMethod("wxpay");
            refundLog.setAgentName("微信支付");
            refundLog.setAgentPayNo(response.getTransactionId());
            refundLog.setAgentRefundNo(outRefundNo);
            refundLog.setRefundMoney(price.multiply(new BigDecimal(100)));
            refundLog.setCreateTime(DateUtils.getNowDate());
            refundLog.setStatus("0");
            appPayRefundLogMapper.insertAppPayRefundLog(refundLog);

            // 先撤销权益，退款结果由回调落库
            AppUserCard up = new AppUserCard();
            up.setRecordId(recordId);
            up.setStatus("3");
            up.setUpdateTime(DateUtils.getNowDate());
            appUserCardMapper.updateAppUserCard(up);
            return AjaxResult.success("退款已提交");
        } catch (HttpException | MalformedMessageException e) {
            log.error("会员卡退款失败 recordId={}", recordId, e);
            throw new ServiceException("退款发起失败，请稍后重试");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("会员卡退款异常 recordId={}", recordId, e);
            throw new ServiceException("退款发起失败，请稍后重试");
        }
    }

    @Override
    public void handleRefundSuccess(Long recordId) {
        if (recordId == null) {
            return;
        }
        AppUserCard card = appUserCardMapper.selectAppUserCardByRecordId(recordId);
        if (card == null) {
            return;
        }
        if (!"3".equals(card.getStatus())) {
            AppUserCard up = new AppUserCard();
            up.setRecordId(recordId);
            up.setStatus("3");
            up.setUpdateTime(DateUtils.getNowDate());
            appUserCardMapper.updateAppUserCard(up);
        }
        // Extension: reverse gold when gold.scope.card=true
        try {
            BigDecimal refundFen = null;
            String refundNo = null;
            AppPayRefundLog query = new AppPayRefundLog();
            query.setOrderId(recordId);
            query.setOrderType("1");
            List<AppPayRefundLog> logs = appPayRefundLogMapper.selectAppPayRefundLogList(query);
            if (logs != null && !logs.isEmpty()) {
                AppPayRefundLog latest = logs.get(0);
                for (AppPayRefundLog item : logs) {
                    if (item.getLogId() != null && latest.getLogId() != null
                            && item.getLogId() > latest.getLogId()) {
                        latest = item;
                    }
                }
                refundFen = latest.getRefundMoney();
                refundNo = latest.getAgentRefundNo();
            }
            if (refundFen == null) {
                AppCard cardDef = cardService.selectAppCardByCardId(card.getCardId());
                if (cardDef != null && StringUtils.isNotEmpty(cardDef.getPrice())) {
                    refundFen = new BigDecimal(cardDef.getPrice()).multiply(new BigDecimal("100"));
                }
            }
            goldService.reverseOnRefund(card.getUserId(), AppGoldBizType.CARD_REFUND,
                    recordId, refundFen, refundNo);
        } catch (Exception ex) {
            log.warn("card refund reverse gold skipped recordId={}", recordId, ex);
        }
    }

    private void closeCardPayLog(AppUserCard card, Date now) {
        if (card == null || card.getCreateTime() == null) {
            return;
        }
        String payNo = "10" + DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, card.getCreateTime());
        AppPayLog payLog = payLogService.selectAppPayLogByPayNo(payNo);
        if (payLog != null && "0".equals(StringUtils.defaultIfBlank(payLog.getStatus(), ""))) {
            AppPayLog upLog = new AppPayLog();
            upLog.setLogId(payLog.getLogId());
            upLog.setStatus("2");
            upLog.setUpdateTime(now);
            payLogService.updateAppPayLog(upLog);
        }
    }
}
