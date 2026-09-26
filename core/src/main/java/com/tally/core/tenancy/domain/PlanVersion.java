package com.tally.core.tenancy.domain;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * What Tally charges a tenant (ADR-028): platform data, owned by operators, immutable once
 * created. {@code forcedTier} is non-null only when the plan's limits require SILO (TLY-101 AC2).
 */
public record PlanVersion(
        UUID id, String planCode, int version, @Nullable IsolationTier forcedTier) {

    public IsolationTier isolationTierFor() {
        return forcedTier == null ? IsolationTier.POOL : forcedTier;
    }
}
