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
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.springframework.util.StringUtils;

final class L2CommonSubgraphMvCandidateGenerator {

    private static final Pattern QUALIFIED_COLUMN_PATTERN =
        Pattern.compile("(?i)\\b([A-Z_][A-Z0-9_$]*)\\.([A-Z_][A-Z0-9_$]*)\\b");
    private static final Pattern IDENTIFIER_PATTERN =
        Pattern.compile("(?i)\\b[A-Z_][A-Z0-9_$]*\\b");
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

    private L2CommonSubgraphMvCandidateGenerator() {
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 Map<String, Object> advancedStructureProfile,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                 List<L2AccelerationArtifactBuilder.CommonSubgraphPeerSql> peerSqls) {
        List<Map<String, Object>> blockingReasons = structuralBlockingReasons(advancedStructureProfile, profile);
        List<SubgraphCandidate> candidates = subgraphCandidates(advancedStructureProfile);
        if (candidates.isEmpty()) {
            blockingReasons.add(commonSubgraphCandidateRequiredReason(profile));
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }

        SubgraphCandidate candidate = chooseCandidate(sourceSql, candidates);
        OutputColumns outputColumns = outputColumns(candidate.subgraphSql);
        blockingReasons.addAll(outputColumns.blockingReasons);
        if (blockingReasons.isEmpty() && outputColumns.columns.isEmpty()) {
            blockingReasons.add(reason(
                "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                "公共子图输出字段无法解析，不能证明上层查询可由 MV 覆盖。"
            ));
        }
        Set<String> requiredColumns = requiredColumns(sourceSql, candidate);
        if (blockingReasons.isEmpty() && !outputColumns.normalizedColumns.containsAll(requiredColumns)) {
            LinkedHashSet<String> missing = new LinkedHashSet<String>(requiredColumns);
            missing.removeAll(outputColumns.normalizedColumns);
            Map<String, Object> reason = reason(
                "SUBGRAPH_OUTPUT_NOT_COVERED",
                "公共子图输出字段不能覆盖上层投影、过滤或分组，不能生成可发布 rewrite。"
            );
            reason.put("missingColumns", new ArrayList<String>(missing));
            reason.put("requiredColumns", new ArrayList<String>(requiredColumns));
            reason.put("outputColumns", outputColumns.columns);
            blockingReasons.add(reason);
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons);
        }

        String rewriteSql = rewriteSql(sourceSql, candidate, mvName);
        if (!StringUtils.hasText(rewriteSql) || rewriteSql.equals(trimTrailingSemicolon(sourceSql))) {
            blockingReasons.add(reason(
                "COMMON_SUBGRAPH_REWRITE_UNSUPPORTED",
                "无法把上层查询来源稳定替换为公共子图 MV。"
            ));
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

    private static SubgraphCandidate chooseCandidate(String sourceSql, List<SubgraphCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        String mainQuery = extractMainQueryAfterWith(sourceSql);
        for (SubgraphCandidate candidate : candidates) {
            if (!"CTE".equals(candidate.sourceKind) || relationReferenced(mainQuery, candidate.sourceName)) {
                return candidate;
            }
        }
        return candidates.get(0);
    }

    private static boolean relationReferenced(String sql, String relationName) {
        if (!StringUtils.hasText(sql) || !StringUtils.hasText(relationName)) {
            return false;
        }
        Pattern pattern = Pattern.compile(
            "(?i)\\b(FROM|JOIN)\\s+" + Pattern.quote(relationName) + "\\b"
        );
        return pattern.matcher(sql).find();
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
                "高级结构画像未完整可用，不能生成可发布的 COMMON_SUBGRAPH_MV SQL。"
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
        if (profile != null && profile.isSetOperation()) {
            reasons.add(reason(
                "SET_OPERATION_COMMON_SUBGRAPH_UNSUPPORTED",
                "集合运算的公共子图边界需要额外覆盖证明，AMV-009 默认阻断。"
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
        if (hasCteDependency(advancedStructureProfile)) {
            reasons.add(reason(
                "CTE_DEPENDENCY_COMMON_SUBGRAPH_UNSUPPORTED",
                "公共子图 CTE 依赖同一 WITH 中的其他 CTE，AMV-009 暂不生成嵌套 CTE 物化 SQL。"
            ));
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

    private static OutputColumns outputColumns(String subgraphSql) {
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        try {
            Select select = parseSelect(subgraphSql);
            PlainSelect plainSelect = plainSelect(select);
            if (plainSelect == null || plainSelect.getSelectItems() == null) {
                blockingReasons.add(reason(
                    "SUBGRAPH_SELECT_UNSUPPORTED",
                    "公共子图必须是可解析的简单 SELECT。"
                ));
                return new OutputColumns(Collections.<String>emptyList(), columns, blockingReasons);
            }
            for (SelectItem item : plainSelect.getSelectItems()) {
                if (item instanceof AllColumns || item instanceof AllTableColumns) {
                    blockingReasons.add(reason(
                        "EXPLICIT_PROJECTION_REQUIRED",
                        "公共子图包含 SELECT *，需要先展开字段后才能生成可审查物化视图。"
                    ));
                    continue;
                }
                if (!(item instanceof SelectExpressionItem)) {
                    blockingReasons.add(reason(
                        "SUBGRAPH_OUTPUT_COLUMNS_UNRESOLVED",
                        "公共子图存在无法解析的输出项，不能证明 rewrite 覆盖。"
                    ));
                    continue;
                }
                SelectExpressionItem expressionItem = (SelectExpressionItem) item;
                String output = expressionItem.getAlias() == null ? "" : expressionItem.getAlias().getName();
                if (!StringUtils.hasText(output)) {
                    Expression expression = expressionItem.getExpression();
                    if (expression instanceof Column) {
                        output = ((Column) expression).getColumnName();
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
        } catch (JSQLParserException ex) {
            blockingReasons.add(reason(
                "SUBGRAPH_SELECT_UNSUPPORTED",
                "公共子图 SQL 无法解析为受支持 SELECT。"
            ));
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String column : columns) {
            normalized.add(normalizeIdentifier(column));
        }
        return new OutputColumns(new ArrayList<String>(columns), normalized, blockingReasons);
    }

    private static Select parseSelect(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(trimTrailingSemicolon(sql));
        if (!(statement instanceof Select)) {
            throw new JSQLParserException("not select");
        }
        return (Select) statement;
    }

    private static PlainSelect plainSelect(Select select) {
        if (select == null || !(select.getSelectBody() instanceof PlainSelect)) {
            return null;
        }
        return (PlainSelect) select.getSelectBody();
    }

    private static Set<String> requiredColumns(String sourceSql, SubgraphCandidate candidate) {
        String upperQuery = "CTE".equals(candidate.sourceKind)
            ? extractMainQueryAfterWith(sourceSql)
            : removeSubgraphSql(sourceSql, candidate);
        String text = stripStringLiterals(upperQuery);
        text = text.replaceAll("(?i)\\bAS\\s+[A-Z_][A-Z0-9_$]*\\b", " ");
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        LinkedHashSet<String> relationNames = new LinkedHashSet<String>();
        addIfText(relationNames, normalizeIdentifier(candidate.sourceName));
        addIfText(relationNames, normalizeIdentifier(candidate.alias));

        Matcher qualified = QUALIFIED_COLUMN_PATTERN.matcher(text);
        while (qualified.find()) {
            String qualifier = normalizeIdentifier(qualified.group(1));
            String column = normalizeIdentifier(qualified.group(2));
            if (relationNames.contains(qualifier)) {
                result.add(column);
            }
        }
        String withoutFrom = text.replaceAll("(?is)\\bFROM\\b.+?(\\bWHERE\\b|\\bGROUP\\s+BY\\b|\\bHAVING\\b|$)", " $1 ");
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

    private static String rewriteSql(String sourceSql, SubgraphCandidate candidate, String mvName) {
        String rewritten;
        if ("CTE".equals(candidate.sourceKind)) {
            rewritten = extractMainQueryAfterWith(sourceSql);
            rewritten = replaceRelationReference(rewritten, candidate.sourceName, mvName);
        } else {
            rewritten = replaceDerivedSource(sourceSql, candidate, mvName);
        }
        rewritten = trimTrailingSemicolon(rewritten);
        return StringUtils.hasText(rewritten) ? rewritten + ";" : "";
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
                + (StringUtils.hasText(alias) ? Pattern.quote(alias) : "[A-Z_][A-Z0-9_$]*")
        );
        Matcher matcher = pattern.matcher(normalizedSource);
        if (matcher.find()) {
            return matcher.replaceFirst(Matcher.quoteReplacement(mvName + " " + alias));
        }
        return normalizedSource;
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
        return shortSha256(canonicalSql(sql));
    }

    private static String canonicalSql(String sql) {
        return trimTrailingSemicolon(sql)
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
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
        boolean inDoubleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                builder.append(' ');
                continue;
            }
            if (current == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                builder.append(' ');
                continue;
            }
            builder.append(inSingleQuote || inDoubleQuote ? ' ' : current);
        }
        return builder.toString();
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
}
