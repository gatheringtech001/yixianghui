package com.ruoyi.system.service.impl;

import java.util.List;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppSupplierMapper;
import com.ruoyi.system.domain.AppSupplier;
import com.ruoyi.system.service.IAppSupplierService;

/**
 * 供应商Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-21
 */
@Service
public class AppSupplierServiceImpl implements IAppSupplierService 
{
    @Autowired
    private AppSupplierMapper appSupplierMapper;

    /**
     * 查询供应商
     * 
     * @param supplierId 供应商主键
     * @return 供应商
     */
    @Override
    public AppSupplier selectAppSupplierBySupplierId(Long supplierId)
    {
        return appSupplierMapper.selectAppSupplierBySupplierId(supplierId);
    }

    /**
     * 查询供应商列表
     * 
     * @param appSupplier 供应商
     * @return 供应商
     */
    @Override
    public List<AppSupplier> selectAppSupplierList(AppSupplier appSupplier)
    {
        return appSupplierMapper.selectAppSupplierList(appSupplier);
    }

    /**
     * 新增供应商
     * 
     * @param appSupplier 供应商
     * @return 结果
     */
    @Override
    public int insertAppSupplier(AppSupplier appSupplier)
    {
        return appSupplierMapper.insertAppSupplier(appSupplier);
    }

    /**
     * 修改供应商
     * 
     * @param appSupplier 供应商
     * @return 结果
     */
    @Override
    public int updateAppSupplier(AppSupplier appSupplier)
    {
        return appSupplierMapper.updateAppSupplier(appSupplier);
    }

    /**
     * 批量删除供应商
     * 
     * @param supplierIds 需要删除的供应商主键
     * @return 结果
     */
    @Override
    public int deleteAppSupplierBySupplierIds(Long[] supplierIds)
    {
        return appSupplierMapper.deleteAppSupplierBySupplierIds(supplierIds);
    }

    /**
     * 删除供应商信息
     * 
     * @param supplierId 供应商主键
     * @return 结果
     */
    @Override
    public int deleteAppSupplierBySupplierId(Long supplierId)
    {
        return appSupplierMapper.deleteAppSupplierBySupplierId(supplierId);
    }

    @Override
    public AppSupplier getCacheSupplier(Long supplierId) {
        AppSupplier supplier = SpringUtils.getBean(RedisCache.class).getCacheObject(CacheConstants.SUPPLIER_KEY +"id:" + supplierId);
        if(null==supplier){
            supplier = appSupplierMapper.selectAppSupplierBySupplierId(supplierId);
            if(null!=supplier){
                SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.SUPPLIER_KEY +"id:" + supplierId, supplier);
            }
        }
        return supplier;
    }
}
