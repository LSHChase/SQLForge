package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class AccelerationArtifactCoverage {

    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_BLOCKED = "BLOCKED";
    private static final String STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";

    private AccelerationArtifactCoverage() {
    }

    static Map<String, Object> countWrapperCoverage(Map<String, Object> coverage) {
        LinkedHashMap<String, Object> result = coverage == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(coverage);
        result.put("coversProjection", Boolean.TRUE);
        result.put("coversMeasures", Boolean.TRUE);
        result.put("outerCountWrapperRecomputedFromMvRows", Boolean.TRUE);
        return result;
    }

    static List<Map<String, Object>> removeCoverageReasons(List<Map<String, Object>> reasons,
                                                           String... removableCodes) {
        if (reasons == null || reasons.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> removable = new LinkedHashSet<String>();
        if (removableCodes != null) {
            Collections.addAll(removable, removableCodes);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> reason : reasons) {
            if (!removable.contains(text(reason.get("code")))) {
                result.add(reason);
            }
        }
        return result;
    }

    static String ensureTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return sql;
        }
        String trimmed = sql.trim();
        return trimmed.endsWith(";") ? trimmed : trimmed + ";";
    }

    static Map<String, Object> coverageProofUnavailable(
        Map<String, Object> coverage,
        MaterializedViewPlanningEvidence planningEvidence) {
        LinkedHashMap<String, Object> proof = new LinkedHashMap<String, Object>();
        proof.put("proofEngine", "MV_COVERAGE_PROOF_ENGINE_V1");
        proof.put("source", planningEvidence.getGenerationSource());
        proof.put("proofStatus", "NOT_EVALUATED");
        proof.put("coverage", coverage == null ? Collections.emptyMap() : coverage);
        proof.put("wrapperEvidence", planningEvidence.getWrapperAnalysis().toEvidence());
        return proof;
    }

    static Map<String, Object> rewriteComposition(
        String plannedRewriteSql,
        String candidateSubgraphRewriteSql,
        String mvName,
        MaterializedViewPlanningEvidence planningEvidence) {
        LinkedHashMap<String, Object> composition = new LinkedHashMap<String, Object>();
        QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis = planningEvidence.getWrapperAnalysis();
        composition.put("compositionType", wrapperAnalysis.isOuterQueryPreserved()
            ? "OUTER_QUERY_OVER_MV_SUBGRAPH"
            : "ROOT_QUERY_REWRITTEN_TO_MV");
        composition.put("rewriteSqlScope", "FULL_ROOT_QUERY");
        composition.put("candidateSubgraphRewriteSql", wrapperAnalysis.isOuterQueryPreserved()
            ? candidateSubgraphRewriteSql
            : null);
        composition.put("mvName", mvName);
        composition.put("referencesMv", Boolean.valueOf(StringUtils.hasText(plannedRewriteSql)
            && StringUtils.hasText(mvName)
            && plannedRewriteSql.toUpperCase(Locale.ROOT).contains(mvName.toUpperCase(Locale.ROOT))));
        composition.put("outerQueryPreserved", Boolean.valueOf(wrapperAnalysis.isOuterQueryPreserved()));
        composition.put("outerProjectionPreserved", Boolean.valueOf(wrapperAnalysis.projectionPreservedBy(plannedRewriteSql)));
        composition.put("orderLimitPreserved", Boolean.valueOf(wrapperAnalysis.orderLimitPreservedBy(plannedRewriteSql)));
        composition.put("sourceQueryBlockIds", wrapperAnalysis.getSourceQueryBlockIds());
        composition.put("replacedSubgraphId", wrapperAnalysis.getReplacedSubgraphId());
        return composition;
    }

    static String artifactStatus(List<Map<String, Object>> blockingReasons,
                                 List<Map<String, Object>> reviewWarnings) {
        if (blockingReasons != null && !blockingReasons.isEmpty()) {
            return STATUS_BLOCKED;
        }
        if (reviewWarnings != null && !reviewWarnings.isEmpty()) {
            return STATUS_REVIEW_REQUIRED;
        }
        return STATUS_GENERATED;
    }
}
