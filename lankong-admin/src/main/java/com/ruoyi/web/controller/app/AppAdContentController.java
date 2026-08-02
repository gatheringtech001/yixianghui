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
import com.ruoyi.system.domain.AppAdContent;
import com.ruoyi.system.service.IAppAdContentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 广告内容Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_ad_content")
@Api(tags = "广告内容管理")
public class AppAdContentController extends BaseController
{
    @Autowired
    private IAppAdContentService appAdContentService;

    /**
     * 查询广告内容列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询广告内容列表")
    public TableDataInfo list(AppAdContent appAdContent)
    {
        startPage();
        List<AppAdContent> list = appAdContentService.selectAppAdContentList(appAdContent);
        return getDataTable(list);
    }

    /**
     * 导出广告内容列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:export')")
    @Log(title = "广告内容", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出广告内容列表")
    public void export(HttpServletResponse response, AppAdContent appAdContent)
    {
        List<AppAdContent> list = appAdContentService.selectAppAdContentList(appAdContent);
        ExcelUtil<AppAdContent> util = new ExcelUtil<AppAdContent>(AppAdContent.class);
        util.exportExcel(response, list, "广告内容数据");
    }

    /**
     * 获取广告内容详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:query')")
    @GetMapping(value = "/{contentId}")
   
    @ApiOperation("获取广告内容详细信息")
    public AjaxResult getInfo(@PathVariable("contentId") Long contentId)
    {
        return success(appAdContentService.selectAppAdContentByContentId(contentId));
    }

    /**
     * 新增广告内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:add')")
    @Log(title = "广告内容", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增广告内容")
    public AjaxResult add(@RequestBody AppAdContent appAdContent)
    {
        return toAjax(appAdContentService.insertAppAdContent(appAdContent));
    }

    /**
     * 修改广告内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:edit')")
    @Log(title = "广告内容", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改广告内容")
    public AjaxResult edit(@RequestBody AppAdContent appAdContent)
    {
        return toAjax(appAdContentService.updateAppAdContent(appAdContent));
    }

    /**
     * 删除广告内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_content:remove')")
    @Log(title = "广告内容", businessType = BusinessType.DELETE)
	@DeleteMapping("/{contentIds}")
   
    @ApiOperation("删除广告内容")
    public AjaxResult remove(@PathVariable Long[] contentIds)
    {
        return toAjax(appAdContentService.deleteAppAdContentByContentIds(contentIds));
    }
}
