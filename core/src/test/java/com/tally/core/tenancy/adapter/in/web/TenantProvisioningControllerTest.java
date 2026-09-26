package com.tally.core.tenancy.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tally.core.kernel.problem.GlobalProblemAdvice;
import com.tally.core.kernel.problem.ProblemException;
import com.tally.core.tenancy.application.CreateTenantUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** MockMvc test for {@code POST /ops/v1/tenants} AC5 (docs/06 TLY-101). */
@ExtendWith(MockitoExtension.class)
class TenantProvisioningControllerTest {

    @Mock
    private CreateTenantUseCase createTenantUseCase;

    @Test
    void should_return_400_validation_failed_when_plan_code_is_unknown__TLY_101_AC5() throws Exception {
        given(createTenantUseCase.create(any()))
                .willThrow(ProblemException.validationFailed("plan_code", "unknown plan_code"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TenantProvisioningController(createTenantUseCase))
                .setControllerAdvice(new GlobalProblemAdvice())
                .build();

        mockMvc.perform(post("/ops/v1/tenants")
                        .header("Idempotency-Key", "test-idem-key-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Acme", "plan_code": "nope", "owner_email": "owner@acme.test"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.param").value("plan_code"));
    }
}
