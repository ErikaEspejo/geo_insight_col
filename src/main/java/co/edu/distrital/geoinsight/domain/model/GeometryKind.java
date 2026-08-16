package co.edu.distrital.geoinsight.domain.model;

import co.edu.distrital.geoinsight.domain.geometry.Geometry;
import co.edu.distrital.geoinsight.domain.geometry.LineString;
import co.edu.distrital.geoinsight.domain.geometry.MultiLineString;
import co.edu.distrital.geoinsight.domain.geometry.MultiPoint;
import co.edu.distrital.geoinsight.domain.geometry.MultiPolygon;
import co.edu.distrital.geoinsight.domain.geometry.Point;
import co.edu.distrital.geoinsight.domain.geometry.Polygon;

/** Tipo de geometría admitida por un dominio (FR-017). */
public enum GeometryKind {
    POINT,
    LINE,
    POLYGON;

    public boolean accepts(Geometry geometry) {
        if (geometry == null) {
            return false;
        }
        return switch (this) {
            case POINT -> geometry instanceof Point || geometry instanceof MultiPoint;
            case LINE -> geometry instanceof LineString || geometry instanceof MultiLineString;
            case POLYGON -> geometry instanceof Polygon || geometry instanceof MultiPolygon;
        };
    }
}
