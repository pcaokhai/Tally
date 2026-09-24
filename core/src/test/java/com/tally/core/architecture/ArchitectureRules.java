package com.tally.core.architecture;

import static com.tngtech.archunit.lang.conditions.ArchConditions.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
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

    /**
     * TLY-102 AC5: a class in {@code ..adapter.out..} that runs SQL (JdbcTemplate/JdbcClient/
     * EntityManager) must do so inside a transaction, so {@code TenantAwareDataSource} always has a
     * transaction-scoped connection to set {@code app.tenant_id} on for the RLS policy to see.
     *
     * <p>Limitation: only checks direct calls from the method body (no transitive call-graph walk),
     * so a method that delegates to a private helper which itself runs SQL is not caught here — keep
     * SQL calls directly in the {@code @Transactional} method.
     */
    static ArchRule sqlInAdapterOutRunsInATransaction() {
        return methods()
                .that()
                .areDeclaredInClassesThat()
                .resideInAPackage("..adapter.out..")
                .and()
                .areDeclaredInClassesThat()
                .resideInAPackage("com.tally.core..")
                .should(callSqlApiWithoutTransactional())
                .as("sqlInAdapterOutRunsInATransaction")
                .allowEmptyShould(true);
    }

    private static ArchCondition<JavaMethod> callSqlApiWithoutTransactional() {
        return new ArchCondition<>("run SQL without @Transactional on the method or its class") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean callsSql = method.getMethodCallsFromSelf().stream()
                        .map(JavaMethodCall::getTargetOwner)
                        .anyMatch(ArchitectureRules::isSqlApi);
                boolean transactional = method.isAnnotatedWith(TRANSACTIONAL)
                        || method.getOwner().isAnnotatedWith(TRANSACTIONAL);
                if (callsSql && !transactional) {
                    events.add(SimpleConditionEvent.violated(
                            method, method.getFullName() + " runs SQL outside @Transactional"));
                }
            }
        };
    }

    private static final String TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional";

    private static boolean isSqlApi(JavaClass owner) {
        return owner.isAssignableTo("org.springframework.jdbc.core.simple.JdbcClient")
                || owner.isAssignableTo("org.springframework.jdbc.core.JdbcTemplate")
                || owner.isAssignableTo("jakarta.persistence.EntityManager")
                || owner.isAssignableTo(java.sql.Statement.class)
                || owner.isAssignableTo(java.sql.Connection.class)
                // the ledger module uses jOOQ, not JPA (core/CLAUDE.md) — must be covered too.
                || owner.isAssignableTo("org.jooq.DSLContext")
                || owner.isAssignableTo("org.jooq.Query")
                || owner.isAssignableTo("org.jooq.ResultQuery");
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
