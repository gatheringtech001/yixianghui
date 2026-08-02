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
import com.ruoyi.system.domain.AppSinglePage;
import com.ruoyi.system.service.IAppSinglePageService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 单页文章Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_single_page")
@Api(tags = "单页文章管理")
public class AppSinglePageController extends BaseController
{
    @Autowired
    private IAppSinglePageService appSinglePageService;

    /**
     * 查询单页文章列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询单页文章列表")
    public TableDataInfo list(AppSinglePage appSinglePage)
    {
        startPage();
        List<AppSinglePage> list = appSinglePageService.selectAppSinglePageList(appSinglePage);
        return getDataTable(list);
    }

    /**
     * 导出单页文章列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:export')")
    @Log(title = "单页文章", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出单页文章列表")
    public void export(HttpServletResponse response, AppSinglePage appSinglePage)
    {
        List<AppSinglePage> list = appSinglePageService.selectAppSinglePageList(appSinglePage);
        ExcelUtil<AppSinglePage> util = new ExcelUtil<AppSinglePage>(AppSinglePage.class);
        util.exportExcel(response, list, "单页文章数据");
    }

    /**
     * 获取单页文章详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:query')")
    @GetMapping(value = "/{pageId}")
   
    @ApiOperation("获取单页文章详细信息")
    public AjaxResult getInfo(@PathVariable("pageId") Long pageId)
    {
        return success(appSinglePageService.selectAppSinglePageByPageId(pageId));
    }

    /**
     * 新增单页文章
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:add')")
    @Log(title = "单页文章", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增单页文章")
    public AjaxResult add(@RequestBody AppSinglePage appSinglePage)
    {
        return toAjax(appSinglePageService.insertAppSinglePage(appSinglePage));
    }

    /**
     * 修改单页文章
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:edit')")
    @Log(title = "单页文章", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改单页文章")
    public AjaxResult edit(@RequestBody AppSinglePage appSinglePage)
    {
        return toAjax(appSinglePageService.updateAppSinglePage(appSinglePage));
    }

    /**
     * 删除单页文章
     */
    @PreAuthorize("@ss.hasPermi('system:app_single_page:remove')")
    @Log(title = "单页文章", businessType = BusinessType.DELETE)
	@DeleteMapping("/{pageIds}")
   
    @ApiOperation("删除单页文章")
    public AjaxResult remove(@PathVariable Long[] pageIds)
    {
        return toAjax(appSinglePageService.deleteAppSinglePageByPageIds(pageIds));
    }
}
