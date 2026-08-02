package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.IAppGoodsService;
import com.ruoyi.system.service.ISysUserService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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
import com.ruoyi.system.domain.AppGoodsCollect;
import com.ruoyi.system.service.IAppGoodsCollectService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品收藏Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_collect")
@Api(tags = "商品收藏管理")
public class AppGoodsCollectController extends BaseController
{
    @Autowired
    private IAppGoodsCollectService appGoodsCollectService;
    @Autowired
    private IAppGoodsService appGoodsService;
    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询商品收藏列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品收藏列表")
    public TableDataInfo list(AppGoodsCollect appGoodsCollect)
    {
        startPage();
        List<AppGoodsCollect> list = appGoodsCollectService.selectAppGoodsCollectList(appGoodsCollect);
        if(null!=list) {
            SysUser tmpuser = null;
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setGoodsName(appGoodsService.getCacheAppGoodsById(list.get(i).getGoodsId()).getGoodsName());
                tmpuser = sysUserService.getCacheUserById(list.get(i).getUserId());
                if (null != tmpuser) {
                    list.get(i).setUserName(tmpuser.getUserName());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出商品收藏列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:export')")
    @Log(title = "商品收藏", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品收藏列表")
    public void export(HttpServletResponse response, AppGoodsCollect appGoodsCollect)
    {
        List<AppGoodsCollect> list = appGoodsCollectService.selectAppGoodsCollectList(appGoodsCollect);
        ExcelUtil<AppGoodsCollect> util = new ExcelUtil<AppGoodsCollect>(AppGoodsCollect.class);
        util.exportExcel(response, list, "商品收藏数据");
    }

    /**
     * 获取商品收藏详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:query')")
    @GetMapping(value = "/{collectId}")
   
    @ApiOperation("获取商品收藏详细信息")
    public AjaxResult getInfo(@PathVariable("collectId") Long collectId)
    {
        return success(appGoodsCollectService.selectAppGoodsCollectByCollectId(collectId));
    }

    /**
     * 新增商品收藏
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:add')")
    @Log(title = "商品收藏", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增商品收藏")
    public AjaxResult add(@RequestBody AppGoodsCollect appGoodsCollect)
    {
        return toAjax(appGoodsCollectService.insertAppGoodsCollect(appGoodsCollect));
    }

    /**
     * 修改商品收藏
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:edit')")
    @Log(title = "商品收藏", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品收藏")
    public AjaxResult edit(@RequestBody AppGoodsCollect appGoodsCollect)
    {
        return toAjax(appGoodsCollectService.updateAppGoodsCollect(appGoodsCollect));
    }

    /**
     * 删除商品收藏
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:remove')")
    @Log(title = "商品收藏", businessType = BusinessType.DELETE)
	@DeleteMapping("/{collectIds}")
   
    @ApiOperation("删除商品收藏")
    public AjaxResult remove(@PathVariable Long[] collectIds)
    {
        return toAjax(appGoodsCollectService.deleteAppGoodsCollectByCollectIds(collectIds));
    }
}
