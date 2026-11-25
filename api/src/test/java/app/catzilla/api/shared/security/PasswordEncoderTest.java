package app.catzilla.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private final PasswordEncoder encoder = new SecurityConfiguration().passwordEncoder();

    @Test
    void hashesWithArgon2idUsingOwaspParameters() {
        String hash = encoder.encode("correct horse battery staple");

        assertThat(hash).startsWith("{argon2}$argon2id$v=19$m=19456,t=2,p=1$");
        assertThat(encoder.matches("correct horse battery staple", hash)).isTrue();
        assertThat(encoder.matches("wrong password", hash)).isFalse();
        assertThat(encoder.upgradeEncoding(hash)).isFalse();
    }

    @Test
    void saltsEachHashDifferently() {
        assertThat(encoder.encode("same password")).isNotEqualTo(encoder.encode("same password"));
    }

    @Test
    void verifiesOlderBcryptHashesAndAsksToUpgradeThem() {
        String bcrypt = "{bcrypt}" + new BCryptPasswordEncoder().encode("old password");

        assertThat(encoder.matches("old password", bcrypt)).isTrue();
        assertThat(encoder.upgradeEncoding(bcrypt)).isTrue();
    }
}
