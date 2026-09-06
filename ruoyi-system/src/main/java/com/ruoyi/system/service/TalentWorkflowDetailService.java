package com.ruoyi.system.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentWorkflowDetailService {
    private final TalentWorkflowAccess access;
    private final TalentBindingStore store;
    public TalentWorkflowDetailService(TalentWorkflowAccess access, TalentBindingStore store) {
        this.access = access; this.store = store;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> detail(String actor, String scope, String resource) {
        access.admin(actor, scope);
        String[] parts = resource.split(":");
        if (parts.length != 2) throw new TalentCenterApiException(400, "记录类型无效");
        long id = TalentWorkflowAccess.id(parts[1]);
        if ("advisors".equals(parts[0])) return advisor(id);
        if ("refunds".equals(parts[0])) return refund(id);
        throw new TalentCenterApiException(400, "记录类型无效");
    }
    private Map<String, Object> advisor(long id) {
        Map<String, Object> row = store.consultant(id, false);
        List<Map<String, String>> fields = new ArrayList<>();
        fields.add(field("申请人", row.get("consultant_name")));
        fields.add(field("档案编号", row.get("consultant_no")));
        fields.add(field("联系电话（脱敏）", row.get("masked_mobile")));
        fields.add(field("关联后台账号", row.get("user_id")));
        fields.add(field("申请备注", row.get("remark")));
        fields.add(field("当前状态", "00".equals(row.get("status")) ? "待审核" : "01".equals(row.get("status")) ? "已通过" : "未通过"));
        return result("康养顾问入驻申请", fields);
    }
    private Map<String, Object> refund(long id) {
        Map<String, Object> row = store.one("SELECT a.after_id,a.order_id,a.out_order_no,a.status,a.app_refund_money,"
            + "a.refund_money,a.reason_description,a.remark,g.goods_name FROM app_goods_order_after a "
            + "LEFT JOIN app_goods g ON g.goods_id=a.goods_id WHERE a.after_id=?", id);
        List<Map<String, String>> fields = new ArrayList<>();
        fields.add(field("售后编号", row.get("after_id")));
        fields.add(field("订单编号", row.get("out_order_no")));
        fields.add(field("商品", row.get("goods_name")));
        fields.add(field("申请退款金额", money(row.get("app_refund_money"))));
        fields.add(field("审核退款金额", money(row.get("refund_money"))));
        fields.add(field("退款原因", row.get("reason_description")));
        fields.add(field("审核备注", row.get("remark")));
        fields.add(field("当前状态", "0".equals(row.get("status")) ? "待审核" : "6".equals(row.get("status")) ? "退款完成" : "已进入后续流程（状态 " + row.get("status") + "）"));
        return result("售后退款详情", fields);
    }
    private static Map<String, Object> result(String title, List<Map<String, String>> fields) {
        Map<String, Object> result = new LinkedHashMap<>(); result.put("title", title); result.put("fields", fields); return result;
    }
    private static Map<String, String> field(String label, Object value) {
        Map<String, String> field = new LinkedHashMap<>(); field.put("label", label);
        field.put("value", value == null || String.valueOf(value).isEmpty() ? "未填写" : String.valueOf(value)); return field;
    }
    private static String money(Object value) { return value == null ? "金额待核对" : "¥" + value; }
}
