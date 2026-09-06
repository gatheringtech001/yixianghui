package com.ruoyi.system.service;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentBindingStore {
    final JdbcTemplate jdbc;
    public TalentBindingStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    Map<String, Object> one(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw new TalentCenterApiException(404, "后台记录不存在");
        return rows.get(0);
    }
    Map<String, Object> binding(String actor, boolean lock) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT user_id,consultant_id,status FROM talent_center_admin_actor WHERE actor_id=?" + suffix(lock), actor);
        return rows.isEmpty() ? null : rows.get(0);
    }
    Map<String, Object> user(long id, boolean lock) {
        Map<String, Object> row = one("SELECT user_id,nick_name,status,del_flag FROM sys_user WHERE user_id=?" + suffix(lock), id);
        if (!"0".equals(row.get("status")) || !"0".equals(row.get("del_flag")))
            throw new TalentCenterApiException(409, "后台账号已停用");
        return row;
    }
    Map<String, Object> consultant(long id, boolean lock) {
        return one("SELECT consultant_id,consultant_no,consultant_name,user_id,status,dept_id,remark,"
            + "CASE WHEN LENGTH(mobile)>=7 THEN CONCAT(LEFT(mobile,3),'****',RIGHT(mobile,4)) ELSE '未填写' END masked_mobile,"
            + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i:%s') updated FROM app_consultant WHERE consultant_id=?" + suffix(lock), id);
    }
    Map<String, Object> order(long id, boolean lock) {
        return one("SELECT order_id,order_no,service_owner,service_owner_user_id,"
            + "DATE_FORMAT(update_time,'%Y-%m-%d %H:%i:%s') updated FROM app_goods_order WHERE order_id=?" + suffix(lock), id);
    }
    String name(Map<String, Object> user) {
        Object name = user.get("nick_name");
        return name == null || String.valueOf(name).trim().isEmpty() ? "后台账号 #" + user.get("user_id") : String.valueOf(name);
    }
    void ensureFreeAccount(String actor, long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM talent_center_admin_actor WHERE user_id=? AND actor_id<>?", Integer.class, userId, actor);
        if (count != null && count > 0) throw new TalentCenterApiException(409, "该后台账号已绑定其他达人，不能重复绑定");
    }
    void ensureFreeConsultant(String actor, long consultantId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM talent_center_admin_actor WHERE consultant_id=? AND actor_id<>?", Integer.class, consultantId, actor);
        if (count != null && count > 0) throw new TalentCenterApiException(409, "该管家档案已绑定其他达人");
    }
    void ensureAssignable(long userId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM talent_center_admin_actor WHERE user_id=? AND status='0'", Integer.class, userId);
        if (count == null || count != 1) throw new TalentCenterApiException(409, "负责人尚未绑定达人账号");
    }
    public Map<String, Object> options(String actor, Map<String, Object> input) {
        String mode = TalentWorkflowAccess.text(input.get("mode"), 16);
        if (!Arrays.asList("bind", "owner").contains(mode)) throw new TalentCenterApiException(400, "搜索条件无效");
        String search = TalentWorkflowAccess.text(input.get("search"), 64);
        String consultantSearch = TalentWorkflowAccess.text(input.getOrDefault("consultantSearch", ""), 64);
        Map<String, Object> link = binding(actor, false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", searchUsers(actor, mode, search));
        result.put("consultants", "bind".equals(mode) ? searchConsultants(consultantSearch, link) : Collections.emptyList());
        result.put("binding", link == null ? null : bindingInfo(link));
        return result;
    }
    private List<Map<String, Object>> searchUsers(String actor, String mode, String search) {
        String filter = "owner".equals(mode)
            ? "EXISTS (SELECT 1 FROM talent_center_admin_actor a WHERE a.user_id=u.user_id AND a.status='0')"
            : "NOT EXISTS (SELECT 1 FROM talent_center_admin_actor a WHERE a.user_id=u.user_id AND a.actor_id<>?)";
        List<Object> args = new ArrayList<>();
        if ("bind".equals(mode)) args.add(actor);
        args.add(pattern(search)); args.add(search);
        return jdbc.queryForList("SELECT CAST(u.user_id AS CHAR) id,COALESCE(NULLIF(u.nick_name,''),'未填写昵称') name,"
            + "CONCAT('后台账号 #',u.user_id) detail FROM sys_user u WHERE u.status='0' AND u.del_flag='0' AND "
            + filter + " AND (COALESCE(u.nick_name,'') LIKE ? OR CAST(u.user_id AS CHAR)=?) ORDER BY u.user_id LIMIT 50", args.toArray());
    }
    private List<Map<String, Object>> searchConsultants(String search, Map<String, Object> link) {
        Object selected = link == null ? null : link.get("consultant_id");
        return jdbc.queryForList("SELECT CAST(consultant_id AS CHAR) id,COALESCE(consultant_name,'未填写姓名') name,"
            + "CONCAT('档案 #',consultant_id,CASE status WHEN '00' THEN ' · 待审核' WHEN '01' THEN ' · 已通过' WHEN '02' THEN ' · 未通过' ELSE ' · 未标记' END,"
            + "CASE WHEN user_id IS NULL THEN ' · 未绑定账号' ELSE CONCAT(' · 后台账号 #',user_id) END) detail FROM app_consultant "
            + "WHERE COALESCE(consultant_name,'') LIKE ? OR CAST(consultant_id AS CHAR)=? OR consultant_id=? "
            + "ORDER BY (consultant_id=?) DESC,consultant_id LIMIT 100", pattern(search), search, selected, selected);
    }
    private Map<String, Object> bindingInfo(Map<String, Object> link) {
        Map<String, Object> account = one("SELECT user_id,nick_name,status,del_flag FROM sys_user WHERE user_id=?", link.get("user_id"));
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", String.valueOf(account.get("user_id")));
        info.put("name", name(account));
        info.put("consultantId", link.get("consultant_id") == null ? null : String.valueOf(link.get("consultant_id")));
        info.put("active", "0".equals(link.get("status")) && "0".equals(account.get("status")) && "0".equals(account.get("del_flag")));
        return info;
    }
    private static String pattern(String value) { return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"; }
    private static String suffix(boolean lock) { return lock ? " FOR UPDATE" : ""; }
}
