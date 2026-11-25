package app.catzilla.api.auth;

import app.catzilla.api.auth.AuthService.SignedIn;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-up, sign-in, token refresh and sign-out (ADR-004). All endpoints here are public. */
@RestController
@RequestMapping("/v1/auth")
class AuthController {

    static final String REFRESH_COOKIE = "catzilla_refresh";
    private static final String AUTH_PATH = "/v1/auth";

    private final AuthService auth;
    private final Clock clock;

    AuthController(AuthService auth, Clock clock) {
        this.auth = auth;
        this.clock = clock;
    }

    @PostMapping("/register")
    ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        SignedIn signedIn = auth.register(body.email(), body.password(), body.displayName());
        return respond(HttpStatus.CREATED, signedIn, deliveryOf(body.tokenDelivery()), request);
    }

    @PostMapping("/login")
    ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        SignedIn signedIn = auth.login(body.email(), body.password(), request.getRemoteAddr());
        return respond(HttpStatus.OK, signedIn, deliveryOf(body.tokenDelivery()), request);
    }

    /** The new refresh token goes back the way the old one came: in the body or in the cookie. */
    @PostMapping("/refresh")
    ResponseEntity<TokenResponse> refresh(
            @RequestBody(required = false) @Nullable RefreshTokenRequest body,
            @CookieValue(name = REFRESH_COOKIE, required = false) @Nullable String cookie,
            HttpServletRequest request) {
        String fromBody = body == null ? null : body.refreshToken();
        String presented = fromBody != null ? fromBody : cookie;
        if (presented == null || presented.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        TokenDelivery delivery = fromBody != null ? TokenDelivery.BODY : TokenDelivery.COOKIE;
        return respond(HttpStatus.OK, auth.refresh(presented), delivery, request);
    }

    /** Ends the session of the presented refresh token and clears the cookie. Always 204, even if already signed out. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout(
            @RequestBody(required = false) @Nullable RefreshTokenRequest body,
            @CookieValue(name = REFRESH_COOKIE, required = false) @Nullable String cookie,
            HttpServletRequest request) {
        String fromBody = body == null ? null : body.refreshToken();
        String presented = fromBody != null ? fromBody : cookie;
        if (presented != null && !presented.isBlank()) {
            auth.logout(presented);
        }
        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie("", Duration.ZERO, request).toString())
                .build();
    }

    private ResponseEntity<TokenResponse> respond(
            HttpStatus status, SignedIn signedIn, TokenDelivery delivery, HttpServletRequest request) {
        Instant now = clock.instant();
        Duration accessLifetime = secondsUntil(now, signedIn.accessToken().expiresAt());
        Duration refreshLifetime = secondsUntil(now, signedIn.refreshToken().expiresAt());
        boolean inCookie = delivery == TokenDelivery.COOKIE;
        TokenResponse response = new TokenResponse(
                signedIn.accessToken().value(),
                "Bearer",
                accessLifetime.toSeconds(),
                inCookie ? null : signedIn.refreshToken().value(),
                refreshLifetime.toSeconds());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        if (inCookie) {
            builder.header(
                    HttpHeaders.SET_COOKIE,
                    refreshCookie(signedIn.refreshToken().value(), refreshLifetime, request)
                            .toString());
        }
        return builder.body(response);
    }

    /** Only sent to the auth endpoints, never readable by JavaScript, never sent cross-site. */
    private static ResponseCookie refreshCookie(String value, Duration maxAge, HttpServletRequest request) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(request.getContextPath() + AUTH_PATH)
                .maxAge(maxAge)
                .build();
    }

    /**
     * Whole seconds, rounded up: the tokens were issued a few milliseconds before this response, and rounding down
     * would report 899 s instead of the configured 15 minutes.
     */
    private static Duration secondsUntil(Instant now, Instant expiresAt) {
        Duration remaining = Duration.between(now, expiresAt);
        long seconds = remaining.toSeconds() + (remaining.toNanosPart() > 0 ? 1 : 0);
        return Duration.ofSeconds(Math.max(0, seconds));
    }

    private static TokenDelivery deliveryOf(@Nullable TokenDelivery requested) {
        return requested == null ? TokenDelivery.BODY : requested;
    }
}
