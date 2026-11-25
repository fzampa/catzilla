package app.catzilla.api.auth;

import app.catzilla.api.shared.security.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opaque, single-use refresh tokens (ADR-004). The raw value is only ever returned to the client; the database keeps a
 * SHA-256 hash. A token presented a second time means it was copied, so the user's sessions are all revoked.
 */
@Service
class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository tokens;
    private final JwtProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    RefreshTokenService(RefreshTokenRepository tokens, JwtProperties properties, Clock clock) {
        this.tokens = tokens;
        this.properties = properties;
        this.clock = clock;
    }

    /** Starts a new session (token family) for a fresh sign-in. */
    @Transactional
    IssuedRefreshToken issueNewSession(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /** Exchanges a token for a new one in the same session; the presented token can't be used again. */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = tokens.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);
        if (token.isRevokedOrExpired(now)) {
            throw new InvalidRefreshTokenException();
        }
        if (token.wasUsed()) {
            // Reuse of an exchanged token: someone else holds a copy. End every session of this user.
            tokens.revokeAllForUser(token.userId(), now);
            throw new InvalidRefreshTokenException();
        }
        token.markUsed(now);
        return new Rotation(token.userId(), issue(token.userId(), token.familyId()));
    }

    /** Ends the session the token belongs to. Unknown tokens are ignored, so signing out is always safe to repeat. */
    @Transactional
    void revokeSession(String rawToken) {
        tokens.findByTokenHash(hash(rawToken))
                .ifPresent(token -> tokens.revokeFamily(token.familyId(), clock.instant()));
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        tokens.save(new RefreshToken(userId, hash(raw), familyId, now, expiresAt));
        return new IssuedRefreshToken(raw, expiresAt);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }

    record IssuedRefreshToken(String value, Instant expiresAt) {}

    record Rotation(UUID userId, IssuedRefreshToken next) {}
}
