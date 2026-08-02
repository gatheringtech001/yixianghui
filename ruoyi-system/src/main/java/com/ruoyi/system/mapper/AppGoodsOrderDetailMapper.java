package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsOrderDetail;

/**
 * 订单详细Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsOrderDetailMapper 
{
    /**
     * 查询订单详细
     * 
     * @param detailId 订单详细主键
     * @return 订单详细
     */
    public AppGoodsOrderDetail selectAppGoodsOrderDetailByDetailId(Long detailId);

    /**
     * 查询订单详细列表
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 订单详细集合
     */
    public List<AppGoodsOrderDetail> selectAppGoodsOrderDetailList(AppGoodsOrderDetail appGoodsOrderDetail);

    /**
     * 新增订单详细
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 结果
     */
    public int insertAppGoodsOrderDetail(AppGoodsOrderDetail appGoodsOrderDetail);

    /**
     * 修改订单详细
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 结果
     */
    public int updateAppGoodsOrderDetail(AppGoodsOrderDetail appGoodsOrderDetail);

    /**
     * 删除订单详细
     * 
     * @param detailId 订单详细主键
     * @return 结果
     */
    public int deleteAppGoodsOrderDetailByDetailId(Long detailId);

    /**
     * 批量删除订单详细
     * 
     * @param detailIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsOrderDetailByDetailIds(Long[] detailIds);
}
