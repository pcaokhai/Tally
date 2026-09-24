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

    /** Empty when this idempotency key has not been used before. */
    Optional<String> findIdempotentResponse(String idempotencyKey);

    void saveIdempotentResponse(String idempotencyKey, String responseJson);
}
