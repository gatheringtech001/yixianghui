package com.ruoyi.web.controller.system;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.servlet.http.Cookie;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.web.service.*;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuanjiaLoginControllerTest {
    static class Cache extends RedisCache {
        final Map<String,Object> data = new ConcurrentHashMap<>();
        @Override public <T> T getCacheObject(String key) { return (T) data.get(key); }
        @Override public <T> void setCacheObject(String key,T value,Integer timeout,TimeUnit unit) { data.put(key,value); }
        @Override public <T> boolean setCacheObjectIfAbsent(String key,T value,long timeout,TimeUnit unit) { return data.putIfAbsent(key,value)==null; }
        @Override public boolean deleteObject(String key) { return data.remove(key)!=null; }
    }
    static class Fixture extends GuanjiaLoginController {
        Cache memory=new Cache(); SysUser account=new SysUser(); int issued=0; boolean granted=true;
        Fixture() throws Exception {
            account.setUserId(200L);account.setDeptId(100L);account.setUserName("jywx_test");account.setStatus("0");account.setDelFlag("0");
            set("secret",GuanjiaLoginAssertionTest.SECRET);set("members",GuanjiaLoginAssertionTest.MEMBERS);set("cache",memory);
            set("users",Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ISysUserService.class},(p,m,a)->account));
            set("permissions",new SysPermissionService(){@Override public Set<String> getMenuPermission(SysUser u){return granted?Collections.singleton("*:*:*"):Collections.emptySet();}});
            set("login",new SysLoginService(){@Override public void loginPreCheck(String u,String p){} @Override public void recordLoginInfo(Long id){}});
            set("tokens",new TokenService(){@Override public String createToken(LoginUser u){issued++;assertEquals("jywx_test",u.getUsername());return "synthetic-test-token";}});
        }
        void set(String name,Object value) throws Exception { Field f=GuanjiaLoginController.class.getDeclaredField(name);f.setAccessible(true);f.set(this,value); }
        @Override protected void auditLogin(String username) { }
        MockHttpServletRequest request(){MockHttpServletRequest r=new MockHttpServletRequest();r.addHeader("Origin","https://shzxj.lk01.cn");r.setCookies(new Cookie("jy_native_state",GuanjiaLoginAssertionTest.STATE));return r;}
        Map<String,String> input() throws Exception {JSONObject v=GuanjiaLoginAssertionTest.claim();long now=System.currentTimeMillis()/1000;v.put("iat",now);v.put("exp",now+60);return Collections.singletonMap("assertion",GuanjiaLoginAssertionTest.sign(v));}
        void pending(){memory.data.put("guanjia:pending:"+GuanjiaLoginAssertionTest.STATE,true);}
    }
    @Test void startSetsBrowserProofAndRedirectsOnlyToTrustedGuanjia() throws Exception {
        Fixture f=new Fixture();MockHttpServletResponse r=new MockHttpServletResponse();f.start(r);
        assertTrue(r.getRedirectedUrl().startsWith("https://gatheringtech.com/guanjia/api/native-auth/start?state="));
        assertTrue(r.getHeader("Set-Cookie").contains("Secure; HttpOnly; SameSite=Lax"));assertEquals("no-store",r.getHeader("Cache-Control"));
        assertEquals(1,f.memory.data.size());
    }
    @Test void successConsumesProofAndReplayCannotMintAnotherToken() throws Exception {
        Fixture f=new Fixture();f.pending();Map<String,String> input=f.input();
        assertEquals("synthetic-test-token",f.exchange(input,f.request(),new MockHttpServletResponse()).get("token"));
        MockHttpServletResponse again=new MockHttpServletResponse();f.exchange(input,f.request(),again);assertEquals(403,again.getStatus());assertEquals(1,f.issued);
    }
    @Test void wrongBrowserMissingStateAndCrossOriginAreRejectedBeforeIssuingTokens() throws Exception {
        Fixture f=new Fixture();f.pending();Map<String,String> input=f.input();
        for(int i=0;i<3;i++){MockHttpServletRequest request=f.request();if(i==0){request.removeHeader("Origin");request.addHeader("Origin","https://evil.invalid");}if(i==1)request.setCookies(new Cookie("jy_native_state","b"));if(i==2)request.setCookies(new Cookie[0]);
            MockHttpServletResponse r=new MockHttpServletResponse();f.exchange(input,request,r);assertEquals(403,r.getStatus());}
        assertEquals(0,f.issued);
    }
    @Test void disabledDeletedDemotedAndBuiltinAccountsCannotUseBridge() throws Exception {
        Fixture f=new Fixture();f.pending();Map<String,String> input=f.input();
        for(int i=0;i<4;i++){f.account.setStatus(i==0?"1":"0");f.account.setDelFlag(i==1?"2":"0");f.granted=i!=2;f.account.setUserId(i==3?1L:200L);
            MockHttpServletResponse r=new MockHttpServletResponse();f.exchange(input,f.request(),r);assertEquals(403,r.getStatus());}
        assertEquals(0,f.issued);
    }
}
