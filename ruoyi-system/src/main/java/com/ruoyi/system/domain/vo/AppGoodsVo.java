package com.ruoyi.system.domain.vo;

import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsRelated;
import com.ruoyi.system.domain.AppGoodsSku;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AppGoodsVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<AppGoodsSku> optionList = new ArrayList();
    private List<AppGoodsRelated> features = new ArrayList();
    private AppGoods goods;

    public List<AppGoodsSku> getOptionList() {
        return optionList;
    }

    public void setOptionList(List<AppGoodsSku> optionList) {
        this.optionList = optionList;
    }

    public List<AppGoodsRelated> getFeatures() {
        return features;
    }

    public void setFeatures(List<AppGoodsRelated> features) {
        this.features = features;
    }

    public AppGoods getGoods() {
        return goods;
    }

    public void setGoods(AppGoods goods) {
        this.goods = goods;
    }
}
