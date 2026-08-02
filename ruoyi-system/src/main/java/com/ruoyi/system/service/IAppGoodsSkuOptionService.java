package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.AppGoodsSkuOption;

/**
 * 属性选项Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsSkuOptionService 
{
    /**
     * 查询属性选项
     * 
     * @param optionId 属性选项主键
     * @return 属性选项
     */
    public AppGoodsSkuOption selectAppGoodsSkuOptionByOptionId(Long optionId);

    /**
     * 查询属性选项列表
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 属性选项集合
     */
    public List<AppGoodsSkuOption> selectAppGoodsSkuOptionList(AppGoodsSkuOption appGoodsSkuOption);

    /**
     * 新增属性选项
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 结果
     */
    public int insertAppGoodsSkuOption(AppGoodsSkuOption appGoodsSkuOption);

    /**
     * 修改属性选项
     * 
     * @param appGoodsSkuOption 属性选项
     * @return 结果
     */
    public int updateAppGoodsSkuOption(AppGoodsSkuOption appGoodsSkuOption);

    /**
     * 批量删除属性选项
     * 
     * @param optionIds 需要删除的属性选项主键集合
     * @return 结果
     */
    public int deleteAppGoodsSkuOptionByOptionIds(Long[] optionIds);

    /**
     * 删除属性选项信息
     * 
     * @param optionId 属性选项主键
     * @return 结果
     */
    public int deleteAppGoodsSkuOptionByOptionId(Long optionId);

    /**
     * 根据skuId获取选项列表
     * @param skuId
     * @return
     */
    List<AppGoodsSkuOption> selectAppGoodsSkuOptionListBySkuId(Long skuId);
}
