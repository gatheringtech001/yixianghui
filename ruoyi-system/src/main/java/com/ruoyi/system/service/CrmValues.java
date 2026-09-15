package com.ruoyi.system.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

final class CrmValues
{
    private CrmValues() { }
    static Map<String, Object> map(Object... pairs)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    static Map<String, Object> pick(Map<String, Object> row, String... keys)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : keys) result.put(key, row.get(key));
        return result;
    }
    static Long customer(String value)
    {
        if (value == null || !value.matches("customer:[1-9][0-9]{0,17}")) throw new TalentCenterApiException(400, "客户编号不正确");
        return Long.valueOf(value.substring(9));
    }
    static String text(Object value)
    {
        if (value == null) return null;
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) return null;
        if (!raw.startsWith("[") && !raw.startsWith("{")) return raw;
        try { return jsonText(new ObjectMapper().readTree(raw)); }
        catch (java.io.IOException e) { return raw; }
    }
    private static String jsonText(JsonNode node)
    {
        if (node.isTextual()) return node.asText();
        if (node.isArray()) {
            List<String> items = new ArrayList<>();
            for (JsonNode item : node) { String value = jsonText(item); if (value != null) items.add(value); }
            return items.isEmpty() ? null : String.join("、", items);
        }
        if (node.has("text")) return node.get("text").asText();
        if (node.has("name")) return node.get("name").asText();
        return node.isNull() ? null : node.toString();
    }
    static Integer age(Map<String, Object> row)
    {
        try {
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            // 历史年龄没有登记日期，不能当作当前周岁；缺少生日时不推测。
            if (row.get("birthday") == null) return null;
            LocalDate birthday = LocalDate.parse(String.valueOf(row.get("birthday")));
            if (birthday.isAfter(today)) return null;
            int age = Period.between(birthday, today).getYears();
            return age < 0 || age > 130 ? null : age;
        } catch (RuntimeException e) { return null; }
    }
    static String instant(Object value)
    {
        try { return Instant.parse(String.valueOf(value)).toString(); }
        catch (RuntimeException e) { throw new TalentCenterApiException(400, "时间格式不正确"); }
    }
}
