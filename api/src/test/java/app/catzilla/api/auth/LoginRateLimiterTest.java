package app.catzilla.api.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class LoginRateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T10:00:00Z"));
    private final LoginRateLimiter limiter = new LoginRateLimiter(clock);

    @Test
    void blocksAnEmailAfterFiveFailuresUntilTheWindowEnds() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES_PER_EMAIL; i++) {
            assertThatCode(() -> limiter.checkAllowed("ana@example.com", "10.0.0.1"))
                    .doesNotThrowAnyException();
            limiter.recordFailure("ana@example.com", "10.0.0.1");
        }

        clock.advance(Duration.ofMinutes(5));
        assertThatThrownBy(() -> limiter.checkAllowed("ana@example.com", "10.0.0.2"))
                .isInstanceOfSatisfying(
                        TooManyAttemptsException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(
                                        e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
                                .isEqualTo("600"));
        assertThatCode(() -> limiter.checkAllowed("bruno@example.com", "10.0.0.1"))
                .doesNotThrowAnyException();

        clock.advance(Duration.ofMinutes(10));
        assertThatCode(() -> limiter.checkAllowed("ana@example.com", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void successfulSignInClearsTheEmailsFailures() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES_PER_EMAIL - 1; i++) {
            limiter.recordFailure("ana@example.com", "10.0.0.1");
        }
        limiter.recordSuccess("ana@example.com");
        limiter.recordFailure("ana@example.com", "10.0.0.1");

        assertThatCode(() -> limiter.checkAllowed("ana@example.com", "10.0.0.1"))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksAnIpThatTriesManyEmails() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES_PER_IP; i++) {
            limiter.recordFailure("user" + i + "@example.com", "10.0.0.9");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("new@example.com", "10.0.0.9"))
                .isInstanceOf(TooManyAttemptsException.class);
        assertThatCode(() -> limiter.checkAllowed("new@example.com", "10.0.0.10"))
                .doesNotThrowAnyException();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
