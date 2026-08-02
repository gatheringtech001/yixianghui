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
import com.ruoyi.system.domain.AppConfig;
import com.ruoyi.system.service.IAppConfigService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商城配置Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_config")
@Api(tags = "商城配置管理")
public class AppConfigController extends BaseController
{
    @Autowired
    private IAppConfigService appConfigService;

    /**
     * 查询商城配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商城配置列表")
    public TableDataInfo list(AppConfig appConfig)
    {
        startPage();
        List<AppConfig> list = appConfigService.selectAppConfigList(appConfig);
        return getDataTable(list);
    }

    /**
     * 导出商城配置列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:export')")
    @Log(title = "商城配置", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商城配置列表")
    public void export(HttpServletResponse response, AppConfig appConfig)
    {
        List<AppConfig> list = appConfigService.selectAppConfigList(appConfig);
        ExcelUtil<AppConfig> util = new ExcelUtil<AppConfig>(AppConfig.class);
        util.exportExcel(response, list, "商城配置数据");
    }

    /**
     * 获取商城配置详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:query')")
    @GetMapping(value = "/{configId}")
   
    @ApiOperation("获取商城配置详细信息")
    public AjaxResult getInfo(@PathVariable("configId") Integer configId)
    {
        return success(appConfigService.selectAppConfigByConfigId(configId));
    }

    /**
     * 新增商城配置
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:add')")
    @Log(title = "商城配置", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商城配置")
    public AjaxResult add(@RequestBody AppConfig appConfig)
    {
        return toAjax(appConfigService.insertAppConfig(appConfig));
    }

    /**
     * 修改商城配置
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:edit')")
    @Log(title = "商城配置", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商城配置")
    public AjaxResult edit(@RequestBody AppConfig appConfig)
    {
        return toAjax(appConfigService.updateAppConfig(appConfig));
    }

    /**
     * 删除商城配置
     */
    @PreAuthorize("@ss.hasPermi('system:app_config:remove')")
    @Log(title = "商城配置", businessType = BusinessType.DELETE)
	@DeleteMapping("/{configIds}")
   
    @ApiOperation("删除商城配置")
    public AjaxResult remove(@PathVariable Integer[] configIds)
    {
        return toAjax(appConfigService.deleteAppConfigByConfigIds(configIds));
    }
}
