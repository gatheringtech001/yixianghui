package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.system.domain.AppGoods;
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
import com.ruoyi.system.domain.AppGoodsSkuData;
import com.ruoyi.system.service.IAppGoodsSkuDataService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 型号信息Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_sku_data")
@Api(tags = "型号信息管理")
public class AppGoodsSkuDataController extends BaseController
{
    @Autowired
    private IAppGoodsSkuDataService appGoodsSkuDataService;

    /**
     * 查询型号信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询型号信息列表")
    public TableDataInfo list(AppGoodsSkuData appGoodsSkuData)
    {
        startPage();
        List<AppGoodsSkuData> list = appGoodsSkuDataService.selectAppGoodsSkuDataList(appGoodsSkuData);
        return getDataTable(list);
    }

    /**
     * 导出型号信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:export')")
    @Log(title = "型号信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出型号信息列表")
    public void export(HttpServletResponse response, AppGoodsSkuData appGoodsSkuData)
    {
        List<AppGoodsSkuData> list = appGoodsSkuDataService.selectAppGoodsSkuDataList(appGoodsSkuData);
        ExcelUtil<AppGoodsSkuData> util = new ExcelUtil<AppGoodsSkuData>(AppGoodsSkuData.class);
        util.exportExcel(response, list, "型号信息数据");
    }

    /**
     * 获取型号信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:query')")
    @GetMapping(value = "/{dataId}")
   
    @ApiOperation("获取型号信息详细信息")
    public AjaxResult getInfo(@PathVariable("dataId") Long dataId)
    {
        return success(appGoodsSkuDataService.selectAppGoodsSkuDataByDataId(dataId));
    }

    /**
     * 新增型号信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:add')")
    @Log(title = "型号信息", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增型号信息")
    public AjaxResult add(@RequestBody AppGoodsSkuData appGoodsSkuData)
    {
        AppGoodsSkuData dataWhere = new AppGoodsSkuData();
        dataWhere.setGoodsId(appGoodsSkuData.getGoodsId());
        dataWhere.setOptionIds(appGoodsSkuData.getOptionIds());
        List<AppGoodsSkuData> lastData = appGoodsSkuDataService.selectAppGoodsSkuDataList(dataWhere);
        if (lastData.size() > 0) {
            return error("组合已存在，请先删除或直接修改！");
        }
        return toAjax(appGoodsSkuDataService.insertAppGoodsSkuData(appGoodsSkuData));
    }

    /**
     * 修改型号信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:edit')")
    @Log(title = "型号信息", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改型号信息")
    public AjaxResult edit(@RequestBody AppGoodsSkuData appGoodsSkuData)
    {
        AppGoodsSkuData dataWhere = new AppGoodsSkuData();
        dataWhere.setGoodsId(appGoodsSkuData.getGoodsId());
        dataWhere.setOptionIds(appGoodsSkuData.getOptionIds());
        List<AppGoodsSkuData> lastData = appGoodsSkuDataService.selectAppGoodsSkuDataList(dataWhere);
        if (lastData.size() > 0
                && lastData.get(0).getDataId().longValue() != appGoodsSkuData.getDataId().longValue()) {
            return error("组合重复，请修改！");
        }
        return toAjax(appGoodsSkuDataService.updateAppGoodsSkuData(appGoodsSkuData));
    }

    /**
     * 删除型号信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_sku_data:remove')")
    @Log(title = "型号信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{dataIds}")
   
    @ApiOperation("删除型号信息")
    public AjaxResult remove(@PathVariable Long[] dataIds)
    {
        return toAjax(appGoodsSkuDataService.deleteAppGoodsSkuDataByDataIds(dataIds));
    }
}
