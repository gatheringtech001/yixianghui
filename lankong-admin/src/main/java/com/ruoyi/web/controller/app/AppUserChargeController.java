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
import com.ruoyi.system.domain.AppUserCharge;
import com.ruoyi.system.service.IAppUserChargeService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户充值Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_charge")
@Api(tags = "用户充值管理")
public class AppUserChargeController extends BaseController
{
    @Autowired
    private IAppUserChargeService appUserChargeService;

    /**
     * 查询用户充值列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户充值列表")
    public TableDataInfo list(AppUserCharge appUserCharge)
    {
        startPage();
        List<AppUserCharge> list = appUserChargeService.selectAppUserChargeList(appUserCharge);
        return getDataTable(list);
    }

    /**
     * 导出用户充值列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:export')")
    @Log(title = "用户充值", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户充值列表")
    public void export(HttpServletResponse response, AppUserCharge appUserCharge)
    {
        List<AppUserCharge> list = appUserChargeService.selectAppUserChargeList(appUserCharge);
        ExcelUtil<AppUserCharge> util = new ExcelUtil<AppUserCharge>(AppUserCharge.class);
        util.exportExcel(response, list, "用户充值数据");
    }

    /**
     * 获取用户充值详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:query')")
    @GetMapping(value = "/{chargeId}")
   
    @ApiOperation("获取用户充值详细信息")
    public AjaxResult getInfo(@PathVariable("chargeId") Long chargeId)
    {
        return success(appUserChargeService.selectAppUserChargeByChargeId(chargeId));
    }

    /**
     * 新增用户充值
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:add')")
    @Log(title = "用户充值", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户充值")
    public AjaxResult add(@RequestBody AppUserCharge appUserCharge)
    {
        return toAjax(appUserChargeService.insertAppUserCharge(appUserCharge));
    }

    /**
     * 修改用户充值
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:edit')")
    @Log(title = "用户充值", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户充值")
    public AjaxResult edit(@RequestBody AppUserCharge appUserCharge)
    {
        return toAjax(appUserChargeService.updateAppUserCharge(appUserCharge));
    }

    /**
     * 删除用户充值
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_charge:remove')")
    @Log(title = "用户充值", businessType = BusinessType.DELETE)
	@DeleteMapping("/{chargeIds}")
   
    @ApiOperation("删除用户充值")
    public AjaxResult remove(@PathVariable Long[] chargeIds)
    {
        return toAjax(appUserChargeService.deleteAppUserChargeByChargeIds(chargeIds));
    }
}
