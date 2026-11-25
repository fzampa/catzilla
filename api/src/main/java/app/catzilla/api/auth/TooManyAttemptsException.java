package app.catzilla.api.auth;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** 429 with a {@code Retry-After} header in seconds. */
final class TooManyAttemptsException extends ErrorResponseException {

    TooManyAttemptsException(Duration retryAfter) {
        super(
                HttpStatus.TOO_MANY_REQUESTS,
                AuthExceptions.problem(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many sign-in attempts",
                        "Too many failed sign-in attempts. Try again later."),
                null);
        getHeaders().set(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, retryAfter.toSeconds())));
    }
}
