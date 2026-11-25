package app.catzilla.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Issuing and verifying access tokens, without Spring or a database. */
class JwtTokensTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final JwtProperties PROPERTIES =
            new JwtProperties("catzilla-api", "catzilla-api", Duration.ofMinutes(15), Duration.ofDays(30), null);

    private final JwtConfiguration configuration = new JwtConfiguration();
    private final RSAKey key = JwtKeys.generate();

    @Test
    void issuedTokenVerifiesAndCarriesOnlyTheUserIdentity() throws Exception {
        UUID userId = UUID.randomUUID();
        AccessTokenIssuer.AccessToken token = issuer(key, PROPERTIES, at(NOW)).issue(userId);

        Jwt jwt = decoder(key, at(NOW.plusSeconds(60))).decode(token.value());

        assertThat(CurrentUser.id(jwt)).isEqualTo(userId);
        // Read as a string: Jwt.getIssuer() expects a URL, and the issuer is a plain name.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("catzilla-api");
        assertThat(jwt.getAudience()).containsExactly("catzilla-api");
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(token.expiresAt()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getHeaders()).containsEntry("kid", key.getKeyID()).containsEntry("typ", "JWT");
        assertThat(jwt.getClaims()).containsOnlyKeys("iss", "aud", "sub", "iat", "exp", "jti");
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String token = issuer(key, PROPERTIES, at(NOW)).issue(UUID.randomUUID()).value();
        String[] parts = token.split("\\.");
        String otherClaims = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        ("{\"sub\":\"" + UUID.randomUUID() + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + otherClaims + "." + parts[2];

        assertThatThrownBy(() -> decoder(key, at(NOW)).decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() throws Exception {
        String token = issuer(JwtKeys.generate(), PROPERTIES, at(NOW))
                .issue(UUID.randomUUID())
                .value();

        assertThatThrownBy(() -> decoder(key, at(NOW)).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void acceptsSmallClockSkewButRejectsExpiredToken() throws Exception {
        String token = issuer(key, PROPERTIES, at(NOW)).issue(UUID.randomUUID()).value();
        Instant expiry = NOW.plus(Duration.ofMinutes(15));

        assertThat(decoder(key, at(expiry.plusSeconds(30))).decode(token)).isNotNull();
        assertThatThrownBy(() -> decoder(key, at(expiry.plusSeconds(61))).decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rejectsWrongAudienceOrIssuer() throws Exception {
        JwtProperties otherAudience =
                new JwtProperties("catzilla-api", "other-app", Duration.ofMinutes(15), Duration.ofDays(30), null);
        JwtProperties otherIssuer =
                new JwtProperties("someone-else", "catzilla-api", Duration.ofMinutes(15), Duration.ofDays(30), null);
        String wrongAudience =
                issuer(key, otherAudience, at(NOW)).issue(UUID.randomUUID()).value();
        String wrongIssuer =
                issuer(key, otherIssuer, at(NOW)).issue(UUID.randomUUID()).value();

        assertThatThrownBy(() -> decoder(key, at(NOW)).decode(wrongAudience)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> decoder(key, at(NOW)).decode(wrongIssuer)).isInstanceOf(JwtException.class);
    }

    @Test
    void loadsConfiguredKeyWithStableKeyId() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String base64 = Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());

        RSAKey loaded = JwtKeys.fromBase64Pkcs8(base64);
        RSAKey loadedAgain = JwtKeys.fromBase64Pkcs8(base64);

        assertThat(loaded.isPrivate()).isTrue();
        assertThat(loaded.getKeyID()).isNotBlank().isEqualTo(loadedAgain.getKeyID());
        String token =
                issuer(loaded, PROPERTIES, at(NOW)).issue(UUID.randomUUID()).value();
        assertThat(decoder(loadedAgain, at(NOW)).decode(token)).isNotNull();
    }

    @Test
    void rejectsInvalidOrWeakConfiguredKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        String weak = Base64.getEncoder()
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());

        assertThatThrownBy(() -> JwtKeys.fromBase64Pkcs8("not a key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY");
        assertThatThrownBy(() -> JwtKeys.fromBase64Pkcs8(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_PRIVATE_KEY");
    }

    private AccessTokenIssuer issuer(RSAKey signingKey, JwtProperties properties, Clock clock) {
        return new AccessTokenIssuer(configuration.jwtEncoder(signingKey), properties, signingKey, clock);
    }

    private JwtDecoder decoder(RSAKey verificationKey, Clock clock) throws Exception {
        return configuration.jwtDecoder(verificationKey, PROPERTIES, clock);
    }

    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
