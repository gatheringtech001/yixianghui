package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppGoodsOrder;
import com.ruoyi.system.domain.AppGoodsOrderAfter;
import com.wechat.pay.java.service.refund.model.RefundNotification;

import javax.servlet.http.HttpServletRequest;

/**
 * 商品订单Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppGoodsOrderService 
{
    /**
     * 查询商品订单
     * 
     * @param orderId 商品订单主键
     * @return 商品订单
     */
    public AppGoodsOrder selectAppGoodsOrderByOrderId(Long orderId);

    /**
     * 查询商品订单列表
     * 
     * @param appGoodsOrder 商品订单
     * @return 商品订单集合
     */
    public List<AppGoodsOrder> selectAppGoodsOrderList(AppGoodsOrder appGoodsOrder);

    /**
     * 新增商品订单
     * 
     * @param appGoodsOrder 商品订单
     * @return 结果
     */
    public AppGoodsOrder insertAppGoodsOrder(AppGoodsOrder appGoodsOrder);

    /**
     * 修改商品订单
     * 
     * @param appGoodsOrder 商品订单
     * @return 结果
     */
    public int updateAppGoodsOrder(AppGoodsOrder appGoodsOrder);

    /**
     * 按旅居履约状态机更新订单；支付、退款状态不能通过此入口修改。
     */
    int updateTravelStatus(Long orderId, String travelStatus);

    /**
     * 批量删除商品订单
     * 
     * @param orderIds 需要删除的商品订单主键集合
     * @return 结果
     */
    public int deleteAppGoodsOrderByOrderIds(Long[] orderIds);

    /**
     * 删除商品订单信息
     * 
     * @param orderId 商品订单主键
     * @return 结果
     */
    public int deleteAppGoodsOrderByOrderId(Long orderId);

    /**
     * 订单进行微信支付
     * @param goodsOrder
     * @return
     */
    AjaxResult wxpayPrepay(AppGoodsOrder goodsOrder);
    /**
     * 订单进行微信退款
     * @param appGoodsOrderAfter
     * @return
     * @throws Exception
     *  */
    AjaxResult wxpayRefund(AppGoodsOrderAfter appGoodsOrderAfter);

    /**
     * 微信支付通知
     * @param request
     * @return
     */
    String wxpayNotify(HttpServletRequest request);

    /**
     * 客户端支付成功后主动向微信查单并同步订单状态（解决回调延迟/丢失导致一直待付款）
     * @param orderId 商品订单ID
     * @param userId 当前用户
     */
    AjaxResult syncPayResult(Long orderId, Long userId);

    /**
     * 客户端主动同步退款结果（解决退款回调延迟/丢失导致一直退款中）
     */
    AjaxResult syncRefundResult(Long orderId, Long userId);

    /**
     * 微信退款通知
     * @return
     */
    String wxpayRefundNotify(HttpServletRequest request);

    /**
     * 获取缓存商品订单
     * @param orderId
     * @return
     */
    AppGoodsOrder getCacheGoodsOrder(Long orderId);
    /**
     * 获取商品订单统计数据
     * @return
     */
    Map selAppGoodsOrderStatData();

    /**
     * 取消未支付订单时，如有教育课程预占名额则释放
     */
    void releaseEducationStockIfNeeded(AppGoodsOrder order);

    void releaseCouponIfNeeded(AppGoodsOrder order);

    /**
     * 关闭超时未支付商品订单（含教育课释库存）
     * @param expireMinutes 超时分钟数
     * @return 关闭数量
     */
    int closeExpiredUnpaidOrders(int expireMinutes);

    /**
     * 解析订单真实实付金额（元），优先微信支付日志
     */
    java.math.BigDecimal resolveActualPayYuan(AppGoodsOrder order);

    /**
     * 已支付订单按微信实付回写本地金额（修复展示/可退金额不一致）
     */
    java.math.BigDecimal healPaidAmountIfNeeded(AppGoodsOrder order);
}
