package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

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
import com.ruoyi.system.domain.AppUserGoldLog;
import com.ruoyi.system.service.IAppUserGoldLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 金币记录Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_gold_log")
@Api(tags = "金币记录管理")
public class AppUserGoldLogController extends BaseController
{
    @Autowired
    private IAppUserGoldLogService appUserGoldLogService;

    /**
     * 查询金币获取记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询金币获取记录列表")
    public TableDataInfo list(AppUserGoldLog appUserGoldLog)
    {
        startPage();
        List<AppUserGoldLog> list = appUserGoldLogService.selectAppUserGoldLogList(appUserGoldLog);
        return getDataTable(list);
    }

    /**
     * 导出金币记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:export')")
    @Log(title = "金币记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出金币记录列表")
    public void export(HttpServletResponse response, AppUserGoldLog appUserGoldLog)
    {
        List<AppUserGoldLog> list = appUserGoldLogService.selectAppUserGoldLogList(appUserGoldLog);
        ExcelUtil<AppUserGoldLog> util = new ExcelUtil<AppUserGoldLog>(AppUserGoldLog.class);
        util.exportExcel(response, list, "金币记录数据");
    }

    /**
     * 获取金币记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:query')")
    @GetMapping(value = "/{logId}")
   
    @ApiOperation("获取金币记录详细信息")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(appUserGoldLogService.selectAppUserGoldLogByLogId(logId));
    }

    /**
     * 新增金币记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:add')")
    @Log(title = "金币记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增金币记录")
    public AjaxResult add(@RequestBody AppUserGoldLog appUserGoldLog)
    {
        return toAjax(appUserGoldLogService.insertAppUserGoldLog(appUserGoldLog));
    }

    /**
     * 修改金币记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:edit')")
    @Log(title = "金币记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改金币记录")
    public AjaxResult edit(@RequestBody AppUserGoldLog appUserGoldLog)
    {
        return toAjax(appUserGoldLogService.updateAppUserGoldLog(appUserGoldLog));
    }

    /**
     * 删除金币记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_gold_log:remove')")
    @Log(title = "金币记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
   
    @ApiOperation("删除金币记录")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(appUserGoldLogService.deleteAppUserGoldLogByLogIds(logIds));
    }
}
