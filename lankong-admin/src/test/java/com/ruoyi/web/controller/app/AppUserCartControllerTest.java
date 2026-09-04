package com.ruoyi.web.controller.app;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsCart;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppUserAddress;
import com.ruoyi.system.service.IAppGoodsCartService;
import com.ruoyi.system.service.IAppGoodsOrderService;
import com.ruoyi.system.service.IAppGoodsService;
import com.ruoyi.system.service.IAppUserAddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppUserCartControllerTest
{
    private static final long CURRENT_USER_ID = 7L;
    private IAppGoodsCartService cartService;
    private IAppGoodsService goodsService;
    private IAppGoodsOrderService orderService;
    private IAppUserAddressService addressService;
    private AppUserController controller;

    @BeforeEach
    void setUp()
    {
        cartService = mock(IAppGoodsCartService.class);
        goodsService = mock(IAppGoodsService.class);
        orderService = mock(IAppGoodsOrderService.class);
        addressService = mock(IAppUserAddressService.class);
        controller = new AppUserController()
        {
            @Override
            public Long getUserId()
            {
                return CURRENT_USER_ID;
            }
        };
        ReflectionTestUtils.setField(controller, "cartService", cartService);
        ReflectionTestUtils.setField(controller, "goodsService", goodsService);
        ReflectionTestUtils.setField(controller, "goodsOrderService", orderService);
        ReflectionTestUtils.setField(controller, "userAddressService", addressService);
    }

    @Test
    void addCreatesOwnedCartItemForPublishedOnlineGoods()
    {
        when(goodsService.selectAppGoodsByGoodsId(31L)).thenReturn(goods(31L, 8L));
        when(cartService.insertAppGoodsCart(any(AppGoodsCart.class))).thenReturn(1);
        AppGoodsCart request = cart(null, null);
        request.setGoodsId(31L);
        request.setGoodsCount(2);

        AjaxResult result = controller.cart_add(request);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        assertEquals(CURRENT_USER_ID, request.getUserId());
        assertEquals(0L, request.getIsSku());
        assertEquals(0L, request.getDataId());
        assertEquals("1", request.getStatus());
        verify(cartService).insertAppGoodsCart(request);
    }

    @Test
    void addMergesSameGoodsAndRejectsCountBeyondStock()
    {
        when(goodsService.selectAppGoodsByGoodsId(31L)).thenReturn(goods(31L, 3L));
        AppGoodsCart existing = cart(21L, CURRENT_USER_ID);
        existing.setGoodsId(31L);
        existing.setGoodsCount(2);
        existing.setIsSku(0L);
        existing.setDataId(0L);
        when(cartService.selectAppGoodsCartList(any(AppGoodsCart.class)))
                .thenReturn(java.util.Collections.singletonList(existing));
        AppGoodsCart request = cart(null, null);
        request.setGoodsId(31L);
        request.setGoodsCount(2);

        AjaxResult result = controller.cart_add(request);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("商品库存不足", result.get(AjaxResult.MSG_TAG));
        assertEquals(2, existing.getGoodsCount());
        verify(cartService, never()).updateAppGoodsCart(existing);
        verify(cartService, never()).insertAppGoodsCart(request);
    }

    @Test
    void addMergesSameGoodsWithinStock()
    {
        when(goodsService.selectAppGoodsByGoodsId(31L)).thenReturn(goods(31L, 4L));
        AppGoodsCart existing = cart(21L, CURRENT_USER_ID);
        existing.setGoodsId(31L);
        existing.setGoodsCount(2);
        existing.setIsSku(0L);
        existing.setDataId(0L);
        when(cartService.selectAppGoodsCartList(any(AppGoodsCart.class)))
                .thenReturn(java.util.Collections.singletonList(existing));
        when(cartService.updateAppGoodsCart(existing)).thenReturn(1);
        AppGoodsCart request = cart(null, null);
        request.setGoodsId(31L);
        request.setGoodsCount(2);

        AjaxResult result = controller.cart_add(request);

        assertEquals(200, result.get(AjaxResult.CODE_TAG));
        assertEquals(4, existing.getGoodsCount());
        verify(cartService).updateAppGoodsCart(existing);
        verify(cartService, never()).insertAppGoodsCart(request);
    }

    @Test
    void editRejectsCountBeyondCurrentStock()
    {
        AppGoodsCart stored = cart(21L, CURRENT_USER_ID);
        stored.setGoodsId(31L);
        when(cartService.selectAppGoodsCartByCartId(21L)).thenReturn(stored);
        when(goodsService.selectAppGoodsByGoodsId(31L)).thenReturn(goods(31L, 3L));
        AppGoodsCart update = cart(21L, null);
        update.setGoodsCount(4);

        AjaxResult result = controller.cart_edit(update);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("商品库存不足", result.get(AjaxResult.MSG_TAG));
        verify(cartService, never()).updateAppGoodsCart(stored);
    }

    @Test
    void editRejectsCartOwnedByAnotherUser()
    {
        AppGoodsCart stored = cart(21L, 99L);
        when(cartService.selectAppGoodsCartByCartId(21L)).thenReturn(stored);

        AppGoodsCart update = cart(21L, null);
        update.setGoodsCount(2);
        AjaxResult result = controller.cart_edit(update);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("购物车商品不存在", result.get(AjaxResult.MSG_TAG));
        verify(cartService, never()).updateAppGoodsCart(update);
    }

    @Test
    void deleteRejectsCartOwnedByAnotherUser()
    {
        when(cartService.selectAppGoodsCartByCartId(21L)).thenReturn(cart(21L, 99L));

        AjaxResult result = controller.cart_remove(21L);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("购物车商品不存在", result.get(AjaxResult.MSG_TAG));
        verify(cartService, never()).deleteAppGoodsCartByCartId(21L);
    }

    @Test
    void orderRejectsOnlineGoodsAddressOwnedByAnotherUser()
    {
        when(goodsService.selectAppGoodsByGoodsId(31L)).thenReturn(goods(31L, 8L));
        AppUserAddress address = new AppUserAddress();
        address.setAddressId(41L);
        address.setUserId(99L);
        when(addressService.selectAppUserAddressByAddressId(41L)).thenReturn(address);
        AppGoodsOrder order = new AppGoodsOrder();
        order.setGoodsId(31L);
        order.setGoodsCount(1L);
        order.setAddressId(41L);

        AjaxResult result = controller.add_goods_order(order);

        assertEquals(500, result.get(AjaxResult.CODE_TAG));
        assertEquals("收货地址无效", result.get(AjaxResult.MSG_TAG));
        verify(orderService, never()).insertAppGoodsOrder(order);
    }

    private AppGoodsCart cart(Long cartId, Long userId)
    {
        AppGoodsCart cart = new AppGoodsCart();
        cart.setCartId(cartId);
        cart.setUserId(userId);
        return cart;
    }

    private AppGoods goods(Long goodsId, Long stock)
    {
        AppGoods goods = new AppGoods();
        goods.setGoodsId(goodsId);
        goods.setGoodsType("online");
        goods.setStatus("1");
        goods.setStock(stock);
        return goods;
    }
}
