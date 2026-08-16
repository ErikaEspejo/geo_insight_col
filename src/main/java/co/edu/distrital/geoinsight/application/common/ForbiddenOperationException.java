package co.edu.distrital.geoinsight.application.common;

/** Operación prohibida para el actor (p. ej. modificar un registro SGC). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
