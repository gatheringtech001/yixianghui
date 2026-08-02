package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsCoupon;

/**
 * 商品优惠券Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsCouponService 
{
    /**
     * 查询商品优惠券
     * 
     * @param couponId 商品优惠券主键
     * @return 商品优惠券
     */
    public AppGoodsCoupon selectAppGoodsCouponByCouponId(Long couponId);

    /**
     * 查询商品优惠券列表
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 商品优惠券集合
     */
    public List<AppGoodsCoupon> selectAppGoodsCouponList(AppGoodsCoupon appGoodsCoupon);

    /**
     * 新增商品优惠券
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 结果
     */
    public int insertAppGoodsCoupon(AppGoodsCoupon appGoodsCoupon);

    /**
     * 修改商品优惠券
     * 
     * @param appGoodsCoupon 商品优惠券
     * @return 结果
     */
    public int updateAppGoodsCoupon(AppGoodsCoupon appGoodsCoupon);

    /**
     * 批量删除商品优惠券
     * 
     * @param couponIds 需要删除的商品优惠券主键集合
     * @return 结果
     */
    public int deleteAppGoodsCouponByCouponIds(Long[] couponIds);

    /**
     * 删除商品优惠券信息
     * 
     * @param couponId 商品优惠券主键
     * @return 结果
     */
    public int deleteAppGoodsCouponByCouponId(Long couponId);
}
