package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppUserBankMapper;
import com.ruoyi.system.domain.AppUserBank;
import com.ruoyi.system.service.IAppUserBankService;

/**
 * 用户银行卡Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppUserBankServiceImpl implements IAppUserBankService 
{
    @Autowired
    private AppUserBankMapper appUserBankMapper;

    /**
     * 查询用户银行卡
     * 
     * @param bankId 用户银行卡主键
     * @return 用户银行卡
     */
    @Override
    public AppUserBank selectAppUserBankByBankId(Long bankId)
    {
        return appUserBankMapper.selectAppUserBankByBankId(bankId);
    }

    /**
     * 查询用户银行卡列表
     * 
     * @param appUserBank 用户银行卡
     * @return 用户银行卡
     */
    @Override
    public List<AppUserBank> selectAppUserBankList(AppUserBank appUserBank)
    {
        return appUserBankMapper.selectAppUserBankList(appUserBank);
    }

    /**
     * 新增用户银行卡
     * 
     * @param appUserBank 用户银行卡
     * @return 结果
     */
    @Override
    public int insertAppUserBank(AppUserBank appUserBank)
    {
        appUserBank.setCreateTime(DateUtils.getNowDate());
        return appUserBankMapper.insertAppUserBank(appUserBank);
    }

    /**
     * 修改用户银行卡
     * 
     * @param appUserBank 用户银行卡
     * @return 结果
     */
    @Override
    public int updateAppUserBank(AppUserBank appUserBank)
    {
        return appUserBankMapper.updateAppUserBank(appUserBank);
    }

    /**
     * 批量删除用户银行卡
     * 
     * @param bankIds 需要删除的用户银行卡主键
     * @return 结果
     */
    @Override
    public int deleteAppUserBankByBankIds(Long[] bankIds)
    {
        return appUserBankMapper.deleteAppUserBankByBankIds(bankIds);
    }

    /**
     * 删除用户银行卡信息
     * 
     * @param bankId 用户银行卡主键
     * @return 结果
     */
    @Override
    public int deleteAppUserBankByBankId(Long bankId)
    {
        return appUserBankMapper.deleteAppUserBankByBankId(bankId);
    }
}
