package com.ruoyi.system.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AppUserCash;

/**
 * 用户提现Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppUserCashMapper 
{
    /**
     * 查询用户提现
     * 
     * @param cashId 用户提现主键
     * @return 用户提现
     */
    public AppUserCash selectAppUserCashByCashId(Long cashId);

    /**
     * 查询用户提现列表
     * 
     * @param appUserCash 用户提现
     * @return 用户提现集合
     */
    public List<AppUserCash> selectAppUserCashList(AppUserCash appUserCash);

    /**
     * 新增用户提现
     * 
     * @param appUserCash 用户提现
     * @return 结果
     */
    public int insertAppUserCash(AppUserCash appUserCash);

    /**
     * 修改用户提现
     * 
     * @param appUserCash 用户提现
     * @return 结果
     */
    public int updateAppUserCash(AppUserCash appUserCash);

    /**
     * 删除用户提现
     * 
     * @param cashId 用户提现主键
     * @return 结果
     */
    public int deleteAppUserCashByCashId(Long cashId);

    /**
     * 批量删除用户提现
     * 
     * @param cashIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppUserCashByCashIds(Long[] cashIds);

    BigDecimal sumWithdrawnAmount(@Param("userId") Long userId);
}
