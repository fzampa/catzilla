package app.catzilla.api.shared.web;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Applies {@link CorsProperties} to every endpoint, including Actuator. A servlet filter is used rather
 * than {@code WebMvcConfigurer#addCorsMappings} because Actuator endpoints do not use the MVC CORS
 * registry.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
class CorsFilterConfiguration {

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(CorsProperties properties) {
        FilterRegistrationBean<CorsFilter> registration =
                new FilterRegistrationBean<>(createFilter(properties.allowedOrigins()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /** Without origins the filter has no mappings and leaves every request untouched. */
    static CorsFilter createFilter(List<String> allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!allowedOrigins.isEmpty()) {
            CorsConfiguration config = new CorsConfiguration();
            config.setAllowedOrigins(allowedOrigins);
            config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            config.setAllowedHeaders(List.of("*"));
            config.setMaxAge(Duration.ofHours(1));
            source.registerCorsConfiguration("/**", config);
        }
        return new CorsFilter(source);
    }
}
