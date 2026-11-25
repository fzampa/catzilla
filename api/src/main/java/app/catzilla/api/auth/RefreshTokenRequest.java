package app.catzilla.api.auth;

import org.jspecify.annotations.Nullable;

/** Used by refresh and logout. Native apps send the token here; the web app sends the cookie instead. */
record RefreshTokenRequest(@Nullable String refreshToken) {}
