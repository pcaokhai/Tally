package com.tally.core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * Runs the AC2 rules against production code. Test sources — including the deliberate violators in
 * {@code com.tally.core.archfixtures} — are excluded; {@link ArchRulesBiteTest} covers those.
 */
class ArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.tally.core");

    @Test
    void should_keep_the_domain_free_of_framework_imports_when_rules_run__TLY_004_AC2() {
        ArchitectureRules.domainHasNoFrameworkImports().check(PRODUCTION_CLASSES);
    }

    @Test
    void should_reject_floating_point_money_when_rules_run__TLY_004_AC2() {
        ArchitectureRules.noFloatingPointMoney().check(PRODUCTION_CLASSES);
    }

    @Test
    void should_reject_field_injection_when_rules_run__TLY_004_AC2() {
        ArchitectureRules.noFieldInjection().check(PRODUCTION_CLASSES);
    }

    @Test
    void should_require_a_transaction_around_adapter_out_sql_when_rules_run__TLY_102_AC5() {
        ArchitectureRules.sqlInAdapterOutRunsInATransaction().check(PRODUCTION_CLASSES);
    }
}
