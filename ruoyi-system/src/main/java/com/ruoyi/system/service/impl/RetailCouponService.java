package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.RetailCheckout.*;
import com.ruoyi.system.mapper.AppGoodsCouponGotMapper;
import com.ruoyi.system.mapper.AppGoodsCouponMapper;

@Service
public class RetailCouponService {
    @Autowired private AppGoodsCouponGotMapper gotMapper;
    @Autowired private AppGoodsCouponMapper couponMapper;

    public void price(Long userId, Long chosenId, Quote quote) {
        AppGoodsCouponGot query = new AppGoodsCouponGot();
        query.setUserId(userId); query.setIsUsed(0); query.setStatus("1");
        AppGoodsCouponGot selected = null;
        AppGoodsCoupon selectedCoupon = null;
        BigDecimal best = BigDecimal.ZERO;
        List<AppGoodsCouponGot> available = gotMapper.selectAppGoodsCouponGotList(query);
        available.sort(java.util.Comparator.comparing(AppGoodsCouponGot::getGotId));
        for (AppGoodsCouponGot got : available) {
            AppGoodsCoupon coupon = couponMapper.selectAppGoodsCouponByCouponId(got.getCouponId());
            BigDecimal discount = discount(coupon, quote);
            if (discount.signum() <= 0) continue;
            Coupon option = new Coupon();
            option.setGotId(got.getGotId()); option.setName(coupon.getCouponName()); option.setDiscount(discount);
            quote.getCoupons().add(option);
            boolean automatic = chosenId == null && got.getChannelCode() != null && !got.getChannelCode().isEmpty();
            if ((automatic && discount.compareTo(best) > 0) || got.getGotId().equals(chosenId)) {
                selected = got; selectedCoupon = coupon; best = discount;
            }
        }
        if (chosenId != null && chosenId > 0 && selected == null) throw new ServiceException("所选优惠券不可用");
        if (selected != null) {
            quote.setCouponGotId(selected.getGotId()); quote.setChannelCode(selected.getChannelCode());
            RetailPricing.allocate(quote, best, eligible(selectedCoupon, quote));
        }
    }
    private List<Line> eligible(AppGoodsCoupon coupon, Quote quote) {
        return quote.getItems().stream().filter(line ->
                (coupon.getGoodsId() == null || coupon.getGoodsId() == 0 || coupon.getGoodsId().equals(line.getGoodsId()))
                && (coupon.getCategoryId() == null || coupon.getCategoryId() == 0 || coupon.getCategoryId().equals(line.getCategoryId())))
                .collect(Collectors.toList());
    }
    private BigDecimal discount(AppGoodsCoupon coupon, Quote quote) {
        Date now = new Date();
        if (coupon == null || !"1".equals(coupon.getStatus())
                || coupon.getEnableStartTime() != null && now.before(coupon.getEnableStartTime())
                || coupon.getEnableEndTime() != null && now.after(coupon.getEnableEndTime())) return BigDecimal.ZERO;
        BigDecimal basis = eligible(coupon, quote).stream().map(Line::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (basis.signum() <= 0 || coupon.getMinPrice() != null && basis.compareTo(coupon.getMinPrice()) < 0) return BigDecimal.ZERO;
        BigDecimal amount = "2".equals(coupon.getDiscountType())
                ? AppGoodsOrderServiceImpl.calculatePercentageDiscount(basis, coupon.getDiscountPrice()) : coupon.getDiscountPrice();
        return amount == null || amount.signum() <= 0 ? BigDecimal.ZERO : amount.min(basis).setScale(2, java.math.RoundingMode.HALF_UP);
    }
    public void consume(Quote quote, AppGoodsOrder order) {
        if (quote.getCouponGotId() == null) return;
        AppGoodsCouponGot got = gotMapper.selectForUpdate(quote.getCouponGotId());
        if (got == null || !order.getUserId().equals(got.getUserId()) || !Integer.valueOf(0).equals(got.getIsUsed())
                || gotMapper.markUsed(got.getGotId(), order.getOrderId(), quote.getMoneyDiscount()) != 1) {
            throw new ServiceException("优惠券已被使用，请重新确认");
        }
        couponMapper.incrementUsedCount(got.getCouponId());
    }
}
