package com.company.sqloptimization.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.springframework.util.StringUtils;

final class L2CommonSubgraphMvCandidateGenerator extends L2CommonSubgraphMvCandidateGeneratorRewriteSupport {


    private L2CommonSubgraphMvCandidateGenerator() {
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 Map<String, Object> advancedStructureProfile,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                 List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        List<Map<String, Object>> blockingReasons = structuralBlockingReasons(advancedStructureProfile, profile);
        List<SubgraphCandidate> candidates = mergeCandidates(
            subgraphCandidates(advancedStructureProfile),
            subgraphCandidatesFromAst(sourceSql)
        );
        if (candidates.isEmpty()) {
            blockingReasons.add(commonSubgraphCandidateRequiredReason(profile));
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }

        SubgraphCandidate candidate = chooseCandidate(
            sourceSql,
            candidateSelectableSubgraphs(candidates),
            advancedStructureProfile
        );
        if (candidate == null) {
            candidate = chooseCandidate(sourceSql, candidates, advancedStructureProfile);
        }
        blockingReasons.addAll(candidateBlockingReasons(candidate));
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }
        OutputColumns outputColumns = outputColumns(candidate.subgraphSql);
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
            return CandidateSql.blocked(blockingReasons);
        }

        RewriteResult rewriteResult = rewriteSqlWithEvidence(sourceSql, candidate, mvName);
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
            return CandidateSql.blocked(blockingReasons);
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, candidate.subgraphSql);
        if (renderedSql == null) {
            return CandidateSql.blocked(Collections.singletonList(reason(
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
            return CandidateSql.blocked(validationSql.getBlockingReasons());
        }
        Map<String, Object> evidence = commonSubgraphEvidence(
            sourceSql,
            candidate,
            outputColumns.columns,
            requiredColumns,
            peerSqls
        );
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql.getValidationSql(),
            renderedSql.getRollbackSql(),
            rewriteSql,
            evidence
        );
    }

    private static SubgraphCandidate chooseCandidate(String sourceSql,
                                                     List<SubgraphCandidate> candidates,
                                                     Map<String, Object> advancedStructureProfile) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String mainQuery = extractMainQueryAfterWith(sourceSql);
        Map<String, Integer> fingerprintCounts = fingerprintCounts(candidates);
        Set<String> originalSources = originalBaseSources(advancedStructureProfile);
        SubgraphCandidate originalAvoidingRepeatedCandidate = bestOriginalAvoidingCandidate(
            sourceSql,
            candidates,
            mainQuery,
            fingerprintCounts,
            originalSources,
            true
        );
        if (originalAvoidingRepeatedCandidate != null) {
            return originalAvoidingRepeatedCandidate;
        }
        SubgraphCandidate originalAvoidingCandidate = bestOriginalAvoidingCandidate(
            sourceSql,
            candidates,
            mainQuery,
            fingerprintCounts,
            originalSources,
            false
        );
        if (originalAvoidingCandidate != null) {
            return originalAvoidingCandidate;
        }
        SubgraphCandidate repeatedCandidate = firstCoveredCandidate(
            sourceSql,
            candidates,
            mainQuery,
            fingerprintCounts,
            true
        );
        if (repeatedCandidate != null) {
            return repeatedCandidate;
        }
        SubgraphCandidate coveredCandidate = firstCoveredCandidate(
            sourceSql,
            candidates,
            mainQuery,
            fingerprintCounts,
            false
        );
        if (coveredCandidate != null) {
            return coveredCandidate;
        }
        for (SubgraphCandidate candidate : candidates) {
            if (!"CTE".equals(candidate.sourceKind) || relationReferenced(mainQuery, candidate.sourceName)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private static SubgraphCandidate bestOriginalAvoidingCandidate(String sourceSql,
                                                                   List<SubgraphCandidate> candidates,
                                                                   String mainQuery,
                                                                   Map<String, Integer> fingerprintCounts,
                                                                   Set<String> originalSources,
                                                                   boolean repeatedOnly) {
        if (originalSources == null || originalSources.isEmpty()) {
            return null;
        }
        SubgraphCandidate bestCandidate = null;
        int bestReplacementCount = 0;
        int bestComplexityScore = -1;
        for (SubgraphCandidate candidate : candidates) {
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(sourceSql, candidate.sourceName)) {
                continue;
            }
            int replacementCount = replacementCount(sourceSql, candidate);
            int repeatCount = Math.max(
                intValue(fingerprintCounts.get(subgraphFingerprint(candidate.subgraphSql))),
                replacementCount
            );
            if (repeatedOnly && repeatCount < 2) {
                continue;
            }
            if (!coveredByCandidate(sourceSql, candidate)) {
                continue;
            }
            String rewriteSql = rewriteSql(sourceSql, candidate, "__mv_common_subgraph__");
            if (!StringUtils.hasText(rewriteSql)
                || rewriteSql.equals(trimTrailingSemicolon(sourceSql))
                || accessesOriginalSources(rewriteSql, originalSources)) {
                continue;
            }
            int complexityScore = candidateComplexityScore(candidate);
            if (bestCandidate == null
                || replacementCount > bestReplacementCount
                || (replacementCount == bestReplacementCount && complexityScore > bestComplexityScore)) {
                bestCandidate = candidate;
                bestReplacementCount = replacementCount;
                bestComplexityScore = complexityScore;
            }
        }
        return bestCandidate;
    }

    private static SubgraphCandidate firstCoveredCandidate(String sourceSql,
                                                           List<SubgraphCandidate> candidates,
                                                           String mainQuery,
                                                           Map<String, Integer> fingerprintCounts,
                                                           boolean repeatedOnly) {
        SubgraphCandidate bestCandidate = null;
        int bestReplacementCount = 0;
        for (SubgraphCandidate candidate : candidates) {
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(sourceSql, candidate.sourceName)) {
                continue;
            }
            int replacementCount = replacementCount(sourceSql, candidate);
            if (repeatedOnly
                && intValue(fingerprintCounts.get(subgraphFingerprint(candidate.subgraphSql))) < 2) {
                if (replacementCount < 2) {
                    continue;
                }
            }
            if (coveredByCandidate(sourceSql, candidate)) {
                if (!repeatedOnly) {
                    return candidate;
                }
                if (replacementCount > bestReplacementCount) {
                    bestCandidate = candidate;
                    bestReplacementCount = replacementCount;
                }
            }
        }
        return bestCandidate;
    }

    private static boolean coveredByCandidate(String sourceSql, SubgraphCandidate candidate) {
        OutputColumns outputColumns = outputColumns(candidate.subgraphSql);
        if (!outputColumns.blockingReasons.isEmpty()) {
            return false;
        }
        Set<String> requiredColumns = requiredColumns(sourceSql, candidate);
        OutputColumns coverageColumns = "DERIVED_TABLE".equals(candidate.sourceKind)
            ? withRequiredQualifiedColumns(outputColumns, requiredColumns)
            : outputColumns;
        return !requiredColumns.isEmpty() && coverageColumns.normalizedColumns.containsAll(requiredColumns);
    }

    private static Map<String, Integer> fingerprintCounts(List<SubgraphCandidate> candidates) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (SubgraphCandidate candidate : candidates == null ? Collections.<SubgraphCandidate>emptyList() : candidates) {
            String fingerprint = subgraphFingerprint(candidate.subgraphSql);
            result.put(fingerprint, Integer.valueOf(intValue(result.get(fingerprint)) + 1));
        }
        return result;
    }

    private static int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private static int candidateComplexityScore(SubgraphCandidate candidate) {
        return candidate == null || candidate.subgraphSql == null ? 0 : candidate.subgraphSql.length();
    }

    private static boolean relationReferenced(String sql, String relationName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return false;
        }
        try {
            return relationReferenced(parseStatement(sql), relationName);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static boolean relationReferenced(SqlNode node, String relationName) {
        LinkedHashSet<String> relations = new LinkedHashSet<String>();
        collectRelationReferences(node, relations);
        String expected = relationKey(relationName);
        String unqualified = relationKey(unqualifiedName(relationName));
        return relations.contains(expected) || relations.contains(unqualified);
    }

    private static Set<String> originalBaseSources(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> sources = new LinkedHashSet<String>();
        if (advancedStructureProfile == null) {
            return sources;
        }
        for (Map<String, Object> table : mapList(advancedStructureProfile.get("tables"))) {
            String sourceType = text(table.get("sourceType"));
            if (StringUtils.hasText(sourceType) && !"BASE_TABLE".equalsIgnoreCase(sourceType)) {
                continue;
            }
            addRelationKey(sources, text(table.get("tableName")));
        }
        return sources;
    }

    private static void addRelationKey(Set<String> sources, String relation) {
        if (!StringUtils.hasText(relation)) {
            return;
        }
        sources.add(relationKey(relation));
        sources.add(relationKey(unqualifiedName(relation)));
    }

    private static boolean accessesOriginalSources(String sql, Set<String> originalSources) {
        if (!StringUtils.hasText(sql) || originalSources == null || originalSources.isEmpty()) {
            return false;
        }
        for (String relation : fromJoinRelations(sql)) {
            if ("__UNPARSEABLE_SQL__".equals(relation)) {
                return true;
            }
            String normalized = relationKey(relation);
            String unqualified = relationKey(unqualifiedName(relation));
            if (originalSources.contains(normalized) || originalSources.contains(unqualified)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> fromJoinRelations(String sql) {
        if (!StringUtils.hasText(sql)) {
            return Collections.emptyList();
        }
        try {
            LinkedHashSet<String> relations = new LinkedHashSet<String>();
            collectRelationReferences(parseStatement(sql), relations);
            return new ArrayList<String>(relations);
        } catch (RuntimeException ex) {
            return Collections.singletonList("__UNPARSEABLE_SQL__");
        }
    }

    private static String cleanRelationToken(String value) {
        String cleaned = cleanReference(value);
        while (cleaned.endsWith(")") || cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    protected static String relationKey(String value) {
        return cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String cleanReference(String value) {
        return StringUtils.hasText(value)
            ? value.replace("`", "").replace("\"", "").replaceAll("\\s*\\.\\s*", ".").trim()
            : "";
    }

    protected static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    protected static void collectRelationReferences(SqlNode node, Set<String> relations) {
        if (node == null || relations == null) {
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectRelationReferences(((SqlOrderBy) node).query, relations);
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        collectRelationReferences(((SqlWithItem) item).query, relations);
                    }
                }
            }
            collectRelationReferences(with.body, relations);
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectFromRelationReferences(select.getFrom(), relations);
            collectRelationReferences(select.getWhere(), relations);
            collectRelationReferences(select.getHaving(), relations);
            collectRelationReferences(select.getSelectList(), relations);
            collectRelationReferences(select.getOrderList(), relations);
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRelationReferences(item, relations);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRelationReferences(operand, relations);
            }
        }
    }

    private static void collectFromRelationReferences(SqlNode from, Set<String> relations) {
        if (from == null || relations == null) {
            return;
        }
        if (from instanceof SqlIdentifier) {
            addRelationReference(relations, from.toString());
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectFromRelationReferences(join.getLeft(), relations);
            collectFromRelationReferences(join.getRight(), relations);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (!operands.isEmpty()) {
                SqlNode relation = operands.get(0);
                if (relation instanceof SqlIdentifier) {
                    addRelationReference(relations, relation.toString());
                } else {
                    collectRelationReferences(relation, relations);
                }
            }
            return;
        }
        if (from instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) from).getOperandList()) {
                collectFromRelationReferences(operand, relations);
            }
        }
    }

    private static void addRelationReference(Set<String> relations, String relation) {
        if (!StringUtils.hasText(relation)) {
            return;
        }
        relations.add(relationKey(relation));
        relations.add(relationKey(unqualifiedName(relation)));
    }

    private static List<Map<String, Object>> structuralBlockingReasons(Map<String, Object> advancedStructureProfile,
                                                                       SqlOptimizationPipelineService.ParsedSqlProfile
                                                                           profile) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "缺少高级结构画像，不能生成 COMMON_SUBGRAPH_MV。"
            ));
            return reasons;
        }
        if (!"AVAILABLE".equals(text(advancedStructureProfile.get("profileStatus")))) {
            reasons.add(reason(
                "ADVANCED_STRUCTURE_PROFILE_REQUIRED",
                "高级结构画像未完整可用，不能生成可激活的 COMMON_SUBGRAPH_MV SQL。"
            ));
        }
        if (profile != null && profile.getCorrelatedSubqueryCount() > 0) {
            reasons.add(reason(
                "CORRELATED_SUBQUERY_COMMON_SUBGRAPH_UNSUPPORTED",
                "相关子查询依赖外层作用域，不能独立物化为 COMMON_SUBGRAPH_MV。"
            ));
        }
        return reasons;
    }

    private static boolean hasCteDependency(Map<String, Object> advancedStructureProfile) {
        List<Map<String, Object>> ctes = mapList(advancedStructureProfile.get("ctes"));
        if (ctes.size() < 2) {
            return false;
        }
        List<String> names = new ArrayList<String>();
        for (Map<String, Object> cte : ctes) {
            addIfText(names, text(cte.get("name")));
        }
        for (Map<String, Object> cte : ctes) {
            String currentName = text(cte.get("name"));
            String query = text(cte.get("query"));
            for (String name : names) {
                if (name.equalsIgnoreCase(currentName)) {
                    continue;
                }
                if (relationReferenced(query, name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Map<String, Object> commonSubgraphCandidateRequiredReason(
        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile != null && profile.getRepeatedSubqueryCount() > 0) {
            return reason(
                "REPEATED_SUBQUERY_COMMON_SUBGRAPH_BLOCKED",
                "重复 predicate/scalar 子查询需要证明无关联、单列输出且 rewrite 完全等价，AMV-009 先保守阻断。"
            );
        }
        return reason(
            "COMMON_SUBGRAPH_CANDIDATE_REQUIRED",
            "未找到可独立物化的命名 CTE 或 FROM/JOIN 派生表。"
        );
    }

    protected static List<SubgraphCandidate> subgraphCandidates(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return Collections.emptyList();
        }
        List<SubgraphCandidate> result = new ArrayList<SubgraphCandidate>();
        List<Map<String, Object>> ctes = mapList(advancedStructureProfile.get("ctes"));
        for (int index = 0; index < ctes.size(); index++) {
            Map<String, Object> cte = ctes.get(index);
            String query = normalizeSubgraphSql(text(cte.get("query")));
            if (!StringUtils.hasText(query)) {
                continue;
            }
            LinkedHashSet<String> materializedCteNames = materializedCteNames(ctes, index);
            result.add(new SubgraphCandidate(
                "CTE",
                text(cte.get("name")),
                text(cte.get("name")),
                expandedCteSubgraphSql(ctes, index, materializedCteNames),
                isRecursiveCteCandidate(ctes, index),
                materializedCteNames
            ));
        }
        for (Map<String, Object> subquery : mapList(advancedStructureProfile.get("subqueries"))) {
            String location = upperText(subquery.get("location"));
            if (!"FROM".equals(location)) {
                continue;
            }
            String alias = text(subquery.get("alias"));
            String query = normalizeSubgraphSql(removeTrailingAlias(text(subquery.get("query")), alias));
            if (!StringUtils.hasText(query)) {
                continue;
            }
            result.add(new SubgraphCandidate(
                "DERIVED_TABLE",
                firstText(alias, text(subquery.get("subqueryId"))),
                alias,
                query,
                false
            ));
        }
        return result;
    }

    private static LinkedHashSet<String> materializedCteNames(List<Map<String, Object>> ctes, int candidateIndex) {
        LinkedHashSet<Integer> dependencyIndexes = new LinkedHashSet<Integer>();
        collectCteDependencyIndexes(ctes, candidateIndex, dependencyIndexes, new LinkedHashSet<Integer>());
        dependencyIndexes.add(Integer.valueOf(candidateIndex));
        LinkedHashSet<String> names = new LinkedHashSet<String>();
        for (Integer index : dependencyIndexes) {
            if (index == null || index.intValue() < 0 || index.intValue() >= ctes.size()) {
                continue;
            }
            addIfText(names, cteName(ctes.get(index.intValue())));
        }
        return names;
    }

    private static void collectCteDependencyIndexes(List<Map<String, Object>> ctes,
                                                    int cteIndex,
                                                    LinkedHashSet<Integer> result,
                                                    Set<Integer> visiting) {
        if (ctes == null || cteIndex < 0 || cteIndex >= ctes.size() || visiting.contains(Integer.valueOf(cteIndex))) {
            return;
        }
        visiting.add(Integer.valueOf(cteIndex));
        Map<String, Object> cte = ctes.get(cteIndex);
        LinkedHashSet<String> knownNames = new LinkedHashSet<String>();
        for (int index = 0; index < ctes.size(); index++) {
            if (index == cteIndex) {
                continue;
            }
            addRelationKey(knownNames, cteName(ctes.get(index)));
        }
        for (String dependencyName : referencedCteNames(text(cte.get("query")), knownNames)) {
            int dependencyIndex = findCteIndex(ctes, dependencyName);
            if (dependencyIndex < 0 || dependencyIndex == cteIndex) {
                continue;
            }
            collectCteDependencyIndexes(ctes, dependencyIndex, result, visiting);
            result.add(Integer.valueOf(dependencyIndex));
        }
        visiting.remove(Integer.valueOf(cteIndex));
    }

    private static LinkedHashSet<String> referencedCteNames(String query, Set<String> knownNames) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (!StringUtils.hasText(query) || knownNames == null || knownNames.isEmpty()) {
            return result;
        }
        try {
            LinkedHashSet<String> relations = new LinkedHashSet<String>();
            collectRelationReferences(parseStatement(query), relations);
            for (String relation : relations) {
                if (knownNames.contains(relationKey(relation)) || knownNames.contains(relationKey(unqualifiedName(relation)))) {
                    result.add(unqualifiedName(relation));
                }
            }
        } catch (RuntimeException ex) {
            return result;
        }
        return result;
    }

    private static int findCteIndex(List<Map<String, Object>> ctes, String dependencyName) {
        String expected = relationKey(dependencyName);
        for (int index = 0; index < ctes.size(); index++) {
            String actual = relationKey(cteName(ctes.get(index)));
            if (actual.equals(expected) || relationKey(unqualifiedName(actual)).equals(relationKey(unqualifiedName(expected)))) {
                return index;
            }
        }
        return -1;
    }

    private static String expandedCteSubgraphSql(List<Map<String, Object>> ctes,
                                                 int candidateIndex,
                                                 Set<String> materializedCteNames) {
        String query = normalizeSubgraphSql(text(ctes.get(candidateIndex).get("query")));
        if (materializedCteNames == null || materializedCteNames.size() <= 1) {
            return query;
        }
        String candidateName = cteName(ctes.get(candidateIndex));
        StringBuilder builder = new StringBuilder();
        builder.append("WITH ");
        boolean first = true;
        for (int index = 0; index < ctes.size(); index++) {
            String name = cteName(ctes.get(index));
            if (!containsRelationName(materializedCteNames, name)
                || relationKey(name).equals(relationKey(candidateName))) {
                continue;
            }
            if (!first) {
                builder.append(", ");
            }
            builder.append(cleanIdentifier(name))
                .append(" AS (")
                .append(normalizeSubgraphSql(text(ctes.get(index).get("query"))))
                .append(")");
            first = false;
        }
        if (first) {
            return query;
        }
        builder.append(" ").append(query);
        return builder.toString();
    }

    protected static boolean containsRelationName(Set<String> names, String relationName) {
        if (names == null || !StringUtils.hasText(relationName)) {
            return false;
        }
        String expected = relationKey(relationName);
        String expectedUnqualified = relationKey(unqualifiedName(relationName));
        for (String name : names) {
            String actual = relationKey(name);
            if (actual.equals(expected) || relationKey(unqualifiedName(actual)).equals(expectedUnqualified)) {
                return true;
            }
        }
        return false;
    }

    private static String cteName(Map<String, Object> cte) {
        return text(cte == null ? null : cte.get("name"));
    }

    private static boolean isRecursiveCteCandidate(List<Map<String, Object>> ctes, int index) {
        if (ctes == null || index < 0 || index >= ctes.size()) {
            return false;
        }
        Map<String, Object> cte = ctes.get(index);
        if (!Boolean.TRUE.equals(cte.get("recursive"))) {
            return false;
        }
        String name = cteName(cte);
        String query = text(cte.get("query"));
        if (!StringUtils.hasText(name)) {
            return true;
        }
        if (relationReferenced(query, name)) {
            return true;
        }
        return ctes.size() == 1;
    }

    private static List<SubgraphCandidate> candidateSelectableSubgraphs(List<SubgraphCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<SubgraphCandidate> result = new ArrayList<SubgraphCandidate>();
        for (SubgraphCandidate candidate : candidates) {
            if (candidateBlockingReasons(candidate).isEmpty()) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<Map<String, Object>> candidateBlockingReasons(SubgraphCandidate candidate) {
        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (candidate == null) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_CANDIDATE_REQUIRED",
                "未找到可独立物化的命名 CTE 或 FROM/JOIN 派生表。"
            ));
            return reasons;
        }
        if (candidate.recursive) {
            reasons.add(reason(
                "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED",
                "递归 CTE 不能独立物化为 AMV-009 的公共子图 MV。"
            ));
        }
        SqlNode candidateNode = parseStatementOrNull(candidate.subgraphSql);
        if (candidateNode == null) {
            reasons.add(reason(
                "COMMON_SUBGRAPH_CANDIDATE_PARSE_UNSUPPORTED",
                "公共子图候选无法经 Calcite 重新解析，不能证明其可独立物化。"
            ));
            return reasons;
        }
        if (hasWindowFunction(candidateNode)) {
            reasons.add(reason(
                "WINDOW_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含窗口函数时需要证明窗口作用域不变，AMV-009 默认阻断。"
            ));
        }
        if (hasNonDeterministicFunction(candidateNode)) {
            reasons.add(reason(
                "NON_DETERMINISTIC_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含当前时间、随机或会话函数，缺少稳定化策略时不能物化。"
            ));
        }
        if (hasOrderOrLimit(candidate.subgraphSql)) {
            reasons.add(reason(
                "SUBGRAPH_ORDER_LIMIT_UNSUPPORTED",
                "公共子图内部包含 ORDER BY 或 LIMIT，物化后可能改变排序分页语义。"
            ));
        }
        return reasons;
    }

    private static List<SubgraphCandidate> subgraphCandidatesFromAst(String sourceSql) {
        if (!StringUtils.hasText(sourceSql)) {
            return Collections.emptyList();
        }
        try {
            List<SubgraphCandidate> result = new ArrayList<SubgraphCandidate>();
            collectAstSubgraphCandidates(parseStatement(sourceSql), result, false);
            return result;
        } catch (RuntimeException ex) {
            return Collections.emptyList();
        }
    }

    private static void collectAstSubgraphCandidates(SqlNode node,
                                                     List<SubgraphCandidate> result,
                                                     boolean relationContext) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (operands.size() >= 2) {
                SqlNode relation = operands.get(0);
                SqlSelect select = unwrapSelect(relation);
                String alias = cleanIdentifier(operands.get(1).toString());
                if (relationContext && select != null && StringUtils.hasText(alias)) {
                    String query = normalizeSubgraphSql(relation.toString());
                    if (StringUtils.hasText(query)) {
                        result.add(new SubgraphCandidate(
                            "DERIVED_TABLE",
                            alias,
                            alias,
                            query,
                            false
                        ));
                    }
                }
                collectAstSubgraphCandidates(relation, result, relationContext);
                return;
            }
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectAstSubgraphCandidates(select.getSelectList(), result, false);
            collectAstSubgraphCandidates(select.getFrom(), result, true);
            collectAstSubgraphCandidates(select.getWhere(), result, false);
            collectAstSubgraphCandidates(select.getGroup(), result, false);
            collectAstSubgraphCandidates(select.getHaving(), result, false);
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectAstSubgraphCandidates(item, result, relationContext);
            }
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            collectAstSubgraphCandidates(with.withList, result, false);
            collectAstSubgraphCandidates(with.body, result, false);
            return;
        }
        if (node instanceof SqlOrderBy) {
            collectAstSubgraphCandidates(((SqlOrderBy) node).query, result, relationContext);
            return;
        }
        if (node instanceof SqlCall) {
            boolean childRelationContext = relationContext || node.getKind() == SqlKind.JOIN;
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectAstSubgraphCandidates(operand, result, childRelationContext);
            }
        }
    }

    private static List<SubgraphCandidate> mergeCandidates(List<SubgraphCandidate> primary,
                                                           List<SubgraphCandidate> secondary) {
        List<SubgraphCandidate> result = new ArrayList<SubgraphCandidate>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        addCandidates(result, seen, primary);
        addCandidates(result, seen, secondary);
        return result;
    }

    private static void addCandidates(List<SubgraphCandidate> result,
                                      Set<String> seen,
                                      List<SubgraphCandidate> candidates) {
        for (SubgraphCandidate candidate : candidates == null
            ? Collections.<SubgraphCandidate>emptyList()
            : candidates) {
            String key = candidate.sourceKind + "|" + normalizeIdentifier(candidate.alias)
                + "|" + subgraphFingerprint(candidate.subgraphSql);
            if (seen.add(key)) {
                result.add(candidate);
            }
        }
    }

}
