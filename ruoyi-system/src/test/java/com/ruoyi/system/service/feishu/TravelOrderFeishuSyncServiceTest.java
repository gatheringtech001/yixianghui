package com.ruoyi.system.service.feishu;

import com.ruoyi.system.domain.TravelOrderSyncRecord;
import com.ruoyi.system.mapper.AppGoodsOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelOrderFeishuSyncServiceTest
{
    private AppGoodsOrderMapper orderMapper;
    private TravelOrderFeishuClient client;
    private TravelOrderFeishuSyncService service;
    private TravelOrderSyncRecord order;

    @BeforeEach
    void setUp()
    {
        orderMapper = mock(AppGoodsOrderMapper.class);
        client = mock(TravelOrderFeishuClient.class);
        service = new TravelOrderFeishuSyncService(orderMapper, client, Runnable::run);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "syncStartAt", "2026-08-30 00:00:00");
        ReflectionTestUtils.setField(service, "ownerOpenId", "ou_owner");

        order = new TravelOrderSyncRecord();
        order.setOrderId(1L);
        order.setOrderNo("20001");
        order.setStatus("0");
    }

    @Test
    void usesConfiguredCutoffAndSkipsUnchangedSuccessfulOrder() throws Exception
    {
        Date cutoff = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-08-30 00:00:00");
        when(orderMapper.selectTravelOrdersCreatedSince(cutoff))
                .thenReturn(Collections.singletonList(order));

        service.initialize();
        service.syncRecentOrders();
        service.syncRecentOrders();

        verify(orderMapper, times(2)).selectTravelOrdersCreatedSince(cutoff);
        verify(client, times(1)).upsert(eq("20001"), any());
    }

    @Test
    void retriesFailedOrderWithoutBreakingScheduler()
    {
        when(orderMapper.selectTravelOrdersCreatedSince(any()))
                .thenReturn(Collections.singletonList(order));
        doThrow(new IllegalStateException("temporary failure"))
                .doNothing()
                .when(client).upsert(eq("20001"), any());

        service.initialize();
        assertDoesNotThrow(service::syncRecentOrders);
        assertDoesNotThrow(service::syncRecentOrders);

        verify(client, times(2)).upsert(eq("20001"), any());
    }

    @Test
    void disabledSyncDoesNotReadOrders()
    {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.initialize();
        service.syncRecentOrders();

        verify(orderMapper, times(0)).selectTravelOrdersCreatedSince(any());
    }

    @Test
    void syncsCreatedTravelOrderImmediatelyById()
    {
        when(orderMapper.selectTravelOrderByOrderId(1L)).thenReturn(order);

        service.syncOrderAfterCommit(1L);

        verify(orderMapper).selectTravelOrderByOrderId(1L);
        verify(client).upsert(eq("20001"), any());
    }

    @Test
    void waitsForTransactionCommitBeforeDispatching()
    {
        Executor executor = mock(Executor.class);
        service = new TravelOrderFeishuSyncService(orderMapper, client, executor);
        ReflectionTestUtils.setField(service, "enabled", true);

        TransactionSynchronizationManager.initSynchronization();
        try
        {
            service.syncOrderAfterCommit(1L);
            verifyNoInteractions(executor);

            TransactionSynchronizationUtils.triggerAfterCommit();
            verify(executor).execute(any(Runnable.class));
        }
        finally
        {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void ignoresNonTravelOrderDuringImmediateSync()
    {
        when(orderMapper.selectTravelOrderByOrderId(1L)).thenReturn(null);

        service.syncOrderAfterCommit(1L);

        verify(client, times(0)).upsert(any(), any());
    }
}
