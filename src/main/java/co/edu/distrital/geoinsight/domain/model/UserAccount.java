package co.edu.distrital.geoinsight.domain.model;

import java.util.Objects;

/** Cuenta de acceso. La contraseña se guarda siempre como hash (FR-024). */
public final class UserAccount {

    private final String username;
    private final String passwordHash;
    private final Role role;

    public UserAccount(String username, String passwordHash, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("El hash de contraseña es obligatorio");
        }
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "rol requerido");
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }
}
