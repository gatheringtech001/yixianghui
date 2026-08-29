package com.ruoyi.system.service.feishu;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 飞书多维表格最小客户端，仅负责按订单编号新增或更新记录。
 */
@Component
public class TravelOrderFeishuClient
{
    private static final String API_ROOT = "https://open.feishu.cn/open-apis";
    private static final String ORDER_KEY_FIELD = "小程序订单号";

    @Value("${feishu.travel-order.app-id:}")
    private String appId;

    @Value("${feishu.travel-order.app-secret:}")
    private String appSecret;

    @Value("${feishu.travel-order.app-token:}")
    private String appToken;

    @Value("${feishu.travel-order.table-id:}")
    private String tableId;

    private volatile String accessToken;
    private volatile long accessTokenExpireAt;

    public void validateConfiguration()
    {
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(appSecret)
                || StringUtils.isEmpty(appToken) || StringUtils.isEmpty(tableId))
        {
            throw new IllegalStateException("飞书旅居订单同步配置不完整");
        }
    }

    public void upsert(String orderNo, JSONObject fields)
    {
        String recordId = findRecordId(orderNo);
        JSONObject body = new JSONObject();
        body.put("fields", fields);
        if (recordId == null)
        {
            request("POST", recordsUrl() + "?user_id_type=open_id", body);
            return;
        }
        request("PUT", recordsUrl() + "/" + recordId + "?user_id_type=open_id", body);
    }

    private String findRecordId(String orderNo)
    {
        JSONObject condition = new JSONObject();
        condition.put("field_name", ORDER_KEY_FIELD);
        condition.put("operator", "is");
        condition.put("value", new String[]{orderNo});

        JSONObject filter = new JSONObject();
        filter.put("conjunction", "and");
        filter.put("conditions", new Object[]{condition});
        JSONObject body = new JSONObject();
        body.put("filter", filter);
        body.put("field_names", new String[]{ORDER_KEY_FIELD});

        JSONObject response = request("POST", recordsUrl() + "/search?user_id_type=open_id", body);
        JSONArray items = response.getJSONObject("data").getJSONArray("items");
        if (items == null || items.isEmpty()) return null;
        if (items.size() > 1) throw new IllegalStateException("飞书存在重复订单编号: " + orderNo);
        return items.getJSONObject(0).getString("record_id");
    }

    private String recordsUrl()
    {
        return API_ROOT + "/bitable/v1/apps/" + appToken + "/tables/" + tableId + "/records";
    }

    private JSONObject request(String method, String target, JSONObject body)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(target).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            writeBody(connection, body);
            JSONObject response = readResponse(connection);
            if (connection.getResponseCode() >= 400 || response.getIntValue("code") != 0)
            {
                throw new IllegalStateException("飞书接口失败: " + response.getString("msg"));
            }
            return response;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("飞书接口请求失败", e);
        }
        finally
        {
            if (connection != null) connection.disconnect();
        }
    }

    private synchronized String getAccessToken()
    {
        if (StringUtils.isNotEmpty(accessToken) && System.currentTimeMillis() < accessTokenExpireAt)
        {
            return accessToken;
        }
        JSONObject body = new JSONObject();
        body.put("app_id", appId);
        body.put("app_secret", appSecret);
        JSONObject response = requestWithoutToken(API_ROOT + "/auth/v3/tenant_access_token/internal", body);
        accessToken = response.getString("tenant_access_token");
        if (StringUtils.isEmpty(accessToken)) throw new IllegalStateException("飞书鉴权未返回访问令牌");
        int expiresIn = response.getIntValue("expire");
        accessTokenExpireAt = System.currentTimeMillis() + Math.max(expiresIn - 300, 60) * 1000L;
        return accessToken;
    }

    private JSONObject requestWithoutToken(String target, JSONObject body)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection) new URL(target).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            writeBody(connection, body);
            JSONObject response = readResponse(connection);
            if (connection.getResponseCode() >= 400 || response.getIntValue("code") != 0)
            {
                throw new IllegalStateException("飞书鉴权失败: " + response.getString("msg"));
            }
            return response;
        }
        catch (IOException e)
        {
            throw new IllegalStateException("飞书鉴权请求失败", e);
        }
        finally
        {
            if (connection != null) connection.disconnect();
        }
    }

    private void writeBody(HttpURLConnection connection, JSONObject body) throws IOException
    {
        try (OutputStream output = connection.getOutputStream())
        {
            output.write(body.toJSONString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private JSONObject readResponse(HttpURLConnection connection) throws IOException
    {
        InputStream input = connection.getResponseCode() >= 400
                ? connection.getErrorStream() : connection.getInputStream();
        if (input == null) throw new IOException("飞书接口返回空响应");
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return JSONObject.parseObject(text.toString());
    }
}
