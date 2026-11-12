package com.ruoyi.web.controller.system;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/auth/guanjia")
public class GuanjiaLoginController {
    private static final String ORIGIN = "https://shzxj.lk01.cn";
    private static final String COOKIE = "jy_native_state";
    @Value("${guanjia.auth.secret:}") private String secret;
    @Value("${guanjia.auth.members:}") private String members;
    @Autowired private RedisCache cache;
    @Autowired private ISysUserService users;
    @Autowired private SysPermissionService permissions;
    @Autowired private SysLoginService login;
    @Autowired private TokenService tokens;

    private void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
    }
    private void cookie(HttpServletResponse response, String state, int age) {
        response.addHeader("Set-Cookie", COOKIE + "=" + state + "; Path=/api/system/auth/guanjia; Max-Age=" + age + "; Secure; HttpOnly; SameSite=Lax");
    }
    protected void auditLogin(String username) {
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, "聚英管家微信登录"));
    }
    @GetMapping("/start")
    public void start(HttpServletResponse response) throws IOException {
        headers(response);
        if (secret.length() < 64 || members.isEmpty()) { response.sendError(503, "微信登录尚未配置"); return; }
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        StringBuilder state = new StringBuilder();
        for (byte b : bytes) state.append(String.format("%02x", b & 255));
        cache.setCacheObject("guanjia:pending:" + state, true, 5, TimeUnit.MINUTES);
        cookie(response, state.toString(), 300);
        response.sendRedirect("https://gatheringtech.com/guanjia/api/native-auth/start?state=" + state);
    }
    @PostMapping("/exchange")
    public AjaxResult exchange(@RequestBody Map<String,String> input, HttpServletRequest request, HttpServletResponse response) {
        headers(response);
        if (!ORIGIN.equals(request.getHeader("Origin"))) { response.setStatus(403); return AjaxResult.error("请求来源无效"); }
        String state = null;
        if (request.getCookies() != null) for (Cookie item : request.getCookies()) if (COOKIE.equals(item.getName())) state = item.getValue();
        try {
            JSONObject identity = GuanjiaLoginAssertion.verify(input.get("assertion"), state, secret, members, System.currentTimeMillis() / 1000);
            if (!Boolean.TRUE.equals(cache.getCacheObject("guanjia:pending:" + state))) throw new IllegalArgumentException("登录已过期，请重新发起");
            SysUser user = users.selectUserByUserName(identity.getString("username"));
            if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag()) || user.isAdmin()) throw new IllegalArgumentException("此后台账号已停用或未开通");
            java.util.Set<String> granted = permissions.getMenuPermission(user);
            if (!granted.contains("*:*:*")) throw new IllegalArgumentException("此账号已不再具有后台管理员权限");
            login.loginPreCheck(user.getUserName(), "wechat-login");
            if (!cache.setCacheObjectIfAbsent("guanjia:used:" + state, true, 6, TimeUnit.MINUTES)) throw new IllegalArgumentException("此登录凭证已经使用");
            cache.deleteObject("guanjia:pending:" + state);
            String token = tokens.createToken(new LoginUser(user.getUserId(), user.getDeptId(), user, granted));
            login.recordLoginInfo(user.getUserId());
            auditLogin(user.getUserName());
            cookie(response, "", 0);
            return AjaxResult.success().put(Constants.TOKEN, token);
        } catch (RuntimeException e) {
            response.setStatus(403);
            return AjaxResult.error("微信登录未完成，请重新登录或联系管理员核对权限");
        }
    }
}
