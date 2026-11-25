package app.catzilla.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import app.catzilla.api.TestcontainersConfiguration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The auth endpoints end to end, over HTTP, against a real PostgreSQL (ADR-004). */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

    private static final String PASSWORD = "correct horse battery";

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api")
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {})
                .build();
    }

    @Test
    void registerSignsInAndMeReturnsTheProfile() {
        String email = uniqueEmail();

        ResponseEntity<Map> registered = register("  " + email.toUpperCase(java.util.Locale.ROOT) + " ", PASSWORD);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map tokens = registered.getBody();
        assertThat(tokens).containsEntry("tokenType", "Bearer").containsEntry("expiresIn", 900);
        assertThat(tokens.get("refreshToken")).isNotNull();

        ResponseEntity<Map> me = me((String) tokens.get("accessToken"));
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("email", email).containsEntry("displayName", "Ana");
    }

    @Test
    void storesOnlyASaltedArgon2HashOfThePassword() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        String hash = jdbc.queryForObject(
                "select c.password_hash from password_credentials c join users u on u.id = c.user_id"
                        + " where u.email = ?",
                String.class,
                email);

        assertThat(hash).startsWith("{argon2}$argon2id$").doesNotContain(PASSWORD);
    }

    @Test
    void rejectsDuplicateEmailRegardlessOfCase() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        ResponseEntity<Map> duplicate = register(email.toUpperCase(java.util.Locale.ROOT), PASSWORD);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void validatesRegistration() {
        ResponseEntity<Map> shortPassword = register(uniqueEmail(), "too-short");
        ResponseEntity<Map> notAnEmail = register("not-an-email", PASSWORD);

        assertThat(shortPassword.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(notAnEmail.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        ResponseEntity<Map> wrongPassword = login(email, "wrong password!", null);
        ResponseEntity<Map> unknownEmail = login(uniqueEmail(), PASSWORD, null);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody()).isEqualTo(unknownEmail.getBody());
        assertThat(wrongPassword.getBody()).containsEntry("detail", "Invalid email or password.");
    }

    @Test
    void refreshRotatesTokensAndReuseEndsAllSessions() {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String first = (String) login(email, PASSWORD, null).getBody().get("refreshToken");

        ResponseEntity<Map> refreshed = refreshWithBody(first);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        String second = (String) refreshed.getBody().get("refreshToken");
        assertThat(second).isNotEqualTo(first);
        assertThat(me((String) refreshed.getBody().get("accessToken")).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // The exchanged token is presented again: treat as stolen and revoke everything.
        assertThat(refreshWithBody(first).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshWithBody(second).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutEndsTheSessionAndIsRepeatable() {
        String email = uniqueEmail();
        String refreshToken = (String) register(email, PASSWORD).getBody().get("refreshToken");

        assertThat(logoutWithBody(refreshToken).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logoutWithBody(refreshToken).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(refreshWithBody(refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void webClientsGetTheRefreshTokenOnlyInAnHttpOnlyCookie() {
        String email = uniqueEmail();
        register(email, PASSWORD);

        ResponseEntity<Map> login = login(email, PASSWORD, "COOKIE");

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).doesNotContainKey("refreshToken").containsKey("accessToken");
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith(AuthController.REFRESH_COOKIE + "=")
                .contains("Path=/api/v1/auth", "HttpOnly", "Secure", "SameSite=Strict", "Max-Age=2592000");

        String cookie = setCookie.substring(0, setCookie.indexOf(';'));
        ResponseEntity<Map> refreshed = client.post()
                .uri("/v1/auth/refresh")
                .header(HttpHeaders.COOKIE, cookie)
                .retrieve()
                .toEntity(Map.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).doesNotContainKey("refreshToken");
        assertThat(refreshed.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(AuthController.REFRESH_COOKIE + "=")
                .doesNotStartWith(cookie);

        ResponseEntity<Void> logout = client.post()
                .uri("/v1/auth/logout")
                .header(
                        HttpHeaders.COOKIE,
                        refreshed.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0])
                .retrieve()
                .toBodilessEntity();
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logout.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void protectedEndpointsNeedAValidAccessToken() {
        String token = (String) register(uniqueEmail(), PASSWORD).getBody().get("accessToken");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(me(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(me(tampered).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(me("not-a-jwt").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthAndApiDocsStayPublic() {
        assertThat(client.get()
                        .uri("/actuator/health")
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(client.get()
                        .uri("/v3/api-docs")
                        .retrieve()
                        .toBodilessEntity()
                        .getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void tooManyFailedSignInsAreRateLimited() {
        String email = uniqueEmail();
        register(email, PASSWORD);
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES_PER_EMAIL; i++) {
            assertThat(login(email, "wrong password!", null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<Map> blocked = login(email, PASSWORD, null);

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isNotBlank();
    }

    private ResponseEntity<Map> register(String email, String password) {
        return client.post()
                .uri("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email, "password", password, "displayName", "Ana"))
                .retrieve()
                .toEntity(Map.class);
    }

    private ResponseEntity<Map> login(String email, String password, @Nullable String tokenDelivery) {
        Map<String, String> body = new HashMap<>(Map.of("email", email, "password", password));
        if (tokenDelivery != null) {
            body.put("tokenDelivery", tokenDelivery);
        }
        return client.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
    }

    private ResponseEntity<Map> refreshWithBody(String refreshToken) {
        return client.post()
                .uri("/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .toEntity(Map.class);
    }

    private ResponseEntity<Void> logoutWithBody(String refreshToken) {
        return client.post()
                .uri("/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refreshToken", refreshToken))
                .retrieve()
                .toBodilessEntity();
    }

    private ResponseEntity<Map> me(@Nullable String accessToken) {
        RestClient.RequestHeadersSpec<?> request = client.get().uri("/v1/me");
        if (accessToken != null) {
            request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return request.retrieve().toEntity(Map.class);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
