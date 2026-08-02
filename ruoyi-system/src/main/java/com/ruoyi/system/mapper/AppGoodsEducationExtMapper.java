package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.AppGoodsEducationExt;

public interface AppGoodsEducationExtMapper
{
    AppGoodsEducationExt selectAppGoodsEducationExtByGoodsId(Long goodsId);

    int insertAppGoodsEducationExt(AppGoodsEducationExt ext);

    int updateAppGoodsEducationExt(AppGoodsEducationExt ext);

    int deleteAppGoodsEducationExtByGoodsId(Long goodsId);
}
