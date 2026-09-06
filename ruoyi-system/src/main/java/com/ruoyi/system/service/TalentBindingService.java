package com.ruoyi.system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import com.alibaba.fastjson2.JSON;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentBindingService {
    private final TalentBindingStore store;
    private final TalentWorkflowAccess access;
    private final RedisCache redis;

    public TalentBindingService(TalentBindingStore store, TalentWorkflowAccess access, RedisCache redis) {
        this.store = store; this.access = access; this.redis = redis;
    }
    public Map<String, Object> options(String actor, String scope, Map<String, Object> input) {
        access.admin(actor, scope);
        return store.options(text(input, "targetActorId", 128), input);
    }
    @Transactional(readOnly = true)
    public Map<String, Object> preview(String actor, String scope, Map<String, Object> input) {
        access.admin(actor, scope);
        return inspect(input, false);
    }
    @Transactional
    public Map<String, Object> execute(String actor, String scope, Map<String, Object> request) {
        access.admin(actor, scope);
        String operation = text(request, "operationId", 36), expected = text(request, "fingerprint", 64);
        if (!validUuid(operation) || !expected.matches("[0-9a-f]{64}"))
            throw new TalentCenterApiException(400, "确认编号或快照无效");
        Map<String, Object> input = input(request);
        Map<String, Object> prior = reserve(actor, request, input);
        if (prior != null) return prior;
        Map<String, Object> current = inspect(input, true);
        if (!expected.equals(current.get("fingerprint")))
            throw new TalentCenterApiException(409, "后台记录已变化，请重新预览确认");
        String kind = text(input, "kind", 32);
        if ("bind-identity".equals(kind)) bind(input);
        else if ("review-advisor".equals(kind)) reviewAdvisor(input);
        else requireOne(store.jdbc.update(
            "UPDATE app_goods_order SET service_owner_user_id=?,update_time=NOW() WHERE order_id=?",
            id(input, "backendUserId"), id(input, "orderId")));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "completed"); result.put("message", "后台已完成操作并记录审计");
        store.jdbc.update("UPDATE talent_center_workflow_audit SET result_json=?,changes_json=? WHERE operation_id=?",
            JSON.toJSONString(result), JSON.toJSONString(current.get("changes")), operation);
        return result;
    }
    private Map<String, Object> reserve(String actor, Map<String, Object> request, Map<String, Object> input) {
        String operation = text(request, "operationId", 36);
        String canonicalInput = JSON.toJSONString(new TreeMap<>(input));
        String inputHash = hash(operation + ":" + request.get("fingerprint") + ":" + canonicalInput);
        int inserted = store.jdbc.update(
            "INSERT IGNORE INTO talent_center_workflow_audit(operation_id,actor_id,action_kind,input_hash,input_json) VALUES (?,?,?,?,?)",
            operation, actor, text(input, "kind", 32), inputHash, canonicalInput);
        if (inserted == 1) return null;
        Map<String, Object> row = store.one(
            "SELECT actor_id,input_hash,result_json FROM talent_center_workflow_audit WHERE operation_id=? FOR UPDATE", operation);
        if (!actor.equals(row.get("actor_id")) || !inputHash.equals(row.get("input_hash")))
            throw new TalentCenterApiException(409, "确认编号已用于其他请求");
        if (row.get("result_json") == null) throw new TalentCenterApiException(409, "操作正在处理");
        return JSON.parseObject(String.valueOf(row.get("result_json")));
    }
    private Map<String, Object> inspect(Map<String, Object> input, boolean lock) {
        String kind = text(input, "kind", 32);
        String note = text(input, "note", "review-advisor".equals(kind) ? 255 : 500);
        if (note.isEmpty()) throw new TalentCenterApiException(400, "请填写审核或绑定说明");
        Inspection inspection = new Inspection();
        if ("bind-identity".equals(kind)) {
            inspection.title = "确认绑定后台身份"; inspectBinding(input, lock, inspection);
        } else if ("review-advisor".equals(kind)) {
            inspection.title = "确认康养顾问审核"; inspectAdvisor(input, lock, inspection);
        } else if ("assign-owner".equals(kind)) {
            inspection.title = "确认分配订单负责人"; inspectOwner(input, lock, inspection);
        } else throw new TalentCenterApiException(400, "不支持的操作");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", inspection.title); result.put("changes", inspection.changes);
        result.put("fingerprint", hash(JSON.toJSONString(inspection.snapshot)));
        return result;
    }
    private void inspectAdvisor(Map<String, Object> input, boolean lock, Inspection inspection) {
        String decision = text(input, "decision", 10);
        if (!Arrays.asList("approve", "reject").contains(decision))
            throw new TalentCenterApiException(400, "审批结果无效");
        Map<String, Object> person = store.consultant(id(input, "consultantId"), lock);
        if (!"00".equals(person.get("status"))) throw new TalentCenterApiException(409, "该入驻申请已处理");
        inspection.snapshot.putAll(person);
        inspection.add("申请人", String.valueOf(person.get("consultant_name")), String.valueOf(person.get("consultant_name")));
        inspection.add("审核状态", "待审核", "approve".equals(decision) ? "审核通过" : "审核拒绝");
        inspection.add("审核备注", Objects.toString(person.get("remark"), ""), text(input, "note", 255));
    }
    private void inspectOwner(Map<String, Object> input, boolean lock, Inspection inspection) {
        Map<String, Object> order = store.order(id(input, "orderId"), lock);
        Map<String, Object> user = store.user(id(input, "backendUserId"), lock);
        store.ensureAssignable(id(input, "backendUserId"));
        inspection.snapshot.putAll(order);
        inspection.snapshot.put("targetUserId", user.get("user_id")); inspection.snapshot.put("targetName", store.name(user));
        inspection.add("订单", String.valueOf(order.get("order_no")), String.valueOf(order.get("order_no")));
        inspection.add("负责人账号", order.get("service_owner_user_id") == null ? "未分配" : "后台账号 #" + order.get("service_owner_user_id"),
            store.name(user) + " #" + user.get("user_id"));
    }
    private void inspectBinding(Map<String, Object> input, boolean lock, Inspection inspection) {
        String actor = text(input, "targetActorId", 128);
        if (!validUuid(actor)) throw new TalentCenterApiException(400, "达人账号编号无效");
        long userId = id(input, "backendUserId");
        Map<String, Object> user = store.user(userId, lock), current = store.binding(actor, lock);
        if (current != null && ((Number) current.get("user_id")).longValue() != userId)
            throw new TalentCenterApiException(409, "该达人已有其他后台账号绑定，本流程不自动改绑");
        store.ensureFreeAccount(actor, userId);
        inspection.snapshot.put("userId", userId);
        inspection.snapshot.put("currentBinding", current == null ? "none" : JSON.toJSONString(new TreeMap<>(current)));
        inspection.snapshot.put("targetName", store.name(user));
        List<String> roles = roles(input);
        inspection.snapshot.put("roles", String.join(",", roles));
        inspection.add("达人账号", text(input, "displayName", 64), text(input, "displayName", 64));
        inspection.add("后台账号", current == null ? "未绑定" : "后台账号 #" + current.get("user_id"), store.name(user) + " #" + userId);
        inspection.add("绑定状态", current == null ? "未关联" : "0".equals(current.get("status")) ? "正常" : "已停用", "正常");
        boolean consultantRole = roles.contains("butler") || roles.contains("advisor");
        if (input.get("consultantId") != null) {
            if (!consultantRole) throw new TalentCenterApiException(400, "只有已通过管家或顾问身份才能关联管家档案");
            if (current != null && current.get("consultant_id") != null
                && ((Number) current.get("consultant_id")).longValue() != id(input, "consultantId"))
                throw new TalentCenterApiException(409, "该达人已关联其他管家档案，本流程不自动改绑");
            inspectConsultant(input, lock, inspection);
        } else if (consultantRole) {
            Integer count = store.jdbc.queryForObject("SELECT COUNT(*) FROM app_consultant WHERE user_id=?", Integer.class, userId);
            inspection.snapshot.put("existingProfiles", count);
            if (count != null && count > 0) throw new TalentCenterApiException(409, "该账号已有管家档案，请明确选择现有档案");
            inspection.add("管家档案", "不存在", "新建审核通过的空白档案");
        }
    }
    private void inspectConsultant(Map<String, Object> input, boolean lock, Inspection inspection) {
        long consultantId = id(input, "consultantId"), userId = id(input, "backendUserId");
        store.ensureFreeConsultant(text(input, "targetActorId", 128), consultantId);
        Map<String, Object> person = store.consultant(consultantId, lock);
        if (person.get("user_id") != null && ((Number) person.get("user_id")).longValue() != userId)
            throw new TalentCenterApiException(409, "管家档案已属于其他后台账号");
        inspection.snapshot.put("consultant", JSON.toJSONString(new TreeMap<>(person)));
        inspection.add("管家档案", person.get("consultant_name") + " #" + consultantId, "关联当前账号并设为审核通过");
    }
    private void bind(Map<String, Object> input) {
        String actor = text(input, "targetActorId", 128), name = text(input, "displayName", 64);
        long userId = id(input, "backendUserId");
        Long consultantId = input.get("consultantId") == null ? null : id(input, "consultantId");
        List<String> roles = roles(input);
        if (consultantId != null)
            requireOne(store.jdbc.update("UPDATE app_consultant SET user_id=?,status='01',update_time=NOW() WHERE consultant_id=?", userId, consultantId));
        else if (roles.contains("butler") || roles.contains("advisor")) consultantId = createConsultant(userId, name);
        if (store.binding(actor, true) == null)
            requireOne(store.jdbc.update("INSERT INTO talent_center_admin_actor(actor_id,user_id,display_name,status,consultant_id) VALUES (?,?,?,'0',?)", actor, userId, name, consultantId));
        else requireOne(store.jdbc.update("UPDATE talent_center_admin_actor SET consultant_id=COALESCE(?,consultant_id),status='0',update_time=NOW() WHERE actor_id=?", consultantId, actor));
        if (consultantId != null) clearConsultant(consultantId, userId);
    }
    private long createConsultant(long userId, String name) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        requireOne(store.jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO app_consultant(consultant_no,consultant_name,status,user_id,create_time,update_time) VALUES (?,?,'01',?,NOW(),NOW())",
                Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, "TC-" + userId); statement.setString(2, name); statement.setLong(3, userId);
            return statement;
        }, key));
        return Objects.requireNonNull(key.getKey(), "Missing consultant key").longValue();
    }
    private void reviewAdvisor(Map<String, Object> input) {
        long id = id(input, "consultantId"); Map<String, Object> person = store.consultant(id, true);
        requireOne(store.jdbc.update("UPDATE app_consultant SET status=?,update_time=NOW(),remark=? WHERE consultant_id=? AND status='00'",
            "approve".equals(input.get("decision")) ? "01" : "02", text(input, "note", 255), id));
        clearConsultant(id, person.get("user_id"));
    }
    private List<String> roles(Map<String, Object> input) {
        Object raw = input.get("roles");
        if (!(raw instanceof List) || ((List<?>)raw).isEmpty() || ((List<?>)raw).size() > 4)
            throw new TalentCenterApiException(400, "缺少已审批身份");
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>)raw) {
            String role = String.valueOf(item);
            if (!Arrays.asList("talent", "host", "butler", "advisor").contains(role))
                throw new TalentCenterApiException(400, "身份类型无效");
            result.add(role);
        }
        Collections.sort(result); return result;
    }
    private void clearConsultant(long id, Object userId) {
        Runnable clear = () -> {
            redis.deleteObject(CacheConstants.CONSULTANT_KEY + "id:" + id);
            if (userId != null) redis.deleteObject(CacheConstants.CONSULTANT_KEY + "by_user_id:" + userId);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { clear.run(); }
            });
        else clear.run();
    }
    @SuppressWarnings("unchecked")
    private static Map<String, Object> input(Map<String, Object> request) {
        Object raw = request.get("input");
        if (!(raw instanceof Map)) throw new TalentCenterApiException(400, "操作内容无效");
        return (Map<String, Object>)raw;
    }
    private static void requireOne(int count) {
        if (count != 1) throw new TalentCenterApiException(409, "记录已变化，操作未完成");
    }
    private static String text(Map<String, Object> input, String key, int max) { return TalentWorkflowAccess.text(input.get(key), max); }
    private static long id(Map<String, Object> input, String key) { return TalentWorkflowAccess.id(input.get(key)); }
    private static boolean validUuid(String value) { return value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"); }
    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception error) { throw new IllegalStateException("SHA-256 unavailable", error); }
    }
    private static class Inspection {
        String title;
        final Map<String, Object> snapshot = new TreeMap<>();
        final List<Map<String, String>> changes = new ArrayList<>();
        void add(String label, String before, String after) {
            Map<String, String> change = new LinkedHashMap<>();
            change.put("label", label); change.put("before", before); change.put("after", after); changes.add(change);
        }
    }
}
