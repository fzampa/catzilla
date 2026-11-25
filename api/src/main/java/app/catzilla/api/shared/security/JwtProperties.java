package app.catzilla.api.shared.security;

import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the tokens this API issues (ADR-004).
 *
 * @param issuer value of the {@code iss} claim, checked on every request
 * @param audience value of the {@code aud} claim, checked on every request
 * @param accessTokenTtl lifetime of an access token
 * @param refreshTokenTtl lifetime of a refresh token, renewed on every use
 * @param privateKey RSA private key as base64-encoded PKCS#8 DER (one line, see api/.env.example). When empty, a
 *     temporary key is generated at startup.
 */
@ConfigurationProperties("catzilla.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        @Nullable String privateKey) {}
