package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.service.IAppGoodsOrderService;
import com.ruoyi.system.service.IAppGoodsService;
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
import com.ruoyi.system.domain.AppGoodsOrderAfter;
import com.ruoyi.system.service.IAppGoodsOrderAfterService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 订单商品售后Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_order_after")
@Api(tags = "订单商品售后管理")
public class AppGoodsOrderAfterController extends BaseController
{
    @Autowired
    private IAppGoodsOrderAfterService appGoodsOrderAfterService;
    @Autowired
    private IAppGoodsOrderService appGoodsOrderService;
    @Autowired
    private IAppGoodsService appGoodsService;

    /**
     * 查询订单商品售后列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询订单商品售后列表")
    public TableDataInfo list(AppGoodsOrderAfter appGoodsOrderAfter)
    {
        startPage();
        List<AppGoodsOrderAfter> list = appGoodsOrderAfterService.selectAppGoodsOrderAfterList(appGoodsOrderAfter);
        if(null!=list){
            AppGoods goods = null;
            for(int i=0;i<list.size();i++){
                goods = appGoodsService.getCacheAppGoodsById(list.get(i).getGoodsId());
                if(null!=goods){
                    list.get(i).setGoodsName(goods.getGoodsName());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出订单商品售后列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:export')")
    @Log(title = "订单商品售后", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出订单商品售后列表")
    public void export(HttpServletResponse response, AppGoodsOrderAfter appGoodsOrderAfter)
    {
        List<AppGoodsOrderAfter> list = appGoodsOrderAfterService.selectAppGoodsOrderAfterList(appGoodsOrderAfter);
        ExcelUtil<AppGoodsOrderAfter> util = new ExcelUtil<AppGoodsOrderAfter>(AppGoodsOrderAfter.class);
        util.exportExcel(response, list, "订单商品售后数据");
    }

    /**
     * 获取订单商品售后详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:query')")
    @GetMapping(value = "/{afterId}")
   
    @ApiOperation("获取订单商品售后详细信息")
    public AjaxResult getInfo(@PathVariable("afterId") Long afterId)
    {
        return success(appGoodsOrderAfterService.selectAppGoodsOrderAfterByAfterId(afterId));
    }

    /**
     * 新增订单商品售后
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:add')")
    @Log(title = "订单商品售后", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增订单商品售后")
    public AjaxResult add(@RequestBody AppGoodsOrderAfter appGoodsOrderAfter)
    {
        return toAjax(appGoodsOrderAfterService.insertAppGoodsOrderAfter(appGoodsOrderAfter));
    }

    /**
     * 修改订单商品售后
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:edit')")
    @Log(title = "订单商品售后", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改订单商品售后")
    public AjaxResult edit(@RequestBody AppGoodsOrderAfter appGoodsOrderAfter)
    {
        return toAjax(appGoodsOrderAfterService.updateAppGoodsOrderAfter(appGoodsOrderAfter));
    }

    /**
     * 删除订单商品售后
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:remove')")
    @Log(title = "订单商品售后", businessType = BusinessType.DELETE)
	@DeleteMapping("/{afterIds}")
   
    @ApiOperation("删除订单商品售后")
    public AjaxResult remove(@PathVariable Long[] afterIds)
    {
        return toAjax(appGoodsOrderAfterService.deleteAppGoodsOrderAfterByAfterIds(afterIds));
    }

    @PreAuthorize("@ss.hasPermi('system:app_goods_order_after:edit')")
    @Log(title = "退款微信对接", businessType = BusinessType.UPDATE)
    @PostMapping("/refundPrepay")
    public AjaxResult refundPrepay(@RequestBody AppGoodsOrderAfter appGoodsOrderAfter){
        if (appGoodsOrderAfter == null || appGoodsOrderAfter.getAfterId() == null) {
            return error("售后单无效");
        }
        // 以库中售后单为准，合并审核入参（避免把管理员ID写成用户ID）
        AppGoodsOrderAfter dbAfter = appGoodsOrderAfterService.selectAppGoodsOrderAfterByAfterId(appGoodsOrderAfter.getAfterId());
        if (dbAfter == null) {
            return error("售后单不存在");
        }
        if (StringUtils.isNotEmpty(appGoodsOrderAfter.getStatus())) {
            dbAfter.setStatus(appGoodsOrderAfter.getStatus());
        }
        if (appGoodsOrderAfter.getRefundMoney() != null) {
            dbAfter.setRefundMoney(appGoodsOrderAfter.getRefundMoney());
        }
        if (StringUtils.isNotEmpty(appGoodsOrderAfter.getRemark())) {
            dbAfter.setRemark(appGoodsOrderAfter.getRemark());
        }
        if (StringUtils.isNotEmpty(appGoodsOrderAfter.getOutOrderNo())) {
            dbAfter.setOutOrderNo(appGoodsOrderAfter.getOutOrderNo());
        }
        if (appGoodsOrderAfter.getOrderMoney() != null) {
            dbAfter.setOrderMoney(appGoodsOrderAfter.getOrderMoney());
        } else if (dbAfter.getOrderMoney() == null && dbAfter.getGoodsMoney() != null) {
            dbAfter.setOrderMoney(dbAfter.getGoodsMoney());
        }
        return appGoodsOrderService.wxpayRefund(dbAfter);
    }
}
