package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.AppGoods;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.service.IAppGoodsOrderService;
import com.ruoyi.system.service.IAppGoodsService;
import com.ruoyi.system.service.ISysUserService;
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
import com.ruoyi.system.domain.AppGoodsComment;
import com.ruoyi.system.service.IAppGoodsCommentService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品评价Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_comment")
@Api(tags = "商品评价管理")
public class AppGoodsCommentController extends BaseController
{
    @Autowired
    private IAppGoodsCommentService appGoodsCommentService;
    @Autowired
    private ISysUserService sysUserService;
    @Autowired
    private IAppGoodsService appGoodsService;
    @Autowired
    private IAppGoodsOrderService appGoodsOrderService;

    /**
     * 查询商品评价列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品评价列表")
    public TableDataInfo list(AppGoodsComment appGoodsComment)
    {
        startPage();
        List<AppGoodsComment> list = appGoodsCommentService.selectAppGoodsCommentList(appGoodsComment);
        if(null!=list){
            SysUser user = null;
            AppGoods goods = null;
            AppGoodsOrder order = null;
            for(int i=0;i<list.size();i++){
                user = sysUserService.getCacheUserById(list.get(i).getUserId());
                if(null!=user) {
                    list.get(i).setUserName(user.getUserName());
                }
                goods = appGoodsService.getCacheAppGoodsById(list.get(i).getGoodsId());
                if(null!=goods) {
                    list.get(i).setGoodsName(goods.getGoodsName());
                }
                order = appGoodsOrderService.getCacheGoodsOrder(list.get(i).getOrderId());
                if(null!=order) {
                    list.get(i).setAppGoodsOrderNo(order.getOrderNo());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出商品评价列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:export')")
    @Log(title = "商品评价", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品评价列表")
    public void export(HttpServletResponse response, AppGoodsComment appGoodsComment)
    {
        List<AppGoodsComment> list = appGoodsCommentService.selectAppGoodsCommentList(appGoodsComment);
        ExcelUtil<AppGoodsComment> util = new ExcelUtil<AppGoodsComment>(AppGoodsComment.class);
        util.exportExcel(response, list, "商品评价数据");
    }

    /**
     * 获取商品评价详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:query')")
    @GetMapping(value = "/{commentId}")
   
    @ApiOperation("获取商品评价详细信息")
    public AjaxResult getInfo(@PathVariable("commentId") Long commentId)
    {
        return success(appGoodsCommentService.selectAppGoodsCommentByCommentId(commentId));
    }

    /**
     * 新增商品评价
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:add')")
    @Log(title = "商品评价", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商品评价")
    public AjaxResult add(@RequestBody AppGoodsComment appGoodsComment)
    {
        return toAjax(appGoodsCommentService.insertAppGoodsComment(appGoodsComment));
    }

    /**
     * 修改商品评价
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:edit')")
    @Log(title = "商品评价", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品评价")
    public AjaxResult edit(@RequestBody AppGoodsComment appGoodsComment)
    {
        return toAjax(appGoodsCommentService.updateAppGoodsComment(appGoodsComment));
    }

    /**
     * 删除商品评价
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_comment:remove')")
    @Log(title = "商品评价", businessType = BusinessType.DELETE)
	@DeleteMapping("/{commentIds}")
   
    @ApiOperation("删除商品评价")
    public AjaxResult remove(@PathVariable Long[] commentIds)
    {
        return toAjax(appGoodsCommentService.deleteAppGoodsCommentByCommentIds(commentIds));
    }
}
