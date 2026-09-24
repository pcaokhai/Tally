package com.tally.core.tenancy.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tally.core.kernel.problem.ProblemException;
import com.tally.core.tenancy.application.port.out.TenantProvisioningPort;
import com.tally.core.tenancy.domain.IsolationTier;
import com.tally.core.tenancy.domain.PlanVersion;
import com.tally.core.tenancy.domain.Tenant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CreateTenantUseCase} against an in-memory fake port (docs/06 TLY-101). */
class CreateTenantUseCaseTest {

    private final FakePort port = new FakePort();
    private final CreateTenantUseCase useCase = new CreateTenantUseCase(port);

    @BeforeEach
    void seedPlans() {
        port.plans.put("starter", new PlanVersion(UUID.randomUUID(), "starter", 1, null));
        port.plans.put("scale", new PlanVersion(UUID.randomUUID(), "scale", 1, IsolationTier.SILO));
    }

    @Test
    void should_create_tenant_owner_invitation_seq_and_accounts_in_one_transaction__TLY_101_AC1() {
        var result = useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "op-1", "key-1"));

        assertThat(result.name()).isEqualTo("Acme");
        assertThat(port.insertedTenants).hasSize(1);
        assertThat(port.insertedInvitations).hasSize(1);
        assertThat(port.insertedSeqs).containsExactly(result.id());
        assertThat(port.insertedAccountsCalls).containsExactly(result.id());
    }

    @Test
    void should_return_same_tenant_when_idempotency_key_repeats__TLY_101_AC1() {
        var first = useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "op-1", "key-1"));
        var second = useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "op-1", "key-1"));

        assertThat(second).isEqualTo(first);
        assertThat(port.insertedTenants).as("no second insert on replay").hasSize(1);
    }

    @Test
    void should_replay_stored_response_without_reprovisioning_when_key_was_already_claimed__TLY_101_AC1() {
        // Simulates the race the port's claim/await contract exists to prevent: another (concurrent
        // or earlier) request already claimed this key and has committed its result.
        port.claimedKeys.add("key-raced");
        port.idempotencyStore.put(
                "key-raced",
                "{\"id\":\"11111111-1111-1111-1111-111111111111\",\"name\":\"Other\",\"planCode\":\"starter\","
                        + "\"tier\":\"POOL\",\"status\":\"ACTIVE\"}");

        var result = useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "op-1", "key-raced"));

        assertThat(result.name()).isEqualTo("Other");
        assertThat(port.insertedTenants)
                .as("the loser never provisions anything itself")
                .isEmpty();
    }

    @Test
    void should_default_isolation_tier_to_pool_when_plan_does_not_force_silo__TLY_101_AC2() {
        var result = useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "op-1", "key-2"));

        assertThat(result.tier()).isEqualTo(IsolationTier.POOL);
    }

    @Test
    void should_use_silo_when_plan_limits_specify_it__TLY_101_AC2() {
        var result = useCase.create(new CreateTenantCommand("BigCo", "scale", "owner@bigco.test", "op-1", "key-3"));

        assertThat(result.tier()).isEqualTo(IsolationTier.SILO);
    }

    @Test
    void should_write_tenant_created_audit_entry_with_operator_as_actor__TLY_101_AC4() {
        useCase.create(new CreateTenantCommand("Acme", "starter", "owner@acme.test", "operator-42", "key-4"));

        assertThat(port.auditActions).containsExactly("tenant.created");
        assertThat(port.auditActors).containsExactly("operator-42");
    }

    @Test
    void should_throw_validation_failed_when_plan_code_is_unknown__TLY_101_AC5() {
        assertThatThrownBy(() ->
                        useCase.create(new CreateTenantCommand("Acme", "nope", "owner@acme.test", "op-1", "key-5")))
                .isInstanceOf(ProblemException.class)
                .satisfies(e -> {
                    ProblemException problem = (ProblemException) e;
                    assertThat(problem.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(problem.param()).isEqualTo("plan_code");
                });
    }

    /** ponytail: a hand-written fake beats a mocking framework for one small port interface. */
    private static final class FakePort implements TenantProvisioningPort {
        final Map<String, PlanVersion> plans = new HashMap<>();
        final Map<String, String> idempotencyStore = new HashMap<>();
        final java.util.Set<String> claimedKeys = new java.util.HashSet<>();
        final java.util.List<Tenant> insertedTenants = new java.util.ArrayList<>();
        final java.util.List<UUID> insertedInvitations = new java.util.ArrayList<>();
        final java.util.List<UUID> insertedSeqs = new java.util.ArrayList<>();
        final java.util.List<UUID> insertedAccountsCalls = new java.util.ArrayList<>();
        final java.util.List<String> auditActions = new java.util.ArrayList<>();
        final java.util.List<String> auditActors = new java.util.ArrayList<>();

        @Override
        public Optional<PlanVersion> findLatestPlanVersion(String planCode) {
            return Optional.ofNullable(plans.get(planCode));
        }

        @Override
        public void insertTenant(Tenant tenant) {
            insertedTenants.add(tenant);
        }

        @Override
        public void insertOwnerInvitation(UUID tenantId, UUID invitationId, String ownerEmail) {
            insertedInvitations.add(invitationId);
        }

        @Override
        public void insertInvoiceNumberSeq(UUID tenantId) {
            insertedSeqs.add(tenantId);
        }

        @Override
        public void insertDefaultLedgerAccounts(UUID tenantId) {
            insertedAccountsCalls.add(tenantId);
        }

        @Override
        public void writeAuditLog(UUID id, UUID tenantId, String actor, String action, String payloadJson) {
            auditActions.add(action);
            auditActors.add(actor);
        }

        @Override
        public boolean claimIdempotencyKey(String idempotencyKey) {
            return claimedKeys.add(idempotencyKey);
        }

        @Override
        public Optional<String> awaitIdempotentResponse(String idempotencyKey) {
            return Optional.ofNullable(idempotencyStore.get(idempotencyKey));
        }

        @Override
        public void saveIdempotentResponse(String idempotencyKey, String responseJson) {
            idempotencyStore.put(idempotencyKey, responseJson);
        }
    }
}
