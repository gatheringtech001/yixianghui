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
import com.ruoyi.system.domain.AppActivityOrder;
import com.ruoyi.system.domain.AppActivity;
import com.ruoyi.system.service.IAppActivityOrderService;
import com.ruoyi.system.service.IAppActivityService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 活动预约Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_activity_order")
@Api(tags = "活动预约管理")
public class AppActivityOrderController extends BaseController
{
    @Autowired
    private IAppActivityOrderService appActivityOrderService;

    @Autowired
    private IAppActivityService appActivityService;

    /**
     * 查询活动预约列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询活动预约列表")
    public TableDataInfo list(AppActivityOrder appActivityOrder)
    {
        startPage();
        List<AppActivityOrder> list = appActivityOrderService.selectAppActivityOrderList(appActivityOrder);
        for (AppActivityOrder order : list) {
            if (order.getActivityId() != null) {
                AppActivity activity = appActivityService.selectAppActivityByActivityId(order.getActivityId());
                order.setActivityInfo(activity);
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出活动预约列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:export')")
    @Log(title = "活动预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出活动预约列表")
    public void export(HttpServletResponse response, AppActivityOrder appActivityOrder)
    {
        List<AppActivityOrder> list = appActivityOrderService.selectAppActivityOrderList(appActivityOrder);
        ExcelUtil<AppActivityOrder> util = new ExcelUtil<AppActivityOrder>(AppActivityOrder.class);
        util.exportExcel(response, list, "活动预约数据");
    }

    /**
     * 获取活动预约详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:query')")
    @GetMapping(value = "/{orderId}")
   
    @ApiOperation("获取活动预约详细信息")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        AppActivityOrder order = appActivityOrderService.selectAppActivityOrderByOrderId(orderId);
        if (order != null && order.getActivityId() != null) {
            order.setActivityInfo(appActivityService.selectAppActivityByActivityId(order.getActivityId()));
        }
        return success(order);
    }

    /**
     * 新增活动预约
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:add')")
    @Log(title = "活动预约", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增活动预约")
    public AjaxResult add(@RequestBody AppActivityOrder appActivityOrder)
    {
        return toAjax(appActivityOrderService.insertAppActivityOrder(appActivityOrder));
    }

    /**
     * 修改活动预约
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:edit')")
    @Log(title = "活动预约", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改活动预约")
    public AjaxResult edit(@RequestBody AppActivityOrder appActivityOrder)
    {
        return toAjax(appActivityOrderService.updateAppActivityOrder(appActivityOrder));
    }

    /**
     * 删除活动预约
     */
    @PreAuthorize("@ss.hasPermi('system:app_activity_order:remove')")
    @Log(title = "活动预约", businessType = BusinessType.DELETE)
	@DeleteMapping("/{orderIds}")
   
    @ApiOperation("删除活动预约")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(appActivityOrderService.deleteAppActivityOrderByOrderIds(orderIds));
    }
}
