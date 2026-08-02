package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.system.domain.AppCustomer;

/**
 * 客户资料Service接口
 * 
 * @author lankong
 * @date 2025-05-07
 */
public interface IAppCustomerService 
{
    /**
     * 查询客户资料
     * 
     * @param customerId 客户资料主键
     * @return 客户资料
     */
    public AppCustomer selectAppCustomerByCustomerId(Long customerId);

    /**
     * 查询客户资料列表
     * 
     * @param appCustomer 客户资料
     * @return 客户资料集合
     */
    public List<AppCustomer> selectAppCustomerList(AppCustomer appCustomer);

    /**
     * 新增客户资料
     * 
     * @param appCustomer 客户资料
     * @return 结果
     */
    public int insertAppCustomer(AppCustomer appCustomer);

    /**
     * 修改客户资料
     * 
     * @param appCustomer 客户资料
     * @return 结果
     */
    public int updateAppCustomer(AppCustomer appCustomer);

    /**
     * 批量删除客户资料
     * 
     * @param customerIds 需要删除的客户资料主键集合
     * @return 结果
     */
    public int deleteAppCustomerByCustomerIds(Long[] customerIds);

    /**
     * 删除客户资料信息
     * 
     * @param customerId 客户资料主键
     * @return 结果
     */
    public int deleteAppCustomerByCustomerId(Long customerId);

    /**
     * 导入客户资料
     * @param customerList
     * @param updateSupport
     * @param operName
     * @return
     */
    String importCustomer(List<AppCustomer> customerList, boolean updateSupport, String operName);

    /**
     * 通过客户姓名查询客户
     * @param customerName
     * @return
     */
    AppCustomer selectCustomerByCustomerName(String customerName);

    /**
     * 根据用户ID查询客户信息
     * @param userId
     * @return
     */
    AppCustomer selectAppCustomerByUserId(Long userId);

    /**
     * 按系统用户保存现居住地址
     */
    int saveLiveAddressByUserId(Long userId, String liveAddress, String customerName, String linkMobile);

    /**
     * 客户资料统计
     * @return
     */
    Map custStatic();
}
