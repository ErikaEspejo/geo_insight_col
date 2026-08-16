package co.edu.distrital.geoinsight.web.dto;

import java.util.List;
import java.util.Map;

public record LayerResponse(String domain, String name, String geometryType, int count,
                            List<AttributeInfo> filterableAttributes,
                            List<String> requiredAttributes,
                            List<String> editableAttributes,
                            Map<String, String> editableAttributeTypes,
                            boolean dataAvailable) {
}
