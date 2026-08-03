package com.ruoyi.web.controller.app;

import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.service.*;
import com.ruoyi.web.core.config.WxPayConfig;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.exception.HttpException;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 商品订单Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@RestController
@RequestMapping("/system/app_goods_order")
@Api(tags = "商品订单管理")
public class AppGoodsOrderController extends BaseController
{
    private static Logger log = LoggerFactory.getLogger(AppGoodsOrderController.class);
    @Autowired
    private IAppGoodsOrderService appGoodsOrderService;
    @Autowired
    private IAppUserAddressService userAddressService;
    @Autowired
    private IAppGoodsOrderDetailService orderDetailService;
    @Autowired
    private IAppGoodsOrderAfterService orderAfterService;
    @Autowired
    private IAppGoodsService goodsService;

    @Autowired
    private IAppUserInfoService userInfoService;

    @Resource
    private WxPayConfig wxPayConfig;

    @Autowired
    private Config wxPayConfigRuntime;

    /**
     * 查询商品订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:list')")
    @GetMapping("/list")
   
    @ApiOperation("查询商品订单列表")
    public TableDataInfo list(AppGoodsOrder appGoodsOrder)
    {
        startPage();
        List<AppGoodsOrder> list = appGoodsOrderService.selectAppGoodsOrderList(appGoodsOrder);
        return getDataTable(list);
    }

    /**
     * 导出商品订单列表
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:export')")
    @Log(title = "商品订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
   
    @ApiOperation("导出商品订单列表")
    public void export(HttpServletResponse response, AppGoodsOrder appGoodsOrder)
    {
        List<AppGoodsOrder> list = appGoodsOrderService.selectAppGoodsOrderList(appGoodsOrder);
        ExcelUtil<AppGoodsOrder> util = new ExcelUtil<AppGoodsOrder>(AppGoodsOrder.class);
        util.exportExcel(response, list, "商品订单数据");
    }

    /**
     * 获取商品订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:query')")
    @GetMapping(value = "/{orderId}")
   
    @ApiOperation("获取商品订单详细信息")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        AppGoodsOrder appGoodsOrder = appGoodsOrderService.selectAppGoodsOrderByOrderId(orderId);
        if (appGoodsOrder != null) {
            appGoodsOrder.setAddressInfo(userAddressService.selectAppUserAddressByAddressId(appGoodsOrder.getAddressId()));
            AppGoodsOrderDetail detailWhere = new AppGoodsOrderDetail();
            detailWhere.setOrderId(orderId);
            appGoodsOrder.setOrderDetailList(orderDetailService.selectAppGoodsOrderDetailList(detailWhere));
            AppGoodsOrderAfter afterWhere = new AppGoodsOrderAfter();
            afterWhere.setOrderId(orderId);
            appGoodsOrder.setOrderAfterList(orderAfterService.selectAppGoodsOrderAfterList(afterWhere));
        }
        return success();
    }

    /**
     * 创建商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:add')")
    @Log(title = "商品订单", businessType = BusinessType.INSERT)
    @PostMapping
   
    @ApiOperation("创建商品订单")
    public AjaxResult add(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        AppGoods goods = goodsService.selectAppGoodsByGoodsId(appGoodsOrder.getGoodsId());
        if (goods == null || goods.getStatus() == null || !goods.getStatus().equals("1")) {
            return AjaxResult.success("商品无效");
        }
        List<AppGoods> goodsList = new ArrayList<>();
        goodsList.add(goods);
        appGoodsOrder.setGoodsList(goodsList);
        appGoodsOrder.setDeptId(goods.getDeptId());
        AppGoodsOrder order = appGoodsOrderService.insertAppGoodsOrder(appGoodsOrder);
        return AjaxResult.success(order);
    }

    /**
     * 商品订单发起支付
     */
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "商品订单发起支付", businessType = BusinessType.UPDATE)
    @PostMapping("/pay")
   
    @ApiOperation("商品订单发起支付")
    public AjaxResult pay(AppGoodsOrder goodsOrder)
    {

        String orderId = goodsOrder.getOrderId() + "";
        String payMoney = "1";

        AjaxResult ajaxResult = AjaxResult.success();
        JsapiServiceExtension service =
                new JsapiServiceExtension.Builder()
                        .config(wxPayConfigRuntime)
                        // 不填默认为RSA
                        .signType("RSA")
                        .build();
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(getUserId());
        try {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(wxPayConfig.getAppId());
            request.setMchid(wxPayConfig.getMerchantId());
            request.setDescription("描述");
            request.setOutTradeNo(orderId);
            // 支付成功后的回调地址
            request.setNotifyUrl(wxPayConfig.getPayNotifyUrl());
            Amount amount = new Amount();
            amount.setTotal(Integer.valueOf(payMoney));
            request.setAmount(amount);
            Payer payer = new Payer();
            payer.setOpenid(userInfo.getWeixinOpenid());
            request.setPayer(payer);
            // 调用预下单接口
            PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
            log.info("订单【{}】发起预支付成功，返回信息：{}", orderId, response);
        } catch (HttpException e) { // 发送HTTP请求失败
            log.error("微信下单发送HTTP请求失败，错误信息：{}", e.getMessage());
        } catch (ServiceException e) { // 服务返回状态小于200或大于等于300，例如500
            log.error("微信下单服务状态错误，错误信息：{}", e.getMessage());
            throw new ServiceException("下单失败");
        } catch (MalformedMessageException e) { // 服务返回成功，返回体类型不合法，或者解析返回体失败
            log.error("服务返回成功，返回体类型不合法，或者解析返回体失败，错误信息：{}", e.getMessage());
            throw new ServiceException("下单失败");
        }
        return success();
    }

    /**
     * 修改商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:edit')")
    @Log(title = "商品订单", businessType = BusinessType.UPDATE)
    @PutMapping
   
    @ApiOperation("修改商品订单")
    public AjaxResult edit(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        return toAjax(appGoodsOrderService.updateAppGoodsOrder(appGoodsOrder));
    }

    /**
     * 删除商品订单
     */
    @PreAuthorize("@ss.hasPermi('system:app_goods_order:remove')")
    @Log(title = "商品订单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{orderIds}")
   
    @ApiOperation("删除商品订单")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(appGoodsOrderService.deleteAppGoodsOrderByOrderIds(orderIds));
    }
}
