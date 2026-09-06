package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Service
public class TalentWorkflowAccess {
    private final JdbcTemplate jdbc;
    public TalentWorkflowAccess(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Scope resolve(String actorId, String role) {
        if (!"admin".equals(role) && !"self".equals(role)) throw new TalentCenterApiException(403,"身份范围无效");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT u.user_id FROM talent_center_admin_actor a JOIN sys_user u ON u.user_id=a.user_id WHERE a.actor_id=? AND a.status='0' AND u.status='0' AND u.del_flag='0'",actorId);
        Long userId=rows.isEmpty()?null:((Number)rows.get(0).get("user_id")).longValue();
        return new Scope(actorId,userId,"admin".equals(role));
    }
    public Scope admin(String actorId,String role) {
        Scope scope=resolve(actorId,role);
        if(!scope.admin || scope.userId==null) throw new TalentCenterApiException(403,"需要已绑定后台账号的管理员");
        return scope;
    }
    public static long id(Object value) {
        String text=String.valueOf(value);
        if(!text.matches("[1-9][0-9]{0,17}")) throw new TalentCenterApiException(400,"记录编号无效");
        return Long.parseLong(text);
    }
    public static String text(Object value,int max) {
        if(!(value instanceof String) || ((String)value).length()>max) throw new TalentCenterApiException(400,"输入内容无效");
        return ((String)value).trim();
    }
    public static class Scope {
        public final String actorId; public final Long userId; public final boolean admin;
        Scope(String actorId,Long userId,boolean admin){this.actorId=actorId;this.userId=userId;this.admin=admin;}
    }
}
