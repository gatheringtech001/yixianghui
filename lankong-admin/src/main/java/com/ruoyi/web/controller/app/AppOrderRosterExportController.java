package com.ruoyi.web.controller.app;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import com.github.pagehelper.PageHelper;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.domain.AppActivityOrder;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.service.IAppActivityService;
import com.ruoyi.system.service.IAppActivityOrderService;
import com.ruoyi.system.service.IAppGoodsOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only roster exports. Uses the existing order services and exact export permissions. */
@RestController
public class AppOrderRosterExportController {
    static final int LIMIT = 10000;
    @Autowired private IAppGoodsOrderService goodsOrders;
    @Autowired private IAppActivityOrderService activityOrders;
    @Autowired private IAppActivityService activities;

    @PreAuthorize("@ss.hasPermi('system:app_goods_order:export')")
    @Log(title = "商品报名名单", businessType = BusinessType.EXPORT)
    @PostMapping("/system/app_goods_order/roster")
    public void goods(HttpServletResponse response, AppGoodsOrder query) {
        List<AppGoodsOrder> orders;
        PageHelper.startPage(1, LIMIT + 1, false);
        try { orders = goodsOrders.selectAppGoodsOrderList(query); }
        finally { PageHelper.clearPage(); }
        checkLimit(orders.size());
        List<RosterRow> rows = new ArrayList<>();
        for (AppGoodsOrder order : orders) rows.add(goodsRow(order));
        new ExcelUtil<RosterRow>(RosterRow.class).exportExcel(response, rows, "商品报名名单");
    }

    @PreAuthorize("@ss.hasPermi('system:app_activity_order:export')")
    @Log(title = "活动报名名单", businessType = BusinessType.EXPORT)
    @PostMapping("/system/app_activity_order/roster")
    public void activity(HttpServletResponse response, AppActivityOrder query) {
        List<AppActivityOrder> orders;
        PageHelper.startPage(1, LIMIT + 1, false);
        try { orders = activityOrders.selectAppActivityOrderList(query); }
        finally { PageHelper.clearPage(); }
        checkLimit(orders.size());
        List<RosterRow> rows = new ArrayList<>();
        Map<Long, String> titles = new HashMap<>();
        for (AppActivityOrder order : orders) {
            Long id = order.getActivityId();
            if (id != null && !titles.containsKey(id)) {
                AppActivity activity = activities.selectAppActivityByActivityId(id);
                titles.put(id, activity == null ? "活动 #" + id : activity.getActivityName());
            }
            rows.add(activityRow(order, titles.get(id)));
        }
        new ExcelUtil<RosterRow>(RosterRow.class).exportExcel(response, rows, "活动报名名单");
    }

    static void checkLimit(int count) {
        if (count > LIMIT) throw new ServiceException("名单超过10000笔，请缩小筛选范围后导出");
    }
    static String payment(String code) {
        if ("0".equals(code)) return "未付款";
        if ("1".equals(code)) return "已付款";
        if ("2".equals(code)) return "已取消";
        if ("3".equals(code)) return "退款中";
        if ("4".equals(code)) return "已退款";
        return code == null || code.isEmpty() ? "未记录" : "支付状态 " + code;
    }
    static RosterRow goodsRow(AppGoodsOrder order) {
        RosterRow row = new RosterRow();
        row.orderNo = order.getOrderNo();
        row.business = "education".equals(order.getGoodsType()) ? "课程" : "hotel".equals(order.getGoodsType()) || order.getTravelStatus() != null ? "旅居" : "商品";
        row.item = order.getGoodsName();
        if (row.item == null || row.item.isEmpty()) row.item = order.getTravelBaseName();
        if (row.item == null || row.item.isEmpty()) row.item = "商品 #" + order.getGoodsId();
        row.name = order.getContactName(); row.phone = order.getContactPhone();
        row.payment = payment(order.getPayStatus()); row.amount = order.getMoneyPayable(); row.paid = order.getPayMoney();
        row.quantity = order.getGoodsCount(); row.created = order.getCreateTime(); row.paidAt = order.getPayTime();
        return row;
    }
    static RosterRow activityRow(AppActivityOrder order, String title) {
        RosterRow row = new RosterRow();
        row.orderNo = order.getOrderNo(); row.business = "活动"; row.item = title == null ? "活动 #" + order.getActivityId() : title;
        row.name = order.getSignName(); row.phone = order.getSignMobile(); row.payment = payment(order.getPayStatus());
        row.amount = order.getMoneyPayable(); row.paid = order.getPayMoney(); row.quantity = order.getSignCount();
        row.created = order.getCreateTime(); row.paidAt = order.getPayTime();
        return row;
    }
    // String cells preserve full phones and leading zeroes; no formulas or account objects are emitted.
    public static class RosterRow {
        @Excel(name = "订单号", width = 28) public String orderNo;
        @Excel(name = "业务") public String business;
        @Excel(name = "报名项目", width = 40) public String item;
        @Excel(name = "联系人") public String name;
        @Excel(name = "联系电话", width = 22) public String phone;
        @Excel(name = "付款状态") public String payment;
        @Excel(name = "应付金额") public BigDecimal amount;
        @Excel(name = "实付金额") public BigDecimal paid;
        @Excel(name = "人数或数量") public Number quantity;
        @Excel(name = "下单时间", width = 24, dateFormat = "yyyy-MM-dd HH:mm:ss") public Date created;
        @Excel(name = "付款时间", width = 24, dateFormat = "yyyy-MM-dd HH:mm:ss") public Date paidAt;
    }
}
