package com.company.sqloptimization.application.service;

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
import org.springframework.util.StringUtils;

final class L2MaterializedViewRewriteCoverageValidator {

    static final String REWRITE_SQL_NOT_READONLY = "REWRITE_SQL_NOT_READONLY";
    static final String REWRITE_SQL_MV_REFERENCE_REQUIRED = "REWRITE_SQL_MV_REFERENCE_REQUIRED";
    static final String REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE = "REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE";
    static final String REWRITE_PROJECTION_NOT_COVERED = "REWRITE_PROJECTION_NOT_COVERED";
    static final String REWRITE_FILTER_NOT_COVERED = "REWRITE_FILTER_NOT_COVERED";
    static final String REWRITE_GROUPING_NOT_COVERED = "REWRITE_GROUPING_NOT_COVERED";
    static final String REWRITE_MEASURE_NOT_COVERED = "REWRITE_MEASURE_NOT_COVERED";
    static final String REWRITE_SECURITY_PREDICATE_NOT_COVERED = "REWRITE_SECURITY_PREDICATE_NOT_COVERED";

    private static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("SUM", "COUNT", "MIN", "MAX", "AVG"));
    private static final Set<String> READONLY_PREFIXES =
        new LinkedHashSet<String>(Arrays.asList("SELECT", "WITH"));
    private static final Set<String> FORBIDDEN_TOKENS =
        new LinkedHashSet<String>(Arrays.asList(
            "INSERT", "UPDATE", "DELETE", "MERGE", "UPSERT", "CREATE", "ALTER", "DROP", "TRUNCATE",
            "GRANT", "REVOKE", "CALL", "EXPORT", "IMPORT", "LOAD"
        ));
    private static final Pattern RELATION_PATTERN =
        Pattern.compile("(?i)\\b(FROM|JOIN)\\s+([`\\\"]?[A-Z_][A-Z0-9_$]*(?:\\.[`\\\"]?[A-Z_][A-Z0-9_$]*)*)");
    private static final Pattern ALIAS_PATTERN =
        Pattern.compile("(?is)\\s+AS\\s+([A-Z_][A-Z0-9_$]*)\\s*$");

    private L2MaterializedViewRewriteCoverageValidator() {
    }

    static ValidationResult validate(ValidationInput input) {
        ValidationInput safeInput = input == null ? ValidationInput.empty() : input;
        String normalizedRewrite = normalizeSingleStatement(safeInput.rewriteSql);
        Set<String> mvFields = coverageFields(
            safeInput.mvFieldNames,
            Collections.<String>emptyList()
        );
        Set<String> coverageFields = coverageFields(safeInput);
        Set<String> selectedOutputs = selectedOutputs(normalizedRewrite);

        boolean readonly = isReadonly(normalizedRewrite);
        boolean referencesMv = referencesMv(normalizedRewrite, safeInput.mvName);
        Set<String> originalSources = originalBaseSources(safeInput.advancedStructureProfile);
        List<String> accessedOriginalSources = accessedOriginalSources(normalizedRewrite, safeInput.mvName, originalSources);
        boolean avoidsOriginalSources = accessedOriginalSources.isEmpty();

        boolean coversProjection = L2GrainMeasureDeriver.MV_TYPE_COMMON_SUBGRAPH.equals(safeInput.mvType)
            ? coversCommonSubgraphProjection(safeInput.additionalCoverageReferences, mvFields, normalizedRewrite)
            : coversProjection(
                mapList(safeInput.advancedStructureProfile.get("projections")),
                safeInput.measures,
                coverageFields,
                selectedOutputs,
                normalizedRewrite
            );
        boolean coversFilters = coversPredicates(
            safeInput.predicateClassification == null
                ? Collections.<Map<String, Object>>emptyList()
                : safeInput.predicateClassification.getExternalizedPredicates(),
            coverageFields
        ) && (safeInput.predicateClassification == null || !safeInput.predicateClassification.hasBlockedPredicates());
        boolean coversGrouping = coversGrouping(
            mapList(safeInput.advancedStructureProfile.get("groupBy")),
            coverageFields
        );
        boolean coversMeasures = coversMeasures(
            mapList(safeInput.advancedStructureProfile.get("aggregations")),
            safeInput.measures,
            coverageFields
        );
        if (rootCountProjectionPreservedByMvRewrite(safeInput.advancedStructureProfile, normalizedRewrite)) {
            coversProjection = true;
            coversMeasures = true;
        }
        boolean coversSecurity = coversPredicates(
            safeInput.predicateClassification == null
                ? Collections.<Map<String, Object>>emptyList()
                : safeInput.predicateClassification.getSecurityPredicates(),
            coverageFields
        );

        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.valueOf(coversProjection));
        coverage.put("coversFilters", Boolean.valueOf(coversFilters));
        coverage.put("coversGrouping", Boolean.valueOf(coversGrouping));
        coverage.put("coversMeasures", Boolean.valueOf(coversMeasures));
        coverage.put("coversSecurity", Boolean.valueOf(coversSecurity));
        coverage.put("rewriteSqlReadonly", Boolean.valueOf(readonly));
        coverage.put("rewriteSqlReferencesMv", Boolean.valueOf(referencesMv));
        coverage.put("rewriteSqlAvoidsOriginalSources", Boolean.valueOf(avoidsOriginalSources));

        List<Map<String, Object>> reasons = new ArrayList<Map<String, Object>>();
        if (!readonly) {
            reasons.add(reason(
                REWRITE_SQL_NOT_READONLY,
                "rewriteSql 必须是单条只读 SELECT/WITH 查询，不能包含 DDL、DML、CALL 或多语句。"
            ));
        }
        if (!referencesMv) {
            reasons.add(reason(
                REWRITE_SQL_MV_REFERENCE_REQUIRED,
                "rewriteSql 必须在 FROM/JOIN 中引用生成的物化视图名称。"
            ));
        }
        if (!avoidsOriginalSources) {
            Map<String, Object> reason = reason(
                REWRITE_SQL_ACCESSES_ORIGINAL_SOURCE,
                "rewriteSql 不能继续从原始基表读取数据。"
            );
            reason.put("originalSources", accessedOriginalSources);
            reasons.add(reason);
        }
        if (!coversProjection) {
            reasons.add(reason(
                REWRITE_PROJECTION_NOT_COVERED,
                "rewriteSql 的投影无法由 MV 字段或可重算指标完整覆盖。"
            ));
        }
        if (!coversFilters) {
            reasons.add(reason(
                REWRITE_FILTER_NOT_COVERED,
                "rewriteSql 的参数化过滤字段未被 MV 字段完整覆盖。"
            ));
        }
        if (!coversGrouping) {
            reasons.add(reason(
                REWRITE_GROUPING_NOT_COVERED,
                "rewriteSql 的 GROUP BY 粒度无法由 MV 字段或上卷表达式完整覆盖。"
            ));
        }
        if (!coversMeasures) {
            reasons.add(reason(
                REWRITE_MEASURE_NOT_COVERED,
                "rewriteSql 的指标无法由 MV 指标列或公共子图输出字段安全重算。"
            ));
        }
        if (!coversSecurity) {
            reasons.add(reason(
                REWRITE_SECURITY_PREDICATE_NOT_COVERED,
                "rewriteSql 的安全谓词字段未被 MV 字段完整覆盖。"
            ));
        }

        return new ValidationResult(
            reasons.isEmpty() && StringUtils.hasText(normalizedRewrite) ? normalizedRewrite + ";" : null,
            coverage,
            reasons
        );
    }

    private static boolean coversProjection(List<Map<String, Object>> projections,
                                            List<Map<String, Object>> measures,
                                            Set<String> coverageFields,
                                            Set<String> selectedOutputs,
                                            String rewriteSql) {
        if (projections.isEmpty()) {
            return StringUtils.hasText(rewriteSql) && !containsSelectStar(rewriteSql);
        }
        if (containsSelectStar(rewriteSql)) {
            return false;
        }
        Set<String> combined = combinedCoverage(coverageFields, selectedOutputs);
        for (Map<String, Object> projection : projections) {
            String expression = stripAlias(text(projection.get("expression")), text(projection.get("alias")));
            String alias = text(projection.get("alias"));
            if (isAggregationProjection(projection, expression)) {
                if (!aggregationCovered(expression, alias, stringList(projection.get("sourceColumns")), measures, combined)) {
                    return false;
                }
                continue;
            }
            if (coveredByAliasOrExpression(expression, alias, stringList(projection.get("sourceColumns")), combined, rewriteSql)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean coversPredicates(List<Map<String, Object>> predicates, Set<String> coverageFields) {
        for (Map<String, Object> predicate : predicates) {
            List<String> sourceColumns = stringList(predicate.get("sourceColumns"));
            if (sourceColumns.isEmpty()) {
                sourceColumns = Collections.singletonList(leftPredicateField(text(predicate.get("expression"))));
            }
            if (!allFieldsCovered(sourceColumns, coverageFields)) {
                return false;
            }
        }
        return true;
    }

    private static boolean coversCommonSubgraphProjection(List<String> requiredColumns,
                                                          Set<String> mvFields,
                                                          String rewriteSql) {
        if (containsSelectStar(rewriteSql)) {
            return false;
        }
        if (requiredColumns == null || requiredColumns.isEmpty()) {
            return StringUtils.hasText(rewriteSql);
        }
        return allFieldsCovered(requiredColumns, mvFields);
    }

    private static boolean coversGrouping(List<Map<String, Object>> groupBy,
                                          Set<String> coverageFields) {
        if (groupBy.isEmpty()) {
            return true;
        }
        for (Map<String, Object> item : groupBy) {
            String expression = text(item.get("expression"));
            if (coveredByAliasOrExpression(expression, "", stringList(item.get("sourceColumns")), coverageFields, "")) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean coversMeasures(List<Map<String, Object>> aggregations,
                                          List<Map<String, Object>> measures,
                                          Set<String> coverageFields) {
        if (aggregations.isEmpty()) {
            return true;
        }
        for (Map<String, Object> aggregation : aggregations) {
            String expression = text(aggregation.get("expression"));
            if (measureCoversExpression(expression, "", measures, coverageFields)) {
                continue;
            }
            if (allFieldsCovered(stringList(aggregation.get("sourceColumns")), coverageFields)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean coveredByAliasOrExpression(String expression,
                                                      String alias,
                                                      List<String> sourceColumns,
                                                      Set<String> coverageFields,
                                                      String rewriteSql) {
        if (StringUtils.hasText(alias) && containsCoverageField(coverageFields, alias)) {
            return true;
        }
        if (StringUtils.hasText(expression) && containsCoverageField(coverageFields, expression)) {
            return true;
        }
        if (allFieldsCovered(sourceColumns, coverageFields)) {
            return true;
        }
        return false;
    }

    private static boolean aggregationCovered(String expression,
                                              String alias,
                                              List<String> sourceColumns,
                                              List<Map<String, Object>> measures,
                                              Set<String> coverageFields) {
        if (measureCoversExpression(expression, alias, measures, coverageFields)) {
            return true;
        }
        if (StringUtils.hasText(alias) && containsCoverageField(coverageFields, alias)) {
            return true;
        }
        return allFieldsCovered(sourceColumns, coverageFields);
    }

    private static boolean measureCoversExpression(String expression,
                                                   String alias,
                                                   List<Map<String, Object>> measures,
                                                   Set<String> coverageFields) {
        String normalizedExpression = normalizeExpression(stripAlias(expression, alias));
        for (Map<String, Object> measure : measures) {
            if (normalizedExpression.equals(normalizeExpression(text(measure.get("sourceExpression"))))) {
                List<Map<String, Object>> components = mapList(measure.get("components"));
                if (components.isEmpty()) {
                    return containsCoverageField(coverageFields, text(measure.get("name")));
                }
                return componentsCovered(components, coverageFields);
            }
            if (StringUtils.hasText(alias)
                && alias.equalsIgnoreCase(text(measure.get("name")))
                && containsCoverageField(coverageFields, text(measure.get("name")))) {
                return true;
            }
            for (Map<String, Object> component : mapList(measure.get("components"))) {
                if (normalizedExpression.equals(normalizeExpression(text(component.get("sourceExpression"))))) {
                    return containsCoverageField(coverageFields, text(component.get("name")));
                }
            }
        }
        return false;
    }

    private static boolean componentsCovered(List<Map<String, Object>> components, Set<String> coverageFields) {
        if (components.isEmpty()) {
            return false;
        }
        for (Map<String, Object> component : components) {
            if (!containsCoverageField(coverageFields, text(component.get("name")))) {
                return false;
            }
        }
        return true;
    }

    private static boolean rootCountProjectionPreservedByMvRewrite(Map<String, Object> advancedStructureProfile,
                                                                   String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return false;
        }
        if (advancedStructureProfile == null || advancedStructureProfile.isEmpty()) {
            return false;
        }
        List<Map<String, Object>> projections = mapList(advancedStructureProfile.get("projections"));
        if (projections.size() != 1) {
            return false;
        }
        String expression = stripAlias(
            text(projections.get(0).get("expression")),
            text(projections.get(0).get("alias"))
        );
        if (!isCountAny(expression)) {
            return false;
        }
        String normalizedRewrite = normalizeExpression(selectList(rewriteSql));
        return normalizedRewrite.startsWith("COUNT(*)")
            || normalizedRewrite.startsWith("COUNT(1)");
    }

    private static boolean isCountAny(String expression) {
        String normalized = normalizeExpression(expression);
        return normalized.startsWith("COUNT(*)")
            || normalized.startsWith("COUNT(1)");
    }

    private static boolean allFieldsCovered(List<String> fields, Set<String> coverageFields) {
        if (fields == null || fields.isEmpty()) {
            return false;
        }
        for (String field : fields) {
            if (!containsCoverageField(coverageFields, field)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsCoverageField(Set<String> coverageFields, String field) {
        if (!StringUtils.hasText(field)) {
            return true;
        }
        String cleaned = cleanReference(field);
        return coverageFields.contains(normalizeExpression(cleaned))
            || coverageFields.contains(normalizeExpression(unqualifiedName(cleaned)));
    }

    private static Set<String> combinedCoverage(Set<String> left, Set<String> right) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    private static Set<String> coverageFields(ValidationInput input) {
        return coverageFields(input.mvFieldNames, input.additionalCoverageReferences);
    }

    private static Set<String> coverageFields(List<String> mvFieldNames, List<String> additionalCoverageReferences) {
        LinkedHashSet<String> fields = new LinkedHashSet<String>();
        addCoverageFields(fields, mvFieldNames);
        addCoverageFields(fields, additionalCoverageReferences);
        return fields;
    }

    private static void addCoverageFields(Set<String> target, List<String> values) {
        for (String value : values) {
            addCoverageField(target, value);
        }
    }

    private static void addCoverageField(Set<String> target, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String cleaned = cleanReference(value);
        target.add(normalizeExpression(cleaned));
        target.add(normalizeExpression(unqualifiedName(cleaned)));
    }

    private static Set<String> selectedOutputs(String rewriteSql) {
        if (!StringUtils.hasText(rewriteSql)) {
            return Collections.emptySet();
        }
        String selectList = selectList(rewriteSql);
        if (!StringUtils.hasText(selectList)) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> outputs = new LinkedHashSet<String>();
        for (String item : splitTopLevel(selectList, ',')) {
            String expression = item.trim();
            Matcher asMatcher = ALIAS_PATTERN.matcher(expression);
            if (asMatcher.find()) {
                addCoverageField(outputs, asMatcher.group(1));
                addCoverageField(outputs, expression.substring(0, asMatcher.start()).trim());
                continue;
            }
            List<String> tokens = splitWhitespace(expression);
            if (tokens.size() > 1 && !containsTopLevelFunction(expression)) {
                addCoverageField(outputs, tokens.get(tokens.size() - 1));
            }
            addCoverageField(outputs, expression);
        }
        return outputs;
    }

    private static String selectList(String sql) {
        String normalized = stripLeadingComments(sql);
        int selectIndex = indexOfTopLevelKeyword(normalized, "SELECT", 0);
        if (selectIndex < 0) {
            return "";
        }
        int fromIndex = indexOfTopLevelKeyword(normalized, "FROM", selectIndex + "SELECT".length());
        if (fromIndex < 0 || fromIndex <= selectIndex) {
            return "";
        }
        return normalized.substring(selectIndex + "SELECT".length(), fromIndex).trim();
    }

    private static List<String> accessedOriginalSources(String rewriteSql,
                                                        String mvName,
                                                        Set<String> originalSources) {
        if (!StringUtils.hasText(rewriteSql) || originalSources.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String relation : fromJoinRelations(rewriteSql)) {
            if (sameRelation(relation, mvName)) {
                continue;
            }
            String normalized = relationKey(relation);
            String unqualified = relationKey(unqualifiedName(relation));
            if (originalSources.contains(normalized) || originalSources.contains(unqualified)) {
                result.add(cleanReference(relation));
            }
        }
        return new ArrayList<String>(result);
    }

    private static boolean referencesMv(String rewriteSql, String mvName) {
        if (!StringUtils.hasText(rewriteSql) || !StringUtils.hasText(mvName)) {
            return false;
        }
        for (String relation : fromJoinRelations(rewriteSql)) {
            if (sameRelation(relation, mvName)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> fromJoinRelations(String sql) {
        if (!StringUtils.hasText(sql)) {
            return Collections.emptyList();
        }
        String withoutStrings = stripStringLiterals(stripSqlComments(sql));
        Matcher matcher = RELATION_PATTERN.matcher(withoutStrings);
        List<String> relations = new ArrayList<String>();
        while (matcher.find()) {
            relations.add(cleanRelationToken(matcher.group(2)));
        }
        return relations;
    }

    private static Set<String> originalBaseSources(Map<String, Object> advancedStructureProfile) {
        LinkedHashSet<String> sources = new LinkedHashSet<String>();
        for (Map<String, Object> table : mapList(advancedStructureProfile.get("tables"))) {
            String sourceType = text(table.get("sourceType"));
            if (StringUtils.hasText(sourceType) && !"BASE_TABLE".equals(sourceType)) {
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

    private static boolean isReadonly(String normalizedRewrite) {
        if (!StringUtils.hasText(normalizedRewrite)) {
            return false;
        }
        if (normalizedRewrite.indexOf(';') >= 0) {
            return false;
        }
        String withoutLeadingComments = stripLeadingComments(normalizedRewrite);
        if (!StringUtils.hasText(withoutLeadingComments)) {
            return false;
        }
        String upper = withoutLeadingComments.toUpperCase(Locale.ROOT);
        boolean readonlyPrefix = false;
        for (String prefix : READONLY_PREFIXES) {
            if ((upper.startsWith(prefix) && wordBoundary(upper, 0, prefix.length())) || upper.equals(prefix)) {
                readonlyPrefix = true;
                break;
            }
        }
        if (!readonlyPrefix) {
            return false;
        }
        String stripped = stripStringLiterals(stripSqlComments(withoutLeadingComments)).toUpperCase(Locale.ROOT);
        for (String token : FORBIDDEN_TOKENS) {
            if (containsToken(stripped, token)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeSingleStatement(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static boolean isAggregationProjection(Map<String, Object> projection, String expression) {
        if ("AGGREGATION".equalsIgnoreCase(text(projection.get("expressionType")))) {
            return true;
        }
        String upper = normalizeExpression(expression);
        for (String functionName : AGGREGATE_FUNCTIONS) {
            if (upper.contains(functionName + "(")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSelectStar(String sql) {
        String selectList = selectList(sql);
        if (!StringUtils.hasText(selectList)) {
            return false;
        }
        for (String item : splitTopLevel(selectList, ',')) {
            String normalized = item.trim().replaceAll("\\s+", " ");
            if ("*".equals(normalized) || normalized.matches("(?is)[A-Z_][A-Z0-9_$]*\\.\\*")) {
                return true;
            }
        }
        return false;
    }

    private static String leftPredicateField(String expression) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String[] operators = {" BETWEEN ", " IN ", ">=", "<=", "<>", "!=", "=", ">", "<", " LIKE "};
        String upper = expression.toUpperCase(Locale.ROOT);
        int index = -1;
        for (String operator : operators) {
            index = upper.indexOf(operator);
            if (index >= 0) {
                break;
            }
        }
        return index < 0 ? "" : expression.substring(0, index).trim();
    }

    private static String stripAlias(String expression, String alias) {
        if (!StringUtils.hasText(expression)) {
            return "";
        }
        String result = expression.trim();
        if (StringUtils.hasText(alias)) {
            result = result.replaceAll("(?is)\\s+AS\\s+" + Pattern.quote(alias.trim()) + "\\s*$", "");
        }
        return result.replaceAll("(?is)\\s+AS\\s+[A-Z_][A-Z0-9_$]*\\s*$", "").trim();
    }

    private static int indexOfTopLevelKeyword(String sql, String keyword, int startIndex) {
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        String upper = sql.toUpperCase(Locale.ROOT);
        for (int i = Math.max(0, startIndex); i <= sql.length() - keyword.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
            if (inSingleQuote || inDoubleQuote) {
                continue;
            }
            if (ch == '(') {
                depth++;
                continue;
            }
            if (ch == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && upper.startsWith(keyword, i) && wordBoundary(sql, i, keyword.length())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean wordBoundary(String sql, int start, int length) {
        int before = start - 1;
        int after = start + length;
        return (before < 0 || !isIdentifierChar(sql.charAt(before)))
            && (after >= sql.length() || !isIdentifierChar(sql.charAt(after)));
    }

    private static boolean isIdentifierChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
    }

    private static List<String> splitTopLevel(String value, char delimiter) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (ch == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (ch == '(' && !inSingleQuote && !inDoubleQuote) {
                depth++;
            } else if (ch == ')' && !inSingleQuote && !inDoubleQuote) {
                depth = Math.max(0, depth - 1);
            }
            if (ch == delimiter && depth == 0 && !inSingleQuote && !inDoubleQuote) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private static List<String> splitWhitespace(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.trim().split("\\s+");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part);
            }
        }
        return result;
    }

    private static boolean containsTopLevelFunction(String value) {
        return StringUtils.hasText(value) && value.matches("(?is).*\\b[A-Z_][A-Z0-9_]*\\s*\\(.*");
    }

    private static boolean containsToken(String text, String token) {
        if (!StringUtils.hasText(text) || !StringUtils.hasText(token)) {
            return false;
        }
        Pattern pattern = Pattern.compile("(^|[^A-Z0-9_])" + Pattern.quote(token) + "([^A-Z0-9_]|$)");
        return pattern.matcher(text).find();
    }

    private static String stripLeadingComments(String sql) {
        String remaining = sql == null ? "" : sql.trim();
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            remaining = remaining.trim();
            if (remaining.startsWith("--")) {
                int lineBreak = remaining.indexOf('\n');
                remaining = lineBreak >= 0 ? remaining.substring(lineBreak + 1) : "";
                stripped = true;
                continue;
            }
            if (remaining.startsWith("/*")) {
                int commentEnd = remaining.indexOf("*/");
                if (commentEnd < 0) {
                    return "";
                }
                remaining = remaining.substring(commentEnd + 2);
                stripped = true;
            }
        }
        return remaining.trim();
    }

    private static String stripSqlComments(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        return sql.replaceAll("(?is)/\\*.*?\\*/", " ").replaceAll("(?m)--.*?$", " ");
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

    private static String normalizeSql(String sql) {
        return StringUtils.hasText(sql)
            ? sql.replace("`", "")
                .replace("\"", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT)
            : "";
    }

    private static String normalizeExpression(String expression) {
        return normalizeSql(expression);
    }

    private static boolean sameRelation(String left, String right) {
        return StringUtils.hasText(left)
            && StringUtils.hasText(right)
            && (relationKey(left).equals(relationKey(right))
            || relationKey(unqualifiedName(left)).equals(relationKey(unqualifiedName(right))));
    }

    private static String relationKey(String value) {
        return cleanReference(value).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private static String cleanRelationToken(String value) {
        String cleaned = cleanReference(value);
        while (cleaned.endsWith(")") || cleaned.endsWith(",")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private static String cleanReference(String value) {
        return StringUtils.hasText(value)
            ? value.replace("`", "").replace("\"", "").trim()
            : "";
    }

    private static String unqualifiedName(String expression) {
        String cleaned = cleanReference(expression);
        int index = cleaned.lastIndexOf('.');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
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

    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof Iterable<?>)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : (Iterable<?>) value) {
            if (item instanceof Map<?, ?>) {
                result.add(copyMap((Map<?, ?>) item));
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        LinkedHashMap<String, Object> target = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    static final class ValidationInput {
        private final String sourceSql;
        private final String mvType;
        private final String mvName;
        private final String rewriteSql;
        private final Map<String, Object> advancedStructureProfile;
        private final L2PredicateClassifier.PredicateClassificationResult predicateClassification;
        private final List<Map<String, Object>> measures;
        private final List<String> mvFieldNames;
        private final List<String> additionalCoverageReferences;

        ValidationInput(String sourceSql,
                        String mvType,
                        String mvName,
                        String rewriteSql,
                        Map<String, Object> advancedStructureProfile,
                        L2PredicateClassifier.PredicateClassificationResult predicateClassification,
                        List<Map<String, Object>> measures,
                        List<String> mvFieldNames,
                        List<String> additionalCoverageReferences) {
            this.sourceSql = text(sourceSql);
            this.mvType = text(mvType);
            this.mvName = text(mvName);
            this.rewriteSql = text(rewriteSql);
            this.advancedStructureProfile = advancedStructureProfile == null
                ? Collections.<String, Object>emptyMap()
                : new LinkedHashMap<String, Object>(advancedStructureProfile);
            this.predicateClassification = predicateClassification;
            this.measures = immutableMapList(measures);
            this.mvFieldNames = immutableStringList(mvFieldNames);
            this.additionalCoverageReferences = immutableStringList(additionalCoverageReferences);
        }

        private static ValidationInput empty() {
            return new ValidationInput(
                "",
                "",
                "",
                "",
                Collections.<String, Object>emptyMap(),
                null,
                Collections.<Map<String, Object>>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
            );
        }
    }

    static final class ValidationResult {
        private final String rewriteSql;
        private final Map<String, Object> coverage;
        private final List<Map<String, Object>> blockingReasons;

        ValidationResult(String rewriteSql,
                         Map<String, Object> coverage,
                         List<Map<String, Object>> blockingReasons) {
            this.rewriteSql = rewriteSql;
            this.coverage = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(coverage));
            this.blockingReasons = immutableMapList(blockingReasons);
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        Map<String, Object> getCoverage() {
            return coverage;
        }

        List<Map<String, Object>> getBlockingReasons() {
            return blockingReasons;
        }

        boolean isGenerated() {
            return blockingReasons.isEmpty() && StringUtils.hasText(rewriteSql);
        }
    }

    private static List<String> immutableStringList(List<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<String>(source));
    }

    private static List<Map<String, Object>> immutableMapList(List<Map<String, Object>> source) {
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
