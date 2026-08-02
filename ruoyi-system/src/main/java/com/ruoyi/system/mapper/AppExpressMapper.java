package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.AppExpress;

/**
 * 快递公司Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppExpressMapper 
{
    /**
     * 查询快递公司
     * 
     * @param expressId 快递公司主键
     * @return 快递公司
     */
    public AppExpress selectAppExpressByExpressId(Long expressId);

    /**
     * 查询快递公司列表
     * 
     * @param appExpress 快递公司
     * @return 快递公司集合
     */
    public List<AppExpress> selectAppExpressList(AppExpress appExpress);

    /**
     * 新增快递公司
     * 
     * @param appExpress 快递公司
     * @return 结果
     */
    public int insertAppExpress(AppExpress appExpress);

    /**
     * 修改快递公司
     * 
     * @param appExpress 快递公司
     * @return 结果
     */
    public int updateAppExpress(AppExpress appExpress);

    /**
     * 删除快递公司
     * 
     * @param expressId 快递公司主键
     * @return 结果
     */
    public int deleteAppExpressByExpressId(Long expressId);

    /**
     * 批量删除快递公司
     * 
     * @param expressIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppExpressByExpressIds(Long[] expressIds);
}
