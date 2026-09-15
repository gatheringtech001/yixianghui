package com.ruoyi.system.service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

final class CrmInput
{
    private CrmInput() { }
    static void validate(String action, boolean update, Map<String,Object> input)
    {
        if (input == null) fail();
        Set<String> allowed=new HashSet<>(Arrays.asList("requestId"));
        uuid(input.get("requestId"));
        if (update) { allowed.add("version"); version(input.get("version")); }
        if ("followups".equals(action) && !update) {
            allowed.addAll(Arrays.asList("content","method","occurredAt","nextAction","nextDueAt","completeTaskId","taskVersion"));
            text(input.get("content"),2000); oneOf(input.get("method"),"phone","wechat","visit","other"); CrmValues.instant(input.get("occurredAt"));
            pair(input,"nextAction","nextDueAt"); pair(input,"completeTaskId","taskVersion");
            if(input.containsKey("nextAction")) { text(input.get("nextAction"),200); CrmValues.instant(input.get("nextDueAt")); }
            if(input.containsKey("completeTaskId")) { uuid(input.get("completeTaskId")); version(input.get("taskVersion")); }
        } else if ("tasks".equals(action)) {
            allowed.addAll(Arrays.asList("title","dueAt")); text(input.get("title"),200); CrmValues.instant(input.get("dueAt"));
            if(update) { allowed.add("status"); oneOf(input.get("status"),"pending","done","cancelled"); }
        } else if ("needs".equals(action)) {
            allowed.add("title"); text(input.get("title"),200);
            if(update) { allowed.add("stage"); oneOf(input.get("stage"),"new","contacting","proposal","done","paused"); }
            else { allowed.add("businessLine"); oneOf(input.get("businessLine"),"travel","eldercare"); }
        } else fail();
        if (!allowed.containsAll(input.keySet())) fail();
    }
    static String uuid(Object value) {
        if (!(value instanceof String) || !((String)value).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) fail();
        return value.toString();
    }
    private static void version(Object value) {
        if (!(value instanceof Number)) fail();
        double number=((Number)value).doubleValue();
        if(number<1 || number>Integer.MAX_VALUE || number!=Math.floor(number)) fail();
    }
    private static void text(Object value,int max) { if(!(value instanceof String) || value.toString().trim().isEmpty() || value.toString().length()>max) fail(); }
    private static void oneOf(Object value,String... values) { if(!Arrays.asList(values).contains(value)) fail(); }
    private static void pair(Map<String,Object> input,String a,String b) { if(input.containsKey(a)!=input.containsKey(b)) fail(); }
    private static void fail() { throw new TalentCenterApiException(400,"提交内容不符合要求"); }
}
