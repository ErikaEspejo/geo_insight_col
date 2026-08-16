package co.edu.distrital.geoinsight.application.common;

/** Credenciales incorrectas. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
