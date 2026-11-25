package app.catzilla.api.shared.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

/** Loads or generates the RS256 signing key and builds the validation rules for incoming tokens. */
final class JwtKeys {

    static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final int KEY_SIZE_BITS = 2048;

    private JwtKeys() {}

    /** A new random key. Tokens signed with it stop verifying once the process ends. */
    static RSAKey generate() {
        try {
            return new RSAKeyGenerator(KEY_SIZE_BITS).keyIDFromThumbprint(true).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not generate a JWT signing key", e);
        }
    }

    /**
     * Reads an RSA private key given as base64-encoded PKCS#8 DER, the output of {@code openssl genpkey -algorithm
     * RSA -pkeyopt rsa_keygen_bits:2048 -outform DER | base64}. The key ID is the key's RFC 7638 thumbprint, so it is
     * stable for a given key and changes when the key is rotated.
     */
    static RSAKey fromBase64Pkcs8(String base64) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(base64.strip());
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
            if (privateKey.getModulus().bitLength() < KEY_SIZE_BITS) {
                throw new IllegalStateException("JWT_PRIVATE_KEY must be at least " + KEY_SIZE_BITS + " bits");
            }
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyIDFromThumbprint()
                    .build();
        } catch (GeneralSecurityException | JOSEException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException(
                    "JWT_PRIVATE_KEY is not a base64-encoded PKCS#8 RSA private key (see api/.env.example)", e);
        }
    }

    /** Signature is checked by the decoder; these rules check expiry, issuer and audience. */
    static OAuth2TokenValidator<Jwt> validator(String issuer, String audience, Clock clock) {
        JwtTimestampValidator timestamps = new JwtTimestampValidator(CLOCK_SKEW);
        timestamps.setClock(clock);
        return new DelegatingOAuth2TokenValidator<>(
                timestamps,
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull),
                new JwtIssuerValidator(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(audience)));
    }
}
