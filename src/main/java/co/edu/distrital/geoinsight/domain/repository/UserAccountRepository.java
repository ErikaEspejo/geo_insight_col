package co.edu.distrital.geoinsight.domain.repository;

import co.edu.distrital.geoinsight.domain.model.UserAccount;

import java.util.List;
import java.util.Optional;

/** Persistencia de cuentas de usuario (FR-024). */
public interface UserAccountRepository {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<UserAccount> findAll();

    UserAccount save(UserAccount account);
}
