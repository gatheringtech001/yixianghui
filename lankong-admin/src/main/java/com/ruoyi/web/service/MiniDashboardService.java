package com.ruoyi.web.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;

/** Read-only mini-program commerce snapshot. All business metrics use order creation cohorts. */
@Service
public class MiniDashboardService {
    private final JdbcTemplate jdbc;
    public MiniDashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    static final String CATEGORY = "CASE "
        + "WHEN EXISTS (SELECT 1 FROM app_goods_category r WHERE r.parent_id=0 AND r.status='1' AND TRIM(r.category_name)='全国旅居' AND (c.category_id=r.category_id OR FIND_IN_SET(r.category_id,c.parent_ids))) THEN '全国旅居' "
        + "WHEN EXISTS (SELECT 1 FROM app_goods_category r WHERE r.parent_id=0 AND r.status='1' AND TRIM(r.category_name)='老年教育' AND (c.category_id=r.category_id OR FIND_IN_SET(r.category_id,c.parent_ids))) THEN '芳华学院' "
        + "WHEN EXISTS (SELECT 1 FROM app_goods_category r WHERE r.parent_id=0 AND r.status='1' AND TRIM(r.category_name) IN ('云南好物','云野好物') AND (c.category_id=r.category_id OR FIND_IN_SET(r.category_id,c.parent_ids))) THEN '云南好物' ELSE '其他商品' END";
    static final String ORDERS = "(SELECT '2' kind,o.order_id,o.create_time,o.pay_status,o.pay_money,o.pay_time," + CATEGORY + " business "
        + "FROM app_goods_order o LEFT JOIN app_goods g ON g.goods_id=o.goods_id LEFT JOIN app_goods_category c ON c.category_id=g.category_id WHERE o.order_origin='mini_program' "
        + "UNION ALL SELECT '3',order_id,create_time,pay_status,pay_money,pay_time,'聚会活动' FROM app_activity_order) o";
    // Refund amounts are stored in fen. Repeated notifications of the same refund are counted once.
    static final String REFUNDS = "(SELECT order_type,order_id,SUM(amount)/100 refundAmount FROM "
        + "(SELECT order_type,order_id,COALESCE(NULLIF(agent_refund_no,''),CONCAT('log:',log_id)) refundKey,MAX(refund_money) amount "
        + "FROM app_pay_refund_log WHERE status='1' AND order_type IN ('2','3') GROUP BY order_type,order_id,refundKey) unique_refunds GROUP BY order_type,order_id) r";
    static final String METRICS = "COUNT(*) orderCount, "
        + "COALESCE(SUM(o.pay_status IN ('1','3','4')),0) paidCount, "
        + "COALESCE(SUM(o.pay_status='0'),0) pendingCount, "
        + "COALESCE(SUM(o.pay_status='2'),0) cancelledCount, "
        + "COALESCE(SUM(o.pay_status='4'),0) refundedCount, "
        + "COALESCE(SUM(CASE WHEN o.pay_status IN ('1','3','4') THEN COALESCE(o.pay_money,0) ELSE 0 END),0) receivedAmount, "
        + "COALESCE(SUM(r.refundAmount),0) refundAmount, "
        + "COALESCE(SUM(CASE WHEN o.pay_status IN ('1','3','4') THEN COALESCE(o.pay_money,0) ELSE 0 END),0)-COALESCE(SUM(r.refundAmount),0) netAmount, "
        + "COALESCE(SUM(o.pay_status IS NULL OR o.pay_status NOT IN ('0','1','2','3','4')),0) unknownStatusCount, "
        + "COALESCE(SUM(o.pay_status IN ('1','3','4') AND o.pay_money>0 AND o.pay_time IS NULL),0) missingPayTimeCount, "
        + "COALESCE(SUM(o.pay_status='4' AND r.refundAmount IS NULL),0) missingRefundCount";

    public static LocalDate periodStart(String period, LocalDate today) {
        switch (period) {
            case "today": return today;
            case "yesterday": return today.minusDays(1);
            case "7d": return today.minusDays(6);
            case "30d": return today.minusDays(29);
            case "all": return null;
            default: throw new ServiceException("统计时间范围无效");
        }
    }

    @Transactional(readOnly = true)
    public Map<String,Object> summary(String period) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate start = periodStart(period, today);
        LocalDate end = "yesterday".equals(period) ? today : today.plusDays(1);
        String where = start == null ? "" : " WHERE o.create_time >= ? AND o.create_time < ?";
        Object[] args = start == null ? new Object[0] : new Object[] {start.toString(),end.toString()};
        String from = " FROM " + ORDERS + " LEFT JOIN " + REFUNDS + " ON r.order_type=o.kind AND r.order_id=o.order_id";
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("totals",jdbc.queryForMap("SELECT " + METRICS + from + where,args));
        result.put("businesses",jdbc.queryForList("SELECT o.business," + METRICS + from + where + " GROUP BY o.business",args));
        result.put("period",period);
        result.put("startDate",start == null ? null : start.toString());
        result.put("endDate",end.minusDays(1).toString());
        result.put("updatedAt",java.time.ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).toOffsetDateTime().toString());
        return result;
    }
}
