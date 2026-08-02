package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsSkuData;

/**
 * 型号信息Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsSkuDataService 
{
    /**
     * 查询型号信息
     * 
     * @param dataId 型号信息主键
     * @return 型号信息
     */
    public AppGoodsSkuData selectAppGoodsSkuDataByDataId(Long dataId);

    /**
     * 查询型号信息列表
     * 
     * @param appGoodsSkuData 型号信息
     * @return 型号信息集合
     */
    public List<AppGoodsSkuData> selectAppGoodsSkuDataList(AppGoodsSkuData appGoodsSkuData);

    /**
     * 新增型号信息
     * 
     * @param appGoodsSkuData 型号信息
     * @return 结果
     */
    public int insertAppGoodsSkuData(AppGoodsSkuData appGoodsSkuData);

    /**
     * 修改型号信息
     * 
     * @param appGoodsSkuData 型号信息
     * @return 结果
     */
    public int updateAppGoodsSkuData(AppGoodsSkuData appGoodsSkuData);

    /**
     * 批量删除型号信息
     * 
     * @param dataIds 需要删除的型号信息主键集合
     * @return 结果
     */
    public int deleteAppGoodsSkuDataByDataIds(Long[] dataIds);

    /**
     * 删除型号信息信息
     * 
     * @param dataId 型号信息主键
     * @return 结果
     */
    public int deleteAppGoodsSkuDataByDataId(Long dataId);

    List<AppGoodsSkuData> selectAppGoodsSkuListByGoodsId(Long goodsId);
}
