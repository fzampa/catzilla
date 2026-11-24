package app.catzilla.api.shared.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API from a different origin.
 *
 * <p>Empty by default: in deployment the web app and the API share one origin behind the reverse proxy,
 * so no cross-origin access is needed. Set {@code CORS_ALLOWED_ORIGINS} (comma-separated) for local
 * development, for example {@code http://localhost:8081} for the Expo web dev server.
 */
@ConfigurationProperties("catzilla.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .map(String::strip)
                        .filter(origin -> !origin.isEmpty())
                        .toList();
    }
}
