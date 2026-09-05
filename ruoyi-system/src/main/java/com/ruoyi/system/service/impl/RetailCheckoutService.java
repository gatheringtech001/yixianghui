package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.RetailCheckout.*;
import com.ruoyi.system.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetailCheckoutService {
    private static final int MAX_LINES = 100;
    @Autowired private AppGoodsMapper goodsMapper;
    @Autowired private AppGoodsCartMapper cartMapper;
    @Autowired private AppUserAddressMapper addressMapper;
    @Autowired private AppGoodsOrderMapper orderMapper;
    @Autowired private AppGoodsOrderDetailMapper detailMapper;
    @Autowired private RetailCouponService coupons;
    @Autowired private RetailOrderStore store;

    public Quote quote(Long userId, Request request) { return price(userId,request,false); }

    private Quote price(Long userId, Request request, boolean lock) {
        if (request == null || userId == null) throw new ServiceException("结算参数无效");
        List<Line> lines = new ArrayList<>();
        List<Long> ids = request.getCartIds();
        if (ids != null && !ids.isEmpty()) {
            if (ids.size() > MAX_LINES || ids.contains(null) || new HashSet<>(ids).size() != ids.size() || request.getGoodsId() != null) throw new ServiceException("购物车选择无效");
            List<Long> sorted = new ArrayList<>(ids); Collections.sort(sorted);
            List<AppGoodsCart> carts = new ArrayList<>();
            for (Long id : sorted) {
                if (id == null || id <= 0) throw new ServiceException("购物车选择无效");
                AppGoodsCart cart = lock ? store.lockCart(id) : cartMapper.selectAppGoodsCartByCartId(id);
                if (cart == null || !userId.equals(cart.getUserId())) throw new ServiceException("购物车商品不存在");
                if (Long.valueOf(1).equals(cart.getIsSku())) throw new ServiceException("所选规格尚不支持合并结算");
                carts.add(cart);
            }
            carts.sort(Comparator.comparing(AppGoodsCart::getGoodsId));
            for (AppGoodsCart cart : carts) {
                Line line = line(cart.getGoodsId(),cart.getGoodsCount(),lock); line.setCartId(cart.getCartId()); lines.add(line);
            }
        } else {
            lines.add(line(request.getGoodsId(),request.getCount(),lock));
        }
        Long supplier = lines.get(0).getSupplierId(); Long dept = lines.get(0).getDeptId();
        for (Line line : lines) {
            if (!supplier.equals(line.getSupplierId()) || !Objects.equals(dept,line.getDeptId())) throw new ServiceException("请选择同一供应商的商品结算");
        }
        Quote quote = RetailPricing.total(lines); quote.setSupplierId(supplier);
        quote.setSupplierName(String.valueOf(store.supplier(lines.get(0).getGoodsId()).get("supplier_name")));
        coupons.price(userId,request.getCouponGotId(),quote);
        quote.setFingerprint(digest(JSON.toJSONString(quote)));
        return quote;
    }
    private Line line(Long id, Integer count, boolean lock) {
        if (id == null || count == null || count < 1 || count > 9999) throw new ServiceException("商品数量无效");
        AppGoods goods = lock ? goodsMapper.selectRetailGoodsForUpdate(id) : goodsMapper.selectAppGoodsByGoodsId(id);
        if (goods == null || !"1".equals(goods.getStatus()) || !"online".equals(goods.getGoodsType())) throw new ServiceException("商品已下架");
        if (Integer.valueOf(1).equals(goods.getIsSku())) throw new ServiceException("请先选择有效商品规格");
        if (goods.getStock() == null || goods.getStock() < count) throw new ServiceException(goods.getGoodsName()+"库存不足");
        if (goods.getPrice() == null || goods.getPrice().signum() <= 0) throw new ServiceException("商品价格无效");
        BigDecimal shipping = goods.getExpressFee() == null ? BigDecimal.ZERO : goods.getExpressFee();
        if (shipping.signum() < 0) throw new ServiceException("商品运费配置无效");
        Line line = new Line(); line.setGoodsId(id); line.setCategoryId(goods.getCategoryId()); line.setDeptId(goods.getDeptId());
        line.setSupplierId(((Number)store.supplier(id).get("supplier_id")).longValue());
        line.setGoodsName(goods.getGoodsName()); line.setGoodsCover(goods.getGoodsCover()); line.setUnit(goods.getUnit());
        line.setSpecifications(goods.getDescription()); line.setCount(count); line.setPrice(goods.getPrice()); line.setShipping(shipping);
        return line;
    }
    @Transactional(rollbackFor=Exception.class, isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AppGoodsOrder submit(Long userId, Request request) {
        if (request == null || request.getCheckoutKey() == null || !request.getCheckoutKey().matches("[A-Za-z0-9-]{16,64}")
                || request.getFingerprint() == null || request.getRemark() != null && request.getRemark().length() > 500) throw new ServiceException("结算参数无效");
        store.requireTransactions(); store.lockUser(userId);
        String hash = digest(JSON.toJSONString(request));
        Map<String,Object> old = store.existing(userId,request.getCheckoutKey());
        if (old != null) {
            if (!hash.equals(old.get("request_hash"))) throw new ServiceException("重复提交内容不一致，请重新结算");
            return store.get(((Number)old.get("order_id")).longValue());
        }
        if (request.getAddressId() == null || request.getAddressId() <= 0) throw new ServiceException("请选择收货地址");
        AppUserAddress address = addressMapper.selectAppUserAddressByAddressId(request.getAddressId());
        if (address == null || !userId.equals(address.getUserId())) throw new ServiceException("请选择有效收货地址");
        Quote quote = price(userId,request,true);
        if (!quote.getFingerprint().equals(request.getFingerprint())) throw new ServiceException("价格、优惠或商品已变化，请重新确认金额");
        AppGoodsOrder order = newOrder(userId,request,quote);
        List<Line> stockOrder = new ArrayList<>(quote.getItems()); stockOrder.sort(Comparator.comparing(Line::getGoodsId));
        for (Line line : stockOrder) {
            if (goodsMapper.reserveStock(line.getGoodsId(),(long)line.getCount()) != 1) throw new ServiceException(line.getGoodsName()+"库存不足");
        }
        if (orderMapper.insertAppGoodsOrder(order) != 1 || order.getOrderId() == null) throw new ServiceException("创建订单失败");
        order.setOrderNo("20"+order.getOrderId());
        if (orderMapper.updateAppGoodsOrder(order) != 1) throw new ServiceException("保存订单失败");
        coupons.consume(quote,order);
        for (Line line : quote.getItems()) saveDetail(order,line);
        store.save(order,quote,request,address,hash); store.enrich(order);
        return order;
    }
    private AppGoodsOrder newOrder(Long userId, Request request, Quote quote) {
        AppGoodsOrder order = new AppGoodsOrder();
        order.setUserId(userId); order.setGoodsId(quote.getItems().get(0).getGoodsId()); order.setDeptId(quote.getItems().get(0).getDeptId());
        order.setAddressId(request.getAddressId()); order.setRemark(request.getRemark()); order.setCreateTime(new Date());
        order.setGoodsCount(quote.getItems().stream().mapToLong(Line::getCount).sum());
        order.setMoneyTotal(quote.getMoneyTotal()); order.setMoneyExpress(quote.getMoneyExpress());
        order.setMoneyDiscount(quote.getMoneyDiscount()); order.setMoneyPayable(quote.getMoneyPayable()); order.setPayMoney(quote.getMoneyPayable());
        order.setStatus("0"); order.setPayStatus("0");
        if (quote.getCouponGotId() != null) { order.setCouponGotIds(String.valueOf(quote.getCouponGotId())); order.setDistributionChannelCode(quote.getChannelCode()); }
        if (quote.getMoneyPayable().signum() == 0) { order.setStatus("1"); order.setPayStatus("1"); order.setPayType("coupon"); order.setPayTime(new Date()); }
        return order;
    }
    private void saveDetail(AppGoodsOrder order, Line line) {
        AppGoodsOrderDetail detail = new AppGoodsOrderDetail();
        detail.setOrderId(order.getOrderId()); detail.setUserId(order.getUserId()); detail.setGoodsId(line.getGoodsId());
        detail.setGoodsCount((long)line.getCount()); detail.setGoodsMoney(line.getSubtotal()); detail.setDiscountMoney(line.getDiscount());
        detail.setIsSku(0); detail.setStatus("0");
        if (detailMapper.insertAppGoodsOrderDetail(detail) != 1) throw new ServiceException("保存商品明细失败");
    }
    private String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(); for (byte b : hash) result.append(String.format("%02x", b)); return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
