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
import com.ruoyi.system.domain.AppUserInviter;
import com.ruoyi.system.service.IAppUserInviterService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 邀请记录Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_inviter")
@Api(tags = "邀请记录管理")
public class AppUserInviterController extends BaseController
{
    @Autowired
    private IAppUserInviterService appUserInviterService;

    /**
     * 查询邀请记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询邀请记录列表")
    public TableDataInfo list(AppUserInviter appUserInviter)
    {
        startPage();
        List<AppUserInviter> list = appUserInviterService.selectAppUserInviterList(appUserInviter);
        return getDataTable(list);
    }

    /**
     * 导出邀请记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:export')")
    @Log(title = "邀请记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出邀请记录列表")
    public void export(HttpServletResponse response, AppUserInviter appUserInviter)
    {
        List<AppUserInviter> list = appUserInviterService.selectAppUserInviterList(appUserInviter);
        ExcelUtil<AppUserInviter> util = new ExcelUtil<AppUserInviter>(AppUserInviter.class);
        util.exportExcel(response, list, "邀请记录数据");
    }

    /**
     * 获取邀请记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:query')")
    @GetMapping(value = "/{inviterId}")
   
    @ApiOperation("获取邀请记录详细信息")
    public AjaxResult getInfo(@PathVariable("inviterId") Long inviterId)
    {
        return success(appUserInviterService.selectAppUserInviterByInviterId(inviterId));
    }

    /**
     * 新增邀请记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:add')")
    @Log(title = "邀请记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增邀请记录")
    public AjaxResult add(@RequestBody AppUserInviter appUserInviter)
    {
        return toAjax(appUserInviterService.insertAppUserInviter(appUserInviter));
    }

    /**
     * 修改邀请记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:edit')")
    @Log(title = "邀请记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改邀请记录")
    public AjaxResult edit(@RequestBody AppUserInviter appUserInviter)
    {
        return toAjax(appUserInviterService.updateAppUserInviter(appUserInviter));
    }

    /**
     * 删除邀请记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_inviter:remove')")
    @Log(title = "邀请记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{inviterIds}")
   
    @ApiOperation("删除邀请记录")
    public AjaxResult remove(@PathVariable Long[] inviterIds)
    {
        return toAjax(appUserInviterService.deleteAppUserInviterByInviterIds(inviterIds));
    }
}
