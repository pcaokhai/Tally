package com.tally.core.kernel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Proves the signature half of TLY-103 AC1: {@code NimbusJwtDecoder} (what {@code SecurityConfig}
 * wires) rejects a token signed by a key other than the one it was configured to trust.
 */
class JwtSignatureVerificationTest {

    @Test
    void should_reject_token_when_signature_does_not_match_the_trusted_key__TLY_103_AC1()
            throws NoSuchAlgorithmException, JOSEException {
        var trustedKeyPair = KeyPairGenerator.getInstance("RSA");
        trustedKeyPair.initialize(2048);
        var trusted = trustedKeyPair.generateKeyPair();

        var attackerKeyPair = KeyPairGenerator.getInstance("RSA");
        attackerKeyPair.initialize(2048);
        var attacker = attackerKeyPair.generateKeyPair();

        JwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) trusted.getPublic())
                .build();
        String tokenSignedByAttacker = sign((RSAPrivateKey) attacker.getPrivate());

        assertThatThrownBy(() -> decoder.decode(tokenSignedByAttacker)).isInstanceOf(JwtException.class);

        String tokenSignedByTrustedKey = sign((RSAPrivateKey) trusted.getPrivate());
        assertThat(decoder.decode(tokenSignedByTrustedKey).getSubject()).isEqualTo("user");
    }

    private static String sign(RSAPrivateKey key) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("user")
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
