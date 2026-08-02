package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsSkuDataMapper;
import com.ruoyi.system.domain.AppGoodsSkuData;
import com.ruoyi.system.service.IAppGoodsSkuDataService;

/**
 * 型号信息Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsSkuDataServiceImpl implements IAppGoodsSkuDataService 
{
    @Autowired
    private AppGoodsSkuDataMapper appGoodsSkuDataMapper;

    /**
     * 查询型号信息
     * 
     * @param dataId 型号信息主键
     * @return 型号信息
     */
    @Override
    public AppGoodsSkuData selectAppGoodsSkuDataByDataId(Long dataId)
    {
        return appGoodsSkuDataMapper.selectAppGoodsSkuDataByDataId(dataId);
    }

    /**
     * 查询型号信息列表
     * 
     * @param appGoodsSkuData 型号信息
     * @return 型号信息
     */
    @Override
    public List<AppGoodsSkuData> selectAppGoodsSkuDataList(AppGoodsSkuData appGoodsSkuData)
    {
        return appGoodsSkuDataMapper.selectAppGoodsSkuDataList(appGoodsSkuData);
    }

    /**
     * 新增型号信息
     * 
     * @param appGoodsSkuData 型号信息
     * @return 结果
     */
    @Override
    public int insertAppGoodsSkuData(AppGoodsSkuData appGoodsSkuData)
    {
        appGoodsSkuData.setCreateTime(DateUtils.getNowDate());
        return appGoodsSkuDataMapper.insertAppGoodsSkuData(appGoodsSkuData);
    }

    /**
     * 修改型号信息
     * 
     * @param appGoodsSkuData 型号信息
     * @return 结果
     */
    @Override
    public int updateAppGoodsSkuData(AppGoodsSkuData appGoodsSkuData)
    {
        return appGoodsSkuDataMapper.updateAppGoodsSkuData(appGoodsSkuData);
    }

    /**
     * 批量删除型号信息
     * 
     * @param dataIds 需要删除的型号信息主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuDataByDataIds(Long[] dataIds)
    {
        return appGoodsSkuDataMapper.deleteAppGoodsSkuDataByDataIds(dataIds);
    }

    /**
     * 删除型号信息信息
     * 
     * @param dataId 型号信息主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuDataByDataId(Long dataId)
    {
        return appGoodsSkuDataMapper.deleteAppGoodsSkuDataByDataId(dataId);
    }

    @Override
    public List<AppGoodsSkuData> selectAppGoodsSkuListByGoodsId(Long goodsId) {

        AppGoodsSkuData dataWhere = new AppGoodsSkuData();
        dataWhere.setGoodsId(goodsId);
        return this.selectAppGoodsSkuDataList(dataWhere);
    }
}
