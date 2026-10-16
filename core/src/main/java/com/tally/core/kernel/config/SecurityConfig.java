package com.tally.core.kernel.config;

import com.tally.core.kernel.security.GlobalSecurityExceptionHandler;
import com.tally.core.kernel.security.JwtValidatorFactory;
import com.tally.core.kernel.security.PermissionEnforcementInterceptor;
import com.tally.core.kernel.security.ProblemJsonAuthenticationEntryPoint;
import com.tally.core.kernel.security.TenantClaimAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Resource server for {@code /v1/**} (TLY-103, ADR-007 realm {@code tally-tenants}). {@code /ops/v1}
 * keeps relying on {@code PortScopeFilter} alone until a dedicated operator-realm story adds its own
 * {@code SecurityFilterChain} (ADR-022) — recorded as a Ruling in docs/plans/TLY-103.md.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain tenantApiSecurityFilterChain(
            HttpSecurity http, @Lazy JwtDecoder jwtDecoder, TenantClaimAuthenticationConverter converter)
            throws Exception {
        http.securityMatcher("/v1/**")
                .csrf(csrf ->
                        csrf.disable()) // ponytail: token API, no cookies/session — CSRF only matters for cookie auth.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(converter)))
                .exceptionHandling(
                        exceptions -> exceptions.authenticationEntryPoint(new ProblemJsonAuthenticationEntryPoint()));
        return http.build();
    }

    // Lazy: NimbusJwtDecoder.withIssuerLocation(...).build() performs OIDC discovery over the
    // network eagerly. Without @Lazy, every context that builds this SecurityFilterChain (plain
    // `./gradlew test`, no Keycloak running) would fail to start; the real Keycloak realm is only
    // reachable when `make up`/integrationTest actually runs the stack.
    @Bean
    @Lazy
    JwtDecoder jwtDecoder(
            @Value("${tally.security.jwt.issuer-uri}") String issuerUri,
            @Value("${tally.security.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(JwtValidatorFactory.create(issuerUri, audience));
        return decoder;
    }

    @Bean
    TenantClaimAuthenticationConverter tenantClaimAuthenticationConverter() {
        return new TenantClaimAuthenticationConverter();
    }

    @Bean
    GlobalSecurityExceptionHandler globalSecurityExceptionHandler() {
        return new GlobalSecurityExceptionHandler();
    }

    @Bean
    WebMvcConfigurer permissionEnforcementConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new PermissionEnforcementInterceptor());
            }
        };
    }
}
