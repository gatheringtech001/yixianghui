package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.AppGoldBizType;
import com.ruoyi.system.service.IAppGoldService;
import com.ruoyi.system.service.GoodsRefundGateway;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Paid booking changes are intents, never edits to a confirmed booking.
 * Approval/payment intents commit before external IO. Stable references make retries safe.
 */
@Service
public class BookingAdjustmentService {
    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final TransactionTemplate joined;
    private final BookingAdjustmentGateway gateway;
    private final IAppGoldService gold;
    private final GoodsRefundGateway refunds;
    private static final String ACTIVE = "('PAYMENT','REVIEW','PROCESSING')";

    public BookingAdjustmentService(JdbcTemplate db, PlatformTransactionManager manager,
                                    BookingAdjustmentGateway gateway, IAppGoldService gold, GoodsRefundGateway refunds) {
        this.db = db; this.gateway = gateway; this.gold = gold; this.refunds = refunds;
        tx = new TransactionTemplate(manager); tx.setTimeout(15);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        joined = new TransactionTemplate(manager); joined.setTimeout(15);
        joined.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @javax.annotation.PostConstruct
    public void verifyTransactionalSchema() {
        List<Map<String, Object>> tables = db.queryForList("SELECT table_name,engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('app_activity','app_activity_order','app_user_card','app_user_gold_log','app_user_info','app_pay_log','app_pay_refund_log','app_booking_adjustment','app_booking_adjustment_refund','app_goods_order_after','talent_center_workflow_audit')");
        require(tables.size() == 11 && tables.stream().allMatch(table -> "InnoDB".equalsIgnoreCase(String.valueOf(table.get("engine")))),
            "报名变更上线前须先完成已批准的建表和InnoDB迁移；未通过安全检查，服务拒绝启动");
        db.queryForList("SELECT business_type,booking_adjustment_id FROM app_goods_order_after LIMIT 0");
    }

    public Map<String, Object> submitActivity(long orderId, long user, int target) {
        return joined.execute(transaction -> {
            Map<String, Object> order = lockOrder("3", orderId);
            own(order, user); require("1".equals(order.get("pay_status")) && "1".equals(order.get("status")), "当前预约不可调整");
            int current = Math.toIntExact(n(order, "sign_count"));
            require(current > 0, "原报名人数异常，请联系客服核对");
            require(target >= 0 && target != current, "请选择新的报名人数");
            require(yuanFen(order.get("pay_money")) > 0, "免费活动仅限本人报名，人数固定为1");
            Map<String, Object> active = active("3", orderId);
            if (active != null) {
                require(n(active, "to_count") == target, "请先处理已有的补款或退款申请");
                return view(active);
            }
            Map<String, Object> activity = one("SELECT activity_id,status,sign_end_time,max_count,sign_count FROM app_activity WHERE activity_id=?", order.get("activity_id"));
            Date expires = new Date(System.currentTimeMillis() + 1800000L);
            // Refund/cancellation remains available after registration closes; adding participants does not.
            if (target > current) {
                require("1".equals(activity.get("status")), "活动未开放报名");
                Date deadline = com.ruoyi.common.utils.DateUtils.parseDate(activity.get("sign_end_time"));
                require(deadline == null || deadline.after(new Date()), "报名已截止");
                if (deadline != null && deadline.before(expires)) expires = deadline;
                assertCapacity(activity, target - current);
            }
            long paid = yuanFen(order.get("pay_money"));
            require(paid % current == 0, "历史订单单价无法精确拆分，请联系客服处理");
            long amount = Math.multiplyExact(paid / current, Math.abs((long) target - current));
            require(amount > 0 && amount <= Integer.MAX_VALUE, "调整金额无效");
            long id = insert("INSERT INTO app_booking_adjustment(business_type,order_id,user_id,activity_id,from_count,to_count,amount_fen,state,expires_at) VALUES ('3',?,?,?,?,?,?,?,?)",
                orderId, user, order.get("activity_id"), current, target, amount,
                target > current ? "PAYMENT" : "REVIEW", target > current ? expires : null);
            if (target > current) db.update("UPDATE app_booking_adjustment SET pay_no=? WHERE adjustment_id=?", MerchantOrderNumbers.create("40", id), id);
            else createAfterSale(read(id), order, String.valueOf(order.get("order_no")));
            return view(read(id));
        });
    }

    public Map<String, Object> submitCard(long recordId, long user) {
        return joined.execute(transaction -> {
            Map<String, Object> card = lockOrder("1", recordId); own(card, user);
            require("1".equals(card.get("status")), "仅生效中的会员卡可申请退款");
            Map<String, Object> prior = active("1", recordId);
            if (prior != null) return view(prior);
            List<Map<String, Object>> payments = payments("1", recordId);
            require(payments.size() == 1, "支付记录不唯一或缺失，请联系客服核对");
            long amount = available(payments.get(0)); require(amount > 0, "没有可申请退款的已付金额");
            long id = insert("INSERT INTO app_booking_adjustment(business_type,order_id,user_id,from_count,to_count,amount_fen,state) VALUES ('1',?,?,1,0,?,'REVIEW')", recordId, user, amount);
            createAfterSale(read(id), card, String.valueOf(payments.get(0).get("pay_no")));
            return view(read(id));
        });
    }

    public Map<String, Object> latest(String type, long orderId, long user) {
        Map<String, Object> order = order(type, orderId, false); own(order, user);
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM app_booking_adjustment WHERE business_type=? AND order_id=? ORDER BY adjustment_id DESC LIMIT 1", type, orderId);
        return rows.isEmpty() ? null : view(rows.get(0));
    }

    private void createAfterSale(Map<String, Object> change, Map<String, Object> original, String paymentNo) {
        BigDecimal amount = BigDecimal.valueOf(n(change, "amount_fen"), 2);
        insert("INSERT INTO app_goods_order_after(business_type,booking_adjustment_id,after_type,user_id,order_id,out_order_no,order_money,goods_id,goods_count,goods_money,express_money,app_refund_money,status,reason_description) VALUES (?,?,'2',?,?,?,?,0,?,?,0,?,'0',?)",
            change.get("business_type"), change.get("adjustment_id"), change.get("user_id"), change.get("order_id"), paymentNo,
            "3".equals(change.get("business_type")) ? original.get("pay_money") : amount,
            n(change,"from_count")-n(change,"to_count"), amount, amount,
            "3".equals(change.get("business_type")) ? "活动报名人数调整："+n(change,"from_count")+"人 → "+n(change,"to_count")+"人" : "会员卡退款申请");
    }

    public Object prepay(long id, long user) {
        Map<String, Object> row = atomic(() -> {
            Map<String, Object> initial = read(id); lockOrder("3", n(initial, "order_id"));
            Map<String, Object> change = locked(id); own(change, user);
            require("PAYMENT".equals(change.get("state")), "补款已完成或已关闭");
            require(((Date) change.get("expires_at")).after(new Date()), "补款已超时，请取消本次调整后重新操作");
            // Persist the payment intent before requesting WeChat. Callback can arrive before HTTP returns.
            List<Map<String, Object>> logs = db.queryForList("SELECT log_id,pay_money,status FROM app_pay_log WHERE pay_no=?", change.get("pay_no"));
            if (logs.isEmpty()) insert("INSERT INTO app_pay_log(user_id,order_id,order_type,pay_no,pay_name,pay_description,pay_money,pay_method,status,create_time) VALUES (?,?,'4',?,'微信支付','活动报名补差价',?,'wxpay','0',NOW())",
                user, id, change.get("pay_no"), change.get("amount_fen"));
            else require(logs.size() == 1 && "0".equals(logs.get(0).get("status")) && n(logs.get(0), "pay_money") == n(change, "amount_fen"), "补款支付流水异常，请先同步支付结果");
            return change;
        });
        return atomic(() -> {
            lockOrder("3", n(row, "order_id")); Map<String, Object> current = locked(id); own(current, user);
            require("PAYMENT".equals(current.get("state")) && ((Date) current.get("expires_at")).after(new Date()), "补款已到账、关闭或超时，请刷新");
            // The intent above is committed. Hold this booking lock during IO so a concurrent
            // cancellation cannot mistake an in-flight create for ORDER_NOT_EXIST.
            return gateway.prepay(current);
        });
    }

    public Map<String, Object> syncPayment(long id, long user, boolean cancel) {
        Map<String, Object> row = read(id); own(row, user);
        if (!"PAYMENT".equals(row.get("state"))) return view(row);
        if (cancel) {
            // Serialize close/query with the external prepay call. ORDER_NOT_EXIST can then
            // safely close a never-created order: no prepay is in flight or can start later.
            Transaction[] paid = new Transaction[1];
            Map<String, Object> result = atomic(() -> {
                Map<String, Object> initial = read(id); lockOrder("3", n(initial, "order_id"));
                Map<String, Object> change = locked(id); own(change, user);
                if (!"PAYMENT".equals(change.get("state"))) return view(change);
                long intents = db.queryForObject("SELECT COUNT(*) FROM app_pay_log WHERE pay_no=?", Long.class, change.get("pay_no"));
                if (intents > 0) {
                    Transaction payment = null;
                    try {
                        payment = gateway.queryPayment(String.valueOf(change.get("pay_no")));
                        require(payment != null && payment.getTradeState() != null, "微信支付结果未知，请稍后同步重试");
                    }
                    catch (com.wechat.pay.java.core.exception.ServiceException error) {
                        if (!"ORDER_NOT_EXIST".equals(error.getErrorCode())) throw error;
                    }
                    if (payment != null && payment.getTradeState() == Transaction.TradeStateEnum.SUCCESS) {
                        paid[0] = payment; return null;
                    }
                    if (payment != null && payment.getTradeState() != Transaction.TradeStateEnum.CLOSED)
                        gateway.closePayment(String.valueOf(change.get("pay_no")));
                }
                requireOne(db.update("UPDATE app_booking_adjustment SET state='CANCELLED',update_time=NOW() WHERE adjustment_id=? AND state='PAYMENT'", id));
                db.update("UPDATE app_pay_log SET status='2',update_time=NOW() WHERE pay_no=? AND status='0'", change.get("pay_no"));
                return view(read(id));
            });
            if (paid[0] != null) { recordPayment(paid[0]); return view(read(id)); }
            return result;
        }
        try {
            Transaction payment = gateway.queryPayment(String.valueOf(row.get("pay_no")));
            if (payment != null && payment.getTradeState() == Transaction.TradeStateEnum.SUCCESS) recordPayment(payment);
        } catch (com.wechat.pay.java.core.exception.ServiceException error) {
            if (!"ORDER_NOT_EXIST".equals(error.getErrorCode())) throw error;
        }
        return view(read(id));
    }

    public void recordPayment(Transaction payment) {
        atomic(() -> {
            Map<String, Object> initial = one("SELECT * FROM app_booking_adjustment WHERE pay_no=?", payment.getOutTradeNo());
            Map<String, Object> order = lockOrder("3", n(initial, "order_id")); Map<String, Object> row = locked(n(initial, "adjustment_id"));
            gateway.validatePayment(row, payment);
            if ("COMPLETED".equals(row.get("state"))) return null;
            require("PAYMENT".equals(row.get("state")), "已关闭的补款收到到账通知，需人工核对");
            require("1".equals(order.get("pay_status")) && "1".equals(order.get("status")) && n(order, "sign_count") == n(row, "from_count"), "原报名已变化，需人工核对");
            one("SELECT log_id FROM app_pay_log WHERE pay_no=? AND order_type='4' AND order_id=? AND user_id=? AND pay_money=? FOR UPDATE",
                row.get("pay_no"), row.get("adjustment_id"), row.get("user_id"), row.get("amount_fen"));
            long delta = n(row, "to_count") - n(row, "from_count");
            db.update("UPDATE app_activity SET sign_count=COALESCE(NULLIF(sign_count,''),0)+? WHERE activity_id=?", delta, row.get("activity_id"));
            BigDecimal net = new BigDecimal(order.get("pay_money").toString()).add(BigDecimal.valueOf(n(row, "amount_fen"), 2));
            requireOne(db.update("UPDATE app_activity_order SET sign_count=?,pay_money=?,money_payable=? WHERE order_id=? AND sign_count=? AND pay_status='1' AND status='1'",
                row.get("to_count"), net, net, row.get("order_id"), row.get("from_count")));
            requireOne(db.update("UPDATE app_pay_log SET status='1',update_time=NOW() WHERE pay_no=?", row.get("pay_no")));
            requireOne(db.update("UPDATE app_booking_adjustment SET state='COMPLETED',update_time=NOW() WHERE adjustment_id=? AND state='PAYMENT'", row.get("adjustment_id")));
            gold.grantOnPay(n(row, "user_id"), AppGoldBizType.ACTIVITY_ADJUSTMENT_PAY, n(row, "adjustment_id"), BigDecimal.valueOf(n(row, "amount_fen")), String.valueOf(row.get("pay_no")));
            return null;
        });
    }

    /** Business adapter for the EXISTING after-sales reviewer. No independent approval API or audit. */
    public Map<String, Object> refundContext(long id, boolean lock) {
        Map<String, Object> initial = read(id);
        Map<String, Object> original = lock ? lockOrder(String.valueOf(initial.get("business_type")), n(initial,"order_id"))
            : order(String.valueOf(initial.get("business_type")), n(initial,"order_id"), false);
        Map<String, Object> row = lock ? locked(id) : initial;
        Map<String, Object> context = new LinkedHashMap<>(row);
        context.put("original_user_id",original.get("user_id"));
        context.put("original_status",original.get("status"));
        context.put("original_pay_status","3".equals(row.get("business_type")) ? original.get("pay_status") : "1");
        context.put("original_count","3".equals(row.get("business_type")) ? original.get("sign_count") : 1);
        context.put("original_order_no",original.get("order_no"));
        try {
            List<Map<String,Object>> receipts=payments(String.valueOf(row.get("business_type")),n(row,"order_id"));
            long total=0,available=0;
            for(Map<String,Object> receipt:receipts){total=Math.addExact(total,n(receipt,"pay_money"));available=Math.addExact(available,available(receipt));}
            context.put("receipts",receipts); context.put("paid_fen",total); context.put("available_fen",available);
            if("1".equals(row.get("business_type")) && receipts.size()==1) context.put("original_order_no",receipts.get(0).get("pay_no"));
        } catch(ServiceException invalid) { context.put("receipt_error",invalid.getMessage()); }
        return context;
    }

    public void reserveAfterSaleRefund(long id, long amount) {
        require(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"退款预约必须在售后审核事务中执行");
        Map<String,Object> row=locked(id);
        Map<String,Object> after=one("SELECT status,refund_money FROM app_goods_order_after WHERE booking_adjustment_id=? FOR UPDATE",id);
        require("1".equals(after.get("status")) && yuanFen(after.get("refund_money"))==amount && amount==n(row,"amount_fen"),"请通过原有售后审核核对完整人数差价");
        require("REVIEW".equals(row.get("state")),"申请已处理，请刷新");
        allocateRefund(row);
        requireOne(db.update("UPDATE app_booking_adjustment SET state='PROCESSING',update_time=NOW() WHERE adjustment_id=? AND state='REVIEW'",id));
    }

    public void rejectAfterSaleRefund(long id) {
        require(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"拒绝申请必须在售后审核事务中执行");
        require("2".equals(one("SELECT status FROM app_goods_order_after WHERE booking_adjustment_id=? FOR UPDATE",id).get("status")),"请通过原有售后审核拒绝申请");
        requireOne(db.update("UPDATE app_booking_adjustment SET state='REJECTED',update_time=NOW() WHERE adjustment_id=? AND state='REVIEW'",id));
    }

    private void allocateRefund(Map<String, Object> row) {
        long remaining = n(row, "amount_fen");
        for (Map<String, Object> payment : payments(String.valueOf(row.get("business_type")), n(row, "order_id"))) {
            long available = available(payment); long amount = Math.min(available, remaining);
            if (amount <= 0) continue;
            String reference = "YXHBR" + n(row, "adjustment_id") + "P" + n(payment, "log_id");
            long logId = insert("INSERT INTO app_pay_refund_log(user_id,order_id,order_type,pay_method,agent_name,agent_refund_no,refund_money,status,create_time) VALUES (?,?,?,'wxpay','微信支付',?,?,'0',NOW())",
                row.get("user_id"), row.get("order_id"), row.get("business_type"), reference, amount);
            insert("INSERT INTO app_booking_adjustment_refund(adjustment_id,pay_log_id,refund_log_id,out_trade_no,out_refund_no,total_fen,refund_fen,state) VALUES (?,?,?,?,?,?,?,'PREPARED')",
                row.get("adjustment_id"), payment.get("log_id"), logId, payment.get("pay_no"), reference, payment.get("pay_money"), amount);
            remaining -= amount; if (remaining == 0) break;
        }
        require(remaining == 0, "已付流水或剩余可退金额不足，请核对历史退款");
    }

    private List<Map<String, Object>> payments(String type, long order) {
        Map<String, Object> original = order(type, order, false);
        List<Map<String, Object>> receipts = "1".equals(type)
            ? db.queryForList("SELECT log_id,pay_no,pay_money FROM app_pay_log WHERE order_type='1' AND order_id=? AND user_id=? AND status='1' AND pay_method='wxpay' ORDER BY log_id DESC", order, original.get("user_id"))
            : db.queryForList("SELECT p.log_id,p.pay_no,p.pay_money FROM app_pay_log p WHERE p.user_id=? AND p.status='1' AND p.pay_method='wxpay' AND ((p.order_type='3' AND p.order_id=? AND p.pay_no=?) OR (p.order_type='4' AND EXISTS (SELECT 1 FROM app_booking_adjustment a WHERE a.adjustment_id=p.order_id AND a.pay_no=p.pay_no AND a.amount_fen=p.pay_money AND a.business_type='3' AND a.order_id=? AND a.state='COMPLETED'))) ORDER BY p.log_id DESC", original.get("user_id"), order, original.get("order_no"), order);
        Set<String> numbers = new HashSet<>();
        for (Map<String, Object> receipt : receipts) require(receipt.get("pay_no") != null && numbers.add(String.valueOf(receipt.get("pay_no"))), "支付流水重复或缺少单号，请人工核对");
        return receipts;
    }

    private long available(Map<String, Object> payment) {
        long spent = db.queryForObject("SELECT COALESCE(SUM(refund_fen),0) FROM app_booking_adjustment_refund WHERE pay_log_id=?", Long.class, payment.get("log_id"));
        // Legacy refunds have no allocation row. Refuse rather than risk refunding the same money twice.
        long legacy = db.queryForObject("SELECT COUNT(*) FROM app_pay_refund_log r JOIN app_pay_log p ON p.log_id=? AND p.order_id=r.order_id AND p.order_type=r.order_type WHERE r.status IN ('0','1','3') AND NOT EXISTS (SELECT 1 FROM app_booking_adjustment_refund i WHERE i.refund_log_id=r.log_id)", Long.class, payment.get("log_id"));
        require(legacy == 0, "存在历史退款记录，请管理员先核对");
        return Math.max(0, n(payment, "pay_money") - spent);
    }

    public void dispatchApprovedRefunds(long id, boolean queryOnly) {
        require(Arrays.asList("PROCESSING","COMPLETED").contains(read(id).get("state")) && approvedAfterSale(id),"退款未经售后审核批准");
        for (Map<String, Object> item : db.queryForList("SELECT * FROM app_booking_adjustment_refund WHERE adjustment_id=? ORDER BY item_id", id)) {
            if ("SUCCESS".equals(item.get("state"))) continue;
            try {
                Refund result = queryOnly ? refunds.query(String.valueOf(item.get("out_refund_no"))) : refunds.createAllocation(item);
                recordRefund(result);
            } catch (RuntimeException error) {
                // Intent remains committed. Only an administrator can retry the SAME refund reference.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("退款结果待核对 adjustment={}, item={}, type={}", id, item.get("item_id"), error.getClass().getSimpleName());
            }
        }
    }

    public void notification(RefundNotification notification) {
        Refund evidence = new Refund(); evidence.setOutRefundNo(notification.getOutRefundNo()); evidence.setOutTradeNo(notification.getOutTradeNo());
        evidence.setRefundId(notification.getRefundId()); evidence.setTransactionId(notification.getTransactionId());
        evidence.setStatus(notification.getRefundStatus()); evidence.setAmount(notification.getAmount()); recordRefund(evidence);
    }

    public void recordRefund(Refund evidence) {
        require(evidence != null, "微信未返回退款凭证");
        atomic(() -> {
            Map<String, Object> initial = one("SELECT * FROM app_booking_adjustment_refund WHERE out_refund_no=?", evidence.getOutRefundNo());
            Map<String, Object> base = read(n(initial, "adjustment_id")); Map<String, Object> order = lockOrder(String.valueOf(base.get("business_type")), n(base, "order_id"));
            Map<String, Object> row = locked(n(base, "adjustment_id"));
            Map<String, Object> item = one("SELECT * FROM app_booking_adjustment_refund WHERE item_id=? FOR UPDATE", initial.get("item_id"));
            require(Arrays.asList("PROCESSING", "COMPLETED").contains(row.get("state")) && approvedAfterSale(n(row,"adjustment_id")), "退款未经管理员批准");
            validateRefund(item, evidence);
            String recorded = db.queryForObject("SELECT pay_no FROM app_pay_refund_log WHERE log_id=?", String.class, item.get("refund_log_id"));
            require(recorded == null || recorded.isEmpty() || recorded.equals(evidence.getRefundId()), "微信退款编号与已记录凭证不一致");
            if ("SUCCESS".equals(item.get("state"))) return null;
            if (Arrays.asList("CLOSED", "ABNORMAL").contains(item.get("state")) && Status.PROCESSING.equals(evidence.getStatus())) return null;
            String status = evidence.getStatus().name();
            db.update("UPDATE app_booking_adjustment_refund SET state=? WHERE item_id=?", status, item.get("item_id"));
            db.update("UPDATE app_pay_refund_log SET status=?,pay_no=?,agent_pay_no=?,update_time=NOW() WHERE log_id=?",
                Status.SUCCESS.equals(evidence.getStatus()) ? "1" : Status.CLOSED.equals(evidence.getStatus()) ? "2" : Status.ABNORMAL.equals(evidence.getStatus()) ? "3" : "0",
                evidence.getRefundId(), evidence.getTransactionId(), item.get("refund_log_id"));
            if(Status.CLOSED.equals(evidence.getStatus()) || Status.ABNORMAL.equals(evidence.getStatus()))
                db.update("UPDATE app_goods_order_after SET status='5' WHERE booking_adjustment_id=? AND status<>'6'",row.get("adjustment_id"));
            if (Status.SUCCESS.equals(evidence.getStatus())) {
                gold.reverseOnRefund(n(row, "user_id"), "3".equals(row.get("business_type")) ? AppGoldBizType.ACTIVITY_REFUND : AppGoldBizType.CARD_REFUND,
                    n(row, "order_id"), BigDecimal.valueOf(n(item, "refund_fen")), String.valueOf(item.get("out_refund_no")));
                long incomplete = db.queryForObject("SELECT COUNT(*) FROM app_booking_adjustment_refund WHERE adjustment_id=? AND state<>'SUCCESS'", Long.class, row.get("adjustment_id"));
                if (incomplete == 0 && !"COMPLETED".equals(row.get("state"))) completeRefund(row, order);
            }
            return null;
        });
    }

    static void validateRefund(Map<String, Object> item, Refund evidence) {
        require(Arrays.asList(Status.SUCCESS, Status.PROCESSING, Status.CLOSED, Status.ABNORMAL).contains(evidence.getStatus())
            && Objects.equals(item.get("out_refund_no"), evidence.getOutRefundNo()) && Objects.equals(item.get("out_trade_no"), evidence.getOutTradeNo())
            && evidence.getRefundId() != null && !evidence.getRefundId().isEmpty() && evidence.getAmount() != null
            && "CNY".equals(evidence.getAmount().getCurrency()) && Objects.equals(n(item, "total_fen"), evidence.getAmount().getTotal())
            && Objects.equals(n(item, "refund_fen"), evidence.getAmount().getRefund()), "微信退款凭证不匹配");
    }

    private void completeRefund(Map<String, Object> row, Map<String, Object> order) {
        if ("3".equals(row.get("business_type"))) {
            require(n(order, "sign_count") == n(row, "from_count"), "原报名人数已变更，请人工核对");
            long delta = n(row, "from_count") - n(row, "to_count");
            BigDecimal net = BigDecimal.valueOf(yuanFen(order.get("pay_money")) - n(row, "amount_fen"), 2);
            requireOne(db.update("UPDATE app_activity_order SET sign_count=?,pay_money=?,money_payable=?,status=?,pay_status=? WHERE order_id=? AND sign_count=? AND status='1' AND pay_status='1'",
                row.get("to_count"), net, net, n(row, "to_count") == 0 ? "4" : "1", n(row, "to_count") == 0 ? "4" : "1", row.get("order_id"), row.get("from_count")));
            db.update("UPDATE app_activity SET sign_count=GREATEST(0,COALESCE(NULLIF(sign_count,''),0)-?) WHERE activity_id=?", delta, row.get("activity_id"));
        } else requireOne(db.update("UPDATE app_user_card SET status='3',update_time=NOW() WHERE record_id=? AND status='1'", row.get("order_id")));
        requireOne(db.update("UPDATE app_booking_adjustment SET state='COMPLETED',update_time=NOW() WHERE adjustment_id=? AND state='PROCESSING'", row.get("adjustment_id")));
        requireOne(db.update("UPDATE app_goods_order_after SET status='6' WHERE booking_adjustment_id=? AND status IN ('1','5')",row.get("adjustment_id")));
    }

    public long reservedSeats(long activityId) {
        return reservedSeats(activityId, null);
    }

    public long reservedSeats(long activityId, Long excludingOrder) {
        long supplements = db.queryForObject("SELECT COALESCE(SUM(to_count-from_count),0) FROM app_booking_adjustment WHERE activity_id=? AND state='PAYMENT'", Long.class, activityId);
        long pending = db.queryForObject("SELECT COALESCE(SUM(o.sign_count),0) FROM app_activity_order o WHERE o.activity_id=? AND o.pay_status='0' AND o.status='0' AND (? IS NULL OR o.order_id<>?) AND EXISTS (SELECT 1 FROM app_pay_log p WHERE p.pay_no=o.order_no AND p.order_type='3' AND p.order_id=o.order_id AND p.status='0')", Long.class, activityId, excludingOrder, excludingOrder);
        return supplements + pending;
    }

    public void closePendingPaymentBeforeReprice(String payNo) {
        // Caller holds the activity and order locks across query/close and local state update.
        try {
            Transaction payment = gateway.queryPayment(payNo);
            require(payment != null && payment.getTradeState() != null, "微信支付结果未知，请稍后同步重试");
            require(payment.getTradeState() != Transaction.TradeStateEnum.SUCCESS, "订单已付款，请刷新后查看");
            if (payment.getTradeState() != Transaction.TradeStateEnum.CLOSED) gateway.closePayment(payNo);
        } catch (com.wechat.pay.java.core.exception.ServiceException error) {
            if (!"ORDER_NOT_EXIST".equals(error.getErrorCode())) throw error;
        }
    }

    public void closeExpired() {
        for (Map<String, Object> row : db.queryForList("SELECT adjustment_id,user_id FROM app_booking_adjustment WHERE state='PAYMENT' AND expires_at<NOW() ORDER BY adjustment_id LIMIT 100")) {
            try { syncPayment(n(row, "adjustment_id"), n(row, "user_id"), true); }
            catch (RuntimeException error) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("补款超时关单待核对 adjustment={}", row.get("adjustment_id")); }
        }
    }

    public void assertCapacity(Map<String, Object> activity, long adding) {
        long max = count(activity.get("max_count"));
        require(max == 0 || count(activity.get("sign_count")) + reservedSeats(n(activity, "activity_id")) + adding <= max, "报名名额不足");
    }

    private Map<String, Object> lockOrder(String type, long id) {
        require(Arrays.asList("1", "3").contains(type), "订单类型无效");
        if ("3".equals(type)) {
            Map<String, Object> initial = order(type, id, false);
            one("SELECT activity_id FROM app_activity WHERE activity_id=? FOR UPDATE", initial.get("activity_id"));
        }
        return order(type, id, true);
    }
    private Map<String, Object> order(String type, long id, boolean lock) {
        require(Arrays.asList("1", "3").contains(type), "订单类型无效");
        return one("SELECT * FROM " + ("3".equals(type) ? "app_activity_order WHERE order_id=?" : "app_user_card WHERE record_id=?") + (lock ? " FOR UPDATE" : ""), id);
    }
    private Map<String, Object> active(String type, long id) {
        List<Map<String, Object>> rows = db.queryForList("SELECT * FROM app_booking_adjustment WHERE business_type=? AND order_id=? AND state IN " + ACTIVE + " ORDER BY adjustment_id DESC FOR UPDATE", type, id);
        require(rows.size() <= 1, "存在重复待处理申请，请联系客服"); return rows.isEmpty() ? null : rows.get(0);
    }
    private Map<String, Object> read(long id) { return one("SELECT * FROM app_booking_adjustment WHERE adjustment_id=?", id); }
    private Map<String, Object> locked(long id) { return one("SELECT * FROM app_booking_adjustment WHERE adjustment_id=? FOR UPDATE", id); }
    private Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = db.queryForList(sql, args); require(rows.size() == 1, "订单记录不存在或不唯一"); return rows.get(0);
    }
    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        String identity = sql.startsWith("INSERT INTO app_booking_adjustment_refund") ? "item_id" : sql.startsWith("INSERT INTO app_booking_adjustment(") ? "adjustment_id" : sql.startsWith("INSERT INTO app_goods_order_after(") ? "after_id" : "log_id";
        db.update(connection -> { java.sql.PreparedStatement ps = connection.prepareStatement(sql, new String[]{identity});
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]); return ps; }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }
    private <T> T atomic(Supplier<T> operation) { return tx.execute(status -> operation.get()); }
    private static long n(Map<String, Object> row, String name) { return BookingAdjustmentGateway.number(row, name); }
    private static long count(Object value) { return value == null || value.toString().trim().isEmpty() ? 0 : new BigDecimal(value.toString()).longValueExact(); }
    private static long yuanFen(Object value) { return value == null ? 0 : new BigDecimal(value.toString()).movePointRight(2).longValueExact(); }
    private static void own(Map<String, Object> order, long user) { require(user > 0 && n(order, "user_id") == user, "非法订单"); }
    private static void require(boolean valid, String message) { if (!valid) throw new ServiceException(message); }
    private static void requireOne(int count) { require(count == 1, "订单状态已变化，请刷新"); }
    private boolean approvedAfterSale(long id) {
        return db.queryForObject("SELECT COUNT(*) FROM app_goods_order_after WHERE booking_adjustment_id=? AND status IN ('1','5','6')",Long.class,id)==1;
    }
    private Map<String, Object> view(Map<String, Object> row) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("adjustmentId", row.get("adjustment_id")); value.put("businessType", row.get("business_type")); value.put("orderId", row.get("order_id"));
        value.put("fromCount", row.get("from_count")); value.put("toCount", row.get("to_count")); value.put("amount", BigDecimal.valueOf(n(row, "amount_fen"), 2));
        List<Map<String,Object>> after=db.queryForList("SELECT after_id,remark FROM app_goods_order_after WHERE booking_adjustment_id=?",row.get("adjustment_id"));
        value.put("state", row.get("state")); value.put("reviewNote", after.isEmpty()?null:after.get(0).get("remark"));
        value.put("afterId",after.isEmpty()?null:after.get(0).get("after_id")); value.put("expiresAt", row.get("expires_at")); return value;
    }
}
