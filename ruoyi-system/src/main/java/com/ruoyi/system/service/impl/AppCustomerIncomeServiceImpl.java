package com.ruoyi.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppCustomerIncomeMapper;
import com.ruoyi.system.domain.AppCustomerIncome;
import com.ruoyi.system.service.IAppCustomerIncomeService;

/**
 * 收入明细Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-14
 */
@Service
public class AppCustomerIncomeServiceImpl implements IAppCustomerIncomeService 
{
    @Autowired
    private AppCustomerIncomeMapper appCustomerIncomeMapper;

    /**
     * 查询收入明细
     * 
     * @param incomeId 收入明细主键
     * @return 收入明细
     */
    @Override
    public AppCustomerIncome selectAppCustomerIncomeByIncomeId(Long incomeId)
    {
        return appCustomerIncomeMapper.selectAppCustomerIncomeByIncomeId(incomeId);
    }

    /**
     * 查询收入明细列表
     * 
     * @param appCustomerIncome 收入明细
     * @return 收入明细
     */
    @Override
    public List<AppCustomerIncome> selectAppCustomerIncomeList(AppCustomerIncome appCustomerIncome)
    {
        return appCustomerIncomeMapper.selectAppCustomerIncomeList(appCustomerIncome);
    }

    /**
     * 新增收入明细
     * 
     * @param appCustomerIncome 收入明细
     * @return 结果
     */
    @Override
    public int insertAppCustomerIncome(AppCustomerIncome appCustomerIncome)
    {
        appCustomerIncome.setCreateTime(DateUtils.getNowDate());
        return appCustomerIncomeMapper.insertAppCustomerIncome(appCustomerIncome);
    }

    /**
     * 修改收入明细
     * 
     * @param appCustomerIncome 收入明细
     * @return 结果
     */
    @Override
    public int updateAppCustomerIncome(AppCustomerIncome appCustomerIncome)
    {
        appCustomerIncome.setUpdateTime(DateUtils.getNowDate());
        return appCustomerIncomeMapper.updateAppCustomerIncome(appCustomerIncome);
    }

    /**
     * 批量删除收入明细
     * 
     * @param incomeIds 需要删除的收入明细主键
     * @return 结果
     */
    @Override
    public int deleteAppCustomerIncomeByIncomeIds(Long[] incomeIds)
    {
        return appCustomerIncomeMapper.deleteAppCustomerIncomeByIncomeIds(incomeIds);
    }

    /**
     * 删除收入明细信息
     * 
     * @param incomeId 收入明细主键
     * @return 结果
     */
    @Override
    public int deleteAppCustomerIncomeByIncomeId(Long incomeId)
    {
        return appCustomerIncomeMapper.deleteAppCustomerIncomeByIncomeId(incomeId);
    }

    /**
     * 收入统计
     * @param appCustomerIncome
     * @return
     */
    @Override
    public Map<String, Object> statAppCustomerIncome(AppCustomerIncome appCustomerIncome) {
        Map<String, Object> rs = new HashMap<>();
        List<AppCustomerIncome> list = appCustomerIncomeMapper.selectAppCustomerIncomeList(appCustomerIncome);
        double chargeAmount = 0;
        double purchaseAmount = 0;
        double consultantIncomeAmount = 0;
        double companyIncomeAmount = 0;
        for (int i = 0; i < list.size(); i++) {
            chargeAmount += list.get(i).getChargeAmount().doubleValue();
            purchaseAmount += list.get(i).getPurchaseAmount().doubleValue();
            consultantIncomeAmount += list.get(i).getConsultantIncome().doubleValue();
            companyIncomeAmount += list.get(i).getCompanyIncome().doubleValue();
        }
        rs.put("chargeAmount", chargeAmount);
        rs.put("purchaseAmount", purchaseAmount);
        rs.put("consultantIncomeAmount", consultantIncomeAmount);
        rs.put("companyIncomeAmount", companyIncomeAmount);
        return rs;
    }
}
