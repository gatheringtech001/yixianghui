package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsCouponGot;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券领取记录Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsCouponGotMapper 
{
    /**
     * 查询优惠券领取记录
     * 
     * @param gotId 优惠券领取记录主键
     * @return 优惠券领取记录
     */
    public AppGoodsCouponGot selectAppGoodsCouponGotByGotId(Long gotId);

    public AppGoodsCouponGot selectForUpdate(Long gotId);

    public AppGoodsCouponGot selectBestChannelCoupon(@Param("userId") Long userId,
                                                      @Param("goodsId") Long goodsId,
                                                      @Param("categoryId") Long categoryId,
                                                      @Param("payable") java.math.BigDecimal payable);

    public AppGoodsCouponGot selectByUserAndCoupon(@Param("userId") Long userId, @Param("couponId") Long couponId);

    public int markUsed(@Param("gotId") Long gotId, @Param("orderId") Long orderId,
                        @Param("discountPrice") java.math.BigDecimal discountPrice);

    public int releaseByOrderId(Long orderId);

    /**
     * 查询优惠券领取记录列表
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 优惠券领取记录集合
     */
    public List<AppGoodsCouponGot> selectAppGoodsCouponGotList(AppGoodsCouponGot appGoodsCouponGot);

    /**
     * 新增优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    public int insertAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot);

    /**
     * 修改优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    public int updateAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot);

    /**
     * 删除优惠券领取记录
     * 
     * @param gotId 优惠券领取记录主键
     * @return 结果
     */
    public int deleteAppGoodsCouponGotByGotId(Long gotId);

    /**
     * 批量删除优惠券领取记录
     * 
     * @param gotIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsCouponGotByGotIds(Long[] gotIds);
}
