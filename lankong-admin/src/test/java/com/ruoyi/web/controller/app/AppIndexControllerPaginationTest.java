package com.ruoyi.web.controller.app;

import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.service.IAppGoodsCategoryService;
import com.ruoyi.system.service.IAppGoodsService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppIndexControllerPaginationTest
{
    @Test
    void returnsTheCompleteCategoryTreeWithoutConsumingPagination()
    {
        IAppGoodsCategoryService categoryService = mock(IAppGoodsCategoryService.class);
        IAppGoodsService goodsService = mock(IAppGoodsService.class);
        AppIndexController controller = new AppIndexController()
        {
            @Override
            protected void startPage()
            {
                throw new AssertionError("完整商品栏目不应启动分页");
            }
        };
        ReflectionTestUtils.setField(controller, "goodsCategoryService", categoryService);
        ReflectionTestUtils.setField(controller, "goodsService", goodsService);
        when(categoryService.selectAppGoodsCategoryAllIdsById(33L)).thenAnswer(invocation -> {
            return "33,66,67";
        });
        when(goodsService.selectAppGoodsList(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            return Collections.emptyList();
        });
        AppGoods query = new AppGoods();
        query.setCategoryId(33L);

        controller.queryGoodsList(query);
    }
}
