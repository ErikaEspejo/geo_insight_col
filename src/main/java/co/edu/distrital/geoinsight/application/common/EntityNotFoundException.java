package co.edu.distrital.geoinsight.application.common;

/** Entidad no encontrada. */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
