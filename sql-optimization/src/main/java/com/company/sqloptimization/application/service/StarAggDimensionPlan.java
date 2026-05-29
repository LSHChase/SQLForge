package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggReferenceSupport.normalizeExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class StarAggDimensionPlan {

    final List<Map<String, Object>> blockingReasons;
    final List<StarAggDimensionSpec> dimensions;

    StarAggDimensionPlan(List<Map<String, Object>> blockingReasons, List<StarAggDimensionSpec> dimensions) {
        this.blockingReasons = blockingReasons;
        this.dimensions = dimensions;
    }

    static StarAggDimensionPlan blocked() {
        return blocked(Collections.<Map<String, Object>>emptyList());
    }

    static StarAggDimensionPlan blocked(List<Map<String, Object>> blockingReasons) {
        return new StarAggDimensionPlan(blockingReasons, Collections.<StarAggDimensionSpec>emptyList());
    }

    List<String> ddlSelectItems() {
        List<String> items = new ArrayList<String>();
        for (StarAggDimensionSpec dimension : dimensions) {
            if (dimension.sourceExpression.equals(dimension.outputName)) {
                items.add(dimension.sourceExpression);
            } else {
                items.add(dimension.sourceExpression + " AS " + dimension.outputName);
            }
        }
        return items;
    }

    List<String> ddlGroupByItems() {
        List<String> items = new ArrayList<String>();
        for (StarAggDimensionSpec dimension : dimensions) {
            items.add(dimension.sourceExpression);
        }
        return items;
    }

    StarAggDimensionSpec find(String expression) {
        String normalized = normalizeExpression(expression);
        for (StarAggDimensionSpec dimension : dimensions) {
            if (dimension.matches(normalized)) {
                return dimension;
            }
        }
        return null;
    }

    StarAggDimensionSpec findBySources(List<String> sourceColumns) {
        for (String sourceColumn : sourceColumns) {
            StarAggDimensionSpec dimension = find(sourceColumn);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }

    List<Map<String, Object>> dimensionSources() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (StarAggDimensionSpec dimension : dimensions) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("sourceExpression", dimension.sourceExpression);
            item.put("mvColumn", dimension.outputName);
            item.put("sourceColumns", new ArrayList<String>(dimension.references));
            item.put("sourceTable", dimension.relation.tableName);
            item.put("sourceAlias", dimension.relation.alias);
            item.put("sourceRole", dimension.role);
            item.put("groupByDimension", Boolean.valueOf(dimension.groupByDimension));
            item.put("externalizedPredicateDimension", Boolean.valueOf(dimension.externalized));
            item.put("securityPredicateDimension", Boolean.valueOf(dimension.security));
            result.add(item);
        }
        return result;
    }
}
