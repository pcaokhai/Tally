package com.tally.core.kernel.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Proves the issuer/audience/expiry checks TLY-103 AC1 requires (signature is NimbusJwtDecoder's
 * own responsibility — proved separately in SecurityConfig's wiring, not re-implemented here).
 */
class JwtValidatorFactoryTest {

    private static final String ISSUER = "https://keycloak.example/realms/tally-tenants";
    private static final String AUDIENCE = "tally-api";
    private final OAuth2TokenValidator<Jwt> validator = JwtValidatorFactory.create(ISSUER, AUDIENCE);

    @Test
    void should_accept_token_when_issuer_audience_and_expiry_are_valid__TLY_103_AC1() {
        OAuth2TokenValidatorResult result =
                validator.validate(jwt(ISSUER, AUDIENCE, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void should_reject_token_when_issuer_does_not_match__TLY_103_AC1() {
        OAuth2TokenValidatorResult result = validator.validate(
                jwt("https://evil.example/realms/other", AUDIENCE, Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void should_reject_token_when_audience_does_not_match__TLY_103_AC1() {
        OAuth2TokenValidatorResult result =
                validator.validate(jwt(ISSUER, "some-other-api", Instant.now().plusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void should_reject_token_when_it_is_expired__TLY_103_AC1() {
        OAuth2TokenValidatorResult result =
                validator.validate(jwt(ISSUER, AUDIENCE, Instant.now().minusSeconds(60)));

        assertThat(result.hasErrors()).isTrue();
    }

    private static Jwt jwt(String issuer, String audience, Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuer(issuer)
                .audience(java.util.List.of(audience))
                .claim("sub", "user")
                .issuedAt(Instant.now().minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }
}
