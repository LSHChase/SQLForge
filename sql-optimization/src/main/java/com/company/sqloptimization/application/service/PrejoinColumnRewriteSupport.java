package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.replaceIdentifier;
import static com.company.sqloptimization.application.service.PrejoinIdentifierSupport.unqualifiedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PrejoinColumnRewriteSupport {

    private PrejoinColumnRewriteSupport() {
    }

    static String rewriteColumns(String expression, PrejoinColumnPlan columnPlan) {
        String result = expression;
        List<PrejoinColumnReplacement> replacements = new ArrayList<PrejoinColumnReplacement>();
        for (PrejoinColumnMapping mapping : columnPlan.mappings) {
            replacements.add(new PrejoinColumnReplacement(mapping.sourceColumn, mapping.outputColumn));
            if (!mapping.ambiguousName) {
                replacements.add(new PrejoinColumnReplacement(unqualifiedName(mapping.sourceColumn), mapping.outputColumn));
            }
        }
        Collections.sort(replacements, new Comparator<PrejoinColumnReplacement>() {
            @Override
            public int compare(PrejoinColumnReplacement left, PrejoinColumnReplacement right) {
                return Integer.compare(right.source.length(), left.source.length());
            }
        });
        Set<String> applied = new LinkedHashSet<String>();
        for (PrejoinColumnReplacement replacement : replacements) {
            String key = replacement.source + "->" + replacement.target;
            if (applied.contains(key)) {
                continue;
            }
            result = replaceIdentifier(result, replacement.source, replacement.target);
            applied.add(key);
        }
        return result;
    }
}
