package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PrejoinColumnPlan {

    final List<Map<String, Object>> blockingReasons;
    final List<PrejoinColumnMapping> mappings;

    PrejoinColumnPlan(List<Map<String, Object>> blockingReasons, List<PrejoinColumnMapping> mappings) {
        this.blockingReasons = blockingReasons;
        this.mappings = mappings;
    }

    static PrejoinColumnPlan blocked(List<Map<String, Object>> blockingReasons) {
        return new PrejoinColumnPlan(blockingReasons, Collections.<PrejoinColumnMapping>emptyList());
    }

    List<String> ddlSelectItems() {
        List<String> items = new ArrayList<String>();
        for (PrejoinColumnMapping mapping : mappings) {
            if (mapping.sourceColumn.equals(mapping.outputColumn)) {
                items.add(mapping.sourceColumn);
            } else {
                items.add(mapping.sourceColumn + " AS " + mapping.outputColumn);
            }
        }
        if (items.isEmpty()) {
            items.add("1 AS prejoin_marker");
        }
        return items;
    }

    List<Map<String, Object>> fieldMappings() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (PrejoinColumnMapping mapping : mappings) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("sourceColumn", mapping.sourceColumn);
            item.put("mvColumn", mapping.outputColumn);
            item.put("sourceQualifier", mapping.qualifier);
            item.put("disambiguation", mapping.ambiguousName ? "QUALIFIER_PREFIXED" : "DIRECT");
            result.add(item);
        }
        return result;
    }

    List<Map<String, Object>> aliasDisambiguation() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (PrejoinColumnMapping mapping : mappings) {
            if (!mapping.ambiguousName) {
                continue;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("sourceColumn", mapping.sourceColumn);
            item.put("mvColumn", mapping.outputColumn);
            item.put("strategy", "QUALIFIER_PREFIXED");
            result.add(item);
        }
        return result;
    }
}
