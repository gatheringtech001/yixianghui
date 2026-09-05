package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppGoodsSku;

/** 新订单仅复制购买意图；支付、履约、归因及主键由服务端生成。 */
final class NewGoodsOrderPolicy {
    private NewGoodsOrderPolicy() { }

    static AppGoodsOrder prepare(AppGoodsOrder input, AppGoods goods) {
        if (input == null || goods == null || !"1".equals(goods.getStatus())) {
            throw new ServiceException("商品无效或已下架");
        }
        if (input.getUserId() == null || input.getUserId() <= 0) {
            throw new ServiceException("请先登录");
        }
        if (input.getGoodsCount() == null || input.getGoodsCount() <= 0) {
            throw new ServiceException("商品数量无效");
        }
        AppGoodsOrder order = new AppGoodsOrder();
        order.setGoodsId(goods.getGoodsId());
        order.setUserId(input.getUserId());
        order.setDeptId(goods.getDeptId());
        order.setGoodsList(Collections.singletonList(goods));
        order.setGoodsCount(input.getGoodsCount());
        order.setAddressId(input.getAddressId() == null ? 0L : input.getAddressId());
        order.setContactName(input.getContactName());
        order.setContactPhone(input.getContactPhone());
        order.setRemark(input.getRemark());
        order.setCouponGotIds(input.getCouponGotIds());
        order.setStatus("0");
        order.setPayStatus("0");
        order.setMoneyExpress(BigDecimal.ZERO);
        if ("hotel".equals(goods.getGoodsType())) copyTravelSelection(input, order);
        return order;
    }

    private static void copyTravelSelection(AppGoodsOrder input, AppGoodsOrder order) {
        if (input.getCheckInDate() == null || input.getCheckOutDate() == null) {
            throw new ServiceException("请选择入住和退房日期");
        }
        ZoneId zone = ZoneId.systemDefault();
        long nights = ChronoUnit.DAYS.between(
                java.time.Instant.ofEpochMilli(input.getCheckInDate().getTime()).atZone(zone).toLocalDate(),
                java.time.Instant.ofEpochMilli(input.getCheckOutDate().getTime()).atZone(zone).toLocalDate());
        if (nights <= 0 || nights > Integer.MAX_VALUE
                || (input.getInterCount() != null && input.getInterCount() != nights)) {
            throw new ServiceException("入住日期与晚数不一致");
        }
        if (input.getSelfGoodsCount() != null && input.getSelfGoodsCount() < 0) {
            throw new ServiceException("供餐人数无效");
        }
        order.setCheckInDate(input.getCheckInDate());
        order.setCheckOutDate(input.getCheckOutDate());
        order.setInterCount((int) nights);
        order.setSkuId(input.getSkuId());
        order.setSkuSeqNo(input.getSkuSeqNo());
        order.setSelfSkuId(input.getSelfSkuId());
        order.setSelfGoodsCount(input.getSelfGoodsCount());
        order.setSelComboIndex(input.getSelComboIndex());
    }

    static void requireOwnedSku(AppGoods goods, AppGoodsSku sku) {
        if (sku == null || !goods.getGoodsId().equals(sku.getGoodsId())) {
            throw new ServiceException("所选规格不属于当前商品");
        }
        long now = System.currentTimeMillis();
        if ((sku.getValidTime() != null && sku.getValidTime().getTime() > now)
                || (sku.getInvalidTime() != null && sku.getInvalidTime().getTime() < now)) {
            throw new ServiceException("所选规格不在有效期内");
        }
    }
}
