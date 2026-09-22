package com.tally.core.architecture;

import static com.tngtech.archunit.lang.conditions.ArchConditions.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.List;
import java.util.Set;

/**
 * The architecture rules required by TLY-004 AC2, shared by {@link ArchitectureTest} (which runs
 * them against production code) and {@link ArchRulesBiteTest} (which proves each one still bites).
 */
final class ArchitectureRules {

    private static final String[] FRAMEWORK_PACKAGES = {
        "org.springframework..",
        "com.fasterxml.jackson..",
        "tools.jackson..",
        "jakarta..",
        "org.hibernate..",
        "org.jooq..",
        "org.apache.kafka..",
        "java.sql..",
        "javax.sql.."
    };

    private static final Set<String> FLOATING_POINT_TYPES =
            Set.of("double", "float", "java.lang.Double", "java.lang.Float", "java.math.BigDecimal");

    private static final List<String> INJECTION_ANNOTATIONS = List.of(
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.beans.factory.annotation.Value",
            "jakarta.inject.Inject",
            "javax.inject.Inject",
            "jakarta.annotation.Resource",
            "javax.annotation.Resource");

    private ArchitectureRules() {}

    /** The domain layer stays pure: no framework, persistence or serialization types. */
    static ArchRule domainHasNoFrameworkImports() {
        return noClasses()
                .that()
                .resideInAPackage("com.tally.core..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(FRAMEWORK_PACKAGES)
                .as("domainHasNoFrameworkImports")
                .allowEmptyShould(true);
    }

    /** ADR-004: money is long minor units, never a floating point or BigDecimal amount. */
    static ArchRule noFloatingPointMoney() {
        return noClasses()
                .that()
                .resideInAnyPackage("..money..", "..ledger..")
                .should(useFloatingPointTypes())
                .as("noFloatingPointMoney")
                .allowEmptyShould(true);
    }

    /** Constructor injection only: no injected fields anywhere in the core. */
    static ArchRule noFieldInjection() {
        ArchCondition<JavaField> annotatedWithAnyInjectionAnnotation = INJECTION_ANNOTATIONS.stream()
                .map(annotation -> beAnnotatedWith(annotation).<JavaField>forSubtype())
                .reduce(ArchCondition::or)
                .orElseThrow();

        return noFields()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("com.tally.core..")
                .and()
                .areDeclaredInClassesThat()
                .resideOutsideOfPackage("com.tally.core.api..")
                .should(annotatedWithAnyInjectionAnnotation)
                .as("noFieldInjection")
                .allowEmptyShould(true);
    }

    private static ArchCondition<JavaClass> useFloatingPointTypes() {
        return new ArchCondition<>("use double, float, Double, Float or BigDecimal") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getFields().forEach(field -> report(events, field.getRawType(), field.getFullName(), "field"));
                item.getCodeUnits().forEach(codeUnit -> checkCodeUnit(codeUnit, events));
            }
        };
    }

    private static void checkCodeUnit(JavaCodeUnit codeUnit, ConditionEvents events) {
        report(events, codeUnit.getRawReturnType(), codeUnit.getFullName(), "return type of");
        codeUnit.getRawParameterTypes()
                .forEach(parameter -> report(events, parameter, codeUnit.getFullName(), "parameter of"));
    }

    private static void report(ConditionEvents events, JavaClass type, String owner, String role) {
        if (FLOATING_POINT_TYPES.contains(type.getName())) {
            events.add(SimpleConditionEvent.satisfied(
                    owner, "%s %s has floating point type %s".formatted(role, owner, type.getName())));
        }
    }
}
