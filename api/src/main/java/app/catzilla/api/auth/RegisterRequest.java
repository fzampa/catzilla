package app.catzilla.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * Password rules follow NIST SP 800-63B: a minimum length and no composition rules (ADR-004). Email and display name
 * are trimmed before validation (phone keyboards often add a trailing space); the password is taken exactly as typed.
 */
record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotNull @Size(min = 10, max = 128) String password,
        @NotBlank @Size(max = 100) String displayName,
        @Nullable TokenDelivery tokenDelivery) {

    RegisterRequest {
        email = email == null ? null : email.strip();
        displayName = displayName == null ? null : displayName.strip();
    }
}
