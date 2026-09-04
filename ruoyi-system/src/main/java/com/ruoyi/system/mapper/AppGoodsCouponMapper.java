package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsCoupon;

/**
 * 商品优惠券Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsCouponMapper 
{
    /**
     * 查询商品优惠券
     * 
     * @param couponId 商品优惠券主键
     * @return 商品优惠券
     */
    public AppGoodsCoupon selectAppGoodsCouponByCouponId(Long couponId);

    public AppGoodsCoupon selectByChannelCodeForUpdate(String channelCode);

    public int incrementGotCount(Long couponId);

    public int incrementUsedCount(Long couponId);

    public int decrementUsedCount(Long couponId);

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
     * 删除商品优惠券
     * 
     * @param couponId 商品优惠券主键
     * @return 结果
     */
    public int deleteAppGoodsCouponByCouponId(Long couponId);

    /**
     * 批量删除商品优惠券
     * 
     * @param couponIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsCouponByCouponIds(Long[] couponIds);
}
