package com.ruoyi.system.service.impl;

import java.util.*;
import java.net.URI;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.RetailCheckout;
import com.ruoyi.system.domain.AppUserAddress;
import com.ruoyi.common.annotation.Excel;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service("supplierFulfillmentService")
public class SupplierFulfillmentService {
    @Autowired private JdbcTemplate jdbc;
    @Value("${supplier.notice.enabled:false}") private boolean enabled;
    @Value("${supplier.notice.operations-webhook:}") private String webhook;
    private static final int MAX_ATTEMPTS = 5;
    private RestTemplate client = createClient();
    private static RestTemplate createClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    public Map<String,Object> configuration() {
        Map<String,Object> result = new HashMap<>(); result.put("enabled",enabled);
        result.put("channel","企业微信内部运营群"); result.put("configured",validWebhook());
        return result;
    }
    @Data
    public static class ShipmentRow {
        @Excel(name="订单号") private String orderNo;
        @Excel(name="商品") private String goodsName;
        @Excel(name="规格") private String specifications;
        @Excel(name="数量") private Integer count;
        @Excel(name="收货人") private String contact;
        @Excel(name="电话") private String phone;
        @Excel(name="地址") private String address;
    }
    public List<ShipmentRow> exportRows(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size()>100 || ids.contains(null) || new HashSet<>(ids).size()!=ids.size()) throw new ServiceException("请选择要导出的订单");
        String marks = String.join(",",Collections.nCopies(ids.size(),"?"));
        List<Map<String,Object>> orders = jdbc.queryForList("SELECT o.order_no,f.address_snapshot,f.lines_snapshot FROM app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id WHERE o.pay_status='1' AND o.status='1' AND (o.is_apply_cancel IS NULL OR o.is_apply_cancel<>1) AND f.order_id IN ("+marks+") ORDER BY f.order_id",ids.toArray());
        if (orders.size()!=ids.size()) throw new ServiceException("订单状态已变化，请刷新清单");
        List<ShipmentRow> result = new ArrayList<>();
        for (Map<String,Object> order : orders) {
            AppUserAddress address = JSON.parseObject(String.valueOf(order.get("address_snapshot")),AppUserAddress.class);
            for (RetailCheckout.Line line : JSON.parseArray(String.valueOf(order.get("lines_snapshot")),RetailCheckout.Line.class)) {
                ShipmentRow row = new ShipmentRow(); row.setOrderNo(String.valueOf(order.get("order_no"))); row.setGoodsName(line.getGoodsName());
                row.setSpecifications(line.getSpecifications()); row.setCount(line.getCount()); row.setContact(address.getLinkPerson()); row.setPhone(address.getLinkMobile());
                row.setAddress(String.join("",Arrays.asList(address.getProvinceName(),address.getCityName(),address.getCountyName(),address.getStreetName(),address.getAddressDetail()).stream().filter(Objects::nonNull).toArray(String[]::new)));
                result.add(row);
            }
        }
        return result;
    }
    public List<Map<String,Object>> list(Long supplierId) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT f.order_id AS orderId,o.order_no AS orderNo,s.supplier_name AS supplierName,f.notice_status AS noticeStatus,f.attempts,f.last_error AS lastError,f.address_snapshot AS addressSnapshot,f.lines_snapshot AS linesSnapshot,o.money_payable AS moneyPayable,o.pay_status AS payStatus,o.is_apply_cancel AS isApplyCancel,o.status AS orderStatus,o.send_express_name AS expressName,o.send_express_no AS expressNo,f.notified_at AS notifiedAt,f.confirmed_at AS confirmedAt FROM app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id JOIN app_supplier s ON s.supplier_id=f.supplier_id WHERE (? IS NULL OR f.supplier_id=?) ORDER BY f.fulfillment_id DESC LIMIT 200",supplierId,supplierId);
        for (Map<String,Object> row : rows) {
            row.put("address",JSON.parseObject(String.valueOf(row.remove("addressSnapshot"))));
            row.put("items",JSON.parseArray(String.valueOf(row.remove("linesSnapshot"))));
        }
        return rows;
    }
    public List<Map<String,Object>> goods(Long supplierId) {
        return jdbc.queryForList("SELECT g.goods_id AS goodsId,g.goods_name AS goodsName,g.status,b.supplier_id AS supplierId FROM app_goods g LEFT JOIN app_supplier_goods b ON b.goods_id=g.goods_id WHERE g.goods_type='online' AND (b.supplier_id=? OR b.supplier_id IS NULL) ORDER BY g.goods_id DESC LIMIT 500",supplierId);
    }
    @Transactional(rollbackFor=Exception.class)
    public void bind(Long supplierId, List<Long> goodsIds) {
        if (supplierId == null || goodsIds == null || goodsIds.contains(null) || goodsIds.isEmpty() || goodsIds.size()>100) throw new ServiceException("请选择要关联的商品");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM app_supplier WHERE supplier_id=? AND status='1'",Integer.class,supplierId)!=1) throw new ServiceException("供应商不存在或已停用");
        for (Long id : new TreeSet<>(goodsIds)) {
            if (id == null || jdbc.queryForObject("SELECT COUNT(*) FROM app_goods WHERE goods_id=? AND goods_type='online'",Integer.class,id)!=1) throw new ServiceException("商品无效");
            List<Long> current = jdbc.query("SELECT supplier_id FROM app_supplier_goods WHERE goods_id=? FOR UPDATE",(rs,n)->rs.getLong(1),id);
            if (!current.isEmpty() && !supplierId.equals(current.get(0))) throw new ServiceException("商品已关联其他供应商");
            if (current.isEmpty()) jdbc.update("INSERT INTO app_supplier_goods(goods_id,supplier_id) VALUES(?,?)",id,supplierId);
        }
    }
    @Transactional(rollbackFor=Exception.class)
    public void confirm(Long orderId) {
        int count = jdbc.update("UPDATE app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id SET f.confirmed_at=NOW(),f.notice_status='confirmed' WHERE f.order_id=? AND o.pay_status='1' AND o.status='1' AND (o.is_apply_cancel IS NULL OR o.is_apply_cancel<>1) AND (o.send_express_no IS NULL OR o.send_express_no='') AND f.notice_status NOT IN ('sending','confirmed','shipped')",orderId);
        if (count != 1) throw new ServiceException("订单不可确认或已确认");
    }
    @Transactional(rollbackFor=Exception.class)
    public void ship(Long orderId, Map<String,String> body) {
        String name = body.get("expressName"), number = body.get("expressNo");
        if (name == null || name.trim().isEmpty() || name.length()>50 || number == null || !number.matches("[A-Za-z0-9-]{5,60}")) throw new ServiceException("请填写有效快递公司和单号");
        // 先锁订单，与退款/关单使用同一把行锁，避免同时发货。
        jdbc.queryForList("SELECT order_id FROM app_goods_order WHERE order_id=? FOR UPDATE",orderId);
        Integer ready = jdbc.queryForObject("SELECT COUNT(*) FROM app_supplier_order WHERE order_id=? AND notice_status='confirmed'",Integer.class,orderId);
        if (ready != 1) throw new ServiceException("请先确认供应商已接单");
        int count = jdbc.update("UPDATE app_goods_order SET send_time=NOW(),send_express_name=?,send_express_no=?,update_time=NOW() WHERE order_id=? AND pay_status='1' AND status='1' AND (is_apply_cancel IS NULL OR is_apply_cancel<>1) AND (send_express_no IS NULL OR send_express_no='')",name.trim(),number,orderId);
        if (count != 1) throw new ServiceException("订单已发货、取消或退款，不能重复发货");
        jdbc.update("UPDATE app_supplier_order SET notice_status='shipped' WHERE order_id=?",orderId);
    }
    public void retry(Long orderId) {
        int count = jdbc.update("UPDATE app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id SET f.notice_status='pending',f.attempts=0,f.next_attempt=NULL,f.last_error=NULL WHERE f.order_id=? AND f.notice_status IN ('failed','uncertain') AND o.pay_status='1' AND o.status='1' AND (o.is_apply_cancel IS NULL OR o.is_apply_cancel<>1) AND (o.send_express_no IS NULL OR o.send_express_no='')",orderId);
        if (count != 1) throw new ServiceException("当前通知不可重试");
    }
    public void dispatchPending() {
        if (!enabled) return;
        if (!validWebhook()) throw new ServiceException("请在服务器配置有效的企业微信内部运营群通知地址");
        jdbc.update("UPDATE app_supplier_order SET notice_status='uncertain',last_error='上次发送中断，请核对后重试' WHERE notice_status='sending' AND next_attempt<NOW()");
        List<Long> ids = jdbc.query("SELECT f.order_id FROM app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id JOIN app_supplier s ON s.supplier_id=f.supplier_id WHERE f.notice_status IN ('pending','failed') AND f.attempts<? AND (f.next_attempt IS NULL OR f.next_attempt<=NOW()) AND s.status='1' AND o.pay_status='1' AND o.status='1' AND (o.is_apply_cancel IS NULL OR o.is_apply_cancel<>1) AND (o.send_express_no IS NULL OR o.send_express_no='') ORDER BY f.fulfillment_id LIMIT 20",(rs,n)->rs.getLong(1),MAX_ATTEMPTS);
        for (Long id : ids) send(id);
    }
    private boolean validWebhook() {
        try {
            URI uri = URI.create(webhook);
            return "https".equals(uri.getScheme()) && "qyapi.weixin.qq.com".equals(uri.getHost())
                    && "/cgi-bin/webhook/send".equals(uri.getPath()) && uri.getUserInfo()==null && uri.getPort()==-1
                    && uri.getFragment()==null && uri.getQuery()!=null && uri.getQuery().matches("key=[A-Za-z0-9-]+");
        } catch (IllegalArgumentException | NullPointerException error) { return false; }
    }
    private void send(Long orderId) {
        int claimed = jdbc.update("UPDATE app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id SET f.notice_status='sending',f.attempts=f.attempts+1,f.next_attempt=DATE_ADD(NOW(),INTERVAL 15 MINUTE) WHERE f.order_id=? AND f.notice_status IN ('pending','failed') AND o.pay_status='1' AND o.status='1' AND (o.is_apply_cancel IS NULL OR o.is_apply_cancel<>1) AND (o.send_express_no IS NULL OR o.send_express_no='')",orderId);
        if (claimed != 1) return;
        Map<String,Object> row = jdbc.queryForMap("SELECT o.order_no,f.lines_snapshot,s.supplier_name FROM app_supplier_order f JOIN app_goods_order o ON o.order_id=f.order_id JOIN app_supplier s ON s.supplier_id=f.supplier_id WHERE f.order_id=?",orderId);
        StringBuilder text = new StringBuilder("【逸享荟·内部运营待办】\n供应商：").append(row.get("supplier_name"))
                .append("\n已付款待发货订单：").append(row.get("order_no")).append("\n");
        List<RetailCheckout.Line> lines = JSON.parseArray(String.valueOf(row.get("lines_snapshot")),RetailCheckout.Line.class);
        for (RetailCheckout.Line line : lines.subList(0,Math.min(5,lines.size()))) text.append(line.getGoodsName(),0,Math.min(40,line.getGoodsName().length())).append(" × ").append(line.getCount()).append("\n");
        text.append("共 ").append(lines.size()).append(" 种商品。\n");
        text.append("请运营在后台【客户管理 → 供应商 → 发货协作】核对订单当前状态，导出完整发货清单，转发给对应供应商。\n")
                .append("供应商确认后登记接单，实际发货后回填物流。此消息仅通知内部运营，不代表已转发、已接单或已发货。");
        try {
            JSONObject payload = new JSONObject(); payload.put("msgtype","text"); payload.put("text",Collections.singletonMap("content",text.toString()));
            String response = client.postForObject(webhook,payload,String.class);
            JSONObject result = JSON.parseObject(response);
            if (result == null || !Integer.valueOf(0).equals(result.getInteger("errcode"))) {
                fail(orderId,"failed","通知服务拒绝请求，将自动重试"); return;
            }
            jdbc.update("UPDATE app_supplier_order SET notice_status='sent',notified_at=NOW(),last_error=NULL,next_attempt=NULL WHERE order_id=? AND notice_status='sending'",orderId);
        } catch (Exception error) { fail(orderId,"uncertain","发送结果不确定，请人工核对后重试"); }
    }
    private void fail(Long id, String status, String message) {
        jdbc.update("UPDATE app_supplier_order SET notice_status=?,last_error=?,next_attempt=DATE_ADD(NOW(),INTERVAL 30 MINUTE) WHERE order_id=? AND notice_status='sending'",status,message,id);
    }
}
