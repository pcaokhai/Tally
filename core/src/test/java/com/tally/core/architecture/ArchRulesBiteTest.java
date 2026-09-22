package com.tally.core.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

/**
 * Proves each AC2 rule still bites. {@code com.tally.core.archfixtures} holds one deliberate
 * violator per rule; asserting that the rule reports it keeps the demonstration honest without
 * shipping a permanently red build.
 */
class ArchRulesBiteTest {

    private static final JavaClasses FIXTURES = new ClassFileImporter().importPackages("com.tally.core.archfixtures");

    @Test
    void should_report_a_violation_when_domain_imports_a_framework_type__TLY_004_AC2() {
        assertViolationsDetected(ArchitectureRules.domainHasNoFrameworkImports());
    }

    @Test
    void should_report_a_violation_when_money_uses_a_floating_point_type__TLY_004_AC2() {
        assertViolationsDetected(ArchitectureRules.noFloatingPointMoney());
    }

    @Test
    void should_report_a_violation_when_a_field_is_injected__TLY_004_AC2() {
        assertViolationsDetected(ArchitectureRules.noFieldInjection());
    }

    private static void assertViolationsDetected(ArchRule rule) {
        EvaluationResult result = rule.evaluate(FIXTURES);
        assertThat(result.getFailureReport().getDetails())
                .as("rule '%s' must still detect its deliberate violator", rule.getDescription())
                .isNotEmpty();
    }
}
