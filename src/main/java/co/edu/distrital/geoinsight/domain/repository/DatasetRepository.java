package co.edu.distrital.geoinsight.domain.repository;

import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.AttributeValueType;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Acceso de solo lectura a los datasets SGC. Los registros son inmutables
 * (FR-003); este repositorio nunca los modifica.
 */
public interface DatasetRepository {

    List<GeoscienceEntity> findSgcByDomain(Domain domain);

    /** Atributos realmente presentes en el dataset (FR-008, FR-018). */
    Set<String> attributeNames(Domain domain);

    /** Campos obligatorios para crear entidades del dominio (FR-018). */
    Set<String> requiredAttributes(Domain domain);

    /** Campos descriptivos reales admitidos al crear entidades GEOINSIGHT. */
    Set<String> editableAttributes(Domain domain);

    Map<String, AttributeValueType> editableAttributeTypes(Domain domain);

    /** Valores distintos realmente presentes para un atributo del dataset. */
    List<Object> distinctValues(Domain domain, String attribute);

    /** Verifica que la geometría pertenece a un dominio (tipo admitido). */
    boolean acceptsGeometry(Domain domain, Geometry geometry);

    boolean isDatasetLoaded(Domain domain);

    Set<Domain> missingDatasets();

    Optional<GeoscienceEntity> findSgcById(String id);
}
