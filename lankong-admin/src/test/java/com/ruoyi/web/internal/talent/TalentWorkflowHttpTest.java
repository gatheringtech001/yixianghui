package com.ruoyi.web.internal.talent;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ruoyi.system.domain.talent.TalentCenterApiException;
import com.ruoyi.system.service.TalentInboxService;
import com.ruoyi.system.service.TalentBindingService;
import com.ruoyi.system.service.TalentWorkflowDetailService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TalentWorkflowHttpTest {
    @Test void workflowErrorsPreserveForbiddenAndConflictHttpStatus() throws Exception {
        TalentInboxService inbox = mock(TalentInboxService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TalentCenterWorkflowController(inbox,
            mock(TalentBindingService.class), mock(TalentWorkflowDetailService.class)))
            .setControllerAdvice(new TalentCenterExceptionHandler()).build();
        for (int code : new int[]{400, 403, 409}) {
            doThrow(new TalentCenterApiException(code, "明确的领域错误")).when(inbox).summary("actor", "self", "all");
            mvc.perform(get("/internal/talent-center/v1/workflow/inbox/all")
                .requestAttr(TalentCenterHmacFilter.ACTOR_ID_ATTRIBUTE, "actor")
                .requestAttr(TalentCenterHmacFilter.ACTOR_SCOPE_ATTRIBUTE, "self"))
                .andExpect(status().is(code)).andExpect(jsonPath("$.code").value(code));
        }
    }
}
