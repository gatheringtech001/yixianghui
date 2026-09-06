package com.ruoyi.system.service;

import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentInboxService {
    private static final List<String> CATEGORIES = Arrays.asList("advisors", "refunds", "travel", "followup");
    private static final String TODAY = "DATE(DATE_ADD(UTC_TIMESTAMP(),INTERVAL 8 HOUR))";
    private static final String ORDER_UNASSIGNED = "(o.service_owner_user_id IS NULL OR u.user_id IS NULL "
        + "OR u.status<>'0' OR u.del_flag<>'0' OR a.actor_id IS NULL)";
    private static final String CUSTOMER_UNASSIGNED = "(c.consultant_id IS NULL OR p.user_id IS NULL OR u.user_id IS NULL "
        + "OR p.status IS NULL OR p.status<>'01' OR u.status<>'0' OR u.del_flag<>'0' OR a.actor_id IS NULL "
        + "OR (a.consultant_id IS NOT NULL AND a.consultant_id<>p.consultant_id))";
    private final JdbcTemplate jdbc;
    private final TalentWorkflowAccess access;

    public TalentInboxService(JdbcTemplate jdbc, TalentWorkflowAccess access) { this.jdbc = jdbc; this.access = access; }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String actor, String scopeName, String view) {
        TalentWorkflowAccess.Scope scope = checkedScope(actor, scopeName, view);
        Map<String, Object> counts = new LinkedHashMap<>(); counts.put("applications", 0);
        long total = 0;
        for (String category : CATEGORIES) {
            long count = count(query(scope, view, category)); counts.put(category, count); total += count;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", scope.admin ? "admin" : "self"); result.put("view", view);
        result.put("linked", scope.userId != null); result.put("counts", counts);
        result.put("total", total); result.put("readAt", Instant.now().toString());
        return result;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> page(String actor, String scopeName, String view, String category, String cursor) {
        TalentWorkflowAccess.Scope scope = checkedScope(actor, scopeName, view);
        if (!CATEGORIES.contains(category)) throw new TalentCenterApiException(400, "未知待办类别");
        long after = "first".equals(cursor) ? 0 : TalentWorkflowAccess.id(cursor);
        Query query = query(scope, view, category);
        List<Object> params = new ArrayList<>(query.params); params.add(after);
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT * FROM (" + query.sql + ") tasks WHERE numericId>? ORDER BY numericId LIMIT 51", params.toArray());
        boolean more = rows.size() > 50; String next = null;
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : rows.subList(0, Math.min(rows.size(), 50))) {
            next = String.valueOf(row.remove("numericId"));
            row.put("overdue", ((Number)row.get("overdue")).intValue() == 1); items.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items); result.put("total", count(query)); result.put("nextCursor", more ? next : null);
        return result;
    }
    private TalentWorkflowAccess.Scope checkedScope(String actor, String role, String view) {
        if (!Arrays.asList("my", "all", "unassigned").contains(view)) throw new TalentCenterApiException(400, "待办范围无效");
        TalentWorkflowAccess.Scope scope = access.resolve(actor, role);
        if (!"my".equals(view) && !scope.admin) throw new TalentCenterApiException(403, "只能查看自己的待办");
        return scope;
    }
    private long count(Query query) {
        Long value = jdbc.queryForObject("SELECT COUNT(*) FROM (" + query.sql + ") tasks", Long.class, query.params.toArray());
        return value == null ? 0 : value;
    }
    private Query query(TalentWorkflowAccess.Scope scope, String view, String category) {
        if ("advisors".equals(category)) return advisors(scope, view);
        if ("refunds".equals(category)) return refunds(scope, view);
        return "travel".equals(category) ? travel(scope, view) : followup(scope, view);
    }
    private Query advisors(TalentWorkflowAccess.Scope scope, String view) {
        return new Query("SELECT c.consultant_id numericId,CONCAT('advisor:',c.consultant_id) id,'advisors' category,"
            + "CONCAT('康养顾问入驻 · ',COALESCE(c.consultant_name,'未填写姓名')) title,"
            + "'请核对申请资料后审批' description,'待审核' status,'review-advisor' action,"
            + "CAST(c.consultant_id AS CHAR) targetId,NULL applicantId,NULL dueDate,0 overdue,NULL ownerName "
            + "FROM app_consultant c WHERE c.status='00' AND ?=1 AND ?<>'unassigned'", scope.admin ? 1 : 0, view);
    }
    private Query refunds(TalentWorkflowAccess.Scope scope, String view) {
        return new Query("SELECT a.after_id numericId,CONCAT('refund:',a.after_id) id,'refunds' category,"
            + "CONCAT(CASE WHEN g.goods_type IN ('hotel','travel') THEN '旅居' WHEN g.goods_type='education' THEN '课程' ELSE '商品' END,"
            + "'售后退款 · ',LEFT(COALESCE(g.goods_name,a.out_order_no,'未填写'),250)) title,"
            + "CONCAT(CASE WHEN a.app_refund_money IS NULL AND a.refund_money IS NULL THEN '金额待核对' "
            + "ELSE CONCAT('申请退款 ¥',COALESCE(a.app_refund_money,a.refund_money)) END,'；需在交易后台核验并审核') description,"
            + "'待审核' status,'view-refund' action,CAST(a.after_id AS CHAR) targetId,NULL applicantId,NULL dueDate,0 overdue,NULL ownerName "
            + "FROM app_goods_order_after a LEFT JOIN app_goods g ON g.goods_id=a.goods_id "
            + "WHERE a.status='0' AND ?=1 AND ?<>'unassigned'", scope.admin ? 1 : 0, view);
    }
    private Query travel(TalentWorkflowAccess.Scope scope, String view) {
        String owner = "all".equals(view) ? "1=1" : "unassigned".equals(view) ? ORDER_UNASSIGNED : "o.service_owner_user_id=?";
        String sql = "SELECT o.order_id numericId,CONCAT('travel:',o.order_id,':',o.travel_status) id,'travel' category,"
            + "CONCAT(CASE o.travel_status WHEN '0' THEN '确认订单' WHEN '1' THEN '安排入住' WHEN '3' THEN '办理离店' ELSE '确认结算' END,"
            + "' · ',LEFT(COALESCE(o.travel_base_name,g.goods_name,'旅居订单'),250)) title,"
            + "CONCAT(COALESCE(o.feishu_order_no,o.order_no,'未填订单编号'),' · ',COALESCE(o.contact_name,'未填写联系人')) description,"
            + "CASE o.travel_status WHEN '0' THEN '待确认' WHEN '1' THEN '待入住' WHEN '3' THEN '待离店' ELSE '待结算' END status,"
            + "CASE WHEN " + ORDER_UNASSIGNED + " THEN 'assign-owner' ELSE 'view-travel' END action,"
            + "CAST(o.order_id AS CHAR) targetId,NULL applicantId,"
            + "DATE_FORMAT(CASE WHEN o.travel_status='1' THEN o.check_in_date WHEN o.travel_status IN ('3','4') THEN o.check_out_date ELSE NULL END,'%Y-%m-%d') dueDate,"
            + "CASE WHEN DATE(CASE WHEN o.travel_status='1' THEN o.check_in_date ELSE o.check_out_date END)<" + TODAY
            + " AND o.travel_status IN ('1','3','4') THEN 1 ELSE 0 END overdue,"
            + "CASE WHEN " + ORDER_UNASSIGNED + " THEN NULL ELSE COALESCE(NULLIF(u.nick_name,''),CONCAT('后台账号 #',u.user_id)) END ownerName "
            + "FROM app_goods_order o LEFT JOIN app_goods g ON g.goods_id=o.goods_id "
            + "LEFT JOIN sys_user u ON u.user_id=o.service_owner_user_id "
            + "LEFT JOIN talent_center_admin_actor a ON a.user_id=o.service_owner_user_id AND a.status='0' WHERE " + owner
            + " AND (g.goods_type IN ('hotel','travel') OR o.order_origin='feishu_history') "
            + "AND (o.order_origin='feishu_history' OR o.pay_status='1') "
            + "AND (o.travel_status='0' OR (o.travel_status='1' AND DATE(o.check_in_date)<=DATE_ADD(" + TODAY + ",INTERVAL 1 DAY)) "
            + "OR (o.travel_status='3' AND DATE(o.check_out_date)<=DATE_ADD(" + TODAY + ",INTERVAL 1 DAY)) OR o.travel_status='4')";
        return "my".equals(view) ? new Query(sql, scope.userId) : new Query(sql);
    }
    private Query followup(TalentWorkflowAccess.Scope scope, String view) {
        String owner = "all".equals(view) ? "1=1" : "unassigned".equals(view) ? CUSTOMER_UNASSIGNED
            : "p.user_id=? AND p.status='01' AND (a.consultant_id IS NULL OR a.consultant_id=p.consultant_id)";
        String sql = "SELECT c.customer_id numericId,CONCAT('followup:',c.customer_id) id,'followup' category,"
            + "CONCAT('客户首次回访 · ',COALESCE(c.customer_name,'未填写姓名')) title,"
            + "'后台尚未登记回访，请联系客户并记录结果' description,'未回访' status,'view-customer' action,"
            + "CAST(c.customer_id AS CHAR) targetId,NULL applicantId,NULL dueDate,0 overdue,"
            + "CASE WHEN " + CUSTOMER_UNASSIGNED + " THEN NULL ELSE p.consultant_name END ownerName "
            + "FROM app_customer c LEFT JOIN app_consultant p ON p.consultant_id=c.consultant_id "
            + "LEFT JOIN sys_user u ON u.user_id=p.user_id "
            + "LEFT JOIN talent_center_admin_actor a ON a.user_id=p.user_id AND a.status='0' "
            + "WHERE c.del_flag='0' AND c.return_visit=0 AND " + owner;
        return "my".equals(view) ? new Query(sql, scope.userId) : new Query(sql);
    }
    private static class Query {
        final String sql; final List<Object> params;
        Query(String sql, Object... params) { this.sql = sql; this.params = Arrays.asList(params); }
    }
}
