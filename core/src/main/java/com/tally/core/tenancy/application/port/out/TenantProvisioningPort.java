package com.tally.core.tenancy.application.port.out;

import com.tally.core.tenancy.domain.PlanVersion;
import com.tally.core.tenancy.domain.Tenant;
import java.util.Optional;
import java.util.UUID;

/**
 * Everything {@code CreateTenantUseCase} needs from storage, as one port (ponytail: five tiny
 * single-method ports would not change a single caller's behavior for this story's size).
 */
public interface TenantProvisioningPort {

    Optional<PlanVersion> findLatestPlanVersion(String planCode);

    void insertTenant(Tenant tenant);

    void insertOwnerInvitation(UUID tenantId, UUID invitationId, String ownerEmail);

    void insertInvoiceNumberSeq(UUID tenantId);

    void insertDefaultLedgerAccounts(UUID tenantId);

    void writeAuditLog(UUID id, UUID tenantId, String actor, String action, String payloadJson);

    /**
     * Claims {@code idempotencyKey} for this transaction: {@code true} means nobody else holds it
     * yet and this call may proceed to provision; {@code false} means another transaction already
     * claimed it (concurrently or previously) and the caller must call {@link
     * #awaitIdempotentResponse(String)} instead of doing the work again.
     */
    boolean claimIdempotencyKey(String idempotencyKey);

    /**
     * Blocks (via a row lock) until the transaction that {@link #claimIdempotencyKey(String)}
     * returns {@code true} for the same key has committed or rolled back, then returns its stored
     * response — empty if that transaction rolled back (the row never got a final response), in
     * which case the caller should retry the claim.
     */
    Optional<String> awaitIdempotentResponse(String idempotencyKey);

    void saveIdempotentResponse(String idempotencyKey, String responseJson);
}
