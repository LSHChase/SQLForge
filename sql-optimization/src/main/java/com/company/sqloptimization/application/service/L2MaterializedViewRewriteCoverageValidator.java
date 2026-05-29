package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.immutableMapList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.immutableStringList;
import static com.company.sqloptimization.application.service.RewriteCoverageProfileValues.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

final class L2MaterializedViewRewriteCoverageValidator {

    static final String REWRITE_SQL_NOT_READONLY = "REWRITE_SQL_NOT_READONLY";
    static final String REWRITE_SQL_MV_REFERENCE_REQUIRED = "REWRITE_SQL_MV_REFERENCE_REQUIRED";
    static final String REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE = "REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE";
    static final String REWRITE_PROJECTION_NOT_COVERED = "REWRITE_PROJECTION_NOT_COVERED";
    static final String REWRITE_FILTER_NOT_COVERED = "REWRITE_FILTER_NOT_COVERED";
    static final String REWRITE_GROUPING_NOT_COVERED = "REWRITE_GROUPING_NOT_COVERED";
    static final String REWRITE_MEASURE_NOT_COVERED = "REWRITE_MEASURE_NOT_COVERED";
    static final String REWRITE_SECURITY_PREDICATE_NOT_COVERED = "REWRITE_SECURITY_PREDICATE_NOT_COVERED";

    private L2MaterializedViewRewriteCoverageValidator() {
    }

    static ValidationResult validate(ValidationInput input) {
        return RewriteCoverageValidationEngine.validate(input);
    }

    static final class ValidationInput {
        final String sourceSql;
        final String mvType;
        final String mvName;
        final String rewriteSql;
        final Map<String, Object> advancedStructureProfile;
        final L2PredicateClassifier.PredicateClassificationResult predicateClassification;
        final List<Map<String, Object>> measures;
        final List<String> mvFieldNames;
        final List<String> additionalCoverageReferences;

        ValidationInput(String sourceSql,
                        String mvType,
                        String mvName,
                        String rewriteSql,
                        Map<String, Object> advancedStructureProfile,
                        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                        List<Map<String, Object>> measures,
                        List<String> mvFieldNames,
                        List<String> additionalCoverageReferences) {
            this.sourceSql = text(sourceSql);
            this.mvType = text(mvType);
            this.mvName = text(mvName);
            this.rewriteSql = text(rewriteSql);
            this.advancedStructureProfile = advancedStructureProfile == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<String, Object>(advancedStructureProfile);
            this.predicateClassification = predicateClassification;
            this.measures = immutableMapList(measures);
            this.mvFieldNames = immutableStringList(mvFieldNames);
            this.additionalCoverageReferences = immutableStringList(additionalCoverageReferences);
        }

        static ValidationInput empty() {
            return new ValidationInput(
                "",
                "",
                "",
                "",
                Collections.<String, Object>emptyMap(),
                null,
                Collections.<Map<String, Object>>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
            );
        }
    }

    static final class ValidationResult {
        private final String rewriteSql;
        private final Map<String, Object> coverage;
        private final List<Map<String, Object>> blockingReasons;

        ValidationResult(String rewriteSql,
                         Map<String, Object> coverage,
                         List<Map<String, Object>> blockingReasons) {
            this.rewriteSql = rewriteSql;
            this.coverage = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(coverage));
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        Map<String, Object> getCoverage() {
            return coverage;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        boolean isGenerated() {
            return blockingReasons.isEmpty() && StringUtils.hasText(rewriteSql);
        }
    }
}
