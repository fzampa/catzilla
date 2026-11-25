package app.catzilla.api.shared.security;

import com.nimbusds.jose.jwk.RSAKey;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Issues access tokens. They identify the user ({@code sub}) and nothing else: Homes and roles are checked against
 * the database on each request (ADR-003), and personal data stays out because a JWT is readable by anyone holding it.
 */
@Component
public class AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final String keyId;
    private final Clock clock;

    AccessTokenIssuer(JwtEncoder encoder, JwtProperties properties, RSAKey jwtSigningKey, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.keyId = jwtSigningKey.getKeyID();
        this.clock = clock;
    }

    public AccessToken issue(UUID userId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(keyId)
                .type("JWT")
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }

    /** A signed access token and the moment it expires. */
    public record AccessToken(String value, Instant expiresAt) {}
}
