package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.AppCustomer;
import com.ruoyi.system.domain.CustomerStatic;

/**
 * 客户资料Mapper接口
 * 
 * @author lankong
 * @date 2025-05-07
 */
public interface AppCustomerMapper 
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
     * 删除客户资料
     * 
     * @param customerId 客户资料主键
     * @return 结果
     */
    public int deleteAppCustomerByCustomerId(Long customerId);

    /**
     * 批量删除客户资料
     * 
     * @param customerIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppCustomerByCustomerIds(Long[] customerIds);

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
     * 性别统计
     * @return
     */
    List<CustomerStatic> custGenderStatic();
    /**
     * 商品统计
     * @return
     */
    List<CustomerStatic> custGoodsStatic();
    /**
     * 数量统计
     * @return
     */
    List<CustomerStatic> custNumStatic();
    /**
     * 保险评价统计
     * @return
     */
    List<CustomerStatic> custInsureEvaStatic();
    /**
     *  居住情况统计
     * @return
     */
    List<CustomerStatic> custResidentialStatic();

    Long countByConsultantId(@Param("consultantId") Long consultantId);
}
