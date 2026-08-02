package com.ruoyi.system.service;

/**
 * 微信小程序服务端能力
 */
public interface IWeChatMiniProgramService
{
    /**
     * 通过 getPhoneNumber 返回的 code 获取用户手机号
     */
    String getPhoneNumber(String phoneCode);

    /**
     * 生成顾问邀请小程序码并返回可访问路径
     */
    String createInviteQrcode(Long userId);
}
