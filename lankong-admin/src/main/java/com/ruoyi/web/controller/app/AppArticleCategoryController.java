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
import com.ruoyi.system.domain.AppArticleCategory;
import com.ruoyi.system.service.IAppArticleCategoryService;
import com.ruoyi.common.utils.poi.ExcelUtil;

/**
 * 内容分类Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_article_category")
@Api(tags = "内容分类管理")
public class AppArticleCategoryController extends BaseController
{
    @Autowired
    private IAppArticleCategoryService appArticleCategoryService;

    /**
     * 查询内容分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询内容分类列表")
    public AjaxResult list(AppArticleCategory appArticleCategory)
    {
        List<AppArticleCategory> list = appArticleCategoryService.selectAppArticleCategoryList(appArticleCategory);
        return success(list);
    }

    /**
     * 导出内容分类列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:export')")
    @Log(title = "内容分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出内容分类列表")
    public void export(HttpServletResponse response, AppArticleCategory appArticleCategory)
    {
        List<AppArticleCategory> list = appArticleCategoryService.selectAppArticleCategoryList(appArticleCategory);
        ExcelUtil<AppArticleCategory> util = new ExcelUtil<AppArticleCategory>(AppArticleCategory.class);
        util.exportExcel(response, list, "内容分类数据");
    }

    /**
     * 获取内容分类详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:query')")
    @GetMapping(value = "/{categoryId}")
   
    @ApiOperation("获取内容分类详细信息")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(appArticleCategoryService.selectAppArticleCategoryByCategoryId(categoryId));
    }

    /**
     * 新增内容分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:add')")
    @Log(title = "内容分类", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增内容分类")
    public AjaxResult add(@RequestBody AppArticleCategory appArticleCategory)
    {
        return toAjax(appArticleCategoryService.insertAppArticleCategory(appArticleCategory));
    }

    /**
     * 修改内容分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:edit')")
    @Log(title = "内容分类", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改内容分类")
    public AjaxResult edit(@RequestBody AppArticleCategory appArticleCategory)
    {
        return toAjax(appArticleCategoryService.updateAppArticleCategory(appArticleCategory));
    }

    /**
     * 删除内容分类
     */
    @PreAuthorize("@ss.hasPermi('system:app_article_category:remove')")
    @Log(title = "内容分类", businessType = BusinessType.DELETE)
	@DeleteMapping("/{categoryIds}")
   
    @ApiOperation("删除内容分类")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(appArticleCategoryService.deleteAppArticleCategoryByCategoryIds(categoryIds));
    }
}
