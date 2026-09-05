package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.common.utils.wxpay.jsapi.WxpayJsapiServiceExtensionUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.*;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.auth.Credential;
import com.wechat.pay.java.core.auth.Validator;
import com.wechat.pay.java.core.cipher.PrivacyDecryptor;
import com.wechat.pay.java.core.cipher.PrivacyEncryptor;
import com.wechat.pay.java.core.cipher.Signer;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RSACombinedNotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.ecommercerefund.model.CreateRefundRequest;
import com.wechat.pay.java.service.ecommercerefund.model.RefundAmount;
import com.wechat.pay.java.service.ecommercerefund.model.RefundReqAmount;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.*;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.log.Log;
import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 商品订单Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
@Slf4j
public class AppGoodsOrderServiceImpl implements IAppGoodsOrderService 
{
    @Autowired
    private AppGoodsOrderMapper appGoodsOrderMapper;
    @Autowired
    private AppGoodsOrderDetailMapper orderDetailMapper;
    @Autowired
    private AppUserAddressMapper userAddressMapper;
    @Autowired
    private AppGoodsCouponMapper couponMapper;
    @Autowired
    private com.ruoyi.system.mapper.AppGoodsCouponGotMapper couponGotMapper;
    @Autowired
    private IAppUserInfoService userInfoService;
    @Autowired
    private Config wxPayConfigRuntime;
    @Autowired
    private WechatPrepayService wechatPrepayService;
    @Autowired
    private NotificationConfig wxPayNotificationConfig;
    @Autowired
    private IAppPayLogService payLogService;
    @Autowired
    private IAppUserCardService userCardService;
    @Autowired
    private IAppUserCardService appUserCardService;
    @Autowired
    private IAppGoldService goldService;
    @Autowired
    private AppPayRefundLogMapper appPayRefundLogMapper;
    @Autowired
    private AppGoodsOrderAfterMapper appGoodsOrderAfterMapper;
    @Autowired
    private AppGoodsSkuMapper appGoodsSkuMapper;
    @Autowired
    private AppGoodsSkuOptionMapper appGoodsSkuOptionMapper;
    @Autowired
    private AppGoodsMapper appGoodsMapper;
    @Autowired
    private AppGoodsEducationExtMapper appGoodsEducationExtMapper;
    @Autowired
    private IAppActivityOrderService activityOrderService;
    @Autowired
    private IAppCardService cardService;
    private IAppGoodsSkuDataService skuDataService;

    /** 微信支付单有效期（分钟），需与小程序收银台倒计时保持一致 */
    private static final long WX_PAY_EXPIRE_MINUTES = 30L;

    /** 小程序APPID */
    @Value("${wx.pay.appId}")
    private String appId;
    /** 商户号 */
    @Value("${wx.pay.merchantId}")
    private String merchantId;
    /** 支付（回调）通知地址 */
    @Value("${wx.pay.payNotifyUrl}")
    public String payNotifyUrl;
    @Value("${wx.pay.refundNotifyUrl}")
    public String refundNotifyUrl;


    /**
     * 查询商品订单
     * 
     * @param orderId 商品订单主键
     * @return 商品订单
     */
    @Override
    public AppGoodsOrder selectAppGoodsOrderByOrderId(Long orderId)
    {
        return appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
    }

    /**
     * 查询商品订单列表
     * 
     * @param appGoodsOrder 商品订单
     * @return 商品订单
     */
    @Override
    public List<AppGoodsOrder> selectAppGoodsOrderList(AppGoodsOrder appGoodsOrder)
    {
        return appGoodsOrderMapper.selectAppGoodsOrderList(appGoodsOrder);
    }

    /**
     * 新增商品订单
     * 
     * @param order 商品订单
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppGoodsOrder insertAppGoodsOrder(AppGoodsOrder order)
    {
        if (order == null || order.getGoodsId() == null) {
            throw new ServiceException("商品无效");
        }
        AppGoods goods = appGoodsMapper.selectAppGoodsByGoodsId(order.getGoodsId());
        order = NewGoodsOrderPolicy.prepare(order, goods);
        validateEducationOrder(order, goods);

        order.setCreateTime(DateUtils.getNowDate());
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("0");
        }
        if (order.getPayStatus() == null || order.getPayStatus().isEmpty()) {
            order.setPayStatus("0");
        }
        if ("hotel".equals(goods.getGoodsType())) {
            order.setTravelStatus(TravelOrderStatusPolicy.PENDING_CONFIRMATION);
        } else {
            order.setTravelStatus(null);
            order.setTravelStatusBeforeRefund(null);
        }
        Integer isSku = goods.getIsSku();
        if (isSku == null || isSku != 1) {
            BigDecimal unitPrice = "online".equals(goods.getGoodsType()) ? goods.getPrice() : goods.getVipPrice();
            if (unitPrice == null || unitPrice.signum() <= 0) {
                throw new ServiceException("商品价格未配置");
            }
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(order.getGoodsCount()));
            if ("hotel".equals(goods.getGoodsType())) {
                total = total.multiply(BigDecimal.valueOf(order.getInterCount()));
            }
            order.setMoneyPayable(total.setScale(2, RoundingMode.HALF_UP));
            order.setPayMoney(order.getMoneyPayable());
        }
        // todo 上述代码可以删除
        //AppGoodsSkuData skuData = new AppGoodsSkuData();
        if (isSku != null && isSku == 1) {
            /*skuData = skuDataService.selectAppGoodsSkuDataByDataId(order.getSkuDataId());
            if(order.getGoodsCount()>1) {
                order.setMoneyPayable(skuData.getDataPrice().multiply(new BigDecimal(order.getGoodsCount())));
                order.setPayMoney(skuData.getDataPrice().multiply(new BigDecimal(order.getGoodsCount())));
            }else{
                order.setMoneyPayable(skuData.getDataPrice());
                order.setPayMoney(skuData.getDataPrice());
            }*/
            AppGoodsSku sku = null;
            AppGoodsSku selsku = null;
            if (null != order.getSkuId() && order.getSkuId() > 0) {
                sku = appGoodsSkuMapper.selectAppGoodsSkuBySkuId(order.getSkuId());
            }
            if (null != order.getSelfSkuId() && order.getSelfSkuId() > 0) {
                selsku = appGoodsSkuMapper.selectAppGoodsSkuBySkuId(order.getSelfSkuId());
            }
            if (sku == null) {
                throw new ServiceException("未找到房型/套餐规格，无法下单");
            }
            NewGoodsOrderPolicy.requireOwnedSku(goods, sku);
            if (!"200".equals(sku.getSkuType()) && !"202".equals(sku.getSkuType())) {
                throw new ServiceException("请选择房型或住宿套餐");
            }
            if (!isSkuEnabled(sku)) {
                throw new ServiceException("所选房型或套餐已停用，请返回重新选择");
            }
            if (order.getSelfSkuId() != null && order.getSelfSkuId() > 0) {
                if (selsku == null) {
                    throw new ServiceException("未找到供餐方案，请返回重新选择");
                }
                NewGoodsOrderPolicy.requireOwnedSku(goods, selsku);
                if (!isSkuEnabled(selsku)) {
                    throw new ServiceException("供餐方案已停用");
                }
            }
            if (order.getInterCount() == null || order.getInterCount() <= 0) {
                throw new ServiceException("入住晚数无效，请返回重新选择");
            }
            int nights = order.getInterCount();
            long rooms = order.getGoodsCount() != null && order.getGoodsCount() > 0 ? order.getGoodsCount() : 1L;

            // 自选晚数：skuType=200 标准房型 + 无有效组合序号 → 售价(每晚) × 晚数 × 房间数
            boolean customNight = "200".equals(StringUtils.trimToEmpty(sku.getSkuType()))
                    && (order.getSkuSeqNo() == null || order.getSkuSeqNo() <= 0);
            BigDecimal roomUnitPrice;
            if (customNight) {
                if (sku.getPrice() == null || sku.getPrice() <= 0) {
                    throw new ServiceException("该房型未配置每晚单价，无法自选晚数下单");
                }
                if (nights < CUSTOM_NIGHT_MIN) {
                    throw new ServiceException("自选晚数不能少于" + CUSTOM_NIGHT_MIN + "晚");
                }
                roomUnitPrice = BigDecimal.valueOf(sku.getPrice())
                        .multiply(new BigDecimal(nights))
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("旅居自选晚数计价 skuId={}, nights={}, rooms={}, perNight={}, roomTotal={}",
                        order.getSkuId(), nights, rooms, sku.getPrice(), roomUnitPrice);
            } else {
                List<AppGoodsSkuOption> scopedOptions = loadSkuOptions(order.getSkuId(), order.getSkuSeqNo());
                List<AppGoodsSkuOption> allOptions = loadSkuOptions(order.getSkuId(), null);
                List<AppGoodsSkuOption> priceOptions = (scopedOptions != null && !scopedOptions.isEmpty())
                        ? scopedOptions : allOptions;

                BigDecimal packageTotal = findSkuOptionPrice(priceOptions, "302");
                BigDecimal perNightPrice = findSkuOptionPrice(priceOptions, "301");
                if (packageTotal == null) {
                    packageTotal = findSkuOptionPrice(allOptions, "302");
                }
                if (perNightPrice == null) {
                    perNightPrice = findSkuOptionPrice(allOptions, "301");
                }

                // 晚数只认公共属性(skuSeqNo=0)的 303，避免组合序号上的「数量」被误当成入住晚数
                Integer packageNights = resolvePackageNights(allOptions, sku.getSkuName());
                if (packageNights != null && packageNights > 0 && nights != packageNights.intValue()) {
                    throw new ServiceException("入住晚数与套餐不符（套餐"
                            + packageNights + "晚，当前" + nights + "晚），请返回重新选择");
                }

                if (packageTotal != null) {
                    roomUnitPrice = packageTotal;
                } else if (perNightPrice != null) {
                    roomUnitPrice = perNightPrice.multiply(new BigDecimal(nights))
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    // 固定套餐禁止用规格售价冒充整段房价，避免少收/错收
                    throw new ServiceException("套餐价格未配置完整，无法下单");
                }
            }
            BigDecimal payable = roomUnitPrice.multiply(new BigDecimal(rooms)).setScale(2, RoundingMode.HALF_UP);
            if (selsku != null && selsku.getPrice() != null && selsku.getPrice() > 0) {
                long people = order.getSelfGoodsCount() != null ? order.getSelfGoodsCount() : 0L;
                payable = payable.add(BigDecimal.valueOf(selsku.getPrice())
                        .multiply(new BigDecimal(people))
                        .multiply(new BigDecimal(nights))).setScale(2, RoundingMode.HALF_UP);
            }
            if (payable.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("订单金额异常，请返回重新选择");
            }
            order.setMoneyPayable(payable);
            order.setPayMoney(payable);
            log.info("旅居下单计价 skuId={}, seq={}, customNight={}, nights={}, rooms={}, roomUnit={}, payable={}",
                    order.getSkuId(), order.getSkuSeqNo(), customNight, nights, rooms, roomUnitPrice, payable);
        }
        /*order.setMoneyPayable(order.getGoodsList().get(0).getVipPrice());
        order.setPayMoney(order.getGoodsList().get(0).getVipPrice());*/
        order.setMoneyTotal(order.getMoneyPayable());
        AppGoodsCouponGot usedCoupon = applyCoupon(order, goods);
        if (usedCoupon != null && order.getMoneyPayable().signum() == 0) {
            order.setStatus("1");
            order.setPayStatus("1");
            order.setPayType("coupon");
            order.setPayTime(DateUtils.getNowDate());
            if ("hotel".equals(goods.getGoodsType())) {
                order.setTravelStatus(TravelOrderStatusPolicy.CONFIRMED);
            }
        }
        reserveStockIfNeeded(order, goods);
        int rs = appGoodsOrderMapper.insertAppGoodsOrder(order);
        if (rs != 1 || order.getOrderId() == null) {
            throw new ServiceException("订单创建失败");
        }
        if (rs > 0) {
            order.setOrderNo(MerchantOrderNumbers.create("20", order.getOrderId()));
            if (appGoodsOrderMapper.updateAppGoodsOrder(order) != 1) {
                throw new ServiceException("订单编号保存失败");
            }
            if (usedCoupon != null) {
                BigDecimal discount = order.getMoneyDiscount();
                if (couponGotMapper.markUsed(usedCoupon.getGotId(), order.getOrderId(), discount) != 1) {
                    throw new ServiceException("优惠券已被使用，请重新提交订单");
                }
                couponMapper.incrementUsedCount(usedCoupon.getCouponId());
            }
            // 订单详情
            AppGoodsOrderDetail orderDetail = new AppGoodsOrderDetail();
            orderDetail.setUserId(order.getUserId());
            orderDetail.setOrderId(order.getOrderId());
            orderDetail.setGoodsId(order.getGoodsList().get(0).getGoodsId());
            orderDetail.setGoodsCount(order.getGoodsCount());
            orderDetail.setGoodsMoney(order.getPayMoney());
            orderDetail.setDiscountMoney(order.getMoneyDiscount());
            orderDetail.setIsSku(order.getGoodsList().get(0).getIsSku());
            orderDetail.setSkuId(order.getSkuId());
            orderDetail.setSkuSeqNo(order.getSkuSeqNo());
            orderDetail.setSelfSkuId(order.getSelfSkuId());
            orderDetail.setSelfGoodsCount(order.getSelfGoodsCount());
            orderDetail.setInterCount(order.getInterCount());
            if (orderDetail.getIsSku() != null && orderDetail.getIsSku() == 1) {
                /*orderDetail.setSkuDataId(order.getSkuDataId());
                orderDetail.setSkuDataValues(skuData.getDataValues());*/
                orderDetail.setSkuId(order.getSkuId());
                orderDetail.setSelfSkuId(order.getSelfSkuId());
                orderDetail.setSkuSeqNo(order.getSkuSeqNo());
            }
            if(null!= order.getCheckInDate()){
                orderDetail.setOrderStartDate(order.getCheckInDate());
            }
            if(null!= order.getCheckOutDate()){
                orderDetail.setOrderEndDate(order.getCheckOutDate());
            }
            if (orderDetailMapper.insertAppGoodsOrderDetail(orderDetail) != 1) {
                throw new ServiceException("订单明细保存失败");
            }
            List<AppGoodsOrderDetail> detailList = new ArrayList<>();
            detailList.add(orderDetail);
            order.setOrderDetailList(detailList);
            // 订单地址
            if (order.getAddressId() != null && order.getAddressId() > 0) {
                AppUserAddress userAddress = userAddressMapper.selectAppUserAddressByAddressId(order.getAddressId());
                order.setAddressInfo(userAddress);
            }
        }
        return order;
    }

    private void reserveStockIfNeeded(AppGoodsOrder order, AppGoods goods) {
        if (!("education".equals(goods.getGoodsType()) || "online".equals(goods.getGoodsType()))) {
            return;
        }
        long reserveCount = order.getGoodsCount() != null ? order.getGoodsCount() : 1L;
        if (appGoodsMapper.reserveStock(goods.getGoodsId(), reserveCount) <= 0) {
            throw new ServiceException("education".equals(goods.getGoodsType())
                    ? "报名名额已满" : "商品库存不足");
        }
    }

    private AppGoodsCouponGot applyCoupon(AppGoodsOrder order, AppGoods goods) {
        String raw = StringUtils.trimToEmpty(order.getCouponGotIds());
        if (raw.isEmpty()) {
            AppGoodsCouponGot automatic = couponGotMapper.selectBestChannelCoupon(
                    order.getUserId(), goods.getGoodsId(), goods.getCategoryId(), order.getMoneyPayable());
            if (automatic == null) {
                order.setMoneyDiscount(BigDecimal.ZERO);
                return null;
            }
            raw = String.valueOf(automatic.getGotId());
            order.setCouponGotIds(raw);
        }
        if (!raw.matches("\\d+")) {
            throw new ServiceException("每单只能使用一张优惠券");
        }
        AppGoodsCouponGot got = couponGotMapper.selectForUpdate(Long.valueOf(raw));
        if (got == null || !order.getUserId().equals(got.getUserId())
                || got.getIsUsed() == null || got.getIsUsed() != 0 || !"1".equals(got.getStatus())) {
            throw new ServiceException("优惠券不可用");
        }
        AppGoodsCoupon coupon = couponMapper.selectAppGoodsCouponByCouponId(got.getCouponId());
        Date now = new Date();
        if (coupon == null || !"1".equals(coupon.getStatus())
                || (coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime()))
                || (coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime()))) {
            throw new ServiceException("优惠券已失效");
        }
        if (coupon.getGoodsId() != null && coupon.getGoodsId() > 0
                && !coupon.getGoodsId().equals(goods.getGoodsId())) {
            throw new ServiceException("该优惠券不适用于此商品");
        }
        if (coupon.getCategoryId() != null && coupon.getCategoryId() > 0
                && !coupon.getCategoryId().equals(goods.getCategoryId())) {
            throw new ServiceException("该优惠券不适用于此分类");
        }
        BigDecimal payable = order.getMoneyPayable();
        if (coupon.getMinPrice() != null && payable.compareTo(coupon.getMinPrice()) < 0) {
            throw new ServiceException("订单金额未达到优惠券使用门槛");
        }
        BigDecimal discount;
        if ("2".equals(coupon.getDiscountType())) {
            discount = calculatePercentageDiscount(payable, coupon.getDiscountPrice());
        } else {
            discount = coupon.getDiscountPrice();
        }
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("优惠券优惠金额无效");
        }
        discount = discount.min(payable).setScale(2, RoundingMode.HALF_UP);
        order.setMoneyTotal(payable);
        order.setMoneyDiscount(discount);
        order.setMoneyPayable(payable.subtract(discount));
        order.setPayMoney(order.getMoneyPayable());
        order.setDistributionChannelCode(got.getChannelCode());
        return got;
    }

    static BigDecimal calculatePercentageDiscount(BigDecimal payable, BigDecimal percent) {
        if (percent == null || percent.compareTo(BigDecimal.ZERO) <= 0
                || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException("优惠券折扣配置错误");
        }
        return payable.multiply(new BigDecimal("100").subtract(percent))
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void validateEducationOrder(AppGoodsOrder order, AppGoods goods) {
        if (goods == null || !"education".equals(goods.getGoodsType())) {
            return;
        }
        if (goods.getStatus() == null || !"1".equals(goods.getStatus())) {
            throw new ServiceException("课程未上架，暂不可报名");
        }
        Long goodsCount = order.getGoodsCount();
        if (goodsCount == null || goodsCount != 1L) {
            throw new ServiceException("课程报名每次仅限1人");
        }
        if (StringUtils.isEmpty(order.getContactName())) {
            throw new ServiceException("请填写报名人姓名");
        }
        if (StringUtils.isEmpty(order.getContactPhone()) || !order.getContactPhone().matches("^1\\d{10}$")) {
            throw new ServiceException("请填写正确的手机号码");
        }
        Long stock = goods.getStock();
        if (stock == null || stock <= 0) {
            throw new ServiceException("报名名额已满");
        }
        AppGoodsEducationExt ext = goods.getEducationExt();
        if (ext == null) {
            ext = appGoodsEducationExtMapper.selectAppGoodsEducationExtByGoodsId(goods.getGoodsId());
        }
        if (ext != null) {
            LocalDate today = LocalDate.now();
            if (StringUtils.isNotEmpty(ext.getSignupStart())) {
                LocalDate signupStart = LocalDate.parse(ext.getSignupStart());
                if (today.isBefore(signupStart)) {
                    throw new ServiceException("报名尚未开始");
                }
            }
            if (StringUtils.isNotEmpty(ext.getSignupEnd())) {
                LocalDate signupEnd = LocalDate.parse(ext.getSignupEnd());
                if (today.isAfter(signupEnd)) {
                    throw new ServiceException("报名时间已截止");
                }
            }
            Integer classSizeMax = ext.getClassSizeMax();
            if (classSizeMax != null && classSizeMax > 0) {
                int enrolled = appGoodsOrderMapper.countEducationActiveSignupByGoodsId(goods.getGoodsId());
                if (enrolled >= classSizeMax) {
                    throw new ServiceException("课程名额已满");
                }
            }
        }
    }

    /**
     * 修改商品订单
     * 
     * @param appGoodsOrder 商品订单
     * @return 结果
     */
    @Override
    public int updateAppGoodsOrder(AppGoodsOrder appGoodsOrder)
    {
        AppGoodsOrder current = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(appGoodsOrder.getOrderId());
        appGoodsOrder.setTravelStatus(null);
        appGoodsOrder.setTravelStatusBeforeRefund(null);
        if (current != null && current.getTravelStatus() != null
                && "0".equals(current.getPayStatus())
                && "2".equals(appGoodsOrder.getStatus()) && !"2".equals(current.getStatus())) {
            appGoodsOrder.setTravelStatus(TravelOrderStatusPolicy.CANCELLED);
        }
        appGoodsOrder.setUpdateTime(DateUtils.getNowDate());
        return appGoodsOrderMapper.updateAppGoodsOrder(appGoodsOrder);
    }

    @Override
    public int updateTravelStatus(Long orderId, String travelStatus)
    {
        AppGoodsOrder current = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
        if (current == null) throw new ServiceException("订单不存在");
        AppGoods goods = appGoodsMapper.selectAppGoodsByGoodsId(current.getGoodsId());
        if (goods == null || !"hotel".equals(goods.getGoodsType())) {
            throw new ServiceException("只有旅居订单可以修改旅居状态");
        }
        try {
            TravelOrderStatusPolicy.requireManualTransition(current.getTravelStatus(), travelStatus);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ex.getMessage());
        }
        AppGoodsOrder update = new AppGoodsOrder();
        update.setOrderId(orderId);
        update.setTravelStatus(travelStatus);
        update.setUpdateTime(DateUtils.getNowDate());
        return appGoodsOrderMapper.updateAppGoodsOrder(update);
    }

    /**
     * 批量删除商品订单
     * 
     * @param orderIds 需要删除的商品订单主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderByOrderIds(Long[] orderIds)
    {
        return appGoodsOrderMapper.deleteAppGoodsOrderByOrderIds(orderIds);
    }

    /**
     * 删除商品订单信息
     * 
     * @param orderId 商品订单主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderByOrderId(Long orderId)
    {
        return appGoodsOrderMapper.deleteAppGoodsOrderByOrderId(orderId);
    }

    /**
     * 订单进行微信支付
     * @param goodsOrder
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult wxpayPrepay(AppGoodsOrder goodsOrder) {
        if (goodsOrder == null || goodsOrder.getOrderId() == null) {
            return AjaxResult.error("非法订单");
        }
        goodsOrder = appGoodsOrderMapper.selectAppGoodsOrderByOrderIdForUpdate(goodsOrder.getOrderId());
        if (goodsOrder == null || !"0".equals(goodsOrder.getStatus()) || !"0".equals(goodsOrder.getPayStatus())) {
            return AjaxResult.error("订单已关闭或已支付");
        }
        if (goodsOrder.getMoneyPayable() == null || goodsOrder.getMoneyPayable().signum() <= 0) {
            return AjaxResult.error("订单金额无效");
        }
        AjaxResult rs = AjaxResult.error();
        try {
            //goodsOrder.setMoneyPayable(new BigDecimal(0.01));
            // ... 调用接口
            AppPayLog payLog = null;
            payLog = payLogService.selectAppPayLogByPayNo(goodsOrder.getOrderNo());
            String paymentNo = MerchantOrderNumbers.forPayment(goodsOrder.getOrderNo(), "20", goodsOrder.getOrderId());
            if (!paymentNo.equals(goodsOrder.getOrderNo())) {
                if (payLog != null) throw new ServiceException("已有支付记录，不能修改支付单号");
                goodsOrder.setOrderNo(paymentNo);
                AppGoodsOrder numbered = new AppGoodsOrder();
                numbered.setOrderId(goodsOrder.getOrderId()); numbered.setOrderNo(paymentNo);
                if (appGoodsOrderMapper.updateAppGoodsOrder(numbered) != 1) throw new ServiceException("支付单号保存失败");
            }

            AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(goodsOrder.getUserId());
            if (userInfo == null || StringUtils.isEmpty(userInfo.getWeixinOpenid())) {
                throw new ServiceException("微信用户信息缺失，请重新登录");
            }
            PrepayRequest request = new PrepayRequest();
            request.setAppid(appId);
            request.setMchid(merchantId);
            request.setDescription("逸享荟商品订单" + goodsOrder.getOrderNo());
            request.setNotifyUrl(payNotifyUrl);
            // 同一订单重试复用商户单号，避免生成可重复支付的交易。
            request.setOutTradeNo(goodsOrder.getOrderNo());

            // 支付截止时间与订单创建时间 + 30 分钟对齐（同收银台/关单任务）
            // 微信要求 time_expire 至少为下单后 1 分钟
            ZonedDateTime expireAt;
            if (goodsOrder.getCreateTime() != null) {
                expireAt = goodsOrder.getCreateTime().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .plusMinutes(WX_PAY_EXPIRE_MINUTES);
            } else {
                expireAt = ZonedDateTime.now().plusMinutes(WX_PAY_EXPIRE_MINUTES);
            }
            ZonedDateTime minExpire = ZonedDateTime.now().plusMinutes(1);
            if (!expireAt.isAfter(minExpire)) {
                return AjaxResult.error("支付已超时，请重新下单");
            }
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            request.setTimeExpire(expireAt.format(formatter));
            Amount amount = new Amount();
            amount.setTotal(goodsOrder.getMoneyPayable().multiply(new BigDecimal(100)).intValueExact());
            amount.setCurrency("CNY");
            request.setAmount(amount);
            Payer payer = new Payer();
            payer.setOpenid(userInfo.getWeixinOpenid());
            request.setPayer(payer);
            // 平台券已计入应付总额；明细留在本系统，不上传非必填的微信单品营销报文。
            SceneInfo sceneInfo = new SceneInfo();
            sceneInfo.setPayerClientIp(IpUtils.getHostIp());
            request.setSceneInfo(sceneInfo);
            PrepayWithRequestPaymentResponse response = wechatPrepayService.create(request);
            rs = AjaxResult.success(response);
            if(null == payLog){
                payLog = new AppPayLog();
                payLog.setOrderId(goodsOrder.getOrderId());
                payLog.setPayNo(goodsOrder.getOrderNo());
                payLog.setPayName("微信支付");
                payLog.setPayDescription(request.getDescription());
                payLog.setPayMoney(goodsOrder.getMoneyPayable().multiply(new BigDecimal(100)));
                payLog.setPayMethod("wxpay");
                payLog.setStatus("0");
                payLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=",""));
                payLog.setAgentName("微信支付");
                payLog.setCreateTime(DateUtils.getNowDate());
                payLog.setOrderType("2");
                payLog.setUserId(goodsOrder.getUserId());
                if (payLogService.insertAppPayLog(payLog) != 1) throw new ServiceException("支付记录保存失败");
                goodsOrder.setPayStatus("0");
                goodsOrder.setUpdateTime(DateUtils.getNowDate());
                appGoodsOrderMapper.updateAppGoodsOrder(goodsOrder);
            }else{
                payLog.setPayNo(goodsOrder.getOrderNo());
                payLog.setUpdateTime(DateUtils.getNowDate());
                payLog.setAgentPayNo(response.getPackageVal().replaceAll("prepay_id=",""));
                payLog.setPayMoney(goodsOrder.getMoneyPayable().multiply(new BigDecimal(100)));
                payLog.setPayDescription(request.getDescription());
                payLogService.updateAppPayLog(payLog);
            }
            appGoodsOrderMapper.updateAppGoodsOrder(goodsOrder);
            log.info("订单【{}】发起预支付成功", goodsOrder.getOrderId());
        } catch (HttpException e) { // 发送HTTP请求失败
            log.error("微信下单发送HTTP请求失败，错误信息：{}", e.getMessage());
        } catch (com.ruoyi.common.exception.ServiceException e) { // 服务返回状态小于200或大于等于300，例如500
            throw e;
        } catch (MalformedMessageException e) { // 服务返回成功，返回体类型不合法，或者解析返回体失败
            log.error("服务返回成功，返回体类型不合法，或者解析返回体失败，错误信息：{}", e.getMessage());
            throw new ServiceException("下单失败");
        }
        return rs;
    }

    @Override
    public AjaxResult wxpayRefund(AppGoodsOrderAfter appGoodsOrderAfter) {
        AjaxResult rs = AjaxResult.error();
        try {
            if (appGoodsOrderAfter == null || appGoodsOrderAfter.getAfterId() == null) {
                return AjaxResult.error("售后单无效");
            }
            if ("1".equals(appGoodsOrderAfter.getStatus())) {
                if (StringUtils.isEmpty(appGoodsOrderAfter.getOutOrderNo())) {
                    return AjaxResult.error("原支付单号缺失");
                }
                if (appGoodsOrderAfter.getRefundMoney() == null) {
                    return AjaxResult.error("退款金额无效");
                }
                long refundFen = toFen(appGoodsOrderAfter.getRefundMoney());
                if (refundFen <= 0) {
                    return AjaxResult.error("退款金额无效");
                }
                long totalFen = resolveOriginalPayFen(appGoodsOrderAfter);
                if (totalFen <= 0) {
                    return AjaxResult.error("原支付金额无效，无法退款");
                }
                if (refundFen > totalFen) {
                    return AjaxResult.error("退款金额不能大于实付金额");
                }

                RefundService service = new RefundService.Builder().config(wxPayConfigRuntime).build();
                String outRefundNo = "RF" + DateUtils.dateTimeNow() + appGoodsOrderAfter.getAfterId();
                CreateRequest refundRequest = new CreateRequest();
                refundRequest.setOutTradeNo(appGoodsOrderAfter.getOutOrderNo());
                refundRequest.setOutRefundNo(outRefundNo);
                refundRequest.setReason(StringUtils.isNotEmpty(appGoodsOrderAfter.getRemark())
                        ? appGoodsOrderAfter.getRemark() : "用户申请退款");
                refundRequest.setNotifyUrl(refundNotifyUrl);
                AmountReq refundAmount = new AmountReq();
                refundAmount.setRefund(refundFen);
                refundAmount.setCurrency("CNY");
                refundAmount.setTotal(totalFen);
                refundRequest.setAmount(refundAmount);

                Refund response;
                try {
                    response = service.create(refundRequest);
                } catch (com.wechat.pay.java.core.exception.ServiceException wxEx) {
                    // 微信侧已全额退款时，补齐本地终态，避免「钱已退但系统失败」
                    String errCode = wxEx.getErrorCode() != null ? wxEx.getErrorCode() : "";
                    String errMsg = wxEx.getErrorMessage() != null ? wxEx.getErrorMessage() : wxEx.getMessage();
                    log.error("微信退款业务失败 afterId={}, code={}, msg={}",
                            appGoodsOrderAfter.getAfterId(), errCode, errMsg);
                    if (isAlreadyRefundedError(errCode, errMsg)) {
                        markGoodsRefundAccepted(appGoodsOrderAfter, null, outRefundNo, refundFen, true);
                        return AjaxResult.success("微信侧已退款，系统状态已同步");
                    }
                    return AjaxResult.error(StringUtils.defaultIfBlank(errMsg, "退款失败"));
                }

                boolean refundDone = response != null && Status.SUCCESS.equals(response.getStatus());
                try {
                    markGoodsRefundAccepted(appGoodsOrderAfter, response, outRefundNo, refundFen, refundDone);
                } catch (Exception persistEx) {
                    log.error("微信退款已受理但本地落库失败 afterId={}, outRefundNo={}",
                            appGoodsOrderAfter.getAfterId(), outRefundNo, persistEx);
                    // 钱可能已退，返回明确提示，便于运营再次同步
                    return AjaxResult.error("微信退款已受理，但系统状态更新失败，请稍后在订单中同步退款结果");
                }
            } else {
                // 审核拒绝：售后关闭，订单恢复已支付
                AppGoodsOrderAfter tmpAppGoodsOrderAfter = new AppGoodsOrderAfter();
                tmpAppGoodsOrderAfter.setAfterId(appGoodsOrderAfter.getAfterId());
                tmpAppGoodsOrderAfter.setRemark(appGoodsOrderAfter.getRemark());
                tmpAppGoodsOrderAfter.setStatus("2");
                tmpAppGoodsOrderAfter.setUpdateTime(DateUtils.getNowDate());
                appGoodsOrderAfterMapper.updateAppGoodsOrderAfter(tmpAppGoodsOrderAfter);
                if (appGoodsOrderAfter.getOrderId() != null) {
                    AppGoodsOrder upOrder = new AppGoodsOrder();
                    upOrder.setOrderId(appGoodsOrderAfter.getOrderId());
                    upOrder.setStatus("1");
                    restoreTravelStatusAfterRefund(upOrder);
                    upOrder.setUpdateTime(DateUtils.getNowDate());
                    appGoodsOrderMapper.updateAppGoodsOrder(upOrder);
                }
            }
            rs = AjaxResult.success();
        } catch (HttpException e) {
            log.error("微信退款发送HTTP请求失败，错误信息：{}", e.getMessage());
            return AjaxResult.error("退款请求失败");
        } catch (MalformedMessageException e) {
            log.error("微信退款响应解析失败，错误信息：{}", e.getMessage());
            return AjaxResult.error("退款请求失败");
        } catch (ServiceException e) {
            log.error("微信退款业务失败，错误信息：{}", e.getMessage());
            return AjaxResult.error(StringUtils.defaultIfBlank(e.getMessage(), "退款失败"));
        } catch (Exception e) {
            log.error("微信退款异常", e);
            return AjaxResult.error("退款失败");
        }
        return rs;
    }

    private long toFen(BigDecimal yuan) {
        if (yuan == null) {
            return 0L;
        }
        return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 原支付金额（分）：优先支付日志，其次售后单/订单实付
     */
    private long resolveOriginalPayFen(AppGoodsOrderAfter after) {
        if (after == null) {
            return 0L;
        }
        if (StringUtils.isNotEmpty(after.getOutOrderNo())) {
            AppPayLog payLog = payLogService.selectAppPayLogByPayNo(after.getOutOrderNo());
            if (payLog != null && payLog.getPayMoney() != null
                    && payLog.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
                return payLog.getPayMoney().setScale(0, RoundingMode.HALF_UP).longValue();
            }
        }
        if (after.getOrderMoney() != null && after.getOrderMoney().compareTo(BigDecimal.ZERO) > 0) {
            return toFen(after.getOrderMoney());
        }
        if (after.getOrderId() != null) {
            AppGoodsOrder order = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(after.getOrderId());
            if (order != null && order.getPayMoney() != null && order.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
                return toFen(order.getPayMoney());
            }
            if (order != null && order.getMoneyPayable() != null) {
                return toFen(order.getMoneyPayable());
            }
        }
        return 0L;
    }

    private boolean isAlreadyRefundedError(String errCode, String errMsg) {
        String code = StringUtils.defaultString(errCode).toUpperCase();
        String msg = StringUtils.defaultString(errMsg);
        if (msg.contains("已全额退款") || msg.contains("订单已全额退款")
                || msg.contains("超过剩余可退") || msg.contains("可退金额不足")) {
            return true;
        }
        // 部分环境下错误码为 INVALID_REQUEST / USER_ACCOUNT_ABNORMAL 等，靠文案识别更稳妥
        return "INVALID_REQUEST".equals(code) && (msg.contains("退款") || msg.contains("refund"));
    }

    private void markGoodsRefundAccepted(AppGoodsOrderAfter after, Refund response, String outRefundNo,
                                         long refundFen, boolean refundDone) {
        Date now = DateUtils.getNowDate();
        AppPayRefundLog theRefundLog = new AppPayRefundLog();
        theRefundLog.setOrderId(after.getOrderId());
        theRefundLog.setUserId(after.getUserId());
        theRefundLog.setOrderType("2");
        if (response != null && StringUtils.isNotEmpty(response.getRefundId())) {
            theRefundLog.setPayNo(response.getRefundId());
            theRefundLog.setAgentPayNo(response.getTransactionId());
        }
        theRefundLog.setPayMethod("wxpay");
        theRefundLog.setAgentName("微信支付");
        theRefundLog.setAgentRefundNo(outRefundNo);
        theRefundLog.setRefundMoney(new BigDecimal(refundFen));
        theRefundLog.setCreateTime(now);
        theRefundLog.setStatus(refundDone ? "1" : "0");
        if (response != null) {
            theRefundLog.setNotifyContent(String.valueOf(response));
            theRefundLog.setUpdateTime(now);
        }
        appPayRefundLogMapper.insertAppPayRefundLog(theRefundLog);

        AppGoodsOrderAfter tmpAppGoodsOrderAfter = new AppGoodsOrderAfter();
        tmpAppGoodsOrderAfter.setAfterId(after.getAfterId());
        tmpAppGoodsOrderAfter.setRemark(after.getRemark());
        tmpAppGoodsOrderAfter.setStatus(refundDone ? "6" : "1");
        tmpAppGoodsOrderAfter.setRefundMoney(after.getRefundMoney());
        tmpAppGoodsOrderAfter.setUpdateTime(now);
        appGoodsOrderAfterMapper.updateAppGoodsOrderAfter(tmpAppGoodsOrderAfter);

        if (after.getOrderId() != null) {
            AppGoodsOrder upOrder = new AppGoodsOrder();
            upOrder.setOrderId(after.getOrderId());
            upOrder.setStatus(refundDone ? "4" : "3");
            AppGoodsOrder current = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(after.getOrderId());
            if (current != null && current.getTravelStatus() != null) {
                if (!TravelOrderStatusPolicy.REFUNDING.equals(current.getTravelStatus())
                        && !TravelOrderStatusPolicy.REFUNDED.equals(current.getTravelStatus())) {
                    upOrder.setTravelStatusBeforeRefund(current.getTravelStatus());
                }
                upOrder.setTravelStatus(refundDone
                        ? TravelOrderStatusPolicy.REFUNDED : TravelOrderStatusPolicy.REFUNDING);
            }
            if (refundDone) {
                upOrder.setPayStatus("4");
            }
            upOrder.setUpdateTime(now);
            appGoodsOrderMapper.updateAppGoodsOrder(upOrder);
            if (refundDone) {
                AppGoodsOrder latest = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(after.getOrderId());
                releaseEducationStockIfNeeded(latest);
                goldService.reverseOnRefund(after.getUserId(), AppGoldBizType.GOODS_REFUND,
                        after.getOrderId(), new BigDecimal(refundFen), outRefundNo);
                clearGoodsOrderCache(after.getOrderId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String wxpayNotify(HttpServletRequest request) {
//        log.info("------收到支付通知------");
        // 请求头WeChat-Signature
        String signature = request.getHeader("Wechatpay-Signature");
        // 请求头WeChat-nonce
        String nonce = request.getHeader("Wechatpay-Nonce");
        // 请求头WeChat-Timestamp
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        // 微信支付证书序列号
        String serial = request.getHeader("Wechatpay-Serial");
        // 签名方式
        String signType = request.getHeader("Wechatpay-Signature-Type");

        // 构造 RequestParam
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(serial)
                .nonce(nonce)
                .signature(signature)
                .timestamp(timestamp)
                .signType(signType)
                .body(ServletUtils.getBody(request))
                .build();

        NotificationParser parser = new NotificationParser(wxPayNotificationConfig);
        // 以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);
        log.info("支付回调验签成功 outTradeNo={}", transaction.getOutTradeNo());
        return handleVerifiedPayment(transaction);
    }

    String handleVerifiedPayment(Transaction transaction) {
        AppPayLog payLog = payLogService.selectAppPayLogByPayNo(transaction.getOutTradeNo());
       /* payLog.setNotifyContent(transaction.toString());
        payLogService.insertAppPayLog(payLog);
        */
        Map<String, String> returnMap = new HashMap<>(2);
        returnMap.put("code", "FAIL");
        returnMap.put("message", "失败");
        if(null==payLog){
            log.error("订单不存在，非法调用");
            return JSONObject.toJSONString(returnMap);
        }
        if (!Objects.equals(appId, transaction.getAppid())
                || !Objects.equals(merchantId, transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || transaction.getAmount() == null || transaction.getAmount().getTotal() == null
                || !"CNY".equals(transaction.getAmount().getCurrency())
                || payLog.getPayMoney() == null
                || payLog.getPayMoney().compareTo(BigDecimal.valueOf(transaction.getAmount().getTotal())) != 0) {
            log.error("支付回调商户、状态或金额不匹配 payNo={}", payLog.getPayNo());
            return JSONObject.toJSONString(returnMap);
        }
        // 已处理过的支付日志：幂等返回成功；仍尝试补赠币（防回调中断漏赠）
        if ("1".equals(payLog.getStatus())) {
            try {
                goldService.grantByPayLog(payLog);
            } catch (Exception ex) {
                log.warn("已支付回调补赠币失败 payNo={}", payLog.getPayNo(), ex);
            }
            returnMap.put("code", "SUCCESS");
            returnMap.put("message", "成功");
            return JSONObject.toJSONString(returnMap);
        }
        AppGoodsOrder goodsOrder = null;
        AppUserCard userCard = null;
        AppActivityOrder activityOrder = null;
        if(payLog.getPayNo().startsWith("20")) {
            goodsOrder = appGoodsOrderMapper.selectAppGoodsOrderByOrderIdForUpdate(payLog.getOrderId());
            if (Objects.isNull(goodsOrder)) {
                log.error("订单不存在，非法调用");
                return JSONObject.toJSONString(returnMap);
            }
            // 已支付：幂等成功并补赠币；已取消：停止重试（钱账需人工核对）
            if (goodsOrder.getPayStatus() != null && !goodsOrder.getPayStatus().equals("0")) {
                if ("1".equals(goodsOrder.getPayStatus())) {
                    try {
                        goldService.grantByPayLog(payLog);
                    } catch (Exception ex) {
                        log.warn("商品已支付回调补赠币失败 orderId={}", goodsOrder.getOrderId(), ex);
                    }
                    returnMap.put("code", "SUCCESS");
                    returnMap.put("message", "成功");
                    return JSONObject.toJSONString(returnMap);
                }
                log.error("已关闭商品订单收到支付成功回调，需对账 orderId={}", goodsOrder.getOrderId());
                return JSONObject.toJSONString(returnMap);
            }
        }else if(payLog.getPayNo().startsWith("10")) {
            userCard = userCardService.selectAppUserCardByRecordId(payLog.getOrderId());
            if (Objects.isNull(userCard)) {
                log.error("会员卡订单不存在，非法调用");
                return JSONObject.toJSONString(returnMap);
            }
            // 已激活则幂等返回成功，避免微信重复通知
            if ("1".equals(userCard.getStatus())) {
                returnMap.put("code", "SUCCESS");
                returnMap.put("message", "成功");
                return JSONObject.toJSONString(returnMap);
            }
            if ("2".equals(userCard.getStatus()) || "3".equals(userCard.getStatus())) {
                log.warn("会员卡开通单已失效/过期，recordId={}, status={}", userCard.getRecordId(), userCard.getStatus());
                return JSONObject.toJSONString(returnMap);
            }
        }else if(payLog.getPayNo().startsWith("30")) {
            activityOrder = activityOrderService.selectAppActivityOrderByOrderId(payLog.getOrderId());
            if (Objects.isNull(activityOrder)) {
                log.error("活动订单不存在，非法调用");
                return JSONObject.toJSONString(returnMap);
            }
            if (activityOrder.getPayStatus() != null && !activityOrder.getPayStatus().equals("0")) {
                if ("1".equals(activityOrder.getPayStatus())) {
                    try {
                        goldService.grantByPayLog(payLog);
                    } catch (Exception ex) {
                        log.warn("活动已支付回调补赠币失败 orderId={}", activityOrder.getOrderId(), ex);
                    }
                    returnMap.put("code", "SUCCESS");
                    returnMap.put("message", "成功");
                    return JSONObject.toJSONString(returnMap);
                }
                log.error("已关闭活动订单收到支付成功回调，需对账 orderId={}", activityOrder.getOrderId());
                return JSONObject.toJSONString(returnMap);
            }
        }
        if (goodsOrder == null && userCard == null && activityOrder == null) {
            return JSONObject.toJSONString(returnMap);
        }

        payLog.setPayMoney(new BigDecimal(transaction.getAmount().getTotal()));
        payLog.setUpdateTime(DateUtils.getNowDate());
        payLog.setNotifyContent(transaction.toString());

        // 如果微信响应支付成功，且当前状态为待支付，修改订单状态为已支付
        if(null!=goodsOrder) {
            applyGoodsPaySuccess(goodsOrder, payLog, payLog.getPayMoney(), false);
            clearGoodsOrderCache(goodsOrder.getOrderId());
        }

        if(null!=userCard){
            Date now = new Date();
            userCard.setActiveTime(now);
            userCard.setEnableStartTime(now);
            userCard.setEnableEndTime(resolveUserCardEndTime(userCard.getCardId(), now));
            userCard.setStatus("1");
            userCard.setUpdateTime(DateUtils.getNowDate());
            appUserCardService.updateAppUserCard(userCard);
        }
        if(null!=activityOrder){
            activityOrderService.handlePaySuccess(activityOrder, payLog.getPayMoney());
        }

        payLog.setStatus("1");
        payLogService.updateAppPayLog(payLog);

        // 统一金币服务赠币（幂等；会员受 gold.scope.card 控制，默认关闭）
        try {
            goldService.grantByPayLog(payLog);
        } catch (Exception ex) {
            log.warn("支付成功赠币失败 payNo={}", payLog.getPayNo(), ex);
        }
        returnMap.put("code", "SUCCESS");
        returnMap.put("message", "成功");
        return JSONObject.toJSONString(returnMap);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult syncPayResult(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return AjaxResult.error("参数无效");
        }
        AppGoodsOrder goodsOrder = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
        if (goodsOrder == null) {
            return AjaxResult.error("订单不存在");
        }
        if (goodsOrder.getUserId() == null || goodsOrder.getUserId().longValue() != userId.longValue()) {
            return AjaxResult.error("非法订单");
        }
        if ("1".equals(goodsOrder.getPayStatus())) {
            // 已支付仍尝试补赠（防回调中断漏赠，金币服务幂等）
            tryCompensateGoodsGold(goodsOrder);
            return AjaxResult.success("已支付", goodsOrder);
        }
        if (!"0".equals(StringUtils.defaultIfBlank(goodsOrder.getPayStatus(), "0"))) {
            return AjaxResult.error("当前订单状态不可同步支付结果");
        }

        AppPayLog payLog = null;
        if (StringUtils.isNotEmpty(goodsOrder.getOrderNo())) {
            payLog = payLogService.selectAppPayLogByPayNo(goodsOrder.getOrderNo());
        }
        if (payLog == null) {
            AppPayLog query = new AppPayLog();
            query.setOrderId(orderId);
            query.setOrderType("2");
            List<AppPayLog> logs = payLogService.selectAppPayLogList(query);
            if (logs != null && !logs.isEmpty()) {
                // 取最新一条
                payLog = logs.get(0);
                for (AppPayLog item : logs) {
                    if (item.getLogId() != null && payLog.getLogId() != null
                            && item.getLogId() > payLog.getLogId()) {
                        payLog = item;
                    }
                }
            }
        }
        if (payLog == null || StringUtils.isEmpty(payLog.getPayNo())) {
            return AjaxResult.error("未找到支付记录，请稍后在订单列表查看");
        }

        try {
            JsapiService service = new JsapiService.Builder().config(wxPayConfigRuntime).build();
            QueryOrderByOutTradeNoRequest queryRequest = new QueryOrderByOutTradeNoRequest();
            queryRequest.setMchid(merchantId);
            queryRequest.setOutTradeNo(payLog.getPayNo());
            com.wechat.pay.java.service.payments.model.Transaction transaction = service.queryOrderByOutTradeNo(queryRequest);
            if (transaction == null || transaction.getTradeState() == null) {
                return AjaxResult.error("查询支付结果失败");
            }
            if (!com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.SUCCESS
                    .equals(transaction.getTradeState())) {
                return AjaxResult.error("微信侧尚未支付成功，状态：" + transaction.getTradeState().name());
            }

            BigDecimal payFen = transaction.getAmount() != null && transaction.getAmount().getTotal() != null
                    ? new BigDecimal(transaction.getAmount().getTotal())
                    : payLog.getPayMoney();
            if (payFen == null) {
                payFen = BigDecimal.ZERO;
            }
            payLog.setPayMoney(payFen);
            payLog.setUpdateTime(DateUtils.getNowDate());
            payLog.setNotifyContent(String.valueOf(transaction));
            payLog.setStatus("1");
            payLogService.updateAppPayLog(payLog);

            applyGoodsPaySuccess(goodsOrder, payLog, payFen, true);
            clearGoodsOrderCache(orderId);
            AppGoodsOrder latest = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
            return AjaxResult.success("支付已确认", latest);
        } catch (HttpException e) {
            log.error("主动查单HTTP失败 orderId={}", orderId, e);
            return AjaxResult.error("查询支付结果失败");
        } catch (MalformedMessageException e) {
            log.error("主动查单解析失败 orderId={}", orderId, e);
            return AjaxResult.error("查询支付结果失败");
        } catch (Exception e) {
            log.error("主动查单异常 orderId={}", orderId, e);
            return AjaxResult.error(StringUtils.defaultIfBlank(e.getMessage(), "同步支付结果失败"));
        }
    }

    /**
     * 商品订单支付成功落库
     * @param grantGold 是否赠币（通知回调可传 false，由外层统一 grantByPayLog；主动查单传 true）
     */
    private void applyGoodsPaySuccess(AppGoodsOrder goodsOrder, AppPayLog payLog, BigDecimal payFen, boolean grantGold) {
        if (goodsOrder == null) {
            return;
        }
        BigDecimal fen = payFen != null ? payFen : BigDecimal.ZERO;
        BigDecimal yuan = fen.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        boolean firstPay = !"1".equals(goodsOrder.getPayStatus());
        AppGoodsOrder up = new AppGoodsOrder();
        up.setOrderId(goodsOrder.getOrderId());
        up.setPayStatus("1");
        up.setStatus("1");
        if (firstPay && TravelOrderStatusPolicy.PENDING_CONFIRMATION.equals(goodsOrder.getTravelStatus())) {
            up.setTravelStatus(TravelOrderStatusPolicy.CONFIRMED);
        }
        // 实付以微信为准，同时回写应付，避免列表/退款仍用下单时的错误金额
        if (yuan.compareTo(BigDecimal.ZERO) > 0) {
            up.setPayMoney(yuan);
            up.setMoneyPayable(yuan);
            goodsOrder.setPayMoney(yuan);
            goodsOrder.setMoneyPayable(yuan);
        }
        if (firstPay) {
            up.setPayTime(new Date());
            goodsOrder.setPayTime(up.getPayTime());
        }
        up.setUpdateTime(DateUtils.getNowDate());
        appGoodsOrderMapper.updateAppGoodsOrder(up);
        goodsOrder.setPayStatus("1");
        goodsOrder.setStatus("1");
        if (up.getTravelStatus() != null) {
            goodsOrder.setTravelStatus(up.getTravelStatus());
        }
        syncOrderDetailGoodsMoney(goodsOrder.getOrderId(), yuan);

        if (!grantGold || payLog == null) {
            return;
        }
        goldService.grantOnPay(payLog.getUserId(), AppGoldBizType.GOODS_PAY, goodsOrder.getOrderId(),
                fen.compareTo(BigDecimal.ZERO) > 0 ? fen : payLog.getPayMoney(), payLog.getPayNo());
    }

    private void syncOrderDetailGoodsMoney(Long orderId, BigDecimal yuan) {
        if (orderId == null || yuan == null || yuan.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            // 多商品明细保存各自小计与优惠，不能把整单实付覆盖到每一行。
            if (appGoodsOrderMapper.countRetailOrder(orderId) > 0) return;
            AppGoodsOrderDetail where = new AppGoodsOrderDetail();
            where.setOrderId(orderId);
            List<AppGoodsOrderDetail> details = orderDetailMapper.selectAppGoodsOrderDetailList(where);
            if (details == null || details.isEmpty()) {
                return;
            }
            for (AppGoodsOrderDetail detail : details) {
                if (detail == null || detail.getDetailId() == null) {
                    continue;
                }
                AppGoodsOrderDetail up = new AppGoodsOrderDetail();
                up.setDetailId(detail.getDetailId());
                up.setGoodsMoney(yuan);
                orderDetailMapper.updateAppGoodsOrderDetail(up);
            }
        } catch (Exception ex) {
            log.warn("回写订单明细金额失败 orderId={}", orderId, ex);
        }
    }

    private BigDecimal findSkuOptionPrice(List<AppGoodsSkuOption> optionList, String optionType) {
        if (optionList == null || StringUtils.isEmpty(optionType)) {
            return null;
        }
        for (AppGoodsSkuOption opt : optionList) {
            if (opt == null || StringUtils.isEmpty(opt.getOptionValue())) {
                continue;
            }
            if (optionType.equals(String.valueOf(opt.getOptionType()))) {
                try {
                    return new BigDecimal(opt.getOptionValue().trim());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /** 自选晚数最少晚数（与小程序 defaultCustomNights 对齐） */
    private static final int CUSTOM_NIGHT_MIN = 7;

    private boolean isSkuEnabled(AppGoodsSku sku) {
        if (sku == null) {
            return false;
        }
        String status = StringUtils.trimToEmpty(sku.getStatus());
        // 未设置状态的历史数据视为可用；显式停用才拦截
        if (StringUtils.isEmpty(status)) {
            return true;
        }
        return !("0".equals(status) || "停用".equals(status));
    }

    private List<AppGoodsSkuOption> loadSkuOptions(Long skuId, Integer skuSeqNo) {
        if (skuId == null) {
            return new ArrayList<>();
        }
        AppGoodsSkuOption query = new AppGoodsSkuOption();
        query.setSkuId(skuId);
        if (skuSeqNo != null && skuSeqNo > 0) {
            query.setSkuSeqNo(skuSeqNo);
        }
        List<AppGoodsSkuOption> list = appGoodsSkuOptionMapper.selectAppGoodsSkuOptionList(query);
        return list != null ? list : new ArrayList<>();
    }

    /**
     * 解析套餐晚数：名称含周/半月/月优先；否则用公共属性 skuSeqNo=0 的 303。
     * 组合序号上的 303「数量」不参与晚数计算。
     */
    private Integer resolvePackageNights(List<AppGoodsSkuOption> optionList, String skuName) {
        String name = skuName == null ? "" : skuName;
        if (name.contains("半月")) {
            return 14;
        }
        if (name.contains("周")) {
            return 6;
        }
        if (name.contains("月")) {
            return 29;
        }
        if (optionList != null) {
            for (AppGoodsSkuOption opt : optionList) {
                if (opt == null || !"303".equals(String.valueOf(opt.getOptionType()))) {
                    continue;
                }
                Integer seq = opt.getSkuSeqNo();
                if (seq != null && seq > 0) {
                    continue;
                }
                if (StringUtils.isEmpty(opt.getOptionValue())) {
                    continue;
                }
                try {
                    int val = Integer.parseInt(opt.getOptionValue().trim());
                    if (val <= 0) {
                        continue;
                    }
                    String unit = StringUtils.trimToEmpty(opt.getOptionValueUnit());
                    if (unit.contains("晚") && !unit.contains("天")) {
                        return val;
                    }
                    // 单位为天、天/晚、或未填：按「天数」理解，晚数 = 天 - 1
                    return Math.max(val - 1, 0);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * 解析订单真实实付（元）：优先微信支付日志（分），再 payMoney / moneyPayable
     */
    @Override
    public BigDecimal resolveActualPayYuan(AppGoodsOrder order) {
        if (order == null) {
            return BigDecimal.ZERO;
        }
        AppPayLog payLog = null;
        if (StringUtils.isNotEmpty(order.getOrderNo())) {
            payLog = payLogService.selectAppPayLogByPayNo(order.getOrderNo());
        }
        if (payLog == null && order.getOrderId() != null) {
            AppPayLog query = new AppPayLog();
            query.setOrderId(order.getOrderId());
            query.setOrderType("2");
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
        if (payLog != null && "1".equals(payLog.getStatus())
                && payLog.getPayMoney() != null
                && payLog.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
            return payLog.getPayMoney().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        if (order.getPayMoney() != null && order.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
            return order.getPayMoney().setScale(2, RoundingMode.HALF_UP);
        }
        if (order.getMoneyPayable() != null && order.getMoneyPayable().compareTo(BigDecimal.ZERO) > 0) {
            return order.getMoneyPayable().setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 已支付订单：若微信实付与本地金额不一致，回写本地（修复展示/可退金额）
     */
    @Override
    public BigDecimal healPaidAmountIfNeeded(AppGoodsOrder order) {
        if (order == null || order.getOrderId() == null) {
            return BigDecimal.ZERO;
        }
        if (!"1".equals(StringUtils.defaultIfBlank(order.getPayStatus(), ""))
                && !"1".equals(StringUtils.defaultIfBlank(order.getStatus(), ""))) {
            return resolveActualPayYuan(order);
        }
        BigDecimal actual = resolveActualPayYuan(order);
        if (actual.compareTo(BigDecimal.ZERO) <= 0) {
            return actual;
        }
        BigDecimal localPay = order.getPayMoney() != null ? order.getPayMoney() : BigDecimal.ZERO;
        BigDecimal localPayable = order.getMoneyPayable() != null ? order.getMoneyPayable() : BigDecimal.ZERO;
        if (actual.compareTo(localPay) == 0 && actual.compareTo(localPayable) == 0) {
            return actual;
        }
        AppGoodsOrder up = new AppGoodsOrder();
        up.setOrderId(order.getOrderId());
        up.setPayMoney(actual);
        up.setMoneyPayable(actual);
        up.setUpdateTime(DateUtils.getNowDate());
        appGoodsOrderMapper.updateAppGoodsOrder(up);
        syncOrderDetailGoodsMoney(order.getOrderId(), actual);
        order.setPayMoney(actual);
        order.setMoneyPayable(actual);
        log.info("已支付订单金额已按微信实付回写 orderId={}, actual={}", order.getOrderId(), actual);
        return actual;
    }

    private void tryCompensateGoodsGold(AppGoodsOrder goodsOrder) {
        if (goodsOrder == null || goodsOrder.getOrderId() == null) {
            return;
        }
        try {
            AppPayLog payLog = null;
            if (StringUtils.isNotEmpty(goodsOrder.getOrderNo())) {
                payLog = payLogService.selectAppPayLogByPayNo(goodsOrder.getOrderNo());
            }
            if (payLog == null) {
                return;
            }
            BigDecimal fen = payLog.getPayMoney();
            if (fen == null && goodsOrder.getPayMoney() != null) {
                fen = goodsOrder.getPayMoney().multiply(new BigDecimal(100));
            }
            goldService.grantOnPay(goodsOrder.getUserId(), AppGoldBizType.GOODS_PAY,
                    goodsOrder.getOrderId(), fen, payLog.getPayNo());
        } catch (Exception ex) {
            log.warn("商品订单补赠币失败 orderId={}", goodsOrder.getOrderId(), ex);
        }
    }

    private void clearGoodsOrderCache(Long orderId) {
        if (orderId == null) {
            return;
        }
        try {
            SpringUtils.getBean(RedisCache.class).deleteObject(CacheConstants.APP_GOODS_ORDER + "id:" + orderId);
        } catch (Exception ignored) {
        }
    }

    @Override
    public String wxpayRefundNotify(HttpServletRequest request) {
        Map<String, String> returnMap = new HashMap<>(2);
        returnMap.put("code", "FAIL");
        returnMap.put("message", "失败");
        try {
            String signature = request.getHeader("Wechatpay-Signature");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String serial = request.getHeader("Wechatpay-Serial");
            String signType = request.getHeader("Wechatpay-Signature-Type");

            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serial)
                    .nonce(nonce)
                    .signature(signature)
                    .timestamp(timestamp)
                    .signType(signType)
                    .body(ServletUtils.getBody(request))
                    .build();

            NotificationParser parser = new NotificationParser(wxPayNotificationConfig);
            RefundNotification refundNotification = parser.parse(requestParam, RefundNotification.class);
            log.info("退款回调验签成功 outRefundNo={}", refundNotification.getOutRefundNo());

            AppPayRefundLog appPayRefundLog = null;
            if (StringUtils.isNotEmpty(refundNotification.getRefundId())) {
                appPayRefundLog = appPayRefundLogMapper.selectAppPayRefundLogByPayno(refundNotification.getRefundId());
            }
            if (appPayRefundLog == null && StringUtils.isNotEmpty(refundNotification.getOutRefundNo())) {
                appPayRefundLog = appPayRefundLogMapper.selectAppPayRefundLogByAgentRefundNo(refundNotification.getOutRefundNo());
            }
            if (appPayRefundLog == null) {
                log.error("退款日志不存在 refundId={}, outRefundNo={}",
                        refundNotification.getRefundId(), refundNotification.getOutRefundNo());
                // 无法识别的回调停止重试，避免无限通知
                returnMap.put("code", "SUCCESS");
                returnMap.put("message", "成功");
                return JSONObject.toJSONString(returnMap);
            }
            Date now = DateUtils.getNowDate();
            // 退款日志已成功：仍要补齐订单/售后终态（避免此前 finalize 失败导致永久卡在退款中）
            if ("1".equals(appPayRefundLog.getStatus())) {
                String orderType = StringUtils.defaultIfBlank(appPayRefundLog.getOrderType(), "");
                String outTradeNo = refundNotification.getOutTradeNo();
                if ("3".equals(orderType) || (outTradeNo != null && outTradeNo.startsWith("30"))) {
                    activityOrderService.handleRefundSuccess(appPayRefundLog.getOrderId());
                } else if ("1".equals(orderType) || (outTradeNo != null && outTradeNo.startsWith("10"))) {
                    userCardService.handleRefundSuccess(appPayRefundLog.getOrderId());
                } else {
                    Long refundFen = refundNotification.getAmount() != null
                            ? refundNotification.getAmount().getRefund() : null;
                    finalizeGoodsRefundSuccess(appPayRefundLog, outTradeNo, refundFen, now);
                }
                returnMap.put("code", "SUCCESS");
                returnMap.put("message", "成功");
                return JSONObject.toJSONString(returnMap);
            }

            Status refundStatus = refundNotification.getRefundStatus();
            if (Status.SUCCESS.equals(refundStatus)) {
                AppPayRefundLog upRefundLog = new AppPayRefundLog();
                upRefundLog.setLogId(appPayRefundLog.getLogId());
                upRefundLog.setStatus("1");
                upRefundLog.setNotifyContent(String.valueOf(refundNotification));
                upRefundLog.setUpdateTime(now);
                if (StringUtils.isNotEmpty(refundNotification.getRefundId())) {
                    upRefundLog.setPayNo(refundNotification.getRefundId());
                }
                appPayRefundLogMapper.updateAppPayRefundLog(upRefundLog);

                String orderType = StringUtils.defaultIfBlank(appPayRefundLog.getOrderType(), "");
                String outTradeNo = refundNotification.getOutTradeNo();
                Long refundFen = refundNotification.getAmount() != null
                        ? refundNotification.getAmount().getRefund() : null;
                if ("3".equals(orderType) || (outTradeNo != null && outTradeNo.startsWith("30"))) {
                    activityOrderService.handleRefundSuccess(appPayRefundLog.getOrderId());
                } else if ("1".equals(orderType) || (outTradeNo != null && outTradeNo.startsWith("10"))) {
                    userCardService.handleRefundSuccess(appPayRefundLog.getOrderId());
                } else {
                    finalizeGoodsRefundSuccess(appPayRefundLog, outTradeNo, refundFen, now);
                }
            } else if (Status.CLOSED.equals(refundStatus) || Status.ABNORMAL.equals(refundStatus)) {
                AppPayRefundLog upRefundLog = new AppPayRefundLog();
                upRefundLog.setLogId(appPayRefundLog.getLogId());
                upRefundLog.setStatus(Status.CLOSED.equals(refundStatus) ? "2" : "3");
                upRefundLog.setNotifyContent(String.valueOf(refundNotification));
                upRefundLog.setUpdateTime(now);
                appPayRefundLogMapper.updateAppPayRefundLog(upRefundLog);

                AppGoodsOrderAfter after = resolveGoodsAfterForRefund(appPayRefundLog.getOrderId(), refundNotification.getOutTradeNo());
                if (after != null) {
                    AppGoodsOrderAfter upAfter = new AppGoodsOrderAfter();
                    upAfter.setAfterId(after.getAfterId());
                    upAfter.setStatus("5");
                    upAfter.setUpdateTime(now);
                    appGoodsOrderAfterMapper.updateAppGoodsOrderAfter(upAfter);
                }
                String orderType = StringUtils.defaultIfBlank(appPayRefundLog.getOrderType(), "");
                String outTradeNo = refundNotification.getOutTradeNo();
                boolean goodsRefund = "2".equals(orderType)
                        || (StringUtils.isEmpty(orderType) && outTradeNo != null && outTradeNo.startsWith("20"));
                if (goodsRefund && appPayRefundLog.getOrderId() != null) {
                    AppGoodsOrder upOrder = new AppGoodsOrder();
                    upOrder.setOrderId(appPayRefundLog.getOrderId());
                    upOrder.setStatus("1");
                    restoreTravelStatusAfterRefund(upOrder);
                    upOrder.setUpdateTime(now);
                    appGoodsOrderMapper.updateAppGoodsOrder(upOrder);
                }
            }
            // PROCESSING：保持退款中，仍回 SUCCESS 避免无意义重试风暴
            returnMap.put("code", "SUCCESS");
            returnMap.put("message", "成功");
            return JSONObject.toJSONString(returnMap);
        } catch (Exception ex) {
            log.error("退款回调处理失败", ex);
            return JSONObject.toJSONString(returnMap);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult syncRefundResult(Long orderId, Long userId)
    {
        if (orderId == null || userId == null) {
            return AjaxResult.error("参数无效");
        }
        AppGoodsOrder order = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
        if (order == null) {
            return AjaxResult.error("订单不存在");
        }
        if (order.getUserId() == null || order.getUserId().longValue() != userId.longValue()) {
            return AjaxResult.error("非法订单");
        }
        if ("4".equals(order.getStatus())) {
            return AjaxResult.success("已退款", order);
        }
        if (!"3".equals(StringUtils.defaultIfBlank(order.getStatus(), ""))) {
            return AjaxResult.error("当前订单非退款中状态");
        }

        AppPayRefundLog refundLog = resolveLatestRefundLog(orderId, "2");
        if (refundLog == null) {
            return AjaxResult.error("未找到退款记录");
        }
        Date now = DateUtils.getNowDate();
        // 本地退款日志已成功但订单未落终态：直接补齐
        if ("1".equals(refundLog.getStatus())) {
            finalizeGoodsRefundSuccess(refundLog, order.getOrderNo(),
                    refundLog.getRefundMoney() != null ? refundLog.getRefundMoney().longValue() : null, now);
            clearGoodsOrderCache(orderId);
            return AjaxResult.success("退款已确认", appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId));
        }
        if (StringUtils.isEmpty(refundLog.getAgentRefundNo())) {
            return AjaxResult.error("退款单号缺失");
        }
        try {
            RefundService service = new RefundService.Builder().config(wxPayConfigRuntime).build();
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
            upRefundLog.setUpdateTime(now);
            if (StringUtils.isNotEmpty(refund.getRefundId())) {
                upRefundLog.setPayNo(refund.getRefundId());
            }
            appPayRefundLogMapper.updateAppPayRefundLog(upRefundLog);

            Long refundFen = refund.getAmount() != null ? refund.getAmount().getRefund() : null;
            finalizeGoodsRefundSuccess(refundLog, refund.getOutTradeNo(), refundFen, now);
            clearGoodsOrderCache(orderId);
            return AjaxResult.success("退款已确认", appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId));
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("主动查退款失败 orderId={}", orderId, e);
            return AjaxResult.error("同步退款结果失败");
        }
    }

    /**
     * 商品/教育售后退款成功：更新售后与订单，释放教育名额，尝试扣回赠送金币
     */
    private void finalizeGoodsRefundSuccess(AppPayRefundLog refundLog, String outTradeNo, Long refundAmountFen, Date now) {
        Long orderId = refundLog != null ? refundLog.getOrderId() : null;
        AppGoodsOrderAfter after = resolveGoodsAfterForRefund(orderId, outTradeNo);
        if (after != null && !"6".equals(after.getStatus())) {
            AppGoodsOrderAfter upAfter = new AppGoodsOrderAfter();
            upAfter.setAfterId(after.getAfterId());
            if (refundAmountFen != null) {
                upAfter.setRefundMoney(new BigDecimal(refundAmountFen).divide(new BigDecimal(100)));
            }
            upAfter.setStatus("6");
            upAfter.setUpdateTime(now);
            appGoodsOrderAfterMapper.updateAppGoodsOrderAfter(upAfter);
        }

        if (orderId == null && after != null) {
            orderId = after.getOrderId();
        }
        if (orderId == null) {
            return;
        }
        AppGoodsOrder order = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
        if (order == null) {
            return;
        }
        boolean travelStatusMissing = order.getTravelStatus() != null
                && !TravelOrderStatusPolicy.REFUNDED.equals(order.getTravelStatus());
        if (!"4".equals(order.getStatus()) || travelStatusMissing) {
            AppGoodsOrder upOrder = new AppGoodsOrder();
            upOrder.setOrderId(orderId);
            upOrder.setStatus("4");
            upOrder.setPayStatus("4");
            if (order.getTravelStatus() != null) {
                upOrder.setTravelStatus(TravelOrderStatusPolicy.REFUNDED);
            }
            upOrder.setUpdateTime(now);
            appGoodsOrderMapper.updateAppGoodsOrder(upOrder);
            releaseEducationStockIfNeeded(order);
        }
        // 无论订单是否已是退款态，均尝试扣币（幂等）
        String refundNo = refundLog != null ? refundLog.getAgentRefundNo() : null;
        Long uid = refundLog != null && refundLog.getUserId() != null
                ? refundLog.getUserId() : order.getUserId();
        BigDecimal refundFen = refundLog != null ? refundLog.getRefundMoney() : null;
        if (refundFen == null && refundAmountFen != null) {
            refundFen = new BigDecimal(refundAmountFen);
        }
        goldService.reverseOnRefund(uid, AppGoldBizType.GOODS_REFUND, orderId, refundFen, refundNo);
        clearGoodsOrderCache(orderId);
    }

    private void restoreTravelStatusAfterRefund(AppGoodsOrder update)
    {
        AppGoodsOrder current = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(update.getOrderId());
        if (current == null || current.getTravelStatus() == null) return;
        String previous = StringUtils.defaultIfBlank(current.getTravelStatusBeforeRefund(),
                TravelOrderStatusPolicy.CONFIRMED);
        update.setTravelStatus(previous);
    }

    /**
     * 按订单定位售后单，避免同一 out_order_no 多条记录导致 TooManyResultsException
     */
    private AppGoodsOrderAfter resolveGoodsAfterForRefund(Long orderId, String outTradeNo)
    {
        if (orderId != null) {
            AppGoodsOrderAfter query = new AppGoodsOrderAfter();
            query.setOrderId(orderId);
            List<AppGoodsOrderAfter> list = appGoodsOrderAfterMapper.selectAppGoodsOrderAfterList(query);
            if (list != null && !list.isEmpty()) {
                AppGoodsOrderAfter preferred = null;
                for (AppGoodsOrderAfter item : list) {
                    if (item == null || item.getAfterId() == null) {
                        continue;
                    }
                    String st = StringUtils.defaultIfBlank(item.getStatus(), "");
                    if ("1".equals(st) || "6".equals(st) || "0".equals(st)) {
                        if (preferred == null || item.getAfterId() > preferred.getAfterId()) {
                            preferred = item;
                        }
                    }
                }
                if (preferred != null) {
                    return preferred;
                }
                return list.stream()
                        .filter(i -> i != null && i.getAfterId() != null)
                        .max((a, b) -> Long.compare(a.getAfterId(), b.getAfterId()))
                        .orElse(null);
            }
        }
        if (StringUtils.isNotEmpty(outTradeNo)) {
            try {
                return appGoodsOrderAfterMapper.selectAppGoodsOrderAfterByOutorderno(outTradeNo);
            } catch (Exception ex) {
                log.warn("按 out_order_no 查询售后失败 outTradeNo={}", outTradeNo, ex);
            }
        }
        return null;
    }

    private AppPayRefundLog resolveLatestRefundLog(Long orderId, String orderType)
    {
        if (orderId == null) {
            return null;
        }
        AppPayRefundLog query = new AppPayRefundLog();
        query.setOrderId(orderId);
        if (StringUtils.isNotEmpty(orderType)) {
            query.setOrderType(orderType);
        }
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

    @Override
    public AppGoodsOrder getCacheGoodsOrder(Long orderId) {
        AppGoodsOrder appGoodsOrder = SpringUtils.getBean(RedisCache.class).getCacheObject(CacheConstants.APP_GOODS_ORDER + "id:"+ orderId);
        if(null==appGoodsOrder){
            appGoodsOrder = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(orderId);
            if(null!=appGoodsOrder){
                SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.APP_GOODS_ORDER + "id:"+ orderId, appGoodsOrder);
            }
        }
        return appGoodsOrder;
    }

    @Override
    public Map selAppGoodsOrderStatData() {
        return appGoodsOrderMapper.selAppGoodsOrderStatData();
    }

    /**
     * 按会员卡配置的有效期（天）计算截止时间；无法解析时默认 365 天
     */
    private Date resolveUserCardEndTime(Long cardId, Date startTime) {
        int days = 365;
        if (cardId != null && cardService != null) {
            AppCard card = cardService.selectAppCardByCardId(cardId);
            if (card != null && StringUtils.isNotEmpty(card.getExpiration())) {
                try {
                    int parsed = Integer.parseInt(card.getExpiration().trim());
                    if (parsed > 0) {
                        days = parsed;
                    }
                } catch (NumberFormatException ex) {
                    log.warn("会员卡有效期解析失败 cardId={}, expiration={}", cardId, card.getExpiration());
                }
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime != null ? startTime : new Date());
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    @Override
    public void releaseEducationStockIfNeeded(AppGoodsOrder order) {
        if (order == null || order.getGoodsId() == null) {
            return;
        }
        try {
            AppGoods goods = appGoodsMapper.selectAppGoodsByGoodsId(order.getGoodsId());
            if (goods == null || !("education".equals(goods.getGoodsType())
                    || "online".equals(goods.getGoodsType()))) {
                return;
            }
            if (appGoodsOrderMapper.countRetailOrder(order.getOrderId()) > 0) {
                AppGoodsOrderDetail filter = new AppGoodsOrderDetail(); filter.setOrderId(order.getOrderId());
                for (AppGoodsOrderDetail detail : orderDetailMapper.selectAppGoodsOrderDetailList(filter)) {
                    if (appGoodsMapper.releaseStock(detail.getGoodsId(),detail.getGoodsCount()) != 1) throw new ServiceException("库存释放失败");
                }
            } else {
                long count = order.getGoodsCount() != null ? order.getGoodsCount() : 1L;
                appGoodsMapper.releaseStock(order.getGoodsId(), count);
            }
        } catch (Exception ex) {
            log.error("释放预占库存失败 orderId={}, goodsId={}", order.getOrderId(), order.getGoodsId(), ex);
            throw new ServiceException("释放库存失败，请重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredUnpaidOrders(int expireMinutes) {
        int minutes = expireMinutes > 0 ? expireMinutes : (int) WX_PAY_EXPIRE_MINUTES;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -minutes);
        Date expireBefore = calendar.getTime();

        AppGoodsOrder query = new AppGoodsOrder();
        query.setPayStatus("0");
        query.getParams().put("expireBefore", expireBefore);
        List<AppGoodsOrder> list = appGoodsOrderMapper.selectAppGoodsOrderList(query);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int closed = 0;
        Date now = DateUtils.getNowDate();
        for (AppGoodsOrder order : list) {
            if (order == null || order.getOrderId() == null) {
                continue;
            }
            AppGoodsOrder latest = appGoodsOrderMapper.selectAppGoodsOrderByOrderIdForUpdate(order.getOrderId());
            if (latest == null || !"0".equals(StringUtils.defaultIfBlank(latest.getPayStatus(), ""))) {
                continue;
            }
            closePayLogByPayNo(latest.getOrderNo(), now);
            AppGoodsOrder up = new AppGoodsOrder();
            up.setOrderId(latest.getOrderId());
            up.setStatus("2");
            up.setPayStatus("2");
            if (latest.getTravelStatus() != null) {
                up.setTravelStatus(TravelOrderStatusPolicy.CANCELLED);
            }
            up.setUpdateTime(now);
            if (appGoodsOrderMapper.updateAppGoodsOrder(up) > 0) {
                releaseEducationStockIfNeeded(latest);
                releaseCouponIfNeeded(latest);
                closed++;
            }
        }
        if (closed > 0) {
            log.info("超时关闭未支付商品订单 {} 笔，阈值={}分钟", closed, minutes);
        }
        return closed;
    }

    @Override
    public void releaseCouponIfNeeded(AppGoodsOrder order) {
        if (order == null || order.getOrderId() == null
                || !StringUtils.defaultString(order.getCouponGotIds()).matches("\\d+")) {
            return;
        }
        AppGoodsCouponGot got = couponGotMapper.selectAppGoodsCouponGotByGotId(
                Long.valueOf(order.getCouponGotIds()));
        if (got != null && couponGotMapper.releaseByOrderId(order.getOrderId()) > 0) {
            couponMapper.decrementUsedCount(got.getCouponId());
        }
    }

    private void closePayLogByPayNo(String payNo, Date now) {
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
}
