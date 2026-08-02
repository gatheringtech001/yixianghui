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
import com.ruoyi.system.domain.AppUserCash;
import com.ruoyi.system.service.IAppUserCashService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户提现Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_cash")
@Api(tags = "用户提现管理")
public class AppUserCashController extends BaseController
{
    @Autowired
    private IAppUserCashService appUserCashService;

    /**
     * 查询用户提现列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户提现列表")
    public TableDataInfo list(AppUserCash appUserCash)
    {
        startPage();
        List<AppUserCash> list = appUserCashService.selectAppUserCashList(appUserCash);
        return getDataTable(list);
    }

    /**
     * 导出用户提现列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:export')")
    @Log(title = "用户提现", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户提现列表")
    public void export(HttpServletResponse response, AppUserCash appUserCash)
    {
        List<AppUserCash> list = appUserCashService.selectAppUserCashList(appUserCash);
        ExcelUtil<AppUserCash> util = new ExcelUtil<AppUserCash>(AppUserCash.class);
        util.exportExcel(response, list, "用户提现数据");
    }

    /**
     * 获取用户提现详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:query')")
    @GetMapping(value = "/{cashId}")
   
    @ApiOperation("获取用户提现详细信息")
    public AjaxResult getInfo(@PathVariable("cashId") Long cashId)
    {
        return success(appUserCashService.selectAppUserCashByCashId(cashId));
    }

    /**
     * 新增用户提现
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:add')")
    @Log(title = "用户提现", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户提现")
    public AjaxResult add(@RequestBody AppUserCash appUserCash)
    {
        return toAjax(appUserCashService.insertAppUserCash(appUserCash));
    }

    /**
     * 修改用户提现
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:edit')")
    @Log(title = "用户提现", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户提现")
    public AjaxResult edit(@RequestBody AppUserCash appUserCash)
    {
        return toAjax(appUserCashService.updateAppUserCash(appUserCash));
    }

    /**
     * 删除用户提现
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_cash:remove')")
    @Log(title = "用户提现", businessType = BusinessType.DELETE)
	@DeleteMapping("/{cashIds}")
   
    @ApiOperation("删除用户提现")
    public AjaxResult remove(@PathVariable Long[] cashIds)
    {
        return toAjax(appUserCashService.deleteAppUserCashByCashIds(cashIds));
    }
}
