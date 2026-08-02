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
import com.ruoyi.system.domain.AppUserBank;
import com.ruoyi.system.service.IAppUserBankService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户银行卡Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_bank")
@Api(tags = "用户银行卡管理")
public class AppUserBankController extends BaseController
{
    @Autowired
    private IAppUserBankService appUserBankService;

    /**
     * 查询用户银行卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户银行卡列表")
    public TableDataInfo list(AppUserBank appUserBank)
    {
        startPage();
        List<AppUserBank> list = appUserBankService.selectAppUserBankList(appUserBank);
        return getDataTable(list);
    }

    /**
     * 导出用户银行卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:export')")
    @Log(title = "用户银行卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户银行卡列表")
    public void export(HttpServletResponse response, AppUserBank appUserBank)
    {
        List<AppUserBank> list = appUserBankService.selectAppUserBankList(appUserBank);
        ExcelUtil<AppUserBank> util = new ExcelUtil<AppUserBank>(AppUserBank.class);
        util.exportExcel(response, list, "用户银行卡数据");
    }

    /**
     * 获取用户银行卡详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:query')")
    @GetMapping(value = "/{bankId}")
   
    @ApiOperation("获取用户银行卡详细信息")
    public AjaxResult getInfo(@PathVariable("bankId") Long bankId)
    {
        return success(appUserBankService.selectAppUserBankByBankId(bankId));
    }

    /**
     * 新增用户银行卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:add')")
    @Log(title = "用户银行卡", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户银行卡")
    public AjaxResult add(@RequestBody AppUserBank appUserBank)
    {
        return toAjax(appUserBankService.insertAppUserBank(appUserBank));
    }

    /**
     * 修改用户银行卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:edit')")
    @Log(title = "用户银行卡", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户银行卡")
    public AjaxResult edit(@RequestBody AppUserBank appUserBank)
    {
        return toAjax(appUserBankService.updateAppUserBank(appUserBank));
    }

    /**
     * 删除用户银行卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_bank:remove')")
    @Log(title = "用户银行卡", businessType = BusinessType.DELETE)
	@DeleteMapping("/{bankIds}")
   
    @ApiOperation("删除用户银行卡")
    public AjaxResult remove(@PathVariable Long[] bankIds)
    {
        return toAjax(appUserBankService.deleteAppUserBankByBankIds(bankIds));
    }
}
