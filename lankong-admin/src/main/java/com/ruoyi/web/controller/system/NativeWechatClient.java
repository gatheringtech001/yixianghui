package com.ruoyi.web.controller.system;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Component;

/** Direct official-account OAuth; never logs URLs, codes, secrets or OpenIDs. */
@Component
public class NativeWechatClient {
    /** Only the numeric WeChat error is retained; response text may contain identifiers. */
    public static class AuthorizationException extends IOException {
        private final int errorCode;
        AuthorizationException(int errorCode) { super("WeChat authorization rejected"); this.errorCode = errorCode; }
        public int getErrorCode() { return errorCode; }
    }
    protected HttpURLConnection connect(String url) throws IOException {
        return (HttpURLConnection) new URL(url).openConnection();
    }
    public String exchange(String appId, String secret, String code) throws IOException {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + encode(appId)
                + "&secret=" + encode(secret) + "&code=" + encode(code) + "&grant_type=authorization_code";
        HttpURLConnection c = connect(url);
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(5000); c.setReadTimeout(10000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("WeChat authorization unavailable");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048]; int n;
                while ((n = in.read(buffer)) != -1) {
                    if (out.size() + n > 32768) throw new IOException("Invalid WeChat response");
                    out.write(buffer, 0, n);
                }
                JSONObject data = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8));
                String openId = data == null ? null : data.getString("openid");
                if (data != null && data.getIntValue("errcode") != 0) {
                    throw new AuthorizationException(data.getIntValue("errcode"));
                }
                if (data == null || openId == null || !openId.matches("[A-Za-z0-9_-]{8,200}")) {
                    throw new IOException("Invalid WeChat response");
                }
                return subject(appId, openId);
            }
        } finally { c.disconnect(); }
    }
    static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (UnsupportedEncodingException impossible) { throw new IllegalStateException(); }
    }
    static String subject(String appId, String openId) { return hash(appId + "\0" + openId); }
    static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format("%02x", b & 255));
            return result.toString();
        } catch (Exception impossible) { throw new IllegalStateException(); }
    }
}
