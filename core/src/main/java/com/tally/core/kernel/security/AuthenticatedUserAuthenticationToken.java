package com.tally.core.kernel.security;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated principal is an {@link AuthenticatedUser}, not a raw {@link Jwt}. */
public final class AuthenticatedUserAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;
    private final Jwt credentials;

    public AuthenticatedUserAuthenticationToken(AuthenticatedUser principal, Jwt credentials) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.activeRole())));
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }

    @Override
    public Jwt getCredentials() {
        return credentials;
    }
}
