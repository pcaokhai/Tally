package com.tally.core.kernel.security;

/**
 * Membership role, matching {@code contracts/openapi.yaml}'s {@code Me.active_tenant.role} /
 * {@code TeamMember.role} enum. Never add a value here without a matching contract change.
 */
public enum Role {
    OWNER,
    ADMIN,
    DEVELOPER,
    FINANCE,
    VIEWER
}
