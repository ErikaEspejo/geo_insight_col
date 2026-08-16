package co.edu.distrital.geoinsight.application.common;

/** Conflicto de datos, p. ej. nombre de usuario ya existente. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
