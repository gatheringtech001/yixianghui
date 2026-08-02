package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.core.domain.entity.SysUser;
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
import com.ruoyi.system.domain.AppUserAddress;
import com.ruoyi.system.service.IAppUserAddressService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 用户地址Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_user_address")
@Api(tags = "用户地址管理")
public class AppUserAddressController extends BaseController
{
    @Autowired
    private IAppUserAddressService appUserAddressService;
    @Autowired
    private ISysUserService sysUserService;

    /**
     * 查询用户地址列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询用户地址列表")
    public TableDataInfo list(AppUserAddress appUserAddress)
    {
        startPage();
        List<AppUserAddress> list = appUserAddressService.selectAppUserAddressList(appUserAddress);
        if(null!= list){
            SysUser user = null;
            for(int i=0;i<list.size();i++){
                user = sysUserService.getCacheUserById(list.get(i).getUserId());
                if(null!=user){
                    list.get(i).setUserName(user.getUserName());
                }
            }
        }
        return getDataTable(list);
    }

    /**
     * 导出用户地址列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:export')")
    @Log(title = "用户地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出用户地址列表")
    public void export(HttpServletResponse response, AppUserAddress appUserAddress)
    {
        List<AppUserAddress> list = appUserAddressService.selectAppUserAddressList(appUserAddress);
        ExcelUtil<AppUserAddress> util = new ExcelUtil<AppUserAddress>(AppUserAddress.class);
        util.exportExcel(response, list, "用户地址数据");
    }

    /**
     * 获取用户地址详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:query')")
    @GetMapping(value = "/{addressId}")
   
    @ApiOperation("获取用户地址详细信息")
    public AjaxResult getInfo(@PathVariable("addressId") Long addressId)
    {
        return success(appUserAddressService.selectAppUserAddressByAddressId(addressId));
    }

    /**
     * 新增用户地址
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:add')")
    @Log(title = "用户地址", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("新增用户地址")
    public AjaxResult add(@RequestBody AppUserAddress appUserAddress)
    {
        return toAjax(appUserAddressService.insertAppUserAddress(appUserAddress));
    }

    /**
     * 修改用户地址
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:edit')")
    @Log(title = "用户地址", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改用户地址")
    public AjaxResult edit(@RequestBody AppUserAddress appUserAddress)
    {
        return toAjax(appUserAddressService.updateAppUserAddress(appUserAddress));
    }

    /**
     * 删除用户地址
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:remove')")
    @Log(title = "用户地址", businessType = BusinessType.DELETE)
    @PostMapping("/delete/{addressId}")
   
    @ApiOperation("删除用户地址")
    public AjaxResult remove(@PathVariable Long addressId)
    {
        return toAjax(appUserAddressService.deleteAppUserAddressByAddressId(addressId));
    }

    /**
     * 删除用户地址
     */
    @PreAuthorize("@ss.hasPermi('system:app_user_address:remove')")
    @Log(title = "用户地址", businessType = BusinessType.DELETE)
    @DeleteMapping("/{addressIds}")
   
    @ApiOperation("删除用户地址")
    public AjaxResult remove(@PathVariable Long[] addressIds)
    {
        return toAjax(appUserAddressService.deleteAppUserAddressByAddressIds(addressIds));
    }
}
