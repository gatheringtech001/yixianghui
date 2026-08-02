package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.IAppGoodsService;
import com.ruoyi.system.service.ISysUserService;
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
import com.ruoyi.system.domain.AppGoodsCart;
import com.ruoyi.system.service.IAppGoodsCartService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户购物车Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_cart")
@Api(tags = "用户购物车管理")
public class AppGoodsCartController extends BaseController
{
    @Autowired
    private IAppGoodsCartService appGoodsCartService;
    @Autowired
    private IAppGoodsService appGoodsService;
    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询用户购物车列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户购物车列表")
    public TableDataInfo list(AppGoodsCart appGoodsCart)
    {
        startPage();
        List<AppGoodsCart> list = appGoodsCartService.selectAppGoodsCartList(appGoodsCart);
        if(null!=list){
            SysUser tmpuser = null;
            for(int i=0;i<list.size();i++){
                list.get(i).setGoodsName(appGoodsService.getCacheAppGoodsById(list.get(i).getGoodsId()).getGoodsName());
                tmpuser = sysUserService.getCacheUserById(list.get(i).getUserId());
                if(null!=tmpuser) {
                    list.get(i).setUserName(tmpuser.getUserName());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出用户购物车列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:export')")
    @Log(title = "用户购物车", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户购物车列表")
    public void export(HttpServletResponse response, AppGoodsCart appGoodsCart)
    {
        List<AppGoodsCart> list = appGoodsCartService.selectAppGoodsCartList(appGoodsCart);
        ExcelUtil<AppGoodsCart> util = new ExcelUtil<AppGoodsCart>(AppGoodsCart.class);
        util.exportExcel(response, list, "用户购物车数据");
    }

    /**
     * 获取用户购物车详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:query')")
    @GetMapping(value = "/{cartId}")
   
    @ApiOperation("获取用户购物车详细信息")
    public AjaxResult getInfo(@PathVariable("cartId") Long cartId)
    {
        return success(appGoodsCartService.selectAppGoodsCartByCartId(cartId));
    }

    /**
     * 新增用户购物车
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:add')")
    @Log(title = "用户购物车", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户购物车")
    public AjaxResult add(@RequestBody AppGoodsCart appGoodsCart)
    {
        return toAjax(appGoodsCartService.insertAppGoodsCart(appGoodsCart));
    }

    /**
     * 修改用户购物车
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:edit')")
    @Log(title = "用户购物车", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户购物车")
    public AjaxResult edit(@RequestBody AppGoodsCart appGoodsCart)
    {
        return toAjax(appGoodsCartService.updateAppGoodsCart(appGoodsCart));
    }

    /**
     * 删除用户购物车
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:remove')")
    @Log(title = "用户购物车", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{cartId}")
   
    @ApiOperation("删除用户购物车")
    public AjaxResult remove(@PathVariable Long cartId)
    {
        return toAjax(appGoodsCartService.deleteAppGoodsCartByCartId(cartId));
    }

    /**
     * 清空购物车
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:remove')")
    @Log(title = "用户购物车", businessType = BusinessType.DELETE)
    @PostMapping("/clear")
   
    @ApiOperation("清空购物车")
    public AjaxResult clear()
    {
        return toAjax(appGoodsCartService.deleteAppGoodsCartByCartId(0L));
    }

    /**
     * 删除用户购物车
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_cart:remove')")
    @Log(title = "用户购物车", businessType = BusinessType.DELETE)
    @DeleteMapping("/{cartIds}")
   
    @ApiOperation("删除用户购物车")
    public AjaxResult remove(@PathVariable Long[] cartIds)
    {
        return toAjax(appGoodsCartService.deleteAppGoodsCartByCartIds(cartIds));
    }
}
