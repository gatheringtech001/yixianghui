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
import com.ruoyi.system.domain.AppPayRefundLog;
import com.ruoyi.system.service.IAppPayRefundLogService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 退款记录Controller
 * 
 * @author lankong
 * @date 2025-05-24
 */
@RestController
@RequestMapping("/system/app_pay_refund_log")
@Api(tags = "退款记录管理")
public class AppPayRefundLogController extends BaseController
{
    @Autowired
    private IAppPayRefundLogService appPayRefundLogService;

    /**
     * 查询退款记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询退款记录列表")
    public TableDataInfo list(AppPayRefundLog appPayRefundLog)
    {
        startPage();
        List<AppPayRefundLog> list = appPayRefundLogService.selectAppPayRefundLogList(appPayRefundLog);
        return getDataTable(list);
    }

    /**
     * 导出退款记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:export')")
    @Log(title = "退款记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出退款记录列表")
    public void export(HttpServletResponse response, AppPayRefundLog appPayRefundLog)
    {
        List<AppPayRefundLog> list = appPayRefundLogService.selectAppPayRefundLogList(appPayRefundLog);
        ExcelUtil<AppPayRefundLog> util = new ExcelUtil<AppPayRefundLog>(AppPayRefundLog.class);
        util.exportExcel(response, list, "退款记录数据");
    }

    /**
     * 获取退款记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:query')")
    @GetMapping(value = "/{logId}")
   
    @ApiOperation("获取退款记录详细信息")
    public AjaxResult getInfo(@PathVariable("logId") Long logId)
    {
        return success(appPayRefundLogService.selectAppPayRefundLogByLogId(logId));
    }

    /**
     * 新增退款记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:add')")
    @Log(title = "退款记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增退款记录")
    public AjaxResult add(@RequestBody AppPayRefundLog appPayRefundLog)
    {
        return toAjax(appPayRefundLogService.insertAppPayRefundLog(appPayRefundLog));
    }

    /**
     * 修改退款记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:edit')")
    @Log(title = "退款记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改退款记录")
    public AjaxResult edit(@RequestBody AppPayRefundLog appPayRefundLog)
    {
        return toAjax(appPayRefundLogService.updateAppPayRefundLog(appPayRefundLog));
    }

    /**
     * 删除退款记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:remove')")
    @Log(title = "退款记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{logIds}")
   
    @ApiOperation("删除退款记录")
    public AjaxResult remove(@PathVariable Long[] logIds)
    {
        return toAjax(appPayRefundLogService.deleteAppPayRefundLogByLogIds(logIds));
    }
}
