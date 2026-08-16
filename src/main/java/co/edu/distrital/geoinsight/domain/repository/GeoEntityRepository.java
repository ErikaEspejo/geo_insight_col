package co.edu.distrital.geoinsight.domain.repository;

import co.edu.distrital.geoinsight.domain.model.Domain;
import co.edu.distrital.geoinsight.domain.model.GeoscienceEntity;

import java.util.List;
import java.util.Optional;

/** Persistencia de entidades GEOINSIGHT (FR-005, FR-019). */
public interface GeoEntityRepository {

    List<GeoscienceEntity> findAll();

    List<GeoscienceEntity> findByDomain(Domain domain);

    Optional<GeoscienceEntity> findById(String id);

    GeoscienceEntity save(GeoscienceEntity entity);

    void delete(String id);
}
