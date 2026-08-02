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
import com.ruoyi.system.domain.AppGoodsOrderDetail;
import com.ruoyi.system.service.IAppGoodsOrderDetailService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 订单详细Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_order_detail")
@Api(tags = "订单详细管理")
public class AppGoodsOrderDetailController extends BaseController
{
    @Autowired
    private IAppGoodsOrderDetailService appGoodsOrderDetailService;

    /**
     * 查询订单详细列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询订单详细列表")
    public TableDataInfo list(AppGoodsOrderDetail appGoodsOrderDetail)
    {
        startPage();
        List<AppGoodsOrderDetail> list = appGoodsOrderDetailService.selectAppGoodsOrderDetailList(appGoodsOrderDetail);
        return getDataTable(list);
    }

    /**
     * 导出订单详细列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:export')")
    @Log(title = "订单详细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出订单详细列表")
    public void export(HttpServletResponse response, AppGoodsOrderDetail appGoodsOrderDetail)
    {
        List<AppGoodsOrderDetail> list = appGoodsOrderDetailService.selectAppGoodsOrderDetailList(appGoodsOrderDetail);
        ExcelUtil<AppGoodsOrderDetail> util = new ExcelUtil<AppGoodsOrderDetail>(AppGoodsOrderDetail.class);
        util.exportExcel(response, list, "订单详细数据");
    }

    /**
     * 获取订单详细详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:query')")
    @GetMapping(value = "/{detailId}")
   
    @ApiOperation("获取订单详细详细信息")
    public AjaxResult getInfo(@PathVariable("detailId") Long detailId)
    {
        return success(appGoodsOrderDetailService.selectAppGoodsOrderDetailByDetailId(detailId));
    }

    /**
     * 新增订单详细
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:add')")
    @Log(title = "订单详细", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增订单详细")
    public AjaxResult add(@RequestBody AppGoodsOrderDetail appGoodsOrderDetail)
    {
        return toAjax(appGoodsOrderDetailService.insertAppGoodsOrderDetail(appGoodsOrderDetail));
    }

    /**
     * 修改订单详细
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:edit')")
    @Log(title = "订单详细", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改订单详细")
    public AjaxResult edit(@RequestBody AppGoodsOrderDetail appGoodsOrderDetail)
    {
        return toAjax(appGoodsOrderDetailService.updateAppGoodsOrderDetail(appGoodsOrderDetail));
    }

    /**
     * 删除订单详细
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_detail:remove')")
    @Log(title = "订单详细", businessType = BusinessType.DELETE)
	@DeleteMapping("/{detailIds}")
   
    @ApiOperation("删除订单详细")
    public AjaxResult remove(@PathVariable Long[] detailIds)
    {
        return toAjax(appGoodsOrderDetailService.deleteAppGoodsOrderDetailByDetailIds(detailIds));
    }
}
