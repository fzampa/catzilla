package app.catzilla.api.user;

import java.time.Instant;
import java.util.UUID;

/** The signed-in user's profile. */
public record UserResponse(UUID id, String email, String displayName, boolean emailVerified, Instant createdAt) {

    static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.isEmailVerified(), user.getCreatedAt());
    }
}
