package com.ruoyi.web.internal.talent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;

@Component
public class TalentCenterHmacVerifier
{
    private static final long ALLOWED_SKEW_SECONDS = 300;
    private static final Pattern SAFE_NONCE = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
    private static final Pattern SAFE_ACTOR_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern HEX_SIGNATURE = Pattern.compile("[A-Fa-f0-9]{64}");
    private final RedisCache redisCache;
    private final String configuredServiceId;
    private final String secret;

    public TalentCenterHmacVerifier(RedisCache redisCache,
            @Value("${TALENT_CENTER_SERVICE_ID:}") String configuredServiceId,
            @Value("${TALENT_CENTER_SERVICE_SECRET:}") String secret)
    {
        this.redisCache = redisCache;
        this.configuredServiceId = configuredServiceId;
        this.secret = secret;
    }

    public void verify(String serviceId, String actorId, String timestamp, String nonce, String signature,
            String method, String path, byte[] body)
    {
        if (isBlank(configuredServiceId) || configuredServiceId.length() > 64 || isBlank(secret))
        {
            throw new TalentCenterApiException(503, "达人中心服务认证未配置");
        }
        if (!configuredServiceId.equals(serviceId))
        {
            throw new TalentCenterApiException(401, "服务认证失败");
        }
        if (actorId == null || !SAFE_ACTOR_ID.matcher(actorId).matches())
        {
            throw new TalentCenterApiException(401, "X-Actor-Id 格式不正确");
        }
        long epochSeconds = parseTimestamp(timestamp);
        if (Math.abs(Instant.now().getEpochSecond() - epochSeconds) > ALLOWED_SKEW_SECONDS)
        {
            throw new TalentCenterApiException(401, "请求时间戳已过期");
        }
        if (nonce == null || !SAFE_NONCE.matcher(nonce).matches())
        {
            throw new TalentCenterApiException(401, "nonce 格式不正确");
        }
        verifySignature(signature, method, path, timestamp, nonce, actorId, body);
        String nonceHash = sha256Hex((serviceId + ":" + nonce).getBytes(StandardCharsets.UTF_8));
        if (!redisCache.setCacheObjectIfAbsent("talent:center:nonce:" + nonceHash, "used", 300, TimeUnit.SECONDS))
        {
            throw new TalentCenterApiException(409, "nonce 已使用");
        }
    }

    private void verifySignature(String signature, String method, String path, String timestamp,
            String nonce, String actorId, byte[] body)
    {
        if (signature == null || !HEX_SIGNATURE.matcher(signature).matches())
        {
            throw new TalentCenterApiException(401, "签名错误");
        }
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + actorId + "\n"
                + sha256Hex(body);
        byte[] expected = hmac(payload);
        byte[] supplied = decodeHex(signature);
        if (!MessageDigest.isEqual(expected, supplied))
        {
            throw new TalentCenterApiException(401, "签名错误");
        }
    }

    byte[] hmac(String payload)
    {
        try
        {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            throw new IllegalStateException("HMAC-SHA256 不可用", e);
        }
    }

    static String sha256Hex(byte[] value)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        }
        catch (Exception e)
        {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    static String encodeHex(byte[] value)
    {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) result.append(String.format("%02x", item));
        return result.toString();
    }

    private byte[] decodeHex(String value)
    {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private long parseTimestamp(String timestamp)
    {
        try
        {
            return Long.parseLong(timestamp);
        }
        catch (Exception e)
        {
            throw new TalentCenterApiException(401, "时间戳必须为 Unix 秒");
        }
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
