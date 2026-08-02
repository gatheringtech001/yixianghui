package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppConsultant;
import org.apache.ibatis.annotations.Param;

/**
 * 康养顾问Mapper接口
 * 
 * @author lankong
 * @date 2025-05-14
 */
public interface AppConsultantMapper 
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
     * 删除康养顾问
     * 
     * @param consultantId 康养顾问主键
     * @return 结果
     */
    public int deleteAppConsultantByConsultantId(Long consultantId);

    /**
     * 批量删除康养顾问
     * 
     * @param consultantIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppConsultantByConsultantIds(Long[] consultantIds);

    /**
     * 根据用户ID获取顾问信息
     * @param userId
     * @return
     */
    AppConsultant selectAppConsultantByUserId(Long userId);

    /**
     * 根据顾问姓名查找顾问
     * @param consultantName
     * @return
     */
    AppConsultant selectAppConsultantByConsultantName(String consultantName);

    /**
     * 根据顾问编号查询顾问
     * @param consultantNo
     * @return
     */
    AppConsultant selectConsultantByConsultantNo(String consultantNo);

    /**
     * 查询未认领的顾问（按手机号）
     * @param mobile 顾问手机号
     * @param approvedOnly 是否仅查询已审核通过
     */
    AppConsultant selectUnclaimedConsultantByMobile(@Param("mobile") String mobile,
                                                    @Param("approvedOnly") boolean approvedOnly);

    /**
     * 解除顾问与小程序用户的绑定
     */
    int clearConsultantUserId(@Param("consultantId") Long consultantId,
                              @Param("updateTime") java.util.Date updateTime);
}
