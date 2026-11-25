package app.catzilla.api.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Expects an address already passed through {@link EmailAddresses#normalize}. */
    Optional<User> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);
}
