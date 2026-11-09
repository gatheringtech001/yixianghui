package com.ruoyi.web.controller.app;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.impl.AppListingPublicationService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Retains existing native-backend edit permissions; does not grant new roles. */
@RestController
public class AppListingPublicationController extends BaseController {
    @Autowired private AppListingPublicationService publications;

    @PreAuthorize("@ss.hasPermi('system:app_goods:edit')")
    @Log(title = "商品上下架", businessType = BusinessType.UPDATE)
    @PutMapping("/system/app_goods/{id}/publication")
    public AjaxResult goods(@PathVariable("id") Long id, @RequestBody Map<String, String> input) {
        return toAjax(publications.changeGoods(id, input.get("expectedStatus"), input.get("status")));
    }

    @PreAuthorize("@ss.hasPermi('system:app_activity:edit')")
    @Log(title = "活动上下架", businessType = BusinessType.UPDATE)
    @PutMapping("/system/app_activity/{id}/publication")
    public AjaxResult activity(@PathVariable("id") Long id, @RequestBody Map<String, String> input) {
        return toAjax(publications.changeActivity(id, input.get("expectedStatus"), input.get("status")));
    }
}
