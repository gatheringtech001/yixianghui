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
import com.ruoyi.system.domain.AppCard;
import com.ruoyi.system.service.IAppCardService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 会员卡Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_card")
@Api(tags = "会员卡管理")
public class AppCardController extends BaseController
{
    @Autowired
    private IAppCardService appCardService;

    /**
     * 查询会员卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询会员卡列表")
    public TableDataInfo list(AppCard appCard)
    {
        startPage();
        List<AppCard> list = appCardService.selectAppCardList(appCard);
        return getDataTable(list);
    }

    /**
     * 导出会员卡列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:export')")
    @Log(title = "会员卡", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出会员卡列表")
    public void export(HttpServletResponse response, AppCard appCard)
    {
        List<AppCard> list = appCardService.selectAppCardList(appCard);
        ExcelUtil<AppCard> util = new ExcelUtil<AppCard>(AppCard.class);
        util.exportExcel(response, list, "会员卡数据");
    }

    /**
     * 获取会员卡详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:query')")
    @GetMapping(value = "/{cardId}")
   
    @ApiOperation("获取会员卡详细信息")
    public AjaxResult getInfo(@PathVariable("cardId") Long cardId)
    {
        return success(appCardService.selectAppCardByCardId(cardId));
    }

    /**
     * 新增会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:add')")
    @Log(title = "会员卡", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增会员卡")
    public AjaxResult add(@RequestBody AppCard appCard)
    {
        return toAjax(appCardService.insertAppCard(appCard));
    }

    /**
     * 修改会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:edit')")
    @Log(title = "会员卡", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改会员卡")
    public AjaxResult edit(@RequestBody AppCard appCard)
    {
        return toAjax(appCardService.updateAppCard(appCard));
    }

    /**
     * 删除会员卡
     */
    @PreAuthorize("@ss.hasPermi('system:app_card:remove')")
    @Log(title = "会员卡", businessType = BusinessType.DELETE)
	@DeleteMapping("/{cardIds}")
   
    @ApiOperation("删除会员卡")
    public AjaxResult remove(@PathVariable Long[] cardIds)
    {
        return toAjax(appCardService.deleteAppCardByCardIds(cardIds));
    }
}
