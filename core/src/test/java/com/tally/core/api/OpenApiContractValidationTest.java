package com.tally.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import com.atlassian.oai.validator.springmvc.InvalidResponseException;
import com.tally.core.support.OpenApiValidationConfig;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC5: requests and responses are validated against {@code contracts/openapi.yaml} in tests. The
 * first test is the positive control — without it, a validator that silently failed to load the spec
 * would still let the rejection tests pass for the wrong reason.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({OpenApiValidationConfig.class, OpenApiContractValidationTest.OffContractController.class})
class OpenApiContractValidationTest {

    private final MockMvc mockMvc;

    @Autowired
    OpenApiContractValidationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void should_accept_the_me_response_when_it_matches_the_contract__TLY_004_AC5() throws Exception {
        var result = mockMvc.perform(get("/v1/me")).andReturn();

        assertThat(result.getResolvedException()).isNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void should_reject_the_response_when_it_violates_the_contract__TLY_004_AC5() throws Exception {
        var result = mockMvc.perform(get(OffContractController.PATH)).andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(InvalidResponseException.class)
                .hasMessageContaining("required property 'has_more' not found");
    }

    @Test
    void should_reject_the_request_when_a_query_parameter_violates_the_contract__TLY_004_AC5() throws Exception {
        // The validator reads the raw query string, so the parameter goes in the URI, not .param().
        var result = mockMvc.perform(get(OffContractController.PATH + "?limit=not-a-number"))
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("validation.request.parameter.schema.type")
                .hasMessageContaining("string found, integer expected");
    }

    /**
     * Serves a contracted path that has no production handler yet, with a body that omits the required
     * {@code has_more}. Nested so it stays out of every other test's context.
     */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class OffContractController {

        static final String PATH = "/v1/attention-items";

        @GetMapping(PATH)
        Map<String, Object> listAttentionItems() {
            return Map.of("data", List.of());
        }
    }
}
