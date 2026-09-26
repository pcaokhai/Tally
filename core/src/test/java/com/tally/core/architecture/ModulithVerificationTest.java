package com.tally.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tally.core.CoreApplication;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithVerificationTest {

    private static final List<String> EXPECTED_MODULES = List.of(
            "billing",
            "catalog",
            "customers",
            "events",
            "kernel",
            "ledger",
            "metering",
            "operations",
            "payments",
            "reporting",
            "tenancy");

    /**
     * {@code com.tally.core.api} and {@code com.tally.core.opsapi} hold the OpenAPI-generated
     * interfaces and models (tenant and ops specs respectively, TLY-101). They are build output,
     * not bounded contexts, so both are excluded from the module model.
     */
    private static final ApplicationModules MODULES = ApplicationModules.of(
            CoreApplication.class,
            JavaClass.Predicates.resideInAPackage("com.tally.core.api..")
                    .or(JavaClass.Predicates.resideInAPackage("com.tally.core.opsapi..")));

    @Test
    void should_expose_the_eleven_bounded_contexts_when_modules_are_scanned__TLY_004_AC1() {
        assertThat(MODULES.stream()
                        .map(module -> module.getIdentifier().toString())
                        .sorted())
                .containsExactlyElementsOf(EXPECTED_MODULES);
    }

    @Test
    void should_have_no_module_violations_when_verify_runs__TLY_004_AC2() {
        MODULES.verify();
    }
}
