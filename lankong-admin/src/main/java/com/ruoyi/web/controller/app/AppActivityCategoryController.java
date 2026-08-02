package com.ruoyi.web.controller.app;

import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
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
import com.ruoyi.system.domain.AppActivityCategory;
import com.ruoyi.system.service.IAppActivityCategoryService;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 活动分类Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_activity_category")
@Api(tags = "活动分类管理")
public class AppActivityCategoryController extends BaseController
{
    @Autowired
    private IAppActivityCategoryService appActivityCategoryService;

    /**
     * 查询活动分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:list')")
    @GetMapping("/list")

    @ApiOperation("查询活动分类列表")
    public AjaxResult list(AppActivityCategory appActivityCategory)
    {
        List<AppActivityCategory> list = appActivityCategoryService.selectAppActivityCategoryList(appActivityCategory);
        return success(list);
    }

    /**
     * 导出活动分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:export')")
    @Log(title = "活动分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出活动分类列表")
    public void export(HttpServletResponse response, AppActivityCategory appActivityCategory)
    {
        List<AppActivityCategory> list = appActivityCategoryService.selectAppActivityCategoryList(appActivityCategory);
        ExcelUtil<AppActivityCategory> util = new ExcelUtil<AppActivityCategory>(AppActivityCategory.class);
        util.exportExcel(response, list, "活动分类数据");
    }

    /**
     * 获取活动分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:query')")
    @GetMapping(value = "/{categoryId}")
   
    @ApiOperation("获取活动分类详细信息")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(appActivityCategoryService.selectAppActivityCategoryByCategoryId(categoryId));
    }

    /**
     * 新增活动分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:add')")
    @Log(title = "活动分类", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增活动分类")
    public AjaxResult add(@RequestBody AppActivityCategory appActivityCategory)
    {
        return toAjax(appActivityCategoryService.insertAppActivityCategory(appActivityCategory));
    }

    /**
     * 修改活动分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:edit')")
    @Log(title = "活动分类", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改活动分类")
    public AjaxResult edit(@RequestBody AppActivityCategory appActivityCategory)
    {
        return toAjax(appActivityCategoryService.updateAppActivityCategory(appActivityCategory));
    }

    /**
     * 删除活动分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_category:remove')")
    @Log(title = "活动分类", businessType = BusinessType.DELETE)
	@DeleteMapping("/{categoryIds}")
   
    @ApiOperation("删除活动分类")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(appActivityCategoryService.deleteAppActivityCategoryByCategoryIds(categoryIds));
    }
}
