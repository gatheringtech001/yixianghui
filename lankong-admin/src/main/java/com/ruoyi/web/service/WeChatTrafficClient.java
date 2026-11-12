package com.ruoyi.web.service;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Official WeChat daily traffic; unavailable is never represented as zero visitors. */
@Component
public class WeChatTrafficClient {
    @Value("${wx.mnp.appId:${wx.pay.appId:}}") private String appId;
    @Value("${wx.mnp.appSecret:}") private String appSecret;
    private String token;
    private long tokenExpiry;
    private Map<String,Object> cached;
    private long cacheExpiry;
    private String cachedDate;

    public synchronized Map<String,Object> yesterday() {
        String date = LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1).toString();
        if (cached != null && date.equals(cachedDate) && System.currentTimeMillis()<cacheExpiry) return new LinkedHashMap<>(cached);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("date",date);
        result.put("source","微信小程序数据分析");
        try {
            if (appId == null || appId.trim().isEmpty() || appSecret == null || appSecret.trim().isEmpty()) {
                result.put("status","unavailable"); result.put("reason","小程序数据分析凭据未配置");
            } else {
                String accessToken = accessToken();
                JSONObject body = new JSONObject();
                body.put("begin_date",date.replace("-","")); body.put("end_date",date.replace("-",""));
                JSONObject response = post("https://api.weixin.qq.com/datacube/getweanalysisappiddailyvisittrend?access_token="+accessToken,body);
                int code = response.getIntValue("errcode");
                if (code != 0) {
                    if (code==40001 || code==42001 || code==40014) tokenExpiry=0;
                    result.put("status","unavailable"); result.put("reason",errorReason(code)); result.put("errorCode",code);
                } else {
                    JSONArray list=response.getJSONArray("list");
                    JSONObject day=null;
                    if (list!=null) for (int i=0;i<list.size();i++) {
                        JSONObject item=list.getJSONObject(i);
                        if (date.replace("-","").equals(item.getString("ref_date"))) { day=item; break; }
                    }
                    if (day==null || !day.containsKey("visit_uv") || !day.containsKey("visit_pv")) {
                        result.put("status","pending"); result.put("reason","微信尚未返回该日访问数据");
                    } else {
                        result.put("status","available");
                        result.put("visitors",day.getLong("visit_uv")); result.put("pageViews",day.getLong("visit_pv"));
                        result.put("newVisitors",day.getLong("visit_uv_new")); result.put("openCount",day.getLong("session_cnt"));
                        result.put("averageStaySeconds",day.getBigDecimal("stay_time_uv"));
                    }
                }
            }
        } catch (WeChatDataException e) {
            result.put("status","unavailable"); result.put("reason",errorReason(e.code)); result.put("errorCode",e.code);
        } catch (Exception e) {
            // Never log exception text or URLs: requests contain the app secret/access token.
            result.put("status","unavailable"); result.put("reason","微信数据接口暂不可用，请稍后刷新");
        }
        result.put("fetchedAt",java.time.OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).toString());
        cached=new LinkedHashMap<>(result); cachedDate=date;
        cacheExpiry=System.currentTimeMillis()+("available".equals(result.get("status"))?600000:60000);
        return result;
    }
    private String accessToken() throws IOException {
        if (token!=null && System.currentTimeMillis()<tokenExpiry) return token;
        JSONObject body=new JSONObject(); body.put("grant_type","client_credential"); body.put("appid",appId); body.put("secret",appSecret); body.put("force_refresh",false);
        JSONObject response=post("https://api.weixin.qq.com/cgi-bin/stable_token",body);
        String value=response.getString("access_token");
        if (value==null || value.isEmpty()) throw new WeChatDataException(response.getIntValue("errcode"));
        token=value; tokenExpiry=System.currentTimeMillis()+Math.max(response.getIntValue("expires_in")-300,60)*1000L;
        return token;
    }
    static String errorReason(int code) {
        if (code==48001 || code==48002) return "微信未授权数据分析接口";
        if (code==45009 || code==45011) return "微信数据接口调用额度已用完";
        if (code==40001 || code==42001 || code==40014) return "微信凭证已失效，将自动重新获取";
        if (code==40064 || code==40164) return "微信接口需配置服务器 IP 白名单";
        return "微信数据接口暂不可用（"+code+"）";
    }
    private static class WeChatDataException extends IOException {
        final int code;
        WeChatDataException(int code) { super("WeChat authentication unavailable"); this.code=code; }
    }
    protected JSONObject post(String url,JSONObject body) throws IOException {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(3000); c.setReadTimeout(5000); c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json;charset=UTF-8");
        try {
            try (OutputStream out=c.getOutputStream()) { out.write(body.toJSONString().getBytes(StandardCharsets.UTF_8)); }
            if (c.getResponseCode()!=200) throw new IOException("WeChat HTTP error");
            try (InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096]; int n; while ((n=in.read(buffer))!=-1) out.write(buffer,0,n);
                JSONObject result=JSONObject.parseObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
                if (result==null) throw new IOException("Empty response"); return result;
            }
        } finally { c.disconnect(); }
    }
}
