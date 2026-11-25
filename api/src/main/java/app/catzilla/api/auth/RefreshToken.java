package app.catzilla.api.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One refresh token, stored only as a hash. Each token can be exchanged once; the replacement belongs to the same
 * {@code familyId} (one sign-in session).
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private UUID familyId;

    @Column(nullable = false)
    private Instant expiresAt;

    private @Nullable Instant usedAt;

    private @Nullable Instant revokedAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant createdAt, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    UUID userId() {
        return userId;
    }

    UUID familyId() {
        return familyId;
    }

    boolean wasUsed() {
        return usedAt != null;
    }

    boolean isRevokedOrExpired(Instant now) {
        return revokedAt != null || !now.isBefore(expiresAt);
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }
}
