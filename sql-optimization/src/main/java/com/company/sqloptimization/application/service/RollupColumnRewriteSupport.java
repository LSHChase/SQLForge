package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RollupIdentifierSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.normalizeExpression;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.RollupIdentifierSupport.unqualifiedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RollupColumnRewriteSupport {

    private RollupColumnRewriteSupport() {
    }

    static String rewriteColumns(String expression, List<RollupDimensionSpec> dimensions) {
        String result = expression;
        List<RollupColumnReplacement> replacements = new ArrayList<RollupColumnReplacement>();
        for (RollupDimensionSpec dimension : dimensions) {
            for (String reference : dimension.references) {
                if (StringUtils.hasText(reference) && isIdentifierReference(reference)) {
                    replacements.add(new RollupColumnReplacement(reference, dimension.outputName));
                    String unqualified = unqualifiedName(reference);
                    if (!unqualified.equals(reference)) {
                        replacements.add(new RollupColumnReplacement(unqualified, dimension.outputName));
                    }
                }
            }
        }
        Collections.sort(replacements, new Comparator<RollupColumnReplacement>() {
            @Override
            public int compare(RollupColumnReplacement left, RollupColumnReplacement right) {
                return Integer.compare(right.source.length(), left.source.length());
            }
        });
        Set<String> applied = new LinkedHashSet<String>();
        for (RollupColumnReplacement replacement : replacements) {
            String key = replacement.source + "->" + replacement.target;
            if (applied.contains(key)) {
                continue;
            }
            result = replaceIdentifier(result, replacement.source, replacement.target);
            applied.add(key);
        }
        return result;
    }

    static RollupDimensionSpec findDimension(String expression, List<RollupDimensionSpec> dimensions) {
        String normalized = normalizeExpression(expression);
        for (RollupDimensionSpec dimension : dimensions) {
            if (dimension.matches(normalized)) {
                return dimension;
            }
        }
        return null;
    }

    static RollupDimensionSpec findDimensionBySources(List<String> sourceColumns, List<RollupDimensionSpec> dimensions) {
        for (String sourceColumn : sourceColumns) {
            RollupDimensionSpec dimension = findDimension(sourceColumn, dimensions);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }
}
