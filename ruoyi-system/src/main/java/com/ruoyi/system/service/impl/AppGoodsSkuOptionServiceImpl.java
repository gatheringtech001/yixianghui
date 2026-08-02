package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsSkuOptionMapper;
import com.ruoyi.system.domain.AppGoodsSkuOption;
import com.ruoyi.system.service.IAppGoodsSkuOptionService;

/**
 * 属性选项Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsSkuOptionServiceImpl implements IAppGoodsSkuOptionService 
{
    @Autowired
    private AppGoodsSkuOptionMapper appGoodsSkuOptionMapper;

    /**
     * 查询属性选项
     * 
     * @param optionId 属性选项主键
     * @return 属性选项
     */
    @Override
    public AppGoodsSkuOption selectAppGoodsSkuOptionByOptionId(Long optionId)
    {
        return appGoodsSkuOptionMapper.selectAppGoodsSkuOptionByOptionId(optionId);
    }

    /**
     * 查询属性选项列表
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 属性选项
     */
    @Override
    public List<AppGoodsSkuOption> selectAppGoodsSkuOptionList(AppGoodsSkuOption appGoodsSkuOption)
    {
        return appGoodsSkuOptionMapper.selectAppGoodsSkuOptionList(appGoodsSkuOption);
    }

    /**
     * 新增属性选项
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 结果
     */
    @Override
    public int insertAppGoodsSkuOption(AppGoodsSkuOption appGoodsSkuOption)
    {
        appGoodsSkuOption.setCreateTime(DateUtils.getNowDate());
        return appGoodsSkuOptionMapper.insertAppGoodsSkuOption(appGoodsSkuOption);
    }

    /**
     * 修改属性选项
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 结果
     */
    @Override
    public int updateAppGoodsSkuOption(AppGoodsSkuOption appGoodsSkuOption)
    {
        return appGoodsSkuOptionMapper.updateAppGoodsSkuOption(appGoodsSkuOption);
    }

    /**
     * 批量删除属性选项
     * 
     * @param optionIds 需要删除的属性选项主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuOptionByOptionIds(Long[] optionIds)
    {
        return appGoodsSkuOptionMapper.deleteAppGoodsSkuOptionByOptionIds(optionIds);
    }

    /**
     * 删除属性选项信息
     * 
     * @param optionId 属性选项主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuOptionByOptionId(Long optionId)
    {
        return appGoodsSkuOptionMapper.deleteAppGoodsSkuOptionByOptionId(optionId);
    }

    /**
     * 根据skuId获取选项列表
     * @param skuId
     * @return
     */
    @Override
    public List<AppGoodsSkuOption> selectAppGoodsSkuOptionListBySkuId(Long skuId) {
        AppGoodsSkuOption optionWhere = new AppGoodsSkuOption();
        optionWhere.setSkuId(skuId);
        return this.selectAppGoodsSkuOptionList(optionWhere);
    }
}
