package app.catzilla.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** The email is trimmed; the password is taken exactly as typed. */
record LoginRequest(
        @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(max = 128) String password,
        @Nullable TokenDelivery tokenDelivery) {

    LoginRequest {
        email = email == null ? null : email.strip();
    }
}
