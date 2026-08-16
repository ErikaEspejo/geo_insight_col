package co.edu.distrital.geoinsight.application.auth;

import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.domain.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Autenticación de cuentas (FR-021). Verifica credenciales contra el
 * repositorio con BCrypt (FR-024) sin exponer el hash.
 */
@Service
public class AuthenticationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<AuthenticatedUser> authenticate(String username, String password) {
        return userAccountRepository.findByUsername(username)
                .filter(account -> passwordEncoder.matches(password, account.passwordHash()))
                .map(AuthenticatedUser::from);
    }

    public record AuthenticatedUser(String username, Role role, boolean admin) {

        static AuthenticatedUser from(UserAccount account) {
            return new AuthenticatedUser(account.username(), account.role(), account.role() == Role.ADMIN);
        }
    }
}
