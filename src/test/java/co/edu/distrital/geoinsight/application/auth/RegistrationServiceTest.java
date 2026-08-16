package co.edu.distrital.geoinsight.application.auth;

import co.edu.distrital.geoinsight.domain.model.Role;
import co.edu.distrital.geoinsight.domain.model.UserAccount;
import co.edu.distrital.geoinsight.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrationServiceTest {

    private RegistrationService service;
    private FakeUserAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeUserAccountRepository();
        service = new RegistrationService(repository, new BCryptPasswordEncoder());
    }

    @Test
    void registerCreatesUserRoleNeverAdmin() {
        UserAccount account = service.register("ana", "clave123");
        assertThat(account.role()).isEqualTo(Role.USER);
        assertThat(account.username()).isEqualTo("ana");
        assertThat(account.passwordHash()).isNotEqualTo("clave123");
        assertThat(account.passwordHash()).startsWith("$2");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        service.register("ana", "clave123");
        assertThatThrownBy(() -> service.register("ana", "otra456"))
                .isInstanceOf(co.edu.distrital.geoinsight.application.common.ConflictException.class);
    }

    @Test
    void registerRejectsInvalidUsernameAndShortPassword() {
        assertThatThrownBy(() -> service.register("ab", "clave123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.register("ana", "abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authenticateWithCorrectPassword() {
        service.register("ana", "clave123");
        AuthenticationService auth = new AuthenticationService(repository, new BCryptPasswordEncoder());
        AuthenticationService.AuthenticatedUser user = auth.authenticate("ana", "clave123").orElseThrow();
        assertThat(user.username()).isEqualTo("ana");
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.admin()).isFalse();
    }

    @Test
    void authenticateRejectsWrongPassword() {
        service.register("ana", "clave123");
        AuthenticationService auth = new AuthenticationService(repository, new BCryptPasswordEncoder());
        assertThat(auth.authenticate("ana", "incorrecta")).isEmpty();
        assertThat(auth.authenticate("desconocido", "clave123")).isEmpty();
    }

    @Test
    void seededAdminAuthenticatesAsAdmin() {
        repository.save(new UserAccount("admin",
                new BCryptPasswordEncoder().encode("admin123"), Role.ADMIN));
        AuthenticationService auth = new AuthenticationService(repository, new BCryptPasswordEncoder());
        AuthenticationService.AuthenticatedUser user = auth.authenticate("admin", "admin123").orElseThrow();
        assertThat(user.admin()).isTrue();
        assertThat(user.role()).isEqualTo(Role.ADMIN);
    }

    static class FakeUserAccountRepository implements UserAccountRepository {
        private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findByUsername(String username) {
            return Optional.ofNullable(accounts.get(username));
        }

        @Override
        public boolean existsByUsername(String username) {
            return accounts.containsKey(username);
        }

        @Override
        public List<UserAccount> findAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public UserAccount save(UserAccount account) {
            accounts.put(account.username(), account);
            return account;
        }
    }
}
