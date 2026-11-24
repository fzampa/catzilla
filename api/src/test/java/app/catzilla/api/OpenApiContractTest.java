package app.catzilla.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Keeps contracts/openapi.yaml in sync with the code. Runs without a database.
 *
 * <p>If it fails because the API changed on purpose, run {@code ./gradlew updateContract} and commit the
 * updated spec together with the code change.
 */
@SpringBootTest(
        properties = {
            "spring.autoconfigure.exclude="
                    + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                    + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                    + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                    + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        })
@AutoConfigureMockMvc
class OpenApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void committedSpecMatchesApi() throws Exception {
        String actual = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs.yaml"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        assertThat(actual).contains("openapi:");

        Path contract = Path.of(System.getProperty("contract.file", "../contracts/openapi.yaml"));
        if (Boolean.getBoolean("contract.update")) {
            Files.writeString(contract, normalize(actual));
            return;
        }
        assertThat(normalize(read(contract)))
                .as("contracts/openapi.yaml is out of date; run ./gradlew updateContract")
                .isEqualTo(normalize(actual));
    }

    private static String read(Path path) throws IOException {
        return Files.exists(path) ? Files.readString(path) : "";
    }

    /** Ignores the server URL, which depends on the test's port. */
    private static String normalize(String spec) {
        return spec.replaceAll("(?m)^servers:\\n(?:[ -].*\\n)*", "").strip() + "\n";
    }
}
