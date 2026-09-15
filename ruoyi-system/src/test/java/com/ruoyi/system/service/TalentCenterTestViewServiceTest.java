package com.ruoyi.system.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TalentCenterTestViewServiceTest
{
    private final RedisCache redis = mock(RedisCache.class);
    private final TalentCenterOperationsMapper mapper = mock(TalentCenterOperationsMapper.class);
    private final TalentCenterResourceMapper actors = mock(TalentCenterResourceMapper.class);
    private final Instant deadline = Instant.now().plusSeconds(3600);

    private TalentCenterTestViewService service(Instant until)
    {
        SysUser actor = new SysUser(); actor.setUserId(108L);
        when(actors.selectEnabledActorByActorId("tester")).thenReturn(actor);
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("name", "测试管家");
        when(mapper.selectTestConsultant(4L)).thenReturn(target);
        return new TalentCenterTestViewService(redis, mapper, actors, "tester", 4L, until.toString());
    }

    @Test void onlyConfiguredActorCanSwitchAndTargetIsNeverBrowserSelected()
    {
        TalentCenterTestViewService service = service(deadline);
        assertEquals(false, service.status("other").get("available"));
        assertThrows(TalentCenterApiException.class, () -> service.setEnabled("other", true));
        verify(redis, never()).setCacheObject(anyString(), any(), anyInt(), any());
        when(redis.getCacheObject(anyString())).thenReturn("true");
        assertEquals(true, service.setEnabled("tester", true).get("enabled"));
        verify(redis).setCacheObject(anyString(), eq("true"), anyInt(), eq(TimeUnit.SECONDS));
        assertEquals(4L, service.activeConsultantId("tester"));
    }

    @Test void missingExpiredAndRestoredStateNeverGrantsTestAccess()
    {
        TalentCenterTestViewService service = service(deadline);
        assertNull(service.activeConsultantId("tester"));
        when(redis.getCacheObject(anyString())).thenReturn("false");
        assertNull(service.activeConsultantId("tester"));
        assertEquals(false, service.setEnabled("tester", false).get("enabled"));
        TalentCenterTestViewService expired = service(Instant.EPOCH);
        assertEquals(false, expired.status("tester").get("available"));
        assertNull(expired.activeConsultantId("tester"));
        assertThrows(TalentCenterApiException.class, () -> expired.setEnabled("tester", true));
    }

    @Test void corruptedStateAndMissingTargetFailExplicitly()
    {
        TalentCenterTestViewService service = service(deadline);
        when(redis.getCacheObject(anyString())).thenReturn("invalid");
        assertThrows(TalentCenterApiException.class, () -> service.activeConsultantId("tester"));
        when(redis.getCacheObject(anyString())).thenReturn("true");
        when(mapper.selectTestConsultant(4L)).thenReturn(null);
        assertThrows(TalentCenterApiException.class, () -> service.status("tester"));
    }

    @Test void testScopeCannotBorrowTesterOrdersAndWritesAreRejected()
    {
        TalentCenterTestViewService testView = service(deadline);
        when(redis.getCacheObject(anyString())).thenReturn("true");
        when(mapper.selectCustomers(any(), any(), anyBoolean())).thenReturn(new java.util.ArrayList<>());
        TalentCenterOperationsService service = new TalentCenterOperationsService(mapper, actors, redis, testView);
        service.snapshot("tester", "self");
        verify(mapper).selectCustomers(null, 4L, false);
        verify(mapper).selectOrders(null, 4L, false);
        verify(mapper).selectSettlements(null, 4L, false);
        assertEquals(403, assertThrows(TalentCenterApiException.class,
            () -> service.update("tester", "self", "eldercare", "customers", "customer:1", null, null)).getHttpStatus());
        verify(mapper, never()).updateCustomer(any(), any(), anyBoolean(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test void commissionsUseTargetAndRestoringUsesOriginalIdentity()
    {
        TalentCenterTestViewService testView = service(deadline);
        when(redis.getCacheObject(anyString())).thenReturn("true");
        when(mapper.selectCommissionRecords(any())).thenReturn(new java.util.ArrayList<>());
        TalentCenterOperationsService service = new TalentCenterOperationsService(mapper, actors, redis, testView);
        service.commissions("tester", "self", "all", "first", null);
        org.mockito.ArgumentCaptor<Map> filters = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(mapper).selectCommissionRecords(filters.capture());
        assertEquals(4L, filters.getValue().get("consultantId"));
        assertEquals(false, filters.getValue().get("admin"));
        assertThrows(TalentCenterApiException.class, () -> service.commissions("tester", "self", "42", "first", null));
        when(redis.getCacheObject(anyString())).thenReturn("false");
        when(mapper.selectConsultantId(108L)).thenReturn(24L);
        when(mapper.selectCustomers(any(), any(), anyBoolean())).thenReturn(new java.util.ArrayList<>());
        service.snapshot("tester", "self");
        verify(mapper).selectCustomers(108L, 24L, false);
        verify(mapper).selectOrders(108L, 24L, false);
    }
}
