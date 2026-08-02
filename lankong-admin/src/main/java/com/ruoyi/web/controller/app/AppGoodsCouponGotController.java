package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.system.service.IAppGoodsCouponService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.AppGoodsCouponGot;
import com.ruoyi.system.service.IAppGoodsCouponGotService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 优惠券领取记录Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_coupon_got")
@Api(tags = "优惠券领取记录管理")
public class AppGoodsCouponGotController extends BaseController
{
    @Autowired
    private IAppGoodsCouponGotService appGoodsCouponGotService;
    @Autowired
    private IAppGoodsCouponService appGoodsCouponService;

    /**
     * 查询优惠券领取记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询优惠券领取记录列表")
    public TableDataInfo list(AppGoodsCouponGot appGoodsCouponGot)
    {
        startPage();
        List<AppGoodsCouponGot> list = appGoodsCouponGotService.selectAppGoodsCouponGotList(appGoodsCouponGot);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setCouponInfo(appGoodsCouponService.selectAppGoodsCouponByCouponId(list.get(i).getCouponId()));
        }
        return getDataTable(list);
    }

    /**
     * 获取可用优惠券
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:list')")
    @GetMapping("/enable_list")
   
    @ApiOperation("获取可用优惠券")
    public TableDataInfo enableList(@RequestParam(name = "goodsId", required = true) String goodsId)
    {
        startPage();
        AppGoodsCouponGot appGoodsCouponGot = new AppGoodsCouponGot();
        appGoodsCouponGot.setIsUsed(1);
        appGoodsCouponGot.setStatus("1");
        List<AppGoodsCouponGot> list = appGoodsCouponGotService.selectAppGoodsCouponGotList(appGoodsCouponGot);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setCouponInfo(appGoodsCouponService.selectAppGoodsCouponByCouponId(list.get(i).getCouponId()));
        }
        return getDataTable(list);
    }

    /**
     * 导出优惠券领取记录列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:export')")
    @Log(title = "优惠券领取记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出优惠券领取记录列表")
    public void export(HttpServletResponse response, AppGoodsCouponGot appGoodsCouponGot)
    {
        List<AppGoodsCouponGot> list = appGoodsCouponGotService.selectAppGoodsCouponGotList(appGoodsCouponGot);
        ExcelUtil<AppGoodsCouponGot> util = new ExcelUtil<AppGoodsCouponGot>(AppGoodsCouponGot.class);
        util.exportExcel(response, list, "优惠券领取记录数据");
    }

    /**
     * 获取优惠券领取记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:query')")
    @GetMapping(value = "/{gotId}")
   
    @ApiOperation("获取优惠券领取记录详细信息")
    public AjaxResult getInfo(@PathVariable("gotId") Long gotId)
    {
        return success(appGoodsCouponGotService.selectAppGoodsCouponGotByGotId(gotId));
    }

    /**
     * 领取优惠券
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:add')")
    @Log(title = "优惠券领取记录", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("领取优惠券")
    public AjaxResult add(@RequestBody AppGoodsCouponGot appGoodsCouponGot)
    {
        return toAjax(appGoodsCouponGotService.insertAppGoodsCouponGot(appGoodsCouponGot));
    }

    /**
     * 修改优惠券领取记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:edit')")
    @Log(title = "优惠券领取记录", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改优惠券领取记录")
    public AjaxResult edit(@RequestBody AppGoodsCouponGot appGoodsCouponGot)
    {
        return toAjax(appGoodsCouponGotService.updateAppGoodsCouponGot(appGoodsCouponGot));
    }

    /**
     * 删除优惠券领取记录
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon_got:remove')")
    @Log(title = "优惠券领取记录", businessType = BusinessType.DELETE)
	@DeleteMapping("/{gotIds}")
   
    @ApiOperation("删除优惠券领取记录")
    public AjaxResult remove(@PathVariable Long[] gotIds)
    {
        return toAjax(appGoodsCouponGotService.deleteAppGoodsCouponGotByGotIds(gotIds));
    }
}
