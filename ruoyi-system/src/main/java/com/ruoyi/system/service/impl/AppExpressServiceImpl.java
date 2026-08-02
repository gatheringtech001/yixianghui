package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppExpressMapper;
import com.ruoyi.system.domain.AppExpress;
import com.ruoyi.system.service.IAppExpressService;

/**
 * 快递公司Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppExpressServiceImpl implements IAppExpressService 
{
    @Autowired
    private AppExpressMapper appExpressMapper;

    /**
     * 查询快递公司
     * 
     * @param expressId 快递公司主键
     * @return 快递公司
     */
    @Override
    public AppExpress selectAppExpressByExpressId(Long expressId)
    {
        return appExpressMapper.selectAppExpressByExpressId(expressId);
    }

    /**
     * 查询快递公司列表
     * 
     * @param appExpress 快递公司
     * @return 快递公司
     */
    @Override
    public List<AppExpress> selectAppExpressList(AppExpress appExpress)
    {
        return appExpressMapper.selectAppExpressList(appExpress);
    }

    /**
     * 新增快递公司
     * 
     * @param appExpress 快递公司
     * @return 结果
     */
    @Override
    public int insertAppExpress(AppExpress appExpress)
    {
        appExpress.setCreateTime(DateUtils.getNowDate());
        return appExpressMapper.insertAppExpress(appExpress);
    }

    /**
     * 修改快递公司
     * 
     * @param appExpress 快递公司
     * @return 结果
     */
    @Override
    public int updateAppExpress(AppExpress appExpress)
    {
        return appExpressMapper.updateAppExpress(appExpress);
    }

    /**
     * 批量删除快递公司
     * 
     * @param expressIds 需要删除的快递公司主键
     * @return 结果
     */
    @Override
    public int deleteAppExpressByExpressIds(Long[] expressIds)
    {
        return appExpressMapper.deleteAppExpressByExpressIds(expressIds);
    }

    /**
     * 删除快递公司信息
     * 
     * @param expressId 快递公司主键
     * @return 结果
     */
    @Override
    public int deleteAppExpressByExpressId(Long expressId)
    {
        return appExpressMapper.deleteAppExpressByExpressId(expressId);
    }
}
