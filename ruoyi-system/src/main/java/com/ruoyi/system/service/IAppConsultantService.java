package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppConsultant;

/**
 * 康养顾问Service接口
 * 
 * @author lankong
 * @date 2025-05-14
 */
public interface IAppConsultantService 
{
    /**
     * 查询康养顾问
     * 
     * @param consultantId 康养顾问主键
     * @return 康养顾问
     */
    public AppConsultant selectAppConsultantByConsultantId(Long consultantId);

    /**
     * 查询康养顾问列表
     * 
     * @param appConsultant 康养顾问
     * @return 康养顾问集合
     */
    public List<AppConsultant> selectAppConsultantList(AppConsultant appConsultant);

    /**
     * 新增康养顾问
     * 
     * @param appConsultant 康养顾问
     * @return 结果
     */
    public int insertAppConsultant(AppConsultant appConsultant);

    /**
     * 修改康养顾问
     * 
     * @param appConsultant 康养顾问
     * @return 结果
     */
    public int updateAppConsultant(AppConsultant appConsultant);

    /**
     * 批量删除康养顾问
     * 
     * @param consultantIds 需要删除的康养顾问主键集合
     * @return 结果
     */
    public int deleteAppConsultantByConsultantIds(Long[] consultantIds);

    /**
     * 删除康养顾问信息
     * 
     * @param consultantId 康养顾问主键
     * @return 结果
     */
    public int deleteAppConsultantByConsultantId(Long consultantId);

    /**
     * 根据用户ID获取顾问信息
     * @param userId
     * @return
     */
    AppConsultant selectAppConsultantByUserId(Long userId);

    /**
     * 根据ID获取顾问名称
     * @param consultantId
     * @return
     */
    public String selectAppConsultantNameById(Long consultantId);

    /**
     * 根据顾问姓名查找顾问
     * @param consultantName
     * @return
     */
    public AppConsultant selectAppConsultantByConsultantName(String consultantName);

    String importConsultant(List<AppConsultant> consultantList, boolean updateSupport, String operName);

    /**
     * 获取缓存的顾问信息
     * @param consultantId
     * @return
     */
    AppConsultant getCacheConsultant(Long consultantId);

    /**
     * 获取当前用户顾问信息：已绑定则校验手机号一致，无绑定则按手机号自动认领已审核档案
     */
    AppConsultant getOrClaimConsultantByUser(Long userId, String mobile);

    /**
     * 小程序申请成为顾问：合并未认领档案或新建
     */
    int applyConsultantAsUser(Long userId, AppConsultant consultant);
}
