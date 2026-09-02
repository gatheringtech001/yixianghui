package com.ruoyi.web.controller.app;

import com.alibaba.fastjson2.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FeishuMigrationControllerTest
{
    @Test
    void masksSensitiveValuesWithoutChangingBusinessFields()
    {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("fieldsJson", "{\"客户名称\":\"测试客户\",\"电话\":\"13800000000\"}");

        FeishuMigrationController.maskSensitiveFields(record, Collections.singleton("电话"));

        assertFalse(record.containsKey("fieldsJson"));
        JSONObject fields = (JSONObject) record.get("fields");
        assertEquals("测试客户", fields.getString("客户名称"));
        assertEquals("******", fields.getString("电话"));
    }
}
