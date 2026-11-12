package com.ruoyi.web.controller.system;
import com.alibaba.fastjson2.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GuanjiaLoginAssertionTest {
    static final String SECRET = String.join("", java.util.Collections.nCopies(64, "s"));
    static final String STATE = String.join("", java.util.Collections.nCopies(64, "a"));
    static final String MEMBERS = "subject=jywx_test";
    static JSONObject claim() {
        JSONObject v = new JSONObject();
        v.put("iss", "gathering-guanjia"); v.put("aud", "https://shzxj.lk01.cn");
        v.put("sub", "subject"); v.put("username", "jywx_test"); v.put("state", STATE);
        v.put("iat", 1000); v.put("exp", 1060); v.put("jti", String.join("", java.util.Collections.nCopies(48, "b")));
        return v;
    }
    static String sign(JSONObject v) throws Exception {
        String body = Base64.getUrlEncoder().withoutPadding().encodeToString(v.toJSONString().getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return body + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(body.getBytes(StandardCharsets.US_ASCII)));
    }
    @Test void validIdentityOnlyMapsToDedicatedApprovedAccount() throws Exception {
        assertEquals("jywx_test", GuanjiaLoginAssertion.verify(sign(claim()), STATE, SECRET, MEMBERS, 1010).getString("username"));
    }
    @Test void tamperedOrMissingCredentialsNeverAuthenticate() throws Exception {
        String token = sign(claim());
        for (String value : new String[]{null, "", token + ".extra", "x" + token, token.substring(0, token.length()-3) + "AAA"})
            assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(value, STATE, SECRET, MEMBERS, 1010));
        assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(token, null, SECRET, MEMBERS, 1010));
        assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(token, STATE, "", MEMBERS, 1010));
    }
    @Test void wrongAudienceIssuerBrowserAndUnapprovedUserAreRejectedEvenWithValidSignature() throws Exception {
        for (String[] change : new String[][]{{"aud","https://evil.invalid"},{"iss","other"},{"state","wrong"},{"sub","other"},{"username","admin"},{"username","jywx_other"},{"jti",""}}) {
            JSONObject v = claim(); v.put(change[0],change[1]); String token=sign(v);
            assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(token, STATE, SECRET, MEMBERS, 1010));
        }
    }
    @Test void expiredFutureAndExtendedLifetimeAssertionsAreRejected() throws Exception {
        String token=sign(claim());
        assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(token, STATE, SECRET, MEMBERS, 1060));
        assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(token, STATE, SECRET, MEMBERS, 990));
        JSONObject extended=claim(); extended.put("exp",1100); String value=sign(extended);
        assertThrows(IllegalArgumentException.class, () -> GuanjiaLoginAssertion.verify(value, STATE, SECRET, MEMBERS, 1010));
    }
}
