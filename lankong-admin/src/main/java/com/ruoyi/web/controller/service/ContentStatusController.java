package com.ruoyi.web.controller.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.service.ContentStatusService;

/**
 * Status-only API for trusted machine integrations.
 */
@RestController
@RequestMapping("/service/content")
public class ContentStatusController extends BaseController
{
    private final ContentStatusService statusService;

    public ContentStatusController(ContentStatusService statusService)
    {
        this.statusService = statusService;
    }

    @PreAuthorize("@ss.hasPermi('service:content:goods:status')")
    @Log(title = "服务账号-商品状态", businessType = BusinessType.UPDATE)
    @PutMapping("/goods/{id}/status")
    public AjaxResult updateGoods(@PathVariable Long id, @RequestBody StatusRequest request)
    {
        return toAjax(statusService.updateGoods(id, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('service:content:activity:status')")
    @Log(title = "服务账号-活动状态", businessType = BusinessType.UPDATE)
    @PutMapping("/activity/{id}/status")
    public AjaxResult updateActivity(@PathVariable Long id, @RequestBody StatusRequest request)
    {
        return toAjax(statusService.updateActivity(id, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('service:content:article:status')")
    @Log(title = "服务账号-文章状态", businessType = BusinessType.UPDATE)
    @PutMapping("/article/{id}/status")
    public AjaxResult updateArticle(@PathVariable Long id, @RequestBody StatusRequest request)
    {
        return toAjax(statusService.updateArticle(id, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('service:content:ad-position:status')")
    @Log(title = "服务账号-广告位状态", businessType = BusinessType.UPDATE)
    @PutMapping("/ad-position/{id}/status")
    public AjaxResult updateAdPosition(@PathVariable Long id, @RequestBody StatusRequest request)
    {
        return toAjax(statusService.updateAdPosition(id, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('service:content:ad-content:status')")
    @Log(title = "服务账号-广告内容状态", businessType = BusinessType.UPDATE)
    @PutMapping("/ad-content/{id}/status")
    public AjaxResult updateAdContent(@PathVariable Long id, @RequestBody StatusRequest request)
    {
        return toAjax(statusService.updateAdContent(id, request.getStatus()));
    }

    public static class StatusRequest
    {
        private String status;

        public String getStatus()
        {
            return status;
        }

        public void setStatus(String status)
        {
            this.status = status;
        }
    }
}
