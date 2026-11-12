package com.ruoyi.web.controller.system;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** A short-lived, audience-bound assertion. Never accepts roles from the browser. */
public final class GuanjiaLoginAssertion {
    private GuanjiaLoginAssertion() { }

    public static JSONObject verify(String assertion, String state, String secret, String members, long now) {
        try {
            if (secret == null || secret.length() < 64 || state == null || !state.matches("[a-f0-9]{64}")
                    || assertion == null || assertion.length() > 4096) throw new IllegalArgumentException();
            String[] parts = assertion.split("\\.", -1);
            if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9_-]+") || !parts[1].matches("[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(parts[0].getBytes(StandardCharsets.US_ASCII)), Base64.getUrlDecoder().decode(parts[1]))) throw new IllegalArgumentException();
            JSONObject value = JSON.parseObject(new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8));
            long issued = value.getLongValue("iat"), expires = value.getLongValue("exp");
            if (!"gathering-guanjia".equals(value.getString("iss")) || !"https://shzxj.lk01.cn".equals(value.getString("aud"))
                    || !state.equals(value.getString("state")) || issued > now + 5 || issued < now - 60
                    || expires <= now || expires <= issued || expires > issued + 60
                    || value.getString("jti") == null || !value.getString("jti").matches("[a-f0-9]{48}")) throw new IllegalArgumentException();
            String username = value.getString("username"), subject = value.getString("sub");
            if (username == null || !username.matches("jywx_[a-z0-9_]{3,48}") || subject == null) throw new IllegalArgumentException();
            boolean allowed = false;
            for (String entry : members.split(",")) if (entry.equals(subject + "=" + username)) allowed = true;
            if (!allowed) throw new IllegalArgumentException();
            return value;
        } catch (Exception e) {
            throw new IllegalArgumentException("微信登录凭证无效或已过期，请重新登录");
        }
    }
}
