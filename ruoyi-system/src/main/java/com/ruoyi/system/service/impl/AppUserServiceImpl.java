package com.ruoyi.system.service.impl;

import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysAuthUser;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IAppUserService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.IWeChatMiniProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 小程序用户 Service
 */
@Service
public class AppUserServiceImpl implements IAppUserService
{
    private static final String DEFAULT_NICKNAME = "微信小程序用户";
    private static final String DEFAULT_AVATAR = "/profile/avatar/default.png";
    private static final int MAX_NICK_NAME_LENGTH = 30;
    private static final int MAX_AVATAR_LENGTH = 500;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private IWeChatMiniProgramService weChatMiniProgramService;

    @Override
    public int updateByMnp(SysUser user)
    {
        sanitizeProfileFields(user);
        return userMapper.updateUser(user);
    }

    @Override
    public SysUser completeWxProfile(Long userId, String phoneCode)
    {
        if (userId == null)
        {
            throw new ServiceException("用户未登录");
        }
        if (StringUtils.isEmpty(phoneCode))
        {
            throw new ServiceException("手机号授权码无效");
        }
        String phone = resolvePhoneNumber(userId, phoneCode);
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setPhonenumber(phone);
        applyWechatProfileDefaults(userId, user, phone);
        sanitizeProfileFields(user);
        userMapper.updateUser(user);
        return userService.selectUserById(userId);
    }

    private String resolvePhoneNumber(Long userId, String code)
    {
        String phone = weChatMiniProgramService.getPhoneNumber(code);
        SysUser current = userService.selectUserById(userId);
        SysUser checkUser = new SysUser();
        checkUser.setUserId(userId);
        checkUser.setPhonenumber(phone);
        if (StringUtils.isNotEmpty(phone) && !userService.checkPhoneUnique(checkUser))
        {
            if (current != null && phone.equals(current.getPhonenumber()))
            {
                return phone;
            }
            throw new ServiceException("该手机号已被其他账号绑定");
        }
        return phone;
    }

    private void applyWechatProfileDefaults(Long userId, SysUser user, String phone)
    {
        SysUser current = userService.selectUserById(userId);
        if (StringUtils.isEmpty(user.getNickName()))
        {
            user.setNickName(resolveNickName(userId, current, phone));
        }
        if (StringUtils.isEmpty(user.getAvatar()))
        {
            user.setAvatar(resolveAvatar(userId, current));
        }
    }

    private String resolveNickName(Long userId, SysUser current, String phone)
    {
        if (current != null && StringUtils.isNotEmpty(current.getNickName())
                && !DEFAULT_NICKNAME.equals(current.getNickName()))
        {
            return current.getNickName();
        }
        List<SysAuthUser> authList = userService.selectAuthUserListByUserId(userId);
        if (authList != null)
        {
            for (SysAuthUser auth : authList)
            {
                if (StringUtils.isNotEmpty(auth.getNickName()) && !DEFAULT_NICKNAME.equals(auth.getNickName()))
                {
                    return auth.getNickName();
                }
            }
        }
        if (StringUtils.isNotEmpty(phone) && phone.length() >= 4)
        {
            return "用户" + phone.substring(phone.length() - 4);
        }
        return "微信用户";
    }

    private String resolveAvatar(Long userId, SysUser current)
    {
        if (current != null && StringUtils.isNotEmpty(current.getAvatar()))
        {
            return current.getAvatar();
        }
        List<SysAuthUser> authList = userService.selectAuthUserListByUserId(userId);
        if (authList != null)
        {
            for (SysAuthUser auth : authList)
            {
                if (StringUtils.isNotEmpty(auth.getAvatar()))
                {
                    return auth.getAvatar();
                }
            }
        }
        return DEFAULT_AVATAR;
    }

    private void sanitizeProfileFields(SysUser user)
    {
        if (user == null)
        {
            return;
        }
        if (StringUtils.isNotEmpty(user.getNickName()))
        {
            String nickName = user.getNickName().trim();
            if (nickName.length() > MAX_NICK_NAME_LENGTH)
            {
                throw new ServiceException("昵称不能超过30个字符");
            }
            user.setNickName(nickName);
        }
        if (StringUtils.isNotEmpty(user.getAvatar()))
        {
            user.setAvatar(normalizeAvatar(user.getAvatar()));
        }
    }

    private String normalizeAvatar(String avatar)
    {
        String value = avatar.trim();
        int profileIndex = value.indexOf(Constants.RESOURCE_PREFIX);
        if (profileIndex >= 0)
        {
            value = value.substring(profileIndex);
        }
        if (value.length() > MAX_AVATAR_LENGTH)
        {
            throw new ServiceException("头像地址过长，请重新上传头像");
        }
        return value;
    }
}
