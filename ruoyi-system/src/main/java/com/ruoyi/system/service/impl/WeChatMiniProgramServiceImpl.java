package com.ruoyi.system.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.system.service.IWeChatMiniProgramService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 微信小程序接口调用
 */
@Service
public class WeChatMiniProgramServiceImpl implements IWeChatMiniProgramService
{
    private static final Logger log = LoggerFactory.getLogger(WeChatMiniProgramServiceImpl.class);

    @Value("${wx.mnp.appId:${wx.pay.appId:}}")
    private String appId;

    @Value("${wx.mnp.appSecret:}")
    private String appSecret;

    private String cachedAccessToken;
    private long accessTokenExpireAt;

    @Override
    public String getPhoneNumber(String phoneCode)
    {
        if (StringUtils.isEmpty(phoneCode))
        {
            throw new ServiceException("手机号授权码无效");
        }
        String accessToken = getAccessToken();
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.put("code", phoneCode);
        String response = HttpUtils.sendPost(url, body.toJSONString(), "application/json;charset=UTF-8");
        JSONObject json = JSONObject.parseObject(response);
        if (json == null)
        {
            throw new ServiceException("获取手机号失败：微信无响应");
        }
        int errcode = json.getIntValue("errcode");
        if (errcode != 0)
        {
            log.error("微信获取手机号失败: {}", response);
            throw new ServiceException("获取手机号失败：" + json.getString("errmsg"));
        }
        JSONObject phoneInfo = json.getJSONObject("phone_info");
        if (phoneInfo == null || StringUtils.isEmpty(phoneInfo.getString("phoneNumber")))
        {
            throw new ServiceException("获取手机号失败：号码为空");
        }
        return phoneInfo.getString("phoneNumber");
    }

    @Override
    public String createInviteQrcode(Long userId)
    {
        if (userId == null)
        {
            throw new ServiceException("用户无效");
        }
        String accessToken = getAccessToken();
        String apiUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;
        JSONObject body = new JSONObject();
        body.put("scene", "u" + userId);
        body.put("page", "pages/home/home");
        body.put("width", 430);
        body.put("check_path", false);
        byte[] imageBytes = postForBytes(apiUrl, body.toJSONString());
        if (imageBytes == null || imageBytes.length == 0)
        {
            throw new ServiceException("生成邀请码失败");
        }
        if (imageBytes.length < 200 && imageBytes[0] == '{')
        {
            JSONObject err = JSONObject.parseObject(new String(imageBytes, StandardCharsets.UTF_8));
            throw new ServiceException("生成邀请码失败：" + err.getString("errmsg"));
        }
        try
        {
            String fileName = "invite_" + userId + ".png";
            String absPath = FileUploadUtils.getAbsoluteFile(RuoYiConfig.getUploadPath(), fileName).getAbsolutePath();
            java.io.FileOutputStream fos = new java.io.FileOutputStream(absPath);
            fos.write(imageBytes);
            fos.close();
            return FileUploadUtils.getPathFileName(RuoYiConfig.getUploadPath(), fileName);
        }
        catch (IOException e)
        {
            log.error("保存邀请小程序码失败", e);
            throw new ServiceException("保存邀请码失败");
        }
    }

    private byte[] postForBytes(String url, String jsonBody)
    {
        HttpURLConnection connection = null;
        try
        {
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            try (OutputStream os = connection.getOutputStream())
            {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
            try (InputStream is = connection.getInputStream();
                 ByteArrayOutputStream bos = new ByteArrayOutputStream())
            {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1)
                {
                    bos.write(buffer, 0, len);
                }
                return bos.toByteArray();
            }
        }
        catch (IOException e)
        {
            // URL 查询参数包含令牌或 AppSecret，异常信息也可能包含完整 URL。
            log.error("微信接口请求失败: {}", e.getClass().getSimpleName());
            throw new ServiceException("微信接口请求失败");
        }
        finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private synchronized String getAccessToken()
    {
        if (StringUtils.isNotEmpty(cachedAccessToken) && System.currentTimeMillis() < accessTokenExpireAt)
        {
            return cachedAccessToken;
        }
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(appSecret))
        {
            throw new ServiceException("小程序 AppId/AppSecret 未配置");
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + appId + "&secret=" + appSecret;
        String response = HttpUtils.sendGet(url);
        JSONObject json = JSONObject.parseObject(response);
        if (json == null || StringUtils.isEmpty(json.getString("access_token")))
        {
            log.error("获取微信 access_token 失败: {}", response);
            throw new ServiceException("获取微信凭证失败");
        }
        cachedAccessToken = json.getString("access_token");
        int expiresIn = json.getIntValue("expires_in");
        accessTokenExpireAt = System.currentTimeMillis() + Math.max(expiresIn - 300, 60) * 1000L;
        return cachedAccessToken;
    }
}
