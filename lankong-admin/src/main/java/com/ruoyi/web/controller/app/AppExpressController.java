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
import com.ruoyi.system.domain.AppExpress;
import com.ruoyi.system.service.IAppExpressService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 快递公司Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_express")
@Api(tags = "快递公司管理")
public class AppExpressController extends BaseController
{
    @Autowired
    private IAppExpressService appExpressService;

    /**
     * 查询快递公司列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询快递公司列表")
    public TableDataInfo list(AppExpress appExpress)
    {
        startPage();
        List<AppExpress> list = appExpressService.selectAppExpressList(appExpress);
        return getDataTable(list);
    }

    /**
     * 导出快递公司列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:export')")
    @Log(title = "快递公司", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出快递公司列表")
    public void export(HttpServletResponse response, AppExpress appExpress)
    {
        List<AppExpress> list = appExpressService.selectAppExpressList(appExpress);
        ExcelUtil<AppExpress> util = new ExcelUtil<AppExpress>(AppExpress.class);
        util.exportExcel(response, list, "快递公司数据");
    }

    /**
     * 获取快递公司详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:query')")
    @GetMapping(value = "/{expressId}")
   
    @ApiOperation("获取快递公司详细信息")
    public AjaxResult getInfo(@PathVariable("expressId") Long expressId)
    {
        return success(appExpressService.selectAppExpressByExpressId(expressId));
    }

    /**
     * 新增快递公司
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:add')")
    @Log(title = "快递公司", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增快递公司")
    public AjaxResult add(@RequestBody AppExpress appExpress)
    {
        return toAjax(appExpressService.insertAppExpress(appExpress));
    }

    /**
     * 修改快递公司
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:edit')")
    @Log(title = "快递公司", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改快递公司")
    public AjaxResult edit(@RequestBody AppExpress appExpress)
    {
        return toAjax(appExpressService.updateAppExpress(appExpress));
    }

    /**
     * 删除快递公司
     */
    @PreAuthorize("@ss.hasPermi('system:app_express:remove')")
    @Log(title = "快递公司", businessType = BusinessType.DELETE)
	@DeleteMapping("/{expressIds}")
   
    @ApiOperation("删除快递公司")
    public AjaxResult remove(@PathVariable Long[] expressIds)
    {
        return toAjax(appExpressService.deleteAppExpressByExpressIds(expressIds));
    }
}
