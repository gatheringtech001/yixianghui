package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import java.util.Date;

import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.TravelOrderSyncRecord;
import org.apache.ibatis.annotations.Param;

/**
 * 商品订单Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsOrderMapper 
{
    /**
     * 查询商品订单
     * 
     * @param orderId 商品订单主键
     * @return 商品订单
     */
    public AppGoodsOrder selectAppGoodsOrderByOrderId(Long orderId);

    /**
     * 查询商品订单列表
     * 
     * @param appGoodsOrder 商品订单
     * @return 商品订单集合
     */
    public List<AppGoodsOrder> selectAppGoodsOrderList(AppGoodsOrder appGoodsOrder);

    /**
     * 新增商品订单
     * 
     * @param appGoodsOrder 商品订单
     * @return 结果
     */
    public int insertAppGoodsOrder(AppGoodsOrder appGoodsOrder);

    /**
     * 修改商品订单
     * 
     * @param appGoodsOrder 商品订单
     * @return 结果
     */
    public int updateAppGoodsOrder(AppGoodsOrder appGoodsOrder);

    /**
     * 删除商品订单
     * 
     * @param orderId 商品订单主键
     * @return 结果
     */
    public int deleteAppGoodsOrderByOrderId(Long orderId);

    /**
     * 批量删除商品订单
     * 
     * @param orderIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsOrderByOrderIds(Long[] orderIds);

    /**
     * 查询商品订单
     * @return
     */
    AppGoodsOrder selectAppGoodsOrderByOrderNo(String orderNo);

    /**
     * 统计教育课程有效报名数
     */
    int countEducationActiveSignupByGoodsId(Long goodsId);

    /**
     * 查询商品订单统计数据
     * @return
     */
    Map selAppGoodsOrderStatData();

    /**
     * 查询指定时间后创建的旅居订单，供飞书增量同步。
     */
    List<TravelOrderSyncRecord> selectTravelOrdersCreatedSince(@Param("createdAfter") Date createdAfter);
}
