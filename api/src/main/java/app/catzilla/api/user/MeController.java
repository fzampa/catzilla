package app.catzilla.api.user;

import app.catzilla.api.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
@SecurityRequirement(name = "bearerAuth")
class MeController {

    private final UserRepository users;

    MeController(UserRepository users) {
        this.users = users;
    }

    /** A valid token for a user that no longer exists is treated as not signed in. */
    @GetMapping
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return users.findById(CurrentUser.id(jwt))
                .map(UserResponse::from)
                .orElseThrow(() -> new ErrorResponseException(HttpStatus.UNAUTHORIZED));
    }
}
