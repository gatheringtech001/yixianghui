package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsOrderAfter;

/**
 * 订单商品售后Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsOrderAfterMapper 
{
    /**
     * 查询订单商品售后
     * 
     * @param afterId 订单商品售后主键
     * @return 订单商品售后
     */
    public AppGoodsOrderAfter selectAppGoodsOrderAfterByAfterId(Long afterId);

    /**
     * 查询订单商品售后列表
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 订单商品售后集合
     */
    public List<AppGoodsOrderAfter> selectAppGoodsOrderAfterList(AppGoodsOrderAfter appGoodsOrderAfter);

    /**
     * 新增订单商品售后
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 结果
     */
    public int insertAppGoodsOrderAfter(AppGoodsOrderAfter appGoodsOrderAfter);

    /**
     * 修改订单商品售后
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 结果
     */
    public int updateAppGoodsOrderAfter(AppGoodsOrderAfter appGoodsOrderAfter);

    /**
     * 删除订单商品售后
     * 
     * @param afterId 订单商品售后主键
     * @return 结果
     */
    public int deleteAppGoodsOrderAfterByAfterId(Long afterId);

    /**
     * 批量删除订单商品售后
     * 
     * @param afterIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsOrderAfterByAfterIds(Long[] afterIds);

    public AppGoodsOrderAfter selectAppGoodsOrderAfterByOutorderno(String outOrderNo);
}
