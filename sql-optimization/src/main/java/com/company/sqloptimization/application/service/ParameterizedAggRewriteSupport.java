package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.isIdentifierReference;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.ParameterizedAggTextSupport.unqualifiedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

final class ParameterizedAggRewriteSupport {

    private ParameterizedAggRewriteSupport() {
    }

    static String rewriteColumns(String expression, List<ParameterizedAggDimensionSpec> dimensions) {
        String result = expression;
        List<ParameterizedAggColumnReplacement> replacements = new ArrayList<ParameterizedAggColumnReplacement>();
        for (ParameterizedAggDimensionSpec dimension : dimensions) {
            for (String reference : dimension.references) {
                if (StringUtils.hasText(reference) && isIdentifierReference(reference)) {
                    replacements.add(new ParameterizedAggColumnReplacement(reference, dimension.outputName));
                    String unqualified = unqualifiedName(reference);
                    if (!unqualified.equals(reference)) {
                        replacements.add(new ParameterizedAggColumnReplacement(unqualified, dimension.outputName));
                    }
                }
            }
        }
        Collections.sort(replacements, new Comparator<ParameterizedAggColumnReplacement>() {
            @Override
            public int compare(ParameterizedAggColumnReplacement left, ParameterizedAggColumnReplacement right) {
                return Integer.compare(right.source.length(), left.source.length());
            }
        });
        Set<String> applied = new LinkedHashSet<String>();
        for (ParameterizedAggColumnReplacement replacement : replacements) {
            String key = replacement.source + "->" + replacement.target;
            if (!applied.contains(key)) {
                result = replaceIdentifier(result, replacement.source, replacement.target);
                applied.add(key);
            }
        }
        return result;
    }
}
