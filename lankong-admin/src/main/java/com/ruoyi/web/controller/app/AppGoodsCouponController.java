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
import com.ruoyi.system.domain.AppGoodsCoupon;
import com.ruoyi.system.service.IAppGoodsCouponService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品优惠券Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_coupon")
@Api(tags = "商品优惠券管理")
public class AppGoodsCouponController extends BaseController
{
    @Autowired
    private IAppGoodsCouponService appGoodsCouponService;

    /**
     * 查询商品优惠券列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品优惠券列表")
    public TableDataInfo list(AppGoodsCoupon appGoodsCoupon)
    {
        startPage();
        List<AppGoodsCoupon> list = appGoodsCouponService.selectAppGoodsCouponList(appGoodsCoupon);
        return getDataTable(list);
    }

    /**
     * 导出商品优惠券列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:export')")
    @Log(title = "商品优惠券", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品优惠券列表")
    public void export(HttpServletResponse response, AppGoodsCoupon appGoodsCoupon)
    {
        List<AppGoodsCoupon> list = appGoodsCouponService.selectAppGoodsCouponList(appGoodsCoupon);
        ExcelUtil<AppGoodsCoupon> util = new ExcelUtil<AppGoodsCoupon>(AppGoodsCoupon.class);
        util.exportExcel(response, list, "商品优惠券数据");
    }

    /**
     * 获取商品优惠券详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:query')")
    @GetMapping(value = "/{couponId}")
   
    @ApiOperation("获取商品优惠券详细信息")
    public AjaxResult getInfo(@PathVariable("couponId") Long couponId)
    {
        return success(appGoodsCouponService.selectAppGoodsCouponByCouponId(couponId));
    }

    /**
     * 新增商品优惠券
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:add')")
    @Log(title = "商品优惠券", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商品优惠券")
    public AjaxResult add(@RequestBody AppGoodsCoupon appGoodsCoupon)
    {
        return toAjax(appGoodsCouponService.insertAppGoodsCoupon(appGoodsCoupon));
    }

    /**
     * 修改商品优惠券
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:edit')")
    @Log(title = "商品优惠券", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品优惠券")
    public AjaxResult edit(@RequestBody AppGoodsCoupon appGoodsCoupon)
    {
        return toAjax(appGoodsCouponService.updateAppGoodsCoupon(appGoodsCoupon));
    }

    /**
     * 删除商品优惠券
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_coupon:remove')")
    @Log(title = "商品优惠券", businessType = BusinessType.DELETE)
	@DeleteMapping("/{couponIds}")
   
    @ApiOperation("删除商品优惠券")
    public AjaxResult remove(@PathVariable Long[] couponIds)
    {
        return toAjax(appGoodsCouponService.deleteAppGoodsCouponByCouponIds(couponIds));
    }
}
