package com.ruoyi.system.service;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.AppConsultantMapper;
import com.ruoyi.system.service.impl.AppConsultantServiceImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class TalentWorkflowTest {
    @Test void ordinaryUsersCannotRequestGlobalOrUnassignedQueues(){
        TalentWorkflowAccess access=mock(TalentWorkflowAccess.class);when(access.resolve("self","self")).thenReturn(new TalentWorkflowAccess.Scope("self",10L,false));
        JdbcTemplate jdbc=mock(JdbcTemplate.class);TalentInboxService service=new TalentInboxService(jdbc,access);
        assertThrows(TalentCenterApiException.class,()->service.summary("self","self","all"));
        assertThrows(TalentCenterApiException.class,()->service.summary("self","self","unassigned"));verifyNoInteractions(jdbc);
    }
    @Test void adminPersonalQueueUsesOwnerAndNotReferrer(){
        TalentWorkflowAccess access=mock(TalentWorkflowAccess.class);when(access.resolve("admin","admin")).thenReturn(new TalentWorkflowAccess.Scope("admin",10L,true));
        JdbcTemplate jdbc=mock(JdbcTemplate.class);when(jdbc.queryForList(anyString(),any(Object[].class))).thenReturn(Collections.emptyList());
        TalentInboxService service=new TalentInboxService(jdbc,access);service.page("admin","admin","my","travel","first");
        verify(jdbc).queryForList(argThat(sql->sql.contains("o.service_owner_user_id=?")&&!sql.contains("app_user_inviter")&&sql.contains("LIMIT 51")),eq(10L),eq(0L));
    }
    @Test void malformedIdsAreRejected(){assertThrows(TalentCenterApiException.class,()->TalentWorkflowAccess.id("1 OR 1=1"));assertThrows(TalentCenterApiException.class,()->TalentWorkflowAccess.id("0"));}
    @Test void bindingIsAdminOnly(){TalentWorkflowAccess access=mock(TalentWorkflowAccess.class);doThrow(new TalentCenterApiException(403,"forbidden")).when(access).admin("self","self");TalentBindingStore store=mock(TalentBindingStore.class);TalentBindingService service=new TalentBindingService(store,access,mock(RedisCache.class));assertThrows(TalentCenterApiException.class,()->service.preview("self","self",Collections.emptyMap()));verifyNoInteractions(store);}
    @Test void preventsImplicitRebinding(){
        JdbcTemplate jdbc=mock(JdbcTemplate.class);TalentBindingStore store=spy(new TalentBindingStore(jdbc));TalentWorkflowAccess access=mock(TalentWorkflowAccess.class);when(access.admin("admin","admin")).thenReturn(new TalentWorkflowAccess.Scope("admin",1L,true));
        Map<String,Object> account=new LinkedHashMap<>();account.put("user_id",10L);account.put("nick_name","后台用户");doReturn(account).when(store).user(10L,false);doReturn(Collections.singletonMap("user_id",20L)).when(store).binding("aa109b99-1137-40e5-80d2-323188a1ac99",false);
        Map<String,Object> input=new LinkedHashMap<>();input.put("kind","bind-identity");input.put("targetActorId","aa109b99-1137-40e5-80d2-323188a1ac99");input.put("backendUserId","10");input.put("note","已核实身份");
        TalentBindingService service=new TalentBindingService(store,access,mock(RedisCache.class));assertThrows(TalentCenterApiException.class,()->service.preview("admin","admin",input));
    }
    @Test void explicitBindingSurvivesPhoneBasedGuessing(){
        AppConsultantMapper mapper=mock(AppConsultantMapper.class);AppConsultant expected=new AppConsultant();expected.setConsultantId(9L);when(mapper.selectExplicitConsultantByUserId(10L)).thenReturn(expected);
        AppConsultantServiceImpl service=new AppConsultantServiceImpl();ReflectionTestUtils.setField(service,"appConsultantMapper",mapper);
        assertSame(expected,service.getOrClaimConsultantByUser(10L,""));verify(mapper,never()).selectAppConsultantByUserId(anyLong());
    }
}
