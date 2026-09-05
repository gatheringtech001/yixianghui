package com.ruoyi.web.controller.app;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.impl.SupplierFulfillmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/app_supplier/fulfillment")
public class SupplierFulfillmentController extends BaseController {
    @Autowired private SupplierFulfillmentService service;
    @GetMapping("/config") @PreAuthorize("@ss.hasPermi('system:app_supplier:query')")
    public AjaxResult config() { return success(service.configuration()); }
    @GetMapping("/list") @PreAuthorize("@ss.hasPermi('system:app_goods_order:query')")
    public AjaxResult list(@RequestParam(required=false) Long supplierId) { return success(service.list(supplierId)); }
    @PostMapping("/export") @PreAuthorize("@ss.hasPermi('system:app_goods_order:export')")
    public void export(@RequestParam List<Long> orderIds, javax.servlet.http.HttpServletResponse response) {
        new com.ruoyi.common.utils.poi.ExcelUtil<>(SupplierFulfillmentService.ShipmentRow.class)
                .exportExcel(response,service.exportRows(orderIds),"供应商发货清单");
    }
    @GetMapping("/goods/{supplierId}") @PreAuthorize("@ss.hasPermi('system:app_supplier:query')")
    public AjaxResult goods(@PathVariable Long supplierId) { return success(service.goods(supplierId)); }
    @PostMapping("/goods/{supplierId}") @PreAuthorize("@ss.hasPermi('system:app_supplier:edit')")
    public AjaxResult bind(@PathVariable Long supplierId,@RequestBody List<Long> ids) { service.bind(supplierId,ids); return success(); }
    @PostMapping("/{orderId}/confirm") @PreAuthorize("@ss.hasPermi('system:app_goods_order:edit')")
    public AjaxResult confirm(@PathVariable Long orderId) { service.confirm(orderId); return success(); }
    @PostMapping("/{orderId}/ship") @PreAuthorize("@ss.hasPermi('system:app_goods_order:edit')")
    public AjaxResult ship(@PathVariable Long orderId,@RequestBody Map<String,String> body) { service.ship(orderId,body); return success(); }
    @PostMapping("/{orderId}/retry") @PreAuthorize("@ss.hasPermi('system:app_supplier:edit')")
    public AjaxResult retry(@PathVariable Long orderId) { service.retry(orderId); return success(); }
}
