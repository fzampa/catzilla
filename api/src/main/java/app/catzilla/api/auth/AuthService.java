package app.catzilla.api.auth;

import app.catzilla.api.auth.RefreshTokenService.IssuedRefreshToken;
import app.catzilla.api.shared.security.AccessTokenIssuer;
import app.catzilla.api.shared.security.AccessTokenIssuer.AccessToken;
import app.catzilla.api.user.EmailAddresses;
import app.catzilla.api.user.User;
import app.catzilla.api.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email and password sign-up and sign-in, and the token lifecycle that follows (ADR-004). */
@Service
class AuthService {

    private final UserRepository users;
    private final PasswordCredentialRepository credentials;
    private final RefreshTokenService refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;
    private final Clock clock;

    /**
     * Compared against when the email has no password, so a sign-in for an unknown email takes as long as one with a
     * wrong password and response times don't reveal which emails have accounts.
     */
    private final String timingDummyHash;

    AuthService(
            UserRepository users,
            PasswordCredentialRepository credentials,
            RefreshTokenService refreshTokens,
            AccessTokenIssuer accessTokens,
            PasswordEncoder passwordEncoder,
            LoginRateLimiter rateLimiter,
            Clock clock) {
        this.users = users;
        this.credentials = credentials;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.timingDummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    SignedIn register(String email, String password, String displayName) {
        String normalizedEmail = EmailAddresses.normalize(email);
        if (users.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException();
        }
        Instant now = clock.instant();
        User user = new User(UUID.randomUUID(), normalizedEmail, displayName, now);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Another registration with the same email won the race.
            throw new EmailAlreadyRegisteredException();
        }
        credentials.save(new PasswordCredential(user.getId(), passwordEncoder.encode(password), now));
        return signIn(user.getId());
    }

    @Transactional
    SignedIn login(String email, String password, String clientIp) {
        String normalizedEmail = EmailAddresses.normalize(email);
        rateLimiter.checkAllowed(normalizedEmail, clientIp);

        Optional<User> user = users.findByEmail(normalizedEmail);
        Optional<PasswordCredential> credential = user.flatMap(found -> credentials.findById(found.getId()));
        String hash = credential.map(PasswordCredential::passwordHash).orElse(timingDummyHash);
        boolean matches = passwordEncoder.matches(password, hash) && credential.isPresent();
        if (!matches || user.isEmpty()) {
            rateLimiter.recordFailure(normalizedEmail, clientIp);
            throw new InvalidCredentialsException();
        }
        rateLimiter.recordSuccess(normalizedEmail);

        if (passwordEncoder.upgradeEncoding(hash)) {
            credential.get().replaceHash(passwordEncoder.encode(password), clock.instant());
        }
        return signIn(user.get().getId());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    SignedIn refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        return new SignedIn(accessTokens.issue(rotation.userId()), rotation.next());
    }

    @Transactional
    void logout(String rawRefreshToken) {
        refreshTokens.revokeSession(rawRefreshToken);
    }

    private SignedIn signIn(UUID userId) {
        return new SignedIn(accessTokens.issue(userId), refreshTokens.issueNewSession(userId));
    }

    record SignedIn(AccessToken accessToken, IssuedRefreshToken refreshToken) {}
}
