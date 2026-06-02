package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrAssembler;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagBuilder;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlanBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class MaterializedViewRecommendationPlanner {

    static final String SOURCE_AST_IR = "AST_IR_RELATIONAL_MV_PLANNER";
    static final String SOURCE_AST_IR_WITH_EXPLAIN = "AST_IR_RELATIONAL_MV_PLANNER_WITH_EXPLAIN";

    private MaterializedViewRecommendationPlanner() {
    }

    static PlanningEvidence plan(String sourceSql,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                 String mvName,
                                 HetuPlanAnalysisResult explainResult) {
        long startNanos = System.nanoTime();
        Map<String, Object> advancedProfile = profile == null
            ? Collections.<String, Object>emptyMap()
            : profile.toAdvancedStructureProfile();
        QueryBlockDag queryBlockDag = null;
        RelationalRewritePlan relationalPlan = null;
        RewriteCoreIrSnapshot coreIr = null;
        List<Map<String, Object>> plannerWarnings = new ArrayList<Map<String, Object>>();
        try {
            if (profile != null) {
                queryBlockDag = new QueryBlockDagBuilder().build(profile.getNormalizedSql(), advancedProfile);
                relationalPlan = new RelationalRewritePlanBuilder().build(queryBlockDag);
                coreIr = new RewriteCoreIrAssembler().assemble(
                    profile.getNormalizedSql(),
                    profile.getParserEngine(),
                    advancedProfile
                );
            }
        } catch (RuntimeException ex) {
            plannerWarnings.add(warning(
                "AST_IR_RELATIONAL_PLANNER_PARTIAL",
                "AST/IR/QBDAG/关系代数规划证据不完整，候选只能依赖已解析结构继续阻断或人工复核。",
                ex.getMessage()
            ));
        }
        QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis =
            QueryWrapperPreserver.analyze(sourceSql, queryBlockDag);
        Map<String, Object> explainEvidence = explainEvidence(explainResult);
        Map<String, Object> metadataEvidence = metadataEvidence(advancedProfile, queryBlockDag);
        String generationSource = "SUCCESS".equals(explainEvidence.get("status"))
            ? SOURCE_AST_IR_WITH_EXPLAIN
            : SOURCE_AST_IR;
        Map<String, Object> plannerEvidence = plannerEvidence(
            profile,
            coreIr,
            queryBlockDag,
            relationalPlan,
            wrapperAnalysis,
            elapsedMillis(startNanos),
            generationSource,
            plannerWarnings
        );
        return new PlanningEvidence(
            candidateId(sourceSql, mvName),
            generationSource,
            wrapperAnalysis,
            explainEvidence,
            metadataEvidence,
            plannerEvidence,
            plannerWarnings
        );
    }

    private static Map<String, Object> plannerEvidence(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                       RewriteCoreIrSnapshot coreIr,
                                                       QueryBlockDag queryBlockDag,
                                                       RelationalRewritePlan relationalPlan,
                                                       QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis,
                                                       long elapsedMillis,
                                                       String generationSource,
                                                       List<Map<String, Object>> plannerWarnings) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("source", generationSource);
        evidence.put("parserEngine", profile == null ? "" : profile.getParserEngine());
        evidence.put("schemaVersion", coreIr == null ? RewriteCoreIrSnapshot.SCHEMA_VERSION : coreIr.getSchemaVersion());
        evidence.put("queryBlockDagSchemaVersion", queryBlockDag == null ? QueryBlockDag.SCHEMA_VERSION : queryBlockDag.getSchemaVersion());
        evidence.put("relationalRewritePlanSchemaVersion", relationalPlan == null
            ? RelationalRewritePlan.SCHEMA_VERSION
            : relationalPlan.getSchemaVersion());
        evidence.put("sourceQueryBlockIds", wrapperAnalysis.getSourceQueryBlockIds());
        evidence.put("replacedSubgraphId", wrapperAnalysis.getReplacedSubgraphId());
        evidence.put("queryBlockCount", Integer.valueOf(queryBlockDag == null ? 0 : queryBlockDag.getBlocks().size()));
        evidence.put("relationalCandidateCount", Integer.valueOf(relationalPlan == null ? 0 : relationalPlan.getCandidates().size()));
        evidence.put("relationalCandidates", relationalCandidates(relationalPlan));
        evidence.put("duplicateStructuralBlocks", Boolean.valueOf(queryBlockDag != null && queryBlockDag.hasDuplicateStructuralBlocks()));
        evidence.put("outerQueryWrapper", wrapperAnalysis.toEvidence());
        evidence.put("plannerElapsedMs", Long.valueOf(elapsedMillis));
        evidence.put("runtimeBoundary", "NO_SQL_EXECUTION");
        evidence.put("plannerWarnings", plannerWarnings);
        return evidence;
    }

    private static List<Map<String, Object>> relationalCandidates(RelationalRewritePlan relationalPlan) {
        if (relationalPlan == null || relationalPlan.getCandidates().isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RelationalRewriteCandidate candidate : relationalPlan.getCandidates()) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("candidateId", candidate.getCandidateId());
            item.put("ruleType", candidate.getRuleType() == null ? "" : candidate.getRuleType().name());
            item.put("primaryBlockId", candidate.getPrimaryBlockId());
            item.put("sourceBlockIds", candidate.getSourceBlockIds());
            item.put("manualReviewRequired", Boolean.valueOf(candidate.isManualReviewRequired()));
            result.add(item);
        }
        return result;
    }

    private static Map<String, Object> explainEvidence(HetuPlanAnalysisResult explainResult) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        if (explainResult == null) {
            evidence.put("status", "EXPLAIN_UNAVAILABLE");
            evidence.put("reason", "HETU_EXPLAIN_NOT_REQUESTED");
            evidence.put("evidence", Collections.singletonList("repoClosedFallback=EXPLAIN_UNAVAILABLE"));
            return evidence;
        }
        String status = explainResult.getStatus() == null ? "EXPLAIN_UNAVAILABLE" : explainResult.getStatus().name();
        evidence.put("status", status);
        evidence.put("datasourceCode", explainResult.getDatasourceCode());
        evidence.put("costMs", explainResult.getCostMs());
        evidence.put("failureReason", explainResult.getFailureReason());
        evidence.put("evidence", explainResult.getEvidence());
        evidence.put("planTextDigest", digest(explainResult.getPlanText()));
        evidence.put("planTextLength", Integer.valueOf(explainResult.getPlanText() == null ? 0 : explainResult.getPlanText().length()));
        if (!"SUCCESS".equals(status)) {
            evidence.put("marker", "EXPLAIN_UNAVAILABLE");
        }
        return evidence;
    }

    private static Map<String, Object> metadataEvidence(Map<String, Object> advancedProfile,
                                                        QueryBlockDag queryBlockDag) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("status", "METADATA_PARTIAL");
        evidence.put("metadataSource", "PARSED_SQL_PROFILE_AND_QBDAG");
        evidence.put("tables", mapList(advancedProfile.get("tables")));
        evidence.put("columns", referencedColumns(advancedProfile, queryBlockDag));
        evidence.put("missingEvidence", list(
            "TABLE_STATISTICS",
            "PARTITION_KEYS",
            "PRIMARY_FOREIGN_KEYS",
            "UNIQUE_KEYS",
            "COLUMN_NULLABILITY",
            "JOIN_CARDINALITY",
            "SCAN_SHUFFLE_ESTIMATE"
        ));
        evidence.put("runtimeBoundary", "NO_PRODUCTION_METADATA_READ");
        return evidence;
    }

    private static List<String> referencedColumns(Map<String, Object> advancedProfile,
                                                  QueryBlockDag queryBlockDag) {
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        collectSourceColumns(columns, mapList(advancedProfile.get("projections")));
        collectSourceColumns(columns, mapList(advancedProfile.get("predicates")));
        collectSourceColumns(columns, mapList(advancedProfile.get("groupBy")));
        collectSourceColumns(columns, mapList(advancedProfile.get("aggregations")));
        if (queryBlockDag != null) {
            for (QueryBlockNode block : queryBlockDag.getBlocks()) {
                columns.addAll(block.getOutputColumns());
                columns.addAll(block.getGroupBy());
            }
        }
        return new ArrayList<String>(columns);
    }

    private static void collectSourceColumns(Set<String> target, List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            for (String column : stringList(item.get("sourceColumns"))) {
                if (StringUtils.hasText(column)) {
                    target.add(column);
                }
            }
        }
    }

    private static Map<String, Object> warning(String code, String description, String detail) {
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", code);
        warning.put("description", description);
        warning.put("detail", detail == null ? "" : detail);
        return warning;
    }

    private static List<String> list(String... values) {
        List<String> result = new ArrayList<String>();
        if (values == null) {
            return result;
        }
        Collections.addAll(result, values);
        return result;
    }

    private static String candidateId(String sourceSql, String mvName) {
        return "mv_candidate_" + digest(firstText(mvName, "") + "\n" + firstText(sourceSql, "")).substring(0, 16);
    }

    private static String digest(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(item & 0xff)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1000000L;
    }

    private static String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (List<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (Object item : (Iterable<?>) value) {
            if (item != null && StringUtils.hasText(String.valueOf(item))) {
                result.add(String.valueOf(item).trim());
            }
        }
        return result;
    }

    static final class PlanningEvidence {
        private final String candidateId;
        private final String generationSource;
        private final QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis;
        private final Map<String, Object> explainEvidence;
        private final Map<String, Object> metadataEvidence;
        private final Map<String, Object> plannerEvidence;
        private final List<Map<String, Object>> plannerWarnings;

        private PlanningEvidence(String candidateId,
                                 String generationSource,
                                 QueryWrapperPreserver.WrapperAnalysis wrapperAnalysis,
                                 Map<String, Object> explainEvidence,
                                 Map<String, Object> metadataEvidence,
                                 Map<String, Object> plannerEvidence,
                                 List<Map<String, Object>> plannerWarnings) {
            this.candidateId = candidateId;
            this.generationSource = generationSource;
            this.wrapperAnalysis = wrapperAnalysis;
            this.explainEvidence = explainEvidence;
            this.metadataEvidence = metadataEvidence;
            this.plannerEvidence = plannerEvidence;
            this.plannerWarnings = plannerWarnings == null
                ? Collections.<Map<String, Object>>emptyList()
                : plannerWarnings;
        }

        String getCandidateId() {
            return candidateId;
        }

        String getGenerationSource() {
            return generationSource;
        }

        QueryWrapperPreserver.WrapperAnalysis getWrapperAnalysis() {
            return wrapperAnalysis;
        }

        Map<String, Object> getExplainEvidence() {
            return explainEvidence;
        }

        Map<String, Object> getMetadataEvidence() {
            return metadataEvidence;
        }

        Map<String, Object> getPlannerEvidence() {
            return plannerEvidence;
        }

        List<Map<String, Object>> getPlannerWarnings() {
            return plannerWarnings;
        }
    }
}
