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
import com.ruoyi.system.domain.AppUserMoneyLog;
import com.ruoyi.system.service.IAppUserMoneyLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 钱包记录Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_money_log")
@Api(tags = "钱包记录管理")
public class AppUserMoneyLogController extends BaseController
{
    @Autowired
    private IAppUserMoneyLogService appUserMoneyLogService;

    /**
     * 查询钱包记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询钱包记录列表")
    public TableDataInfo list(AppUserMoneyLog appUserMoneyLog)
    {
        startPage();
        List<AppUserMoneyLog> list = appUserMoneyLogService.selectAppUserMoneyLogList(appUserMoneyLog);
        return getDataTable(list);
    }

    /**
     * 导出钱包记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:export')")
    @Log(title = "钱包记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出钱包记录列表")
    public void export(HttpServletResponse response, AppUserMoneyLog appUserMoneyLog)
    {
        List<AppUserMoneyLog> list = appUserMoneyLogService.selectAppUserMoneyLogList(appUserMoneyLog);
        ExcelUtil<AppUserMoneyLog> util = new ExcelUtil<AppUserMoneyLog>(AppUserMoneyLog.class);
        util.exportExcel(response, list, "钱包记录数据");
    }

    /**
     * 获取钱包记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:query')")
    @GetMapping(value = "/{logId}")
   
    @ApiOperation("获取钱包记录详细信息")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(appUserMoneyLogService.selectAppUserMoneyLogByLogId(logId));
    }

    /**
     * 新增钱包记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:add')")
    @Log(title = "钱包记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增钱包记录")
    public AjaxResult add(@RequestBody AppUserMoneyLog appUserMoneyLog)
    {
        return toAjax(appUserMoneyLogService.insertAppUserMoneyLog(appUserMoneyLog));
    }

    /**
     * 修改钱包记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:edit')")
    @Log(title = "钱包记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改钱包记录")
    public AjaxResult edit(@RequestBody AppUserMoneyLog appUserMoneyLog)
    {
        return toAjax(appUserMoneyLogService.updateAppUserMoneyLog(appUserMoneyLog));
    }

    /**
     * 删除钱包记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_money_log:remove')")
    @Log(title = "钱包记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
   
    @ApiOperation("删除钱包记录")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(appUserMoneyLogService.deleteAppUserMoneyLogByLogIds(logIds));
    }
}
