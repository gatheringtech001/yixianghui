package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.system.service.IAppActivityCategoryService;
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
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.service.IAppActivityService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 活动Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_activity")
@Api(tags = "活动管理")
public class AppActivityController extends BaseController
{
    @Autowired
    private IAppActivityService appActivityService;
    @Autowired
    private IAppActivityCategoryService categoryService;

    /**
     * 查询活动列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询活动列表")
    public TableDataInfo list(AppActivity appActivity)
    {
        startPage();
        List<AppActivity> list = appActivityService.selectAppActivityList(appActivity);
        for (int i = 0; i < list.size(); i++) {
            String categoryName = categoryService.selectAppActivityCategoryCacheNameByCategoryId(list.get(i).getCategoryId());
            list.get(i).setCategoryName(categoryName);
        }
        return getDataTable(list);
    }

    /**
     * 导出活动列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:export')")
    @Log(title = "活动", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出活动列表")
    public void export(HttpServletResponse response, AppActivity appActivity)
    {
        List<AppActivity> list = appActivityService.selectAppActivityList(appActivity);
        ExcelUtil<AppActivity> util = new ExcelUtil<AppActivity>(AppActivity.class);
        util.exportExcel(response, list, "活动数据");
    }

    /**
     * 获取活动详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:query')")
    @GetMapping(value = "/{activityId}")
   
    @ApiOperation("获取活动详细信息")
    public AjaxResult getInfo(@PathVariable("activityId") Long activityId)
    {
        AppActivity info = appActivityService.selectAppActivityByActivityId(activityId);
        if (info == null) {
            return error("无效信息");
        }
        String categoryName = categoryService.selectAppActivityCategoryCacheNameByCategoryId(info.getCategoryId());
        info.setCategoryName(categoryName);
        return success(info);
    }

    /**
     * 新增活动
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:add')")
    @Log(title = "活动", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增活动")
    public AjaxResult add(@RequestBody AppActivity appActivity)
    {
        return toAjax(appActivityService.insertAppActivity(appActivity));
    }

    /**
     * 修改活动
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:edit')")
    @Log(title = "活动", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改活动")
    public AjaxResult edit(@RequestBody AppActivity appActivity)
    {
        return toAjax(appActivityService.updateAppActivity(appActivity));
    }

    /**
     * 删除活动
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity:remove')")
    @Log(title = "活动", businessType = BusinessType.DELETE)
	@DeleteMapping("/{activityIds}")
   
    @ApiOperation("删除活动")
    public AjaxResult remove(@PathVariable Long[] activityIds)
    {
        return toAjax(appActivityService.deleteAppActivityByActivityIds(activityIds));
    }
}
