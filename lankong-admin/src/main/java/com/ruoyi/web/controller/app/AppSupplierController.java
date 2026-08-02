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
import com.ruoyi.system.domain.AppSupplier;
import com.ruoyi.system.service.IAppSupplierService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 供应商Controller
 * 
 * @author lankong
 * @date 2025-05-21
 */
@RestController
@RequestMapping("/system/app_supplier")
@Api(tags = "供应商管理")
public class AppSupplierController extends BaseController
{
    @Autowired
    private IAppSupplierService appSupplierService;

    /**
     * 查询供应商列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询供应商列表")
    public TableDataInfo list(AppSupplier appSupplier)
    {
        startPage();
        List<AppSupplier> list = appSupplierService.selectAppSupplierList(appSupplier);
        return getDataTable(list);
    }

    /**
     * 导出供应商列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:export')")
    @Log(title = "供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出供应商列表")
    public void export(HttpServletResponse response, AppSupplier appSupplier)
    {
        List<AppSupplier> list = appSupplierService.selectAppSupplierList(appSupplier);
        ExcelUtil<AppSupplier> util = new ExcelUtil<AppSupplier>(AppSupplier.class);
        util.exportExcel(response, list, "供应商数据");
    }

    /**
     * 获取供应商详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:query')")
    @GetMapping(value = "/{supplierId}")
   
    @ApiOperation("获取供应商详细信息")
    public AjaxResult getInfo(@PathVariable("supplierId") Long supplierId)
    {
        return success(appSupplierService.selectAppSupplierBySupplierId(supplierId));
    }

    /**
     * 新增供应商
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:add')")
    @Log(title = "供应商", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增供应商")
    public AjaxResult add(@RequestBody AppSupplier appSupplier)
    {
        return toAjax(appSupplierService.insertAppSupplier(appSupplier));
    }

    /**
     * 修改供应商
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:edit')")
    @Log(title = "供应商", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改供应商")
    public AjaxResult edit(@RequestBody AppSupplier appSupplier)
    {
        return toAjax(appSupplierService.updateAppSupplier(appSupplier));
    }

    /**
     * 删除供应商
     */
    @PreAuthorize("@ss.hasPermi('system:app_supplier:remove')")
    @Log(title = "供应商", businessType = BusinessType.DELETE)
	@DeleteMapping("/{supplierIds}")
   
    @ApiOperation("删除供应商")
    public AjaxResult remove(@PathVariable Long[] supplierIds)
    {
        return toAjax(appSupplierService.deleteAppSupplierBySupplierIds(supplierIds));
    }
}
