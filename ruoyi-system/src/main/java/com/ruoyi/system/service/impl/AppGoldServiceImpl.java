package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AppGoldBizType;
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.domain.AppUserGoldLog;
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.mapper.AppUserGoldLogMapper;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.IAppUserInfoService;
import com.ruoyi.system.service.ISysConfigService;
import lombok.extern.slf4j.Slf4j;

/**
 * Unified gold coin service implementation.
 */
@Service
@Slf4j
public class AppGoldServiceImpl implements IAppGoldService
{
    private static final String CFG_PAY_ENABLED = "gold.pay.enabled";
    private static final String CFG_PAY_RATE = "gold.pay.rate";
    private static final String CFG_REFUND_REVERSE = "gold.refund.reverse.enabled";
    private static final String CFG_SCOPE_GOODS = "gold.scope.goods";
    private static final String CFG_SCOPE_ACTIVITY = "gold.scope.activity";
    private static final String CFG_SCOPE_CARD = "gold.scope.card";

    @Autowired
    private AppUserGoldLogMapper appUserGoldLogMapper;

    @Autowired
    private IAppUserInfoService userInfoService;

    @Autowired
    private ISysConfigService configService;

    @Override
    public long calcGoldByFen(BigDecimal fen)
    {
        if (fen == null || fen.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }
        long yuan = fen.divide(new BigDecimal(100), 0, RoundingMode.DOWN).longValue();
        if (yuan <= 0) {
            return 0L;
        }
        long rate = resolveRate();
        return yuan * rate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long grantInviteRegister(Long inviterUserId, Long newUserId, long gold)
    {
        if (inviterUserId == null || newUserId == null || inviterUserId.equals(newUserId) || gold <= 0) {
            return 0L;
        }
        // 按被邀请人幂等：同一新用户只给邀请人发一次
        if (hasInviteGrantLog(inviterUserId, newUserId)) {
            return 0L;
        }
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(inviterUserId);
        if (userInfo == null) {
            userInfo = userInfoService.initUserInfo(inviterUserId);
        }
        if (userInfo == null) {
            log.warn("invite gold skipped, inviter missing userId={}", inviterUserId);
            return 0L;
        }
        long current = userInfo.getGolden() == null ? 0L : userInfo.getGolden();
        AppUserGoldLog goldLog = new AppUserGoldLog();
        goldLog.setUserId(inviterUserId);
        goldLog.setGold(gold);
        goldLog.setBalance(current + gold);
        goldLog.setTradeType(1);
        goldLog.setTradeTitle(AppGoldBizType.TITLE_INVITE);
        goldLog.setTradeDetail("邀请新用户注册奖励金币，数量：" + gold + "，新用户ID：" + newUserId);
        goldLog.setTradeData(DateUtils.parseDateToStr("yyyyMMdd", DateUtils.getNowDate()));
        goldLog.setBusinessType(AppGoldBizType.INVITE_REGISTER);
        goldLog.setBusinessId(newUserId);
        goldLog.setCreateTime(DateUtils.getNowDate());
        goldLog.setStatus("1");
        appUserGoldLogMapper.insertAppUserGoldLog(goldLog);

        AppUserInfo up = new AppUserInfo();
        up.setUserId(inviterUserId);
        up.setGolden(current + gold);
        userInfoService.updateAppUserInfo(up);
        log.info("invite gold ok inviterUserId={}, newUserId={}, gold={}", inviterUserId, newUserId, gold);
        return gold;
    }

    private boolean hasInviteGrantLog(Long inviterUserId, Long newUserId)
    {
        AppUserGoldLog query = new AppUserGoldLog();
        query.setUserId(inviterUserId);
        query.setBusinessType(AppGoldBizType.INVITE_REGISTER);
        query.setBusinessId(newUserId);
        query.setTradeType(1);
        List<AppUserGoldLog> list = appUserGoldLogMapper.selectAppUserGoldLogList(query);
        return list != null && !list.isEmpty();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long grantOnPay(Long userId, String businessType, Long businessId, BigDecimal payFen, String refNo)
    {
        if (!isTrue(CFG_PAY_ENABLED, true)) {
            return 0L;
        }
        if (!isBizScopeEnabled(businessType)) {
            return 0L;
        }
        if (userId == null || StringUtils.isEmpty(businessType) || businessId == null) {
            return 0L;
        }
        long gold = calcGoldByFen(payFen);
        if (gold <= 0) {
            return 0L;
        }
        if (hasGrantLog(userId, businessType, businessId, refNo)) {
            return 0L;
        }
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userId);
        if (userInfo == null) {
            log.warn("grant gold skipped, user missing userId={}", userId);
            return 0L;
        }
        long current = userInfo.getGolden() == null ? 0L : userInfo.getGolden();
        AppUserGoldLog goldLog = new AppUserGoldLog();
        goldLog.setUserId(userId);
        goldLog.setGold(gold);
        goldLog.setBalance(current + gold);
        goldLog.setTradeType(1);
        goldLog.setTradeTitle(AppGoldBizType.TITLE_PAY);
        goldLog.setTradeDetail("\u652f\u4ed8\u83b7\u53d6\u91d1\u5e01\uff0c\u6570\u91cf\uff1a" + gold);
        goldLog.setTradeData(StringUtils.isNotEmpty(refNo) ? refNo : DateUtils.dateTime());
        goldLog.setBusinessType(businessType);
        goldLog.setBusinessId(businessId);
        goldLog.setCreateTime(DateUtils.getNowDate());
        goldLog.setStatus("1");
        appUserGoldLogMapper.insertAppUserGoldLog(goldLog);

        AppUserInfo up = new AppUserInfo();
        up.setUserId(userId);
        up.setGolden(current + gold);
        userInfoService.updateAppUserInfo(up);
        log.info("grant gold ok userId={}, bizType={}, bizId={}, gold={}, refNo={}",
                userId, businessType, businessId, gold, refNo);
        return gold;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long reverseOnRefund(Long userId, String businessType, Long businessId, BigDecimal refundFen, String refNo)
    {
        if (!isTrue(CFG_REFUND_REVERSE, true)) {
            return 0L;
        }
        if (!isBizScopeEnabled(businessType)) {
            return 0L;
        }
        if (userId == null || StringUtils.isEmpty(businessType) || businessId == null) {
            return 0L;
        }
        long gold = calcGoldByFen(refundFen);
        if (gold <= 0) {
            return 0L;
        }
        if (hasReverseLog(userId, businessType, businessId, refNo)) {
            return 0L;
        }
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userId);
        if (userInfo == null) {
            return 0L;
        }
        long current = userInfo.getGolden() == null ? 0L : userInfo.getGolden();
        long deduct = Math.min(current, gold);
        if (deduct <= 0) {
            return 0L;
        }
        AppUserGoldLog goldLog = new AppUserGoldLog();
        goldLog.setUserId(userId);
        goldLog.setGold(deduct);
        goldLog.setBalance(current - deduct);
        goldLog.setTradeType(2);
        goldLog.setTradeTitle(AppGoldBizType.TITLE_REFUND);
        String detail = "\u9000\u6b3e\u6263\u56de\u91d1\u5e01\uff0c\u6570\u91cf\uff1a" + deduct;
        if (deduct < gold) {
            detail = detail + "\uff08\u5e94\u6263" + gold + "\uff0c\u4f59\u989d\u4e0d\u8db3\uff09";
        }
        goldLog.setTradeDetail(detail);
        goldLog.setTradeData(StringUtils.isNotEmpty(refNo) ? refNo : ("RF" + businessType + businessId));
        goldLog.setBusinessType(businessType);
        goldLog.setBusinessId(businessId);
        goldLog.setCreateTime(DateUtils.getNowDate());
        goldLog.setStatus("1");
        appUserGoldLogMapper.insertAppUserGoldLog(goldLog);

        AppUserInfo up = new AppUserInfo();
        up.setUserId(userId);
        up.setGolden(current - deduct);
        userInfoService.updateAppUserInfo(up);
        log.info("reverse gold ok userId={}, bizType={}, bizId={}, deduct={}, refNo={}",
                userId, businessType, businessId, deduct, refNo);
        return deduct;
    }

    @Override
    public long grantByPayLog(AppPayLog payLog)
    {
        if (payLog == null || payLog.getUserId() == null || payLog.getOrderId() == null) {
            return 0L;
        }
        String payNo = payLog.getPayNo();
        String bizType = resolvePayBizType(payNo);
        if (bizType == null) {
            return 0L;
        }
        return grantOnPay(payLog.getUserId(), bizType, payLog.getOrderId(), payLog.getPayMoney(), payNo);
    }

    private String resolvePayBizType(String payNo)
    {
        if (StringUtils.isEmpty(payNo)) {
            return null;
        }
        if (payNo.startsWith("20")) {
            return AppGoldBizType.GOODS_PAY;
        }
        if (payNo.startsWith("30")) {
            return AppGoldBizType.ACTIVITY_PAY;
        }
        if (payNo.startsWith("10")) {
            return AppGoldBizType.CARD_PAY;
        }
        return null;
    }

    private boolean hasGrantLog(Long userId, String businessType, Long businessId, String refNo)
    {
        AppUserGoldLog query = new AppUserGoldLog();
        query.setUserId(userId);
        query.setBusinessType(businessType);
        query.setBusinessId(businessId);
        query.setTradeType(1);
        query.setStatus("1");
        List<AppUserGoldLog> list = appUserGoldLogMapper.selectAppUserGoldLogList(query);
        if (list == null || list.isEmpty()) {
            return false;
        }
        if (StringUtils.isEmpty(refNo)) {
            return true;
        }
        for (AppUserGoldLog item : list) {
            if (refNo.equals(item.getTradeData()) || AppGoldBizType.TITLE_PAY.equals(item.getTradeTitle())) {
                return true;
            }
        }
        return !list.isEmpty();
    }

    private boolean hasReverseLog(Long userId, String businessType, Long businessId, String refNo)
    {
        if (StringUtils.isNotEmpty(refNo)) {
            AppUserGoldLog byRef = new AppUserGoldLog();
            byRef.setUserId(userId);
            byRef.setTradeType(2);
            byRef.setTradeData(refNo);
            byRef.setStatus("1");
            List<AppUserGoldLog> refList = appUserGoldLogMapper.selectAppUserGoldLogList(byRef);
            if (refList != null && !refList.isEmpty()) {
                return true;
            }
        }
        if (StringUtils.isEmpty(refNo)) {
            AppUserGoldLog query = new AppUserGoldLog();
            query.setUserId(userId);
            query.setBusinessType(businessType);
            query.setBusinessId(businessId);
            query.setTradeType(2);
            query.setStatus("1");
            List<AppUserGoldLog> list = appUserGoldLogMapper.selectAppUserGoldLogList(query);
            return list != null && !list.isEmpty();
        }
        return false;
    }

    private boolean isBizScopeEnabled(String businessType)
    {
        if (AppGoldBizType.GOODS_PAY.equals(businessType) || AppGoldBizType.GOODS_REFUND.equals(businessType)) {
            return isTrue(CFG_SCOPE_GOODS, true);
        }
        if (AppGoldBizType.ACTIVITY_PAY.equals(businessType) || AppGoldBizType.ACTIVITY_REFUND.equals(businessType)) {
            return isTrue(CFG_SCOPE_ACTIVITY, true);
        }
        if (AppGoldBizType.CARD_PAY.equals(businessType) || AppGoldBizType.CARD_REFUND.equals(businessType)) {
            return isTrue(CFG_SCOPE_CARD, false);
        }
        return true;
    }

    private long resolveRate()
    {
        String val = configService.selectConfigByKey(CFG_PAY_RATE);
        if (StringUtils.isEmpty(val)) {
            return 1L;
        }
        try {
            long rate = Long.parseLong(val.trim());
            return rate > 0 ? rate : 1L;
        } catch (Exception e) {
            return 1L;
        }
    }

    private boolean isTrue(String key, boolean defaultVal)
    {
        String val = configService.selectConfigByKey(key);
        if (StringUtils.isEmpty(val)) {
            return defaultVal;
        }
        return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim()) || "Y".equalsIgnoreCase(val.trim());
    }
}