package app.catzilla.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

final class EmailAlreadyRegisteredException extends ErrorResponseException {

    EmailAlreadyRegisteredException() {
        super(
                HttpStatus.CONFLICT,
                AuthExceptions.problem(
                        HttpStatus.CONFLICT, "Email already registered", "An account with this email already exists."),
                null);
    }
}
