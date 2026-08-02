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
import com.ruoyi.system.domain.AppAttachments;
import com.ruoyi.system.service.IAppAttachmentsService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 附件Controller
 * 
 * @author lankong
 * @date 2025-07-20
 */
@RestController
@RequestMapping("/system/attachments")
@Api(tags = "附件管理")
public class AppAttachmentsController extends BaseController
{
    @Autowired
    private IAppAttachmentsService appAttachmentsService;

    /**
     * 查询附件列表
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询附件列表")
    public TableDataInfo list(AppAttachments appAttachments)
    {
        startPage();
        List<AppAttachments> list = appAttachmentsService.selectAppAttachmentsList(appAttachments);
        return getDataTable(list);
    }

    /**
     * 导出附件列表
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:export')")
    @Log(title = "附件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出附件列表")
    public void export(HttpServletResponse response, AppAttachments appAttachments)
    {
        List<AppAttachments> list = appAttachmentsService.selectAppAttachmentsList(appAttachments);
        ExcelUtil<AppAttachments> util = new ExcelUtil<AppAttachments>(AppAttachments.class);
        util.exportExcel(response, list, "附件数据");
    }

    /**
     * 获取附件详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:query')")
    @GetMapping(value = "/{attachmentId}")
   
    @ApiOperation("获取附件详细信息")
    public AjaxResult getInfo(@PathVariable("attachmentId") Long attachmentId)
    {
        return success(appAttachmentsService.selectAppAttachmentsByAttachmentId(attachmentId));
    }

    /**
     * 新增附件
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:add')")
    @Log(title = "附件", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增附件")
    public AjaxResult add(@RequestBody AppAttachments appAttachments)
    {
        return toAjax(appAttachmentsService.insertAppAttachments(appAttachments));
    }

    /**
     * 修改附件
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:edit')")
    @Log(title = "附件", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改附件")
    public AjaxResult edit(@RequestBody AppAttachments appAttachments)
    {
        return toAjax(appAttachmentsService.updateAppAttachments(appAttachments));
    }

    /**
     * 删除附件
     */
    @PreAuthorize("@ss.hasPermi('system:attachments:remove')")
    @Log(title = "附件", businessType = BusinessType.DELETE)
	@DeleteMapping("/{attachmentIds}")
   
    @ApiOperation("删除附件")
    public AjaxResult remove(@PathVariable Long[] attachmentIds)
    {
        return toAjax(appAttachmentsService.deleteAppAttachmentsByAttachmentIds(attachmentIds));
    }
}
