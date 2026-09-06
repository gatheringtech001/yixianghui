package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GoodsRefundStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    public GoodsRefundStore(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc; transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(10);
    }
    public GoodsRefundSnapshot read(long id) { return read(id, false); }
    private GoodsRefundSnapshot read(long id, boolean lock) {
        if (lock) {
            Map<String, Object> after = one("SELECT order_id FROM app_goods_order_after WHERE after_id=?", id);
            one("SELECT order_id FROM app_goods_order WHERE order_id=? FOR UPDATE", after.get("order_id"));
            one("SELECT after_id FROM app_goods_order_after WHERE after_id=? FOR UPDATE", id);
        }
        Map<String, Object> row = one("SELECT a.after_id,a.after_type,a.user_id,a.order_id,a.out_order_no,a.app_refund_money,"
            + "a.refund_money,a.goods_money,a.status,a.reason_description,a.remark,a.back_express_name,a.back_express_no,"
            + "o.order_no,o.user_id order_user_id,o.pay_status,o.order_origin,o.status order_status,o.travel_status,"
            + "o.travel_status_before_refund,g.goods_name,p.pay_money payment_fen,p.status payment_status,"
            + "p.pay_method payment_method,p.order_id payment_order_id,p.user_id payment_user_id,"
            + "(SELECT COUNT(*) FROM app_pay_log q WHERE q.pay_no=a.out_order_no) payment_count "
            + "FROM app_goods_order_after a LEFT JOIN app_goods_order o ON o.order_id=a.order_id "
            + "LEFT JOIN app_goods g ON g.goods_id=a.goods_id LEFT JOIN app_pay_log p ON p.log_id="
            + "(SELECT MAX(q.log_id) FROM app_pay_log q WHERE q.pay_no=a.out_order_no) WHERE a.after_id=?", id);
        List<Map<String, Object>> logs = jdbc.queryForList("SELECT log_id,agent_refund_no,pay_no,refund_money,status "
            + "FROM app_pay_refund_log WHERE order_id=? AND order_type='2' ORDER BY log_id", row.get("order_id"));
        return new GoodsRefundSnapshot(row, logs);
    }
    public Prepared prepare(GoodsRefundRequest request) {
        return Objects.requireNonNull(transaction.execute(tx -> {
            GoodsRefundSnapshot snapshot = read(request.afterId, true);
            List<Map<String, Object>> prior = jdbc.queryForList(
                "SELECT actor_id,input_hash FROM talent_center_workflow_audit WHERE operation_id=? FOR UPDATE", request.operationId);
            if (!prior.isEmpty()) {
                if (!request.actor.equals(prior.get(0).get("actor_id")) || !request.inputHash().equals(prior.get(0).get("input_hash")))
                    throw new TalentCenterApiException(409, "确认编号已用于其他操作");
                return new Prepared(snapshot, false);
            }
            if (!snapshot.fingerprint().equals(request.expected)) throw new TalentCenterApiException(409, "售后状态或金额已变化，请重新预览");
            snapshot.validate(request);
            jdbc.update("INSERT INTO talent_center_workflow_audit(operation_id,actor_id,action_kind,input_hash,input_json,changes_json) VALUES (?,?,?,?,?,?)",
                request.operationId, request.actor, request.kind(), request.inputHash(), request.json(), JSON.toJSONString(snapshot.changes(request)));
            if ("approve".equals(request.decision)) reserve(snapshot, request);
            if ("reject".equals(request.decision)) {
                requireOne(jdbc.update("UPDATE app_goods_order_after SET status='2',remark=? WHERE after_id=? AND status='0'", request.note, request.afterId));
                restoreOrder(snapshot);
            }
            saveResult(request.operationId, read(request.afterId).result());
            return new Prepared(snapshot, !"reject".equals(request.decision));
        }));
    }
    private void reserve(GoodsRefundSnapshot snapshot, GoodsRefundRequest request) {
        requireOne(jdbc.update("INSERT INTO app_pay_refund_log(user_id,order_id,order_type,pay_method,agent_name,agent_refund_no,refund_money,status,create_time) VALUES (?,?,'2','wxpay','微信支付',?,?,'0',NOW())",
            snapshot.row.get("user_id"), snapshot.orderId(), snapshot.reference(), BigDecimal.valueOf(Objects.requireNonNull(request.refundFen))));
        requireOne(jdbc.update("UPDATE app_goods_order_after SET status='1',refund_money=?,remark=? WHERE after_id=? AND status='0'",
            BigDecimal.valueOf(request.refundFen, 2), request.note, snapshot.afterId()));
        requireOne(jdbc.update("UPDATE app_goods_order SET status='3',travel_status_before_refund=CASE WHEN travel_status IS NOT NULL AND travel_status NOT IN (?,?) THEN travel_status ELSE travel_status_before_refund END,"
            + "travel_status=CASE WHEN travel_status IS NULL THEN NULL ELSE ? END,update_time=NOW() WHERE order_id=?",
            TravelOrderStatusPolicy.REFUNDING, TravelOrderStatusPolicy.REFUNDED, TravelOrderStatusPolicy.REFUNDING, snapshot.orderId()));
    }
    public Map<String, Object> record(Refund evidence, Consumer<AppPayRefundLog> complete) {
        long id = afterId(evidence.getOutRefundNo());
        return Objects.requireNonNull(transaction.execute(tx -> {
            GoodsRefundSnapshot snapshot = read(id, true);
            Map<String, Object> log = snapshot.ownLog(); validateEvidence(snapshot, evidence, log);
            if ("1".equals(log.get("status")) && "6".equals(snapshot.text("status"))) return snapshot.result();
            // A delayed PROCESSING response must not overwrite an already verified terminal result.
            if ("1".equals(log.get("status")) && !Status.SUCCESS.equals(evidence.getStatus())) return snapshot.result();
            if (Arrays.asList("2", "3").contains(String.valueOf(log.get("status"))) && Status.PROCESSING.equals(evidence.getStatus())) return snapshot.result();
            String status = Status.SUCCESS.equals(evidence.getStatus()) ? "1" : Status.CLOSED.equals(evidence.getStatus()) ? "2" : Status.ABNORMAL.equals(evidence.getStatus()) ? "3" : "0";
            requireOne(jdbc.update("UPDATE app_pay_refund_log SET status=?,pay_no=?,agent_pay_no=?,update_time=NOW() WHERE log_id=?",
                status, evidence.getRefundId(), evidence.getTransactionId(), log.get("log_id")));
            if ("1".equals(status)) complete.accept(refundLog(snapshot, log, evidence));
            else if (!"0".equals(status)) {
                requireOne(jdbc.update("UPDATE app_goods_order_after SET status='5' WHERE after_id=? AND status<>'6'", id));
                restoreOrder(snapshot);
            }
            return read(id).result();
        }));
    }
    static void validateEvidence(GoodsRefundSnapshot snapshot, Refund evidence, Map<String, Object> log) {
        if (log == null || !Arrays.asList(Status.SUCCESS, Status.PROCESSING, Status.CLOSED, Status.ABNORMAL).contains(evidence.getStatus()) || evidence.getAmount() == null
                || !snapshot.reference().equals(evidence.getOutRefundNo()) || !snapshot.text("order_no").equals(evidence.getOutTradeNo())
                || !"CNY".equals(evidence.getAmount().getCurrency())
                || !Objects.equals(snapshot.paidFen(), evidence.getAmount().getTotal())
                || !Objects.equals(GoodsRefundSnapshot.integral(log.get("refund_money")), evidence.getAmount().getRefund())
                || evidence.getRefundId() == null || evidence.getRefundId().isEmpty())
            throw new TalentCenterApiException(502, "微信退款凭证与本次审核不匹配，未更新结果");
        String recorded = Objects.toString(log.get("pay_no"), "");
        if (!recorded.isEmpty() && !recorded.equals(evidence.getRefundId()))
            throw new TalentCenterApiException(502, "微信退款编号与已记录结果不一致");
    }
    private AppPayRefundLog refundLog(GoodsRefundSnapshot snapshot, Map<String, Object> row, Refund evidence) {
        AppPayRefundLog log = new AppPayRefundLog(); log.setLogId(TalentWorkflowAccess.id(row.get("log_id")));
        log.setUserId(TalentWorkflowAccess.id(snapshot.row.get("user_id"))); log.setOrderId(snapshot.orderId());
        log.setOrderType("2"); log.setStatus("1"); log.setAgentRefundNo(snapshot.reference());
        log.setRefundMoney(BigDecimal.valueOf(evidence.getAmount().getRefund())); return log;
    }
    private void restoreOrder(GoodsRefundSnapshot snapshot) {
        jdbc.update("UPDATE app_goods_order SET status='1',travel_status=CASE WHEN travel_status=? THEN COALESCE(travel_status_before_refund,?) ELSE travel_status END,update_time=NOW() WHERE order_id=? AND status='3'",
            TravelOrderStatusPolicy.REFUNDING, TravelOrderStatusPolicy.CONFIRMED, snapshot.orderId());
    }
    public void saveResult(String operation, Map<String, Object> result) {
        requireOne(jdbc.update("UPDATE talent_center_workflow_audit SET result_json=? WHERE operation_id=?", JSON.toJSONString(result), operation));
    }
    private Map<String, Object> one(String sql, Object id) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, id);
        if (rows.size() != 1) throw new TalentCenterApiException(404, "售后或原订单不存在");
        return rows.get(0);
    }
    static long afterId(String reference) {
        if (reference == null || !reference.matches("YXHAF[1-9][0-9]{0,17}")) throw new TalentCenterApiException(400, "退款编号无效");
        return TalentWorkflowAccess.id(reference.substring(5));
    }
    private static void requireOne(int count) { if (count != 1) throw new TalentCenterApiException(409, "退款记录已变化，操作未完成"); }
    public static final class Prepared {
        final GoodsRefundSnapshot snapshot; final boolean send;
        Prepared(GoodsRefundSnapshot snapshot, boolean send) { this.snapshot = snapshot; this.send = send; }
    }
}
