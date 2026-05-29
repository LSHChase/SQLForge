package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.cleanReference;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.normalizeExpression;
import static com.company.sqloptimization.application.service.RewriteCoverageSqlText.unqualifiedName;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.StringUtils;

final class RewriteCoverageFields {

    private RewriteCoverageFields() {
    }

    static boolean allFieldsCovered(List<String> fields, Set<String> coverageFields) {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        for (String field : fields) {
            if (!containsCoverageField(coverageFields, field)) {
                return false;
            }
        }
        return true;
    }

    static boolean containsCoverageField(Set<String> coverageFields, String field) {
        if (!StringUtils.hasText(field)) {
            return true;
        }
        String cleaned = cleanReference(field);
        return coverageFields.contains(normalizeExpression(cleaned))
            || coverageFields.contains(normalizeExpression(unqualifiedName(cleaned)));
    }

    static Set<String> combinedCoverage(Set<String> left, Set<String> right) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    static Set<String> coverageFields(L2MaterializedViewRewriteCoverageValidator.ValidationInput input) {
        return coverageFields(input.mvFieldNames, input.additionalCoverageReferences);
    }

    static Set<String> coverageFields(List<String> mvFieldNames, List<String> additionalCoverageReferences) {
        LinkedHashSet<String> fields = new LinkedHashSet<String>();
        addCoverageFields(fields, mvFieldNames);
        addCoverageFields(fields, additionalCoverageReferences);
        return fields;
    }

    static void addCoverageField(Set<String> target, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String cleaned = cleanReference(value);
        target.add(normalizeExpression(cleaned));
        target.add(normalizeExpression(unqualifiedName(cleaned)));
    }

    private static void addCoverageFields(Set<String> target, List<String> values) {
        for (String value : values) {
            addCoverageField(target, value);
        }
    }
}
