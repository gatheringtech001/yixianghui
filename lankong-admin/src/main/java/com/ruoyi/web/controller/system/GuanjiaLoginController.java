package com.ruoyi.web.controller.system;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.*;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.*;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.*;
import org.springframework.web.bind.annotation.*;

/** Native QR login. All state, OAuth and administrator checks stay on this server. */
@RestController
@RequestMapping("/system/auth/guanjia")
public class GuanjiaLoginController {
    static final String ORIGIN = "https://shzxj.lk01.cn";
    static final String PATH = "/api/system/auth/guanjia";
    static final String DESKTOP = "jy_native_qr", MOBILE = "jy_native_mobile";
    private static final SecureRandom RANDOM = new SecureRandom();
    @Value("${native.wechat.app-id:}") private String appId;
    @Value("${native.wechat.app-secret:}") private String appSecret;
    // Same AppID + SHA256(AppID + NUL + OpenID) bindings as the existing administrator identities.
    @Value("${native.wechat.members:}") private String members;
    @Autowired private RedisCache cache;
    @Autowired private ISysUserService users;
    @Autowired private SysPermissionService permissions;
    @Autowired private SysLoginService login;
    @Autowired private TokenService tokens;
    @Autowired private NativeWechatClient wechat;

    protected long now() { return System.currentTimeMillis(); }
    private static String random() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format("%02x", b & 255)); return result.toString();
    }
    private static boolean valid(String value) { return value != null && value.matches("[a-f0-9]{64}"); }
    private static String key(String kind, String id) { return "nativewx:" + kind + ":" + NativeWechatClient.hash(id); }
    private static String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null) for (Cookie c : request.getCookies()) if (name.equals(c.getName())) return c.getValue();
        return "";
    }
    private void cookie(HttpServletResponse response, String name, String value, int age) {
        response.addHeader("Set-Cookie", name + "=" + value + "; Path=" + PATH + "; Max-Age=" + age + "; Secure; HttpOnly; SameSite=Lax");
    }
    private void headers(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
    }
    private void configured() {
        if (appId == null || !appId.matches("wx[A-Za-z0-9]{8,40}") || appSecret == null || appSecret.isEmpty()
                || members == null || members.isEmpty()) throw new IllegalStateException("微信登录尚未配置");
    }
    private void origin(HttpServletRequest request) {
        if (!ORIGIN.equals(request.getHeader("Origin"))) throw new IllegalArgumentException("请求来源无效");
    }
    private JSONObject read(String kind, String id) {
        if (!valid(id)) throw new IllegalArgumentException("登录已失效，请重新扫码");
        JSONObject value = cache.getCacheObject(key(kind, id));
        if (value == null || value.getLongValue("expires") <= now()) throw new IllegalArgumentException("登录已失效，请重新扫码");
        return value;
    }
    private void save(String kind, String id, JSONObject value) {
        long remaining = value.getLongValue("expires") - now();
        if (remaining <= 0) throw new IllegalArgumentException("登录已失效，请重新扫码");
        cache.setCacheObject(key(kind, id), value, (int) Math.max(1, (remaining + 999) / 1000), TimeUnit.SECONDS);
    }
    private void once(String kind, String id) {
        if (!cache.setCacheObjectIfAbsent(key(kind, id), true, 6, TimeUnit.MINUTES)) throw new IllegalArgumentException("登录凭证已使用");
    }
    private SysUser account(String subject) {
        String username = null;
        if (!valid(subject)) throw new IllegalArgumentException("此微信未开通后台权限");
        for (String entry : members.split(",")) {
            String[] parts = entry.trim().split("=", -1);
            if (parts.length == 2 && subject.equals(parts[0]) && parts[1].matches("jywx_[a-z0-9_]{3,48}")) {
                if (username != null) throw new IllegalArgumentException("微信绑定配置重复");
                username = parts[1];
            }
        }
        if (username == null) throw new IllegalArgumentException("此微信未开通后台权限");
        SysUser user = users.selectUserByUserName(username);
        if (user == null || user.isAdmin() || !username.equals(user.getUserName())
                || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag())
                || !permissions.getMenuPermission(user).contains("*:*:*")) throw new IllegalArgumentException("此账号已停用或没有管理员权限");
        return user;
    }
    private JSONObject desktop(String id, HttpServletRequest request) {
        JSONObject ticket = read("ticket", id);
        String[] proof = cookie(request, DESKTOP).split("\\.", -1);
        if (proof.length != 2 || !id.equals(proof[0]) || !valid(proof[1])
                || !NativeWechatClient.hash(proof[1]).equals(ticket.getString("browser"))) throw new IllegalArgumentException("请在发起扫码的浏览器中继续");
        return ticket;
    }
    private JSONObject mobile(String state, HttpServletRequest request) {
        JSONObject attempt = read("attempt", state);
        String proof = cookie(request, MOBILE);
        if (!valid(proof) || !NativeWechatClient.hash(proof).equals(attempt.getString("browser"))) throw new IllegalArgumentException("请重新使用微信扫码");
        return attempt;
    }
    protected void auditLogin(String username) {
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, "原生后台微信登录"));
    }
    @GetMapping("/start")
    public void start(HttpServletResponse response) throws IOException {
        headers(response); response.sendRedirect(ORIGIN + "/guanjia-login");
    }
    @PostMapping("/qr")
    public AjaxResult qr(HttpServletRequest request, HttpServletResponse response) {
        headers(response);
        try {
            origin(request); configured();
            if (!cache.setCacheObjectIfAbsent(key("rate", String.valueOf(request.getRemoteAddr())), true, 2, TimeUnit.SECONDS)) {
                response.setStatus(429); return AjaxResult.error("操作较快，请稍后刷新二维码");
            }
            // Invalidate only the previous ticket owned by this browser.
            String previous = cookie(request, DESKTOP).split("\\.", -1)[0];
            if (valid(previous)) try { desktop(previous, request); cache.deleteObject(key("ticket", previous)); } catch (RuntimeException ignored) { }
            String id = random(), proof = random(); JSONObject ticket = new JSONObject();
            ticket.put("browser", NativeWechatClient.hash(proof)); ticket.put("expires", now() + 300000); ticket.put("status", "waiting");
            save("ticket", id, ticket); cookie(response, DESKTOP, id + "." + proof, 300);
            return AjaxResult.success().put("ticket", id).put("expires", ticket.getLongValue("expires"))
                    .put("url", ORIGIN + PATH + "/scan?ticket=" + id);
        } catch (RuntimeException e) {
            response.setStatus(403); return AjaxResult.error("微信登录暂不可用，请使用账号密码登录");
        }
    }
    @GetMapping("/scan")
    public void scan(@RequestParam(name="ticket") String id, HttpServletRequest request, HttpServletResponse response) throws IOException {
        headers(response);
        try {
            configured(); JSONObject ticket = read("ticket", id);
            if (!"waiting".equals(ticket.getString("status"))) throw new IllegalArgumentException("二维码已使用，请刷新");
            if (!String.valueOf(request.getHeader("User-Agent")).contains("MicroMessenger")) {
                page(response, "请使用微信扫码", "<p>请用手机微信扫描电脑上的二维码。</p>"); return;
            }
            // One in-flight OAuth attempt per QR; repeated scans cannot allocate unlimited Redis state.
            once("scanned", id);
            String state = random(), proof = random(); JSONObject attempt = new JSONObject();
            attempt.put("ticket", id); attempt.put("browser", NativeWechatClient.hash(proof));
            attempt.put("expires", ticket.getLongValue("expires")); attempt.put("phase", "oauth");
            save("attempt", state, attempt); cookie(response, MOBILE, proof, 300);
            response.setHeader("Referrer-Policy", "no-referrer");
            response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + NativeWechatClient.encode(appId)
                    + "&redirect_uri=" + NativeWechatClient.encode(ORIGIN + PATH + "/callback")
                    + "&response_type=code&scope=snsapi_base&state=" + state + "#wechat_redirect");
        } catch (RuntimeException e) { page(response, "二维码已失效", "<p>请在电脑上刷新二维码后重新扫码。</p>"); }
    }
    @GetMapping("/callback")
    public void callback(@RequestParam(name="state", required=false) String state, @RequestParam(name="code", required=false) String code,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        headers(response);
        try {
            configured(); JSONObject attempt = mobile(state, request); JSONObject ticket = read("ticket", attempt.getString("ticket"));
            if (!"oauth".equals(attempt.getString("phase")) || !"waiting".equals(ticket.getString("status"))
                    || code == null || !code.matches("[A-Za-z0-9_-]{1,512}")) throw new IllegalArgumentException();
            once("callback", state);
            String subject = wechat.exchange(appId, appSecret, code); account(subject);
            String confirm = random(); attempt.put("subject", subject); attempt.put("phase", "confirm");
            attempt.put("confirm", NativeWechatClient.hash(confirm)); save("attempt", state, attempt);
            page(response, "确认登录管理后台", "<p>仅在你本人发起登录时确认。</p><form method=\"post\" action=\"" + PATH + "/confirm\">"
                    + "<input type=\"hidden\" name=\"state\" value=\"" + state + "\"><input type=\"hidden\" name=\"confirm\" value=\"" + confirm
                    + "\"><button type=\"submit\">确认登录</button></form>");
        } catch (Exception e) {
            // Exceptions may contain OAuth URLs/secrets; never log or echo them.
            page(response, "微信登录未完成", "<p>请确认使用已授权的管理员微信，并在电脑上刷新二维码重试。</p>");
        }
    }
    @PostMapping("/confirm")
    public void confirm(@RequestParam(name="state") String state, @RequestParam(name="confirm") String confirm,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        headers(response);
        try {
            origin(request); configured(); JSONObject attempt = mobile(state, request);
            String id = attempt.getString("ticket"); JSONObject ticket = read("ticket", id);
            if (!"confirm".equals(attempt.getString("phase")) || !"waiting".equals(ticket.getString("status"))
                    || !valid(confirm) || !NativeWechatClient.hash(confirm).equals(attempt.getString("confirm"))) throw new IllegalArgumentException();
            SysUser user = account(attempt.getString("subject"));
            once("confirmed", id); ticket.put("subject", attempt.getString("subject")); ticket.put("userId", user.getUserId());
            ticket.put("status", "confirmed"); save("ticket", id, ticket); cache.deleteObject(key("attempt", state));
            cookie(response, MOBILE, "", 0);
            boolean sameDevice = false;
            try { desktop(id, request); sameDevice = true; } catch (RuntimeException ignored) { }
            page(response, "已确认登录", sameDevice ? "<p><a href=\"/guanjia-login?ticket=" + id + "\">进入管理后台</a></p>" : "<p>请返回电脑继续操作。</p>");
        } catch (RuntimeException e) { response.setStatus(403); page(response, "确认已失效", "<p>请在电脑上刷新二维码后重试。</p>"); }
    }
    @PostMapping("/poll")
    public AjaxResult poll(@RequestBody Map<String,String> input, HttpServletRequest request, HttpServletResponse response) {
        headers(response);
        try {
            origin(request); configured(); String id = input.get("ticket"); JSONObject ticket = desktop(id, request);
            if (!"confirmed".equals(ticket.getString("status"))) return AjaxResult.success().put("status", "waiting");
            SysUser user = account(ticket.getString("subject"));
            if (!user.getUserId().equals(ticket.getLong("userId"))) throw new IllegalArgumentException();
            login.loginPreCheck(user.getUserName(), "wechat-login");
            once("used", id); cache.deleteObject(key("ticket", id));
            String token = tokens.createToken(new LoginUser(user.getUserId(), user.getDeptId(), user, permissions.getMenuPermission(user)));
            login.recordLoginInfo(user.getUserId()); auditLogin(user.getUserName()); cookie(response, DESKTOP, "", 0);
            return AjaxResult.success().put("status", "signed-in").put(Constants.TOKEN, token);
        } catch (RuntimeException e) { response.setStatus(403); return AjaxResult.error("登录已失效或权限已变更，请刷新二维码"); }
    }
    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody Map<String,String> input, HttpServletRequest request, HttpServletResponse response) {
        headers(response);
        try {
            origin(request); String id = input.get("ticket"); desktop(id, request); cache.deleteObject(key("ticket", id));
            cookie(response, DESKTOP, "", 0); return AjaxResult.success();
        } catch (RuntimeException e) { response.setStatus(403); return AjaxResult.error("登录已失效"); }
    }
    private void page(HttpServletResponse response, String title, String body) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"
                + title + "</title><style>body{font:16px -apple-system,sans-serif;margin:0;padding:64px 24px;color:#232735;text-align:center}main{max-width:360px;margin:auto}h1{font-size:24px}p{line-height:1.8;color:#626b7b}button{background:#07a960;color:#fff;border:0;border-radius:10px;width:100%;padding:15px;font-size:16px}</style></head><body><main><h1>"
                + title + "</h1>" + body + "</main></body></html>");
    }
}
