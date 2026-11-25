package app.catzilla.api.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * @param accessToken the JWT to send as {@code Authorization: Bearer <accessToken>}
 * @param expiresIn seconds until the access token expires
 * @param refreshToken present only with {@link TokenDelivery#BODY}; with cookies it is never visible to JavaScript
 * @param refreshTokenExpiresIn seconds until the refresh token expires
 */
record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable String refreshToken,
        long refreshTokenExpiresIn) {}
