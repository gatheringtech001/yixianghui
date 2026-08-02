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
import com.ruoyi.system.domain.AppArticle;
import com.ruoyi.system.service.IAppArticleService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 图文内容Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_article")
@Api(tags = "图文内容管理")
public class AppArticleController extends BaseController
{
    @Autowired
    private IAppArticleService appArticleService;

    /**
     * 查询图文内容列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询图文内容列表")
    public TableDataInfo list(AppArticle appArticle)
    {
        startPage();
        List<AppArticle> list = appArticleService.selectAppArticleList(appArticle);
        return getDataTable(list);
    }

    /**
     * 导出图文内容列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:export')")
    @Log(title = "图文内容", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出图文内容列表")
    public void export(HttpServletResponse response, AppArticle appArticle)
    {
        List<AppArticle> list = appArticleService.selectAppArticleList(appArticle);
        ExcelUtil<AppArticle> util = new ExcelUtil<AppArticle>(AppArticle.class);
        util.exportExcel(response, list, "图文内容数据");
    }

    /**
     * 获取图文内容详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:query')")
    @GetMapping(value = "/{articleId}")
   
    @ApiOperation("获取图文内容详细信息")
    public AjaxResult getInfo(@PathVariable("articleId") Long articleId)
    {
        return success(appArticleService.selectAppArticleByArticleId(articleId));
    }

    /**
     * 新增图文内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:add')")
    @Log(title = "图文内容", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增图文内容")
    public AjaxResult add(@RequestBody AppArticle appArticle)
    {
        return toAjax(appArticleService.insertAppArticle(appArticle));
    }

    /**
     * 修改图文内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:edit')")
    @Log(title = "图文内容", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改图文内容")
    public AjaxResult edit(@RequestBody AppArticle appArticle)
    {
        return toAjax(appArticleService.updateAppArticle(appArticle));
    }

    /**
     * 删除图文内容
     */
    @PreAuthorize("@ss.hasPermi('system:app_article:remove')")
    @Log(title = "图文内容", businessType = BusinessType.DELETE)
	@DeleteMapping("/{articleIds}")
   
    @ApiOperation("删除图文内容")
    public AjaxResult remove(@PathVariable Long[] articleIds)
    {
        return toAjax(appArticleService.deleteAppArticleByArticleIds(articleIds));
    }
}
