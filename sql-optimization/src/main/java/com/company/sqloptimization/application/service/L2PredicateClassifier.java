package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class L2PredicateClassifier {

    static final String EXTERNALIZED_PARAMETER_PREDICATE = "EXTERNALIZED_PARAMETER_PREDICATE";
    static final String RETAINED_BUSINESS_PREDICATE = "RETAINED_BUSINESS_PREDICATE";
    static final String SECURITY_PREDICATE = "SECURITY_PREDICATE";
    static final String BLOCKED_UNSTABLE_PREDICATE = "BLOCKED_UNSTABLE_PREDICATE";

    private L2PredicateClassifier() {
    }

    static PredicateClassificationResult classify(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return classify(profile == null ? null : profile.toAdvancedStructureProfile());
    }

    static PredicateClassificationResult classify(Map<String, Object> advancedStructureProfile) {
        return PredicateClassifierEngine.classify(advancedStructureProfile);
    }

    static final class PredicateClassificationResult {

        private final List<Map<String, Object>> externalizedPredicates;
        private final List<Map<String, Object>> retainedPredicates;
        private final List<Map<String, Object>> securityPredicates;
        private final List<Map<String, Object>> blockedPredicates;

        PredicateClassificationResult(List<Map<String, Object>> externalizedPredicates,
                                      List<Map<String, Object>> retainedPredicates,
                                      List<Map<String, Object>> securityPredicates,
                                      List<Map<String, Object>> blockedPredicates) {
            this.externalizedPredicates = immutableCopy(externalizedPredicates);
            this.retainedPredicates = immutableCopy(retainedPredicates);
            this.securityPredicates = immutableCopy(securityPredicates);
            this.blockedPredicates = immutableCopy(blockedPredicates);
        }

        static PredicateClassificationResult empty() {
            return new PredicateClassificationResult(
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList()
            );
        }

        List<Map<String, Object>> getExternalizedPredicates() {
            return externalizedPredicates;
        }

        List<Map<String, Object>> getRetainedPredicates() {
            return retainedPredicates;
        }

        List<Map<String, Object>> getSecurityPredicates() {
            return securityPredicates;
        }

        List<Map<String, Object>> getBlockedPredicates() {
            return blockedPredicates;
        }

        boolean hasBlockedPredicates() {
            return !blockedPredicates.isEmpty();
        }

        private static List<Map<String, Object>> immutableCopy(List<Map<String, Object>> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> item : source) {
                result.add(Collections.unmodifiableMap(new LinkedHashMap<String, Object>(item)));
            }
            return Collections.unmodifiableList(result);
        }
    }
}
