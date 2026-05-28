package com.company.sqloptimization.application.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlSetOperator;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.util.StringUtils;

final class L2CommonSubgraphMvCandidateGenerator {

    private static final Pattern QUALIFIED_COLUMN_PATTERN =
        Pattern.compile("(?i)\\b([A-Z_][A-Z0-9_$]*)\\.([A-Z_][A-Z0-9_$]*)\\b");
    private static final Pattern QUOTED_QUALIFIED_COLUMN_PATTERN =
        Pattern.compile("\"([^\"]+)\"\\s*\\.\\s*\"([^\"]+)\"");
    private static final Pattern IDENTIFIER_PATTERN =
        Pattern.compile("(?i)\\b[A-Z_][A-Z0-9_$]*\\b");
    private static final Pattern RELATION_PATTERN = Pattern.compile(
        "(?i)\\b(FROM|JOIN)\\s+((?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*)(?:\\s*\\.\\s*(?:[`\\\"][^`\\\"]+[`\\\"]|[A-Z_][A-Z0-9_$]*))*)"
    );
    private static final Set<String> SQL_KEYWORDS = new LinkedHashSet<String>(Arrays.asList(
        "SELECT", "FROM", "WHERE", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "ON", "AS",
        "AND", "OR", "NOT", "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "BETWEEN", "IN", "IS",
        "NULL", "DATE", "TIMESTAMP", "CASE", "WHEN", "THEN", "ELSE", "END", "DISTINCT", "TRUE",
        "FALSE", "WITH", "UNION", "ALL", "CAST", "OVER", "PARTITION"
    ));
    private static final Set<String> SQL_FUNCTIONS = new LinkedHashSet<String>(Arrays.asList(
        "SUM", "COUNT", "MIN", "MAX", "AVG", "DATE_TRUNC", "TRUNC", "DATE_FORMAT", "YEAR", "MONTH",
        "DAY", "COALESCE", "NULLIF", "ROUND", "LOWER", "UPPER"
    ));
    private static final int FINGERPRINT_CACHE_MAX_SIZE = 1024;
    private static final Map<String, String> SUBGRAPH_FINGERPRINT_CACHE =
        Collections.synchronizedMap(new LinkedHashMap<String, String>(FINGERPRINT_CACHE_MAX_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > FINGERPRINT_CACHE_MAX_SIZE;
            }
        });

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

        SubgraphCandidate candidate = chooseCandidate(sourceSql, candidates, advancedStructureProfile);
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
        for (SubgraphCandidate candidate : candidates) {
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(mainQuery, candidate.sourceName)) {
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
            if (bestCandidate == null || replacementCount > bestReplacementCount) {
                bestCandidate = candidate;
                bestReplacementCount = replacementCount;
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
            if ("CTE".equals(candidate.sourceKind) && !relationReferenced(mainQuery, candidate.sourceName)) {
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

    private static String relationKey(String value) {
        return cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String cleanReference(String value) {
        return StringUtils.hasText(value)
            ? value.replace("`", "").replace("\"", "").replaceAll("\\s*\\.\\s*", ".").trim()
            : "";
    }

    private static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    private static void collectRelationReferences(SqlNode node, Set<String> relations) {
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
        if (profile != null && profile.getWindowFunctionCount() > 0) {
            reasons.add(reason(
                "WINDOW_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含窗口函数时需要证明窗口作用域不变，AMV-009 默认阻断。"
            ));
        }
        if (profile != null && profile.getCorrelatedSubqueryCount() > 0) {
            reasons.add(reason(
                "CORRELATED_SUBQUERY_COMMON_SUBGRAPH_UNSUPPORTED",
                "相关子查询依赖外层作用域，不能独立物化为 COMMON_SUBGRAPH_MV。"
            ));
        }
        if (!mapList(advancedStructureProfile.get("nonDeterministicFunctions")).isEmpty()) {
            reasons.add(reason(
                "NON_DETERMINISTIC_FUNCTION_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图包含当前时间、随机或会话函数，缺少稳定化策略时不能物化。"
            ));
        }
        for (Map<String, Object> cte : mapList(advancedStructureProfile.get("ctes"))) {
            if (Boolean.TRUE.equals(cte.get("recursive"))) {
                reasons.add(reason(
                    "RECURSIVE_CTE_COMMON_SUBGRAPH_UNSUPPORTED",
                    "递归 CTE 不能独立物化为 AMV-009 的公共子图 MV。"
                ));
            }
        }
        if (hasSubgraphOrderOrLimit(advancedStructureProfile)) {
            reasons.add(reason(
                "SUBGRAPH_ORDER_LIMIT_UNSUPPORTED",
                "公共子图内部包含 ORDER BY 或 LIMIT，物化后可能改变排序分页语义。"
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

    private static boolean hasSubgraphOrderOrLimit(Map<String, Object> advancedStructureProfile) {
        for (Map<String, Object> cte : mapList(advancedStructureProfile.get("ctes"))) {
            if (hasOrderOrLimit(text(cte.get("query")))) {
                return true;
            }
        }
        for (Map<String, Object> subquery : mapList(advancedStructureProfile.get("subqueries"))) {
            if ("FROM".equals(upperText(subquery.get("location"))) && hasOrderOrLimit(text(subquery.get("query")))) {
                return true;
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

    private static List<SubgraphCandidate> subgraphCandidates(Map<String, Object> advancedStructureProfile) {
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return Collections.emptyList();
        }
        List<SubgraphCandidate> result = new ArrayList<SubgraphCandidate>();
        for (Map<String, Object> cte : mapList(advancedStructureProfile.get("ctes"))) {
            String query = normalizeSubgraphSql(text(cte.get("query")));
            if (!StringUtils.hasText(query)) {
                continue;
            }
            result.add(new SubgraphCandidate(
                "CTE",
                text(cte.get("name")),
                text(cte.get("name")),
                query,
                Boolean.TRUE.equals(cte.get("recursive"))
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
        List<SubgraphCandidate> safe = new ArrayList<SubgraphCandidate>();
        for (SubgraphCandidate candidate : result) {
            if (candidate.recursive) {
                continue;
            }
            if (hasOrderOrLimit(candidate.subgraphSql)) {
                continue;
            }
            safe.add(candidate);
        }
        return safe;
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

    private static OutputColumns outputColumns(String subgraphSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        try {
            SqlSelect select = parseSelect(subgraphSql);
            SqlNodeList selectList = select == null ? null : select.getSelectList();
            if (selectList == null) {
                return outputColumnsByText(subgraphSql);
            }
            for (SqlNode item : selectList.getList()) {
                if (isStar(item)) {
                    blockingReasons.add(reason(
                        "EXPLICIT_PROJECTION_REQUIRED",
                        "公共子图包含 SELECT *，需要先展开字段后才能生成可审查物化视图。"
                    ));
                    continue;
                }
                String output = aliasName(item);
                if (!StringUtils.hasText(output)) {
                    SqlNode expression = stripAlias(item);
                    if (expression instanceof SqlIdentifier) {
                        output = identifierTail((SqlIdentifier) expression);
                    }
                }
                if (!StringUtils.hasText(output)) {
                    blockingReasons.add(reason(
                        "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                        "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。"
                    ));
                    continue;
                }
                columns.add(cleanIdentifier(output));
            }
            OutputColumns textColumns = outputColumnsByText(subgraphSql);
            columns.addAll(textColumns.columns);
        } catch (RuntimeException ex) {
            return outputColumnsByText(subgraphSql);
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(new ArrayList<String>(columns), normalized, blockingReasons);
    }

    private static OutputColumns outputColumnsByText(String subgraphSql) {
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        String firstSelectList = firstSelectList(subgraphSql);
        if (!StringUtils.hasText(firstSelectList)) {
            return new OutputColumns(
                Collections.<String>emptyList(),
                Collections.<String>emptySet(),
                Collections.singletonList(reason(
                    "SUBGRAPH_SELECT_UNSUPPORTED",
                    "公共子图 SQL 无法解析出可证明的 SELECT 输出字段。"
                ))
            );
        }
        for (String item : splitTopLevelComma(firstSelectList)) {
            String alias = trailingAlias(item);
            if (!StringUtils.hasText(alias)) {
                alias = trailingColumnName(item);
            }
            if (StringUtils.hasText(alias)) {
                columns.add(cleanIdentifier(alias));
            }
        }
        if (columns.isEmpty()) {
            return new OutputColumns(
                Collections.<String>emptyList(),
                Collections.<String>emptySet(),
                Collections.singletonList(reason(
                    "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                    "公共子图表达式输出缺少稳定别名，不能证明上层查询字段覆盖。"
                ))
            );
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(new ArrayList<String>(columns), normalized, Collections.<Map<String, Object>>emptyList());
    }

    private static SqlSelect parseSelect(String sql) {
        SqlNode statement = parseStatement(sql);
        SqlSelect select = unwrapSelect(statement);
        if (select == null) {
            throw new IllegalArgumentException("not select");
        }
        return select;
    }

    private static SqlNode parseStatement(String sql) {
        try {
            SqlParser.Config parserConfig = SqlParser.config()
                .withConformance(SqlConformanceEnum.LENIENT)
                .withUnquotedCasing(Casing.UNCHANGED);
            return SqlParser.create(trimTrailingSemicolon(SqlDialectNormalizer.normalize(sql)), parserConfig).parseStmt();
        } catch (Exception ex) {
            throw new IllegalArgumentException("parse failed", ex);
        }
    }

    private static SqlSelect unwrapSelect(SqlNode node) {
        if (node instanceof SqlSelect) {
            return (SqlSelect) node;
        }
        if (node instanceof SqlOrderBy) {
            return unwrapSelect(((SqlOrderBy) node).query);
        }
        if (node instanceof SqlWith) {
            return unwrapSelect(((SqlWith) node).body);
        }
        return null;
    }

    private static boolean isStar(SqlNode item) {
        if (item instanceof SqlIdentifier) {
            return ((SqlIdentifier) item).isStar();
        }
        return item != null && "*".equals(item.toString().trim());
    }

    private static String aliasName(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (operands.size() >= 2) {
                return operands.get(1).toString();
            }
        }
        return "";
    }

    private static SqlNode stripAlias(SqlNode item) {
        if (item instanceof SqlBasicCall && item.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) item).getOperandList();
            if (!operands.isEmpty()) {
                return operands.get(0);
            }
        }
        return item;
    }

    private static String identifierTail(SqlIdentifier identifier) {
        if (identifier == null) {
            return "";
        }
        if (identifier.names != null && !identifier.names.isEmpty()) {
            return identifier.names.get(identifier.names.size() - 1);
        }
        String text = cleanIdentifier(identifier.toString());
        int dot = text.lastIndexOf('.');
        return dot >= 0 ? text.substring(dot + 1) : text;
    }

    private static Set<String> requiredColumns(String sourceSql, SubgraphCandidate candidate) {
        if ("DERIVED_TABLE".equals(candidate.sourceKind)) {
            Set<String> astColumns = requiredQualifiedColumnsByAst(sourceSql, candidate);
            if (!astColumns.isEmpty()) {
                return astColumns;
            }
        }
        String upperQuery = "CTE".equals(candidate.sourceKind)
            ? extractMainQueryAfterWith(sourceSql)
            : removeSubgraphSql(sourceSql, candidate);
        String text = stripSingleQuotedLiterals(upperQuery);
        text = text.replaceAll("(?i)\\bAS\\s+(?:\"[^\"]+\"|[A-Z_][A-Z0-9_$]*)\\b", " ");
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        LinkedHashSet<String> relationNames = new LinkedHashSet<String>();
        addIfText(relationNames, normalizeIdentifier(candidate.sourceName));
        addIfText(relationNames, normalizeIdentifier(candidate.alias));

        Matcher quotedQualified = QUOTED_QUALIFIED_COLUMN_PATTERN.matcher(text);
        while (quotedQualified.find()) {
            String qualifier = normalizeIdentifier(quotedQualified.group(1));
            String column = normalizeIdentifier(quotedQualified.group(2));
            if (relationNames.contains(qualifier)) {
                result.add(column);
            }
        }
        Matcher qualified = QUALIFIED_COLUMN_PATTERN.matcher(text);
        while (qualified.find()) {
            String qualifier = normalizeIdentifier(qualified.group(1));
            String column = normalizeIdentifier(qualified.group(2));
            if (relationNames.contains(qualifier)) {
                result.add(column);
            }
        }
        String withoutFrom = stripDoubleQuotedIdentifiers(
            text.replaceAll("(?is)\\bFROM\\b.+?(\\bWHERE\\b|\\bGROUP\\s+BY\\b|\\bHAVING\\b|$)", " $1 ")
        );
        Matcher identifiers = IDENTIFIER_PATTERN.matcher(withoutFrom);
        while (identifiers.find()) {
            String token = normalizeIdentifier(identifiers.group());
            if (SQL_KEYWORDS.contains(token.toUpperCase(Locale.ROOT))
                || SQL_FUNCTIONS.contains(token.toUpperCase(Locale.ROOT))
                || relationNames.contains(token)) {
                continue;
            }
            result.add(token);
        }
        return result;
    }

    private static OutputColumns withRequiredQualifiedColumns(OutputColumns outputColumns, Set<String> requiredColumns) {
        if (outputColumns == null || requiredColumns == null || requiredColumns.isEmpty()) {
            return outputColumns;
        }
        LinkedHashSet<String> columns = new LinkedHashSet<String>(outputColumns.columns);
        columns.addAll(requiredColumns);
        LinkedHashSet<String> normalized = new LinkedHashSet<String>(outputColumns.normalizedColumns);
        for (String column : requiredColumns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(
            new ArrayList<String>(columns),
            normalized,
            outputColumns.blockingReasons
        );
    }

    private static Set<String> requiredQualifiedColumnsByAst(String sourceSql, SubgraphCandidate candidate) {
        LinkedHashSet<String> relationNames = new LinkedHashSet<String>();
        addIfText(relationNames, normalizeIdentifier(candidate.sourceName));
        addIfText(relationNames, normalizeIdentifier(candidate.alias));
        if (relationNames.isEmpty()) {
            return Collections.emptySet();
        }
        try {
            LinkedHashSet<String> result = new LinkedHashSet<String>();
            collectQualifiedColumns(parseStatement(sourceSql), relationNames, result);
            return result;
        } catch (RuntimeException ex) {
            return Collections.emptySet();
        }
    }

    private static void collectQualifiedColumns(SqlNode node,
                                                Set<String> relationNames,
                                                Set<String> result) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlIdentifier) {
            SqlIdentifier identifier = (SqlIdentifier) node;
            if (identifier.names != null && identifier.names.size() >= 2) {
                String qualifier = normalizeIdentifier(identifier.names.get(0));
                if (relationNames.contains(qualifier)) {
                    result.add(normalizeIdentifier(identifier.names.get(identifier.names.size() - 1)));
                }
            }
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectQualifiedColumns(item, relationNames, result);
            }
            return;
        }
        if (node instanceof SqlWith) {
            collectQualifiedColumns(((SqlWith) node).body, relationNames, result);
            return;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            collectQualifiedColumns(orderBy.query, relationNames, result);
            collectQualifiedColumns(orderBy.orderList, relationNames, result);
            return;
        }
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
            if (!operands.isEmpty()) {
                collectQualifiedColumns(operands.get(0), relationNames, result);
            }
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectQualifiedColumns(operand, relationNames, result);
            }
        }
    }

    private static String rewriteSql(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String rewritten = rewriteSqlWithEvidence(sourceSql, candidate, mvName).sql;
        return StringUtils.hasText(rewritten) ? rewritten + ";" : "";
    }

    private static RewriteResult rewriteSqlWithEvidence(String sourceSql, SubgraphCandidate candidate, String mvName) {
        if (!StringUtils.hasText(sourceSql) || candidate == null || !StringUtils.hasText(mvName)) {
            return new RewriteResult("", Collections.<Map<String, Object>>emptyList());
        }
        AstRewriteState state = new AstRewriteState(candidate);
        try {
            SqlNode statement = parseStatement(sourceSql);
            SqlNode rewritten;
            if ("CTE".equals(candidate.sourceKind)) {
                SqlNode body = statement instanceof SqlWith ? ((SqlWith) statement).body : statement;
                rewritten = rewriteRelationReferencesByAst(body, candidate.sourceName, mvName, state);
            } else {
                rewritten = rewriteDerivedSourcesByAst(statement, candidate, mvName, state);
            }
            if (!state.replaced || rewritten == null) {
                return new RewriteResult("", state.rewriteAttempts);
            }
            return new RewriteResult(
                normalizeCalciteRenderedRewrite(trimTrailingSemicolon(rewritten.toString())),
                state.rewriteAttempts
            );
        } catch (RuntimeException ex) {
            state.recordError(ex);
            return new RewriteResult("", state.rewriteAttempts);
        }
    }

    private static String normalizeCalciteRenderedRewrite(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return "";
        }
        String normalized = rewriteSql.replaceAll("`([A-Za-z_][A-Za-z0-9_$]*)`", "$1");
        normalized = normalized.replaceAll(
            "(?is)\\bFETCH\\s+NEXT\\s+(\\d+)\\s+ROWS\\s+ONLY\\b",
            "LIMIT $1"
        );
        normalized = normalized.replaceAll(
            "(?i)\\b(FROM|JOIN)\\s+([A-Za-z_][A-Za-z0-9_$]*(?:\\.[A-Za-z_][A-Za-z0-9_$]*)?)\\s+AS\\s+([A-Za-z_][A-Za-z0-9_$]*)\\b",
            "$1 $2 $3"
        );
        return normalized;
    }

    private static SqlNode rewriteRelationReferencesByAst(SqlNode node,
                                                          String relationName,
                                                          String mvName,
                                                          AstRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteRelationReferencesByAst(orderBy.query, relationName, mvName, state);
            return rewrittenQuery == orderBy.query
                ? orderBy
                : new SqlOrderBy(
                    orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                    rewrittenQuery,
                    orderBy.orderList,
                    orderBy.offset,
                    orderBy.fetch
                );
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            with.body = rewriteRelationReferencesByAst(with.body, relationName, mvName, state);
            return with;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            select.setFrom(rewriteRelationInFrom(select.getFrom(), relationName, mvName, state));
            rewriteChildQueries(select.getSelectList(), relationName, mvName, state);
            rewriteRelationReferencesByAst(select.getWhere(), relationName, mvName, state);
            rewriteRelationReferencesByAst(select.getHaving(), relationName, mvName, state);
            rewriteChildQueries(select.getOrderList(), relationName, mvName, state);
            return select;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteRelationReferencesByAst(operand, relationName, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return node;
    }

    private static SqlNode rewriteRelationInFrom(SqlNode from,
                                                 String relationName,
                                                 String mvName,
                                                 AstRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlIdentifier && relationNameMatches((SqlIdentifier) from, relationName)) {
            state.replaced = true;
            return aliasedMvReference(mvName, unqualifiedName(relationName));
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteRelationInFrom(join.getLeft(), relationName, mvName, state));
            join.setRight(rewriteRelationInFrom(join.getRight(), relationName, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                SqlNode relation = operands.get(0);
                SqlNode alias = operands.get(1);
                if (relation instanceof SqlIdentifier && relationNameMatches((SqlIdentifier) relation, relationName)) {
                    state.replaced = true;
                    return aliasedMvReference(mvName, alias);
                }
                SqlNode rewrittenRelation = rewriteRelationReferencesByAst(relation, relationName, mvName, state);
                if (rewrittenRelation != relation) {
                    return SqlStdOperatorTable.AS.createCall(
                        from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
                        rewrittenRelation,
                        alias
                    );
                }
            }
            return from;
        }
        if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteRelationInFrom(operand, relationName, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return from;
    }

    private static boolean relationNameMatches(SqlIdentifier identifier, String relationName) {
        if (identifier == null || !StringUtils.hasText(relationName)) {
            return false;
        }
        String actual = relationKey(identifier.toString());
        String expected = relationKey(relationName);
        return actual.equals(expected)
            || relationKey(unqualifiedName(actual)).equals(relationKey(unqualifiedName(expected)));
    }

    private static SqlNode rewriteDerivedSourcesByAst(SqlNode node,
                                                      SubgraphCandidate candidate,
                                                      String mvName,
                                                      AstRewriteState state) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            SqlNode rewrittenQuery = rewriteDerivedSourcesByAst(orderBy.query, candidate, mvName, state);
            return rewrittenQuery == orderBy.query
                ? orderBy
                : new SqlOrderBy(
                    orderBy.getParserPosition() == null ? SqlParserPos.ZERO : orderBy.getParserPosition(),
                    rewrittenQuery,
                    orderBy.orderList,
                    orderBy.offset,
                    orderBy.fetch
                );
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        withItem.query = rewriteDerivedSourcesByAst(withItem.query, candidate, mvName, state);
                    }
                }
            }
            with.body = rewriteDerivedSourcesByAst(with.body, candidate, mvName, state);
            return with;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            select.setFrom(rewriteDerivedInFrom(select.getFrom(), candidate, mvName, state));
            rewriteDerivedChildren(select.getSelectList(), candidate, mvName, state);
            rewriteDerivedSourcesByAst(select.getWhere(), candidate, mvName, state);
            rewriteDerivedSourcesByAst(select.getHaving(), candidate, mvName, state);
            rewriteDerivedChildren(select.getOrderList(), candidate, mvName, state);
            return select;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteDerivedSourcesByAst(operand, candidate, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return node;
    }

    private static SqlNode rewriteDerivedInFrom(SqlNode from,
                                                SubgraphCandidate candidate,
                                                String mvName,
                                                AstRewriteState state) {
        if (from == null) {
            return null;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            join.setLeft(rewriteDerivedInFrom(join.getLeft(), candidate, mvName, state));
            join.setRight(rewriteDerivedInFrom(join.getRight(), candidate, mvName, state));
            return join;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                SqlNode relation = operands.get(0);
                SqlNode alias = operands.get(1);
                state.recordDerivedAttempt(relation, alias);
                if (derivedCandidateMatches(relation, alias, candidate)) {
                    state.replaced = true;
                    return aliasedMvReference(mvName, alias);
                }
                SqlNode rewrittenRelation = rewriteDerivedSourcesByAst(relation, candidate, mvName, state);
                if (rewrittenRelation != relation) {
                    return SqlStdOperatorTable.AS.createCall(
                        from.getParserPosition() == null ? SqlParserPos.ZERO : from.getParserPosition(),
                        rewrittenRelation,
                        alias
                    );
                }
            }
            return from;
        }
        if (from instanceof SqlCall) {
            SqlCall call = (SqlCall) from;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewritten = rewriteDerivedInFrom(operand, candidate, mvName, state);
                if (rewritten != operand) {
                    setOperandIfPossible(call, index, rewritten);
                }
            }
        }
        return from;
    }

    private static boolean derivedCandidateMatches(SqlNode relation, SqlNode alias, SubgraphCandidate candidate) {
        if (candidate == null || relation == null || !isSelectLike(relation)) {
            return false;
        }
        String candidateAlias = normalizeIdentifier(firstText(candidate.alias, candidate.sourceName));
        String actualAlias = normalizeIdentifier(alias == null ? "" : alias.toString());
        String relationSql = normalizeSubgraphSql(relation.toString());
        boolean structuralMatch = subgraphFingerprint(candidate.subgraphSql).equals(subgraphFingerprint(relationSql));
        if (structuralMatch) {
            return true;
        }
        boolean aliasMatches = !StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias);
        return aliasMatches && canReplaceByAliasCoverage(relationSql, candidate);
    }

    private static boolean isSelectLike(SqlNode relation) {
        return relation instanceof SqlSelect
            || relation instanceof SqlWith
            || relation instanceof SqlOrderBy
            || isUnionAll(relation);
    }

    private static boolean isUnionAll(SqlNode relation) {
        if (!(relation instanceof SqlCall) || relation.getKind() != SqlKind.UNION) {
            return false;
        }
        return ((SqlCall) relation).getOperator() instanceof SqlSetOperator
            && ((SqlSetOperator) ((SqlCall) relation).getOperator()).isAll();
    }

    private static SqlNode aliasedMvReference(String mvName, String alias) {
        return aliasedMvReference(mvName, new SqlIdentifier(firstText(alias, mvName), SqlParserPos.ZERO));
    }

    private static SqlNode aliasedMvReference(String mvName, SqlNode alias) {
        SqlNode mvIdentifier = new SqlIdentifier(mvName, SqlParserPos.ZERO);
        SqlNode resolvedAlias = alias == null ? new SqlIdentifier(mvName, SqlParserPos.ZERO) : alias;
        return SqlStdOperatorTable.AS.createCall(SqlParserPos.ZERO, mvIdentifier, resolvedAlias);
    }

    private static void rewriteChildQueries(SqlNodeList nodes,
                                            String relationName,
                                            String mvName,
                                            AstRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteRelationReferencesByAst(node, relationName, mvName, state);
        }
    }

    private static void rewriteDerivedChildren(SqlNodeList nodes,
                                               SubgraphCandidate candidate,
                                               String mvName,
                                               AstRewriteState state) {
        if (nodes == null) {
            return;
        }
        for (SqlNode node : nodes.getList()) {
            rewriteDerivedSourcesByAst(node, candidate, mvName, state);
        }
    }

    private static void setOperandIfPossible(SqlCall call, int index, SqlNode rewritten) {
        try {
            call.setOperand(index, rewritten);
        } catch (UnsupportedOperationException ex) {
            // 少数 Calcite 节点暴露不可变 operand 视图，常见查询节点已在父级 setter 中处理。
        }
    }

    private static String replaceRelationReference(String sql, String relationName, String mvName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return sql;
        }
        Pattern pattern = Pattern.compile(
            "(?i)\\b(FROM|JOIN)\\s+"
                + Pattern.quote(relationName)
                + "(\\s+(?:AS\\s+)?(?!WHERE\\b|GROUP\\b|HAVING\\b|ORDER\\b|LIMIT\\b|JOIN\\b|ON\\b)"
                + "([A-Z_][A-Z0-9_$]*))?"
        );
        Matcher matcher = pattern.matcher(sql);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String alias = matcher.group(3);
            String replacement = matcher.group(1) + " " + mvName;
            if (StringUtils.hasText(alias)) {
                replacement += " " + alias;
            } else {
                replacement += " " + relationName;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String replaceDerivedSource(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String normalizedSource = trimTrailingSemicolon(sourceSql);
        String alias = firstText(candidate.alias, candidate.sourceName);
        String subgraphPattern = Pattern.quote(stripOuterParentheses(candidate.subgraphSql));
        Pattern pattern = Pattern.compile(
            "(?is)\\(\\s*" + subgraphPattern + "\\s*\\)\\s+(?:AS\\s+)?"
                + quotedIdentifierPattern(alias)
        );
        Matcher matcher = pattern.matcher(normalizedSource);
        if (matcher.find()) {
            StringBuffer buffer = new StringBuffer();
            do {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(mvName + " " + renderAlias(alias)));
            } while (matcher.find());
            matcher.appendTail(buffer);
            return buffer.toString();
        }
        return replaceDerivedSourceByBalancedScan(normalizedSource, candidate, mvName);
    }

    private static String replaceDerivedSourceByBalancedScan(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String alias = firstText(candidate.alias, candidate.sourceName);
        if (!StringUtils.hasText(sourceSql) || !StringUtils.hasText(alias)) {
            return sourceSql;
        }
        String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
        StringBuilder result = new StringBuilder();
        int cursor = 0;
        boolean replaced = false;
        for (int index = 0; index < sourceSql.length(); index++) {
            if (sourceSql.charAt(index) != '(') {
                continue;
            }
            int close = matchingParen(sourceSql, index);
            if (close < 0) {
                continue;
            }
            AliasMatch aliasMatch = aliasAfter(sourceSql, close + 1, alias);
            if (!aliasMatch.matched) {
                continue;
            }
            String innerSql = sourceSql.substring(index + 1, close);
            if (!candidateFingerprint.equals(subgraphFingerprint(innerSql))
                && !canReplaceByAliasCoverage(innerSql, candidate)) {
                continue;
            }
            result.append(sourceSql, cursor, index);
            result.append(mvName).append(" ").append(renderAlias(alias));
            cursor = aliasMatch.endIndex;
            index = aliasMatch.endIndex - 1;
            replaced = true;
        }
        if (!replaced) {
            return sourceSql;
        }
        result.append(sourceSql.substring(cursor));
        return result.toString();
    }

    private static int replacementCount(String sourceSql, SubgraphCandidate candidate) {
        String alias = firstText(candidate.alias, candidate.sourceName);
        if (!StringUtils.hasText(sourceSql) || !StringUtils.hasText(alias)) {
            return 0;
        }
        String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
        int count = 0;
        for (int index = 0; index < sourceSql.length(); index++) {
            if (sourceSql.charAt(index) != '(') {
                continue;
            }
            int close = matchingParen(sourceSql, index);
            if (close < 0) {
                continue;
            }
            AliasMatch aliasMatch = aliasAfter(sourceSql, close + 1, alias);
            if (!aliasMatch.matched) {
                continue;
            }
            String innerSql = sourceSql.substring(index + 1, close);
            if (candidateFingerprint.equals(subgraphFingerprint(innerSql))
                || canReplaceByAliasCoverage(innerSql, candidate)) {
                count++;
                index = aliasMatch.endIndex - 1;
            }
        }
        return count;
    }

    private static boolean canReplaceByAliasCoverage(String innerSql, SubgraphCandidate candidate) {
        if (!"DERIVED_TABLE".equals(candidate.sourceKind) || !StringUtils.hasText(innerSql)) {
            return false;
        }
        String normalizedInner = stripOuterParentheses(trimTrailingSemicolon(innerSql));
        if (!isReplacementEligibleSubquery(normalizedInner) || hasOrderOrLimit(normalizedInner)) {
            return false;
        }
        OutputColumns innerColumns = outputColumns(normalizedInner);
        OutputColumns candidateColumns = outputColumns(candidate.subgraphSql);
        if (!innerColumns.blockingReasons.isEmpty()
            || !candidateColumns.blockingReasons.isEmpty()
            || innerColumns.normalizedColumns.isEmpty()
            || candidateColumns.normalizedColumns.isEmpty()) {
            return false;
        }
        return innerColumns.normalizedColumns.containsAll(candidateColumns.normalizedColumns)
            || candidateColumns.normalizedColumns.containsAll(innerColumns.normalizedColumns);
    }

    private static boolean isReplacementEligibleSubquery(String sql) {
        if (!StringUtils.hasText(sql)) {
            return false;
        }
        try {
            return isSelectLike(parseStatement(sql));
        } catch (RuntimeException ex) {
            return startsWithWord(sql, 0, "SELECT");
        }
    }

    private static int matchingParen(String sql, int openIndex) {
        int depth = 0;
        char quote = 0;
        for (int index = openIndex; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == quote) {
                        index++;
                    } else {
                        quote = 0;
                    }
                }
                continue;
            }
            if (current == '\'' || current == '"' || current == '`') {
                quote = current;
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static AliasMatch aliasAfter(String sql, int start, String expectedAlias) {
        int index = skipWhitespace(sql, start);
        int aliasStart = index;
        if (startsWithWord(sql, index, "AS")) {
            aliasStart = skipWhitespace(sql, index + 2);
        }
        AliasToken token = readAliasToken(sql, aliasStart);
        if (!token.present) {
            return AliasMatch.none();
        }
        return normalizeIdentifier(token.value).equals(normalizeIdentifier(expectedAlias))
            ? new AliasMatch(true, token.endIndex)
            : AliasMatch.none();
    }

    private static int skipWhitespace(String sql, int start) {
        int index = start;
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index;
    }

    private static AliasToken readAliasToken(String sql, int start) {
        if (start >= sql.length()) {
            return AliasToken.none();
        }
        char first = sql.charAt(start);
        if (first == '"' || first == '`') {
            StringBuilder value = new StringBuilder();
            for (int index = start + 1; index < sql.length(); index++) {
                char current = sql.charAt(index);
                if (current == first) {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == first) {
                        value.append(current);
                        index++;
                        continue;
                    }
                    return new AliasToken(true, value.toString(), index + 1);
                }
                value.append(current);
            }
            return AliasToken.none();
        }
        int end = start;
        while (end < sql.length() && isIdentifierPart(sql.charAt(end))) {
            end++;
        }
        if (end == start) {
            return AliasToken.none();
        }
        return new AliasToken(true, sql.substring(start, end), end);
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private static String renderAlias(String alias) {
        String cleaned = cleanIdentifier(alias);
        if (cleaned.matches("(?i)[A-Z_][A-Z0-9_$]*")) {
            return cleaned;
        }
        return "\"" + cleaned.replace("\"", "\"\"") + "\"";
    }

    private static String removeSubgraphSql(String sourceSql, SubgraphCandidate candidate) {
        String alias = firstText(candidate.alias, candidate.sourceName);
        return replaceDerivedSource(sourceSql, candidate, alias);
    }

    private static Map<String, Object> commonSubgraphEvidence(
        String sourceSql,
        SubgraphCandidate candidate,
        List<String> outputColumns,
        Set<String> requiredColumns,
        List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        String fingerprint = subgraphFingerprint(candidate.subgraphSql);
        List<Map<String, Object>> matchedSourceRefs = new ArrayList<Map<String, Object>>();
        List<String> matchedSqlFingerprints = new ArrayList<String>();
        addSourceRef(matchedSourceRefs, matchedSqlFingerprints, "CURRENT_SQL", "CURRENT", sourceSql, fingerprint);
        if (peerSqls != null) {
            for (L2AccelerationArtifactBuilder.CommonSubgraphPeerSql peerSql : peerSqls) {
                if (peerSql == null || !StringUtils.hasText(peerSql.getSqlText())) {
                    continue;
                }
                List<SubgraphCandidate> peerCandidates =
                    subgraphCandidates(peerSql.getAdvancedStructureProfile());
                for (SubgraphCandidate peerCandidate : peerCandidates) {
                    if (!fingerprint.equals(subgraphFingerprint(peerCandidate.subgraphSql))) {
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
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("status", "COVERED");
        coverage.put("requiredColumns", new ArrayList<String>(requiredColumns));
        coverage.put("outputColumns", outputColumns);
        coverage.put("rewriteSource", "MV_ONLY");

        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("mode", matchedSourceRefs.size() > 1 ? "CROSS_SQL_SHARED_SUBGRAPH" : "SINGLE_SQL_SUBGRAPH");
        evidence.put("candidateSelectionSource", "CALCITE_AST_QBDAG_STRUCTURAL_REUSE");
        evidence.put("staticConstantMatchUsed", Boolean.FALSE);
        evidence.put("subgraphFingerprint", fingerprint);
        evidence.put("sourceKind", candidate.sourceKind);
        evidence.put("sourceName", candidate.sourceName);
        evidence.put("alias", candidate.alias);
        evidence.put("matchedSqlFingerprints", matchedSqlFingerprints);
        evidence.put("matchedSourceRefs", matchedSourceRefs);
        evidence.put("referenceCount", Integer.valueOf(matchedSourceRefs.size()));
        evidence.put("outputColumns", outputColumns);
        evidence.put("rewriteCoverage", coverage);
        return evidence;
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
        String resolvedSqlFingerprint = firstText(sqlFingerprint, shortSha256(canonicalSql(sqlText)));
        if (!fingerprints.contains(resolvedSqlFingerprint)) {
            fingerprints.add(resolvedSqlFingerprint);
        }
        LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("sourceKind", firstText(sourceKind, "UNKNOWN"));
        item.put("sourceRef", firstText(sourceRef, "UNKNOWN"));
        item.put("sqlFingerprint", resolvedSqlFingerprint);
        item.put("reportCode", reportCode);
        item.put("subgraphFingerprint", subgraphFingerprint);
        refs.add(item);
    }

    private static String extractMainQueryAfterWith(String sourceSql) {
        String sql = trimTrailingSemicolon(sourceSql);
        if (!StringUtils.hasText(sql) || !sql.trim().toUpperCase(Locale.ROOT).startsWith("WITH ")) {
            return sql;
        }
        int depth = 0;
        boolean seenCteBody = false;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                seenCteBody = true;
            } else if (current == ')') {
                depth--;
                if (seenCteBody && depth == 0) {
                    int cursor = i + 1;
                    while (cursor < sql.length()
                        && (Character.isWhitespace(sql.charAt(cursor)) || sql.charAt(cursor) == ',')) {
                        cursor++;
                    }
                    if (startsWithWord(sql, cursor, "SELECT")) {
                        return sql.substring(cursor).trim();
                    }
                }
            }
        }
        return sql;
    }

    private static boolean startsWithWord(String sql, int offset, String word) {
        if (sql == null || offset < 0 || offset + word.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, offset, word, 0, word.length())) {
            return false;
        }
        int end = offset + word.length();
        return end >= sql.length() || !Character.isLetterOrDigit(sql.charAt(end));
    }

    private static boolean hasOrderOrLimit(String sql) {
        String normalized = stripStringLiterals(sql).toUpperCase(Locale.ROOT);
        return normalized.matches("(?is).*\\bORDER\\s+BY\\b.*")
            || normalized.matches("(?is).*\\bLIMIT\\b.*");
    }

    private static String normalizeSubgraphSql(String sql) {
        String normalized = stripOuterParentheses(trimTrailingSemicolon(sql));
        return trimTrailingSemicolon(normalized);
    }

    private static String removeTrailingAlias(String sql, String alias) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(alias)) {
            return sql;
        }
        return sql.replaceFirst("(?is)\\s+(?:AS\\s+)?"
            + Pattern.quote(alias)
            + "\\s*$", "");
    }

    private static String stripOuterParentheses(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        while (normalized.startsWith("(") && normalized.endsWith(")") && wrapsWholeExpression(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private static boolean wrapsWholeExpression(String value) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0 && i < value.length() - 1) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    private static String subgraphFingerprint(String sql) {
        String key = trimTrailingSemicolon(sql);
        String cached = SUBGRAPH_FINGERPRINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        String fingerprint = shortSha256(canonicalSql(key));
        SUBGRAPH_FINGERPRINT_CACHE.put(key, fingerprint);
        return fingerprint;
    }

    private static String canonicalSql(String sql) {
        try {
            SqlNode parsed = parseStatement(sql);
            String canonical = canonicalSqlText(parsed.toString());
            LinkedHashMap<String, String> relationAliases = relationAliasReplacements(parsed);
            List<String> aliases = new ArrayList<String>(relationAliases.keySet());
            Collections.sort(aliases, new java.util.Comparator<String>() {
                @Override
                public int compare(String left, String right) {
                    return Integer.compare(right.length(), left.length());
                }
            });
            for (String alias : aliases) {
                canonical = replaceRelationAliasReferences(canonical, alias, relationAliases.get(alias));
            }
            return canonical;
        } catch (RuntimeException ex) {
            return canonicalSqlText(sql);
        }
    }

    private static String canonicalSqlText(String sql) {
        return trimTrailingSemicolon(sql)
            .replace("`", "")
            .replace("\"", "")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    private static LinkedHashMap<String, String> relationAliasReplacements(SqlNode node) {
        LinkedHashSet<String> aliases = new LinkedHashSet<String>();
        collectRelationAliases(node, aliases);
        LinkedHashMap<String, String> replacements = new LinkedHashMap<String, String>();
        int index = 0;
        for (String alias : aliases) {
            String normalized = canonicalSqlText(cleanIdentifier(alias));
            if (!StringUtils.hasText(normalized) || replacements.containsKey(normalized)) {
                continue;
            }
            replacements.put(normalized, "__REL_ALIAS_" + index + "__");
            index++;
        }
        return replacements;
    }

    private static void collectRelationAliases(SqlNode node, Set<String> aliases) {
        if (node == null) {
            return;
        }
        if (node instanceof SqlNodeList) {
            for (SqlNode item : ((SqlNodeList) node).getList()) {
                collectRelationAliases(item, aliases);
            }
            return;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            collectRelationAliases(orderBy.query, aliases);
            collectRelationAliases(orderBy.orderList, aliases);
            return;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode item : with.withList.getList()) {
                    if (item instanceof SqlWithItem) {
                        SqlWithItem withItem = (SqlWithItem) item;
                        if (withItem.name != null) {
                            addIfText(aliases, withItem.name.toString());
                        }
                        collectRelationAliases(withItem.query, aliases);
                    }
                }
            }
            collectRelationAliases(with.body, aliases);
            return;
        }
        if (node instanceof SqlSelect) {
            SqlSelect select = (SqlSelect) node;
            collectRelationAliasesFrom(select.getFrom(), aliases);
            collectRelationAliases(select.getWhere(), aliases);
            collectRelationAliases(select.getHaving(), aliases);
            collectRelationAliases(select.getSelectList(), aliases);
            collectRelationAliases(select.getOrderList(), aliases);
            return;
        }
        if (node instanceof SqlCall) {
            for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                collectRelationAliases(operand, aliases);
            }
        }
    }

    private static void collectRelationAliasesFrom(SqlNode from, Set<String> aliases) {
        if (from == null) {
            return;
        }
        if (from instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) from;
            collectRelationAliasesFrom(join.getLeft(), aliases);
            collectRelationAliasesFrom(join.getRight(), aliases);
            return;
        }
        if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
            List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
            if (operands.size() >= 2) {
                addIfText(aliases, operands.get(1).toString());
                collectRelationAliases(operands.get(0), aliases);
            }
            return;
        }
        collectRelationAliases(from, aliases);
    }

    private static String replaceRelationAliasReferences(String canonicalSql,
                                                         String alias,
                                                         String replacement) {
        if (!StringUtils.hasText(canonicalSql)
            || !StringUtils.hasText(alias)
            || !StringUtils.hasText(replacement)) {
            return canonicalSql;
        }
        String boundaryBefore = "(?<![\\p{L}\\p{N}_$])";
        String boundaryAfter = "(?![\\p{L}\\p{N}_$])";
        String quotedAlias = Pattern.quote(alias);
        String result = canonicalSql.replaceAll(
            boundaryBefore + quotedAlias + "\\s*\\.",
            replacement + "."
        );
        return result.replaceAll(
            "(?i)(\\bAS\\s+)" + quotedAlias + boundaryAfter,
            "$1" + replacement
        );
    }

    private static String shortSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bytes.length && i < 8; i++) {
                String hex = Integer.toHexString(bytes[i] & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    private static String stripStringLiterals(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                if (inSingleQuote && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    builder.append(' ');
                    i++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote ? ' ' : current);
        }
        return builder.toString();
    }

    private static String stripSingleQuotedLiterals(String sql) {
        if (sql == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sql.length());
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote ? ' ' : current);
        }
        return builder.toString();
    }

    private static String stripDoubleQuotedIdentifiers(String sql) {
        return sql == null ? "" : sql.replaceAll("\"[^\"]+\"", " ");
    }

    private static String firstSelectList(String sql) {
        String normalized = trimTrailingSemicolon(stripOuterParentheses(sql));
        int selectIndex = findKeywordAtDepth(normalized, "SELECT", 0);
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = findKeywordAtDepth(normalized, "FROM", selectIndex + "SELECT".length());
        if (fromIndex < 0 || fromIndex <= selectIndex) {
            return "";
        }
        return normalized.substring(selectIndex + "SELECT".length(), fromIndex).trim();
    }

    private static int findKeywordAtDepth(String sql, String keyword, int offset) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        for (int i = Math.max(0, offset); i <= sql.length() - keyword.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
                continue;
            }
            if (inSingleQuote || inDoubleQuote || inBacktickQuote) {
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && startsWithWord(sql, i, keyword)) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> splitTopLevelComma(String value) {
        List<String> result = new ArrayList<String>();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktickQuote = false;
        int start = 0;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\'' && !inDoubleQuote && !inBacktickQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktickQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktickQuote = !inBacktickQuote;
            } else if (!inSingleQuote && !inDoubleQuote && !inBacktickQuote) {
                if (current == '(') {
                    depth++;
                } else if (current == ')') {
                    depth = Math.max(0, depth - 1);
                } else if (current == ',' && depth == 0) {
                    result.add(value.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    private static String trailingAlias(String item) {
        Matcher quoted = Pattern.compile("(?is)\\s+AS\\s+(?:\"([^\"]+)\"|`([^`]+)`)\\s*$")
            .matcher(item == null ? "" : item);
        if (quoted.find()) {
            return firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)\\s+AS\\s+([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    private static String trailingColumnName(String item) {
        Matcher quoted = Pattern.compile("(?:\"([^\"]+)\"|`([^`]+)`)\\s*$").matcher(item == null ? "" : item);
        if (quoted.find()) {
            return firstText(quoted.group(1), quoted.group(2));
        }
        Matcher unquoted = Pattern.compile("(?is)([\\p{L}_][\\p{L}\\p{N}_$]*)\\s*$")
            .matcher(item == null ? "" : item);
        return unquoted.find() ? unquoted.group(1) : "";
    }

    private static String quotedIdentifierPattern(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return "\"[^\"]+\"|[A-Z_][A-Z0-9_$]*";
        }
        return "(?:\"" + Pattern.quote(identifier) + "\"|" + Pattern.quote(identifier) + ")";
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
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

    private static String trimTrailingSemicolon(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        while (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static String upperText(Object value) {
        return text(value).toUpperCase(Locale.ROOT);
    }

    private static String cleanIdentifier(String value) {
        return value == null ? "" : value.replace("\"", "").replace("`", "").trim();
    }

    private static String normalizeIdentifier(String value) {
        return cleanIdentifier(value).toUpperCase(Locale.ROOT);
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static void addIfText(java.util.Collection<String> values, String value) {
        if (StringUtils.hasText(value)) {
            values.add(value);
        }
    }

    static final class CandidateSql {

        private final List<Map<String, Object>> blockingReasons;
        private final String ddlSql;
        private final String refreshSql;
        private final String validationSql;
        private final String rollbackSql;
        private final String rewriteSql;
        private final Map<String, Object> commonSubgraphEvidence;

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             Map<String, Object> commonSubgraphEvidence) {
            this.blockingReasons = blockingReasons;
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.commonSubgraphEvidence = commonSubgraphEvidence;
        }

        static CandidateSql blocked(List<Map<String, Object>> blockingReasons) {
            return new CandidateSql(
                blockingReasons == null ? Collections.<Map<String, Object>>emptyList() : blockingReasons,
                null,
                null,
                null,
                null,
                null,
                Collections.<String, Object>emptyMap()
            );
        }

        static CandidateSql generated(String ddlSql,
                                      String refreshSql,
                                      String validationSql,
                                      String rollbackSql,
                                      String rewriteSql,
                                      Map<String, Object> commonSubgraphEvidence) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                commonSubgraphEvidence
            );
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        String getDdlSql() {
            return ddlSql;
        }

        String getRefreshSql() {
            return refreshSql;
        }

        String getValidationSql() {
            return validationSql;
        }

        String getRollbackSql() {
            return rollbackSql;
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        Map<String, Object> getCommonSubgraphEvidence() {
            return commonSubgraphEvidence;
        }
    }

    private static final class SubgraphCandidate {

        private final String sourceKind;
        private final String sourceName;
        private final String alias;
        private final String subgraphSql;
        private final boolean recursive;

        private SubgraphCandidate(String sourceKind,
                                  String sourceName,
                                  String alias,
                                  String subgraphSql,
                                  boolean recursive) {
            this.sourceKind = sourceKind;
            this.sourceName = sourceName;
            this.alias = alias;
            this.subgraphSql = subgraphSql;
            this.recursive = recursive;
        }
    }

    private static final class OutputColumns {

        private final List<String> columns;
        private final Set<String> normalizedColumns;
        private final List<Map<String, Object>> blockingReasons;

        private OutputColumns(List<String> columns,
                              Set<String> normalizedColumns,
                              List<Map<String, Object>> blockingReasons) {
            this.columns = columns;
            this.normalizedColumns = normalizedColumns;
            this.blockingReasons = blockingReasons;
        }
    }

    private static final class AliasMatch {

        private final boolean matched;
        private final int endIndex;

        private AliasMatch(boolean matched, int endIndex) {
            this.matched = matched;
            this.endIndex = endIndex;
        }

        private static AliasMatch none() {
            return new AliasMatch(false, -1);
        }
    }

    private static final class AliasToken {

        private final boolean present;
        private final String value;
        private final int endIndex;

        private AliasToken(boolean present, String value, int endIndex) {
            this.present = present;
            this.value = value;
            this.endIndex = endIndex;
        }

        private static AliasToken none() {
            return new AliasToken(false, "", -1);
        }
    }

    private static final class RewriteResult {

        private final String sql;
        private final List<Map<String, Object>> rewriteAttempts;

        private RewriteResult(String sql, List<Map<String, Object>> rewriteAttempts) {
            this.sql = sql == null ? "" : sql;
            this.rewriteAttempts = rewriteAttempts == null
                ? Collections.<Map<String, Object>>emptyList()
                : rewriteAttempts;
        }
    }

    private static final class AstRewriteState {

        private final SubgraphCandidate candidate;
        private final List<Map<String, Object>> rewriteAttempts = new ArrayList<Map<String, Object>>();
        private boolean replaced;

        private AstRewriteState() {
            this(null);
        }

        private AstRewriteState(SubgraphCandidate candidate) {
            this.candidate = candidate;
        }

        private void recordDerivedAttempt(SqlNode relation, SqlNode alias) {
            if (candidate == null || rewriteAttempts.size() >= 12) {
                return;
            }
            String candidateAlias = normalizeIdentifier(firstText(candidate.alias, candidate.sourceName));
            String actualAlias = normalizeIdentifier(alias == null ? "" : alias.toString());
            String relationSql = relation == null ? "" : normalizeSubgraphSql(relation.toString());
            String relationFingerprint = subgraphFingerprint(relationSql);
            String candidateFingerprint = subgraphFingerprint(candidate.subgraphSql);
            LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
            attempt.put("actualAlias", cleanIdentifier(alias == null ? "" : alias.toString()));
            attempt.put("candidateAlias", cleanIdentifier(firstText(candidate.alias, candidate.sourceName)));
            attempt.put("aliasMatched", Boolean.valueOf(
                !StringUtils.hasText(candidateAlias) || candidateAlias.equals(actualAlias)
            ));
            attempt.put("relationKind", relation == null ? "" : String.valueOf(relation.getKind()));
            attempt.put("relationClass", relation == null ? "" : relation.getClass().getSimpleName());
            attempt.put("selectLike", Boolean.valueOf(isSelectLike(relation)));
            attempt.put("relationFingerprint", relationFingerprint);
            attempt.put("candidateFingerprint", candidateFingerprint);
            attempt.put("fingerprintEqual", Boolean.valueOf(candidateFingerprint.equals(relationFingerprint)));
            attempt.put("aliasCoverageReplaceable", Boolean.valueOf(canReplaceByAliasCoverage(relationSql, candidate)));
            attempt.put("relationSqlPrefix", relationSql.length() <= 160 ? relationSql : relationSql.substring(0, 160));
            rewriteAttempts.add(attempt);
        }

        private void recordError(RuntimeException ex) {
            if (rewriteAttempts.size() >= 12) {
                return;
            }
            LinkedHashMap<String, Object> attempt = new LinkedHashMap<String, Object>();
            attempt.put("error", ex == null ? "" : ex.getClass().getSimpleName());
            attempt.put("message", ex == null ? "" : ex.getMessage());
            rewriteAttempts.add(attempt);
        }
    }
}
