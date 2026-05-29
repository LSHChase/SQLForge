package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.CommonSubgraphFingerprint.subgraphFingerprint;
import static com.company.sqloptimization.application.service.CommonSubgraphEvidenceBuilder.commonSubgraphEvidence;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidatePolicy.candidateBlockingReasons;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidatePolicy.candidateSelectableSubgraphs;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidatePolicy.commonSubgraphCandidateRequiredReason;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidatePolicy.structuralBlockingReasons;
import static com.company.sqloptimization.application.service.CommonSubgraphAstCandidateExtractor.subgraphCandidatesFromAst;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidateMerger.mergeCandidates;
import static com.company.sqloptimization.application.service.CommonSubgraphCandidateSelector.chooseCandidate;
import static com.company.sqloptimization.application.service.CommonSubgraphProfileCandidateExtractor.subgraphCandidates;
import static com.company.sqloptimization.application.service.CommonSubgraphOutputColumnAnalyzer.outputColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphOutputColumnAnalyzer.withRequiredQualifiedColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphRequiredColumnAnalyzer.requiredColumns;
import static com.company.sqloptimization.application.service.CommonSubgraphSqlRewriter.rewriteSqlWithEvidence;
import static com.company.sqloptimization.application.service.CommonSubgraphReason.reason;
import static com.company.sqloptimization.application.service.CommonSubgraphSqlText.trimTrailingSemicolon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class L2CommonSubgraphMvCandidateGenerator {

    private L2CommonSubgraphMvCandidateGenerator() {
    }

    static CommonSubgraphCandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 Map<String, Object> advancedStructureProfile,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                 List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        List<Map<String, Object>> blockingReasons = structuralBlockingReasons(advancedStructureProfile, profile);
        List<CommonSubgraphCandidate> candidates = mergeCandidates(
            subgraphCandidates(advancedStructureProfile),
            subgraphCandidatesFromAst(sourceSql)
        );
        if (candidates.isEmpty()) {
            blockingReasons.add(commonSubgraphCandidateRequiredReason(profile));
        }
        if (!blockingReasons.isEmpty()) {
            return CommonSubgraphCandidateSql.blocked(blockingReasons);
        }

        CommonSubgraphCandidate candidate = chooseCandidate(
            sourceSql,
            candidateSelectableSubgraphs(candidates),
            advancedStructureProfile
        );
        if (candidate == null) {
            candidate = chooseCandidate(sourceSql, candidates, advancedStructureProfile);
        }
        blockingReasons.addAll(candidateBlockingReasons(candidate));
        if (!blockingReasons.isEmpty()) {
            return CommonSubgraphCandidateSql.blocked(blockingReasons);
        }
        CommonSubgraphOutputColumns outputColumns = outputColumns(candidate.subgraphSql);
        blockingReasons.addAll(outputColumns.blockingReasons);
        if (blockingReasons.isEmpty() && outputColumns.columns.isEmpty()) {
            blockingReasons.add(reason(
                "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                "公共子图输出字段无法解析，不能证明上层查询可由 MV 覆盖。"
            ));
        }
        Set<String> requiredColumns = requiredColumns(sourceSql, candidate);
        if ("DERIVED_TABLE".equals(candidate.sourceKind)) {
            outputColumns = withRequiredQualifiedColumns(outputColumns, requiredColumns);
        }
        if (blockingReasons.isEmpty() && !outputColumns.normalizedColumns.containsAll(requiredColumns)) {
            LinkedHashSet<String> missing = new LinkedHashSet<String>(requiredColumns);
            missing.removeAll(outputColumns.normalizedColumns);
            Map<String, Object> reason = reason(
                "SUBGRAPH_OUTPUT_NOT_COVERED",
                "公共子图输出字段不能覆盖上层投影、过滤或分组，不能生成可激活 rewrite。"
            );
            reason.put("missingColumns", new ArrayList<String>(missing));
            reason.put("requiredColumns", new ArrayList<String>(requiredColumns));
            reason.put("outputColumns", outputColumns.columns);
            blockingReasons.add(reason);
        }
        if (!blockingReasons.isEmpty()) {
            return CommonSubgraphCandidateSql.blocked(blockingReasons);
        }

        CommonSubgraphRewriteResult rewriteResult = rewriteSqlWithEvidence(sourceSql, candidate, mvName);
        String rewriteSql = rewriteResult.sql;
        if (!StringUtils.hasText(rewriteSql) || rewriteSql.equals(trimTrailingSemicolon(sourceSql))) {
            Map<String, Object> rewriteUnsupportedReason = reason(
                "COMMON_SUBGRAPH_REWRITE_UNSUPPORTED",
                "无法把上层查询来源稳定替换为公共子图 MV。"
            );
            rewriteUnsupportedReason.put("sourceKind", candidate.sourceKind);
            rewriteUnsupportedReason.put("sourceName", candidate.sourceName);
            rewriteUnsupportedReason.put("alias", candidate.alias);
            rewriteUnsupportedReason.put("subgraphFingerprint", subgraphFingerprint(candidate.subgraphSql));
            rewriteUnsupportedReason.put("rewriteAttempts", rewriteResult.rewriteAttempts);
            blockingReasons.add(rewriteUnsupportedReason);
            return CommonSubgraphCandidateSql.blocked(blockingReasons);
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, candidate.subgraphSql);
        if (renderedSql == null) {
            return CommonSubgraphCandidateSql.blocked(Collections.singletonList(reason(
                "UNSUPPORTED_TARGET_ENGINE",
                "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"
            )));
        }
        L2GrainMeasureDeriver.DerivationResult validationDerivation =
            L2GrainMeasureDeriver.derive(
                advancedStructureProfile,
                L2PredicateClassifier.classify(advancedStructureProfile)
            );
        L2MaterializedViewValidationSqlBuilder.ValidationSqlResult validationSql =
            L2MaterializedViewValidationSqlBuilder.build(
                new L2MaterializedViewValidationSqlBuilder.ValidationInput(
                    L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH,
                    sourceSql,
                    rewriteSql,
                    mvName,
                    advancedStructureProfile,
                    validationDerivation.getMeasures(),
                    candidate.subgraphSql,
                    outputColumns.columns
                )
            );
        if (!validationSql.isGenerated()) {
            return CommonSubgraphCandidateSql.blocked(validationSql.getBlockingReasons());
        }
        Map<String, Object> evidence = commonSubgraphEvidence(
            sourceSql,
            candidate,
            outputColumns.columns,
            requiredColumns,
            peerSqls
        );
        return CommonSubgraphCandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql,
            evidence
        );
    }

}
