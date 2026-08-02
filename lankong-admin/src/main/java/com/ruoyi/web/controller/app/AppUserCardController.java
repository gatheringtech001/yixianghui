package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

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
import com.ruoyi.system.domain.AppUserCard;
import com.ruoyi.system.service.IAppUserCardService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户会员卡Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_card")
@Api(tags = "用户会员卡管理")
public class AppUserCardController extends BaseController
{
    @Autowired
    private IAppUserCardService appUserCardService;

    /**
     * 查询用户会员卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户会员卡列表")
    public TableDataInfo list(AppUserCard appUserCard)
    {
        startPage();
        List<AppUserCard> list = appUserCardService.selectAppUserCardList(appUserCard);
        return getDataTable(list);
    }

    /**
     * 导出用户会员卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:export')")
    @Log(title = "用户会员卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户会员卡列表")
    public void export(HttpServletResponse response, AppUserCard appUserCard)
    {
        List<AppUserCard> list = appUserCardService.selectAppUserCardList(appUserCard);
        ExcelUtil<AppUserCard> util = new ExcelUtil<AppUserCard>(AppUserCard.class);
        util.exportExcel(response, list, "用户会员卡数据");
    }

    /**
     * 获取用户会员卡详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:query')")
    @GetMapping(value = "/{recordId}")
   
    @ApiOperation("获取用户会员卡详细信息")
    public AjaxResult getInfo(@PathVariable("recordId") Long recordId)
    {
        return success(appUserCardService.selectAppUserCardByRecordId(recordId));
    }

    /**
     * 查询用户会员卡详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:query')")
    @GetMapping(value = "/info")
   
    @ApiOperation("查询用户会员卡详细信息")
    public AjaxResult getInfo()
    {

        return success(appUserCardService.selectAppUserCardByRecordId(1L));
    }

    /**
     * 新增用户会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:add')")
    @Log(title = "用户会员卡", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户会员卡")
    public AjaxResult add(@RequestBody AppUserCard appUserCard)
    {
        return toAjax(appUserCardService.insertAppUserCard(appUserCard));
    }

    /**
     * 修改用户会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:edit')")
    @Log(title = "用户会员卡", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户会员卡")
    public AjaxResult edit(@RequestBody AppUserCard appUserCard)
    {
        return toAjax(appUserCardService.updateAppUserCard(appUserCard));
    }

    /**
     * 删除用户会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_card:remove')")
    @Log(title = "用户会员卡", businessType = BusinessType.DELETE)
	@DeleteMapping("/{recordIds}")
   
    @ApiOperation("删除用户会员卡")
    public AjaxResult remove(@PathVariable Long[] recordIds)
    {
        return toAjax(appUserCardService.deleteAppUserCardByRecordIds(recordIds));
    }
}
