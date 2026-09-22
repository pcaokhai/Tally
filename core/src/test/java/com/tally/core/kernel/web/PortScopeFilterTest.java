package com.tally.core.kernel.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class PortScopeFilterTest {

    private static final int API_PORT = 8080;
    private static final int OPS_PORT = 8081;

    private final PortScopeFilter filter = new PortScopeFilter(OPS_PORT);

    @Test
    void should_pass_the_request_through_when_a_tenant_path_arrives_on_the_tenant_port__TLY_004_AC4() throws Exception {
        MockHttpServletResponse response = filter("/v1/me", API_PORT);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void should_pass_the_request_through_when_an_operator_path_arrives_on_the_operator_port__TLY_004_AC4()
            throws Exception {
        MockHttpServletResponse response = filter("/ops/v1/health/services", OPS_PORT);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void should_answer_not_found_when_a_tenant_path_arrives_on_the_operator_port__TLY_004_AC4() throws Exception {
        MockHttpServletResponse response = filter("/v1/me", OPS_PORT);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void should_answer_not_found_when_an_operator_path_arrives_on_the_tenant_port__TLY_004_AC4() throws Exception {
        MockHttpServletResponse response = filter("/ops/v1/health/services", API_PORT);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }

    private MockHttpServletResponse filter(String uri, int localPort) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setLocalPort(localPort);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
