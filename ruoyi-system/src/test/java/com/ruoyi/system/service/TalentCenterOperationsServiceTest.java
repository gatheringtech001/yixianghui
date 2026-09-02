package com.ruoyi.system.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.domain.talent.TalentCenterOperationUpdateRequest;
import com.ruoyi.system.mapper.TalentCenterOperationsMapper;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TalentCenterOperationsServiceTest
{
    private TalentCenterOperationsMapper mapper;
    private TalentCenterResourceMapper resourceMapper;
    private ISysMenuService menuService;
    private RedisCache redisCache;
    private TalentCenterOperationsService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(TalentCenterOperationsMapper.class);
        resourceMapper = mock(TalentCenterResourceMapper.class);
        menuService = mock(ISysMenuService.class);
        redisCache = mock(RedisCache.class);
        service = new TalentCenterOperationsService(mapper, resourceMapper, menuService, redisCache);
        when(redisCache.setCacheObjectIfAbsent(anyString(), eq("used"), eq(24L), any())).thenReturn(true);
        when(mapper.selectCustomerStatuses()).thenReturn(Collections.singletonList("已成交"));
        when(mapper.selectCustomers(any(), any(), eq(false))).thenReturn(Collections.emptyList());
        when(mapper.selectOrders(any(), eq(false))).thenReturn(Collections.emptyList());
        when(mapper.selectSettlements(any(), any(), eq(false))).thenReturn(Collections.emptyList());
    }

    @Test
    void ordinaryUserReadsOnlyRowsScopedByBackendIdentity()
    {
        bind("talent-user", 101L, Collections.emptySet());
        when(mapper.selectConsultantId(101L)).thenReturn(501L);

        Map<String, Object> result = service.snapshot("talent-user");

        assertEquals("self", result.get("scope"));
        verify(mapper).selectCustomers(101L, 501L, false);
        verify(mapper).selectOrders(101L, false);
        verify(mapper).selectSettlements(101L, 501L, false);
    }

    @Test
    void administratorReadsAllRows()
    {
        bind("talent-admin", 1L, setOf("*:*:*"));
        when(mapper.selectConsultantId(1L)).thenReturn(null);
        when(mapper.selectCustomers(1L, null, true)).thenReturn(Collections.emptyList());
        when(mapper.selectOrders(1L, true)).thenReturn(Collections.emptyList());
        when(mapper.selectSettlements(1L, null, true)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.snapshot("talent-admin");

        assertEquals("admin", result.get("scope"));
        verify(mapper).selectCustomers(1L, null, true);
    }

    @Test
    void unmappedActorCannotReadAnyBusinessData()
    {
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.snapshot("unknown-user"));

        assertEquals(403, error.getHttpStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void customerUpdateUsesOwnerScopeAndCompareAndSet()
    {
        bind("talent-user", 101L, Collections.emptySet());
        when(mapper.selectConsultantId(101L)).thenReturn(501L);
        when(mapper.updateCustomer(7L, 501L, false, "潜在客户", "已成交", null, null,
                true, false, false)).thenReturn(1);

        Map<String, Object> result = service.update("talent-user", "eldercare", "customers", "customer:7",
                request("潜在客户", "已成交"), "idem-00000001");

        assertEquals("customer", result.get("kind"));
        verify(mapper).updateCustomer(7L, 501L, false, "潜在客户", "已成交", null, null,
                true, false, false);
    }

    @Test
    void compareAndSetFailureDoesNotPretendToSucceed()
    {
        bind("talent-user", 101L, Collections.emptySet());
        when(mapper.selectConsultantId(101L)).thenReturn(501L);

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.update("talent-user", "eldercare", "customers", "customer:7",
                        request("潜在客户", "已成交"), "idem-00000002"));

        assertEquals(409, error.getHttpStatus());
    }

    @Test
    void resourceCannotBeReinterpretedAsAnotherBusinessLine()
    {
        bind("talent-admin", 1L, setOf("*:*:*"));

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.update("talent-admin", "travel", "settlements", "income:7",
                        settlementRequest(), "idem-00000003"));

        assertEquals(404, error.getHttpStatus());
        verify(mapper).selectConsultantId(1L);
    }

    @Test
    void settlementUnionNormalizesHistoricalTextCollations() throws Exception
    {
        try (InputStream stream = getClass().getResourceAsStream(
                "/mapper/system/TalentCenterOperationsMapper.xml"))
        {
            assertNotNull(stream);
            String mapperXml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(mapperXml.contains(
                    "coalesce(o.feishu_order_no, o.order_no) collate utf8mb4_general_ci code"));
            assertTrue(mapperXml.contains(
                    "coalesce(o.travel_base_name, g.goods_name) collate utf8mb4_general_ci productName"));
            assertTrue(mapperXml.contains("i.income_no collate utf8mb4_general_ci code"));
            assertTrue(mapperXml.contains("i.product_name collate utf8mb4_general_ci productName"));
        }
    }

    private void bind(String actorId, Long userId, Set<String> permissions)
    {
        SysUser actor = new SysUser();
        actor.setUserId(userId);
        when(resourceMapper.selectEnabledActorByActorId(actorId)).thenReturn(actor);
        when(menuService.selectMenuPermsByUserId(userId)).thenReturn(permissions);
    }

    private TalentCenterOperationUpdateRequest request(String expectedStatus, String status)
    {
        TalentCenterOperationUpdateRequest request = new TalentCenterOperationUpdateRequest();
        request.setExpectedStatus(expectedStatus);
        request.setStatus(status);
        request.setConfirmationId("confirm-00000001");
        request.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        return request;
    }

    private TalentCenterOperationUpdateRequest settlementRequest()
    {
        TalentCenterOperationUpdateRequest request = request("false", null);
        request.setSettled(true);
        return request;
    }

    private Set<String> setOf(String... values)
    {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, values);
        return result;
    }
}
