package co.edu.distrital.geoinsight.application.auth;

import co.edu.distrital.geoinsight.application.common.ConflictException;
import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.domain.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registro de cuentas de consulta. Invariante (FR-022): toda cuenta registrada
 * obtiene rol USER; ninguna cuenta registrada puede ser admin.
 */
@Service
public class RegistrationService {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 30;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String username, String password) {
        validate(username, password);
        if (userAccountRepository.existsByUsername(username)) {
            throw new ConflictException("El nombre de usuario ya existe");
        }
        UserAccount account = new UserAccount(username, passwordEncoder.encode(password), Role.USER);
        return userAccountRepository.save(account);
    }

    private void validate(String username, String password) {
        if (username == null || username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH
                || !username.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException(
                    "El nombre de usuario debe tener entre " + MIN_USERNAME_LENGTH + " y " + MAX_USERNAME_LENGTH
                            + " caracteres alfanuméricos o guion bajo");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
    }
}
