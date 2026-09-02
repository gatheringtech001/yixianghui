package com.ruoyi.system.service.impl;

import java.util.List;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.mapper.AppAttachmentsMapper;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import com.ruoyi.system.mapper.AppPayLogMapper;
import com.ruoyi.system.service.TravelOrderStatusPolicy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsOrderAfterMapper;
import com.ruoyi.system.service.IAppGoodsOrderAfterService;

/**
 * 订单商品售后Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsOrderAfterServiceImpl implements IAppGoodsOrderAfterService 
{
    Logger log = org.slf4j.LoggerFactory.getLogger(AppGoodsOrderAfterServiceImpl.class);
    private static final String ERR_MSG = "订单商品售后异常: ";
    @Autowired
    private AppGoodsOrderAfterMapper appGoodsOrderAfterMapper;
    @Autowired
    private AppPayLogMapper appPayLogMapper;
    @Autowired
    private AppGoodsOrderMapper appGoodsOrderMapper;
    @Autowired
    private AppAttachmentsMapper appAttachmentsMapper;

    /**
     * 查询订单商品售后
     * 
     * @param afterId 订单商品售后主键
     * @return 订单商品售后
     */
    @Override
    public AppGoodsOrderAfter selectAppGoodsOrderAfterByAfterId(Long afterId)
    {
        return appGoodsOrderAfterMapper.selectAppGoodsOrderAfterByAfterId(afterId);
    }

    /**
     * 查询订单商品售后列表
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 订单商品售后
     */
    @Override
    public List<AppGoodsOrderAfter> selectAppGoodsOrderAfterList(AppGoodsOrderAfter appGoodsOrderAfter)
    {
        return appGoodsOrderAfterMapper.selectAppGoodsOrderAfterList(appGoodsOrderAfter);
    }

    /**
     * 新增订单商品售后
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 结果
     */
    @Override
    public int insertAppGoodsOrderAfter(AppGoodsOrderAfter appGoodsOrderAfter)
    {
        return appGoodsOrderAfterMapper.insertAppGoodsOrderAfter(appGoodsOrderAfter);
    }

    /**
     * 修改订单商品售后
     * 
     * @param appGoodsOrderAfter 订单商品售后
     * @return 结果
     */
    @Override
    public int updateAppGoodsOrderAfter(AppGoodsOrderAfter appGoodsOrderAfter)
    {
        return appGoodsOrderAfterMapper.updateAppGoodsOrderAfter(appGoodsOrderAfter);
    }

    /**
     * 批量删除订单商品售后
     * 
     * @param afterIds 需要删除的订单商品售后主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderAfterByAfterIds(Long[] afterIds)
    {
        return appGoodsOrderAfterMapper.deleteAppGoodsOrderAfterByAfterIds(afterIds);
    }

    /**
     * 删除订单商品售后信息
     * 
     * @param afterId 订单商品售后主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsOrderAfterByAfterId(Long afterId)
    {
        return appGoodsOrderAfterMapper.deleteAppGoodsOrderAfterByAfterId(afterId);
    }

    /**
     * 订单商品售后
     *
     * @param appGoodsOrderAfter 订单商品售后
     * @return 订单商品售后
     */
    @Override
    public int appGoodsOrderAfter(AppGoodsOrderAfter appGoodsOrderAfter) {
        int retres = 0;
        try{
            if (appGoodsOrderAfter.getOrderId() != null) {
                AppGoodsOrder dbOrder = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(appGoodsOrderAfter.getOrderId());
                if (dbOrder != null) {
                    if (StringUtils.isEmpty(appGoodsOrderAfter.getOutOrderNo())) {
                        appGoodsOrderAfter.setOutOrderNo(dbOrder.getOrderNo());
                    }
                    if (appGoodsOrderAfter.getUserId() == null) {
                        appGoodsOrderAfter.setUserId(dbOrder.getUserId());
                    }
                    if (appGoodsOrderAfter.getOrderMoney() == null) {
                        appGoodsOrderAfter.setOrderMoney(dbOrder.getMoneyPayable());
                    }
                }
            }
            if(appGoodsOrderAfter.getAfterType() != null && appGoodsOrderAfter.getAfterType().equals("2")) {
                AppGoodsOrder appGoodsOrder = new AppGoodsOrder();
                appGoodsOrder.setOrderId(appGoodsOrderAfter.getOrderId());
                appGoodsOrder.setStatus("3");
                AppGoodsOrder current = appGoodsOrderMapper.selectAppGoodsOrderByOrderId(appGoodsOrderAfter.getOrderId());
                if (current != null && current.getTravelStatus() != null) {
                    if (!TravelOrderStatusPolicy.REFUNDING.equals(current.getTravelStatus())
                            && !TravelOrderStatusPolicy.REFUNDED.equals(current.getTravelStatus())) {
                        appGoodsOrder.setTravelStatusBeforeRefund(current.getTravelStatus());
                    }
                    appGoodsOrder.setTravelStatus(TravelOrderStatusPolicy.REFUNDING);
                }
                appGoodsOrderMapper.updateAppGoodsOrder(appGoodsOrder);
            }
            appGoodsOrderAfter.setStatus("0");
            retres = appGoodsOrderAfterMapper.insertAppGoodsOrderAfter(appGoodsOrderAfter);
            //审批凭证
            List<JSONObject> theAttatchs = appGoodsOrderAfter.getFileList();
            if(null!=theAttatchs){
                AppAttachments theattatch;
                for(JSONObject theAttatch:theAttatchs){
                    theattatch = new AppAttachments();
                    theattatch.setAttStatus("1");
                    theattatch.setBussId(appGoodsOrderAfter.getAfterId());
                    theattatch.setBussType("001");
                    theattatch.setFilePath(theAttatch.getString("fileName"));
                    theattatch.setCreateTime(DateUtils.getNowDate());
                    appAttachmentsMapper.insertAppAttachments(theattatch);
                }
            }
        }catch (Exception ex){
            log.error(ERR_MSG,ex);
        }
        return retres;
    }
}
