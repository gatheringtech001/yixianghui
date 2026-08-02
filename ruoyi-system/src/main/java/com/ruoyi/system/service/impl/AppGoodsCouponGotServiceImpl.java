package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCouponGotMapper;
import com.ruoyi.system.domain.AppGoodsCouponGot;
import com.ruoyi.system.service.IAppGoodsCouponGotService;

/**
 * 优惠券领取记录Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCouponGotServiceImpl implements IAppGoodsCouponGotService 
{
    @Autowired
    private AppGoodsCouponGotMapper appGoodsCouponGotMapper;

    /**
     * 查询优惠券领取记录
     * 
     * @param gotId 优惠券领取记录主键
     * @return 优惠券领取记录
     */
    @Override
    public AppGoodsCouponGot selectAppGoodsCouponGotByGotId(Long gotId)
    {
        return appGoodsCouponGotMapper.selectAppGoodsCouponGotByGotId(gotId);
    }

    /**
     * 查询优惠券领取记录列表
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 优惠券领取记录
     */
    @Override
    public List<AppGoodsCouponGot> selectAppGoodsCouponGotList(AppGoodsCouponGot appGoodsCouponGot)
    {
        return appGoodsCouponGotMapper.selectAppGoodsCouponGotList(appGoodsCouponGot);
    }

    /**
     * 新增优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    @Override
    public int insertAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot)
    {
        appGoodsCouponGot.setCreateTime(DateUtils.getNowDate());
        return appGoodsCouponGotMapper.insertAppGoodsCouponGot(appGoodsCouponGot);
    }

    /**
     * 修改优惠券领取记录
     * 
     * @param appGoodsCouponGot 优惠券领取记录
     * @return 结果
     */
    @Override
    public int updateAppGoodsCouponGot(AppGoodsCouponGot appGoodsCouponGot)
    {
        appGoodsCouponGot.setUpdateTime(DateUtils.getNowDate());
        return appGoodsCouponGotMapper.updateAppGoodsCouponGot(appGoodsCouponGot);
    }

    /**
     * 批量删除优惠券领取记录
     * 
     * @param gotIds 需要删除的优惠券领取记录主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponGotByGotIds(Long[] gotIds)
    {
        return appGoodsCouponGotMapper.deleteAppGoodsCouponGotByGotIds(gotIds);
    }

    /**
     * 删除优惠券领取记录信息
     * 
     * @param gotId 优惠券领取记录主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCouponGotByGotId(Long gotId)
    {
        return appGoodsCouponGotMapper.deleteAppGoodsCouponGotByGotId(gotId);
    }
}
