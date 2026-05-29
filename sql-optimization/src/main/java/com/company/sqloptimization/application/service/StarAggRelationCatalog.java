package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.qualifier;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.relationKey;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggRelationCatalog {

    private final Map<String, StarAggRelationSpec> relationByKey;
    private final Map<String, StarAggRelationSpec> relationByReference;

    private StarAggRelationCatalog(Map<String, StarAggRelationSpec> relationByKey,
                                   Map<String, StarAggRelationSpec> relationByReference) {
        this.relationByKey = relationByKey;
        this.relationByReference = relationByReference;
    }

    static StarAggRelationCatalog from(List<Map<String, Object>> tables) {
        LinkedHashMap<String, StarAggRelationSpec> byKey = new LinkedHashMap<String, StarAggRelationSpec>();
        LinkedHashMap<String, StarAggRelationSpec> byReference = new LinkedHashMap<String, StarAggRelationSpec>();
        for (Map<String, Object> table : tables) {
            StarAggRelationSpec relation = new StarAggRelationSpec(text(table.get("tableName")), text(table.get("alias")));
            byKey.put(relation.key, relation);
            for (String reference : relation.references) {
                byReference.put(relationKey(reference), relation);
            }
        }
        return new StarAggRelationCatalog(byKey, byReference);
    }

    StarAggRelationSpec relationForColumn(String sourceColumn) {
        String sourceQualifier = qualifier(sourceColumn);
        if (!StringUtils.hasText(sourceQualifier)) {
            return null;
        }
        return relationByReference.get(relationKey(sourceQualifier));
    }

    Map<String, Object> factEvidence(String factKey, String inference) {
        StarAggRelationSpec fact = relationByKey.get(factKey);
        Map<String, Object> item = fact == null ? new LinkedHashMap<String, Object>() : fact.toMap();
        item.put("inference", inference);
        return item;
    }

    List<Map<String, Object>> dimensionEvidence(Set<String> dimensionKeys) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (String dimensionKey : dimensionKeys) {
            StarAggRelationSpec relation = relationByKey.get(dimensionKey);
            if (relation != null) {
                result.add(relation.toMap());
            }
        }
        return result;
    }
}
