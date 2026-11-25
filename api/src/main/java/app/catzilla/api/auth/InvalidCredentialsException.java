package app.catzilla.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/** Same response for an unknown email and a wrong password. */
final class InvalidCredentialsException extends ErrorResponseException {

    InvalidCredentialsException() {
        super(
                HttpStatus.UNAUTHORIZED,
                AuthExceptions.problem(HttpStatus.UNAUTHORIZED, "Invalid credentials", "Invalid email or password."),
                null);
    }
}
