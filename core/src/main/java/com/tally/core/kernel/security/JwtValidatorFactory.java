package com.tally.core.kernel.security;

import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/**
 * The issuer + audience + expiry checks {@code SecurityConfig} wires into its {@code JwtDecoder}
 * (TLY-103 AC1), split out so it is testable without a live issuer discovery endpoint. Signature
 * verification itself is {@code NimbusJwtDecoder}'s own job (it fails closed for any signature that
 * doesn't match the resolved JWK), not re-implemented here.
 */
public final class JwtValidatorFactory {

    private JwtValidatorFactory() {}

    public static OAuth2TokenValidator<Jwt> create(String issuerUri, String requiredAudience) {
        return new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuerUri),
                new AudienceValidator(requiredAudience)));
    }
}
