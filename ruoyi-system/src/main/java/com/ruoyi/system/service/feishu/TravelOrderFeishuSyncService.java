package com.ruoyi.system.service.feishu;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.TravelOrderSyncRecord;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 旅居订单提交后立即同步飞书；定时扫描仅负责失败补偿。
 */
@Service
public class TravelOrderFeishuSyncService
{
    private static final Logger log = LoggerFactory.getLogger(TravelOrderFeishuSyncService.class);
    private static final String START_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final AppGoodsOrderMapper orderMapper;
    private final TravelOrderFeishuClient client;
    private final Executor executor;
    private final Map<String, String> syncedPayloads = new ConcurrentHashMap<>();

    @Value("${feishu.travel-order.enabled:false}")
    private boolean enabled;

    @Value("${feishu.travel-order.sync-start-at:}")
    private String syncStartAt;

    @Value("${feishu.travel-order.owner-open-id:}")
    private String ownerOpenId;

    private Date syncStartDate;

    public TravelOrderFeishuSyncService(AppGoodsOrderMapper orderMapper,
                                        TravelOrderFeishuClient client,
                                        @Qualifier("threadPoolTaskExecutor") Executor executor)
    {
        this.orderMapper = orderMapper;
        this.client = client;
        this.executor = executor;
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

    /**
     * 在本地订单事务提交后异步同步，避免外部接口失败回滚用户下单。
     */
    public void syncOrderAfterCommit(Long orderId)
    {
        if (!enabled || orderId == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
            {
                @Override
                public void afterCommit()
                {
                    dispatch(orderId);
                }
            });
            return;
        }
        dispatch(orderId);
    }

    private void dispatch(Long orderId)
    {
        try
        {
            executor.execute(() -> syncOrderById(orderId));
        }
        catch (RuntimeException e)
        {
            log.error("提交旅居订单飞书同步任务失败 orderId={}", orderId, e);
        }
    }

    private void syncOrderById(Long orderId)
    {
        try
        {
            TravelOrderSyncRecord order = orderMapper.selectTravelOrderByOrderId(orderId);
            if (order != null) syncOrder(order);
        }
        catch (RuntimeException e)
        {
            log.error("读取待同步旅居订单失败 orderId={}", orderId, e);
        }
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
