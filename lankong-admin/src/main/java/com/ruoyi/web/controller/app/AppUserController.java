package com.ruoyi.web.controller.app;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.vo.ConsultantStatVo;
import com.ruoyi.system.service.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;

/**
 * 用户中心Controller
 * 
 * @author lankong
 * @date 2025-04-06
 */
@Api("用户接口控制器")
@RestController
@RequestMapping("/mnp/app_user")
public class AppUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;
    @Autowired
    private IAppCardService cardService;
    @Autowired
    private IAppUserCardService userCardService;
    @Autowired
    private IAppConsultantService consultantService;
    @Autowired
    private IAppGoodsCollectService collectService;
    @Autowired
    private IAppGoodsCartService cartService;
    @Autowired
    private IAppGoodsCouponGotService couponGotService;
    @Autowired
    private IAppGoodsCouponService couponService;
    @Autowired
    private IAppGoodsService goodsService;
    @Autowired
    private IAppGoodsOrderService goodsOrderService;
    @Autowired
    private IAppUserAddressService userAddressService;
    @Autowired
    private IAppGoodsOrderDetailService orderDetailService;
    @Autowired
    private IAppGoodsOrderAfterService orderAfterService;
    @Autowired
    private IAppUserGoldLogService goldLogService;
    @Autowired
    private IAppUserService appUserService;
    @Autowired
    private IAppUserInfoService userInfoService;
    @Autowired
    private IAppUserInviterService inviterService;
    @Autowired
    private IAppCustomerService customerService;
    @Autowired
    private IAppActivityOrderService activityOrderService;
    @Autowired
    private IAppActivityService activityService;
    @Autowired
    private IAppConsultantMnpService consultantMnpService;
    @Autowired
    private IAppCustomerIncomeService customerIncomeService;
    @Autowired
    private IAppUserCashService userCashService;
    @Autowired
    private IAppPayLogService payLogService;
    @Autowired
    private IAppGoodsOrderAfterService appGoodsOrderAfterService;
    @Autowired
    private IAppCustomerService appCustomerService;
    @Autowired
    private IAppGoodsSkuDataService skuDataService;
    /**
     * 小程序用户登录
     */

    /**
     * 获取个人信息
     */
    @ApiOperation("获取个人信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = { "/data" })
    public AjaxResult getInfo()
    {
        Long userId = getUserId();
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userId);
            if (userInfo == null) {
                userInfo = userInfoService.initUserInfo(userId);
            }
            ajax.put("userInfo", userInfo);
            ajax.put("userCard", userCardService.selectAppUserCardByUserId(userId));
            String mobile = sysUser != null ? sysUser.getPhonenumber() : null;
            ajax.put("consultant", consultantService.getOrClaimConsultantByUser(userId, mobile));
            AppCustomer customer = customerService.selectAppCustomerByUserId(userId);
            ajax.put("liveAddress", customer != null ? customer.getLiveAddress() : null);
        }
        return ajax;
    }

    /**
     * 微信一键授权登录（绑定手机号 + 自动补全昵称头像）
     * 专用接口，不依赖 SysUser 入参校验，避免「用户账号不能为空」等问题
     */
    @ApiOperation("微信一键授权完善资料")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/wx_profile_auth")
    public AjaxResult wxProfileAuth(@RequestBody Map<String, String> body)
    {
        String phoneCode = body != null ? body.get("phoneCode") : null;
        SysUser user = appUserService.completeWxProfile(getUserId(), phoneCode);
        AjaxResult ajax = AjaxResult.success();
        ajax.put(AjaxResult.DATA_TAG, user);
        ajax.put("mobile", user != null ? user.getPhonenumber() : null);
        return ajax;
    }

    /**
     * 修改个人信息（昵称、头像；不含手机号，手机号请走 wx_profile_auth）
     */
    @ApiOperation("修改个人信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "修改个人信息", businessType = BusinessType.UPDATE)
    @PostMapping("/update_by_mnp")
    public AjaxResult updateByMnp(@RequestBody AppUserMnpUpdateBody body)
    {
        if (body == null)
        {
            return error("参数不能为空");
        }
        if (StringUtils.isNotEmpty(body.getPhoneCode()))
        {
            return wxProfileAuth(Collections.singletonMap("phoneCode", body.getPhoneCode()));
        }
        Long userId = getUserId();
        SysUser user = new SysUser();
        user.setUserId(userId);
        if (StringUtils.isNotEmpty(body.getNickName()))
        {
            user.setNickName(body.getNickName());
        }
        if (StringUtils.isNotEmpty(body.getAvatar()))
        {
            user.setAvatar(body.getAvatar());
        }
        boolean hasLiveAddress = body.getLiveAddress() != null;
        if (StringUtils.isEmpty(user.getNickName()) && StringUtils.isEmpty(user.getAvatar()) && !hasLiveAddress)
        {
            return error("没有需要更新的资料");
        }
        int rows = 0;
        if (StringUtils.isNotEmpty(user.getNickName()) || StringUtils.isNotEmpty(user.getAvatar()))
        {
            rows += appUserService.updateByMnp(user);
        }
        if (hasLiveAddress)
        {
            SysUser sysUser = userService.selectUserById(userId);
            String customerName = StringUtils.isNotEmpty(user.getNickName())
                ? user.getNickName()
                : (sysUser != null ? sysUser.getNickName() : null);
            String linkMobile = sysUser != null ? sysUser.getPhonenumber() : null;
            rows += customerService.saveLiveAddressByUserId(
                userId, body.getLiveAddress(), customerName, linkMobile);
        }
        return toAjax(rows);
    }

    /**
     * 绑定微信授权手机号（兼容旧调用，内部走 wx_profile_auth）
     */
    @ApiOperation("绑定微信授权手机号")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/bind_phone")
    public AjaxResult bindPhone(@RequestBody Map<String, String> body)
    {
        String code = body != null ? body.get("code") : null;
        if (StringUtils.isEmpty(code))
        {
            code = body != null ? body.get("phoneCode") : null;
        }
        return wxProfileAuth(Collections.singletonMap("phoneCode", code));
    }

    /**
     * 获取推广链接
     */

    /**
     * 查询商品收藏列表
     */
    @ApiOperation("查询收藏列表")
    @GetMapping("/collect/list")
    public TableDataInfo collect_list(AppGoodsCollect appGoodsCollect)
    {
        startPage();
        appGoodsCollect.setUserId(getUserId());
        List<AppGoodsCollect> list = collectService.selectAppGoodsCollectList(appGoodsCollect);
        for (AppGoodsCollect goodsCollect : list) {
            fillCollectDetail(goodsCollect);
        }
        return getDataTable(list);
    }

    /**
     * 获取商品收藏详细信息
     */
    @ApiOperation("获取商品收藏详细信息")
    @PreAuthorize("@ss.hasPermi('system:app_goods_collect:query')")
    @GetMapping(value = "/collect/{collectId}")
    public AjaxResult collect_getInfo(@PathVariable("collectId") Long collectId)
    {
        AppGoodsCollect collect = collectService.selectAppGoodsCollectByCollectId(collectId);
        if (collect == null || collect.getUserId() == null
                || collect.getUserId().longValue() != getUserId().longValue()) {
            return error("收藏不存在");
        }
        fillCollectDetail(collect);
        return success(collect);
    }

    /**
     * 新增商品收藏
     */
    @ApiOperation("新增收藏")
    @Log(title = "商品收藏", businessType = BusinessType.INSERT)
    @PostMapping("/collect/add")
    public AjaxResult collect_add(@RequestBody AppGoodsCollect appGoodsCollect)
    {
        Long userId = getUserId();
        String collectType = StringUtils.defaultIfBlank(appGoodsCollect.getCollectType(), "goods");
        if (!"goods".equals(collectType) && !"activity".equals(collectType)) {
            return error("不支持的收藏类型");
        }
        appGoodsCollect.setUserId(userId);
        appGoodsCollect.setCollectType(collectType);
        appGoodsCollect.setStatus("1");

        AppGoodsCollect where = new AppGoodsCollect();
        where.setUserId(userId);
        where.setCollectType(collectType);
        where.setStatus("1");
        if ("activity".equals(collectType)) {
            if (appGoodsCollect.getActivityId() == null) {
                return error("活动id不能为空");
            }
            appGoodsCollect.setGoodsId(null);
            where.setActivityId(appGoodsCollect.getActivityId());
            AppActivity activity = activityService.selectAppActivityByActivityId(appGoodsCollect.getActivityId());
            if (activity == null) {
                return error("活动不存在");
            }
        } else {
            if (appGoodsCollect.getGoodsId() == null) {
                return error("商品id不能为空");
            }
            appGoodsCollect.setActivityId(null);
            where.setGoodsId(appGoodsCollect.getGoodsId());
            AppGoods goods = goodsService.selectAppGoodsByGoodsId(appGoodsCollect.getGoodsId());
            if (goods == null) {
                return error("商品不存在");
            }
        }
        List<AppGoodsCollect> exists = collectService.selectAppGoodsCollectList(where);
        if (exists != null && !exists.isEmpty()) {
            AppGoodsCollect existed = exists.get(0);
            fillCollectDetail(existed);
            return AjaxResult.success("已收藏", existed);
        }
        int rows = collectService.insertAppGoodsCollect(appGoodsCollect);
        if (rows > 0) {
            fillCollectDetail(appGoodsCollect);
            return AjaxResult.success("收藏成功", appGoodsCollect);
        }
        return error("收藏失败");
    }

    /**
     * 删除商品收藏
     */
    @ApiOperation("删除收藏")
    @Log(title = "商品收藏", businessType = BusinessType.DELETE)
    @PostMapping("/collect/delete")
    public AjaxResult collect_remove(@RequestParam("collectId") Long collectId)
    {
        if (ObjectUtils.isEmpty(collectId)) {
            return error();
        }
        AppGoodsCollect collect = collectService.selectAppGoodsCollectByCollectId(collectId);
        if (collect == null || collect.getUserId() == null
                || collect.getUserId().longValue() != getUserId().longValue()) {
            return error("收藏不存在");
        }
        return toAjax(collectService.deleteAppGoodsCollectByCollectId(collectId));
    }

    private void fillCollectDetail(AppGoodsCollect collect)
    {
        if (collect == null) {
            return;
        }
        String type = StringUtils.defaultIfBlank(collect.getCollectType(), "goods");
        collect.setCollectType(type);
        if ("activity".equals(type)) {
            if (collect.getActivityId() != null) {
                collect.setActivityInfo(activityService.selectAppActivityByActivityId(collect.getActivityId()));
            }
        } else if (collect.getGoodsId() != null) {
            collect.setGoodsInfo(goodsService.selectAppGoodsByGoodsId(collect.getGoodsId()));
        }
    }

    /**
     * 查询用户购物车列表
     */
    @ApiOperation("查询用户购物车列表")
    @GetMapping("/cart/list")
    public TableDataInfo cart_list(AppGoodsCart appGoodsCart)
    {
        startPage();
        appGoodsCart.setUserId(getUserId());
        List<AppGoodsCart> list = cartService.selectAppGoodsCartList(appGoodsCart);
        for (AppGoodsCart goodsCart : list) {
            goodsCart.setGoodsInfo(goodsService.selectAppGoodsByGoodsId(goodsCart.getGoodsId()));
        }
        return getDataTable(list);
    }

    /**
     * 新增用户购物车
     */
    @ApiOperation("新增购物车商品")
    @Log(title = "用户购物车", businessType = BusinessType.INSERT)
    @PostMapping("/cart/add")
    public AjaxResult cart_add(@RequestBody AppGoodsCart appGoodsCart)
    {
        AjaxResult invalid = validateCartRequest(appGoodsCart);
        if (invalid != null) {
            return invalid;
        }
        Long userId = getUserId();
        AppGoods goods = goodsService.selectAppGoodsByGoodsId(appGoodsCart.getGoodsId());
        invalid = validateCartGoods(goods, appGoodsCart.getGoodsCount());
        if (invalid != null) {
            return invalid;
        }
        appGoodsCart.setUserId(userId);
        appGoodsCart.setIsSku(appGoodsCart.getIsSku() == null ? 0L : appGoodsCart.getIsSku());
        appGoodsCart.setDataId(appGoodsCart.getDataId() == null ? 0L : appGoodsCart.getDataId());
        appGoodsCart.setStatus("1");
        AppGoodsCart existing = findCartItem(appGoodsCart);
        if (existing == null) {
            return toAjax(cartService.insertAppGoodsCart(appGoodsCart));
        }
        int nextCount = existing.getGoodsCount() + appGoodsCart.getGoodsCount();
        invalid = validateCartGoods(goods, nextCount);
        if (invalid != null) {
            return invalid;
        }
        existing.setGoodsCount(nextCount);
        return toAjax(cartService.updateAppGoodsCart(existing));
    }

    /**
     * 修改用户购物车
     */
    @ApiOperation("修改购物车商品")
    @Log(title = "用户购物车", businessType = BusinessType.UPDATE)
    @PostMapping("/cart/edit")
    public AjaxResult cart_edit(@RequestBody AppGoodsCart appGoodsCart)
    {
        if (appGoodsCart == null || appGoodsCart.getCartId() == null
                || appGoodsCart.getGoodsCount() == null || appGoodsCart.getGoodsCount() < 1) {
            return error("购物车数量无效");
        }
        AppGoodsCart stored = ownedCart(appGoodsCart.getCartId());
        if (stored == null) {
            return error("购物车商品不存在");
        }
        AjaxResult invalid = validateCartGoods(
                goodsService.selectAppGoodsByGoodsId(stored.getGoodsId()),
                appGoodsCart.getGoodsCount());
        if (invalid != null) {
            return invalid;
        }
        stored.setGoodsCount(appGoodsCart.getGoodsCount());
        return toAjax(cartService.updateAppGoodsCart(stored));
    }

    /**
     * 删除用户购物车商品
     */
    @ApiOperation("删除购物车商品")
    @Log(title = "用户购物车", businessType = BusinessType.DELETE)
    @PostMapping("/cart/delete")
    public AjaxResult cart_remove(@RequestParam("cartId") Long cartId)
    {
        if (ownedCart(cartId) == null) {
            return error("购物车商品不存在");
        }
        return toAjax(cartService.deleteAppGoodsCartByCartId(cartId));
    }

    private AjaxResult validateCartRequest(AppGoodsCart cart)
    {
        if (cart == null || cart.getGoodsId() == null
                || cart.getGoodsCount() == null || cart.getGoodsCount() < 1) {
            return error("购物车参数无效");
        }
        return null;
    }

    private AjaxResult validateCartGoods(AppGoods goods, int count)
    {
        if (goods == null || !"1".equals(goods.getStatus()) || !"online".equals(goods.getGoodsType())) {
            return error("商品无效或已下架");
        }
        if (goods.getStock() == null || goods.getStock() < count) {
            return error("商品库存不足");
        }
        return null;
    }

    private AppGoodsCart ownedCart(Long cartId)
    {
        if (cartId == null) {
            return null;
        }
        AppGoodsCart cart = cartService.selectAppGoodsCartByCartId(cartId);
        return cart != null && getUserId().equals(cart.getUserId()) ? cart : null;
    }

    private AppGoodsCart findCartItem(AppGoodsCart candidate)
    {
        AppGoodsCart query = new AppGoodsCart();
        query.setUserId(candidate.getUserId());
        query.setGoodsId(candidate.getGoodsId());
        query.setIsSku(candidate.getIsSku());
        query.setDataId(candidate.getDataId());
        List<AppGoodsCart> matches = cartService.selectAppGoodsCartList(query);
        return matches == null || matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * 清空购物车
     */
    @ApiOperation("清空购物车商品")
    @Log(title = "用户购物车", businessType = BusinessType.DELETE)
    @PostMapping("/cart/clear")
    public AjaxResult cart_clear()
    {
        QueryWrapper<AppGoodsCart> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", getUserId());
        return toAjax(cartService.remove(wrapper));
    }

    /**
     * 获取我的优惠券列表
     */
    @ApiOperation("获取我的优惠券列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/get_coupon_list")
    public TableDataInfo couponList(AppGoodsCouponGot couponGot)
    {
        startPage();
        couponGot.setUserId(getUserId());
//        appGoodsCouponGot.setStatus("1");
        List<AppGoodsCouponGot> list = couponGotService.selectAppGoodsCouponGotList(couponGot);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setCouponInfo(couponService.selectAppGoodsCouponByCouponId(list.get(i).getCouponId()));
        }
        return getDataTable(list);
    }

    /**
     * 获取可用优惠券（预提交订单）
     */
    @ApiOperation("订单获取可用优惠券")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/get_coupon_enable_list")
    public TableDataInfo enableList(@RequestParam(name = "goodsId", required = true) String goodsId)
    {
        startPage();
        AppGoodsCouponGot appGoodsCouponGot = new AppGoodsCouponGot();
        appGoodsCouponGot.setUserId(getUserId());
        appGoodsCouponGot.setIsUsed(0);
        appGoodsCouponGot.setStatus("1");
        List<AppGoodsCouponGot> list = couponGotService.selectAppGoodsCouponGotList(appGoodsCouponGot);
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setCouponInfo(couponService.selectAppGoodsCouponByCouponId(list.get(i).getCouponId()));
        }
        Date now = new Date();
        list.removeIf(got -> got.getCouponInfo() == null
                || !"1".equals(got.getCouponInfo().getStatus())
                || (got.getCouponInfo().getGoodsId() != null && got.getCouponInfo().getGoodsId() > 0
                    && !goodsId.equals(String.valueOf(got.getCouponInfo().getGoodsId())))
                || (got.getCouponInfo().getEnableStartTime() != null
                    && now.before(got.getCouponInfo().getEnableStartTime()))
                || (got.getCouponInfo().getEnableEndTime() != null
                    && now.after(got.getCouponInfo().getEnableEndTime())));
        return getDataTable(list);
    }

    @ApiOperation("获取渠道优惠")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/distribution_offer")
    public AjaxResult distributionOffer(@RequestParam String channelCode,
                                        @RequestParam(required = false) String sourceAppId,
                                        @RequestParam(required = false) String scene) {
        return success(couponGotService.getDistributionOffer(getUserId(), channelCode, sourceAppId, scene));
    }

    @ApiOperation("领取渠道优惠券")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/claim_distribution_coupon")
    public AjaxResult claimDistributionCoupon(@RequestBody Map<String, String> body) {
        if (body == null) {
            return error("领取参数无效");
        }
        return success(couponGotService.claimDistributionCoupon(getUserId(),
                body.get("channelCode"), body.get("sourceAppId")));
    }

    /**
     * 提交订单
     */
    @ApiOperation("创建商品订单")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "商品订单", businessType = BusinessType.INSERT)
    @PostMapping("/add_goods_order")
    public AjaxResult add_goods_order(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        try {
            if (appGoodsOrder == null || appGoodsOrder.getGoodsId() == null) {
                return AjaxResult.error("商品无效");
            }
            AppGoods goods = goodsService.selectAppGoodsByGoodsId(appGoodsOrder.getGoodsId());
            if (goods == null || goods.getStatus() == null || !goods.getStatus().equals("1")) {
                return AjaxResult.error("商品无效或已下架");
            }
            if ("online".equals(goods.getGoodsType())) {
                if (appGoodsOrder.getGoodsCount() == null || appGoodsOrder.getGoodsCount() < 1) {
                    return AjaxResult.error("商品数量无效");
                }
                AppUserAddress address = appGoodsOrder.getAddressId() == null ? null
                        : userAddressService.selectAppUserAddressByAddressId(appGoodsOrder.getAddressId());
                if (address == null || address.getUserId() == null
                        || !address.getUserId().equals(getUserId())) {
                    return AjaxResult.error("收货地址无效");
                }
            }
            if ("hotel".equals(goods.getGoodsType())) {
                if (appGoodsOrder.getCheckInDate() == null || appGoodsOrder.getCheckOutDate() == null) {
                    return AjaxResult.error("请选择入住和退房日期");
                }
                if (appGoodsOrder.getSkuId() == null) {
                    return AjaxResult.error("请选择房型套餐");
                }
                if (appGoodsOrder.getGoodsCount() == null || appGoodsOrder.getGoodsCount() < 1) {
                    return AjaxResult.error("请选择房间数量");
                }
            }
            List<AppGoods> goodsList = new ArrayList<>();
            goodsList.add(goods);
            appGoodsOrder.setGoodsList(goodsList);
            appGoodsOrder.setUserId(getUserId());
            appGoodsOrder.setDeptId(goods.getDeptId());
            if (appGoodsOrder.getAddressId() == null) {
                appGoodsOrder.setAddressId(0L);
            }
            AppGoodsOrder order = goodsOrderService.insertAppGoodsOrder(appGoodsOrder);
            if (order == null || order.getOrderId() == null) {
                return AjaxResult.error("订单创建失败");
            }
            return AjaxResult.success(order);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            logger.error("创建商品订单失败 goodsId={}", appGoodsOrder != null ? appGoodsOrder.getGoodsId() : null, e);
            return AjaxResult.error(StringUtils.defaultIfBlank(e.getMessage(), "订单创建失败，请稍后重试"));
        }
    }

    /**
     * 订单列表
     */
    @ApiOperation("获取商品订单列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = "/get_goods_order_list")
    public TableDataInfo getGoodsOrderList(AppGoodsOrder appGoodsOrder)
    {
        startPage();
        appGoodsOrder.setUserId(getUserId());
        List<AppGoodsOrder> list = goodsOrderService.selectAppGoodsOrderList(appGoodsOrder);
        AppGoodsSkuData skuData = new AppGoodsSkuData();
        List<AppGoodsOrderDetail> tmpdetails;
        AppGoodsOrderDetail detailWhere;
        AppGoodsOrder oneorder;
        List<AppGoodsSkuOption> skuOptions;
        int comboxindex = 0;
        int mycomboxindex = 0;
        for (int i = 0; i < list.size(); i++) {
            AppGoods goods = goodsService.selectAppGoodsByGoodsId(list.get(i).getGoodsId());
            oneorder = list.get(i);
            if (goods.getIsSku() == 1) {
                detailWhere = new AppGoodsOrderDetail();
                detailWhere.setOrderId(list.get(i).getOrderId());
                tmpdetails = orderDetailService.selectAppGoodsOrderDetailList(detailWhere);
                if (tmpdetails != null && !tmpdetails.isEmpty()) {
                    AppGoodsOrderDetail listDetail = tmpdetails.get(0);
                    mergeHotelOrderDetail(oneorder, listDetail);
                    applyHotelGoodsPresentation(goods, listDetail);
                    mycomboxindex = resolveSelComboIndex(goods, listDetail);
                    // 列表单价以订单明细金额为准，避免展示当前套餐改价后的价格导致与总金额不一致
                    if (listDetail.getGoodsMoney() != null) {
                        goods.setPrice(listDetail.getGoodsMoney());
                    } else if (oneorder.getPayMoney() != null && oneorder.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
                        goods.setPrice(oneorder.getPayMoney());
                    } else if (oneorder.getMoneyPayable() != null) {
                        goods.setPrice(oneorder.getMoneyPayable());
                    }
                }
            }
            oneorder.setSelComboIndex(mycomboxindex);
            // 已支付：按微信实付回写，保证列表/售后金额与真实支付一致
            if ("1".equals(StringUtils.defaultIfBlank(oneorder.getPayStatus(), ""))
                    || "1".equals(StringUtils.defaultIfBlank(oneorder.getStatus(), ""))) {
                BigDecimal actual = goodsOrderService.healPaidAmountIfNeeded(oneorder);
                if (actual != null && actual.compareTo(BigDecimal.ZERO) > 0) {
                    oneorder.setPayMoney(actual);
                    oneorder.setMoneyPayable(actual);
                    goods.setPrice(actual);
                }
            } else if (oneorder.getMoneyPayable() != null) {
                goods.setPrice(oneorder.getMoneyPayable());
            }
            List<AppGoods> goodsList = new ArrayList<>();
            goodsList.add(goods);
            oneorder.setGoodsList(goodsList);
            // 附带售后记录，供前端展示「售后已拒绝」等反馈
            AppGoodsOrderAfter afterWhere = new AppGoodsOrderAfter();
            afterWhere.setOrderId(oneorder.getOrderId());
            oneorder.setOrderAfterList(orderAfterService.selectAppGoodsOrderAfterList(afterWhere));
            list.set(i,oneorder);
        }
        return getDataTable(list);
    }

    /**
     * 查询订单
     */
    @ApiOperation("获取商品订单详细信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = "/get_goods_order")
    public AjaxResult getGoodsOrderInfo(@RequestParam(name = "orderId", required = true) Long orderId)
    {
        AppGoodsOrder goodsOrder = goodsOrderService.selectAppGoodsOrderByOrderId(orderId);
        if (goodsOrder == null || !getUserId().equals(goodsOrder.getUserId())) {
            return error("非法订单");
        }
        if (goodsOrder != null) {
            if ("1".equals(StringUtils.defaultIfBlank(goodsOrder.getPayStatus(), ""))
                    || "1".equals(StringUtils.defaultIfBlank(goodsOrder.getStatus(), ""))) {
                BigDecimal actual = goodsOrderService.healPaidAmountIfNeeded(goodsOrder);
                if (actual != null && actual.compareTo(BigDecimal.ZERO) > 0) {
                    goodsOrder.setPayMoney(actual);
                    goodsOrder.setMoneyPayable(actual);
                }
            }
            AppGoods goods = goodsService.selectAppGoodsByGoodsId(goodsOrder.getGoodsId());
            List<AppGoods> goodsList = new ArrayList<>();
            /*if (goods.getIsSku() == 1) {
                AppGoodsSkuData skuData = new AppGoodsSkuData();
                skuData = skuDataService.selectAppGoodsSkuDataByDataId(goodsOrder.getSkuDataId());
                if(null!=skuData) {
                    goods.setGoodsName(skuData.getDataValues());
                    goods.setGoodsCover(skuData.getDataImage());
                    goods.setPrice(skuData.getDataPrice());
                }
            }*/
            if (goodsOrder.getPayMoney() != null && goodsOrder.getPayMoney().compareTo(BigDecimal.ZERO) > 0) {
                goods.setPrice(goodsOrder.getPayMoney());
            } else if (goodsOrder.getMoneyPayable() != null) {
                goods.setPrice(goodsOrder.getMoneyPayable());
            }
            goodsOrder.setAddressInfo(userAddressService.selectAppUserAddressByAddressId(goodsOrder.getAddressId()));
            AppGoodsOrderDetail detailWhere = new AppGoodsOrderDetail();
            detailWhere.setOrderId(orderId);
            List<AppGoodsOrderDetail> tmpdetails = orderDetailService.selectAppGoodsOrderDetailList(detailWhere);
            goodsOrder.setOrderDetailList(tmpdetails);
            int comboxindex = 0;
            int mycomboxindex = 0;
            List<AppGoodsSkuOption> skuOptions;
            if (goods.getIsSku() == 1 && tmpdetails != null && !tmpdetails.isEmpty()) {
                AppGoodsOrderDetail detail = tmpdetails.get(0);
                mergeHotelOrderDetail(goodsOrder, detail);
                applyHotelGoodsPresentation(goods, detail);
                mycomboxindex = resolveSelComboIndex(goods, detail);
            }
            goodsOrder.setSelComboIndex(mycomboxindex);
            goodsList.add(goods);
            goodsOrder.setGoodsList(goodsList);
            AppGoodsOrderAfter afterWhere = new AppGoodsOrderAfter();
            afterWhere.setOrderId(orderId);
            goodsOrder.setOrderAfterList(orderAfterService.selectAppGoodsOrderAfterList(afterWhere));
        }
        return success(goodsOrder);
    }

    /**
     * 订单支付
     */
    @ApiOperation("订单支付")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/pay_goods_order")
    public AjaxResult pay_goods_order(@RequestBody AppGoodsOrder appGoodsOrder)
    {
        Long orderId = appGoodsOrder.getOrderId();
        AppGoodsOrder goodsOrder = goodsOrderService.selectAppGoodsOrderByOrderId(orderId);
        if (goodsOrder == null || !getUserId().equals(goodsOrder.getUserId())) {
            return error("非法订单");
        }
        goodsOrder.setOrderDetailList(orderDetailService.selectAppGoodsOrderDetailByOrderId(orderId));
        List<AppGoods> goodsList = new ArrayList<>();
        for (int i = 0; i < goodsOrder.getOrderDetailList().size(); i++) {
            goodsList.add(goodsService.selectAppGoodsByGoodsId(goodsOrder.getOrderDetailList().get(i).getGoodsId()));
        }
        goodsOrder.setGoodsList(goodsList);
        // 支付
        AjaxResult rs = goodsOrderService.wxpayPrepay(goodsOrder);

        return rs;
    }

    /**
     * 客户端支付成功后主动同步支付结果（解决回调延迟导致一直待付款）
     */
    @ApiOperation("同步商品订单支付结果")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/sync_goods_order_pay")
    public AjaxResult sync_goods_order_pay(@RequestParam("orderId") Long orderId)
    {
        return goodsOrderService.syncPayResult(orderId, getUserId());
    }

    /**
     * 客户端主动同步退款结果（解决回调延迟导致一直退款中）
     */
    @ApiOperation("同步商品订单退款结果")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/sync_goods_order_refund")
    public AjaxResult sync_goods_order_refund(@RequestParam("orderId") Long orderId)
    {
        return goodsOrderService.syncRefundResult(orderId, getUserId());
    }

    /**
     * 微信支付通知
     * @return
     */
    @Anonymous
    @RequestMapping("/wxpay_notify")
    public ResponseEntity<String> wxpay_notify(HttpServletRequest request)
    {
        return paymentNotificationResponse(() -> goodsOrderService.wxpayNotify(request));
    }

    /**
     * 微信退款通知
     * @return
     */
    @Anonymous
    @RequestMapping("/wxpay_refund_notify")
    public ResponseEntity<String> wxpay_refund_notify(HttpServletRequest request)
    {
        return paymentNotificationResponse(() -> goodsOrderService.wxpayRefundNotify(request));
    }

    private ResponseEntity<String> paymentNotificationResponse(Supplier<String> handler)
    {
        String result;
        int status = 500;
        try {
            result = handler.get();
            if ("SUCCESS".equals(JSON.parseObject(result).getString("code"))) {
                status = 200;
            }
        } catch (Exception error) {
            logger.error("支付通知处理失败: {}", error.getClass().getSimpleName());
            result = "{\"code\":\"FAIL\",\"message\":\"通知处理失败\"}";
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(result);
    }

    /**
     * 签到获取金币规则    */
    @ApiOperation("签到获取金币规则")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/get_golden_rule")
    public AjaxResult get_golden_rule()
    {
        return success(getGoldenRule());
    }

    /** 连续签到奖励：第1~7天对应 1,2,3,5,6,8,10；第7天及以后按第7天封顶 */
    private List<Long> getGoldenRule() {
        return Arrays.asList(1L, 2L, 3L, 5L, 6L, 8L, 10L);
    }

    /** 金币签到业务日，统一 yyyyMMdd（与入库 trade_data 一致） */
    private String goldSignDay(Date date) {
        return DateUtils.parseDateToStr("yyyyMMdd", date);
    }

    private boolean hasSignedOnDay(Long userId, String dayYmd) {
        AppUserGoldLog query = new AppUserGoldLog();
        query.setUserId(userId);
        query.setTradeType(1);
        query.setBusinessType("签到");
        query.setTradeData(dayYmd);
        // 不强制 status，兼容历史未写 status 的签到流水
        List<AppUserGoldLog> list = goldLogService.selectAppUserGoldLogList(query);
        return list != null && !list.isEmpty();
    }

    /**
     * 统计「不含今天」的连续签到天数（从昨天往前数）
     */
    private int countConsecutiveSignDaysBeforeToday(Long userId) {
        int streak = 0;
        int maxDays = getGoldenRule().size();
        for (int i = 1; i <= maxDays; i++) {
            String day = goldSignDay(DateUtils.addDays(new Date(), -i));
            if (hasSignedOnDay(userId, day)) {
                streak++;
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 签到获取金币
     */
    @ApiOperation("签到获取金币")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "签到获取金币", businessType = BusinessType.INSERT)
    @PostMapping("/sign_got_golden")
    public AjaxResult sign_got_golden()
    {
        Long userId = getUserId();
        String today = goldSignDay(new Date());
        if (hasSignedOnDay(userId, today)) {
            return error("您今天已经签过到了");
        }

        List<Long> rule = getGoldenRule();
        int streakBefore = countConsecutiveSignDaysBeforeToday(userId);
        // 今天签到后的连续天数（含今天），最多按规则天数封顶
        int signDays = Math.min(streakBefore + 1, rule.size());
        Long gold = rule.get(signDays - 1);

        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(userId);
        if (userInfo.getGolden() == null) {
            userInfo.setGolden(0L);
        }

        AppUserGoldLog goldLogWhere = new AppUserGoldLog();
        goldLogWhere.setUserId(userId);
        goldLogWhere.setTradeType(1);
        goldLogWhere.setBusinessType("签到");
        goldLogWhere.setGold(gold);
        goldLogWhere.setTradeData(today);
        goldLogWhere.setBalance(userInfo.getGolden() + gold);
        goldLogWhere.setTradeTitle("签到获取金币");
        goldLogWhere.setTradeDetail("签到获取金币，数量：" + gold);
        goldLogWhere.setStatus("1");
        int rs = goldLogService.insertAppUserGoldLog(goldLogWhere);
        if (rs <= 0) {
            return error("签到失败，请稍后重试");
        }
        userInfo.setGolden(goldLogWhere.getBalance());
        userInfoService.updateAppUserInfo(userInfo);

        Map<String, Object> data = new HashMap<>();
        data.put("gold", gold);
        data.put("signDays", signDays);
        data.put("balance", goldLogWhere.getBalance());
        return success(data);
    }

    /**
     * 查询金币记录
     * @return
     */
    @ApiOperation("查询金币记录")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = "/get_gold_log")
    public AjaxResult get_gold_log(
            @RequestParam(value = "tradeType", required = false) Integer tradeType,
            @RequestParam(value = "businessType", required = false) String businessType)
    {
        startPage();
        AppUserGoldLog goldLogWhere = new AppUserGoldLog();
        goldLogWhere.setUserId(getUserId());
        goldLogWhere.setStatus("1");
        // 可选筛选：1增加 2减少；不传则返回全部（含支付赠送/退款扣回/签到）
        if (tradeType != null) {
            goldLogWhere.setTradeType(tradeType);
        }
        if (StringUtils.isNotEmpty(businessType)) {
            goldLogWhere.setBusinessType(businessType);
        }
        List<AppUserGoldLog> goldLogList = goldLogService.selectAppUserGoldLogList(goldLogWhere);
        return success(goldLogList);
    }

    /**
     * 查询用户地址列表
     */
    @ApiOperation("查询用户地址列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/address/list")
    public TableDataInfo address_list(AppUserAddress appUserAddress)
    {
        startPage();
        appUserAddress.setUserId(getUserId());
        List<AppUserAddress> list = userAddressService.selectAppUserAddressList(appUserAddress);
        return getDataTable(list);
    }

    /**
     * 获取用户地址详细信息
     */
    @ApiOperation("获取用户地址详细信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = "/address/{addressId}")
    public AjaxResult address_getInfo(@PathVariable("addressId") Long addressId)
    {
        AppUserAddress userAddress = userAddressService.selectAppUserAddressByAddressId(addressId);
        if (userAddress == null || !getUserId().equals(userAddress.getUserId())) {
            return error("无效地址");
        }
        return success(userAddress);
    }

    /**
     * 新增用户地址
     */
    @ApiOperation("新增用户地址")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "用户地址", businessType = BusinessType.INSERT)
    @PostMapping(value = "/address/add")
    public AjaxResult address_add(@RequestBody AppUserAddress appUserAddress)
    {
        appUserAddress.setUserId(getUserId());
        return toAjax(userAddressService.insertAppUserAddress(appUserAddress));
    }

    /**
     * 修改用户地址
     */
    @ApiOperation("修改用户地址")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "用户地址", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/address/edit")
    public AjaxResult address_edit(@RequestBody AppUserAddress appUserAddress)
    {
        AppUserAddress userAddress = userAddressService.selectAppUserAddressByAddressId(appUserAddress.getAddressId());
        if (userAddress == null || !getUserId().equals(userAddress.getUserId())) {
            return error("无效操作");
        }
        appUserAddress.setUserId(getUserId());
        return toAjax(userAddressService.updateAppUserAddress(appUserAddress));
    }

    /**
     * 删除用户地址
     */
    @ApiOperation("删除用户地址")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "用户地址", businessType = BusinessType.DELETE)
    @PostMapping("/address/delete/{addressId}")
    public AjaxResult address_remove(@PathVariable Long addressId)
    {
        AppUserAddress userAddress = userAddressService.selectAppUserAddressByAddressId(addressId);
        if (userAddress == null || !getUserId().equals(userAddress.getUserId())) {
            return error("无效操作");
        }
        return toAjax(userAddressService.deleteAppUserAddressByAddressId(addressId));
    }

    /************************ 顾问部分 ************************/
    /**
     * 申请成为顾问
     */
    @ApiOperation("申请成为顾问")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "申请成为顾问", businessType = BusinessType.INSERT)
    @PostMapping("/apply_consultant")
    public AjaxResult applyConsultant(@Validated @RequestBody AppConsultant consultant)
    {
        try
        {
            return toAjax(consultantService.applyConsultantAsUser(getUserId(), consultant));
        }
        catch (ServiceException e)
        {
            AppConsultant consultantLast = consultantService.selectAppConsultantByUserId(getUserId());
            return AjaxResult.error(e.getMessage(), consultantLast);
        }
    }

    /**
     * 查询上级信息
     */
    @ApiOperation("查询上级信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/parent")
    public AjaxResult consultant_parentInfo()
    {
        AppUserInviter inviterWhere = new AppUserInviter();
        inviterWhere.setNewUserId(getUserId());
        List<AppUserInviter> inviters = inviterService.selectAppUserInviterList(inviterWhere);
        if (inviters.size() == 0) {
            return success(userService.selectUserById(1L));
        }
        SysUser parent = userService.selectUserById(inviters.get(0).getUserId());
        return success(parent);
    }

    /**
     * 查询下级列表（我邀请的用户）
     */
    @ApiOperation("查询我邀请的用户列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/children")
    public TableDataInfo consultant_children_list()
    {
        startPage();
        return getDataTable(consultantMnpService.selectInviteUserList(getUserId()));
    }

    /**
     * 顾问中心统计
     */
    @ApiOperation("顾问中心统计")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/stat")
    public AjaxResult consultant_stat()
    {
        SysUser sysUser = userService.selectUserById(getUserId());
        String mobile = sysUser != null ? sysUser.getPhonenumber() : null;
        ConsultantStatVo stat = consultantMnpService.getConsultantStat(getUserId(), mobile);
        return success(stat);
    }

    /**
     * 顾问收支明细
     */
    @ApiOperation("顾问收支明细")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/income_list")
    public AjaxResult consultant_income_list()
    {
        SysUser sysUser = userService.selectUserById(getUserId());
        String mobile = sysUser != null ? sysUser.getPhonenumber() : null;
        AppConsultant consultant = consultantMnpService.requireApprovedConsultant(getUserId(), mobile);
        startPage();
        AppCustomerIncome incomeWhere = new AppCustomerIncome();
        incomeWhere.setConsultantId(consultant.getConsultantId());
        List<AppCustomerIncome> list = customerIncomeService.selectAppCustomerIncomeList(incomeWhere);
        TableDataInfo table = getDataTable(list);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("rows", table.getRows());
        ajax.put("total", table.getTotal());
        ajax.put("summary", consultantMnpService.getIncomeSummary(consultant.getConsultantId()));
        return ajax;
    }

    /**
     * 顾问提现记录
     */
    @ApiOperation("顾问提现记录")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/cash_list")
    public TableDataInfo consultant_cash_list()
    {
        startPage();
        AppUserCash cashWhere = new AppUserCash();
        cashWhere.setUserId(getUserId());
        List<AppUserCash> list = userCashService.selectAppUserCashList(cashWhere);
        return getDataTable(list);
    }

    /**
     * 顾问邀请二维码
     */
    @ApiOperation("顾问邀请二维码")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/invite_qrcode")
    public AjaxResult consultant_invite_qrcode()
    {
        SysUser sysUser = userService.selectUserById(getUserId());
        String mobile = sysUser != null ? sysUser.getPhonenumber() : null;
        consultantMnpService.requireApprovedConsultant(getUserId(), mobile);
        Map<String, Object> data = new HashMap<>(2);
        data.put("qrcodeUrl", consultantMnpService.getOrCreateInviteQrcodeUrl(getUserId()));
        data.put("inviteUserId", getUserId());
        return success(data);
    }

    /**
     * 绑定邀请人
     */
    @ApiOperation("绑定邀请人")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/bind_inviter")
    public AjaxResult bind_inviter(@RequestParam("parentUserId") Long parentUserId)
    {
        return toAjax(consultantMnpService.bindInviterIfAbsent(getUserId(), parentUserId));
    }

    /**
     * 查询客户列表
     */
    @ApiOperation("查询客户列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/consultant/customer_list")
    public TableDataInfo consultant_customer_list()
    {
        SysUser sysUser = userService.selectUserById(getUserId());
        String mobile = sysUser != null ? sysUser.getPhonenumber() : null;
        AppConsultant consultant = consultantService.getOrClaimConsultantByUser(getUserId(), mobile);
        if (consultant == null) {
            return getDataTable(Collections.emptyList());
        }
        startPage();
        AppCustomer customerWhere = new AppCustomer();
        customerWhere.setConsultantId(consultant.getConsultantId());
        List<AppCustomer> list = customerService.selectAppCustomerList(customerWhere);
        return getDataTable(list);
    }

    /**
     * 查询活动预约列表
     */
    @ApiOperation("查询活动预约列表")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping("/acticity_order/list")
    public TableDataInfo acticity_order_list(AppActivityOrder appActivityOrder)
    {
        startPage();
        appActivityOrder.setUserId(getUserId());
        List<AppActivityOrder> list = activityOrderService.selectAppActivityOrderList(appActivityOrder);
        for (int i = 0; i < list.size(); i++) {
            AppActivity info = activityService.selectAppActivityByActivityId(list.get(i).getActivityId());
            list.get(i).setActivityInfo(info);
        }
        return getDataTable(list);
    }

    /**
     * 获取活动预约详细信息
     */
    @ApiOperation("获取活动预约详细信息")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @GetMapping(value = "acticity_order_info/{orderId}")
    public AjaxResult acticity_order_getInfo(@PathVariable("orderId") Long orderId)
    {
        AppActivityOrder order = activityOrderService.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            return error("预约记录不存在");
        }
        if (order.getUserId() == null || order.getUserId().longValue() != getUserId().longValue()) {
            return error("非法操作");
        }
        AppActivity info = activityService.selectAppActivityByActivityId(order.getActivityId());
        order.setActivityInfo(info);
        return success(order);
    }

    /**
     * 活动预约报名
     */
    @ApiOperation("活动预约报名")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "活动预约", businessType = BusinessType.INSERT)
    @PostMapping(value = "acticity_order/add")
    public AjaxResult acticity_order_add(@RequestBody AppActivityOrder appActivityOrder)
    {
        appActivityOrder.setUserId(getUserId());
        try {
            AppActivityOrder order = activityOrderService.signupActivity(appActivityOrder);
            return success(order);
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 创建付费活动待支付订单
     */
    @ApiOperation("创建付费活动待支付订单")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "活动预约", businessType = BusinessType.INSERT)
    @PostMapping(value = "acticity_order/create_pending")
    public AjaxResult acticity_order_create_pending(@RequestBody AppActivityOrder appActivityOrder)
    {
        appActivityOrder.setUserId(getUserId());
        try {
            AppActivityOrder order = activityOrderService.createPendingActivityOrder(appActivityOrder);
            return success(order);
        } catch (ServiceException e) {
            return error(e.getMessage());
        } catch (Exception e) {
            logger.error("创建活动待支付订单失败 userId={}, activityId={}", getUserId(),
                    appActivityOrder != null ? appActivityOrder.getActivityId() : null, e);
            return error("报名失败，请稍后重试");
        }
    }

    /**
     * 活动订单支付
     */
    @ApiOperation("活动订单支付")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/pay_activity_order")
    public AjaxResult pay_activity_order(@RequestBody AppActivityOrder appActivityOrder)
    {
        Long orderId = appActivityOrder.getOrderId();
        AppActivityOrder order = activityOrderService.selectAppActivityOrderByOrderId(orderId);
        if (order == null) {
            return error("订单不存在");
        }
        if (order.getUserId() == null || order.getUserId().longValue() != getUserId().longValue()) {
            return error("非法订单");
        }
        if (!"0".equals(order.getPayStatus())) {
            return error("订单非待支付状态");
        }
        return activityOrderService.wxpayPrepay(order);
    }

    /**
     * 客户端支付成功后主动同步活动订单支付结果（解决回调延迟导致一直待支付）
     */
    @ApiOperation("同步活动订单支付结果")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/sync_activity_order_pay")
    public AjaxResult sync_activity_order_pay(@RequestParam("orderId") Long orderId)
    {
        return activityOrderService.syncPayResult(orderId, getUserId());
    }

    /**
     * 客户端主动同步活动退款结果（解决回调延迟导致一直退款中）
     */
    @ApiOperation("同步活动订单退款结果")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/sync_activity_order_refund")
    public AjaxResult sync_activity_order_refund(@RequestParam("orderId") Long orderId)
    {
        return activityOrderService.syncRefundResult(orderId, getUserId());
    }

    /**
     * 修改活动预约
     */
    @ApiOperation("修改活动预约")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "活动预约", businessType = BusinessType.UPDATE)
    @PostMapping(value = "acticity_order/edit")
    public AjaxResult edit(@RequestBody AppActivityOrder appActivityOrder)
    {
        try {
            return toAjax(activityOrderService.editUserActivityOrder(appActivityOrder, getUserId()));
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 取消活动预约
     */
    @ApiOperation("取消活动预约")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "活动预约", businessType = BusinessType.DELETE)
    @PostMapping(value = "acticity_order/cancel")
    public AjaxResult remove(@RequestParam("orderId") Long orderId)
    {
        try {
            return toAjax(activityOrderService.cancelActivityOrder(orderId, getUserId()));
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 取消未支付会员卡开通单
     */
    @ApiOperation("取消未支付会员卡")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/user_card/cancel")
    public AjaxResult cancelUserCard(@RequestParam("recordId") Long recordId)
    {
        try {
            return toAjax(userCardService.cancelUnpaidUserCard(recordId, getUserId()));
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 会员卡退款（撤销权益并微信退款）
     */
    @ApiOperation("会员卡退款")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/user_card/refund")
    public AjaxResult refundUserCard(@RequestParam("recordId") Long recordId)
    {
        try {
            return userCardService.refundUserCard(recordId, getUserId());
        } catch (ServiceException e) {
            return error(e.getMessage());
        }
    }

    /**
     * 用户会员卡预购买
     */
    @ApiOperation("用户会员卡预购买")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "用户会员卡", businessType = BusinessType.INSERT)
    @PostMapping("/user_card/prepay")
    public AjaxResult prepay(@RequestBody AppCard appCard)
    {
        // 查询开卡记录
        AppUserCard userCardLast = userCardService.selectAppUserCardByUserId(getUserId());
        if (userCardLast != null
                && userCardLast.getStatus() != null
                && userCardLast.getStatus().equals("1")
                && userCardLast.getEnableEndTime().getTime() > System.currentTimeMillis()) {
            return error("当前会员卡处于生效中！");
        }
        // 查询卡状态
        AppCard card = cardService.selectAppCardByCardId(appCard.getCardId());
        if (card == null || card.getStatus() == null || !card.getStatus().equals("1")) {// 1-启用，0-停用
            return error("卡无效，请稍后再试！");
        }
        AppUserCard userCard = new AppUserCard();
        userCard.setCardId(appCard.getCardId());
        userCard.setUserId(getUserId());
        userCard.setStatus("0");  // 0-待激活，1-已激活，2-已过期，3-已失效
        int rs = userCardService.insertAppUserCard(userCard);
        if (rs > 0) {
            return success(userCard);
        }
        return error("创建失败！");
    }

    /**
     * 用户会员卡支付
     */
    @ApiOperation("用户会员卡支付")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @Log(title = "用户会员卡", businessType = BusinessType.UPDATE)
    @PostMapping("/user_card/pay")
    public AjaxResult pay(@RequestBody AppUserCard appUserCard)
    {
        // 记录查询
        AppUserCard userCard = userCardService.selectAppUserCardByRecordId(appUserCard.getRecordId());
        if (userCard == null || userCard.getStatus() == null || userCard.getStatus().equals("2") || userCard.getStatus().equals("3")) {// 0-待激活，1-已激活，2-已过期，3-已失效
            return error("记录无效请重新提交！");
        }
        // 卡片有效性查询
        AppCard card = cardService.selectAppCardByCardId(userCard.getCardId());
        if (card == null || card.getStatus() == null || !card.getStatus().equals("1")) {// 1-启用，0-停用
            return error("卡无效，请稍后再试！");
        }
        userCard.setCardInfo(card);
        // 已支付判断
        if (userCard.getStatus().equals("1")) {
            return error("记录已完成支付，请核对！");
        }
        // 处理支付逻辑
        AjaxResult rs = userCardService.wxpayPrepay(userCard);
        return rs;
    }

    /**
     * 获取用户各项统计
     */
    @ApiOperation("获取用户各项统计")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @RequestMapping("/user_statistic/all")
    public AjaxResult user_statistic_all()
    {
        Map<String, Object> rs = new HashMap<>();
        // 金币统计
        AppUserInfo userInfo = userInfoService.selectAppUserInfoByUserId(getUserId());
        rs.put("golden", userInfo.getGolden());
        // 可用优惠券统计
        AppGoodsCouponGot couponGotWhere  = new AppGoodsCouponGot();
        couponGotWhere.setUserId(getUserId());
        couponGotWhere.setIsUsed(0);            // 是否使用 0-否， 1-是
        couponGotWhere.setStatus("1");          // 是否有效 0-否， 1-是
        List<AppGoodsCouponGot> couponGotList = couponGotService.selectAppGoodsCouponGotList(couponGotWhere);
        rs.put("couponGotCount", couponGotList.size());
        // 收藏统计
        AppGoodsCollect collectWhere  = new AppGoodsCollect();
        collectWhere.setUserId(getUserId());
        collectWhere.setStatus("1");          // 是否有效 0-否， 1-是
        List<AppGoodsCollect> collectList = collectService.selectAppGoodsCollectList(collectWhere);
        rs.put("collectCount", collectList.size());
        // 预约统计
        AppActivityOrder activityOrderWhere  = new AppActivityOrder();
        activityOrderWhere.setUserId(getUserId());
        activityOrderWhere.setStatus("1");          // 是否有效 0-否， 1-是
        List<AppActivityOrder> activityOrderList = activityOrderService.selectAppActivityOrderList(activityOrderWhere);
        rs.put("activityOrderCount", activityOrderList.size());
        // 订单统计
        AppGoodsOrder goodsOrderWhere  = new AppGoodsOrder();
        goodsOrderWhere.setUserId(getUserId());
        goodsOrderWhere.setPayStatus("0");         // 支付状态 0待支付，1已支付，2已取消，3退款中，4退款成功
        goodsOrderWhere.setStatus("1");          // 是否有效 0-否， 1-是
        List<AppGoodsOrder> goodsOrderList = goodsOrderService.selectAppGoodsOrderList(goodsOrderWhere);
        rs.put("goodsOrderCount", goodsOrderList.size());

        return AjaxResult.success(rs);
    }

    @ApiOperation("取消订单")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/cacelOrder")
    public AjaxResult cacelOrder(@RequestParam("orderId") Long orderId){
        try {
            AppGoodsOrder goodsOrder = goodsOrderService.selectAppGoodsOrderByOrderId(orderId);
            if (goodsOrder == null) {
                return error("订单不存在");
            }
            if (goodsOrder.getUserId() == null || goodsOrder.getUserId().longValue() != getUserId().longValue()) {
                return error("非法订单");
            }
            // 仅允许取消待支付订单（payStatus=0）；status=0 为下单初始态
            String payStatus = StringUtils.defaultIfBlank(goodsOrder.getPayStatus(), "0");
            if (!"0".equals(payStatus)) {
                return error("当前订单不可取消");
            }
            // 关闭对应支付日志（payNo = 商户订单号 orderNo）
            if (StringUtils.isNotEmpty(goodsOrder.getOrderNo())) {
                AppPayLog thepaylog = payLogService.selectAppPayLogByPayNo(goodsOrder.getOrderNo());
                if (thepaylog != null) {
                    AppPayLog uppaylog = new AppPayLog();
                    uppaylog.setLogId(thepaylog.getLogId());
                    uppaylog.setStatus("2");
                    uppaylog.setUpdateTime(DateUtils.getNowDate());
                    payLogService.updateAppPayLog(uppaylog);
                }
            }
            AppGoodsOrder uporder = new AppGoodsOrder();
            uporder.setOrderId(orderId);
            uporder.setStatus("2");
            uporder.setPayStatus("2");
            uporder.setUpdateTime(DateUtils.getNowDate());
            int rs = goodsOrderService.updateAppGoodsOrder(uporder);
            if (rs > 0) {
                // 教育课程下单时预占了名额，取消时释放
                goodsOrderService.releaseEducationStockIfNeeded(goodsOrder);
                goodsOrderService.releaseCouponIfNeeded(goodsOrder);
                return AjaxResult.success("取消成功");
            }
            return AjaxResult.error("取消失败");
        }catch (Exception ex){
            logger.error("取消订单失败 orderId={}", orderId, ex);
            return error("系统错误");
        }
    }

    @ApiOperation("订单退款")
    @PreAuthorize("@ss.hasPermi('system:mnp:user')")
    @PostMapping("/appRefundOrder")
    public AjaxResult appRefundOrder(@RequestBody AppGoodsOrderAfter refund){
        try {
            if (refund == null || refund.getOrderId() == null) {
                return error("订单无效");
            }
            AppGoodsOrder order = goodsOrderService.selectAppGoodsOrderByOrderId(refund.getOrderId());
            if (order == null) {
                return error("订单不存在");
            }
            if (order.getUserId() == null || order.getUserId().longValue() != getUserId().longValue()) {
                return error("非法订单");
            }
            if (!"1".equals(StringUtils.defaultIfBlank(order.getStatus(), ""))) {
                return error("当前订单不可退款");
            }
            if (refund.getAppRefundMoney() == null) {
                return error("请填写退款金额");
            }
            // 可退上限：微信实付（必要时回写本地）
            BigDecimal orderAmount = goodsOrderService.healPaidAmountIfNeeded(order);
            if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
                orderAmount = goodsOrderService.resolveActualPayYuan(order);
            }
            if (orderAmount != null
                    && orderAmount.compareTo(refund.getAppRefundMoney()) < 0) {
                return error("退款金额不能大于实付金额（实付￥" + orderAmount.toPlainString() + "）");
            }
            // 防重复申请：已有进行中售后则拒绝
            AppGoodsOrderAfter query = new AppGoodsOrderAfter();
            query.setOrderId(order.getOrderId());
            List<AppGoodsOrderAfter> existList = appGoodsOrderAfterService.selectAppGoodsOrderAfterList(query);
            if (existList != null) {
                for (AppGoodsOrderAfter exist : existList) {
                    String st = StringUtils.defaultIfBlank(exist.getStatus(), "");
                    if ("0".equals(st) || "1".equals(st)) {
                        return error("该订单已有进行中的售后申请");
                    }
                }
            }
            refund.setUserId(getUserId());
            refund.setOutOrderNo(order.getOrderNo());
            refund.setOrderMoney(orderAmount);
            if (refund.getRefundMoney() == null) {
                refund.setRefundMoney(refund.getAppRefundMoney());
            }
            appGoodsOrderAfterService.appGoodsOrderAfter(refund);
            return success();
        }catch (Exception ex){
            logger.error("申请退款失败 orderId={}", refund != null ? refund.getOrderId() : null, ex);
            return error("系统错误");
        }
    }

    /** 旅居订单：明细表字段回填到订单主对象（晚数、入住日期等） */
    private void mergeHotelOrderDetail(AppGoodsOrder order, AppGoodsOrderDetail detail) {
        if (order == null || detail == null) {
            return;
        }
        order.setGoodsCount(detail.getGoodsCount());
        order.setSkuId(detail.getSkuId());
        order.setSkuSeqNo(detail.getSkuSeqNo());
        order.setSelfGoodsCount(detail.getSelfGoodsCount());
        order.setSelfSkuId(detail.getSelfSkuId());
        if (detail.getInterCount() != null) {
            order.setInterCount(detail.getInterCount());
        }
        if (order.getCheckInDate() == null && detail.getOrderStartDate() != null) {
            order.setCheckInDate(detail.getOrderStartDate());
        }
        if (order.getCheckOutDate() == null && detail.getOrderEndDate() != null) {
            order.setCheckOutDate(detail.getOrderEndDate());
        }
    }

    /** 自选晚数：无有效 skuSeqNo */
    private boolean isCustomNightHotelOrder(AppGoodsOrderDetail detail) {
        return detail != null && (detail.getSkuSeqNo() == null || detail.getSkuSeqNo() <= 0);
    }

    /** 供餐套餐在 optionList 中的下标 */
    private int resolveSelComboIndex(AppGoods goods, AppGoodsOrderDetail detail) {
        if (goods == null || detail == null || detail.getSelfSkuId() == null
                || goods.getOptionList() == null || goods.getOptionList().isEmpty()) {
            return 0;
        }
        int comboIndex = -1;
        int selectedIndex = 0;
        for (AppGoodsSku option : goods.getOptionList()) {
            if (option.getOptions() == null || option.getOptions().isEmpty()) {
                comboIndex = comboIndex + 1;
            }
            if (detail.getSelfSkuId().longValue() == option.getSkuId()) {
                selectedIndex = comboIndex;
            }
        }
        return selectedIndex;
    }

    /**
     * 旅居商品展示：自选晚数用标准房型名称；固定套餐按下单时的组合序号展示。
     */
    private void applyHotelGoodsPresentation(AppGoods goods, AppGoodsOrderDetail detail) {
        if (goods == null || detail == null || goods.getOptionList() == null) {
            return;
        }
        boolean customNight = isCustomNightHotelOrder(detail);
        Integer orderSeqNo = detail.getSkuSeqNo();
        String specName = null;
        for (AppGoodsSku option : goods.getOptionList()) {
            if (detail.getSkuId() == null || !detail.getSkuId().equals(option.getSkuId())) {
                continue;
            }
            if (customNight && StringUtils.isNotBlank(option.getSkuName())) {
                specName = option.getSkuName();
            }
            List<AppGoodsSkuOption> skuOptions = option.getOptions();
            if (skuOptions == null || skuOptions.isEmpty()) {
                continue;
            }
            for (AppGoodsSkuOption skuoption : skuOptions) {
                int seqNo = skuoption.getSkuSeqNo() != null ? skuoption.getSkuSeqNo().intValue() : 0;
                if ("305".equals(skuoption.getOptionType())) {
                    if (customNight && seqNo == 0 && StringUtils.isNotBlank(skuoption.getOptionValue())) {
                        goods.setGoodsCover(skuoption.getOptionValue());
                    } else if (!customNight && seqNo != 0 && orderSeqNo != null
                            && seqNo == orderSeqNo.intValue()) {
                        goods.setGoodsCover(skuoption.getOptionValue());
                    }
                }
                if ("304".equals(skuoption.getOptionType()) && !customNight && seqNo != 0
                        && orderSeqNo != null && seqNo == orderSeqNo.intValue()) {
                    specName = skuoption.getOptionValue();
                }
                if ("302".equals(skuoption.getOptionType()) && !customNight && seqNo != 0
                        && orderSeqNo != null && seqNo == orderSeqNo.intValue()) {
                    goods.setPrice(new BigDecimal(skuoption.getOptionValue()));
                }
            }
        }
        if (StringUtils.isNotBlank(specName)) {
            goods.setSpecifications(specName);
        }
    }
}
