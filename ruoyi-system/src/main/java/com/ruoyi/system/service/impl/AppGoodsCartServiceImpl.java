package com.ruoyi.system.service.impl;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppGoodsCartMapper;
import com.ruoyi.system.domain.AppGoodsCart;
import com.ruoyi.system.service.IAppGoodsCartService;

/**
 * 用户购物车Service业务层处理
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Service
public class AppGoodsCartServiceImpl extends ServiceImpl<AppGoodsCartMapper, AppGoodsCart> implements IAppGoodsCartService
{
    @Autowired
    private AppGoodsCartMapper appGoodsCartMapper;

    /**
     * 查询用户购物车
     * 
     * @param cartId 用户购物车主键
     * @return 用户购物车
     */
    @Override
    public AppGoodsCart selectAppGoodsCartByCartId(Long cartId)
    {
        return appGoodsCartMapper.selectAppGoodsCartByCartId(cartId);
    }

    /**
     * 查询用户购物车列表
     * 
     * @param appGoodsCart 用户购物车
     * @return 用户购物车
     */
    @Override
    public List<AppGoodsCart> selectAppGoodsCartList(AppGoodsCart appGoodsCart)
    {
        return appGoodsCartMapper.selectAppGoodsCartList(appGoodsCart);
    }

    /**
     * 新增用户购物车
     * 
     * @param appGoodsCart 用户购物车
     * @return 结果
     */
    @Override
    public int insertAppGoodsCart(AppGoodsCart appGoodsCart)
    {
        appGoodsCart.setCreateTime(DateUtils.getNowDate());
        return appGoodsCartMapper.insertAppGoodsCart(appGoodsCart);
    }

    /**
     * 修改用户购物车
     * 
     * @param appGoodsCart 用户购物车
     * @return 结果
     */
    @Override
    public int updateAppGoodsCart(AppGoodsCart appGoodsCart)
    {
        appGoodsCart.setUpdateTime(DateUtils.getNowDate());
        return appGoodsCartMapper.updateAppGoodsCart(appGoodsCart);
    }

    /**
     * 批量删除用户购物车
     * 
     * @param cartIds 需要删除的用户购物车主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCartByCartIds(Long[] cartIds)
    {
        return appGoodsCartMapper.deleteAppGoodsCartByCartIds(cartIds);
    }

    /**
     * 删除用户购物车信息
     * 
     * @param cartId 用户购物车主键
     * @return 结果
     */
    @Override
    public int deleteAppGoodsCartByCartId(Long cartId)
    {
        return appGoodsCartMapper.deleteAppGoodsCartByCartId(cartId);
    }
}
