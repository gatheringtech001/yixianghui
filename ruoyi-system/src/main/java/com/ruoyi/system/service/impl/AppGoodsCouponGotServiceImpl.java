package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AppGoodsCoupon;
import com.ruoyi.system.mapper.AppGoodsCouponMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.mapper.AppGoodsCouponGotMapper;
import com.ruoyi.system.domain.AppGoodsCouponGot;
import com.ruoyi.system.service.IAppGoodsCouponGotService;

/**
 * 优惠券领取记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCouponGotServiceImpl implements IAppGoodsCouponGotService 
{
    @Autowired
    private AppGoodsCouponGotMapper appGoodsCouponGotMapper;
    @Autowired
    private AppGoodsCouponMapper appGoodsCouponMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 查询优惠券领取记录
     * 
     * @param gotId 优惠券领取记录主键
     * @return 优惠券领取记录
     */
    @Override
    public AppGoodsCouponGot selectAppGoodsCouponGotByGotId(Long gotId)
    {
        return appGoodsCouponGotMapper.selectAppGoodsCouponGotByGotId(gotId);
    }

    /**
     * 查询优惠券领取记录列表
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 优惠券领取记录
     */
    @Override
    public List<AppGoodsCouponGot> selectAppGoodsCouponGotList(AppGoodsCouponGot appGoodsCouponGot)
    {
        return appGoodsCouponGotMapper.selectAppGoodsCouponGotList(appGoodsCouponGot);
    }

    /**
     * 新增优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    @Override
    public int insertAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot)
    {
        appGoodsCouponGot.setCreateTime(DateUtils.getNowDate());
        return appGoodsCouponGotMapper.insertAppGoodsCouponGot(appGoodsCouponGot);
    }

    /**
     * 修改优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    @Override
    public int updateAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot)
    {
        appGoodsCouponGot.setUpdateTime(DateUtils.getNowDate());
        return appGoodsCouponGotMapper.updateAppGoodsCouponGot(appGoodsCouponGot);
    }

    /**
     * 批量删除优惠券领取记录
     * 
     * @param gotIds 需要删除的优惠券领取记录主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponGotByGotIds(Long[] gotIds)
    {
        return appGoodsCouponGotMapper.deleteAppGoodsCouponGotByGotIds(gotIds);
    }

    /**
     * 删除优惠券领取记录信息
     * 
     * @param gotId 优惠券领取记录主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponGotByGotId(Long gotId)
    {
        return appGoodsCouponGotMapper.deleteAppGoodsCouponGotByGotId(gotId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getDistributionOffer(Long userId, String channelCode,
                                                     String sourceAppId, String scene) {
        AppGoodsCoupon coupon = requireActiveChannel(channelCode, sourceAppId);
        jdbcTemplate.update("insert into app_distribution_visit "
                        + "(channel_code,user_id,source_app_id,launch_scene,create_time) values (?,?,?,?,now())",
                channelCode, userId, sourceAppId, StringUtils.substring(scene, 0, 32));
        AppGoodsCouponGot got = appGoodsCouponGotMapper.selectByUserAndCoupon(userId, coupon.getCouponId());
        Map<String, Object> result = new HashMap<>();
        result.put("coupon", coupon);
        result.put("claimed", got != null);
        result.put("gotId", got == null ? null : got.getGotId());
        result.put("linkPath", "/pages/home/home?channelCode=" + channelCode);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppGoodsCouponGot claimDistributionCoupon(Long userId, String channelCode,
                                                      String sourceAppId) {
        AppGoodsCoupon coupon = requireActiveChannel(channelCode, sourceAppId);
        AppGoodsCouponGot existing = appGoodsCouponGotMapper.selectByUserAndCoupon(userId, coupon.getCouponId());
        if (existing != null) {
            return existing;
        }
        if (coupon.getCouponTotal() != null && coupon.getCouponGotCount() >= coupon.getCouponTotal()) {
            throw new ServiceException("优惠券已领完");
        }
        AppGoodsCouponGot got = new AppGoodsCouponGot();
        got.setCouponId(coupon.getCouponId());
        got.setUserId(userId);
        got.setOrderId(0L);
        got.setGetMethod("channel");
        got.setChannelCode(channelCode);
        got.setIsUsed(0);
        got.setStatus("1");
        got.setCreateTime(DateUtils.getNowDate());
        appGoodsCouponGotMapper.insertAppGoodsCouponGot(got);
        appGoodsCouponMapper.incrementGotCount(coupon.getCouponId());
        got.setCouponInfo(coupon);
        return got;
    }

    private AppGoodsCoupon requireActiveChannel(String channelCode, String sourceAppId) {
        String code = StringUtils.trimToEmpty(channelCode);
        if (!code.matches("[A-Za-z0-9_-]{2,64}")) {
            throw new ServiceException("渠道链接无效");
        }
        AppGoodsCoupon coupon = appGoodsCouponMapper.selectByChannelCodeForUpdate(code);
        java.util.Date now = DateUtils.getNowDate();
        if (coupon == null || !"1".equals(coupon.getStatus())
                || (coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime()))
                || (coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime()))) {
            throw new ServiceException("渠道优惠已失效");
        }
        if (StringUtils.isNotBlank(coupon.getSourceAppId())
                && !coupon.getSourceAppId().equals(StringUtils.trimToEmpty(sourceAppId))) {
            throw new ServiceException("渠道来源不匹配");
        }
        return coupon;
    }
}
