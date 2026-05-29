package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.MaterializedViewPlanningValues.digest;
import static com.company.sqloptimization.application.service.MaterializedViewPlanningValues.firstText;
import static com.company.sqloptimization.application.service.MaterializedViewPlanningValues.list;
import static com.company.sqloptimization.application.service.MaterializedViewPlanningValues.mapList;
import static com.company.sqloptimization.application.service.MaterializedViewPlanningValues.stringList;

import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class MaterializedViewPlanningEvidenceBuilder {

    private MaterializedViewPlanningEvidenceBuilder() {
    }

    static Map<String, Object> planner(SqlOptimizationPipelineService.ParsedSqlProfile profile,
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
        evidence.put("schemaVersion", coreIr == null
            ? RewriteCoreIrSnapshot.SCHEMA_VERSION
            : coreIr.getSchemaVersion());
        evidence.put("queryBlockDagSchemaVersion", queryBlockDag == null
            ? QueryBlockDag.SCHEMA_VERSION
            : queryBlockDag.getSchemaVersion());
        evidence.put("relationalRewritePlanSchemaVersion", relationalPlan == null
            ? RelationalRewritePlan.SCHEMA_VERSION
            : relationalPlan.getSchemaVersion());
        evidence.put("sourceQueryBlockIds", wrapperAnalysis.getSourceQueryBlockIds());
        evidence.put("replacedSubgraphId", wrapperAnalysis.getReplacedSubgraphId());
        evidence.put("queryBlockCount", Integer.valueOf(queryBlockDag == null ? 0 : queryBlockDag.getBlocks().size()));
        evidence.put("relationalCandidateCount", Integer.valueOf(relationalPlan == null
            ? 0
            : relationalPlan.getCandidates().size()));
        evidence.put("relationalCandidates", relationalCandidates(relationalPlan));
        evidence.put("duplicateStructuralBlocks", Boolean.valueOf(queryBlockDag != null
            && queryBlockDag.hasDuplicateStructuralBlocks()));
        evidence.put("outerQueryWrapper", wrapperAnalysis.toEvidence());
        evidence.put("plannerElapsedMs", Long.valueOf(elapsedMillis));
        evidence.put("runtimeBoundary", "NO_SQL_EXECUTION");
        evidence.put("plannerWarnings", plannerWarnings);
        return evidence;
    }

    static Map<String, Object> explain(HetuPlanAnalysisResult explainResult) {
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
        evidence.put("planTextLength", Integer.valueOf(explainResult.getPlanText() == null
            ? 0
            : explainResult.getPlanText().length()));
        if (!"SUCCESS".equals(status)) {
            evidence.put("marker", "EXPLAIN_UNAVAILABLE");
        }
        return evidence;
    }

    static Map<String, Object> metadata(Map<String, Object> advancedProfile,
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

    static Map<String, Object> warning(String code, String description, String detail) {
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", code);
        warning.put("description", description);
        warning.put("detail", detail == null ? "" : detail);
        return warning;
    }

    static String candidateId(String sourceSql, String mvName) {
        return "mv_candidate_" + digest(firstText(mvName, "") + "\n" + firstText(sourceSql, "")).substring(0, 16);
    }

    static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1000000L;
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
}
