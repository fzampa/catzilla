package app.catzilla.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Full application against a real PostgreSQL. Skipped when Docker is not available. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "catzilla.cors.allowed-origins=" + CatzillaApiApplicationTests.DEV_ORIGIN)
class CatzillaApiApplicationTests {

    static final String DEV_ORIGIN = "http://localhost:8081";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE);

    @LocalServerPort
    int port;

    RestClient client;

    @BeforeEach
    void createClient() {
        client = RestClient.create("http://localhost:" + port + "/api");
    }

    @Test
    void healthIsUpWithMigrationsApplied() {
        String body = client.get().uri("/actuator/health").retrieve().body(String.class);

        assertThat(body).contains("\"status\":\"UP\"");
    }

    @Test
    void configuredOriginMayCallActuatorCrossOrigin() {
        ResponseEntity<String> response = client.get()
                .uri("/actuator/health")
                .header(HttpHeaders.ORIGIN, DEV_ORIGIN)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(DEV_ORIGIN);
    }
}
