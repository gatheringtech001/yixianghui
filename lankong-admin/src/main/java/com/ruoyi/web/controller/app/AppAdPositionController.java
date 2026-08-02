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
import com.ruoyi.system.domain.AppAdPosition;
import com.ruoyi.system.service.IAppAdPositionService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 广告管理Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_ad_position")
@Api(tags = "广告管理管理")
public class AppAdPositionController extends BaseController
{
    @Autowired
    private IAppAdPositionService appAdPositionService;

    /**
     * 查询广告管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询广告管理列表")
    public TableDataInfo list(AppAdPosition appAdPosition)
    {
        startPage();
        List<AppAdPosition> list = appAdPositionService.selectAppAdPositionList(appAdPosition);
        return getDataTable(list);
    }

    /**
     * 导出广告管理列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:export')")
    @Log(title = "广告管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出广告管理列表")
    public void export(HttpServletResponse response, AppAdPosition appAdPosition)
    {
        List<AppAdPosition> list = appAdPositionService.selectAppAdPositionList(appAdPosition);
        ExcelUtil<AppAdPosition> util = new ExcelUtil<AppAdPosition>(AppAdPosition.class);
        util.exportExcel(response, list, "广告管理数据");
    }

    /**
     * 获取广告管理详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:query')")
    @GetMapping(value = "/{positionId}")
   
    @ApiOperation("获取广告管理详细信息")
    public AjaxResult getInfo(@PathVariable("positionId") Long positionId)
    {
        return success(appAdPositionService.selectAppAdPositionByPositionId(positionId));
    }

    /**
     * 新增广告管理
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:add')")
    @Log(title = "广告管理", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增广告管理")
    public AjaxResult add(@RequestBody AppAdPosition appAdPosition)
    {
        return toAjax(appAdPositionService.insertAppAdPosition(appAdPosition));
    }

    /**
     * 修改广告管理
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:edit')")
    @Log(title = "广告管理", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改广告管理")
    public AjaxResult edit(@RequestBody AppAdPosition appAdPosition)
    {
        return toAjax(appAdPositionService.updateAppAdPosition(appAdPosition));
    }

    /**
     * 删除广告管理
     */
    @PreAuthorize("@ss.hasPermi('system:app_ad_position:remove')")
    @Log(title = "广告管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{positionIds}")
   
    @ApiOperation("删除广告管理")
    public AjaxResult remove(@PathVariable Long[] positionIds)
    {
        return toAjax(appAdPositionService.deleteAppAdPositionByPositionIds(positionIds));
    }
}
