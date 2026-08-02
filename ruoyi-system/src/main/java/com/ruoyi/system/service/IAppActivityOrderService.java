package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.AppActivityOrder;

/**
 * 活动预约Service接口
 * 
 * @author lankong
 * @date 2025-04-06
 */
public interface IAppActivityOrderService 
{
    /**
     * 查询活动预约
     * 
     * @param orderId 活动预约主键
     * @return 活动预约
     */
    public AppActivityOrder selectAppActivityOrderByOrderId(Long orderId);

    /**
     * 查询活动预约列表
     * 
     * @param appActivityOrder 活动预约
     * @return 活动预约集合
     */
    public List<AppActivityOrder> selectAppActivityOrderList(AppActivityOrder appActivityOrder);

    /**
     * 新增活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    public int insertAppActivityOrder(AppActivityOrder appActivityOrder);

    /**
     * 修改活动预约
     * 
     * @param appActivityOrder 活动预约
     * @return 结果
     */
    public int updateAppActivityOrder(AppActivityOrder appActivityOrder);

    /**
     * 用户端修改活动预约（付费后不可改人数；仅允许改联系人信息等）
     */
    int editUserActivityOrder(AppActivityOrder input, Long userId);

    /**
     * 批量删除活动预约
     * 
     * @param orderIds 需要删除的活动预约主键集合
     * @return 结果
     */
    public int deleteAppActivityOrderByOrderIds(Long[] orderIds);

    /**
     * 删除活动预约信息
     * 
     * @param orderId 活动预约主键
     * @return 结果
     */
    public int deleteAppActivityOrderByOrderId(Long orderId);

    /**
     * 活动报名（免费）
     */
    public AppActivityOrder signupActivity(AppActivityOrder appActivityOrder);

    /**
     * 创建付费活动待支付订单
     */
    public AppActivityOrder createPendingActivityOrder(AppActivityOrder appActivityOrder);

    /**
     * 取消活动预约（恢复名额；付费已支付则发起微信退款）
     */
    public int cancelActivityOrder(Long orderId, Long userId);

    /**
     * 活动订单微信支付预下单
     */
    public AjaxResult wxpayPrepay(AppActivityOrder appActivityOrder);

    /**
     * 支付成功回调处理
     */
    public void handlePaySuccess(AppActivityOrder order, BigDecimal payMoneyFen);

    /**
     * 退款成功回调处理（释放名额已在取消时处理，这里落库退款完成态）
     */
    void handleRefundSuccess(Long orderId);

    /**
     * 客户端支付成功后主动向微信查单并同步活动订单状态
     */
    AjaxResult syncPayResult(Long orderId, Long userId);

    /**
     * 客户端主动同步活动退款结果（解决回调延迟导致一直退款中）
     */
    AjaxResult syncRefundResult(Long orderId, Long userId);

    /**
     * 关闭超时未支付活动订单
     * @param expireMinutes 超时分钟数
     * @return 关闭数量
     */
    int closeExpiredUnpaidOrders(int expireMinutes);
}
