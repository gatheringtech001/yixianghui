package com.ruoyi.system.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.IAppActivityOrderService;
import com.ruoyi.system.service.IAppGoodsOrderService;
import com.ruoyi.system.service.IAppUserCardService;
import lombok.extern.slf4j.Slf4j;

/**
 * Close unpaid orders after 30 minutes (align with prepay/cashier).
 * Quartz invoke target: appOrderTimeoutTask.closeExpiredOrders
 */
@Component("appOrderTimeoutTask")
@Slf4j
public class AppOrderTimeoutTask
{
    private static final int EXPIRE_MINUTES = 30;

    @Autowired
    private IAppGoodsOrderService goodsOrderService;

    @Autowired
    private IAppActivityOrderService activityOrderService;

    @Autowired
    private IAppUserCardService userCardService;

    public void closeExpiredOrders()
    {
        int goods = goodsOrderService.closeExpiredUnpaidOrders(EXPIRE_MINUTES);
        int activity = activityOrderService.closeExpiredUnpaidOrders(EXPIRE_MINUTES);
        int card = userCardService.closeExpiredUnpaidCards(EXPIRE_MINUTES);
        if (goods > 0 || activity > 0 || card > 0) {
            log.info("close unpaid timeout goods={}, activity={}, card={}", goods, activity, card);
        }
    }
}
