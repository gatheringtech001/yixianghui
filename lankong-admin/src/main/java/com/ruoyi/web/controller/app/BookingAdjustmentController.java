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

}
