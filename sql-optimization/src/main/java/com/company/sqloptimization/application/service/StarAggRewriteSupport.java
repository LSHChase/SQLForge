package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.StarAggReferenceSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.StarAggReferenceSupport.unqualifiedName;
import static com.company.sqloptimization.application.service.StarAggSqlTextSupport.replaceIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

final class StarAggRewriteSupport {

    private StarAggRewriteSupport() {
    }

    static String rewriteColumns(String expression, StarAggDimensionPlan dimensionPlan) {
        String result = expression;
        List<StarAggColumnReplacement> replacements = new ArrayList<StarAggColumnReplacement>();
        for (StarAggDimensionSpec dimension : dimensionPlan.dimensions) {
            for (String reference : dimension.references) {
                if (StringUtils.hasText(reference) && isIdentifierReference(reference)) {
                    replacements.add(new StarAggColumnReplacement(reference, dimension.outputName));
                    String unqualified = unqualifiedName(reference);
                    if (!unqualified.equals(reference)) {
                        replacements.add(new StarAggColumnReplacement(unqualified, dimension.outputName));
                    }
                }
            }
        }
        Collections.sort(replacements, new Comparator<StarAggColumnReplacement>() {
            @Override
            public int compare(StarAggColumnReplacement left, StarAggColumnReplacement right) {
                return Integer.compare(right.source.length(), left.source.length());
            }
        });
        Set<String> applied = new LinkedHashSet<String>();
        for (StarAggColumnReplacement replacement : replacements) {
            String key = replacement.source + "->" + replacement.target;
            if (!applied.contains(key)) {
                result = replaceIdentifier(result, replacement.source, replacement.target);
                applied.add(key);
            }
        }
        return result;
    }
}
