package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.service.IAppConsultantService;
import com.ruoyi.system.service.ISysDeptService;
import io.swagger.annotations.ApiOperation;
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
import com.ruoyi.system.domain.AppCustomer;
import com.ruoyi.system.service.IAppCustomerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 客户资料Controller
 * 
 * @author lankong
 * @date 2025-05-07
 */
@RestController
@RequestMapping("/system/app_customer")
@Api(tags = "客户资料管理")
public class AppCustomerController extends BaseController
{
    @Autowired
    private IAppCustomerService appCustomerService;
    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private IAppConsultantService consultantService;

    /**
     * 查询客户资料列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询客户资料列表")
    public TableDataInfo list(AppCustomer appCustomer)
    {
        startPage();
        List<AppCustomer> list = appCustomerService.selectAppCustomerList(appCustomer);
        return getDataTable(list);
    }
    @PreAuthorize("@ss.hasPermi('system:app_customer:list')")
    @GetMapping("/alllist")

    @ApiOperation("查询全部列表")
    public TableDataInfo alllist(AppCustomer appCustomer)
    {
        List<AppCustomer> list = appCustomerService.selectAppCustomerList(appCustomer);
        return getDataTable(list);
    }

    /**
     * 导出客户资料列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:export')")
    @Log(title = "客户资料", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出客户资料列表")
    public void export(HttpServletResponse response, AppCustomer appCustomer)
    {
        List<AppCustomer> list = appCustomerService.selectAppCustomerList(appCustomer);
        for (int i = 0; i < list.size(); i++) {
            AppConsultant consultant = consultantService.selectAppConsultantByConsultantId(list.get(i).getConsultantId());
            if (consultant != null) {
                list.get(i).setConsultantName(consultant.getConsultantName());
            }
            SysDept dept = deptService.selectDeptById(list.get(i).getDeptId());
            if (dept != null) {
                list.get(i).setDeptName(dept.getDeptName());
            }
        }
        ExcelUtil<AppCustomer> util = new ExcelUtil<AppCustomer>(AppCustomer.class);
        util.exportExcel(response, list, "客户资料数据");
    }

    /**
     * 导入客户资料
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @Log(title = "客户资料管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:app_customer:import')")
    @PostMapping("/importData")
   
    @ApiOperation("导入客户资料")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<AppCustomer> util = new ExcelUtil<AppCustomer>(AppCustomer.class);
        List<AppCustomer> customerList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = appCustomerService.importCustomer(customerList, updateSupport, operName);
        return success(message);
    }

    /**
     * 导入模板
     * @param response
     */
    @PostMapping("/importTemplate")
   
    @ApiOperation("导入模板")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<AppCustomer> util = new ExcelUtil<AppCustomer>(AppCustomer.class);
        util.importTemplateExcel(response, "客户数据");
    }

    /**
     * 获取客户资料详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:query')")
    @GetMapping(value = "/{customerId}")
   
    @ApiOperation("获取客户资料详细信息")
    public AjaxResult getInfo(@PathVariable("customerId") Long customerId)
    {
        AppCustomer appCustomer = appCustomerService.selectAppCustomerByCustomerId(customerId);
        if(null != appCustomer){
            SysDept dept = deptService.selectDeptById(appCustomer.getDeptId());
            if(null!=dept){
                appCustomer.setDeptName(dept.getDeptName());
            }
            if (appCustomer.getConsultantId() != null) {
                appCustomer.setConsultantName(consultantService.selectAppConsultantNameById(appCustomer.getConsultantId()));
            }
        }
        return success(appCustomer);
    }

    /**
     * 新增客户资料
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:add')")
    @Log(title = "客户资料", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增客户资料")
    public AjaxResult add(@RequestBody AppCustomer appCustomer)
    {
        return toAjax(appCustomerService.insertAppCustomer(appCustomer));
    }

    /**
     * 修改客户资料
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:edit')")
    @Log(title = "客户资料", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改客户资料")
    public AjaxResult edit(@RequestBody AppCustomer appCustomer)
    {
        return toAjax(appCustomerService.updateAppCustomer(appCustomer));
    }

    /**
     * 删除客户资料
     */
    @PreAuthorize("@ss.hasPermi('system:app_customer:remove')")
    @Log(title = "客户资料", businessType = BusinessType.DELETE)
	@DeleteMapping("/{customerIds}")
   
    @ApiOperation("删除客户资料")
    public AjaxResult remove(@PathVariable Long[] customerIds)
    {
        return toAjax(appCustomerService.deleteAppCustomerByCustomerIds(customerIds));
    }
}
