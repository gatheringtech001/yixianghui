package com.ruoyi.system.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.domain.talent.TalentCenterAudit;
import com.ruoyi.system.domain.talent.TalentCenterResource;
import com.ruoyi.system.domain.talent.TalentCenterStatusRequest;
import com.ruoyi.system.domain.SysAuthUser;
import com.ruoyi.system.mapper.TalentCenterResourceMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TalentCenterAdminServiceTest
{
    private TalentCenterResourceMapper mapper;
    private ISysMenuService menuService;
    private RedisCache redisCache;
    private TalentCenterAdminService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(TalentCenterResourceMapper.class);
        menuService = mock(ISysMenuService.class);
        redisCache = mock(RedisCache.class);
        service = new TalentCenterAdminService(mapper, menuService, redisCache);
        when(redisCache.setCacheObjectIfAbsent(anyString(), eq("accepted"), eq(24L), any())).thenReturn(true);
        when(mapper.insertAudit(any())).thenAnswer(invocation -> {
            TalentCenterAudit audit = invocation.getArgument(0);
            audit.setAuditId(99L);
            return 1;
        });
    }

    @Test
    void rejectsOrdinaryUserReadingWithoutListPermission()
    {
        SysUser actor = actor(101L);
        when(mapper.selectEnabledActorByActorId("talent-user-ordinary")).thenReturn(actor);
        when(menuService.selectMenuPermsByUserId(101L)).thenReturn(Collections.emptySet());

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.list("goods", "talent-user-ordinary", 1, 20));

        assertEquals(403, error.getHttpStatus());
        verify(mapper).selectEnabledActorByActorId("talent-user-ordinary");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void rejectsUnmappedActor()
    {
        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.list("goods", "talent-user-unknown", 1, 20));

        assertEquals(403, error.getHttpStatus());
        verify(mapper).selectEnabledActorByActorId("talent-user-unknown");
    }

    @Test
    void rejectsUserMissingEditPermission()
    {
        SysUser actor = actor(102L);
        when(mapper.selectEnabledActorByActorId("talent-user-limited")).thenReturn(actor);
        when(menuService.selectMenuPermsByUserId(102L))
                .thenReturn(setOf("system:app_goods:list", "system:app_goods:query"));

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.updateStatus("goods", 8L, "talent-user-limited", request("0", "1"),
                        "idem-key-0001", "talent-service", "127.0.0.1"));

        assertEquals(403, error.getHttpStatus());
        verify(mapper).finishAudit(99L, null, "1", "PERMISSION_DENIED");
    }

    @Test
    void rejectsStatusConflictAndAuditsCurrentState()
    {
        allowEditor("talent-user-editor", 103L);
        when(mapper.getGoods(8L)).thenReturn(resource(8L, "1"));

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.updateStatus("goods", 8L, "talent-user-editor", request("0", "1"),
                        "idem-key-0002", "talent-service", "127.0.0.1"));

        assertEquals(409, error.getHttpStatus());
        verify(mapper).finishAudit(99L, "1", "1", "STATUS_CONFLICT");
    }

    @Test
    void rejectsIdempotencyReplayBeforeDatabaseMutation()
    {
        when(redisCache.setCacheObjectIfAbsent(anyString(), eq("accepted"), eq(24L), any()))
                .thenReturn(false);

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.updateStatus("goods", 8L, "talent-user-editor", request("0", "1"),
                        "idem-key-0003", "talent-service", "127.0.0.1"));

        assertEquals(409, error.getHttpStatus());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsConfirmationOrIdempotencyReplayFromDatabaseConstraint()
    {
        doThrow(new DuplicateKeyException("duplicate")).when(mapper).insertAudit(any());

        TalentCenterApiException error = assertThrows(TalentCenterApiException.class,
                () -> service.updateStatus("goods", 8L, "talent-user-editor", request("0", "1"),
                        "idem-key-0005", "talent-service", "127.0.0.1"));

        assertEquals(409, error.getHttpStatus());
    }

    @Test
    void modifiesStatusWithCompareAndSetAndReturnsFreshResource()
    {
        allowEditor("talent-user-editor", 104L);
        TalentCenterResource before = resource(8L, "0");
        TalentCenterResource after = resource(8L, "1");
        when(mapper.getGoods(8L)).thenReturn(before, after);
        when(mapper.updateGoodsStatus(8L, "0", "1")).thenReturn(1);

        TalentCenterResource result = service.updateStatus("goods", 8L,
                "talent-user-editor", request("0", "1"), "idem-key-0004", "talent-service", "127.0.0.1");

        assertEquals("1", result.getStatus());
        verify(mapper).selectEnabledActorByActorId("talent-user-editor");
        verify(mapper).updateAuditActor(99L, 104L);
        verify(mapper).updateGoodsStatus(8L, "0", "1");
        verify(mapper).finishAudit(99L, "0", "1", "SUCCESS");
        ArgumentCaptor<TalentCenterAudit> captor = ArgumentCaptor.forClass(TalentCenterAudit.class);
        verify(mapper).insertAudit(captor.capture());
        assertEquals(null, captor.getValue().getActorUserId());
        assertEquals(64, captor.getValue().getIdempotencyKeyHash().length());
    }

    @Test
    void allowsMappedTalentAdminWithDedicatedContentStatusPermission()
    {
        SysUser actor = actor(108L);
        when(mapper.selectEnabledActorByActorId("talent-user-admin")).thenReturn(actor);
        when(menuService.selectMenuPermsByUserId(108L))
                .thenReturn(setOf("service:content:goods:status"));
        when(mapper.getGoods(108L)).thenReturn(resource(108L, "1"), resource(108L, "0"));
        when(mapper.updateGoodsStatus(108L, "1", "0")).thenReturn(1);

        TalentCenterResource result = service.updateStatus("goods", 108L,
                "talent-user-admin", request("1", "0"), "idem-key-0108", "talent-service", "127.0.0.1");

        assertEquals("0", result.getStatus());
        verify(mapper).updateGoodsStatus(108L, "1", "0");
    }

    @Test
    void doesNotSerializeWechatIdentifiers() throws Exception
    {
        SysAuthUser auth = new SysAuthUser();
        auth.setUuid("wechat_mnp_open-id-value");
        auth.setUnionId("union-id-sensitive");
        auth.setAppId("wx-app-id");

        String json = new ObjectMapper().writeValueAsString(auth);

        assertFalse(json.contains("open-id-value"));
        assertFalse(json.contains("union-id-sensitive"));
        assertFalse(json.contains("wx-app-id"));
    }

    private void allowEditor(String actorId, Long userId)
    {
        when(mapper.selectEnabledActorByActorId(actorId)).thenReturn(actor(userId));
        when(menuService.selectMenuPermsByUserId(userId)).thenReturn(setOf("service:content:goods:status"));
    }

    private TalentCenterStatusRequest request(String expected, String status)
    {
        TalentCenterStatusRequest request = new TalentCenterStatusRequest();
        request.setExpectedStatus(expected);
        request.setStatus(status);
        request.setConfirmationId("confirm-00000001");
        request.setConfirmedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        return request;
    }

    private SysUser actor(Long id)
    {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setStatus("0");
        user.setDelFlag("0");
        return user;
    }

    private TalentCenterResource resource(Long id, String status)
    {
        TalentCenterResource resource = new TalentCenterResource();
        resource.setId(id);
        resource.setName("test-resource");
        resource.setStatus(status);
        return resource;
    }

    private Set<String> setOf(String... permissions)
    {
        Set<String> result = new HashSet<>();
        Collections.addAll(result, permissions);
        return result;
    }
}
