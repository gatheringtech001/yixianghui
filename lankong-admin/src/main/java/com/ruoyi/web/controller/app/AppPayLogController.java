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
import com.ruoyi.system.domain.AppPayLog;
import com.ruoyi.system.service.IAppPayLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 支付记录Controller
 * 
 * @author lankong
 * @date 2025-05-24
 */
@RestController
@RequestMapping("/system/app_pay_log")
@Api(tags = "支付记录管理")
public class AppPayLogController extends BaseController
{
    @Autowired
    private IAppPayLogService appPayLogService;

    /**
     * 查询支付记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询支付记录列表")
    public TableDataInfo list(AppPayLog appPayLog)
    {
        startPage();
        List<AppPayLog> list = appPayLogService.selectAppPayLogList(appPayLog);
        return getDataTable(list);
    }

    /**
     * 导出支付记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:export')")
    @Log(title = "支付记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出支付记录列表")
    public void export(HttpServletResponse response, AppPayLog appPayLog)
    {
        List<AppPayLog> list = appPayLogService.selectAppPayLogList(appPayLog);
        ExcelUtil<AppPayLog> util = new ExcelUtil<AppPayLog>(AppPayLog.class);
        util.exportExcel(response, list, "支付记录数据");
    }

    /**
     * 获取支付记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:query')")
    @GetMapping(value = "/{logId}")
   
    @ApiOperation("获取支付记录详细信息")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(appPayLogService.selectAppPayLogByLogId(logId));
    }

    /**
     * 新增支付记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:add')")
    @Log(title = "支付记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增支付记录")
    public AjaxResult add(@RequestBody AppPayLog appPayLog)
    {
        return toAjax(appPayLogService.insertAppPayLog(appPayLog));
    }

    /**
     * 修改支付记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:edit')")
    @Log(title = "支付记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改支付记录")
    public AjaxResult edit(@RequestBody AppPayLog appPayLog)
    {
        return toAjax(appPayLogService.updateAppPayLog(appPayLog));
    }

    /**
     * 删除支付记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_log:remove')")
    @Log(title = "支付记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
   
    @ApiOperation("删除支付记录")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(appPayLogService.deleteAppPayLogByLogIds(logIds));
    }
}
