package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppUserBank;

/**
 * 用户银行卡Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppUserBankMapper 
{
    /**
     * 查询用户银行卡
     * 
     * @param bankId 用户银行卡主键
     * @return 用户银行卡
     */
    public AppUserBank selectAppUserBankByBankId(Long bankId);

    /**
     * 查询用户银行卡列表
     * 
     * @param appUserBank 用户银行卡
     * @return 用户银行卡集合
     */
    public List<AppUserBank> selectAppUserBankList(AppUserBank appUserBank);

    /**
     * 新增用户银行卡
     * 
     * @param appUserBank 用户银行卡
     * @return 结果
     */
    public int insertAppUserBank(AppUserBank appUserBank);

    /**
     * 修改用户银行卡
     * 
     * @param appUserBank 用户银行卡
     * @return 结果
     */
    public int updateAppUserBank(AppUserBank appUserBank);

    /**
     * 删除用户银行卡
     * 
     * @param bankId 用户银行卡主键
     * @return 结果
     */
    public int deleteAppUserBankByBankId(Long bankId);

    /**
     * 批量删除用户银行卡
     * 
     * @param bankIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserBankByBankIds(Long[] bankIds);
}
