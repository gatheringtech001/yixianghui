package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.AppGoodsRelated;
import com.ruoyi.system.domain.AppGoodsSku;
import com.ruoyi.system.domain.AppGoodsSkuOption;
import com.ruoyi.system.domain.AppGoodsEducationExt;
import com.ruoyi.system.domain.vo.AppGoodsVo;
import com.ruoyi.system.mapper.AppGoodsRelatedMapper;
import com.ruoyi.system.mapper.AppGoodsSkuMapper;
import com.ruoyi.system.mapper.AppGoodsSkuOptionMapper;
import com.ruoyi.system.mapper.AppGoodsEducationExtMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsMapper;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.service.IAppGoodsService;

/**
 * 商品Service业务层处理
 * 
 * @author lankong
 * @date 2025-03-31
 */
@Service
public class AppGoodsServiceImpl implements IAppGoodsService 
{
    @Autowired
    private AppGoodsMapper appGoodsMapper;
    @Autowired
    private AppGoodsSkuMapper appGoodsSkuMapper;
    @Autowired
    private AppGoodsSkuOptionMapper appGoodsSkuOptionMapper;
    @Autowired
    private AppGoodsRelatedMapper appGoodsRelatedMapper;
    @Autowired
    private AppGoodsEducationExtMapper appGoodsEducationExtMapper;

    /**
     * 查询商品
     * 
     * @param goodsId 商品主键
     * @return 商品
     */
    @Override
    public AppGoods selectAppGoodsByGoodsId(Long goodsId)
    {
        AppGoods retgoods = appGoodsMapper.selectAppGoodsByGoodsId(goodsId);
        AppGoodsRelated querelated = new AppGoodsRelated();
        querelated.setGoodsId(goodsId);
        List<AppGoodsRelated> relatedList = appGoodsRelatedMapper.selectAppGoodsRelatedList(querelated);
        retgoods.setFeatures(relatedList);
        AppGoodsSku querySku = new AppGoodsSku();
        querySku.setGoodsId(goodsId);
        List<AppGoodsSku> skuList = appGoodsSkuMapper.selectAppGoodsSkuList(querySku);
        if(null!=skuList && skuList.size()>0){
            for (int i = 0; i < skuList.size(); i++){
                AppGoodsSkuOption queryOption = new AppGoodsSkuOption();
                queryOption.setSkuId(skuList.get(i).getSkuId());
                List<AppGoodsSkuOption> optionList = appGoodsSkuOptionMapper.selectAppGoodsSkuOptionList(queryOption);
                if(null!=optionList && optionList.size()>0){
                    skuList.get(i).setOptions(optionList);
                }
            }
            retgoods.setOptionList(skuList);
        }
        if (retgoods != null && "education".equals(retgoods.getGoodsType())) {
            retgoods.setEducationExt(appGoodsEducationExtMapper.selectAppGoodsEducationExtByGoodsId(goodsId));
        }
        return retgoods;
    }

    /**
     * 查询商品列表
     * 
     * @param appGoods 商品
     * @return 商品
     */
    @Override
    public List<AppGoods> selectAppGoodsList(AppGoods appGoods)
    {
        List<AppGoods> list = appGoodsMapper.selectAppGoodsList(appGoods);
        if (list != null) {
            for (AppGoods goods : list) {
                if (goods != null && "education".equals(goods.getGoodsType())) {
                    goods.setEducationExt(appGoodsEducationExtMapper.selectAppGoodsEducationExtByGoodsId(goods.getGoodsId()));
                }
            }
        }
        return list;
    }

    /**
     * 新增商品
     * 
     * @param appGoods 商品
     * @return 结果
     */
    @Override
    public int insertAppGoods(AppGoods appGoods)
    {
        appGoods.setCreateTime(DateUtils.getNowDate());
        appGoodsMapper.insertAppGoods(appGoods);
        List<AppGoodsSku> skuList = appGoods.getOptionList();
        List<AppGoodsSkuOption> optionList;
        Map skuidmap = new HashMap();
        if(null!=skuList && skuList.size()>0) {
            if (skuList != null && !skuList.isEmpty()) {
                skuList.sort((sku1, sku2) -> {
                    if (sku1.getSkuType() == null) return -1;
                    if (sku2.getSkuType() == null) return 1;
                    return sku1.getSkuType().compareTo(sku2.getSkuType());
                });
            }
            for (int i = 0; i < skuList.size(); i++) {
                AppGoodsSku sku = skuList.get(i);
                sku.setGoodsId(appGoods.getGoodsId());
                sku.setSortOrder(i + 1);
                sku.setCreateTime(DateUtils.getNowDate());
                if(null!=sku.getTmpParSkuId() && sku.getTmpParSkuId().startsWith("temp_")){
                    if(skuidmap.containsKey(sku.getTmpParSkuId())){
                        sku.setParSkuId(skuidmap.get(sku.getTmpParSkuId()).toString());
                    }
                }
                appGoodsSkuMapper.insertAppGoodsSku(sku);
                if(sku.getTmpSkuId()!=null && sku.getTmpSkuId().startsWith("temp_")){
                    skuidmap.put(sku.getTmpSkuId(), sku.getSkuId());
                }
                optionList = sku.getOptions();
                if (null != optionList && optionList.size() > 0) {
                    for (int j = 0; j < optionList.size(); j++) {
                        AppGoodsSkuOption option = optionList.get(j);
                        option.setGoodsId(appGoods.getGoodsId());
                        option.setSkuId(sku.getSkuId());
                        option.setOptionSort(j + 1);
                        option.setCreateTime(DateUtils.getNowDate());
                        appGoodsSkuOptionMapper.insertAppGoodsSkuOption(option);
                    }
                }
            }
        }
        List<AppGoodsRelated> features = appGoods.getFeatures();
        if(null!=features){
            for(int i = 0; i < features.size(); i++){
                AppGoodsRelated feature = features.get(i);
                feature.setGoodsId(appGoods.getGoodsId());
                feature.setSortOrder(i+1);
                feature.setCreateTime(DateUtils.getNowDate());
                appGoodsRelatedMapper.insertAppGoodsRelated(feature);
            }
        }
        saveEducationExt(appGoods);
        return 1;
    }

    /**
     * 修改商品
     * 
     * @param appGoods 商品
     * @return 结果
     */
    @Override
    public int updateAppGoods(AppGoods appGoods)
    {
        int retint = appGoodsMapper.updateAppGoods(appGoods);
        List<AppGoodsSku> skuList = appGoods.getOptionList();
        List<AppGoodsSkuOption> optionList;
        Map skuidmap = new HashMap();
        if(null!=skuList && skuList.size()>0) {
            if (skuList != null && !skuList.isEmpty()) {
                skuList.sort((sku1, sku2) -> {
                    if (sku1.getSkuType() == null) return -1;
                    if (sku2.getSkuType() == null) return 1;
                    return sku1.getSkuType().compareTo(sku2.getSkuType());
                });
            }
            for (int i = 0; i < skuList.size(); i++) {
                AppGoodsSku sku = skuList.get(i);
                if(null!=sku.getSkuId() && sku.getSkuId()>0){
                    if(null!=sku.getTmpParSkuId() && sku.getTmpParSkuId().startsWith("temp_")){
                        if(skuidmap.containsKey(sku.getTmpParSkuId())){
                            sku.setParSkuId(skuidmap.get(sku.getTmpParSkuId()).toString());
                        }
                    }
                    appGoodsSkuMapper.updateAppGoodsSku(sku);
                    if(sku.getTmpSkuId()!=null && sku.getTmpSkuId().startsWith("temp_")){
                        skuidmap.put(sku.getTmpSkuId(), sku.getSkuId());
                    }
                }else{
                    sku.setGoodsId(appGoods.getGoodsId());
                    sku.setSortOrder(i + 1);
                    sku.setCreateTime(DateUtils.getNowDate());
                    if(null!=sku.getTmpParSkuId() && sku.getTmpParSkuId().startsWith("temp_")){
                        if(skuidmap.containsKey(sku.getTmpParSkuId())){
                            sku.setParSkuId(skuidmap.get(sku.getTmpParSkuId()).toString());
                        }
                    }
                    appGoodsSkuMapper.insertAppGoodsSku(sku);
                    if(sku.getTmpSkuId()!=null && sku.getTmpSkuId().startsWith("temp_")){
                        skuidmap.put(sku.getTmpSkuId(), sku.getSkuId());
                    }
                    optionList = sku.getOptions();
                    if (null != optionList && optionList.size() > 0) {
                        for (int j = 0; j < optionList.size(); j++) {
                            AppGoodsSkuOption option = optionList.get(j);
                            option.setGoodsId(appGoods.getGoodsId());
                            option.setSkuId(sku.getSkuId());
                            option.setOptionSort(j + 1);
                            option.setCreateTime(DateUtils.getNowDate());
                            appGoodsSkuOptionMapper.insertAppGoodsSkuOption(option);
                        }
                    }
                }

                optionList = sku.getOptions();
                if (null != optionList && optionList.size() > 0) {
                    for (int j = 0; j < optionList.size(); j++) {
                        AppGoodsSkuOption option = optionList.get(j);
                        if(null!=option.getOptionId() && option.getOptionId()>0) {
                            appGoodsSkuOptionMapper.updateAppGoodsSkuOption(option);
                        }else{
                            option.setGoodsId(appGoods.getGoodsId());
                            option.setSkuId(sku.getSkuId());
                            option.setOptionSort(j + 1);
                            option.setCreateTime(DateUtils.getNowDate());
                            appGoodsSkuOptionMapper.insertAppGoodsSkuOption(option);
                        }
                    }
                }
            }
        }
        List<AppGoodsRelated> features = appGoods.getFeatures();
        appGoodsRelatedMapper.deleteByProductId(appGoods.getGoodsId());
        if(null!=features){
            for(int i = 0; i < features.size(); i++){
                AppGoodsRelated feature = features.get(i);
                feature.setGoodsId(appGoods.getGoodsId());
                feature.setSortOrder(i+1);
                feature.setCreateTime(DateUtils.getNowDate());
                appGoodsRelatedMapper.insertAppGoodsRelated(feature);
            }
        }
        saveEducationExt(appGoods);
        return retint;
    }

    /**
     * 批量删除商品
     * 
     * @param goodsIds 需要删除的商品主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsByGoodsIds(Long[] goodsIds)
    {
        return appGoodsMapper.deleteAppGoodsByGoodsIds(goodsIds);
    }

    /**
     * 删除商品信息
     * 
     * @param goodsId 商品主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsByGoodsId(Long goodsId)
    {
        return appGoodsMapper.deleteAppGoodsByGoodsId(goodsId);
    }

    @Override
    public AppGoods getCacheAppGoodsById(Long goodsId) {
        AppGoods appGoods = SpringUtils.getBean(RedisCache.class).getCacheObject(CacheConstants.APP_GOODS+"id:"+goodsId);
        if(null==appGoods){
            appGoods = appGoodsMapper.selectAppGoodsByGoodsId(goodsId);
            if(null!=appGoods){
                SpringUtils.getBean(RedisCache.class).setCacheObject(CacheConstants.APP_GOODS+"id:"+goodsId, appGoods);
            }
        }
        return appGoods;
    }

    private void saveEducationExt(AppGoods appGoods) {
        if (!"education".equals(appGoods.getGoodsType())) {
            return;
        }
        AppGoodsEducationExt ext = appGoods.getEducationExt();
        if (ext == null) {
            return;
        }
        ext.setGoodsId(appGoods.getGoodsId());
        AppGoodsEducationExt existing = appGoodsEducationExtMapper.selectAppGoodsEducationExtByGoodsId(appGoods.getGoodsId());
        if (existing != null) {
            appGoodsEducationExtMapper.updateAppGoodsEducationExt(ext);
        } else {
            appGoodsEducationExtMapper.insertAppGoodsEducationExt(ext);
        }
    }
}
