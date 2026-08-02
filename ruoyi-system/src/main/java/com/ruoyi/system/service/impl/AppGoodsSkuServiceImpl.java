package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsSkuMapper;
import com.ruoyi.system.domain.AppGoodsSku;
import com.ruoyi.system.service.IAppGoodsSkuService;

/**
 * 商品属性Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsSkuServiceImpl implements IAppGoodsSkuService 
{
    @Autowired
    private AppGoodsSkuMapper appGoodsSkuMapper;

    /**
     * 查询商品属性
     * 
     * @param skuId 商品属性主键
     * @return 商品属性
     */
    @Override
    public AppGoodsSku selectAppGoodsSkuBySkuId(Long skuId)
    {
        return appGoodsSkuMapper.selectAppGoodsSkuBySkuId(skuId);
    }

    /**
     * 查询商品属性列表
     * 
     * @param appGoodsSku 商品属性
     * @return 商品属性
     */
    @Override
    public List<AppGoodsSku> selectAppGoodsSkuList(AppGoodsSku appGoodsSku)
    {
        return appGoodsSkuMapper.selectAppGoodsSkuList(appGoodsSku);
    }

    /**
     * 新增商品属性
     * 
     * @param appGoodsSku 商品属性
     * @return 结果
     */
    @Override
    public int insertAppGoodsSku(AppGoodsSku appGoodsSku)
    {
        appGoodsSku.setCreateTime(DateUtils.getNowDate());
        return appGoodsSkuMapper.insertAppGoodsSku(appGoodsSku);
    }

    /**
     * 修改商品属性
     * 
     * @param appGoodsSku 商品属性
     * @return 结果
     */
    @Override
    public int updateAppGoodsSku(AppGoodsSku appGoodsSku)
    {
        return appGoodsSkuMapper.updateAppGoodsSku(appGoodsSku);
    }

    /**
     * 批量删除商品属性
     * 
     * @param skuIds 需要删除的商品属性主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuBySkuIds(Long[] skuIds)
    {
        return appGoodsSkuMapper.deleteAppGoodsSkuBySkuIds(skuIds);
    }

    /**
     * 删除商品属性信息
     * 
     * @param skuId 商品属性主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsSkuBySkuId(Long skuId)
    {
        return appGoodsSkuMapper.deleteAppGoodsSkuBySkuId(skuId);
    }

    @Override
    public List<AppGoodsSku> selectAppGoodsSkuListByGoodsId(Long goodsId) {

        AppGoodsSku skuWhere = new AppGoodsSku();
        skuWhere.setGoodsId(goodsId);
        return this.selectAppGoodsSkuList(skuWhere);
    }
}
