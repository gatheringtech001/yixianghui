package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.AppCustomerIncome;

/**
 * 收入明细Service接口
 * 
 * @author lankong
 * @date 2025-05-14
 */
public interface IAppCustomerIncomeService 
{
    /**
     * 查询收入明细
     * 
     * @param incomeId 收入明细主键
     * @return 收入明细
     */
    public AppCustomerIncome selectAppCustomerIncomeByIncomeId(Long incomeId);

    /**
     * 查询收入明细列表
     * 
     * @param appCustomerIncome 收入明细
     * @return 收入明细集合
     */
    public List<AppCustomerIncome> selectAppCustomerIncomeList(AppCustomerIncome appCustomerIncome);

    /**
     * 新增收入明细
     * 
     * @param appCustomerIncome 收入明细
     * @return 结果
     */
    public int insertAppCustomerIncome(AppCustomerIncome appCustomerIncome);

    /**
     * 修改收入明细
     * 
     * @param appCustomerIncome 收入明细
     * @return 结果
     */
    public int updateAppCustomerIncome(AppCustomerIncome appCustomerIncome);

    /**
     * 批量删除收入明细
     * 
     * @param incomeIds 需要删除的收入明细主键集合
     * @return 结果
     */
    public int deleteAppCustomerIncomeByIncomeIds(Long[] incomeIds);

    /**
     * 删除收入明细信息
     * 
     * @param incomeId 收入明细主键
     * @return 结果
     */
    public int deleteAppCustomerIncomeByIncomeId(Long incomeId);

    /**
     * 收入统计
     * @param appCustomerIncome
     * @return
     */
    Map<String, Object> statAppCustomerIncome(AppCustomerIncome appCustomerIncome);
}
