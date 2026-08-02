package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.AppSupplier;
import com.ruoyi.system.service.*;
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
import com.ruoyi.system.domain.AppCustomerIncome;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 收入明细Controller
 * 
 * @author lankong
 * @date 2025-05-14
 */
@RestController
@RequestMapping("/system/app_customer_income")
@Api(tags = "收入明细管理")
public class AppCustomerIncomeController extends BaseController
{
    @Autowired
    private IAppCustomerIncomeService appCustomerIncomeService;
    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private IAppConsultantService consultantService;
    @Autowired
    private IAppSupplierService appSupplierService;
    @Autowired
    private ISysUserService sysUserService;
    /**
     * 查询收入明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询收入明细列表")
    public TableDataInfo list(AppCustomerIncome appCustomerIncome)
    {
        startPage();
        List<AppCustomerIncome> list = appCustomerIncomeService.selectAppCustomerIncomeList(appCustomerIncome);
        if(null!=list){
            SysDept dept = null;
            AppConsultant consultant = null;
            AppSupplier supplier = null;
            SysUser user = null;
            for (int i = 0; i < list.size(); i++) {
                dept = deptService.getCacheDeptById(list.get(i).getDeptId());
                if (dept != null) {
                    list.get(i).setDepartName(dept.getDeptName());
                }
                consultant = consultantService.getCacheConsultant(list.get(i).getConsultantId());
                if (consultant != null) {
                    list.get(i).setConsultantName(consultant.getConsultantName());
                }
                supplier = appSupplierService.getCacheSupplier(list.get(i).getSupplierId());
                if (supplier != null) {
                    list.get(i).setSupplierName(supplier.getSupplierName());
                }
                user = sysUserService.getCacheUserById(list.get(i).getUserId());
                if(null!=user){
                    list.get(i).setUserName(user.getUserName());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 查询收入明细统计
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:list')")
    @GetMapping("/stat")
   
    @ApiOperation("查询收入明细统计")
    public AjaxResult stat(AppCustomerIncome appCustomerIncome)
    {
        Map<String, Object> rs = new HashMap<>();
        rs = appCustomerIncomeService.statAppCustomerIncome(appCustomerIncome);
        return AjaxResult.success(rs);
    }

    /**
     * 导出收入明细列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:export')")
    @Log(title = "收入明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出收入明细列表")
    public void export(HttpServletResponse response, AppCustomerIncome appCustomerIncome)
    {
        List<AppCustomerIncome> list = appCustomerIncomeService.selectAppCustomerIncomeList(appCustomerIncome);
        ExcelUtil<AppCustomerIncome> util = new ExcelUtil<AppCustomerIncome>(AppCustomerIncome.class);
        util.exportExcel(response, list, "收入明细数据");
    }

    /**
     * 获取收入明细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:query')")
    @GetMapping(value = "/{incomeId}")
   
    @ApiOperation("获取收入明细详细信息")
    public AjaxResult getInfo(@PathVariable("incomeId") Long incomeId)
    {
        return success(appCustomerIncomeService.selectAppCustomerIncomeByIncomeId(incomeId));
    }

    /**
     * 新增收入明细
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:add')")
    @Log(title = "收入明细", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增收入明细")
    public AjaxResult add(@RequestBody AppCustomerIncome appCustomerIncome)
    {
        appCustomerIncome.setUserId(getUserId());
        appCustomerIncome.setIncomeNo(DateUtils.dateTimeNow());
        return toAjax(appCustomerIncomeService.insertAppCustomerIncome(appCustomerIncome));
    }

    /**
     * 修改收入明细
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:edit')")
    @Log(title = "收入明细", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改收入明细")
    public AjaxResult edit(@RequestBody AppCustomerIncome appCustomerIncome)
    {
        return toAjax(appCustomerIncomeService.updateAppCustomerIncome(appCustomerIncome));
    }

    /**
     * 删除收入明细
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer_income:remove')")
    @Log(title = "收入明细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{incomeIds}")
   
    @ApiOperation("删除收入明细")
    public AjaxResult remove(@PathVariable Long[] incomeIds)
    {
        return toAjax(appCustomerIncomeService.deleteAppCustomerIncomeByIncomeIds(incomeIds));
    }
}
