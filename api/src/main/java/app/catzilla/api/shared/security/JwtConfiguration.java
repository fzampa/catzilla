package app.catzilla.api.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** The API both issues and verifies its own RS256 tokens with one key pair (ADR-004). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
class JwtConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JwtConfiguration.class);

    @Bean
    RSAKey jwtSigningKey(JwtProperties properties) {
        String configured = properties.privateKey();
        if (configured == null || configured.isBlank()) {
            log.warn("JWT_PRIVATE_KEY is not set: using a temporary signing key. Issued tokens stop working when"
                    + " the API restarts. Set JWT_PRIVATE_KEY anywhere other than local development and tests.");
            return JwtKeys.generate();
        }
        return JwtKeys.fromBase64Pkcs8(configured);
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwtSigningKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtSigningKey, JwtProperties properties, Clock clock) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtSigningKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(JwtKeys.validator(properties.issuer(), properties.audience(), clock));
        return decoder;
    }
}
