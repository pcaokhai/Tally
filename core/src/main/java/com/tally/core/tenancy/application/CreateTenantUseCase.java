package com.tally.core.tenancy.application;

import com.tally.core.kernel.problem.ProblemException;
import com.tally.core.kernel.tenant.TenantContext;
import com.tally.core.kernel.tenant.TenantContextHolder;
import com.tally.core.tenancy.application.port.out.TenantProvisioningPort;
import com.tally.core.tenancy.domain.PlanVersion;
import com.tally.core.tenancy.domain.Tenant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Creates a tenant, its owner invitation, invoice number sequence and default ledger accounts in
 * one transaction, idempotently (docs/06 TLY-101 AC1-AC5).
 */
@Service
public class CreateTenantUseCase {

    private final TenantProvisioningPort port;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CreateTenantUseCase(TenantProvisioningPort port) {
        this.port = port;
    }

    @Transactional
    public CreateTenantResult create(CreateTenantCommand command) {
        var replay = port.findIdempotentResponse(command.idempotencyKey());
        if (replay.isPresent()) {
            return objectMapper.readValue(replay.get(), CreateTenantResult.class);
        }

        PlanVersion plan = port.findLatestPlanVersion(command.planCode())
                .orElseThrow(() -> ProblemException.validationFailed("plan_code", "unknown plan_code"));

        // ponytail: UUID.randomUUID() (v4) — docs/05 wants UUIDv7, but no generator dependency
        // exists yet in this repo; add one (Ruling) when a story actually needs sortable ids.
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant(tenantId, command.name(), plan.id(), plan.isolationTierFor(), Tenant.STATUS_ACTIVE);

        CreateTenantResult result = TenantContextHolder.runWith(new TenantContext(tenantId), () -> {
            port.insertTenant(tenant);
            port.insertOwnerInvitation(tenantId, UUID.randomUUID(), command.ownerEmail());
            port.insertInvoiceNumberSeq(tenantId);
            port.insertDefaultLedgerAccounts(tenantId);
            CreateTenantResult r = new CreateTenantResult(
                    tenantId, tenant.name(), plan.planCode(), tenant.isolationTier(), tenant.status());
            port.writeAuditLog(
                    UUID.randomUUID(),
                    tenantId,
                    command.operatorActor(),
                    "tenant.created",
                    objectMapper.writeValueAsString(r));
            return r;
        });

        port.saveIdempotentResponse(command.idempotencyKey(), objectMapper.writeValueAsString(result));
        return result;
    }
}
