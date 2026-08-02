package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.domain.AppActivityOrder;
import com.ruoyi.system.domain.AppGoldBizType;
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.mapper.AppActivityMapper;
import com.ruoyi.system.mapper.AppActivityOrderMapper;
import com.ruoyi.system.mapper.AppPayRefundLogMapper;
import com.ruoyi.system.service.IAppActivityOrderService;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.IAppPayLogService;
import com.ruoyi.system.service.IAppUserInfoService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Detail;
import com.wechat.pay.java.service.payments.jsapi.model.GoodsDetail;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.jsapi.model.SceneInfo;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.QueryByOutRefundNoRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 活动预约Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
@Slf4j
public class AppActivityOrderServiceImpl implements IAppActivityOrderService 
{
    @Autowired
    private AppActivityOrderMapper appActivityOrderMapper;

    @Autowired
    private AppActivityMapper appActivityMapper;

    @Autowired
    private IAppUserInfoService userInfoService;

    @Autowired
    private IAppPayLogService payLogService;

    @Autowired
    private AppPayRefundLogMapper appPayRefundLogMapper;

    @Autowired
    private RSAAutoCertificateConfig rsaAutoCertificateConfig;

    @Autowired
    private IAppGoldService goldService;

    /** 自注入，保证 cancel 内查单走独立事务（REQUIRES_NEW） */
    @Lazy
    @Autowired
    private IAppActivityOrderService self;

    @Value("${wx.pay.appId}")
    private String appId;

    @Value("${wx.pay.merchantId}")
    private String merchantId;

    @Value("${wx.pay.privateKey}")
    private String privateKey;

    @Value("${wx.pay.merchantSerialNumber}")
    private String merchantSerialNumber;

    @Value("${wx.pay.apiV3Key}")
    private String apiV3Key;

    @Value("${wx.pay.payNotifyUrl}")
    private String payNotifyUrl;

    @Value("${wx.pay.refundNotifyUrl}")
    private String refundNotifyUrl;

    /**
     * 查询活动预约
     * 
     * @param orderId 活动预约主键
     * @return 活动预约
     */
    @Override
    public AppActivityOrder selectAppActivityOrderByOrderId(Long orderId)
    {
        return appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
    }

    /**
     * 查询活动预约列表
     * 
     * @param appActivityOrder 活动预约
     * @return 活动预约
     */
    @Override
    public List<AppActivityOrder> selectAppActivityOrderList(AppActivityOrder appActivityOrder)
    {
        return appActivityOrderMapper.selectAppActivityOrderList(appActivityOrder);
    }

    /**
     * 新增活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    @Override
    public int insertAppActivityOrder(AppActivityOrder appActivityOrder)
    {
        appActivityOrder.setCreateTime(DateUtils.getNowDate());
        return appActivityOrderMapper.insertAppActivityOrder(appActivityOrder);
    }

    /**
     * 修改活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    @Override
    public int updateAppActivityOrder(AppActivityOrder appActivityOrder)
    {
        return appActivityOrderMapper.updateAppActivityOrder(appActivityOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int editUserActivityOrder(AppActivityOrder input, Long userId)
    {
        if (input == null || input.getOrderId() == null) {
            throw new ServiceException("预约记录无效");
        }
        AppActivityOrder db = appActivityOrderMapper.selectAppActivityOrderByOrderId(input.getOrderId());
        if (db == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (userId == null || db.getUserId() == null || db.getUserId().longValue() != userId.longValue()) {
            throw new ServiceException("非法操作");
        }
        String payStatus = StringUtils.defaultIfBlank(db.getPayStatus(), "0");
        String status = StringUtils.defaultIfBlank(db.getStatus(), "0");
        // 已取消/退款中/已退款不可改
        if ("2".equals(payStatus) || "3".equals(payStatus) || "4".equals(payStatus) || "2".equals(status)) {
            throw new ServiceException("当前订单不可修改");
        }

        if (StringUtils.isEmpty(input.getSignName())) {
            throw new ServiceException("请填写联系人");
        }
        if (StringUtils.isEmpty(input.getSignMobile())) {
            throw new ServiceException("请填写联系电话");
        }

        Integer newCount = input.getSignCount();
        int oldCount = db.getSignCount() != null ? db.getSignCount() : 0;
        boolean countChanged = newCount != null && newCount.intValue() != oldCount;
        boolean paidWithMoney = "1".equals(payStatus)
                && db.getPayMoney() != null
                && db.getPayMoney().compareTo(BigDecimal.ZERO) > 0;

        // 付费已支付：人数按支付时锁定，不可增减（防止一次付费多人）
        if (paidWithMoney && countChanged) {
            throw new ServiceException("付费报名后不可修改人数，如需调整请取消后重新报名");
        }

        AppActivityOrder up = new AppActivityOrder();
        up.setOrderId(db.getOrderId());
        up.setSignName(input.getSignName().trim());
        up.setSignMobile(input.getSignMobile().trim());

        if (countChanged) {
            if (newCount < 1) {
                throw new ServiceException("报名人数至少为1人");
            }
            AppActivity activity = appActivityMapper.selectAppActivityByActivityId(db.getActivityId());
            if (activity == null) {
                throw new ServiceException("活动不存在");
            }
            int maxCount = parseCount(activity.getMaxCount());
            int currentCount = parseCount(activity.getSignCount());
            int delta = newCount - oldCount;

            // 待支付：允许改人数并重算应付金额
            if ("0".equals(payStatus)) {
                if (delta > 0 && maxCount > 0 && currentCount + delta > maxCount) {
                    throw new ServiceException("报名名额不足");
                }
                BigDecimal unitPrice = resolveUnitPrice(activity);
                if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("活动价格未配置");
                }
                up.setSignCount(newCount);
                up.setMoneyPayable(unitPrice.multiply(new BigDecimal(newCount)));
            } else if ("1".equals(payStatus)) {
                // 免费已报名：允许改人数，同步占用名额
                if (delta > 0) {
                    if (maxCount > 0 && currentCount + delta > maxCount) {
                        throw new ServiceException("报名名额不足");
                    }
                    int updated = appActivityMapper.increaseSignCount(db.getActivityId(), delta);
                    if (updated <= 0) {
                        throw new ServiceException("报名名额不足");
                    }
                } else if (delta < 0) {
                    appActivityMapper.decreaseSignCount(db.getActivityId(), -delta);
                }
                up.setSignCount(newCount);
            }
        }

        return appActivityOrderMapper.updateAppActivityOrder(up);
    }

    /**
     * 批量删除活动预约
     * 
     * @param orderIds 需要删除的活动预约主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityOrderByOrderIds(Long[] orderIds)
    {
        return appActivityOrderMapper.deleteAppActivityOrderByOrderIds(orderIds);
    }

    /**
     * 删除活动预约信息
     * 
     * @param orderId 活动预约主键
     * @return 结果
     */
    @Override
    public int deleteAppActivityOrderByOrderId(Long orderId)
    {
        return appActivityOrderMapper.deleteAppActivityOrderByOrderId(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppActivityOrder signupActivity(AppActivityOrder order)
    {
        if (order == null || order.getActivityId() == null) {
            throw new ServiceException("活动信息无效");
        }
        if (order.getUserId() == null) {
            throw new ServiceException("请先登录");
        }
        int count = order.getSignCount() != null ? order.getSignCount() : 0;
        if (count < 1) {
            throw new ServiceException("请填写报名人数");
        }

        AppActivity activity = appActivityMapper.selectAppActivityByActivityId(order.getActivityId());
        if (activity == null || !"1".equals(activity.getStatus())) {
            throw new ServiceException("活动未开放报名");
        }
        if (StringUtils.isNotEmpty(activity.getSignEndTime())) {
            Date endTime = DateUtils.parseDate(activity.getSignEndTime());
            if (endTime != null && endTime.getTime() < System.currentTimeMillis()) {
                throw new ServiceException("报名已截止");
            }
        }
        Integer isFree = activity.getIsFree();
        if (isFree != null && isFree == 0) {
            throw new ServiceException("付费活动请通过支付流程报名");
        }

        int maxCount = parseCount(activity.getMaxCount());
        int currentCount = parseCount(activity.getSignCount());
        if (maxCount > 0 && currentCount + count > maxCount) {
            throw new ServiceException("报名名额不足");
        }

        assertNoConfirmedDuplicate(order.getUserId(), order.getActivityId());

        order.setStatus("1");
        order.setPayStatus("1");
        order.setMoneyPayable(BigDecimal.ZERO);
        order.setPayMoney(BigDecimal.ZERO);
        order.setCreateTime(DateUtils.getNowDate());
        int rows = appActivityOrderMapper.insertAppActivityOrder(order);
        if (rows <= 0) {
            throw new ServiceException("报名失败");
        }

        int updated = appActivityMapper.increaseSignCount(order.getActivityId(), count);
        if (updated <= 0) {
            throw new ServiceException("报名名额已满");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppActivityOrder createPendingActivityOrder(AppActivityOrder order)
    {
        if (order == null || order.getActivityId() == null) {
            throw new ServiceException("活动信息无效");
        }
        if (order.getUserId() == null) {
            throw new ServiceException("请先登录");
        }
        if (StringUtils.isEmpty(order.getSignName())) {
            throw new ServiceException("请填写联系人姓名");
        }
        if (StringUtils.isEmpty(order.getSignMobile())) {
            throw new ServiceException("请填写联系电话");
        }
        int count = order.getSignCount() != null ? order.getSignCount() : 0;
        if (count < 1) {
            throw new ServiceException("请填写报名人数");
        }

        AppActivity activity = appActivityMapper.selectAppActivityByActivityId(order.getActivityId());
        if (activity == null || !"1".equals(activity.getStatus())) {
            throw new ServiceException("活动未开放报名");
        }
        if (StringUtils.isNotEmpty(activity.getSignEndTime())) {
            Date endTime = DateUtils.parseDate(activity.getSignEndTime());
            if (endTime != null && endTime.getTime() < System.currentTimeMillis()) {
                throw new ServiceException("报名已截止");
            }
        }
        Integer isFree = activity.getIsFree();
        if (isFree == null || isFree != 0) {
            throw new ServiceException("免费活动请直接报名");
        }

        int maxCount = parseCount(activity.getMaxCount());
        int currentCount = parseCount(activity.getSignCount());
        if (maxCount > 0 && currentCount + count > maxCount) {
            throw new ServiceException("报名名额不足");
        }

        // 已有待支付单：直接复用，避免取消后/暂不支付后无法再次报名
        AppActivityOrder existingPending = findActivePendingOrder(order.getUserId(), order.getActivityId());
        if (existingPending != null) {
            existingPending.setSignName(order.getSignName());
            existingPending.setSignMobile(order.getSignMobile());
            existingPending.setSignCount(count);
            BigDecimal unitPrice = resolveUnitPrice(activity);
            if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("活动价格未配置");
            }
            existingPending.setMoneyPayable(unitPrice.multiply(new BigDecimal(count)));
            appActivityOrderMapper.updateAppActivityOrder(existingPending);
            return existingPending;
        }

        assertNoConfirmedDuplicate(order.getUserId(), order.getActivityId());

        BigDecimal unitPrice = resolveUnitPrice(activity);
        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("活动价格未配置");
        }

        order.setStatus("0");
        order.setPayStatus("0");
        order.setMoneyPayable(unitPrice.multiply(new BigDecimal(count)));
        order.setPayMoney(BigDecimal.ZERO);
        order.setOrderNo("30" + DateUtils.dateTimeNow() + order.getActivityId());
        order.setCreateTime(DateUtils.getNowDate());
        int rows = appActivityOrderMapper.insertAppActivityOrder(order);
        if (rows <= 0) {
            throw new ServiceException("订单创建失败");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelActivityOrder(Long orderId, Long userId)
    {
        AppActivityOrder order = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (userId == null || order.getUserId() == null || order.getUserId().longValue() != userId.longValue()) {
            throw new ServiceException("非法操作");
        }
        String payStatus = StringUtils.defaultIfBlank(order.getPayStatus(), "0");
        if ("2".equals(payStatus) || "3".equals(payStatus) || "4".equals(payStatus)) {
            throw new ServiceException("当前订单不可取消");
        }

        Date now = DateUtils.getNowDate();
        // 未支付：先向微信查单，避免「已付款但回调未落库」被误取消
        if ("0".equals(payStatus)) {
            trySyncPaidBeforeCancel(order);
            order = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
            payStatus = StringUtils.defaultIfBlank(order.getPayStatus(), "0");
            if ("1".equals(payStatus)) {
                throw new ServiceException("订单已支付成功，请刷新后查看");
            }
            closePayLogByPayNo(order.getOrderNo(), now);
            AppActivityOrder up = new AppActivityOrder();
            up.setOrderId(orderId);
            up.setStatus("2");
            up.setPayStatus("2");
            return appActivityOrderMapper.updateAppActivityOrder(up);
        }

        // 已支付/已确认：先释放名额
        int signCount = order.getSignCount() != null ? order.getSignCount() : 0;
        if (signCount > 0 && order.getActivityId() != null) {
            appActivityMapper.decreaseSignCount(order.getActivityId(), signCount);
        }

        BigDecimal payMoney = order.getPayMoney() != null ? order.getPayMoney() : BigDecimal.ZERO;
        // 免费报名或实付为 0：直接取消
        if (payMoney.compareTo(BigDecimal.ZERO) <= 0) {
            AppActivityOrder up = new AppActivityOrder();
            up.setOrderId(orderId);
            up.setStatus("2");
            up.setPayStatus("2");
            return appActivityOrderMapper.updateAppActivityOrder(up);
        }

        // 付费已支付：发起微信退款
        createActivityWxRefund(order);
        AppActivityOrder latest = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
        if (latest != null && "4".equals(latest.getPayStatus())) {
            return 1;
        }
        AppActivityOrder up = new AppActivityOrder();
        up.setOrderId(orderId);
        up.setStatus("3");
        up.setPayStatus("3");
        return appActivityOrderMapper.updateAppActivityOrder(up);
    }

    /**
     * 发起活动订单微信退款并写退款日志
     */
    private void createActivityWxRefund(AppActivityOrder order)
    {
        if (StringUtils.isEmpty(order.getOrderNo())) {
            throw new ServiceException("支付单号缺失，无法退款");
        }
        try {
            RefundService service = new RefundService.Builder().config(rsaAutoCertificateConfig).build();
            String outRefundNo = "RF30" + DateUtils.dateTimeNow() + order.getOrderId();
            CreateRequest refundRequest = new CreateRequest();
            refundRequest.setOutTradeNo(order.getOrderNo());
            refundRequest.setOutRefundNo(outRefundNo);
            refundRequest.setReason("取消活动报名退款");
            refundRequest.setNotifyUrl(refundNotifyUrl);
            AmountReq refundAmount = new AmountReq();
            long fen = order.getPayMoney().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
            refundAmount.setRefund(fen);
            refundAmount.setTotal(fen);
            refundAmount.setCurrency("CNY");
            refundRequest.setAmount(refundAmount);
            Refund response = service.create(refundRequest);
            if (response == null) {
                throw new ServiceException("退款发起失败，请稍后重试");
            }

            boolean refundDone = Status.SUCCESS.equals(response.getStatus());
            AppPayRefundLog refundLog = new AppPayRefundLog();
            refundLog.setOrderId(order.getOrderId());
            refundLog.setUserId(order.getUserId());
            refundLog.setOrderType("3");
            refundLog.setPayNo(response.getRefundId());
            refundLog.setPayMethod("wxpay");
            refundLog.setAgentName("微信支付");
            refundLog.setAgentPayNo(response.getTransactionId());
            refundLog.setAgentRefundNo(outRefundNo);
            refundLog.setRefundMoney(new BigDecimal(fen));
            refundLog.setCreateTime(DateUtils.getNowDate());
            refundLog.setStatus(refundDone ? "1" : "0");
            if (refundDone) {
                refundLog.setNotifyContent(String.valueOf(response));
                refundLog.setUpdateTime(DateUtils.getNowDate());
            }
            appPayRefundLogMapper.insertAppPayRefundLog(refundLog);
            if (refundDone) {
                handleRefundSuccess(order.getOrderId());
            }
        } catch (HttpException | MalformedMessageException e) {
            log.error("活动订单退款失败 orderId={}", order.getOrderId(), e);
            throw new ServiceException("退款发起失败，请稍后重试");
        } catch (com.wechat.pay.java.core.exception.ServiceException e) {
            String errMsg = StringUtils.defaultIfBlank(e.getErrorMessage(), e.getMessage());
            log.error("活动订单微信退款业务失败 orderId={}, msg={}", order.getOrderId(), errMsg);
            if (errMsg != null && (errMsg.contains("已全额退款") || errMsg.contains("订单已全额退款")
                    || errMsg.contains("超过剩余可退"))) {
                handleRefundSuccess(order.getOrderId());
                return;
            }
            throw new ServiceException(StringUtils.defaultIfBlank(errMsg, "退款发起失败"));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("活动订单退款异常 orderId={}", order.getOrderId(), e);
            throw new ServiceException("退款发起失败，请稍后重试");
        }
    }

    @Override
    public AjaxResult wxpayPrepay(AppActivityOrder order)
    {
        AjaxResult rs = AjaxResult.error();
        if (order == null || order.getOrderId() == null) {
            throw new ServiceException("订单信息无效");
        }
        if (!"0".equals(order.getPayStatus())) {
            throw new ServiceException("订单非待支付状态");
        }
        AppActivity activity = appActivityMapper.selectAppActivityByActivityId(order.getActivityId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        try {
            AppPayLog payLog = payLogService.selectAppPayLogByPayNo(order.getOrderNo());
            AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(order.getUserId());
            if (userInfo == null || StringUtils.isEmpty(userInfo.getWeixinOpenid())) {
                throw new ServiceException("微信用户信息缺失，请重新登录");
            }

            JsapiServiceExtension service = new JsapiServiceExtension.Builder()
                    .config(rsaAutoCertificateConfig)
                    .signType("RSA")
                    .build();

            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(merchantId);
            request.setDescription("活动报名-" + activity.getActivityName());
            request.setNotifyUrl(payNotifyUrl);
            if (payLog == null) {
                request.setOutTradeNo(order.getOrderNo());
            } else {
                order.setOrderNo("30" + DateUtils.dateTimeNow() + order.getActivityId());
                request.setOutTradeNo(order.getOrderNo());
            }

            ZonedDateTime expireAt;
            if (order.getCreateTime() != null) {
                expireAt = order.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusMinutes(30L);
            } else {
                expireAt = ZonedDateTime.now().plusMinutes(30L);
            }
            ZonedDateTime minExpire = ZonedDateTime.now().plusMinutes(1);
            if (!expireAt.isAfter(minExpire)) {
                return AjaxResult.error("支付已超时，请重新报名");
            }
            request.setTimeExpire(expireAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            Amount amount = new Amount();
            amount.setTotal(order.getMoneyPayable().multiply(new BigDecimal(100)).intValue());
            amount.setCurrency("CNY");
            request.setAmount(amount);

            Payer payer = new Payer();
            payer.setOpenid(userInfo.getWeixinOpenid());
            request.setPayer(payer);

            Detail detail = new Detail();
            detail.setCostPrice(order.getMoneyPayable().multiply(new BigDecimal(100)).intValue());
            List<GoodsDetail> goodsDetailList = new ArrayList<>();
            GoodsDetail goodsDetail = new GoodsDetail();
            goodsDetail.setGoodsName(activity.getActivityName());
            goodsDetail.setMerchantGoodsId(order.getActivityId().toString());
            goodsDetail.setQuantity(order.getSignCount() != null ? order.getSignCount() : 1);
            int unitFen = order.getMoneyPayable()
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(goodsDetail.getQuantity()), 0, RoundingMode.HALF_UP)
                    .intValue();
            goodsDetail.setUnitPrice(unitFen);
            goodsDetailList.add(goodsDetail);
            detail.setGoodsDetail(goodsDetailList);
            request.setDetail(detail);

            SceneInfo sceneInfo = new SceneInfo();
            sceneInfo.setPayerClientIp(IpUtils.getHostIp());
            request.setSceneInfo(sceneInfo);

            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            rs = AjaxResult.success(response);

            if (payLog == null) {
                payLog = new AppPayLog();
                payLog.setOrderId(order.getOrderId());
                payLog.setPayNo(order.getOrderNo());
                payLog.setPayName("微信支付");
                payLog.setPayDescription(request.getDescription());
                payLog.setPayMoney(order.getMoneyPayable().multiply(new BigDecimal(100)));
                payLog.setPayMethod("wxpay");
                payLog.setStatus("0");
                payLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=", ""));
                payLog.setAgentName("微信支付");
                payLog.setCreateTime(DateUtils.getNowDate());
                payLog.setOrderType("3");
                payLog.setUserId(order.getUserId());
                payLogService.insertAppPayLog(payLog);
            } else {
                payLog.setPayNo(order.getOrderNo());
                payLog.setUpdateTime(DateUtils.getNowDate());
                payLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=", ""));
                payLog.setPayMoney(order.getMoneyPayable().multiply(new BigDecimal(100)));
                payLog.setPayDescription(request.getDescription());
                payLogService.updateAppPayLog(payLog);
            }
            appActivityOrderMapper.updateAppActivityOrder(order);
            log.info("活动订单【{}】发起预支付成功", order.getOrderId());
        } catch (HttpException e) {
            log.error("活动订单微信下单HTTP失败：{}", e.getMessage());
            throw new ServiceException("下单失败");
        } catch (MalformedMessageException e) {
            log.error("活动订单微信下单响应解析失败：{}", e.getMessage());
            throw new ServiceException("下单失败");
        }
        return rs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaySuccess(AppActivityOrder order, BigDecimal payMoneyFen)
    {
        if (order == null || !"0".equals(order.getPayStatus())) {
            return;
        }
        int signCount = order.getSignCount() != null ? order.getSignCount() : 0;
        int updated = appActivityMapper.increaseSignCount(order.getActivityId(), signCount);
        if (updated <= 0) {
            throw new ServiceException("报名名额已满");
        }
        order.setPayStatus("1");
        order.setStatus("1");
        order.setPayType("wxpay");
        order.setPayMoney(payMoneyFen.divide(new BigDecimal(100)));
        order.setPayTime(DateUtils.getNowDate());
        appActivityOrderMapper.updateAppActivityOrder(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRefundSuccess(Long orderId)
    {
        if (orderId == null) {
            return;
        }
        AppActivityOrder order = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            return;
        }
        if (!"4".equals(order.getPayStatus())) {
            AppActivityOrder up = new AppActivityOrder();
            up.setOrderId(orderId);
            up.setStatus("4");
            up.setPayStatus("4");
            appActivityOrderMapper.updateAppActivityOrder(up);
        }
        // 退款扣币（幂等）；金额取最新退款日志，否则按实付
        AppPayRefundLog refundLog = resolveLatestActivityRefundLog(orderId);
        BigDecimal refundFen = refundLog != null ? refundLog.getRefundMoney() : null;
        if (refundFen == null && order.getPayMoney() != null) {
            refundFen = order.getPayMoney().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
        }
        String refundNo = refundLog != null ? refundLog.getAgentRefundNo() : null;
        Long uid = order.getUserId();
        if (refundLog != null && refundLog.getUserId() != null) {
            uid = refundLog.getUserId();
        }
        try {
            goldService.reverseOnRefund(uid, AppGoldBizType.ACTIVITY_REFUND, orderId, refundFen, refundNo);
        } catch (Exception ex) {
            log.warn("活动退款扣币失败 orderId={}", orderId, ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public AjaxResult syncPayResult(Long orderId, Long userId)
    {
        if (orderId == null || userId == null) {
            return AjaxResult.error("参数无效");
        }
        AppActivityOrder order = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            return AjaxResult.error("订单不存在");
        }
        if (order.getUserId() == null || order.getUserId().longValue() != userId.longValue()) {
            return AjaxResult.error("非法订单");
        }
        if ("1".equals(order.getPayStatus())) {
            // 已支付仍尝试补赠（防回调中断漏赠）
            tryCompensateActivityGold(order);
            return AjaxResult.success("已支付", order);
        }
        if (!"0".equals(StringUtils.defaultIfBlank(order.getPayStatus(), "0"))) {
            return AjaxResult.error("当前订单状态不可同步支付结果");
        }

        AppPayLog payLog = resolveActivityPayLog(order);
        if (payLog == null || StringUtils.isEmpty(payLog.getPayNo())) {
            return AjaxResult.error("未找到支付记录");
        }
        try {
            JsapiService service = new JsapiService.Builder().config(rsaAutoCertificateConfig).build();
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(merchantId);
            queryRequest.setOutTradeNo(payLog.getPayNo());
            com.wechat.pay.java.service.payments.model.Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);
            if (transaction == null || transaction.getTradeState() == null) {
                return AjaxResult.error("查询支付结果失败");
            }
            if (!com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.SUCCESS
                    .equals(transaction.getTradeState())) {
                return AjaxResult.error("微信侧尚未支付成功");
            }
            BigDecimal payFen = transaction.getAmount() != null && transaction.getAmount().getTotal() != null
                    ? new BigDecimal(transaction.getAmount().getTotal())
                    : payLog.getPayMoney();
            if (payFen == null) {
                payFen = BigDecimal.ZERO;
            }
            payLog.setPayMoney(payFen);
            payLog.setStatus("1");
            payLog.setNotifyContent(String.valueOf(transaction));
            payLog.setUpdateTime(DateUtils.getNowDate());
            payLogService.updateAppPayLog(payLog);

            // 若预下单时 orderNo 被刷新，与 payLog 对齐
            if (StringUtils.isNotEmpty(payLog.getPayNo()) && !payLog.getPayNo().equals(order.getOrderNo())) {
                order.setOrderNo(payLog.getPayNo());
            }
            handlePaySuccess(order, payFen);
            try {
                goldService.grantOnPay(order.getUserId(), AppGoldBizType.ACTIVITY_PAY, order.getOrderId(),
                        payFen, payLog.getPayNo());
            } catch (Exception ex) {
                log.warn("活动查单补赠币失败 orderId={}", orderId, ex);
            }
            AppActivityOrder latest = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
            return AjaxResult.success("支付已确认", latest);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("活动订单主动查单失败 orderId={}", orderId, e);
            return AjaxResult.error("同步支付结果失败");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public AjaxResult syncRefundResult(Long orderId, Long userId)
    {
        if (orderId == null || userId == null) {
            return AjaxResult.error("参数无效");
        }
        AppActivityOrder order = appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            return AjaxResult.error("订单不存在");
        }
        if (order.getUserId() == null || order.getUserId().longValue() != userId.longValue()) {
            return AjaxResult.error("非法订单");
        }
        if ("4".equals(order.getPayStatus())) {
            // 已退款仍尝试扣币（幂等）
            handleRefundSuccess(orderId);
            return AjaxResult.success("已退款",
                    appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId));
        }
        if (!"3".equals(StringUtils.defaultIfBlank(order.getPayStatus(), ""))) {
            return AjaxResult.error("当前订单非退款中状态");
        }

        AppPayRefundLog refundLog = resolveLatestActivityRefundLog(orderId);
        if (refundLog == null) {
            return AjaxResult.error("未找到退款记录");
        }
        if ("1".equals(refundLog.getStatus())) {
            handleRefundSuccess(orderId);
            return AjaxResult.success("退款已确认",
                    appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId));
        }
        if (StringUtils.isEmpty(refundLog.getAgentRefundNo())) {
            return AjaxResult.error("退款单号缺失");
        }
        try {
            RefundService service = new RefundService.Builder().config(rsaAutoCertificateConfig).build();
            QueryByOutRefundNoRequest queryRequest = new QueryByOutRefundNoRequest();
            queryRequest.setOutRefundNo(refundLog.getAgentRefundNo());
            Refund refund = service.queryByOutRefundNo(queryRequest);
            if (refund == null || refund.getStatus() == null) {
                return AjaxResult.error("查询退款结果失败");
            }
            if (!Status.SUCCESS.equals(refund.getStatus())) {
                return AjaxResult.error("微信侧退款尚未完成，状态：" + refund.getStatus().name());
            }

            AppPayRefundLog upRefundLog = new AppPayRefundLog();
            upRefundLog.setLogId(refundLog.getLogId());
            upRefundLog.setStatus("1");
            upRefundLog.setNotifyContent(String.valueOf(refund));
            upRefundLog.setUpdateTime(DateUtils.getNowDate());
            if (StringUtils.isNotEmpty(refund.getRefundId())) {
                upRefundLog.setPayNo(refund.getRefundId());
            }
            appPayRefundLogMapper.updateAppPayRefundLog(upRefundLog);
            handleRefundSuccess(orderId);
            return AjaxResult.success("退款已确认",
                    appActivityOrderMapper.selectAppActivityOrderByOrderId(orderId));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("活动订单主动查退款失败 orderId={}", orderId, e);
            return AjaxResult.error("同步退款结果失败");
        }
    }

    private AppPayRefundLog resolveLatestActivityRefundLog(Long orderId)
    {
        if (orderId == null) {
            return null;
        }
        AppPayRefundLog query = new AppPayRefundLog();
        query.setOrderId(orderId);
        query.setOrderType("3");
        List<AppPayRefundLog> logs = appPayRefundLogMapper.selectAppPayRefundLogList(query);
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        AppPayRefundLog latest = logs.get(0);
        for (AppPayRefundLog item : logs) {
            if (item.getLogId() != null && latest.getLogId() != null
                    && item.getLogId() > latest.getLogId()) {
                latest = item;
            }
        }
        return latest;
    }

    private void tryCompensateActivityGold(AppActivityOrder order)
    {
        if (order == null || order.getOrderId() == null) {
            return;
        }
        try {
            AppPayLog payLog = resolveActivityPayLog(order);
            BigDecimal fen = payLog != null ? payLog.getPayMoney() : null;
            if (fen == null && order.getPayMoney() != null) {
                fen = order.getPayMoney().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
            }
            String payNo = payLog != null ? payLog.getPayNo() : order.getOrderNo();
            goldService.grantOnPay(order.getUserId(), AppGoldBizType.ACTIVITY_PAY, order.getOrderId(), fen, payNo);
        } catch (Exception ex) {
            log.warn("活动订单补赠币失败 orderId={}", order.getOrderId(), ex);
        }
    }

    /**
     * 取消前尝试同步已支付状态，防止误关单
     */
    private void trySyncPaidBeforeCancel(AppActivityOrder order)
    {
        try {
            AjaxResult rs = self.syncPayResult(order.getOrderId(), order.getUserId());
            if (rs != null && rs.isSuccess()) {
                log.info("取消前查单确认已支付 orderId={}", order.getOrderId());
            }
        } catch (Exception ex) {
            log.warn("取消前查单失败 orderId={}", order.getOrderId(), ex);
        }
    }

    private AppPayLog resolveActivityPayLog(AppActivityOrder order)
    {
        AppPayLog payLog = null;
        if (order != null && StringUtils.isNotEmpty(order.getOrderNo())) {
            payLog = payLogService.selectAppPayLogByPayNo(order.getOrderNo());
        }
        if (payLog == null && order != null && order.getOrderId() != null) {
            AppPayLog query = new AppPayLog();
            query.setOrderId(order.getOrderId());
            query.setOrderType("3");
            List<AppPayLog> logs = payLogService.selectAppPayLogList(query);
            if (logs != null && !logs.isEmpty()) {
                payLog = logs.get(0);
                for (AppPayLog item : logs) {
                    if (item.getLogId() != null && payLog.getLogId() != null
                            && item.getLogId() > payLog.getLogId()) {
                        payLog = item;
                    }
                }
            }
        }
        return payLog;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredUnpaidOrders(int expireMinutes)
    {
        int minutes = expireMinutes > 0 ? expireMinutes : 30;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minutes);
        Date expireBefore = calendar.getTime();

        AppActivityOrder query = new AppActivityOrder();
        query.setPayStatus("0");
        query.getParams().put("expireBefore", expireBefore);
        List<AppActivityOrder> list = appActivityOrderMapper.selectAppActivityOrderList(query);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int closed = 0;
        Date now = DateUtils.getNowDate();
        for (AppActivityOrder order : list) {
            if (order == null || order.getOrderId() == null) {
                continue;
            }
            AppActivityOrder latest = appActivityOrderMapper.selectAppActivityOrderByOrderId(order.getOrderId());
            if (latest == null || !"0".equals(StringUtils.defaultIfBlank(latest.getPayStatus(), ""))) {
                continue;
            }
            closePayLogByPayNo(latest.getOrderNo(), now);
            AppActivityOrder up = new AppActivityOrder();
            up.setOrderId(latest.getOrderId());
            up.setStatus("2");
            up.setPayStatus("2");
            up.setUpdateTime(now);
            if (appActivityOrderMapper.updateAppActivityOrder(up) > 0) {
                closed++;
            }
        }
        if (closed > 0) {
            log.info("超时关闭未支付活动订单 {} 笔，阈值={}分钟", closed, minutes);
        }
        return closed;
    }

    private void closePayLogByPayNo(String payNo, Date now)
    {
        if (StringUtils.isEmpty(payNo)) {
            return;
        }
        AppPayLog payLog = payLogService.selectAppPayLogByPayNo(payNo);
        if (payLog == null || !"0".equals(StringUtils.defaultIfBlank(payLog.getStatus(), ""))) {
            return;
        }
        AppPayLog up = new AppPayLog();
        up.setLogId(payLog.getLogId());
        up.setStatus("2");
        up.setUpdateTime(now);
        payLogService.updateAppPayLog(up);
    }

    private int parseCount(String value)
    {
        if (StringUtils.isEmpty(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void assertNoConfirmedDuplicate(Long userId, Long activityId)
    {
        AppActivityOrder confirmedQuery = new AppActivityOrder();
        confirmedQuery.setUserId(userId);
        confirmedQuery.setActivityId(activityId);
        confirmedQuery.setPayStatus("1");
        List<AppActivityOrder> confirmedList = appActivityOrderMapper.selectAppActivityOrderList(confirmedQuery);
        if (confirmedList == null || confirmedList.isEmpty()) {
            return;
        }
        for (AppActivityOrder item : confirmedList) {
            String st = StringUtils.defaultIfBlank(item.getStatus(), "");
            // 已报名成功且未取消/退款
            if (!"2".equals(st) && !"3".equals(st) && !"4".equals(st)) {
                throw new ServiceException("您已报名该活动");
            }
        }
    }

    /**
     * 查找仍有效的待支付报名单（排除已取消/退款）
     */
    private AppActivityOrder findActivePendingOrder(Long userId, Long activityId)
    {
        AppActivityOrder pendingQuery = new AppActivityOrder();
        pendingQuery.setUserId(userId);
        pendingQuery.setActivityId(activityId);
        pendingQuery.setPayStatus("0");
        List<AppActivityOrder> pendingList = appActivityOrderMapper.selectAppActivityOrderList(pendingQuery);
        if (pendingList == null || pendingList.isEmpty()) {
            return null;
        }
        for (AppActivityOrder item : pendingList) {
            String st = StringUtils.defaultIfBlank(item.getStatus(), "0");
            // status=0 待支付；已取消(2)/退款中(3)/已退款(4)不算
            if ("0".equals(st) || StringUtils.isEmpty(st)) {
                return item;
            }
        }
        return null;
    }

    private BigDecimal resolveUnitPrice(AppActivity activity)
    {
        BigDecimal vipPrice = activity.getVipPrice();
        if (vipPrice != null && vipPrice.compareTo(BigDecimal.ZERO) > 0) {
            return vipPrice;
        }
        BigDecimal price = activity.getPrice();
        return price != null ? price : BigDecimal.ZERO;
    }
}
