package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppActivityOrder;

/**
 * 活动预约Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppActivityOrderMapper 
{
    /**
     * 查询活动预约
     * 
     * @param orderId 活动预约主键
     * @return 活动预约
     */
    public AppActivityOrder selectAppActivityOrderByOrderId(Long orderId);

    /**
     * 查询活动预约列表
     * 
     * @param appActivityOrder 活动预约
     * @return 活动预约集合
     */
    public List<AppActivityOrder> selectAppActivityOrderList(AppActivityOrder appActivityOrder);

    /**
     * 新增活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    public int insertAppActivityOrder(AppActivityOrder appActivityOrder);

    /**
     * 修改活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    public int updateAppActivityOrder(AppActivityOrder appActivityOrder);

    /**
     * 删除活动预约
     * 
     * @param orderId 活动预约主键
     * @return 结果
     */
    public int deleteAppActivityOrderByOrderId(Long orderId);

    /**
     * 批量删除活动预约
     * 
     * @param orderIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppActivityOrderByOrderIds(Long[] orderIds);
}
