package com.ruoyi.system.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.system.domain.AppGoodsCart;

/**
 * 用户购物车Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsCartService extends IService<AppGoodsCart>
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
     * 批量删除用户购物车
     * 
     * @param cartIds 需要删除的用户购物车主键集合
     * @return 结果
     */
    public int deleteAppGoodsCartByCartIds(Long[] cartIds);

    /**
     * 删除用户购物车信息
     * 
     * @param cartId 用户购物车主键
     * @return 结果
     */
    public int deleteAppGoodsCartByCartId(Long cartId);
}
