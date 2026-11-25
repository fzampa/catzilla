package app.catzilla.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** Missing, unknown, expired, revoked or reused refresh token. The client should sign in again. */
final class InvalidRefreshTokenException extends ErrorResponseException {

    InvalidRefreshTokenException() {
        super(
                HttpStatus.UNAUTHORIZED,
                AuthExceptions.problem(
                        HttpStatus.UNAUTHORIZED, "Invalid refresh token", "Sign in again to start a new session."),
                null);
    }
}
