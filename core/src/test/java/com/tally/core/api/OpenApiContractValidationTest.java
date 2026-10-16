package com.tally.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import com.atlassian.oai.validator.springmvc.InvalidResponseException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.tally.core.support.OpenApiValidationConfig;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC5: requests and responses are validated against {@code contracts/openapi.yaml} in tests. The
 * first test is the positive control — without it, a validator that silently failed to load the spec
 * would still let the rejection tests pass for the wrong reason.
 */
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Import({
    OpenApiValidationConfig.class,
    OpenApiContractValidationTest.OffContractController.class,
    OpenApiContractValidationTest.TestJwtDecoderConfig.class
})
class OpenApiContractValidationTest {

    private final MockMvc mockMvc;

    @Autowired
    OpenApiContractValidationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void should_accept_the_me_response_when_it_matches_the_contract__TLY_004_AC5() throws Exception {
        var result = mockMvc.perform(get("/v1/me").header("Authorization", "Bearer " + bearerToken()))
                .andReturn();

        assertThat(result.getResolvedException()).isNull();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void should_reject_the_response_when_it_violates_the_contract__TLY_004_AC5() throws Exception {
        var result = mockMvc.perform(get(OffContractController.PATH).header("Authorization", "Bearer " + bearerToken()))
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(InvalidResponseException.class)
                .hasMessageContaining("required property 'has_more' not found");
    }

    @Test
    void should_reject_the_request_when_a_query_parameter_violates_the_contract__TLY_004_AC5() throws Exception {
        // The validator reads the raw query string, so the parameter goes in the URI, not .param().
        var result = mockMvc.perform(get(OffContractController.PATH + "?limit=not-a-number")
                        .header("Authorization", "Bearer " + bearerToken()))
                .andReturn();

        assertThat(result.getResolvedException())
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("validation.request.parameter.schema.type")
                .hasMessageContaining("string found, integer expected");
    }

    /**
     * Everything under {@code /v1/**} requires a valid bearer token since TLY-103; a locally-signed
     * one (verified by {@link TestJwtDecoderConfig}'s decoder, not a live Keycloak) is enough to
     * reach the contract validator this test actually exercises.
     */
    private static String bearerToken() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "contract-test@example.invalid")
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

    /** Overrides the real (network-fetching) {@code JwtDecoder} with a locally-keyed one for tests. */
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
            } catch (java.security.NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
        }
    }

    /**
     * Serves a contracted path that has no production handler yet, with a body that omits the required
     * {@code has_more}. Nested so it stays out of every other test's context.
     */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class OffContractController {

        static final String PATH = "/v1/attention-items";

        @GetMapping(PATH)
        Map<String, Object> listAttentionItems() {
            return Map.of("data", List.of());
        }
    }
}
