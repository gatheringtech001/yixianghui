package com.ruoyi.web.controller.app;



import java.util.List;

import javax.servlet.http.HttpServletResponse;



import com.ruoyi.system.service.ISysDeptService;

import io.swagger.annotations.Api;

import io.swagger.annotations.ApiImplicitParam;

import io.swagger.annotations.ApiOperation;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;

import com.ruoyi.common.core.controller.BaseController;

import com.ruoyi.common.core.domain.AjaxResult;

import com.ruoyi.common.core.page.TableDataInfo;

import com.ruoyi.common.enums.BusinessType;

import com.ruoyi.common.utils.poi.ExcelUtil;

import com.ruoyi.system.domain.AppGoods;

import com.ruoyi.system.service.IAppGoodsService;



/**

 * 商品Controller

 * 

 * @author lankong

 * @date 2025-03-31

 */

@Api(tags = "商品管理")

@RestController

@RequestMapping("/system/app_goods")

public class AppGoodsController extends BaseController

{

    @Autowired

    private IAppGoodsService appGoodsService;



    @Autowired

    private ISysDeptService deptService;



    /**

     * 查询商品列表

     */

    @ApiOperation("查询商品列表")

    @PreAuthorize("@ss.hasPermi('system:app_goods:list')")

    @GetMapping("/list")

    public TableDataInfo list(AppGoods appGoods)

    {

        startPage();

        List<AppGoods> list = appGoodsService.selectAppGoodsList(appGoods);

        for(int i = 0; i < list.size(); i++){

            list.get(i).setDeptName(deptService.getCacheDeptNameById(list.get(i).getDeptId()));

        }



        return getDataTable(list);

    }



    /**

     * 导出商品列表

     */

    @ApiOperation("导出商品列表")

    @PreAuthorize("@ss.hasPermi('system:app_goods:export')")

    @Log(title = "商品", businessType = BusinessType.EXPORT)

    @PostMapping("/export")

    public void export(HttpServletResponse response, AppGoods appGoods)

    {

        List<AppGoods> list = appGoodsService.selectAppGoodsList(appGoods);

        ExcelUtil<AppGoods> util = new ExcelUtil<AppGoods>(AppGoods.class);

        util.exportExcel(response, list, "商品数据");

    }



    /**

     * 获取商品详细信息

     */

    @ApiOperation("获取商品详细信息")

    @ApiImplicitParam(name = "goodsId", value = "商品ID", required = true, dataType = "long", paramType = "path", dataTypeClass = Long.class)

    @PreAuthorize("@ss.hasPermi('system:app_goods:query')")

    @GetMapping(value = "/{goodsId}")

    public AjaxResult getInfo(@PathVariable("goodsId") Long goodsId)

    {

        return success(appGoodsService.selectAppGoodsByGoodsId(goodsId));

    }



    /**

     * 新增商品

     */

    @ApiOperation("新增商品")

    @PreAuthorize("@ss.hasPermi('system:app_goods:add')")

    @Log(title = "商品", businessType = BusinessType.INSERT)

    @PostMapping

    public AjaxResult add(@RequestBody AppGoods appGoods)

    {

        return toAjax(appGoodsService.insertAppGoods(appGoods));

    }



    /**

     * 修改商品

     */

    @ApiOperation("修改商品")

    @PreAuthorize("@ss.hasPermi('system:app_goods:edit')")

    @Log(title = "商品", businessType = BusinessType.UPDATE)

    @PutMapping

    public AjaxResult edit(@RequestBody AppGoods appGoods)

    {

        return toAjax(appGoodsService.updateAppGoods(appGoods));

    }



    /**

     * 删除商品

     */

    @ApiOperation("删除商品")

    @ApiImplicitParam(name = "goodsIds", value = "商品ID数组", required = true, dataType = "long", paramType = "path", allowMultiple = true, dataTypeClass = Long.class)

    @PreAuthorize("@ss.hasPermi('system:app_goods:remove')")

    @Log(title = "商品", businessType = BusinessType.DELETE)

    @DeleteMapping("/{goodsIds}")

    public AjaxResult remove(@PathVariable Long[] goodsIds)

    {

        return toAjax(appGoodsService.deleteAppGoodsByGoodsIds(goodsIds));

    }

}


