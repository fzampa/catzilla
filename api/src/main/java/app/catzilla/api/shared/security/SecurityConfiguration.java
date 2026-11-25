package app.catzilla.api.shared.security;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless API secured by Bearer JWTs (ADR-004). Paths are relative to the {@code /api} context path. CSRF
 * protection is off because requests authenticate with an {@code Authorization} header, which browsers never add on
 * their own; the refresh cookie is limited to the auth endpoints and sent with {@code SameSite=Strict}.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    /** OWASP's minimum Argon2id settings: 19 MiB memory, 2 iterations, 1 lane; 16-byte salt, 32-byte hash. */
    private static final int ARGON2_SALT_BYTES = 16;

    private static final int ARGON2_HASH_BYTES = 32;
    private static final int ARGON2_PARALLELISM = 1;
    private static final int ARGON2_MEMORY_KIB = 19 * 1024;
    private static final int ARGON2_ITERATIONS = 2;

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.requestMatchers(
                                HttpMethod.POST,
                                "/v1/auth/register",
                                "/v1/auth/login",
                                "/v1/auth/refresh",
                                "/v1/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html",
                                "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * New hashes use Argon2id. The {@code {id}} prefix stored with each hash lets older formats keep verifying and be
     * upgraded on the next sign-in.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(
                ARGON2_SALT_BYTES, ARGON2_HASH_BYTES, ARGON2_PARALLELISM, ARGON2_MEMORY_KIB, ARGON2_ITERATIONS);
        return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2, "bcrypt", new BCryptPasswordEncoder()));
    }
}
