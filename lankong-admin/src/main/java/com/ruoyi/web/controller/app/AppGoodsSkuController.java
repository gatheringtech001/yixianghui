package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.system.service.IAppGoodsService;
import com.ruoyi.system.service.IAppGoodsSkuOptionService;
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
import com.ruoyi.system.domain.AppGoodsSku;
import com.ruoyi.system.service.IAppGoodsSkuService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品属性Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_sku")
@Api(tags = "商品属性管理")
public class AppGoodsSkuController extends BaseController
{
    @Autowired
    private IAppGoodsSkuService appGoodsSkuService;
    @Autowired
    private IAppGoodsSkuOptionService skuOptionService;
    @Autowired
    private IAppGoodsService appGoodsService;

    /**
     * 查询商品属性列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品属性列表")
    public TableDataInfo list(AppGoodsSku appGoodsSku)
    {
        startPage();
        List<AppGoodsSku> list = appGoodsSkuService.selectAppGoodsSkuList(appGoodsSku);
        if(null!=list) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setGoodsName(appGoodsService.getCacheAppGoodsById(list.get(i).getGoodsId()).getGoodsName());
                list.get(i).setOptions(skuOptionService.selectAppGoodsSkuOptionListBySkuId(list.get(i).getSkuId()));
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出商品属性列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:export')")
    @Log(title = "商品属性", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品属性列表")
    public void export(HttpServletResponse response, AppGoodsSku appGoodsSku)
    {
        List<AppGoodsSku> list = appGoodsSkuService.selectAppGoodsSkuList(appGoodsSku);
        ExcelUtil<AppGoodsSku> util = new ExcelUtil<AppGoodsSku>(AppGoodsSku.class);
        util.exportExcel(response, list, "商品属性数据");
    }

    /**
     * 获取商品属性详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:query')")
    @GetMapping(value = "/{skuId}")
   
    @ApiOperation("获取商品属性详细信息")
    public AjaxResult getInfo(@PathVariable("skuId") Long skuId)
    {
        return success(appGoodsSkuService.selectAppGoodsSkuBySkuId(skuId));
    }

    /**
     * 新增商品属性
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:add')")
    @Log(title = "商品属性", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商品属性")
    public AjaxResult add(@RequestBody AppGoodsSku appGoodsSku)
    {
        return toAjax(appGoodsSkuService.insertAppGoodsSku(appGoodsSku));
    }

    /**
     * 修改商品属性
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:edit')")
    @Log(title = "商品属性", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品属性")
    public AjaxResult edit(@RequestBody AppGoodsSku appGoodsSku)
    {
        return toAjax(appGoodsSkuService.updateAppGoodsSku(appGoodsSku));
    }

    /**
     * 删除商品属性
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku:remove')")
    @Log(title = "商品属性", businessType = BusinessType.DELETE)
	@DeleteMapping("/{skuIds}")
   
    @ApiOperation("删除商品属性")
    public AjaxResult remove(@PathVariable Long[] skuIds)
    {
        return toAjax(appGoodsSkuService.deleteAppGoodsSkuBySkuIds(skuIds));
    }
}
