package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AppGoodsCategory;
import com.ruoyi.system.service.IAppGoodsCategoryService;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 商品分类Controller
 * 
 * @author lankong
 * @date 2025-03-31
 */
@RestController
@RequestMapping("/system/app_goods_category")
@Api(tags = "商品分类管理")
public class AppGoodsCategoryController extends BaseController
{
    @Autowired
    private IAppGoodsCategoryService appGoodsCategoryService;

    /**
     * 查询商品分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品分类列表")
    public AjaxResult list(AppGoodsCategory appGoodsCategory)
    {
        List<AppGoodsCategory> list = appGoodsCategoryService.selectAppGoodsCategoryList(appGoodsCategory);
        if(null!=list) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setParentName(appGoodsCategoryService.getCacheAppGoodsCategoryById(list.get(i).getParentId()).getCategoryName());
            }
        }
        return success(list);
    }

    /**
     * 导出商品分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:export')")
    @Log(title = "商品分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品分类列表")
    public void export(HttpServletResponse response, AppGoodsCategory appGoodsCategory)
    {
        List<AppGoodsCategory> list = appGoodsCategoryService.selectAppGoodsCategoryList(appGoodsCategory);
        ExcelUtil<AppGoodsCategory> util = new ExcelUtil<AppGoodsCategory>(AppGoodsCategory.class);
        util.exportExcel(response, list, "商品分类数据");
    }

    /**
     * 获取商品分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:query')")
    @GetMapping(value = "/{categoryId}")
   
    @ApiOperation("获取商品分类详细信息")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(appGoodsCategoryService.selectAppGoodsCategoryByCategoryId(categoryId));
    }

    /**
     * 新增商品分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商品分类")
    public AjaxResult add(@RequestBody AppGoodsCategory appGoodsCategory)
    {
        return toAjax(appGoodsCategoryService.insertAppGoodsCategory(appGoodsCategory));
    }

    /**
     * 修改商品分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品分类")
    public AjaxResult edit(@RequestBody AppGoodsCategory appGoodsCategory)
    {
        return toAjax(appGoodsCategoryService.updateAppGoodsCategory(appGoodsCategory));
    }

    /**
     * 删除商品分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
	@DeleteMapping("/{categoryIds}")
   
    @ApiOperation("删除商品分类")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(appGoodsCategoryService.deleteAppGoodsCategoryByCategoryIds(categoryIds));
    }
}
