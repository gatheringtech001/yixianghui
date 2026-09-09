package com.ruoyi.web.controller.app;

import com.ruoyi.system.domain.vo.ConsultantApplicationRequest;
import com.ruoyi.system.service.IAppConsultantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ConsultantApplicationBoundaryTest {
    private final IAppConsultantService service = mock(IAppConsultantService.class);
    private MockMvc mvc;
    private static final String URL = "/mnp/app_user/apply_consultant";

    @BeforeEach void setup() {
        AppUserController controller = new AppUserController() {
            @Override public Long getUserId() { return 7L; }
        };
        ReflectionTestUtils.setField(controller, "consultantService", service);
        mvc = standaloneSetup(controller).build();
        when(service.applyConsultantAsUser(eq(7L), any())).thenReturn(1);
    }

    @Test void rejectsMissingConsentAndMalformedContactAtHttpBoundary() throws Exception {
        for (String json : new String[]{
                "{\"consultantName\":\"test\",\"mobile\":\"13800000000\"}",
                "{\"consultantName\":\"test\",\"mobile\":\"123\",\"acceptedTerms\":true,\"termsVersion\":\"2026-09-v1\"}",
                "{\"consultantName\":\"test\",\"mobile\":\"13800000000\",\"acceptedTerms\":false,\"termsVersion\":\"2026-09-v1\"}"
        }) {
            mvc.perform(post(URL).contentType("application/json").content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void serverUsesAuthenticatedUserInsteadOfPostedIdentityOrStatus() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content(
                "{\"consultantName\":\"test\",\"mobile\":\"13800000000\",\"acceptedTerms\":true,"
                + "\"termsVersion\":\"2026-09-v1\",\"userId\":999,\"consultantId\":888,\"status\":\"01\",\"deptId\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).applyConsultantAsUser(eq(7L), any(ConsultantApplicationRequest.class));
    }
}
