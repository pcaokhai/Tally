package com.tally.core.kernel;

import static org.assertj.core.api.Assertions.assertThat;

import com.tally.core.support.Http;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC4: proves requests really are handled on virtual threads, not merely that the property is set.
 * The probe is mapped outside the contracted {@code /v1} and {@code /ops/v1} prefixes so it can
 * never be mistaken for a real endpoint, and so {@code PortScopeFilter} lets it through.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VirtualThreadsIT.ThreadProbeController.class)
class VirtualThreadsIT {

    private static final int OPS_PORT = Http.freePort();

    @DynamicPropertySource
    static void opsPort(DynamicPropertyRegistry registry) {
        registry.add("tally.ops.port", () -> OPS_PORT);
    }

    private final int port;

    @Autowired
    VirtualThreadsIT(@LocalServerPort int port) {
        this.port = port;
    }

    @Test
    void should_handle_the_request_on_a_virtual_thread_when_a_controller_runs__TLY_004_AC4() {
        assertThat(Http.get(port, ThreadProbeController.PATH).body()).isEqualTo("true");
    }

    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class ThreadProbeController {

        static final String PATH = "/test-probe/thread";

        @GetMapping(PATH)
        String isVirtual() {
            return Boolean.toString(Thread.currentThread().isVirtual());
        }
    }
}
