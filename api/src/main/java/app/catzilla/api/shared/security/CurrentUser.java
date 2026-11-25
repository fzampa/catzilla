package app.catzilla.api.shared.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** The authenticated user of a request: the access token's {@code sub}, already verified by Spring Security. */
public final class CurrentUser {

    private CurrentUser() {}

    /** Use with a controller parameter annotated {@code @AuthenticationPrincipal Jwt}. */
    public static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
