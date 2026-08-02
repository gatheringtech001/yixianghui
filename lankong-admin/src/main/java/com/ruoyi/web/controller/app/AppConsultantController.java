package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.AppCustomer;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;
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
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.service.IAppConsultantService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 康养顾问Controller
 * 
 * @author lankong
 * @date 2025-05-14
 */
@RestController
@RequestMapping("/system/app_consultant")
@Api(tags = "康养顾问管理")
public class AppConsultantController extends BaseController
{
    @Autowired
    private IAppConsultantService appConsultantService;
    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询康养顾问列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询康养顾问列表")
    public TableDataInfo list(AppConsultant appConsultant)
    {
        startPage();
        List<AppConsultant> list = appConsultantService.selectAppConsultantList(appConsultant);
        if(null!=list){
            for(int i=0;i<list.size();i++){
                list.get(i).setDeptName(deptService.getCacheDeptNameById(list.get(i).getDeptId()));
                if (list.get(i).getUserId() != null) {
                    SysUser user = sysUserService.selectUserById(list.get(i).getUserId());
                    if (user != null) {
                        list.get(i).setUserNickName(user.getNickName());
                    }
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出康养顾问列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:export')")
    @Log(title = "康养顾问", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出康养顾问列表")
    public void export(HttpServletResponse response, AppConsultant appConsultant)
    {
        List<AppConsultant> list = appConsultantService.selectAppConsultantList(appConsultant);
        for (int i = 0; i < list.size(); i++) {
            SysDept dept = deptService.selectDeptById(list.get(i).getDeptId());
            if (dept != null) {
                list.get(i).setDeptName(dept.getDeptName());
            }
        }
        ExcelUtil<AppConsultant> util = new ExcelUtil<AppConsultant>(AppConsultant.class);
        util.exportExcel(response, list, "康养顾问数据");
    }

    /**
     * 导入顾问资料
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @Log(title = "顾问管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:app_consultant:import')")
    @PostMapping("/importData")
   
    @ApiOperation("导入顾问资料")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<AppConsultant> util = new ExcelUtil<AppConsultant>(AppConsultant.class);
        List<AppConsultant> consultantList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = appConsultantService.importConsultant(consultantList, updateSupport, operName);
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
        ExcelUtil<AppConsultant> util = new ExcelUtil<AppConsultant>(AppConsultant.class);
        util.importTemplateExcel(response, "顾问数据");
    }

    /**
     * 获取康养顾问详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:query')")
    @GetMapping(value = "/{consultantId}")
   
    @ApiOperation("获取康养顾问详细信息")
    public AjaxResult getInfo(@PathVariable("consultantId") Long consultantId)
    {
        AppConsultant consultant = appConsultantService.selectAppConsultantByConsultantId(consultantId);
        if (consultant != null && consultant.getUserId() != null)
        {
            SysUser user = sysUserService.selectUserById(consultant.getUserId());
            if (user != null)
            {
                consultant.setUserNickName(user.getNickName());
            }
        }
        return success(consultant);
    }

    /**
     * 新增康养顾问
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:add')")
    @Log(title = "康养顾问", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增康养顾问")
    public AjaxResult add(@RequestBody AppConsultant appConsultant)
    {
        appConsultant.setUserId(null);
        return toAjax(appConsultantService.insertAppConsultant(appConsultant));
    }

    /**
     * 修改康养顾问
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:edit')")
    @Log(title = "康养顾问", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改康养顾问")
    public AjaxResult edit(@RequestBody AppConsultant appConsultant)
    {
        AppConsultant db = appConsultantService.selectAppConsultantByConsultantId(appConsultant.getConsultantId());
        if (db != null)
        {
            appConsultant.setUserId(db.getUserId());
        }
        return toAjax(appConsultantService.updateAppConsultant(appConsultant));
    }

    /**
     * 删除康养顾问
     */
    @PreAuthorize("@ss.hasPermi('system:app_consultant:remove')")
    @Log(title = "康养顾问", businessType = BusinessType.DELETE)
	@DeleteMapping("/{consultantIds}")
   
    @ApiOperation("删除康养顾问")
    public AjaxResult remove(@PathVariable Long[] consultantIds)
    {
        return toAjax(appConsultantService.deleteAppConsultantByConsultantIds(consultantIds));
    }
}
