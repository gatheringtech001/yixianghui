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
import com.ruoyi.system.domain.AppGoodsSkuOption;
import com.ruoyi.system.service.IAppGoodsSkuOptionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 属性选项Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_sku_option")
@Api(tags = "属性选项管理")
public class AppGoodsSkuOptionController extends BaseController
{
    @Autowired
    private IAppGoodsSkuOptionService appGoodsSkuOptionService;

    /**
     * 查询属性选项列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询属性选项列表")
    public TableDataInfo list(AppGoodsSkuOption appGoodsSkuOption)
    {
        startPage();
        List<AppGoodsSkuOption> list = appGoodsSkuOptionService.selectAppGoodsSkuOptionList(appGoodsSkuOption);
        return getDataTable(list);
    }

    /**
     * 导出属性选项列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:export')")
    @Log(title = "属性选项", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出属性选项列表")
    public void export(HttpServletResponse response, AppGoodsSkuOption appGoodsSkuOption)
    {
        List<AppGoodsSkuOption> list = appGoodsSkuOptionService.selectAppGoodsSkuOptionList(appGoodsSkuOption);
        ExcelUtil<AppGoodsSkuOption> util = new ExcelUtil<AppGoodsSkuOption>(AppGoodsSkuOption.class);
        util.exportExcel(response, list, "属性选项数据");
    }

    /**
     * 获取属性选项详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:query')")
    @GetMapping(value = "/{optionId}")
   
    @ApiOperation("获取属性选项详细信息")
    public AjaxResult getInfo(@PathVariable("optionId") Long optionId)
    {
        return success(appGoodsSkuOptionService.selectAppGoodsSkuOptionByOptionId(optionId));
    }

    /**
     * 新增属性选项
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:add')")
    @Log(title = "属性选项", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增属性选项")
    public AjaxResult add(@RequestBody AppGoodsSkuOption appGoodsSkuOption)
    {
        return toAjax(appGoodsSkuOptionService.insertAppGoodsSkuOption(appGoodsSkuOption));
    }

    /**
     * 修改属性选项
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:edit')")
    @Log(title = "属性选项", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改属性选项")
    public AjaxResult edit(@RequestBody AppGoodsSkuOption appGoodsSkuOption)
    {
        return toAjax(appGoodsSkuOptionService.updateAppGoodsSkuOption(appGoodsSkuOption));
    }

    /**
     * 删除属性选项
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_option:remove')")
    @Log(title = "属性选项", businessType = BusinessType.DELETE)
	@DeleteMapping("/{optionIds}")
   
    @ApiOperation("删除属性选项")
    public AjaxResult remove(@PathVariable Long[] optionIds)
    {
        return toAjax(appGoodsSkuOptionService.deleteAppGoodsSkuOptionByOptionIds(optionIds));
    }
}
