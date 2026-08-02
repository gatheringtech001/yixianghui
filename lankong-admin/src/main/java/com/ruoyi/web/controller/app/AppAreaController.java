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
import com.ruoyi.system.domain.AppArea;
import com.ruoyi.system.service.IAppAreaService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 行政区域Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_area")
@Api(tags = "行政区域管理")
public class AppAreaController extends BaseController
{
    @Autowired
    private IAppAreaService appAreaService;

    /**
     * 查询行政区域列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询行政区域列表")
    public TableDataInfo list(AppArea appArea)
    {
        startPage();
        List<AppArea> list = appAreaService.selectAppAreaList(appArea);
        return getDataTable(list);
    }

    /**
     * 导出行政区域列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:export')")
    @Log(title = "行政区域", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出行政区域列表")
    public void export(HttpServletResponse response, AppArea appArea)
    {
        List<AppArea> list = appAreaService.selectAppAreaList(appArea);
        ExcelUtil<AppArea> util = new ExcelUtil<AppArea>(AppArea.class);
        util.exportExcel(response, list, "行政区域数据");
    }

    /**
     * 获取行政区域详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:query')")
    @GetMapping(value = "/{areaId}")
   
    @ApiOperation("获取行政区域详细信息")
    public AjaxResult getInfo(@PathVariable("areaId") Long areaId)
    {
        return success(appAreaService.selectAppAreaByAreaId(areaId));
    }

    /**
     * 新增行政区域
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:add')")
    @Log(title = "行政区域", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增行政区域")
    public AjaxResult add(@RequestBody AppArea appArea)
    {
        return toAjax(appAreaService.insertAppArea(appArea));
    }

    /**
     * 修改行政区域
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:edit')")
    @Log(title = "行政区域", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改行政区域")
    public AjaxResult edit(@RequestBody AppArea appArea)
    {
        return toAjax(appAreaService.updateAppArea(appArea));
    }

    /**
     * 删除行政区域
     */
    @PreAuthorize("@ss.hasPermi('system:app_area:remove')")
    @Log(title = "行政区域", businessType = BusinessType.DELETE)
	@DeleteMapping("/{areaIds}")
   
    @ApiOperation("删除行政区域")
    public AjaxResult remove(@PathVariable Long[] areaIds)
    {
        return toAjax(appAreaService.deleteAppAreaByAreaIds(areaIds));
    }
}
