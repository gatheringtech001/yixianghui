package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.system.domain.*;
import com.ruoyi.system.service.*;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品订单Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_order")
@Api(tags = "商品订单管理")
public class AppGoodsOrderController extends BaseController
{
    @Autowired
    private com.ruoyi.system.service.impl.RetailOrderStore retailOrderStore;
    @Autowired
    private IAppGoodsOrderService appGoodsOrderService;
    @Autowired
    private IAppUserAddressService userAddressService;
    @Autowired
    private IAppGoodsOrderDetailService orderDetailService;
    @Autowired
    private IAppGoodsOrderAfterService orderAfterService;
    @Autowired
    private IAppGoodsService goodsService;


    /**
     * 查询商品订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品订单列表")
    public TableDataInfo list(AppGoodsOrder appGoodsOrder)
    {
        startPage();
        List<AppGoodsOrder> list = appGoodsOrderService.selectAppGoodsOrderList(appGoodsOrder);
        return getDataTable(list);
    }

    /**
     * 导出商品订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:export')")
    @Log(title = "商品订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品订单列表")
    public void export(HttpServletResponse response, AppGoodsOrder appGoodsOrder)
    {
        List<AppGoodsOrder> list = appGoodsOrderService.selectAppGoodsOrderList(appGoodsOrder);
        ExcelUtil<AppGoodsOrder> util = new ExcelUtil<AppGoodsOrder>(AppGoodsOrder.class);
        util.exportExcel(response, list, "商品订单数据");
    }

    /**
     * 获取商品订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:query')")
    @GetMapping(value = "/{orderId}")
   
    @ApiOperation("获取商品订单详细信息")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        AppGoodsOrder appGoodsOrder = appGoodsOrderService.selectAppGoodsOrderByOrderId(orderId);
        if (appGoodsOrder != null) {
            appGoodsOrder.setAddressInfo(userAddressService.selectAppUserAddressByAddressId(appGoodsOrder.getAddressId()));
            AppGoodsOrderDetail detailWhere = new AppGoodsOrderDetail();
            detailWhere.setOrderId(orderId);
            appGoodsOrder.setOrderDetailList(orderDetailService.selectAppGoodsOrderDetailList(detailWhere));
            AppGoodsOrderAfter afterWhere = new AppGoodsOrderAfter();
            afterWhere.setOrderId(orderId);
            appGoodsOrder.setOrderAfterList(orderAfterService.selectAppGoodsOrderAfterList(afterWhere));
        }
        retailOrderStore.enrich(appGoodsOrder);
        return success(appGoodsOrder);
    }

    /**
     * 创建商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:add')")
    @Log(title = "商品订单", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("创建商品订单")
    public AjaxResult add(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        AppGoods goods = goodsService.selectAppGoodsByGoodsId(appGoodsOrder.getGoodsId());
        if (goods == null || goods.getStatus() == null || !goods.getStatus().equals("1")) {
            return AjaxResult.success("商品无效");
        }
        List<AppGoods> goodsList = new ArrayList<>();
        goodsList.add(goods);
        appGoodsOrder.setGoodsList(goodsList);
        appGoodsOrder.setDeptId(goods.getDeptId());
        AppGoodsOrder order = appGoodsOrderService.insertAppGoodsOrder(appGoodsOrder);
        return AjaxResult.success(order);
    }

    /**
     * 商品订单发起支付
     */
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "商品订单发起支付", businessType = BusinessType.UPDATE)
    @PostMapping("/pay")
   
    @ApiOperation("商品订单发起支付")
    public AjaxResult pay(AppGoodsOrder goodsOrder)
    {

        if (goodsOrder == null || goodsOrder.getOrderId() == null) {
            return error("非法订单");
        }
        AppGoodsOrder stored = appGoodsOrderService.selectAppGoodsOrderByOrderId(goodsOrder.getOrderId());
        if (stored == null || !getUserId().equals(stored.getUserId())) {
            return error("非法订单");
        }
        return appGoodsOrderService.wxpayPrepay(stored);
    }

    /**
     * 修改商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:edit')")
    @Log(title = "商品订单", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品订单")
    public AjaxResult edit(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        return toAjax(appGoodsOrderService.updateAppGoodsOrder(appGoodsOrder));
    }

    /**
     * 推进旅居履约状态。支付与退款状态由原有交易流程维护。
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:edit')")
    @Log(title = "旅居订单状态", businessType = BusinessType.UPDATE)
    @PatchMapping("/{orderId}/travel-status")
    @ApiOperation("修改旅居订单状态")
    public AjaxResult updateTravelStatus(@PathVariable Long orderId,
                                         @RequestBody TravelStatusRequest request)
    {
        return toAjax(appGoodsOrderService.updateTravelStatus(orderId, request.getTravelStatus()));
    }

    public static class TravelStatusRequest
    {
        private String travelStatus;

        public String getTravelStatus()
        {
            return travelStatus;
        }

        public void setTravelStatus(String travelStatus)
        {
            this.travelStatus = travelStatus;
        }
    }

    /**
     * 删除商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:remove')")
    @Log(title = "商品订单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{orderIds}")
   
    @ApiOperation("删除商品订单")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(appGoodsOrderService.deleteAppGoodsOrderByOrderIds(orderIds));
    }
}
