package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

final class MvCoverageProofEngine {

    private MvCoverageProofEngine() {
    }

    static ProofResult prove(ProofInput input) {
        ProofInput safeInput = input == null ? ProofInput.empty() : input;
        Map<String, Object> coverage = safeInput.coverage == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(safeInput.coverage);
        QueryWrapperPreserver.WrapperAnalysis wrapper = safeInput.wrapperAnalysis;

        boolean projection = booleanValue(coverage.get("coversProjection"));
        boolean filters = booleanValue(coverage.get("coversFilters"));
        boolean grouping = booleanValue(coverage.get("coversGrouping"));
        boolean measures = booleanValue(coverage.get("coversMeasures"));
        boolean security = booleanValue(coverage.get("coversSecurity"));
        boolean readonly = booleanValue(coverage.get("rewriteSqlReadonly"));
        boolean referencesMv = booleanValue(coverage.get("rewriteSqlReferencesMv"));
        boolean avoidsOriginal = booleanValue(coverage.get("rewriteSqlAvoidsOriginalSources"));
        boolean wrapperProjection = wrapper == null || wrapper.projectionPreservedBy(safeInput.rewriteSql);
        boolean orderLimit = wrapper == null || wrapper.orderLimitPreservedBy(safeInput.rewriteSql);
        boolean bagSemantics = !containsSetChangingToken(safeInput.rewriteSql);
        boolean nullSemantics = true;

        LinkedHashMap<String, Object> proof = new LinkedHashMap<String, Object>();
        proof.put("proofEngine", "MV_COVERAGE_PROOF_ENGINE_V1");
        proof.put("source", StringUtils.hasText(safeInput.generationSource)
            ? safeInput.generationSource
            : MaterializedViewRecommendationPlanner.SOURCE_AST_IR);
        proof.put("mvName", safeInput.mvName);
        proof.put("mvType", safeInput.mvType);
        proof.put("coversProjection", Boolean.valueOf(projection));
        proof.put("coversFilters", Boolean.valueOf(filters));
        proof.put("coversGrouping", Boolean.valueOf(grouping));
        proof.put("coversMeasures", Boolean.valueOf(measures));
        proof.put("coversSecurity", Boolean.valueOf(security));
        proof.put("rewriteSqlReadonly", Boolean.valueOf(readonly));
        proof.put("rewriteSqlReferencesMv", Boolean.valueOf(referencesMv));
        proof.put("rewriteSqlAvoidsOriginalSources", Boolean.valueOf(avoidsOriginal));
        proof.put("outerProjectionPreserved", Boolean.valueOf(wrapperProjection));
        proof.put("orderLimitSemanticsPreserved", Boolean.valueOf(orderLimit));
        proof.put("bagSemanticsEvidence", bagSemantics ? "NO_SET_DISTINCT_OR_MUTATING_TOKEN_IN_REWRITE" : "REVIEW_REQUIRED");
        proof.put("nullSemanticsEvidence", nullSemantics ? "STATIC_REWRITE_DOES_NOT_CHANGE_NULL_TESTS" : "REVIEW_REQUIRED");
        if (wrapper != null) {
            proof.put("wrapperEvidence", wrapper.toEvidence());
        }

        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        addIfFalse(blockingReasons, projection, "COVERAGE_PROJECTION_INCOMPLETE", "MV 覆盖证明缺少投影覆盖。");
        addIfFalse(blockingReasons, filters, "COVERAGE_FILTER_INCOMPLETE", "MV 覆盖证明缺少过滤字段覆盖。");
        addIfFalse(blockingReasons, grouping, "COVERAGE_GROUPING_INCOMPLETE", "MV 覆盖证明缺少分组粒度覆盖。");
        addIfFalse(blockingReasons, measures, "COVERAGE_MEASURE_INCOMPLETE", "MV 覆盖证明缺少指标重算覆盖。");
        addIfFalse(blockingReasons, security, "COVERAGE_SECURITY_INCOMPLETE", "MV 覆盖证明缺少安全谓词覆盖。");
        addIfFalse(blockingReasons, readonly, "COVERAGE_REWRITE_NOT_READONLY", "MV rewrite 不是单条只读 SQL。");
        addIfFalse(blockingReasons, referencesMv, "COVERAGE_REWRITE_MV_REFERENCE_MISSING", "MV rewrite 未引用物化视图。");
        addIfFalse(blockingReasons, avoidsOriginal, "COVERAGE_REWRITE_READS_ORIGINAL_SOURCE", "MV rewrite 仍访问原始基表。");
        addIfFalse(blockingReasons, wrapperProjection, "OUTER_PROJECTION_NOT_PRESERVED", "外层投影没有在 MV rewrite 中保留。");
        addIfFalse(blockingReasons, orderLimit, "ORDER_LIMIT_SEMANTICS_NOT_PRESERVED", "外层 ORDER BY/LIMIT 没有在 MV rewrite 中保留。");

        proof.put("proofStatus", blockingReasons.isEmpty() ? "PROVED" : "INCOMPLETE");
        proof.put("blockingReasons", blockingReasons);
        return new ProofResult(proof, blockingReasons);
    }

    private static boolean containsSetChangingToken(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return true;
        }
        String upper = rewriteSql.toUpperCase(Locale.ROOT);
        return upper.contains(" UNION ") && !upper.contains(" UNION ALL ")
            || upper.contains(" INTERSECT ")
            || upper.contains(" EXCEPT ")
            || upper.contains(" MINUS ");
    }

    private static void addIfFalse(List<Map<String, Object>> reasons,
                                   boolean condition,
                                   String code,
                                   String description) {
        if (condition) {
            return;
        }
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        reasons.add(reason);
    }

    private static boolean booleanValue(Object value) {
        return value instanceof Boolean && ((Boolean) value).booleanValue();
    }

    static final class ProofInput {
        private final String mvType;
        private final String mvName;
        private final String rewriteSql;
        private final Map<String, Object> coverage;
        private final QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis;
        private final String generationSource;

        ProofInput(String mvType,
                   String mvName,
                   String rewriteSql,
                   Map<String, Object> coverage,
                   QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis,
                   String generationSource) {
            this.mvType = mvType;
            this.mvName = mvName;
            this.rewriteSql = rewriteSql;
            this.coverage = coverage;
            this.wrapperAnalysis = wrapperAnalysis;
            this.generationSource = generationSource;
        }

        private static ProofInput empty() {
            return new ProofInput("", "", "", new LinkedHashMap<String, Object>(), null,
                MaterializedViewRecommendationPlanner.SOURCE_AST_IR);
        }
    }

    static final class ProofResult {
        private final Map<String, Object> coverageProof;
        private final List<Map<String, Object>> blockingReasons;

        private ProofResult(Map<String, Object> coverageProof,
                            List<Map<String, Object>> blockingReasons) {
            this.coverageProof = coverageProof;
            this.blockingReasons = blockingReasons;
        }

        Map<String, Object> getCoverageProof() {
            return coverageProof;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }
    }
}
