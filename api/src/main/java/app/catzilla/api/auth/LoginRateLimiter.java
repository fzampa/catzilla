package app.catzilla.api.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Slows down password guessing by counting failed sign-ins per email and per client IP in a fixed window. In memory,
 * which is enough while the API runs as a single instance (ADR-004).
 */
@Component
class LoginRateLimiter {

    static final int MAX_FAILURES_PER_EMAIL = 5;
    static final int MAX_FAILURES_PER_IP = 20;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentMap<String, Window> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Throws when either the email or the IP has used up its failed attempts for the current window. */
    void checkAllowed(String normalizedEmail, String clientIp) {
        Instant now = clock.instant();
        Optional<Duration> emailWait = retryAfter(emailKey(normalizedEmail), MAX_FAILURES_PER_EMAIL, now);
        Optional<Duration> ipWait = retryAfter(ipKey(clientIp), MAX_FAILURES_PER_IP, now);
        Optional<Duration> wait = emailWait.isPresent() && ipWait.isPresent()
                ? Optional.of(max(emailWait.get(), ipWait.get()))
                : emailWait.or(() -> ipWait);
        if (wait.isPresent()) {
            throw new TooManyAttemptsException(wait.get());
        }
    }

    void recordFailure(String normalizedEmail, String clientIp) {
        Instant now = clock.instant();
        increment(emailKey(normalizedEmail), now);
        increment(ipKey(clientIp), now);
        if (failures.size() > CLEANUP_THRESHOLD) {
            failures.values().removeIf(window -> window.isOver(now));
        }
    }

    /** A successful sign-in clears the email's failures; the IP's count stays, as one IP may try many emails. */
    void recordSuccess(String normalizedEmail) {
        failures.remove(emailKey(normalizedEmail));
    }

    private void increment(String key, Instant now) {
        failures.compute(
                key, (k, window) -> window == null || window.isOver(now) ? new Window(now, 1) : window.plusOne());
    }

    private Optional<Duration> retryAfter(String key, int max, Instant now) {
        Window window = failures.get(key);
        if (window == null || window.isOver(now) || window.count() < max) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, window.start().plus(WINDOW)));
    }

    private static Duration max(Duration a, Duration b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private static String emailKey(String normalizedEmail) {
        return "email:" + normalizedEmail;
    }

    private static String ipKey(String clientIp) {
        return "ip:" + clientIp;
    }

    private record Window(Instant start, int count) {
        boolean isOver(Instant now) {
            return !now.isBefore(start.plus(WINDOW));
        }

        Window plusOne() {
            return new Window(start, count + 1);
        }
    }
}
