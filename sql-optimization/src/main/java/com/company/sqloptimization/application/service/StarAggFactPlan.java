package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggProfileValues.firstText;
import static com.company.sqloptimization.application.service.StarAggProfileValues.text;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.relationKey;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class StarAggFactPlan {

    final List<Map<String, Object>> blockingReasons;
    final String factKey;
    final String inference;
    final Map<String, Object> factTable;
    final List<Map<String, Object>> dimensionTables;

    private StarAggFactPlan(List<Map<String, Object>> blockingReasons,
                            String factKey,
                            String inference,
                            Map<String, Object> factTable,
                            List<Map<String, Object>> dimensionTables) {
        this.blockingReasons = blockingReasons;
        this.factKey = factKey;
        this.inference = inference;
        this.factTable = factTable;
        this.dimensionTables = dimensionTables;
    }

    static StarAggFactPlan blocked() {
        return blocked(Collections.<Map<String, Object>>emptyList());
    }

    static StarAggFactPlan blocked(List<Map<String, Object>> blockingReasons) {
        return new StarAggFactPlan(
            blockingReasons,
            "",
            "UNRESOLVED",
            Collections.<String, Object>emptyMap(),
            Collections.<Map<String, Object>>emptyList()
        );
    }

    static StarAggFactPlan generated(String factKey,
                                     String inference,
                                     Map<String, Object> factTable,
                                     List<Map<String, Object>> dimensionTables) {
        return new StarAggFactPlan(Collections.<Map<String, Object>>emptyList(), factKey, inference, factTable, dimensionTables);
    }

    Set<String> dimensionKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        for (Map<String, Object> dimensionTable : dimensionTables) {
            keys.add(relationKey(firstText(text(dimensionTable.get("alias")), text(dimensionTable.get("tableName")))));
        }
        return keys;
    }
}
