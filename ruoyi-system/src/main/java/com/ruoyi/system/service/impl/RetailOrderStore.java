package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.*;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.RetailCheckout.*;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RetailOrderStore {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AppGoodsOrderMapper orders;

    public Map<String,Object> supplier(Long goodsId) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT s.supplier_id,s.supplier_name,s.status FROM app_supplier_goods b JOIN app_supplier s ON s.supplier_id=b.supplier_id WHERE b.goods_id=?", goodsId);
        if (rows.size() != 1 || !"1".equals(rows.get(0).get("status"))) throw new ServiceException("商品供应商未配置或已停用");
        return rows.get(0);
    }
    public void requireTransactions() {
        Integer bad = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('app_goods','app_goods_cart','app_goods_order','app_goods_order_detail','app_goods_coupon','app_goods_coupon_got','app_supplier_order') AND engine<>'InnoDB'", Integer.class);
        if (bad != null && bad > 0) throw new ServiceException("交易数据库尚未完成升级，暂不能合并结算");
    }
    public AppGoodsCart lockCart(Long cartId) {
        List<AppGoodsCart> rows = jdbc.query("SELECT cart_id,user_id,goods_id,goods_count,is_sku,data_id FROM app_goods_cart WHERE cart_id=? FOR UPDATE", (rs,n)-> {
            AppGoodsCart cart = new AppGoodsCart(); cart.setCartId(rs.getLong("cart_id")); cart.setUserId(rs.getLong("user_id"));
            cart.setGoodsId(rs.getLong("goods_id")); cart.setGoodsCount(rs.getInt("goods_count"));
            cart.setIsSku(rs.getLong("is_sku")); cart.setDataId(rs.getLong("data_id")); return cart;
        },cartId);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public Map<String,Object> existing(Long userId, String key) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT order_id,request_hash FROM app_supplier_order WHERE user_id=? AND checkout_key=? FOR UPDATE",userId,key);
        return rows.isEmpty() ? null : rows.get(0);
    }
    public void lockUser(Long userId) {
        // 同一用户的提交串行化，保护幂等键及购物车清理；不同用户互不阻塞。
        List<Long> ids = jdbc.query("SELECT user_id FROM sys_user WHERE user_id=? FOR UPDATE", (rs,n)->rs.getLong(1),userId);
        if (ids.isEmpty()) throw new ServiceException("用户不存在");
    }
    public void save(AppGoodsOrder order, Quote quote, Request request, AppUserAddress address, String hash) {
        jdbc.update("INSERT INTO app_supplier_order(order_id,user_id,supplier_id,checkout_key,request_hash,address_snapshot,lines_snapshot) VALUES(?,?,?,?,?,?,?)",
                order.getOrderId(),order.getUserId(),quote.getSupplierId(),request.getCheckoutKey(),hash,JSON.toJSONString(address),JSON.toJSONString(quote.getItems()));
        for (Line line : quote.getItems()) {
            if (line.getCartId() == null) continue;
            int changed = jdbc.update("DELETE FROM app_goods_cart WHERE cart_id=? AND user_id=? AND goods_count=?",line.getCartId(),order.getUserId(),line.getCount());
            if (changed != 1) throw new ServiceException("购物车已变化，请重新结算");
        }
    }
    public boolean enrich(AppGoodsOrder order) {
        if (order == null || order.getOrderId() == null) return false;
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT address_snapshot,lines_snapshot FROM app_supplier_order WHERE order_id=?",order.getOrderId());
        if (rows.isEmpty()) return false;
        Map<String,Object> row = rows.get(0);
        List<AppGoods> goods = new ArrayList<>();
        for (Line line : JSON.parseArray(String.valueOf(row.get("lines_snapshot")),Line.class)) {
            AppGoods item = new AppGoods(); item.setGoodsId(line.getGoodsId()); item.setGoodsName(line.getGoodsName());
            item.setGoodsCover(line.getGoodsCover()); item.setGoodsType("online"); item.setIsSku(0);
            item.setPrice(line.getPrice()); item.setUnit(line.getUnit()); item.setSpecifications(line.getSpecifications());
            item.setOrderQuantity((long)line.getCount()); goods.add(item);
        }
        order.setGoodsList(goods);
        order.setAddressInfo(JSON.parseObject(String.valueOf(row.get("address_snapshot")),AppUserAddress.class));
        return true;
    }
    public AppGoodsOrder get(Long orderId) {
        AppGoodsOrder order = orders.selectAppGoodsOrderByOrderId(orderId);
        enrich(order); return order;
    }
}
