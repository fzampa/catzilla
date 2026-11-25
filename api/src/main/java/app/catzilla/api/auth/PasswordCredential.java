package app.catzilla.api.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A user's password, as a salted Argon2id hash in Spring Security's {@code {id}...} format. */
@Entity
@Table(name = "password_credentials")
class PasswordCredential {

    @Id
    private UUID userId;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private Instant updatedAt;

    protected PasswordCredential() {}

    PasswordCredential(UUID userId, String passwordHash, Instant updatedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.updatedAt = updatedAt;
    }

    String passwordHash() {
        return passwordHash;
    }

    void replaceHash(String newHash, Instant now) {
        this.passwordHash = newHash;
        this.updatedAt = now;
    }
}
