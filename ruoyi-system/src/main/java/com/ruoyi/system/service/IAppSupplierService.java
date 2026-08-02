package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppSupplier;

/**
 * 供应商Service接口
 * 
 * @author lankong
 * @date 2025-05-21
 */
public interface IAppSupplierService 
{
    /**
     * 查询供应商
     * 
     * @param supplierId 供应商主键
     * @return 供应商
     */
    public AppSupplier selectAppSupplierBySupplierId(Long supplierId);

    /**
     * 查询供应商列表
     * 
     * @param appSupplier 供应商
     * @return 供应商集合
     */
    public List<AppSupplier> selectAppSupplierList(AppSupplier appSupplier);

    /**
     * 新增供应商
     * 
     * @param appSupplier 供应商
     * @return 结果
     */
    public int insertAppSupplier(AppSupplier appSupplier);

    /**
     * 修改供应商
     * 
     * @param appSupplier 供应商
     * @return 结果
     */
    public int updateAppSupplier(AppSupplier appSupplier);

    /**
     * 批量删除供应商
     * 
     * @param supplierIds 需要删除的供应商主键集合
     * @return 结果
     */
    public int deleteAppSupplierBySupplierIds(Long[] supplierIds);

    /**
     * 删除供应商信息
     * 
     * @param supplierId 供应商主键
     * @return 结果
     */
    public int deleteAppSupplierBySupplierId(Long supplierId);
    /**
     * 根据供应商ID查询供应商缓存信息
     * @param supplierId
     * @return
     */
    public AppSupplier getCacheSupplier(Long supplierId);
}
