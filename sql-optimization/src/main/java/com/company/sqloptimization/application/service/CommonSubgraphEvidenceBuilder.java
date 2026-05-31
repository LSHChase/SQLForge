package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.CommonSubgraphReplacementMatcher.replacementCount;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

final class CommonSubgraphEvidenceBuilder {

    private CommonSubgraphEvidenceBuilder() {
    }

    static Map<String, Object> commonSubgraphEvidence(
        String sourceSql,
        CommonSubgraphCandidate candidate,
        List<String> outputColumns,
        Set<String> requiredColumns,
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        String fingerprint = CommonSubgraphFingerprint.subgraphFingerprint(candidate.subgraphSql);
        List<Map<String, Object>> matchedSourceRefs = new ArrayList<Map<String, Object>>();
        List<String> matchedSqlFingerprints = new ArrayList<String>();
        addSourceRef(matchedSourceRefs, matchedSqlFingerprints, "CURRENT_SQL", "CURRENT", sourceSql, fingerprint);
        appendPeerSourceRefs(peerSqls, fingerprint, matchedSourceRefs, matchedSqlFingerprints);
        int currentSqlReferenceCount = Math.max(1, replacementCount(sourceSql, candidate));
        int crossSqlReferenceCount = Math.max(0, matchedSourceRefs.size() - 1);

        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("status", "COVERED");
        coverage.put("requiredColumns", new ArrayList<String>(requiredColumns));
        coverage.put("outputColumns", outputColumns);
        coverage.put("rewriteSource", "MV_ONLY");

        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("mode", evidenceMode(currentSqlReferenceCount, crossSqlReferenceCount));
        evidence.put("candidateSelectionSource", "CALCITE_AST_QBDAG_STRUCTURAL_REUSE");
        evidence.put("staticConstantMatchUsed", Boolean.FALSE);
        evidence.put("subgraphFingerprint", fingerprint);
        evidence.put("sourceKind", candidate.sourceKind);
        evidence.put("sourceName", candidate.sourceName);
        evidence.put("alias", candidate.alias);
        evidence.put("materializedCteNames", new ArrayList<String>(candidate.materializedCteNames));
        evidence.put("matchedSqlFingerprints", matchedSqlFingerprints);
        evidence.put("matchedSourceRefs", matchedSourceRefs);
        evidence.put("currentSqlReferenceCount", Integer.valueOf(currentSqlReferenceCount));
        evidence.put("crossSqlReferenceCount", Integer.valueOf(crossSqlReferenceCount));
        evidence.put("referenceCount", Integer.valueOf(currentSqlReferenceCount + crossSqlReferenceCount));
        evidence.put("rewriteReplacementCount", Integer.valueOf(currentSqlReferenceCount));
        evidence.put("outputColumns", outputColumns);
        evidence.put("rewriteCoverage", coverage);
        return evidence;
    }

    private static String evidenceMode(int currentSqlReferenceCount, int crossSqlReferenceCount) {
        if (crossSqlReferenceCount > 0) {
            return "CROSS_SQL_SHARED_SUBGRAPH";
        }
        return currentSqlReferenceCount > 1 ? "SINGLE_SQL_REPEATED_SUBGRAPH" : "SINGLE_SQL_SUBGRAPH";
    }

    private static void appendPeerSourceRefs(List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls,
                                             String fingerprint,
                                             List<Map<String, Object>> matchedSourceRefs,
                                             List<String> matchedSqlFingerprints) {
        if (peerSqls == null) {
            return;
        }
        for (L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peerSql : peerSqls) {
            if (peerSql == null || !StringUtils.hasText(peerSql.getSqlText())) {
                continue;
            }
            for (CommonSubgraphCandidate peerCandidate
                : CommonSubgraphProfileCandidateExtractor.subgraphCandidates(peerSql.getAdvancedStructureProfile())) {
                if (!fingerprint.equals(CommonSubgraphFingerprint.subgraphFingerprint(peerCandidate.subgraphSql))) {
                    continue;
                }
                addSourceRef(
                    matchedSourceRefs,
                    matchedSqlFingerprints,
                    peerSql.getSourceKind(),
                    peerSql.getSourceRef(),
                    peerSql.getSqlText(),
                    fingerprint,
                    peerSql.getSqlFingerprint(),
                    peerSql.getReportCode()
                );
                break;
            }
        }
    }

    private static void addSourceRef(List<Map<String, Object>> refs,
                                     List<String> fingerprints,
                                     String sourceKind,
                                     String sourceRef,
                                     String sqlText,
                                     String subgraphFingerprint) {
        addSourceRef(refs, fingerprints, sourceKind, sourceRef, sqlText, subgraphFingerprint, null, null);
    }

    private static void addSourceRef(List<Map<String, Object>> refs,
                                     List<String> fingerprints,
                                     String sourceKind,
                                     String sourceRef,
                                     String sqlText,
                                     String subgraphFingerprint,
                                     String sqlFingerprint,
                                     String reportCode) {
        String resolvedSqlFingerprint = CommonSubgraphSqlText.firstText(
            sqlFingerprint,
            CommonSubgraphFingerprint.shortSha256(CommonSubgraphFingerprint.canonicalSql(sqlText))
        );
        if (!fingerprints.contains(resolvedSqlFingerprint)) {
            fingerprints.add(resolvedSqlFingerprint);
        }
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("sourceKind", CommonSubgraphSqlText.firstText(sourceKind, "UNKNOWN"));
        item.put("sourceRef", CommonSubgraphSqlText.firstText(sourceRef, "UNKNOWN"));
        item.put("sqlFingerprint", resolvedSqlFingerprint);
        item.put("reportCode", reportCode);
        item.put("subgraphFingerprint", subgraphFingerprint);
        refs.add(item);
    }
}
