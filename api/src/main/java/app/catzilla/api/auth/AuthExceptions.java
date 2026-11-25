package app.catzilla.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds the RFC 9457 problem details for the auth errors. Messages never reveal whether an email has an account,
 * except at registration, where a duplicate has to be reported.
 */
final class AuthExceptions {

    private AuthExceptions() {}

    static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
