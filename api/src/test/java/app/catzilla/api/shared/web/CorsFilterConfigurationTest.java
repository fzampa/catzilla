package app.catzilla.api.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class CorsFilterConfigurationTest {

    private static final String DEV_ORIGIN = "http://localhost:8081";

    @RestController
    static class PingController {
        @GetMapping("/ping")
        String ping() {
            return "pong";
        }
    }

    private static MockMvc mockMvc(String... allowedOrigins) {
        return MockMvcBuilders.standaloneSetup(new PingController())
                .addFilters(CorsFilterConfiguration.createFilter(Arrays.asList(allowedOrigins)))
                .build();
    }

    @Test
    void allowsConfiguredOrigin() throws Exception {
        mockMvc(DEV_ORIGIN)
                .perform(get("/ping").header(HttpHeaders.ORIGIN, DEV_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN));
    }

    @Test
    void answersPreflightForConfiguredOrigin() throws Exception {
        mockMvc(DEV_ORIGIN)
                .perform(options("/ping")
                        .header(HttpHeaders.ORIGIN, DEV_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, DEV_ORIGIN));
    }

    @Test
    void rejectsOtherOrigins() throws Exception {
        mockMvc(DEV_ORIGIN)
                .perform(get("/ping").header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void addsNoCorsHeadersWhenNoOriginsConfigured() throws Exception {
        mockMvc()
                .perform(get("/ping").header(HttpHeaders.ORIGIN, DEV_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void ignoresBlankEntries() {
        assertThat(new CorsProperties(List.of(" http://localhost:8081 ", "", "  ")).allowedOrigins())
                .containsExactly(DEV_ORIGIN);
        assertThat(new CorsProperties(null).allowedOrigins()).isEmpty();
    }
}
