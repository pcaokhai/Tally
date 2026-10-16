package com.tally.core.kernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class TenantClaimAuthenticationConverterTest {

    private final TenantClaimAuthenticationConverter converter = new TenantClaimAuthenticationConverter();

    @Test
    void should_resolve_active_tenant_and_memberships_when_tenant_id_claim_is_an_active_membership__TLY_103_AC2() {
        UUID userId = UUID.randomUUID();
        UUID activeTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();
        Jwt jwt = jwtWith(
                userId,
                activeTenant,
                List.of(
                        Map.of("tenant_id", activeTenant.toString(), "tenant_name", "Acme", "role", "ADMIN"),
                        Map.of("tenant_id", otherTenant.toString(), "tenant_name", "Beta", "role", "VIEWER")));

        AuthenticatedUserAuthenticationToken token = (AuthenticatedUserAuthenticationToken) converter.convert(jwt);
        AuthenticatedUser user = token.getPrincipal();

        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.activeTenantId()).isEqualTo(activeTenant);
        assertThat(user.activeTenantName()).isEqualTo("Acme");
        assertThat(user.activeRole()).isEqualTo(Role.ADMIN);
        assertThat(user.memberships()).hasSize(2);
    }

    @Test
    void should_reject_when_tenant_id_claim_is_not_among_memberships__TLY_103_AC3() {
        UUID userId = UUID.randomUUID();
        UUID activeTenant = UUID.randomUUID();
        UUID onlyMembership = UUID.randomUUID();
        Jwt jwt = jwtWith(
                userId,
                activeTenant,
                List.of(Map.of("tenant_id", onlyMembership.toString(), "tenant_name", "Beta", "role", "VIEWER")));

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void should_reject_when_memberships_claim_is_missing__TLY_103_AC3() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("email", "user@example.invalid")
                .claim("tenant_id", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwtWith(UUID userId, UUID tenantId, List<Map<String, Object>> memberships) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .claim("email", "user@example.invalid")
                .claim("tenant_id", tenantId.toString())
                .claim("memberships", memberships)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
