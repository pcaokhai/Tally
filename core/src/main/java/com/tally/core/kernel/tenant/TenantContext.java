package com.tally.core.kernel.tenant;

import java.util.UUID;

/**
 * The resolved tenant for the current request (ADR-003). Carries only the tenant id: nothing else
 * is trusted from the credential for isolation purposes.
 */
public record TenantContext(UUID tenantId) {

    public TenantContext {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
    }
}
