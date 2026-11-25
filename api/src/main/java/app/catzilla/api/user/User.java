package app.catzilla.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Everybody who uses the app. Roles in Homes are separate memberships (ADR-003). */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt;

    protected User() {}

    public User(UUID id, String email, String displayName, Instant createdAt) {
        this.id = id;
        this.email = EmailAddresses.normalize(email);
        this.displayName = displayName.strip();
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
