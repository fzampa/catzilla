package app.catzilla.api.auth;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {}
