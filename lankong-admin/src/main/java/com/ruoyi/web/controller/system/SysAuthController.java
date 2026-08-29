package com.ruoyi.web.controller.system;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.AppUserInviter;
import com.ruoyi.system.mapper.AppConsultantMapper;
import com.ruoyi.system.mapper.AppUserInviterMapper;
import com.ruoyi.system.service.IAppConsultantMnpService;
import io.swagger.annotations.ApiOperation;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthWechatMiniProgramRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.AuthUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.domain.SysAuthUser;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysUserService;
import me.zhyd.oauth.cache.AuthDefaultStateCache;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthRequest;
import me.zhyd.oauth.utils.AuthStateUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方认证授权处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/auth")
public class SysAuthController extends BaseController
{
    private AuthStateCache authStateCache;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private AppConsultantMapper consultantMapper;

    @Autowired
    private AppUserInviterMapper userInviterMapper;

    @Autowired
    private IAppConsultantMnpService consultantMnpService;

    @Value("${oauth.gitee.client-id:}")
    private String giteeClientId;

    @Value("${oauth.gitee.client-secret:}")
    private String giteeClientSecret;

    @Value("${oauth.gitee.redirect-uri:http://127.0.0.1:80/social-login?source=gitee}")
    private String giteeRedirectUri;

    @Value("${oauth.github.client-id:}")
    private String githubClientId;

    @Value("${oauth.github.client-secret:}")
    private String githubClientSecret;

    @Value("${oauth.github.redirect-uri:http://127.0.0.1:80/social-login?source=github}")
    private String githubRedirectUri;

    @Value("${wx.mnp.appId:${wx.pay.appId:}}")
    private String wechatMiniProgramAppId;

    @Value("${wx.mnp.appSecret:}")
    private String wechatMiniProgramAppSecret;

    private final Map<String, String> auths = new HashMap<String, String>();

    @PostConstruct
    private void initAuths()
    {
        auths.put("gitee", buildAuthConfig(giteeClientId, giteeClientSecret, giteeRedirectUri));
        auths.put("github", buildAuthConfig(githubClientId, githubClientSecret, githubRedirectUri));
        auths.put("wechat_mnp", buildAuthConfig(wechatMiniProgramAppId, wechatMiniProgramAppSecret, null));
        authStateCache = AuthDefaultStateCache.INSTANCE;
    }

    private String buildAuthConfig(String clientId, String clientSecret, String redirectUri)
    {
        if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret))
        {
            return null;
        }
        JSONObject config = new JSONObject();
        config.put("clientId", clientId);
        config.put("clientSecret", clientSecret);
        if (StringUtils.isNotEmpty(redirectUri))
        {
            config.put("redirectUri", redirectUri);
        }
        return config.toJSONString();
    }

    /**
     * 认证授权
     * 
     * @param source
     * @throws IOException
     */
    @GetMapping("/binding/{source}")
    @ResponseBody
    public AjaxResult authBinding(@PathVariable("source") String source, HttpServletRequest request) throws IOException
    {
        LoginUser tokenUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(tokenUser) && userMapper.checkAuthUser(tokenUser.getUserId(), source) > 0)
        {
            return error(source + "平台账号已经绑定");
        }

        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return error(source + "平台账号暂不支持");
        }
        JSONObject json = JSONObject.parseObject(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source, json.getString("clientId"), json.getString("clientSecret"), json.getString("redirectUri"), authStateCache);
        String authorizeUrl = authRequest.authorize(AuthStateUtils.createState());
        return success(authorizeUrl);
    }

    @SuppressWarnings("unchecked")
    @ApiOperation("授权登录接口")
    @RequestMapping("/social-login/{source}")
    public AjaxResult socialLogin(@PathVariable("source") String source, AuthCallback callback, HttpServletRequest request)
    {
        String obj = auths.get(source);
        if (StringUtils.isEmpty(obj))
        {
            return AjaxResult.error(10002, "第三方平台系统不支持或未提供来源");
        }
        JSONObject json = JSONObject.parseObject(obj);
        AuthRequest authRequest = AuthUtils.getAuthRequest(source, json.getString("clientId"), json.getString("clientSecret"), json.getString("redirectUri"), authStateCache);
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (response.ok())
        {
            String uuid = source + response.getData().getUuid();
            String unionId = resolveWechatUnionId(source, response.getData());
            if (StringUtils.isNotEmpty(unionId))
            {
                userMapper.updateWechatIdentityByUuid(uuid, unionId, wechatMiniProgramAppId);
            }
            LoginUser tokenUser = tokenService.getLoginUser(request);
            if (StringUtils.isNotNull(tokenUser))
            {
                SysUser user = userMapper.selectAuthUserByUuid(uuid);
                if (StringUtils.isNotNull(user))
                {
                    String token = tokenService.createToken(SecurityUtils.getLoginUser());
                    return success().put(Constants.TOKEN, token);
                }

                // 已经登录状态，直接绑定系统账号
                SysAuthUser authUser = new SysAuthUser();
                authUser.setAvatar(response.getData().getAvatar());
                authUser.setUuid(uuid);
                authUser.setUserId(SecurityUtils.getUserId());
                authUser.setUserName(response.getData().getUsername());
                authUser.setNickName(response.getData().getNickname());
                authUser.setEmail(response.getData().getEmail());
                authUser.setSource(source);
                applyWechatIdentity(authUser, unionId);
                userMapper.insertAuthUser(authUser);
                String token = tokenService.createToken(SecurityUtils.getLoginUser());
                return success().put(Constants.TOKEN, token);
            }
            SysUser authUser = userMapper.selectAuthUserByUuid(uuid);
            if (StringUtils.isNotNull(authUser))
            {
                SysUser user = userService.selectUserByUserName(authUser.getUserName());
                if (StringUtils.isNull(user))
                {
                    throw new ServiceException("登录用户不存在");
                }
                else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
                {
                    throw new ServiceException("对不起，您的账号：" + user.getUserName() + " 已被删除");
                }
                else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
                {
                    throw new ServiceException("对不起，您的账号：" + user.getUserName() + " 已停用");
                }
                LoginUser loginUser = new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
                Long parentUserId = resolveParentUserId(request);
                if (parentUserId != null)
                {
                    consultantMnpService.bindInviterIfAbsent(user.getUserId(), parentUserId);
                }
                String token = tokenService.createToken(loginUser);
                return success().put(Constants.TOKEN, token);
            }
            else
            {
                // todo 新用户授权登录
                SysUser newUser = new SysUser();
                newUser.setNickName("微信小程序用户");
                newUser.setUserName(source + "_" +response.getData().getUuid());
                if (!userService.checkUserNameUnique(newUser))
                {
                    return error("微信用户创建失败，登录账号已存在");
                }
                else if (StringUtils.isNotEmpty(newUser.getPhonenumber()) && !userService.checkPhoneUnique(newUser))
                {
                    return error("微信用户创建失败，手机号码已存在");
                }
                newUser.setCreateBy(source);
                // todo 该默认密码不允许直接登录
                newUser.setPassword(SecurityUtils.encryptPassword("123456"));
                newUser.setRoleIds(new Long[]{102L});  // todo 小程序用户默认普通角色
                newUser.setDeptId(100L); // todo 默认归属到默认用户
                userService.insertUser(newUser);
                SysUser sessionUser = userService.selectUserByUserName(newUser.getUserName());
                if (StringUtils.isNull(sessionUser))
                {
                    throw new ServiceException("小程序用户创建后读取失败");
                }

                // 绑定第三方系统账号
                SysAuthUser newAuthUser = new SysAuthUser();
                newAuthUser.setAvatar(response.getData().getAvatar());
                newAuthUser.setUuid(uuid);
                newAuthUser.setUserId(sessionUser.getUserId());
                newAuthUser.setUserName(sessionUser.getUserName());
                newAuthUser.setNickName(sessionUser.getNickName());
//                newAuthUser.setEmail(response.getData().getEmail());
                newAuthUser.setSource(source);
                applyWechatIdentity(newAuthUser, unionId);
                userMapper.insertAuthUser(newAuthUser);
                Long parentUserId = resolveParentUserId(request);
                if (parentUserId != null)
                {
                    consultantMnpService.bindInviterIfAbsent(sessionUser.getUserId(), parentUserId);
                }

                LoginUser loginUser = new LoginUser(sessionUser.getUserId(), sessionUser.getDeptId(), sessionUser, permissionService.getMenuPermission(sessionUser));
                String token = tokenService.createToken(loginUser);
                return success().put(Constants.TOKEN, token);
                // return AjaxResult.error(10002, "对不起，您没有绑定注册用户，请先注册后在个人中心绑定第三方授权信息！");
            }
        }
        return AjaxResult.error(10002, "对不起，授权信息验证不通过，请联系管理员");
    }

    /**
     * 取消授权
     */
    @DeleteMapping(value = "/unlock/{authId}")
    public AjaxResult unlockAuth(@PathVariable Long authId)
    {
        return toAjax(userMapper.deleteAuthUser(authId));
    }

    @GetMapping("/wxmnp/login")
    @ResponseBody
    public AjaxResult wxmnpLogin(String code, Integer parentUserId) throws IOException {
        if (StringUtils.isEmpty(code)) {
            return error("无效code");
        }

        AuthRequest authRequest = new AuthWechatMiniProgramRequest(AuthConfig.builder()
                .clientId(wechatMiniProgramAppId)
                .clientSecret(wechatMiniProgramAppSecret)
                .ignoreCheckRedirectUri(true)
                .ignoreCheckState(true)
                .build());
//        String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId +
//                "&secret=" + appSecret +
//                "&js_code=" + code +
//                "&grant_type=authorization_code";
//        RestTemplate restTemplate = new RestTemplate();
//        String response = restTemplate.getForObject(url, String.class);
//        System.out.println(response);
//        // 解析返回的JSON，获取openid和session_key等数据
//        // 这里可以根据实际情况进行解析和进一步处理，例如存储用户信息等。
//        JSONObject json = JSONObject.parseObject(response);
//        //获取微信的OpenId
//        String openid = json.getString("openid");
//        String sessionkey = json.getString("session_key");
//
//        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper();
//        lambdaQueryWrapper.eq(User::getOpenId, openid);
//        User user = userMapper.selectOne(lambdaQueryWrapper); //查询数据库 查询用户信息
//        WxLoginVO wxLoginVO = new WxLoginVO();
//        UserLoginVO userLoginVO = new UserLoginVO();
//        if (user == null) {
//            //创建用户
//            User user1 = new User(); //用户信息
//            user1.setIsAdministrator(0); //系统用户 （用户都是普通用户）
//            user1.setNickName(RandomGenerate.userName()); //用户名称 随机
//            String pwd = DigestUtil.md5Hex("123456"); //默认密码：123456
//            user1.setLoginKey(pwd);
//            user1.setUserName("11111111111");//登录账号
//            user1.setPhone("11111111111");//手机号
//            // user1.setRoleAid(3);
//            user1.setOpenId(openid); //第三方平台用户唯一ID
//            user1.setFromMethod("微信小程序");
//            if (fatherUserAid != null) {
//                user1.setFatherUserAid(fatherUserAid);
//            }
//            userMapper.insert(user1);
//            if (user1.getAid() != null) {
//                UserToUserProportion userToUserProportion = new UserToUserProportion();
//                userToUserProportion.setCurrentUserAid(user1.getAid());
//                userToUserProportion.setFatherUserAid(fatherUserAid);
//                userToUserProportionMapper.insert(userToUserProportion);
//            }
//            //登录认证
//            StpUtil.setLoginId(user1.getAid(), "wx");
//            // 获取当前会话的token值
//            String token = StpUtil.getTokenValue();
//            wxLoginVO.setToken(token);
//            wxLoginVO.setSessionkey(sessionkey);
//            wxLoginVO.setAid(user1.getAid());
//            wxLoginVO.setUserType(userTypeMapper.selectById(2));
//            wxLoginVO.setFatherUserAid(fatherUserAid);
//            return ResponseWrapper.markCustomSuccess("请授权", wxLoginVO);
//        }
//        //登录认证
//        StpUtil.setLoginId(user.getAid(), "wx");
//        // 获取当前会话的token值
//        String token = StpUtil.getTokenValue();
//        userLoginVO.setToken(token);
//        BeanUtil.copyProperties(user, userLoginVO);
//        userLoginVO.setUserType(userTypeMapper.selectById(user.getTypeAid()));
//        return ResponseWrapper.markSuccess(userLoginVO);
        return null; // 或者返回处理后的数据，比如用户的session信息等。
    }

    private Long resolveParentUserId(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }
        String raw = request.getParameter("parentUserId");
        if (StringUtils.isEmpty(raw))
        {
            raw = request.getParameter("parentId");
        }
        if (StringUtils.isEmpty(raw))
        {
            raw = request.getParameter("parent_id");
        }
        if (StringUtils.isEmpty(raw))
        {
            return null;
        }
        try
        {
            return Long.parseLong(raw.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private String resolveWechatUnionId(String source, AuthUser authUser)
    {
        if (!"wechat_mnp".equals(source) || authUser == null || authUser.getToken() == null)
        {
            return null;
        }
        return authUser.getToken().getUnionId();
    }

    private void applyWechatIdentity(SysAuthUser authUser, String unionId)
    {
        if (StringUtils.isNotEmpty(unionId))
        {
            authUser.setUnionId(unionId);
            authUser.setAppId(wechatMiniProgramAppId);
        }
    }
}
