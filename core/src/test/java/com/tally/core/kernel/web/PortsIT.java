package com.tally.core.kernel.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tally.core.support.Http;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * AC4: three listening ports with disjoint scopes. Fixed ports would be flaky on a shared machine,
 * so the three ports move to free ones and only the routing behaviour is asserted here;
 * {@link com.tally.core.kernel.config.ServerPortDefaultsTest} pins the documented defaults.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PortsIT {

    private static final int API_PORT = Http.freePort();
    private static final int OPS_PORT = Http.freePort();
    private static final int MANAGEMENT_PORT = Http.freePort();

    @DynamicPropertySource
    static void ports(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> API_PORT);
        registry.add("tally.ops.port", () -> OPS_PORT);
        registry.add("management.server.port", () -> MANAGEMENT_PORT);
    }

    @Test
    void should_serve_the_actuator_health_probes_when_called_on_the_management_port__TLY_004_AC4() {
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health")).isEqualTo(200);
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health/liveness")).isEqualTo(200);
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health/readiness")).isEqualTo(200);
    }

    @Test
    void should_serve_the_tenant_api_when_called_on_the_tenant_port__TLY_004_AC4() {
        assertThat(Http.status(API_PORT, "/v1/me")).isEqualTo(200);
    }

    @Test
    void should_answer_not_found_when_the_tenant_api_is_called_on_the_operator_port__TLY_004_AC4() {
        assertThat(Http.status(OPS_PORT, "/v1/me")).isEqualTo(404);
    }

    @Test
    void should_answer_not_found_when_an_operator_path_is_called_on_the_tenant_port__TLY_004_AC4() {
        assertThat(Http.status(API_PORT, "/ops/v1/health/services")).isEqualTo(404);
    }

    @Test
    void should_answer_not_found_when_the_tenant_api_is_called_on_the_management_port__TLY_004_AC4() {
        assertThat(Http.status(MANAGEMENT_PORT, "/v1/me")).isEqualTo(404);
    }
}
