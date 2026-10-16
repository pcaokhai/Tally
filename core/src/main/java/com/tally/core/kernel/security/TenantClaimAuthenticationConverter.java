package com.tally.core.kernel.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

/**
 * Builds the {@link AuthenticatedUser} straight from the (already signature/issuer/audience/expiry
 * validated) JWT's claims — {@code sub}, {@code email}, {@code tenant_id} and a {@code memberships}
 * array Keycloak populates via a protocol mapper: {@code [{tenant_id, tenant_name, role}]}. A
 * {@code tenant_id} claim absent from that array is rejected the same way an invalid signature
 * would be (TLY-103 AC3): as an {@link InvalidBearerTokenException}, so Spring Security's normal
 * 401 entry point handles both uniformly.
 */
public final class TenantClaimAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = requireUuid(jwt, "sub");
        String email = requireString(jwt, "email");
        UUID activeTenantId = requireUuid(jwt, "tenant_id");
        List<Membership> memberships = readMemberships(jwt);

        Membership active = memberships.stream()
                .filter(m -> m.tenantId().equals(activeTenantId))
                .findFirst()
                .orElseThrow(() ->
                        new InvalidBearerTokenException("tenant_id claim is not among the token's active memberships"));

        AuthenticatedUser user = new AuthenticatedUser(
                userId, email, active.tenantId(), active.tenantName(), active.role(), memberships);
        return new AuthenticatedUserAuthenticationToken(user, jwt);
    }

    private static List<Membership> readMemberships(Jwt jwt) {
        Object raw = jwt.getClaim("memberships");
        if (!(raw instanceof List<?> list)) {
            throw new InvalidBearerTokenException("memberships claim is missing or not an array");
        }
        List<Membership> memberships = new ArrayList<>(list.size());
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                throw new InvalidBearerTokenException("memberships claim entry is malformed");
            }
            try {
                memberships.add(new Membership(
                        UUID.fromString(String.valueOf(map.get("tenant_id"))),
                        String.valueOf(map.get("tenant_name")),
                        Role.valueOf(String.valueOf(map.get("role")))));
            } catch (RuntimeException malformed) {
                throw new InvalidBearerTokenException("memberships claim entry is malformed");
            }
        }
        return List.copyOf(memberships);
    }

    private static UUID requireUuid(Jwt jwt, String claim) {
        try {
            return UUID.fromString(requireString(jwt, claim));
        } catch (IllegalArgumentException notAUuid) {
            throw new InvalidBearerTokenException(claim + " claim is not a valid id");
        }
    }

    private static String requireString(Jwt jwt, String claim) {
        String value = jwt.getClaimAsString(claim);
        if (value == null || value.isBlank()) {
            throw new InvalidBearerTokenException(claim + " claim is required");
        }
        return value;
    }
}
