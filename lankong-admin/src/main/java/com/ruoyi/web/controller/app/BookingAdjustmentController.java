package com.ruoyi.web.controller.app;

import java.util.Map;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.impl.BookingAdjustmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BookingAdjustmentController extends BaseController {
    private final BookingAdjustmentService changes;
    public BookingAdjustmentController(BookingAdjustmentService changes) { this.changes = changes; }

    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/mnp/app_user/booking_adjustment/latest")
    public AjaxResult latest(@RequestParam String businessType, @RequestParam long orderId) {
        return success(changes.latest(businessType, orderId, getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/mnp/app_user/booking_adjustment/{id}/prepay")
    public AjaxResult prepay(@PathVariable long id) { return success(changes.prepay(id, getUserId())); }

    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/mnp/app_user/booking_adjustment/{id}/sync")
    public AjaxResult sync(@PathVariable long id) { return success(changes.syncPayment(id, getUserId(), false)); }

    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/mnp/app_user/booking_adjustment/{id}/cancel")
    public AjaxResult cancel(@PathVariable long id) { return success(changes.syncPayment(id, getUserId(), true)); }

    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:list')")
    @GetMapping("/system/booking_adjustment/list")
    public AjaxResult list(@RequestParam(required=false) String state) { return success(changes.list(state)); }

    @PreAuthorize("@ss.hasPermi('system:app_pay_refund_log:edit')")
    @Log(title="报名退款审核", businessType=BusinessType.UPDATE)
    @PostMapping("/system/booking_adjustment/{id}/review")
    public AjaxResult review(@PathVariable long id, @RequestBody Map<String, String> input) {
        return success(changes.review(id, SecurityUtils.getUserId(), input.get("decision"), input.get("note")));
    }
}
