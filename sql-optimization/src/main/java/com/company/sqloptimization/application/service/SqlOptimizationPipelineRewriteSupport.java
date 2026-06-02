package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.springframework.util.StringUtils;

abstract class SqlOptimizationPipelineRewriteSupport extends SqlOptimizationPipelineCollectorSupport {

    protected int calculateRecommendationConfidence(ParsedSqlProfile profile,
                                                  RewriteOutcome outcome,
                                                  List<Map<String, Object>> unappliedRules) {
        return clamp(62 + outcome.appliedRules.size() * 6 - unappliedRules.size() * 5 - profile.getWarnings().size() * 2, 25, 88);
    }

    protected RewriteOutcome applyCalciteRewriteRules(SqlNode statement) {
        LinkedHashSet<String> appliedRules = new LinkedHashSet<String>();
        SqlNode rewritten = rewriteCalciteNode(statement, appliedRules);
        String rewrittenSql = rewritten == null
            ? ""
            : removeSimpleIdentifierQuotes(rewritten.toString()).replaceAll("\\s+", " ").trim();
        return new RewriteOutcome(rewrittenSql, new ArrayList<String>(appliedRules));
    }

    protected SqlNode rewriteCalciteNode(SqlNode node, Set<String> appliedRules) {
        if (node == null) {
            return null;
        }
        if (node instanceof SqlOrderBy) {
            SqlOrderBy orderBy = (SqlOrderBy) node;
            rewriteCalciteNode(orderBy.query, appliedRules);
            deduplicateCalciteNodeList(orderBy.orderList, appliedRules, "DEDUPLICATE_ORDER_BY_KEYS");
            return orderBy;
        }
        if (node instanceof SqlWith) {
            SqlWith with = (SqlWith) node;
            if (with.withList != null) {
                for (SqlNode itemNode : with.withList.getList()) {
                    if (itemNode instanceof SqlWithItem) {
                        SqlWithItem item = (SqlWithItem) itemNode;
                        item.query = rewriteCalciteNode(item.query, appliedRules);
                    } else {
                        rewriteCalciteNode(itemNode, appliedRules);
                    }
                }
            }
            with.body = rewriteCalciteNode(with.body, appliedRules);
            return with;
        }
        if (node instanceof SqlSelect) {
            rewriteCalciteSelect((SqlSelect) node, appliedRules);
            return node;
        }
        if (node instanceof SqlJoin) {
            SqlJoin join = (SqlJoin) node;
            join.setLeft(rewriteCalciteNode(join.getLeft(), appliedRules));
            join.setRight(rewriteCalciteNode(join.getRight(), appliedRules));
            return join;
        }
        if (node instanceof SqlCall) {
            SqlCall call = (SqlCall) node;
            List<SqlNode> operands = call.getOperandList();
            for (int index = 0; index < operands.size(); index++) {
                SqlNode operand = operands.get(index);
                SqlNode rewrittenOperand = rewriteCalciteNode(operand, appliedRules);
                if (rewrittenOperand != operand) {
                    try {
                        call.setOperand(index, rewrittenOperand);
                    } catch (UnsupportedOperationException ex) {
                        // 部分 Calcite 节点操作数不可变；遍历仍会继续改写可变子节点。
                    }
                }
            }
        }
        return node;
    }

    protected void rewriteCalciteSelect(SqlSelect select, Set<String> appliedRules) {
        if (select == null) {
            return;
        }
        select.setFrom(rewriteCalciteNode(select.getFrom(), appliedRules));
        if (select.getSelectList() != null) {
            for (int index = 0; index < select.getSelectList().size(); index++) {
                SqlNode item = select.getSelectList().get(index);
                SqlNode rewrittenItem = rewriteCountLiteralProjection(item, appliedRules);
                if (rewrittenItem != item) {
                    select.getSelectList().set(index, rewrittenItem);
                }
                rewriteCalciteNode(rewrittenItem, appliedRules);
            }
        }
        if (select.getWhere() != null) {
            SqlNode deduplicatedWhere = deduplicateCalciteAndExpression(select.getWhere());
            if (!normalizeCalciteKey(select.getWhere()).equals(normalizeCalciteKey(deduplicatedWhere))) {
                select.setWhere(deduplicatedWhere);
                appliedRules.add("DEDUPLICATE_WHERE_PREDICATES");
            }
            rewriteCalciteNode(select.getWhere(), appliedRules);
        }
        if (select.getHaving() != null) {
            SqlNode deduplicatedHaving = deduplicateCalciteAndExpression(select.getHaving());
            if (!normalizeCalciteKey(select.getHaving()).equals(normalizeCalciteKey(deduplicatedHaving))) {
                select.setHaving(deduplicatedHaving);
                appliedRules.add("DEDUPLICATE_HAVING_PREDICATES");
            }
            rewriteCalciteNode(select.getHaving(), appliedRules);
        }
        if (select.getGroup() != null && deduplicateCalciteNodeList(select.getGroup(), appliedRules, "DEDUPLICATE_GROUP_BY_KEYS")) {
            select.setGroupBy(select.getGroup());
        }
        if (select.getOrderList() != null && deduplicateCalciteNodeList(select.getOrderList(), appliedRules, "DEDUPLICATE_ORDER_BY_KEYS")) {
            select.setOrderBy(select.getOrderList());
        }
    }

    protected SqlNode rewriteCountLiteralProjection(SqlNode node, Set<String> appliedRules) {
        if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
            SqlBasicCall asCall = (SqlBasicCall) node;
            List<SqlNode> operands = asCall.getOperandList();
            if (operands.size() >= 2) {
                SqlNode rewritten = rewriteCountLiteralProjection(operands.get(0), appliedRules);
                if (rewritten != operands.get(0)) {
                    return SqlStdOperatorTable.AS.createCall(node.getParserPosition(), rewritten, operands.get(1));
                }
            }
            return node;
        }
        if (!(node instanceof SqlBasicCall)) {
            return node;
        }
        SqlBasicCall call = (SqlBasicCall) node;
        String operatorName = call.getOperator() == null ? "" : call.getOperator().getName();
        List<SqlNode> operands = call.getOperandList();
        if ((call.getKind() == SqlKind.COUNT || "COUNT".equalsIgnoreCase(operatorName))
            && operands.size() == 1
            && isNonNullCalciteLiteral(operands.get(0))) {
            SqlParserPos pos = node.getParserPosition() == null ? SqlParserPos.ZERO : node.getParserPosition();
            appliedRules.add("COUNT_LITERAL_TO_COUNT_STAR");
            return SqlStdOperatorTable.COUNT.createCall(pos, SqlIdentifier.star(pos));
        }
        return node;
    }

    protected boolean deduplicateCalciteNodeList(SqlNodeList nodeList, Set<String> appliedRules, String rule) {
        if (nodeList == null || nodeList.size() <= 1) {
            return false;
        }
        LinkedHashMap<String, SqlNode> unique = new LinkedHashMap<String, SqlNode>();
        for (SqlNode node : nodeList.getList()) {
            unique.put(normalizeCalciteKey(node), node);
        }
        if (unique.size() == nodeList.size()) {
            return false;
        }
        nodeList.clear();
        nodeList.addAll(unique.values());
        appliedRules.add(rule);
        return true;
    }

    protected SqlNode deduplicateCalciteAndExpression(SqlNode expression) {
        List<SqlNode> flattened = new ArrayList<SqlNode>();
        flattenCalciteAndExpression(expression, flattened);
        LinkedHashMap<String, SqlNode> unique = new LinkedHashMap<String, SqlNode>();
        for (SqlNode item : flattened) {
            unique.put(normalizeCalciteKey(item), item);
        }
        return rebuildCalciteAndExpression(unique.values(), expression == null ? SqlParserPos.ZERO : expression.getParserPosition());
    }

    protected void flattenCalciteAndExpression(SqlNode expression, List<SqlNode> collector) {
        if (expression instanceof SqlBasicCall && expression.getKind() == SqlKind.AND) {
            for (SqlNode operand : ((SqlBasicCall) expression).getOperandList()) {
                flattenCalciteAndExpression(operand, collector);
            }
            return;
        }
        if (expression != null) {
            collector.add(expression);
        }
    }

    protected SqlNode rebuildCalciteAndExpression(Collection<SqlNode> expressions, SqlParserPos pos) {
        SqlNode result = null;
        for (SqlNode expression : expressions) {
            if (result == null) {
                result = expression;
            } else {
                result = SqlStdOperatorTable.AND.createCall(pos == null ? SqlParserPos.ZERO : pos, result, expression);
            }
        }
        return result;
    }

    protected String normalizeCalciteKey(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.ROOT);
    }

    protected static String removeSimpleIdentifierQuotes(String value) {
        if (value == null) {
            return "";
        }
        return SIMPLE_BACKTICK_IDENTIFIER_PATTERN.matcher(value).replaceAll("$1");
    }

    protected void finalizeWarnings(ParsedSqlProfile profile) {
        profile.recalculateRepeatedExpressions();
        if (profile.selectStar) {
            profile.warnings.add("SELECT_STAR");
        }
        if (profile.predicateCount == 0) {
            profile.warnings.add("NO_PREDICATE");
        }
        if (profile.orderByCount > 0 && !profile.limitPresent) {
            profile.warnings.add("ORDER_BY_WITHOUT_LIMIT");
        }
        if (profile.joinCount >= 3) {
            profile.warnings.add("HEAVY_JOIN_GRAPH");
        }
        if (profile.joinCount >= 2 && profile.joinCriteriaCount <= profile.joinCount) {
            profile.warnings.add("LARGE_JOIN_PAIR_RISK");
        }
        if (profile.repeatedExpressionCount > 0) {
            profile.warnings.add("REPEATED_EXPRESSION_COMPUTE");
        }
        if (!profile.limitPresent && (profile.selectStar || profile.predicateCount == 0)) {
            profile.warnings.add("LARGE_RESULT_SET_RISK");
        }
        if (profile.scalarSubqueryCount > 0) {
            profile.warnings.add("SCALAR_SUBQUERY_IN_SELECT");
        }
        if (profile.subqueryCount >= 3 || profile.nestedSubqueryDepth >= 2) {
            profile.warnings.add("NESTED_SUBQUERY_RISK");
        }
        if (profile.correlatedSubqueryCount > 0) {
            profile.warnings.add("CORRELATED_SUBQUERY_RISK");
        }
        if (profile.functionWrappedPredicateCount > 0) {
            profile.warnings.add("FUNCTION_WRAPPED_PREDICATE");
        }
        if (profile.notExistsCount > 0) {
            profile.warnings.add("NOT_EXISTS_ANTI_JOIN_RISK");
        }
        if (profile.leadingWildcardLikeCount > 0) {
            profile.warnings.add("LEADING_WILDCARD_LIKE_RISK");
        }
        if (profile.orPredicateCount > 0) {
            profile.warnings.add("OR_PREDICATE_INDEX_RISK");
        }
        if (profile.randomOrderCount > 0) {
            profile.warnings.add("ORDER_BY_RANDOM_RISK");
        }
        if (profile.repeatedTableScanCount > 0) {
            profile.warnings.add("REPEATED_TABLE_SCAN_RISK");
        }
        if (profile.orderByExpressionCount >= 3
            || profile.duplicateOrderByKeyCount > 0
            || (profile.orderByExpressionCount > 0 && profile.groupByCount > 0 && profile.aggregateFunctionCount >= 2)) {
            profile.warnings.add("ORDER_BY_COMPLEXITY_RISK");
        }
        if (profile.joinCount >= 3
            || (profile.joinCount >= 2 && (profile.joinCriteriaCount <= profile.joinCount || profile.subqueryCount > 0))) {
            profile.warnings.add("JOIN_LATENCY_RISK");
        }
        if (profile.aggregateFunctionCount >= 4
            || (profile.aggregateFunctionCount >= 2 && profile.groupByCount >= 3)
            || (profile.aggregateFunctionCount >= 2 && profile.orderByExpressionCount > 0 && profile.groupByCount > 0)
            || profile.largeStringAggregateCount > 0) {
            profile.warnings.add("AGGREGATION_COMPLEXITY_RISK");
        }
        if (profile.groupByCount > 0 && profile.aggregateFunctionCount == 0) {
            profile.warnings.add("GROUP_BY_WITHOUT_AGGREGATE_RISK");
        }
        if (profile.duplicateGroupByKeyCount > 0 || profile.duplicateOrderByKeyCount > 0) {
            profile.warnings.add("DUPLICATE_GROUP_OR_ORDER_KEY_RISK");
        }
        if (profile.repeatedSubqueryCount > 0) {
            profile.warnings.add("REPEATED_SUBQUERY_RISK");
        }
        if (profile.largeStringAggregateCount > 0
            || profile.stringConcatenationCount >= 2
            || profile.stringProjectionCount >= 4
            || (!profile.limitPresent && profile.stringProjectionCount >= 2)) {
            profile.warnings.add("LARGE_STRING_RESULT_RISK");
        }
        if (profile.subqueryCount + profile.joinCount >= 5 || profile.predicateCount >= 8) {
            profile.warnings.add("COMPLEX_QUERY_GRAPH_RISK");
        }
    }

    protected static boolean isStringLikeProjectionKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.replace("\"", "").replace("`", "").trim();
        return STRING_LIKE_PROJECTION_PATTERN.matcher(normalized).find();
    }

    protected LinkedHashMap<AccelerationSuggestionType, String> deriveAccelerationReasons(ParsedSqlProfile profile) {
        LinkedHashMap<AccelerationSuggestionType, String> reasons = new LinkedHashMap<AccelerationSuggestionType, String>();
        if (!profile.aggregateFunctions.isEmpty() || profile.groupByCount > 0 || hasCommonSubgraphSignal(profile)) {
            reasons.put(
                AccelerationSuggestionType.PRECOMPUTE,
                "聚合函数、分组键或可复用公共子图表明存在预计算或物化视图价值。"
            );
        }
        if (!profile.datePredicateColumns.isEmpty() || profile.predicateCount >= 2) {
            reasons.put(
                AccelerationSuggestionType.PARTITION,
                "日期类过滤或重复谓词表明存在分区裁剪机会。"
            );
        }
        if (profile.joinCount > 0) {
            reasons.put(
                AccelerationSuggestionType.BUCKET,
                "join 活动表明分桶对齐或共置可能降低 shuffle 开销。"
            );
        }
        if (profile.joinCount >= 3 || profile.setOperation) {
            reasons.put(
                AccelerationSuggestionType.SPLIT,
                "查询图规模较大，需要评估分阶段执行或拆解方案。"
            );
        }
        if (profile.selectStar || profile.tables.size() >= 4) {
            reasons.put(
                AccelerationSuggestionType.REPLACE,
                "宽投影或大表图表明应考虑用已治理服务对象替换原始查询。"
            );
        }
        return reasons;
    }

    protected boolean hasCommonSubgraphSignal(ParsedSqlProfile profile) {
        if (profile == null) {
            return false;
        }
        Map<String, Object> advancedStructureProfile = profile.toAdvancedStructureProfile();
        return !advancedMapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !advancedMapList(advancedStructureProfile.get("subqueries")).isEmpty()
            || profile.getRepeatedSubqueryCount() > 0;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> advancedMapList(Object value) {
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

    protected LinkedHashMap<AccelerationSuggestionType, String> filterRequestedTypes(
        LinkedHashMap<AccelerationSuggestionType, String> reasons,
        List<AccelerationSuggestionType> requestedTypes
    ) {
        if (requestedTypes == null || requestedTypes.isEmpty() || requestedTypes.contains(AccelerationSuggestionType.ALL)) {
            return new LinkedHashMap<AccelerationSuggestionType, String>(reasons);
        }
        LinkedHashMap<AccelerationSuggestionType, String> filtered = new LinkedHashMap<AccelerationSuggestionType, String>();
        for (AccelerationSuggestionType requestedType : requestedTypes) {
            if (reasons.containsKey(requestedType)) {
                filtered.put(requestedType, reasons.get(requestedType));
            }
        }
        return filtered;
    }

    protected List<OptimizationTaskRisk> buildShapeRisks(ParsedSqlProfile profile) {
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>();
        if (profile == null) {
            return risks;
        }
        if (profile.selectStar) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "SELECT_STAR",
                    "宽投影会隐藏实际列足迹，并降低下游改写确定性。",
                    "批准改写或加速前，请将星号投影替换为显式列。"
                )
            );
        }
        if (profile.predicateCount == 0) {
            risks.add(
                new OptimizationTaskRisk(
                    "HIGH",
                    "FULL_SCAN_RISK",
                    "已解析语句没有过滤谓词，可能扫描完整数据集。",
                    "上线前请添加租户、时间或业务键谓词。"
                )
            );
        }
        if (profile.orderByCount > 0 && !profile.limitPresent) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "ORDER_BY_WITHOUT_LIMIT",
                    "没有限制子句的排序可能产生不必要的宽 shuffle 或内存压力。",
                    "请添加 limit，或将排序工作迁移到预计算服务对象。"
                )
            );
        }
        if (profile.joinCount >= 3) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "HEAVY_JOIN_GRAPH",
                    "已解析查询连接多个数据集，可能需要分阶段执行或更强加速。",
                    "批准前请评审 join 键、过滤位置和服务层替代方案。"
                )
            );
        }
        if (profile.getOrderByExpressionCount() >= 3 || profile.getDuplicateOrderByKeyCount() > 0) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "ORDER_BY_COMPLEXITY_RISK",
                    "已解析语句包含多个或重复的 ORDER BY 键。",
                    "请减少排序键、移除重复排序，或在校验后提供已排序服务输出。"
                )
            );
        }
        if (profile.getJoinCount() >= 2 && (profile.getJoinCriteriaCount() <= profile.getJoinCount() || profile.getSubqueryCount() > 0)) {
            risks.add(
                new OptimizationTaskRisk(
                    "HIGH",
                    "JOIN_LATENCY_RISK",
                    "join 图存在可能导致长时间分布式执行的静态信号。",
                    "请通过访问解析或压测证据确认 join 键、过滤位置和行移动。"
                )
            );
        }
        if (profile.getAggregateFunctionCount() >= 4 || profile.getLargeStringAggregateCount() > 0) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "AGGREGATION_COMPLEXITY_RISK",
                    "聚合数量或字符串聚合表明存在较重的计算和内存压力。",
                    "批准前请预聚合可复用阶段，或评审物化服务对象。"
                )
            );
        }
        return risks;
    }

    protected String buildParseSummary(ParsedSqlProfile profile) {
        return "已解析一条 SELECT 语句，覆盖 "
            + profile.tables.size()
            + " 张表、"
            + profile.joinCount
            + " 个 join、"
            + profile.predicateCount
            + " 个谓词片段。";
    }

    protected String buildParseRecommendation(ParsedSqlProfile profile) {
        if (profile.selectStar) {
            return "请先替换星号投影，再使用同一 AST 画像开展改写和加速评审。";
        }
        if (profile.predicateCount == 0) {
            return "将该语句带入改写或加速规划前，请先补充显式过滤条件。";
        }
        if (!profile.aggregateFunctions.isEmpty()) {
            return "请使用已解析的聚合函数和分组键评估预计算或服务层加速。";
        }
        return "请将 AST 画像作为后续改写校验和加速规划的权威输入。";
    }

    protected int calculateParseConfidence(ParsedSqlProfile profile) {
        return clamp(92 - profile.warnings.size() * 8 - profile.joinCount * 2, 45, 96);
    }

    protected int calculateRewriteConfidence(ParsedSqlProfile profile, RewriteOutcome outcome) {
        return clamp(58 + outcome.appliedRules.size() * 9 - profile.warnings.size() * 4, 35, 92);
    }

    protected int calculateRewriteConfidence(ParsedSqlProfile profile, List<String> appliedRules) {
        int appliedRuleCount = appliedRules == null ? 0 : appliedRules.size();
        return clamp(58 + appliedRuleCount * 9 - profile.warnings.size() * 4, 35, 92);
    }

    protected String rewriteSummary(List<String> appliedRules) {
        return "已生成候选改写 SQL，包含 " + appliedRules.size() + " 条安全 AST 规则。";
    }

    protected String rewriteRecommendation() {
        return "请先将候选改写结果与原始语句做校验，再把批准后的 SQL 带入下一步治理。";
    }

    protected int calculateAccelerationConfidence(ParsedSqlProfile profile,
                                                LinkedHashMap<AccelerationSuggestionType, String> reasons) {
        return clamp(54 + reasons.size() * 8 + profile.aggregateFunctions.size() * 3 - profile.warnings.size() * 3, 40, 90);
    }

    protected boolean isNonNullCalciteLiteral(SqlNode expression) {
        if (!(expression instanceof SqlLiteral)) {
            return false;
        }
        return ((SqlLiteral) expression).getValue() != null;
    }

    protected SqlOptimizationExecutionException invalidTask(String message, String suggestedAction) {
        return new SqlOptimizationExecutionException(
            ErrorCodeConstants.SQL_OPTIMIZATION_TASK_INVALID,
            message,
            suggestedAction,
            false,
            OptimizationTaskPhase.DEEP_PARSING,
            Collections.singletonList(
                new OptimizationTaskRisk(
                    "HIGH",
                    "TASK_INPUT_INVALID",
                    message,
                    suggestedAction
                )
            ),
            null,
            null
        );
    }

    protected SqlOptimizationExecutionException parserFailure(String message,
                                                            String suggestedAction,
                                                            List<OptimizationTaskRisk> risks,
                                                            Exception cause,
                                                            String sqlText) {
        SqlFailurePosition position = analyzeSqlFailurePosition(cause, sqlText);
        return new SqlOptimizationExecutionException(
            ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_PARSER_FAILURE,
            message,
            suggestedAction,
            false,
            OptimizationTaskPhase.DEEP_PARSING,
            risks,
            cause,
            position
        );
    }

    protected String normalizeSql(String sqlText) {
        return SqlDialectNormalizer.normalize(sqlText);
    }

    protected void augmentCalciteProfileFromSqlText(String normalizedSql, ParsedSqlProfile profile) {
        if (!StringUtils.hasText(normalizedSql) || profile == null) {
            return;
        }
        profile.datePredicateColumns.addAll(extractDatePredicateColumns(normalizedSql));
        recordSqlLevelFunctionWrappedPredicates(normalizedSql, profile);
    }

    protected List<String> extractDatePredicateColumns(String expressionSql) {
        if (expressionSql == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        Matcher matcher = DATE_PREDICATE_PATTERN.matcher(expressionSql.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return new ArrayList<String>(columns);
    }

    protected void recordSqlLevelFunctionWrappedPredicates(String normalizedSql, ParsedSqlProfile profile) {
        Matcher matcher = FUNCTION_WRAPPED_PREDICATE_PATTERN.matcher(normalizedSql);
        int detected = 0;
        while (matcher.find()) {
            String expression = matcher.group().replace('\n', ' ').replace('\r', ' ').trim().replaceAll("\\s+", " ");
            if (StringUtils.hasText(expression) && !profile.functionWrappedPredicateExpressions.contains(expression)) {
                profile.functionWrappedPredicateExpressions.add(expression);
            }
            detected++;
        }
        if (detected > 0 && profile.functionWrappedPredicateCount == 0) {
            profile.functionWrappedPredicateCount = detected;
        }
    }

    protected SqlFailurePosition analyzeSqlFailurePosition(Throwable cause, String sqlText) {
        String message = collectExceptionMessage(cause);
        Integer line = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)"), 1);
        Integer column = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)"), 2);
        if (line == null || column == null) {
            line = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*:\\s*(\\d+)"), 1);
            column = findFirstInteger(message, Pattern.compile("(?i)line\\s+(\\d+)\\s*:\\s*(\\d+)"), 2);
        }
        String token = firstRegexGroup(message, Pattern.compile("(?i)unexpected token:\\s*\"([^\"]+)\""));
        if (token == null) {
            token = firstRegexGroup(message, Pattern.compile("(?i)mismatched input ['\"]([^'\"]+)['\"]"));
        }
        if (token == null) {
            token = firstRegexGroup(message, Pattern.compile("(?i)extraneous input ['\"]([^'\"]+)['\"]"));
        }
        SqlFailurePosition selectProjectionFailure = guessSelectProjectionFailure(sqlText);
        if (selectProjectionFailure != null) {
            return selectProjectionFailure;
        }
        Integer offset = toOffset(sqlText, line, column);
        Integer tokenOffset = findTokenOffset(sqlText, token, line, offset);
        if (tokenOffset != null && (offset == null || !startsWithToken(sqlText, offset.intValue(), token))) {
            offset = tokenOffset;
            int[] lineColumn = toLineColumn(sqlText, tokenOffset.intValue());
            line = Integer.valueOf(lineColumn[0]);
            column = Integer.valueOf(lineColumn[1]);
        }
        return new SqlFailurePosition(line, column, offset, token, snippet(sqlText, offset));
    }

    protected SqlFailurePosition guessSelectProjectionFailure(String sqlText) {
        if (sqlText == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?is)^\\s*SELECT\\s+(FROM|WHERE|GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT)\\b")
            .matcher(sqlText);
        if (!matcher.find()) {
            return null;
        }
        int offset = matcher.start(1);
        String token = matcher.group(1).trim().split("\\s+")[0];
        int[] lineColumn = toLineColumn(sqlText, offset);
        return new SqlFailurePosition(
            Integer.valueOf(lineColumn[0]),
            Integer.valueOf(lineColumn[1]),
            Integer.valueOf(offset),
            token,
            snippet(sqlText, Integer.valueOf(offset))
        );
    }

    protected String collectExceptionMessage(Throwable cause) {
        StringBuilder builder = new StringBuilder();
        Throwable current = cause;
        while (current != null) {
            if (current.getMessage() != null) {
                if (builder.length() > 0) {
                    builder.append(" | ");
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString();
    }

    protected Integer findFirstInteger(String message, Pattern pattern, int group) {
        if (message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(group));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    protected String firstRegexGroup(String message, Pattern pattern) {
        if (message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    protected Integer findTokenOffset(String sqlText, String token, Integer line, Integer anchorOffset) {
        if (sqlText == null || token == null || token.trim().isEmpty() || token.startsWith("<")) {
            return null;
        }
        String upperSql = sqlText.toUpperCase(Locale.ROOT);
        String upperToken = token.toUpperCase(Locale.ROOT);
        int start = 0;
        int end = sqlText.length();
        if (line != null && line.intValue() > 0) {
            int[] bounds = lineBounds(sqlText, line.intValue());
            start = bounds[0];
            end = bounds[1];
        } else if (anchorOffset != null) {
            start = Math.max(0, Math.min(anchorOffset.intValue(), sqlText.length()));
        }
        int found = upperSql.indexOf(upperToken, start);
        if (found >= 0 && found < end) {
            return Integer.valueOf(found);
        }
        found = upperSql.indexOf(upperToken);
        return found >= 0 ? Integer.valueOf(found) : null;
    }

    protected int[] lineBounds(String sqlText, int line) {
        int currentLine = 1;
        int start = 0;
        for (int index = 0; index < sqlText.length(); index++) {
            if (currentLine == line) {
                start = index;
                break;
            }
            if (sqlText.charAt(index) == '\n') {
                currentLine++;
                start = index + 1;
            }
        }
        int end = sqlText.length();
        for (int index = start; index < sqlText.length(); index++) {
            if (sqlText.charAt(index) == '\n' || sqlText.charAt(index) == '\r') {
                end = index;
                break;
            }
        }
        return new int[] {start, end};
    }

    protected boolean startsWithToken(String sqlText, int offset, String token) {
        if (sqlText == null || token == null || offset < 0 || offset + token.length() > sqlText.length()) {
            return false;
        }
        return sqlText.regionMatches(true, offset, token, 0, token.length());
    }

    protected Integer toOffset(String sqlText, Integer line, Integer column) {
        if (sqlText == null || line == null || column == null || line.intValue() <= 0 || column.intValue() <= 0) {
            return null;
        }
        int currentLine = 1;
        int currentColumn = 1;
        for (int index = 0; index < sqlText.length(); index++) {
            if (currentLine == line.intValue() && currentColumn == column.intValue()) {
                return Integer.valueOf(index);
            }
            char current = sqlText.charAt(index);
            if (current == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }
        return currentLine == line.intValue() && currentColumn == column.intValue()
            ? Integer.valueOf(sqlText.length())
            : null;
    }

    protected int[] toLineColumn(String sqlText, int offset) {
        int line = 1;
        int column = 1;
        int safeOffset = Math.max(0, Math.min(offset, sqlText == null ? 0 : sqlText.length()));
        for (int index = 0; index < safeOffset; index++) {
            char current = sqlText.charAt(index);
            if (current == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] {line, column};
    }

    protected String snippet(String sqlText, Integer offset) {
        if (sqlText == null || offset == null) {
            return null;
        }
        int safeOffset = Math.max(0, Math.min(offset.intValue(), sqlText.length()));
        int start = Math.max(0, safeOffset - 30);
        int end = Math.min(sqlText.length(), safeOffset + 30);
        return sqlText.substring(start, end).replace('\n', ' ').replace('\r', ' ').trim();
    }

    protected List<String> deduplicate(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(new LinkedHashSet<String>(input));
    }

    protected int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
