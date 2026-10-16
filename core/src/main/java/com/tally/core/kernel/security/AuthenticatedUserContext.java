package com.tally.core.kernel.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Thin accessor over {@link SecurityContextHolder} for the {@link AuthenticatedUser} that {@link
 * TenantClaimAuthenticationConverter} placed there. Spring Security already scopes the security
 * context per request (cleared by its filter chain), so this needs no {@code ScopedValue} of its
 * own — unlike {@code TenantContextHolder} (TLY-102), which exists precisely because there is no
 * servlet-managed request context for it to piggyback on.
 */
public final class AuthenticatedUserContext {

    private AuthenticatedUserContext() {}

    public static Optional<AuthenticatedUser> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof AuthenticatedUserAuthenticationToken token
                ? Optional.of(token.getPrincipal())
                : Optional.empty();
    }

    public static AuthenticatedUser require() {
        return current().orElseThrow(() -> new IllegalStateException("no AuthenticatedUser bound on this request"));
    }
}
