package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.vo.ConsultantApplicationRequest;
import com.ruoyi.system.mapper.AppConsultantMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultantApplicationTest {
    private final AppConsultantMapper mapper = mock(AppConsultantMapper.class);
    private final AppConsultantServiceImpl service = new AppConsultantServiceImpl();
    private final RedisCache cache = mock(RedisCache.class);
    private Object oldFactory;

    @BeforeEach void setup() {
        oldFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("redisCache", cache);
        new SpringUtils().postProcessBeanFactory(factory);
        ReflectionTestUtils.setField(service, "appConsultantMapper", mapper);
        when(mapper.lockConsultantApplicant(7L)).thenReturn(7L);
        when(mapper.insertAppConsultant(any())).thenAnswer(call -> {
            ((AppConsultant) call.getArgument(0)).setConsultantId(9L);
            return 1;
        });
        when(mapper.updateAppConsultant(any())).thenReturn(1);
    }

    @AfterEach void cleanup() { ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", oldFactory); }

    private ConsultantApplicationRequest request() {
        ConsultantApplicationRequest input = new ConsultantApplicationRequest();
        input.setConsultantName(" 张三 ");
        input.setMobile("13800000000");
        input.setAcceptedTerms(true);
        input.setTermsVersion(ConsultantApplicationRequest.TERMS_VERSION);
        return input;
    }

    @Test void approvesOwnedApplicationWithoutClaimingSomeoneElsesPhoneRecord() {
        assertEquals(1, service.applyConsultantAsUser(7L, request()));
        ArgumentCaptor<AppConsultant> saved = ArgumentCaptor.forClass(AppConsultant.class);
        verify(mapper).insertAppConsultant(saved.capture());
        AppConsultant row = saved.getValue();
        assertEquals(7L, row.getUserId());
        assertEquals("01", row.getStatus());
        assertEquals("张三", row.getConsultantName());
        assertTrue(row.getRemark().contains(ConsultantApplicationRequest.TERMS_VERSION));
        assertNull(row.getDeptId());
        assertNull(row.getConsultantNo());
        verify(mapper, never()).selectUnclaimedConsultantByMobile(anyString(), anyBoolean());
        verify(cache).deleteObject(contains("by_user_id:7"));
    }

    @Test void explicitConsentAndCurrentTermsAreRequiredBeforeAnyWrite() {
        ConsultantApplicationRequest input = request();
        input.setAcceptedTerms(false);
        assertThrows(ServiceException.class, () -> service.applyConsultantAsUser(7L, input));
        input.setAcceptedTerms(true);
        input.setTermsVersion("old");
        assertThrows(ServiceException.class, () -> service.applyConsultantAsUser(7L, input));
        input.setTermsVersion(ConsultantApplicationRequest.TERMS_VERSION);
        input.setMobile("123");
        assertThrows(ServiceException.class, () -> service.applyConsultantAsUser(7L, input));
        input.setMobile("13800000000");
        input.setConsultantName(" ");
        assertThrows(ServiceException.class, () -> service.applyConsultantAsUser(7L, input));
        verify(mapper, never()).insertAppConsultant(any());
    }

    @Test void approvedRetryIsIdempotentAndDoesNotOverwriteIdentity() {
        AppConsultant row = existing("01");
        when(mapper.selectAppConsultantByUserId(7L)).thenReturn(row);
        assertEquals(1, service.applyConsultantAsUser(7L, request()));
        verify(mapper, never()).insertAppConsultant(any());
        verify(mapper, never()).updateAppConsultant(any());
    }

    @Test void pendingAndRejectedApplicationsAreReusedWhenUserAppliesAgain() {
        for (String state : new String[]{"00", "02"}) {
            AppConsultant row = existing(state);
            when(mapper.selectAppConsultantByUserId(7L)).thenReturn(row);
            when(mapper.selectAppConsultantByConsultantId(9L)).thenReturn(row);
            assertEquals(1, service.applyConsultantAsUser(7L, request()));
        }
        verify(mapper, times(2)).updateAppConsultant(argThat(row -> row.getConsultantId() == 9L && "01".equals(row.getStatus())));
        verify(mapper, never()).insertAppConsultant(any());
    }

    @Test void ownedSelfApplicationSurvivesMissingWechatPhoneAndManualRejection() {
        service.applyConsultantAsUser(7L, request());
        ArgumentCaptor<AppConsultant> saved = ArgumentCaptor.forClass(AppConsultant.class);
        verify(mapper).insertAppConsultant(saved.capture());
        AppConsultant row = saved.getValue();
        row.setStatus("02");
        when(mapper.selectAppConsultantByUserId(7L)).thenReturn(row);
        assertSame(row, service.getOrClaimConsultantByUser(7L, ""));
        verify(mapper, never()).clearConsultantUserId(anyLong(), any());
    }

    @Test void rejectsMissingUserAndUsesTransactionForSameUserSerialization() throws Exception {
        when(mapper.lockConsultantApplicant(7L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.applyConsultantAsUser(7L, request()));
        assertNotNull(AppConsultantServiceImpl.class.getMethod("applyConsultantAsUser", Long.class,
                ConsultantApplicationRequest.class).getAnnotation(Transactional.class));
        verify(mapper, never()).insertAppConsultant(any());
    }

    private AppConsultant existing(String status) {
        AppConsultant row = new AppConsultant();
        row.setConsultantId(9L); row.setUserId(7L); row.setStatus(status);
        return row;
    }
}
