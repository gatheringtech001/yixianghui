package com.ruoyi.web.controller.system;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.URLDecoder;
import javax.servlet.http.Cookie;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.web.service.*;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ruoyi.web.controller.system.GuanjiaLoginController.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class GuanjiaLoginControllerTest {
    static final String APP="wx1234567890abcdef", SUBJECT="31ecf49407cd34e9cabc9a62eb85d08fe27cd7e20eb64eb50799ff6d411461df";
    static class Cache extends RedisCache {
        final Map<String,Object> data = new ConcurrentHashMap<>();
        @Override public <T> T getCacheObject(String key) { return (T) data.get(key); }
        @Override public <T> void setCacheObject(String key,T value,Integer timeout,TimeUnit unit) { data.put(key,value); }
        @Override public <T> boolean setCacheObjectIfAbsent(String key,T value,long timeout,TimeUnit unit) { return data.putIfAbsent(key,value)==null; }
        @Override public boolean deleteObject(String key) { return data.remove(key)!=null; }
    }
    static class Fixture extends GuanjiaLoginController {
        Cache memory=new Cache(); SysUser user=new SysUser(); int issued=0, exchanged=0; boolean granted=true, failure=false;
        long clock=System.currentTimeMillis(); String desktop,mobile,ticket,state,confirm; MockHttpServletResponse callback;
        Fixture() throws Exception {
            user.setUserId(200L);user.setDeptId(100L);user.setUserName("jywx_test");user.setStatus("0");user.setDelFlag("0");
            set("appId",APP);set("appSecret","synthetic-secret");set("members",SUBJECT+"=jywx_test");set("cache",memory);
            set("users",Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ISysUserService.class},(p,m,a)->user));
            set("permissions",new SysPermissionService(){@Override public Set<String> getMenuPermission(SysUser u){return granted?Collections.singleton("*:*:*"):Collections.emptySet();}});
            set("login",new SysLoginService(){@Override public void loginPreCheck(String u,String p){} @Override public void recordLoginInfo(Long id){}});
            set("tokens",new TokenService(){@Override public String createToken(LoginUser u){issued++;assertEquals("jywx_test",u.getUsername());return "synthetic-test-token";}});
            set("wechat",new NativeWechatClient(){@Override public String exchange(String a,String s,String c) throws java.io.IOException {exchanged++;assertEquals(APP,a);if(failure)throw new java.io.IOException("SECRET_URL_SHOULD_NOT_APPEAR");return SUBJECT;}});
        }
        void set(String name,Object value) throws Exception { Field f=GuanjiaLoginController.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value); }
        @Override protected void auditLogin(String username) { }
        @Override protected long now(){return clock;}
        MockHttpServletRequest request(boolean phone){MockHttpServletRequest r=new MockHttpServletRequest();r.addHeader("Origin",ORIGIN);r.addHeader("User-Agent","MicroMessenger");if(phone&&mobile!=null)r.setCookies(new Cookie(MOBILE,mobile));if(!phone&&desktop!=null)r.setCookies(new Cookie(DESKTOP,desktop));return r;}
        String value(MockHttpServletResponse r){return r.getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1];}
        void begin(){MockHttpServletResponse r=new MockHttpServletResponse();ticket=(String)qr(request(false),r).get("ticket");assertNotNull(ticket);desktop=value(r);}
        void scan() throws Exception {MockHttpServletResponse r=new MockHttpServletResponse();super.scan(ticket,request(true),r);assertTrue(r.getRedirectedUrl().startsWith("https://open.weixin.qq.com/"));assertTrue(URLDecoder.decode(r.getRedirectedUrl(),"UTF-8").contains("redirect_uri="+ORIGIN+PATH+"/callback"));assertFalse(r.getRedirectedUrl().contains("gatheringtech"));state=r.getRedirectedUrl().split("state=")[1].split("#")[0];mobile=value(r);}
        void callback() throws Exception {callback=new MockHttpServletResponse();super.callback(state,"synthetic-code",request(true),callback);String html=callback.getContentAsString();if(html.contains("name=\"confirm\""))confirm=html.split("name=\"confirm\" value=\"")[1].split("\"")[0];}
        void ready() throws Exception {begin();scan();callback();assertNotNull(confirm);MockHttpServletResponse r=new MockHttpServletResponse();super.confirm(state,confirm,request(true),r);assertTrue(r.getContentAsString().contains("已确认登录"));}
        Object poll(){return super.poll(Collections.singletonMap("ticket",ticket),request(false),new MockHttpServletResponse());}
    }
    @Test void startNeverLeavesNativeServer() throws Exception {
        Fixture f=new Fixture();MockHttpServletResponse r=new MockHttpServletResponse();f.start(r);
        assertEquals(ORIGIN+"/guanjia-login",r.getRedirectedUrl());assertEquals("no-store",r.getHeader("Cache-Control"));
    }
    @Test void fullFlowRequiresPhoneConfirmationAndMintsOneToken() throws Exception {
        Fixture f=new Fixture();f.begin();assertTrue(f.desktop.contains("."));assertTrue(f.poll().toString().contains("waiting"));f.scan();f.callback();
        assertNotNull(f.confirm);assertEquals(1,f.exchanged);assertFalse(f.callback.getContentAsString().contains(SUBJECT));assertTrue(f.poll().toString().contains("waiting"));
        f.confirm(f.state,f.confirm,f.request(true),new MockHttpServletResponse());assertTrue(f.poll().toString().contains("synthetic-test-token"));f.poll();assertEquals(1,f.issued);
    }
    @Test void wrongBrowserMissingCookieAndCrossOriginCannotPoll() throws Exception {
        Fixture f=new Fixture();f.ready();
        for(int i=0;i<3;i++){MockHttpServletRequest q=f.request(false);if(i==0)q.setCookies(new Cookie(DESKTOP,f.ticket+"."+String.join("",Collections.nCopies(64,"b"))));if(i==1)q.setCookies(new Cookie[0]);if(i==2){q.removeHeader("Origin");q.addHeader("Origin","https://evil.invalid");}
            MockHttpServletResponse r=new MockHttpServletResponse();f.poll(Collections.singletonMap("ticket",f.ticket),q,r);assertEquals(403,r.getStatus());}
        assertEquals(0,f.issued);assertTrue(f.poll().toString().contains("synthetic-test-token"));
    }
    @Test void crossOriginCreateAndCancelAreRejected() throws Exception {
        Fixture f=new Fixture();MockHttpServletRequest q=f.request(false);q.removeHeader("Origin");MockHttpServletResponse r=new MockHttpServletResponse();f.qr(q,r);assertEquals(403,r.getStatus());assertTrue(f.memory.data.isEmpty());
        f.begin();q=f.request(false);q.removeHeader("Origin");r=new MockHttpServletResponse();f.cancel(Collections.singletonMap("ticket",f.ticket),q,r);assertEquals(403,r.getStatus());assertTrue(f.poll().toString().contains("waiting"));
    }
    @Test void oauthReplayAndWrongPhoneCannotExchangeAgain() throws Exception {
        Fixture f=new Fixture();f.begin();f.scan();String proof=f.mobile;f.mobile="wrong";f.callback();assertEquals(0,f.exchanged);f.mobile=proof;f.callback();assertEquals(1,f.exchanged);f.callback();assertEquals(1,f.exchanged);
    }
    @Test void wrongConfirmationAndCrossOriginCannotApprove() throws Exception {
        Fixture f=new Fixture();f.begin();f.scan();f.callback();
        MockHttpServletResponse r=new MockHttpServletResponse();f.confirm(f.state,String.join("",Collections.nCopies(64,"b")),f.request(true),r);assertEquals(403,r.getStatus());
        MockHttpServletRequest q=f.request(true);q.removeHeader("Origin");r=new MockHttpServletResponse();f.confirm(f.state,f.confirm,q,r);assertEquals(403,r.getStatus());assertTrue(f.poll().toString().contains("waiting"));
    }
    @Test void expiredAndCancelledQrCannotAuthenticate() throws Exception {
        Fixture f=new Fixture();f.ready();f.clock+=300001;f.poll();assertEquals(0,f.issued);
        f=new Fixture();f.ready();f.cancel(Collections.singletonMap("ticket",f.ticket),f.request(false),new MockHttpServletResponse());f.poll();assertEquals(0,f.issued);
    }
    @Test void existingBindingAndCurrentNativeAdminPermissionsAreRequired() throws Exception {
        for(int i=0;i<6;i++){
            Fixture f=new Fixture();f.ready();if(i==0)f.user.setStatus("1");if(i==1)f.user.setDelFlag("2");if(i==2)f.granted=false;if(i==3)f.user.setUserId(1L);if(i==4)f.set("members",NativeWechatClient.hash("unbound")+"=jywx_test");if(i==5)f.user.setUserId(201L);
            f.poll();assertEquals(0,f.issued,"case "+i);
        }
    }
    @Test void unknownWeChatAndExchangeFailureNeverExposeSecretsOrConfirm() throws Exception {
        Fixture f=new Fixture();f.begin();f.scan();f.set("members",NativeWechatClient.hash("unbound")+"=jywx_test");f.callback();assertNull(f.confirm);f.poll();assertEquals(0,f.issued);
        f=new Fixture();f.begin();f.scan();f.failure=true;f.callback();assertNull(f.confirm);assertFalse(f.callback.getContentAsString().contains("SECRET_URL"));
    }
    @Test void scanMustBeWechatAndSingleAttemptIsBounded() throws Exception {
        Fixture f=new Fixture();f.begin();MockHttpServletRequest q=f.request(true);q.removeHeader("User-Agent");MockHttpServletResponse r=new MockHttpServletResponse();f.scan(f.ticket,q,r);assertNull(r.getRedirectedUrl());f.scan();
        r=new MockHttpServletResponse();f.scan(f.ticket,f.request(true),r);assertNull(r.getRedirectedUrl());
    }
    @Test void mobileSameDeviceGetsNativeResumeLinkAndConsumedAttemptCannotConfirmAgain() throws Exception {
        Fixture f=new Fixture();f.begin();f.scan();f.callback();MockHttpServletRequest q=f.request(true);
        q.setCookies(new Cookie(MOBILE,f.mobile),new Cookie(DESKTOP,f.desktop));MockHttpServletResponse r=new MockHttpServletResponse();
        f.confirm(f.state,f.confirm,q,r);assertTrue(r.getContentAsString().contains("/guanjia-login?ticket="+f.ticket));
        r=new MockHttpServletResponse();f.confirm(f.state,f.confirm,q,r);assertEquals(403,r.getStatus());
        assertTrue(f.poll().toString().contains("synthetic-test-token"));assertEquals(1,f.issued);
    }
    @Test void sameAppSubjectHashRemainsCompatibleWithExistingBindings() throws Exception {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",NativeWechatClient.hash("test"));
        assertEquals(SUBJECT,NativeWechatClient.subject(APP,"synthetic_openid"));
        assertNotEquals(SUBJECT,NativeWechatClient.hash(APP+"\0synthetic_openid"));
        assertNotEquals(NativeWechatClient.subject(APP,"synthetic_openid"),NativeWechatClient.subject("wxotheraccount","synthetic_openid"));
    }
    @Test void runtimeSpringBindingWorksAndOldBridgeExchangeIsGone() throws Exception {
        Fixture f=new Fixture();MockMvc mvc=MockMvcBuilders.standaloneSetup(f).build();
        MvcResult r=mvc.perform(post("/system/auth/guanjia/qr").header("Origin",ORIGIN)).andReturn();assertEquals(200,r.getResponse().getStatus());String id=JSONObject.parseObject(r.getResponse().getContentAsString()).getString("ticket");assertNotNull(id);
        r=mvc.perform(get("/system/auth/guanjia/scan").param("ticket",id)).andReturn();assertEquals(200,r.getResponse().getStatus());assertTrue(r.getResponse().getContentAsString().contains("请使用微信扫码"));
        r=mvc.perform(get("/system/auth/guanjia/callback")).andReturn();assertEquals(200,r.getResponse().getStatus());assertTrue(r.getResponse().getContentAsString().contains("微信登录未完成"));
        assertEquals(404,mvc.perform(post("/system/auth/guanjia/exchange").contentType("application/json").content("{\"assertion\":\"old-bridge\"}")).andReturn().getResponse().getStatus());
    }
    @Test void callbackFailuresReachOwnedDesktopWithoutMisleadingPermissionAdvice() throws Exception {
        Fixture f=new Fixture(); f.begin(); f.scan(); f.failure=true; f.callback();
        assertTrue(f.callback.getContentAsString().contains("WX-OAUTH-NETWORK"));
        assertFalse(f.callback.getContentAsString().contains("请确认使用已授权的管理员微信"));
        MockHttpServletResponse r=new MockHttpServletResponse();
        Object result=f.poll(Collections.singletonMap("ticket",f.ticket),f.request(false),r);
        assertEquals(403,r.getStatus()); assertTrue(result.toString().contains("WX-OAUTH-NETWORK")); assertEquals(0,f.issued);
        MockHttpServletRequest stranger=f.request(false);stranger.setCookies(new Cookie[0]);r=new MockHttpServletResponse();
        result=f.poll(Collections.singletonMap("ticket",f.ticket),stranger,r);
        assertFalse(result.toString().contains("WX-OAUTH-NETWORK"));
    }
    @Test void replayCannotInvalidatePendingConfirmation() throws Exception {
        Fixture f=new Fixture();f.begin();f.scan();f.callback();String original=f.confirm;f.callback();
        f.confirm(f.state,original,f.request(true),new MockHttpServletResponse());
        assertTrue(f.poll().toString().contains("synthetic-test-token"));assertEquals(1,f.exchanged);
    }
    @Test void diagnosticsAreAllowlistedAndNeverContainExceptionMessage() {
        assertEquals("WX-OAUTH-40029",failureReason("oauth",new NativeWechatClient.AuthorizationException(40029)));
        assertEquals("WX-BINDING",failureReason("binding",new IllegalArgumentException("SECRET_URL_SHOULD_NOT_APPEAR")));
        assertEquals("WX-SESSION",failureReason("session",new Exception("SECRET")));
        assertTrue(failureMessage("WX-OAUTH-40029").contains("凭证已失效"));
        assertFalse(failureMessage("untrusted <script>").contains("<script>"));
    }

}
