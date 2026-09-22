package com.tally.core.kernel.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Pins the port numbers of docs/02 §8 and ADR-022. {@link com.tally.core.kernel.web.PortsIT} proves
 * the behaviour on free ports; this proves the shipped defaults are the documented ones.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ServerPortDefaultsTest {

    private final Environment environment;
    private final OpsServerProperties opsServerProperties;

    @Autowired
    ServerPortDefaultsTest(Environment environment, OpsServerProperties opsServerProperties) {
        this.environment = environment;
        this.opsServerProperties = opsServerProperties;
    }

    @Test
    void should_default_to_the_documented_tenant_operator_and_actuator_ports__TLY_004_AC4() {
        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8080);
        assertThat(opsServerProperties.port()).isEqualTo(8081);
        assertThat(environment.getProperty("management.server.port", Integer.class))
                .isEqualTo(8082);
    }

    @Test
    void should_expose_only_the_health_and_info_actuator_endpoints__TLY_004_AC4() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info");
        assertThat(environment.getProperty("management.endpoint.health.probes.enabled", Boolean.class))
                .isTrue();
    }

    @Test
    void should_enable_virtual_threads__TLY_004_AC4() {
        assertThat(environment.getProperty("spring.threads.virtual.enabled", Boolean.class))
                .isTrue();
    }
}
