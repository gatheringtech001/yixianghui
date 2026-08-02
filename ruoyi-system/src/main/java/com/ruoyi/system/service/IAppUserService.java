package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 小程序用户 Service
 */
public interface IAppUserService
{
    int updateByMnp(SysUser user);

    /**
     * 微信一键授权：绑定手机号并自动补全昵称、头像
     */
    SysUser completeWxProfile(Long userId, String phoneCode);
}
