package com.ruoyi.system.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.system.domain.AppGoodsCart;

/**
 * 用户购物车Mapper接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface AppGoodsCartMapper extends BaseMapper<AppGoodsCart>
{
    /**
     * 查询用户购物车
     * 
     * @param cartId 用户购物车主键
     * @return 用户购物车
     */
    public AppGoodsCart selectAppGoodsCartByCartId(Long cartId);

    /**
     * 查询用户购物车列表
     * 
     * @param appGoodsCart 用户购物车
     * @return 用户购物车集合
     */
    public List<AppGoodsCart> selectAppGoodsCartList(AppGoodsCart appGoodsCart);

    /**
     * 新增用户购物车
     * 
     * @param appGoodsCart 用户购物车
     * @return 结果
     */
    public int insertAppGoodsCart(AppGoodsCart appGoodsCart);

    /**
     * 修改用户购物车
     * 
     * @param appGoodsCart 用户购物车
     * @return 结果
     */
    public int updateAppGoodsCart(AppGoodsCart appGoodsCart);

    /**
     * 删除用户购物车
     * 
     * @param cartId 用户购物车主键
     * @return 结果
     */
    public int deleteAppGoodsCartByCartId(Long cartId);

    /**
     * 批量删除用户购物车
     * 
     * @param cartIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAppGoodsCartByCartIds(Long[] cartIds);
}
