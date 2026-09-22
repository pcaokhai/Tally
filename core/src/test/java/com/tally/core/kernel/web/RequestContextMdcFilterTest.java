package com.tally.core.kernel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestContextMdcFilterTest {

    private static final String TRACEPARENT = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";

    private final RequestContextMdcFilter filter = new RequestContextMdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void should_adopt_the_inbound_ids_when_a_valid_traceparent_is_present__TLY_004_AC4() throws Exception {
        Map<String, String> seen = runWith(TRACEPARENT);

        assertThat(seen).containsEntry("trace_id", "4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(seen).containsEntry("span_id", "00f067aa0ba902b7");
        assertThat(seen.get("request_id")).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "garbage",
                "00-tooshort-00f067aa0ba902b7-01",
                "00-4BF92F3577B34DA6A3CE929D0E0E4736-00f067aa0ba902b7-01",
                "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01-extra",
                "'; drop table postings; --"
            })
    void should_generate_ids_when_the_traceparent_is_malformed__TLY_004_AC4(String header) throws Exception {
        Map<String, String> seen = runWith(header);

        assertThat(seen.get("trace_id")).hasSize(32).matches("[0-9a-f]+").isNotEqualTo(header);
        assertThat(seen.get("span_id")).hasSize(16).matches("[0-9a-f]+");
        assertThat(seen.get("request_id")).isNotBlank();
    }

    @Test
    void should_generate_ids_when_no_traceparent_is_present__TLY_004_AC4() throws Exception {
        Map<String, String> seen = runWith(null);

        assertThat(seen.get("trace_id")).hasSize(32).matches("[0-9a-f]+");
    }

    @Test
    void should_clear_the_mdc_when_the_filter_chain_throws__TLY_004_AC4() {
        FilterChain exploding = (request, response) -> {
            throw new IOException("boom");
        };

        assertThatIOException()
                .isThrownBy(
                        () -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), exploding));

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    private Map<String, String> runWith(String traceparent) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/me");
        if (traceparent != null) {
            request.addHeader("traceparent", traceparent);
        }
        Map<String, String> seen = new HashMap<>();
        FilterChain chain = (req, res) -> seen.putAll(MDC.getCopyOfContextMap());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        return seen;
    }
}
