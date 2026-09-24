package com.tally.core.kernel.security;

import java.util.UUID;

/** One tenant membership, as carried by the token's {@code memberships} claim. */
public record Membership(UUID tenantId, String tenantName, Role role) {

    public Membership {
        if (tenantId == null || tenantName == null || tenantName.isBlank() || role == null) {
            throw new IllegalArgumentException("tenantId, tenantName and role are required");
        }
    }
}
