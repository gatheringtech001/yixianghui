package com.ruoyi.web.controller.app;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.RetailCheckout.Request;
import com.ruoyi.system.service.impl.RetailCheckoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mnp/app_user/retail")
@PreAuthorize("@ss.hasPermi('system:mnp:user')")
public class RetailCheckoutController extends BaseController {
    @Autowired private RetailCheckoutService checkout;
    @PostMapping("/quote") public AjaxResult quote(@RequestBody Request request) { return success(checkout.quote(getUserId(),request)); }
    @PostMapping("/submit") public AjaxResult submit(@RequestBody Request request) { return success(checkout.submit(getUserId(),request)); }
}
