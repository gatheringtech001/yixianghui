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
import com.ruoyi.system.domain.AppUserInfo;
import com.ruoyi.system.service.IAppUserInfoService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户信息Controller
 * 
 * @author lankong
 * @date 2025-06-03
 */
@RestController
@RequestMapping("/system/app_user_info")
@Api(tags = "用户信息管理")
public class AppUserInfoController extends BaseController
{
    @Autowired
    private IAppUserInfoService appUserInfoService;

    /**
     * 查询用户信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户信息列表")
    public TableDataInfo list(AppUserInfo appUserInfo)
    {
        startPage();
        List<AppUserInfo> list = appUserInfoService.selectAppUserInfoList(appUserInfo);
        return getDataTable(list);
    }

    /**
     * 导出用户信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:export')")
    @Log(title = "用户信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户信息列表")
    public void export(HttpServletResponse response, AppUserInfo appUserInfo)
    {
        List<AppUserInfo> list = appUserInfoService.selectAppUserInfoList(appUserInfo);
        ExcelUtil<AppUserInfo> util = new ExcelUtil<AppUserInfo>(AppUserInfo.class);
        util.exportExcel(response, list, "用户信息数据");
    }

    /**
     * 获取用户信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:query')")
    @GetMapping(value = "/{userId}")
   
    @ApiOperation("获取用户信息详细信息")
    public AjaxResult getInfo(@PathVariable("userId") Long userId)
    {
        return success(appUserInfoService.selectAppUserInfoByUserId(userId));
    }

    /**
     * 新增用户信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:add')")
    @Log(title = "用户信息", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户信息")
    public AjaxResult add(@RequestBody AppUserInfo appUserInfo)
    {
        return toAjax(appUserInfoService.insertAppUserInfo(appUserInfo));
    }

    /**
     * 修改用户信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:edit')")
    @Log(title = "用户信息", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户信息")
    public AjaxResult edit(@RequestBody AppUserInfo appUserInfo)
    {
        return toAjax(appUserInfoService.updateAppUserInfo(appUserInfo));
    }

    /**
     * 删除用户信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_info:remove')")
    @Log(title = "用户信息", businessType = BusinessType.DELETE)
	@DeleteMapping("/{userIds}")
   
    @ApiOperation("删除用户信息")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        return toAjax(appUserInfoService.deleteAppUserInfoByUserIds(userIds));
    }
}
