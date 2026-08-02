package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.system.domain.AppGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsOrderDetailMapper;
import com.ruoyi.system.domain.AppGoodsOrderDetail;
import com.ruoyi.system.service.IAppGoodsOrderDetailService;

/**
 * 订单详细Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsOrderDetailServiceImpl implements IAppGoodsOrderDetailService 
{
    @Autowired
    private AppGoodsOrderDetailMapper appGoodsOrderDetailMapper;

    /**
     * 查询订单详细
     * 
     * @param detailId 订单详细主键
     * @return 订单详细
     */
    @Override
    public AppGoodsOrderDetail selectAppGoodsOrderDetailByDetailId(Long detailId)
    {
        return appGoodsOrderDetailMapper.selectAppGoodsOrderDetailByDetailId(detailId);
    }

    /**
     * 查询订单详细列表
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 订单详细
     */
    @Override
    public List<AppGoodsOrderDetail> selectAppGoodsOrderDetailList(AppGoodsOrderDetail appGoodsOrderDetail)
    {
        return appGoodsOrderDetailMapper.selectAppGoodsOrderDetailList(appGoodsOrderDetail);
    }

    /**
     * 新增订单详细
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 结果
     */
    @Override
    public int insertAppGoodsOrderDetail(AppGoodsOrderDetail appGoodsOrderDetail)
    {
        return appGoodsOrderDetailMapper.insertAppGoodsOrderDetail(appGoodsOrderDetail);
    }

    /**
     * 修改订单详细
     * 
     * @param appGoodsOrderDetail 订单详细
     * @return 结果
     */
    @Override
    public int updateAppGoodsOrderDetail(AppGoodsOrderDetail appGoodsOrderDetail)
    {
        return appGoodsOrderDetailMapper.updateAppGoodsOrderDetail(appGoodsOrderDetail);
    }

    /**
     * 批量删除订单详细
     * 
     * @param detailIds 需要删除的订单详细主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderDetailByDetailIds(Long[] detailIds)
    {
        return appGoodsOrderDetailMapper.deleteAppGoodsOrderDetailByDetailIds(detailIds);
    }

    /**
     * 删除订单详细信息
     * 
     * @param detailId 订单详细主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderDetailByDetailId(Long detailId)
    {
        return appGoodsOrderDetailMapper.deleteAppGoodsOrderDetailByDetailId(detailId);
    }

    /**
     * 根据订单ID查询列表
     * @param orderId
     * @return
     */
    @Override
    public List<AppGoodsOrderDetail> selectAppGoodsOrderDetailByOrderId(Long orderId) {
        AppGoodsOrderDetail detailWhere = new AppGoodsOrderDetail();
        detailWhere.setOrderId(orderId);
        return selectAppGoodsOrderDetailList(detailWhere);
    }
}
