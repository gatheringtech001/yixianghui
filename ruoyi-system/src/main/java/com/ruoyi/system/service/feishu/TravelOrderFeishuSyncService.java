package com.ruoyi.system.service.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.TravelOrderSyncRecord;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时对账部署后创建的旅居订单，失败会在下一轮自动重试。
 */
@Service
public class TravelOrderFeishuSyncService
{
    private static final Logger log = LoggerFactory.getLogger(TravelOrderFeishuSyncService.class);
    private static final String START_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final AppGoodsOrderMapper orderMapper;
    private final TravelOrderFeishuClient client;
    private final Map<String, String> syncedPayloads = new ConcurrentHashMap<>();

    @Value("${feishu.travel-order.enabled:false}")
    private boolean enabled;

    @Value("${feishu.travel-order.sync-start-at:}")
    private String syncStartAt;

    @Value("${feishu.travel-order.owner-open-id:}")
    private String ownerOpenId;

    private Date syncStartDate;

    public TravelOrderFeishuSyncService(AppGoodsOrderMapper orderMapper, TravelOrderFeishuClient client)
    {
        this.orderMapper = orderMapper;
        this.client = client;
    }

    @PostConstruct
    public void initialize()
    {
        if (!enabled) return;
        client.validateConfiguration();
        if (StringUtils.isEmpty(ownerOpenId))
        {
            throw new IllegalStateException("飞书旅居订单客服负责人未配置");
        }
        syncStartDate = parseStartDate(syncStartAt);
        log.info("飞书旅居订单增量同步已启用，起始时间={}", syncStartAt);
    }

    @Scheduled(initialDelayString = "${feishu.travel-order.initial-delay-ms:10000}",
            fixedDelayString = "${feishu.travel-order.fixed-delay-ms:60000}")
    public void syncRecentOrders()
    {
        if (!enabled) return;
        List<TravelOrderSyncRecord> orders = orderMapper.selectTravelOrdersCreatedSince(syncStartDate);
        for (TravelOrderSyncRecord order : orders) syncOrder(order);
    }

    private void syncOrder(TravelOrderSyncRecord order)
    {
        try
        {
            JSONObject fields = TravelOrderFeishuPayloadMapper.toFields(order, ownerOpenId);
            String payload = fields.toJSONString();
            if (payload.equals(syncedPayloads.get(order.getOrderNo()))) return;
            client.upsert(order.getOrderNo(), fields);
            syncedPayloads.put(order.getOrderNo(), payload);
        }
        catch (RuntimeException e)
        {
            log.error("旅居订单同步飞书失败 orderId={}, orderNo={}",
                    order.getOrderId(), order.getOrderNo(), e);
        }
    }

    private Date parseStartDate(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            throw new IllegalStateException("飞书旅居订单同步起始时间未配置");
        }
        try
        {
            SimpleDateFormat format = new SimpleDateFormat(START_TIME_PATTERN);
            format.setLenient(false);
            return format.parse(value);
        }
        catch (ParseException e)
        {
            throw new IllegalStateException("飞书旅居订单同步起始时间格式错误", e);
        }
    }
}
