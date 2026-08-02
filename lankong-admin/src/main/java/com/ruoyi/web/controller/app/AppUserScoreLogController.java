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
import com.ruoyi.system.domain.AppUserScoreLog;
import com.ruoyi.system.service.IAppUserScoreLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 积分记录Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_score_log")
@Api(tags = "积分记录管理")
public class AppUserScoreLogController extends BaseController
{
    @Autowired
    private IAppUserScoreLogService appUserScoreLogService;

    /**
     * 查询积分记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询积分记录列表")
    public TableDataInfo list(AppUserScoreLog appUserScoreLog)
    {
        startPage();
        List<AppUserScoreLog> list = appUserScoreLogService.selectAppUserScoreLogList(appUserScoreLog);
        return getDataTable(list);
    }

    /**
     * 导出积分记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:export')")
    @Log(title = "积分记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出积分记录列表")
    public void export(HttpServletResponse response, AppUserScoreLog appUserScoreLog)
    {
        List<AppUserScoreLog> list = appUserScoreLogService.selectAppUserScoreLogList(appUserScoreLog);
        ExcelUtil<AppUserScoreLog> util = new ExcelUtil<AppUserScoreLog>(AppUserScoreLog.class);
        util.exportExcel(response, list, "积分记录数据");
    }

    /**
     * 获取积分记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:query')")
    @GetMapping(value = "/{logId}")
   
    @ApiOperation("获取积分记录详细信息")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(appUserScoreLogService.selectAppUserScoreLogByLogId(logId));
    }

    /**
     * 新增积分记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:add')")
    @Log(title = "积分记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增积分记录")
    public AjaxResult add(@RequestBody AppUserScoreLog appUserScoreLog)
    {
        return toAjax(appUserScoreLogService.insertAppUserScoreLog(appUserScoreLog));
    }

    /**
     * 修改积分记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:edit')")
    @Log(title = "积分记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改积分记录")
    public AjaxResult edit(@RequestBody AppUserScoreLog appUserScoreLog)
    {
        return toAjax(appUserScoreLogService.updateAppUserScoreLog(appUserScoreLog));
    }

    /**
     * 删除积分记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_score_log:remove')")
    @Log(title = "积分记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
   
    @ApiOperation("删除积分记录")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(appUserScoreLogService.deleteAppUserScoreLogByLogIds(logIds));
    }
}
