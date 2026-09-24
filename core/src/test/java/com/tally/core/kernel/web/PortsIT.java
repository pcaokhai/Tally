package com.tally.core.kernel.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tally.core.support.Http;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * AC4: three listening ports with disjoint scopes. Fixed ports would be flaky on a shared machine,
 * so the three ports move to free ones and only the routing behaviour is asserted here;
 * {@link com.tally.core.kernel.config.ServerPortDefaultsTest} pins the documented defaults.
 *
 * <p>Since TLY-103, {@code /v1/me} requires a valid bearer token; this test overrides {@code
 * JwtDecoder} with a locally-keyed one (no live Keycloak) purely so the tenant-port case can prove
 * routing succeeds, not to re-test authentication itself (covered in kernel.security tests).
 */
@Tag("integration")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
@Import(PortsIT.TestJwtDecoderConfig.class)
class PortsIT {

    private static final int API_PORT = Http.freePort();
    private static final int OPS_PORT = Http.freePort();
    private static final int MANAGEMENT_PORT = Http.freePort();

    @DynamicPropertySource
    static void ports(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> API_PORT);
        registry.add("tally.ops.port", () -> OPS_PORT);
        registry.add("management.server.port", () -> MANAGEMENT_PORT);
    }

    @Test
    void should_serve_the_actuator_health_probes_when_called_on_the_management_port__TLY_004_AC4() {
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health")).isEqualTo(200);
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health/liveness")).isEqualTo(200);
        assertThat(Http.status(MANAGEMENT_PORT, "/actuator/health/readiness")).isEqualTo(200);
    }

    @Test
    void should_serve_the_tenant_api_when_called_on_the_tenant_port__TLY_004_AC4() {
        assertThat(Http.status(API_PORT, "/v1/me", authorizationHeader())).isEqualTo(200);
    }

    private static Map<String, String> authorizationHeader() {
        try {
            return Map.of("Authorization", "Bearer " + bearerToken());
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String bearerToken() throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "ports-it@example.invalid")
                .claim("tenant_id", TestJwtDecoderConfig.TENANT_ID.toString())
                .claim(
                        "memberships",
                        List.of(Map.of(
                                "tenant_id", TestJwtDecoderConfig.TENANT_ID.toString(),
                                "tenant_name", "Acme",
                                "role", "OWNER")))
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner((RSAPrivateKey) TestJwtDecoderConfig.KEY_PAIR.getPrivate()));
        return jwt.serialize();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestJwtDecoderConfig {

        static final UUID TENANT_ID = UUID.randomUUID();
        static final KeyPair KEY_PAIR = generateKeyPair();

        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic())
                    .build();
        }

        private static KeyPair generateKeyPair() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                return generator.generateKeyPair();
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    @Test
    void should_answer_not_found_when_the_tenant_api_is_called_on_the_operator_port__TLY_004_AC4() {
        assertThat(Http.status(OPS_PORT, "/v1/me")).isEqualTo(404);
    }

    @Test
    void should_answer_not_found_when_an_operator_path_is_called_on_the_tenant_port__TLY_004_AC4() {
        assertThat(Http.status(API_PORT, "/ops/v1/health/services")).isEqualTo(404);
    }

    /**
     * Was a 404 before TLY-103. Boot's {@code ManagementWebSecurityAutoConfiguration} now auto-
     * secures the (separate child-context) management port the instant Spring Security is on the
     * classpath, and that filter runs ahead of the dispatcher's "no such mapping" 404 — it cannot be
     * excluded from just the child context (a class exclude on {@code @SpringBootApplication} and a
     * {@code spring.autoconfigure.exclude} property were both tried; neither reaches the child
     * context here). The port isolation this AC actually cares about — the management port never
     * serves tenant-API content — still holds: 401 refuses the request exactly as firmly as 404 did,
     * it just does so before touching routing. See the Ruling in docs/plans/TLY-103.md.
     */
    @Test
    void should_refuse_the_tenant_api_when_called_on_the_management_port__TLY_004_AC4() {
        assertThat(Http.status(MANAGEMENT_PORT, "/v1/me")).isEqualTo(401);
    }
}
