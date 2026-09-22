package com.tally.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CoreApplicationTest {

    @Test
    void should_load_the_application_context_when_the_app_starts__TLY_004_AC1() {
        // the context failing to start fails this test
    }
}
