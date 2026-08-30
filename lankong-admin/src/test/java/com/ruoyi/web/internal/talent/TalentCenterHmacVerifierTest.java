package com.ruoyi.web.internal.talent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TalentCenterHmacVerifierTest
{
    private static final String SERVICE_ID = "talent-center-test";
    private static final String ACTOR_ID = "talent-user-1001";
    private static final String SECRET = "test-secret-not-for-production";
    private static final String PATH = "/internal/talent-center/v1/admin/resources/goods";
    private RedisCache redisCache;
    private TalentCenterHmacVerifier verifier;

    @BeforeEach
    void setUp()
    {
        redisCache = mock(RedisCache.class);
        verifier = new TalentCenterHmacVerifier(redisCache, SERVICE_ID, SECRET);
    }

    @Test
    void rejectsBadSignatureBeforeConsumingNonce()
    {
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> verifier.verify(SERVICE_ID, ACTOR_ID, now(), "nonce-0001", repeat("0", 64),
                        "GET", PATH, new byte[0]));

        assertEquals(401, error.getHttpStatus());
        verifyNoInteractions(redisCache);
    }

    @Test
    void rejectsExpiredTimestamp()
    {
        String timestamp = Long.toString(Instant.now().minusSeconds(301).getEpochSecond());
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> verifier.verify(SERVICE_ID, ACTOR_ID, timestamp, "nonce-0002",
                        signature("GET", PATH, timestamp, "nonce-0002", ACTOR_ID, new byte[0]),
                        "GET", PATH, new byte[0]));

        assertEquals(401, error.getHttpStatus());
        verifyNoInteractions(redisCache);
    }

    @Test
    void rejectsNonceReplay()
    {
        String timestamp = now();
        String nonce = "nonce-0003";
        String signature = signature("GET", PATH, timestamp, nonce, ACTOR_ID, new byte[0]);
        when(redisCache.setCacheObjectIfAbsent(anyString(), eq("used"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(true, false);

        verifier.verify(SERVICE_ID, ACTOR_ID, timestamp, nonce, signature, "GET", PATH, new byte[0]);
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> verifier.verify(SERVICE_ID, ACTOR_ID, timestamp, nonce, signature,
                        "GET", PATH, new byte[0]));

        assertEquals(409, error.getHttpStatus());
    }

    @Test
    void signsRawBodyBytes()
    {
        String timestamp = now();
        String nonce = "nonce-0004";
        byte[] body = "{\"status\":\"1\"}".getBytes(StandardCharsets.UTF_8);
        when(redisCache.setCacheObjectIfAbsent(anyString(), eq("used"), eq(300L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        verifier.verify(SERVICE_ID, ACTOR_ID, timestamp, nonce,
                signature("PUT", PATH, timestamp, nonce, ACTOR_ID, body), "PUT", PATH, body);
    }

    @Test
    void rejectsActorIdSubstitution()
    {
        String timestamp = now();
        String nonce = "nonce-0005";
        String signature = signature("GET", PATH, timestamp, nonce, ACTOR_ID, new byte[0]);

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> verifier.verify(SERVICE_ID, "talent-user-9999", timestamp, nonce, signature,
                        "GET", PATH, new byte[0]));

        assertEquals(401, error.getHttpStatus());
        verifyNoInteractions(redisCache);
    }

    @Test
    void rejectsMissingActorIdBeforeConsumingNonce()
    {
        String timestamp = now();
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> verifier.verify(SERVICE_ID, null, timestamp, "nonce-0006", repeat("0", 64),
                        "GET", PATH, new byte[0]));

        assertEquals(401, error.getHttpStatus());
        verifyNoInteractions(redisCache);
    }

    private String signature(String method, String path, String timestamp, String nonce,
            String actorId, byte[] body)
    {
        String payload = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + actorId + "\n"
                + TalentCenterHmacVerifier.sha256Hex(body);
        return TalentCenterHmacVerifier.encodeHex(verifier.hmac(payload));
    }

    private String now() { return Long.toString(Instant.now().getEpochSecond()); }
    private String repeat(String value, int count) { return new String(new char[count]).replace("\0", value); }
}
