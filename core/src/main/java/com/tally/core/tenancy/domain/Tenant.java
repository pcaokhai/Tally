package com.tally.core.tenancy.domain;

import java.util.UUID;

/** A Tally customer organization (docs/06 TLY-101). The tenant registry root; no {@code tenant_id}
 * column of its own and no RLS — it *is* the tenant. */
public record Tenant(UUID id, String name, UUID planVersionId, IsolationTier isolationTier, String status) {

    public static final String STATUS_ACTIVE = "ACTIVE";
}
