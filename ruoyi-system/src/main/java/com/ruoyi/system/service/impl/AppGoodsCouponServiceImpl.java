package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCouponMapper;
import com.ruoyi.system.domain.AppGoodsCoupon;
import com.ruoyi.system.service.IAppGoodsCouponService;

/**
 * 商品优惠券Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCouponServiceImpl implements IAppGoodsCouponService 
{
    @Autowired
    private AppGoodsCouponMapper appGoodsCouponMapper;

    /**
     * 查询商品优惠券
     * 
     * @param couponId 商品优惠券主键
     * @return 商品优惠券
     */
    @Override
    public AppGoodsCoupon selectAppGoodsCouponByCouponId(Long couponId)
    {
        return appGoodsCouponMapper.selectAppGoodsCouponByCouponId(couponId);
    }

    /**
     * 查询商品优惠券列表
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 商品优惠券
     */
    @Override
    public List<AppGoodsCoupon> selectAppGoodsCouponList(AppGoodsCoupon appGoodsCoupon)
    {
        return appGoodsCouponMapper.selectAppGoodsCouponList(appGoodsCoupon);
    }

    /**
     * 新增商品优惠券
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 结果
     */
    @Override
    public int insertAppGoodsCoupon(AppGoodsCoupon appGoodsCoupon)
    {
        appGoodsCoupon.setCreateTime(DateUtils.getNowDate());
        return appGoodsCouponMapper.insertAppGoodsCoupon(appGoodsCoupon);
    }

    /**
     * 修改商品优惠券
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 结果
     */
    @Override
    public int updateAppGoodsCoupon(AppGoodsCoupon appGoodsCoupon)
    {
        appGoodsCoupon.setUpdateTime(DateUtils.getNowDate());
        return appGoodsCouponMapper.updateAppGoodsCoupon(appGoodsCoupon);
    }

    /**
     * 批量删除商品优惠券
     * 
     * @param couponIds 需要删除的商品优惠券主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponByCouponIds(Long[] couponIds)
    {
        return appGoodsCouponMapper.deleteAppGoodsCouponByCouponIds(couponIds);
    }

    /**
     * 删除商品优惠券信息
     * 
     * @param couponId 商品优惠券主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponByCouponId(Long couponId)
    {
        return appGoodsCouponMapper.deleteAppGoodsCouponByCouponId(couponId);
    }
}
