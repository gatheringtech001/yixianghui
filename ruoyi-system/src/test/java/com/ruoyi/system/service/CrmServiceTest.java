package com.ruoyi.system.service;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.mapper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrmServiceTest
{
    @Test void currentAgeRequiresAValidBirthdayRatherThanUnverifiedLegacyAge() {
        assertNull(CrmValues.age(CrmValues.map("birthday",null,"age",125)));
        assertNull(CrmValues.age(CrmValues.map("birthday",null,"age",65)));
        assertNull(CrmValues.age(CrmValues.map("birthday","invalid","age",65)));
        assertNull(CrmValues.age(CrmValues.map("birthday",java.time.LocalDate.now().plusDays(1).toString(),"age",0)));
        String birthday=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusYears(68).toString();
        assertEquals(68,CrmValues.age(CrmValues.map("birthday",birthday,"age",125)));
    }
    @Test void sharedUnknownAndRefundedMoneyIsNotPersonalSpend() {
        Map<String,Object> result=CrmLedger.build(Arrays.asList(
            CrmValues.map("id","1","businessLine","eldercare","amountCents",12000L,"allocation","single","excluded",0),
            CrmValues.map("id","2","businessLine","eldercare","amountCents",50000L,"allocation","shared","excluded",0),
            CrmValues.map("id","3","businessLine","eldercare","amountCents",null,"allocation","single","excluded",0),
            CrmValues.map("id","4","businessLine","travel","amountCents",90000L,"allocation","single","excluded",1)));
        Map<?,?> elder=(Map<?,?>)result.get("eldercare"), travel=(Map<?,?>)result.get("travel");
        assertEquals(12000L, elder.get("totalCents")); assertEquals(1,elder.get("missingAmountCount"));
        assertEquals(1,elder.get("sharedCount")); assertNull(travel.get("totalCents"));
    }
    @Test void testViewUsesReadScopeButNeverGrantsWrites() {
        TalentCenterResourceMapper actors=mock(TalentCenterResourceMapper.class);
        TalentCenterOperationsMapper operations=mock(TalentCenterOperationsMapper.class);
        TalentCenterTestViewService test=mock(TalentCenterTestViewService.class);
        SysUser actor=new SysUser(); actor.setUserId(108L); actor.setUserName("tester");
        when(actors.selectEnabledActorByActorId("actor")).thenReturn(actor);
        when(test.activeConsultantId("actor")).thenReturn(4L);
        CrmAccess service=new CrmAccess(actors,operations,test);
        assertNull(service.resolve("actor","self",false).get("actorUserId"));
        assertEquals(4L,service.resolve("actor","self",false).get("consultantId"));
        assertThrows(TalentCenterApiException.class,()->service.resolve("actor","self",true));
        assertThrows(TalentCenterApiException.class,()->service.resolve("actor","admin",false));
        verifyNoInteractions(operations);
    }
    @Test void validatesWriteFieldsAndPairedValues() {
        Map<String,Object> input=CrmValues.map("requestId","cfd07cb2-4a18-4f41-afc1-e7065a45bfb7", "content","回访", "method","phone", "occurredAt","2026-09-15T08:00:00Z");
        assertDoesNotThrow(()->CrmInput.validate("followups",false,input));
        input.put("nextAction","发方案");
        assertThrows(TalentCenterApiException.class,()->CrmInput.validate("followups",false,input));
        input.remove("nextAction"); input.put("consultantId",4);
        assertThrows(TalentCenterApiException.class,()->CrmInput.validate("followups",false,input));
    }
}
