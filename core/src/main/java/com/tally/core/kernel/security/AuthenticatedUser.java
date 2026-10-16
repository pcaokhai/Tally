package com.tally.core.kernel.security;

import java.util.List;
import java.util.UUID;

/**
 * The caller identity resolved from a validated bearer JWT (TLY-103 AC2): the user, the active
 * tenant (from the {@code tenant_id} claim) with the caller's role in it, and every membership the
 * token carries. Nothing here is read from a database — see docs/plans/TLY-103.md's Ruling.
 */
public record AuthenticatedUser(
        UUID userId,
        String email,
        UUID activeTenantId,
        String activeTenantName,
        Role activeRole,
        List<Membership> memberships) {

    public AuthenticatedUser {
        if (userId == null
                || email == null
                || email.isBlank()
                || activeTenantId == null
                || activeTenantName == null
                || activeRole == null) {
            throw new IllegalArgumentException(
                    "userId, email, activeTenantId, activeTenantName and activeRole are required");
        }
        memberships = List.copyOf(memberships);
    }
}
