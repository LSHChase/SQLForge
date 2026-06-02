package com.company.sqloptimization.application.service;

import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
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

abstract class SqlOptimizationPipelineModelSupport {

    protected static final Pattern DATE_PREDICATE_PATTERN =
        Pattern.compile("[`\"]?([A-Z0-9_\\.]*?(DATE|TIME|DTE|DT|DAY)[A-Z0-9_\\.]*)[`\"]?\\s*(=|>=|<=|>|<|BETWEEN|IN)");
    protected static final Pattern FUNCTION_WRAPPED_PREDICATE_PATTERN =
        Pattern.compile("(?is)\\b[A-Z_][A-Z0-9_]*\\s*\\([^()]*\\)\\s*(?:=|<>|!=|>=|<=|>|<)\\s*(?:DATE\\s+)?(?:'[^']*'|\\d+(?:\\.\\d+)?)");
    protected static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "COUNT", "SUM", "AVG", "MIN", "MAX", "APPROX_DISTINCT", "GROUP_CONCAT", "STRING_AGG", "LISTAGG"
        ));
    protected static final Set<String> STRING_AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("GROUP_CONCAT", "STRING_AGG", "LISTAGG"));
    protected static final Set<String> STRING_CONCAT_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("CONCAT", "CONCAT_WS"));
    protected static final Set<String> STRING_SCALAR_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "LOWER", "UPPER", "SUBSTR", "SUBSTRING", "TRIM", "LTRIM", "RTRIM", "REPLACE", "REGEXP_REPLACE", "FORMAT"
        ));
    protected static final Set<String> BUILT_IN_SCALAR_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "DATE_TRUNC", "CAST", "COALESCE", "IF", "NULLIF", "LOWER", "UPPER", "CONCAT", "CONCAT_WS",
            "SUBSTR", "SUBSTRING", "TRIM", "LTRIM", "RTRIM", "REPLACE", "REGEXP_REPLACE", "FORMAT"
        ));
    protected static final Set<String> TIME_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "DATE_TRUNC", "TRUNC", "TO_DATE", "DATE_FORMAT", "YEAR", "MONTH", "DAY", "DAY_OF_MONTH",
            "HOUR", "MINUTE", "SECOND", "EXTRACT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "LOCALTIME", "LOCALTIMESTAMP", "NOW"
        ));
    protected static final Set<String> NON_DETERMINISTIC_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "RAND", "RANDOM", "UUID", "NOW", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "LOCALTIME", "LOCALTIMESTAMP", "CURRENT_USER", "SESSION_USER"
        ));
    protected static final int PRODUCTION_TARGET_CONCURRENCY = 10000;
    protected static final long PRODUCTION_TARGET_DAILY_QUERY_VOLUME = 10000000L;
    protected static final long PRODUCTION_TARGET_DATASET_SIZE_BYTES = 30000000000000000L;
    protected static final int PRODUCTION_TARGET_REPLAY_HOURS = 24;
    protected static final List<String> PRODUCTION_SCALE_REQUIRED_EVIDENCE = Collections.unmodifiableList(
        Arrays.asList(
            "VERIFIED_10000_CONCURRENCY",
            "VERIFIED_10M_DAILY_QUERY_VOLUME",
            "VERIFIED_30PB_DATA_LAYOUT",
            "VERIFIED_24H_WORKLOAD_REPLAY",
            "VERIFIED_P95_P99_LATENCY",
            "VERIFIED_SCAN_BYTES",
            "VERIFIED_CPU_USAGE",
            "VERIFIED_QUEUE_WAIT",
            "VERIFIED_COST_BILL"
        )
    );
    protected static final Pattern STRING_LIKE_PROJECTION_PATTERN =
        Pattern.compile("(?i)(^|[._])(?:name|title|desc|description|comment|remark|note|text|content|address|email|phone|status|type|code|label)$");
    protected static final Pattern STRING_AGGREGATE_PATTERN =
        Pattern.compile("(?i)\\b(GROUP_CONCAT|STRING_AGG|LISTAGG)\\s*\\(");
    protected static final Pattern HAVING_PATTERN = Pattern.compile("(?is)\\bHAVING\\b");
    protected static final Pattern IN_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bIN\\s*\\(\\s*SELECT\\b");
    protected static final Pattern EXISTS_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bEXISTS\\s*\\(\\s*SELECT\\b");
    protected static final Pattern NOT_IN_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bNOT\\s+IN\\s*\\(\\s*SELECT\\b");
    protected static final Pattern LEFT_JOIN_NULL_PATTERN =
        Pattern.compile("(?is)\\bLEFT\\s+(?:OUTER\\s+)?JOIN\\b.*\\bIS\\s+NULL\\b");
    protected static final Pattern CROSS_JOIN_PATTERN = Pattern.compile("(?is)\\bCROSS\\s+JOIN\\b");
    protected static final Pattern CAST_COMPARISON_PATTERN =
        Pattern.compile("(?is)(CAST\\s*\\([^)]*\\)\\s*=|=\\s*CAST\\s*\\()");
    protected static final Pattern STRING_NUMERIC_COMPARISON_PATTERN =
        Pattern.compile("(?is)\\b[A-Za-z_][A-Za-z0-9_\\.]*\\s*=\\s*'\\d+(?:\\.\\d+)?'");
    protected static final Pattern PREFIX_LIKE_PATTERN = Pattern.compile("(?is)\\bLIKE\\s+'[^%_][^']*%'");
    protected static final Pattern REGEXP_PATTERN =
        Pattern.compile("(?is)\\b(REGEXP_LIKE|RLIKE|REGEXP)\\b");
    protected static final Pattern LONG_IN_LIST_PATTERN =
        Pattern.compile("(?is)\\bIN\\s*\\((?:\\s*[^,()]+\\s*,){8,}[^()]*\\)");
    protected static final Pattern ROW_NUMBER_PATTERN =
        Pattern.compile("(?is)\\bROW_NUMBER\\s*\\(\\s*\\)\\s*OVER\\s*\\(");
    protected static final Pattern UNION_DISTINCT_PATTERN = Pattern.compile("(?is)\\bUNION\\b(?!\\s+ALL\\b)");
    protected static final Pattern INTERSECT_PATTERN = Pattern.compile("(?is)\\bINTERSECT\\b");
    protected static final Pattern EXCEPT_PATTERN = Pattern.compile("(?is)\\b(EXCEPT|MINUS)\\b");
    protected static final Pattern JSON_EXTRACT_PATTERN =
        Pattern.compile("(?is)\\b(JSON_EXTRACT|JSON_VALUE|GET_JSON_OBJECT|JSON_QUERY)\\s*\\(|->");
    protected static final Pattern UNNEST_LATERAL_PATTERN = Pattern.compile("(?is)\\b(UNNEST|LATERAL)\\b");
    protected static final Pattern NULL_SAFE_PATTERN =
        Pattern.compile("(?is)\\b(COALESCE|NVL)\\s*\\(|\\bIS\\s+NOT\\s+DISTINCT\\s+FROM\\b");
    protected static final Pattern CASE_AGGREGATION_PATTERN =
        Pattern.compile("(?is)\\b(SUM|COUNT|MAX|MIN)\\s*\\(\\s*CASE\\s+WHEN\\b");
    protected static final Pattern DATE_TRUNC_PATTERN =
        Pattern.compile("(?is)\\b(DATE_TRUNC|TRUNC|TO_DATE)\\s*\\(");
    protected static final Pattern OFFSET_PATTERN = Pattern.compile("(?is)\\bOFFSET\\s+\\d+\\b");
    protected static final Pattern SEMI_STRUCTURED_FLATTEN_PATTERN =
        Pattern.compile("(?is)\\b(FLATTEN|EXPLODE)\\s*\\(");
    protected static final Pattern WITH_CLAUSE_PATTERN = Pattern.compile("(?is)^\\s*WITH\\b");
    protected static final Pattern ORDER_BY_FUNCTION_PATTERN =
        Pattern.compile("(?is)\\bORDER\\s+BY\\b[^;]*(LOWER|UPPER|DATE_TRUNC|TRUNC|CAST|COALESCE|NVL)\\s*\\(");
    protected static final Pattern NOT_EQUAL_PATTERN = Pattern.compile("(?is)(<>|!=|\\bNOT\\s+LIKE\\b)");
    protected static final Pattern IS_NULL_PATTERN = Pattern.compile("(?is)\\bIS\\s+(?:NOT\\s+)?NULL\\b");
    protected static final Pattern ARRAY_FUNCTION_PATTERN =
        Pattern.compile("(?is)\\b(ARRAY_CONTAINS|CONTAINS|ANY_MATCH|CARDINALITY|JSON_ARRAY_LENGTH)\\s*\\(");
    protected static final Pattern RANGE_JOIN_PATTERN =
        Pattern.compile("(?is)\\bJOIN\\b.+\\bON\\b.+\\bBETWEEN\\b.+\\bAND\\b");
    protected static final Pattern CASE_EXPRESSION_PATTERN = Pattern.compile("(?is)\\bCASE\\s+WHEN\\b");
    protected static final Pattern COUNT_DISTINCT_PATTERN = Pattern.compile("(?is)\\bCOUNT\\s*\\(\\s*DISTINCT\\b");
    protected static final Pattern APPROX_DISTINCT_PATTERN =
        Pattern.compile("(?is)\\b(APPROX_DISTINCT|HLL|HLL_UNION|HLL_CARDINALITY)\\s*\\(");
    protected static final Pattern PERCENTILE_PATTERN =
        Pattern.compile("(?is)\\b(APPROX_PERCENTILE|PERCENTILE_CONT|PERCENTILE_DISC|QUANTILE)\\s*\\(");
    protected static final Pattern SQL_LEVEL_FUNCTION_PATTERN =
        Pattern.compile(
            "(?is)\\b(CURRENT_DATE|CURRENT_TIME|CURRENT_TIMESTAMP|LOCALTIME|LOCALTIMESTAMP|NOW|RAND|RANDOM|UUID"
                + "|CURRENT_USER|SESSION_USER)\\b\\s*(?:\\(|\\b)"
        );
    protected static final Pattern UNNEST_FUNCTION_PATTERN = Pattern.compile("(?i)\\bUNNEST\\s*\\(");
    protected static final Pattern SIMPLE_BACKTICK_IDENTIFIER_PATTERN =
        Pattern.compile("[`\"]([A-Za-z_][A-Za-z0-9_]*)[`\"]");

    protected boolean isCalciteSelectLike(SqlNode node) {
        if (node instanceof SqlSelect || node instanceof SqlWith || node instanceof SqlOrderBy) {
            return true;
        }
        if (node instanceof SqlCall) {
            SqlKind kind = node.getKind();
            return kind == SqlKind.UNION || kind == SqlKind.INTERSECT || kind == SqlKind.EXCEPT;
        }
        return false;
    }

    protected static String removeSimpleIdentifierQuotes(String value) {
        if (value == null) {
            return "";
        }
        return SIMPLE_BACKTICK_IDENTIFIER_PATTERN.matcher(value).replaceAll("$1");
    }

    protected static boolean isStringLikeProjectionKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.replace("\"", "").replace("`", "").trim();
        return STRING_LIKE_PROJECTION_PATTERN.matcher(normalized).find();
    }

    protected static final class AdvancedStructureProfile {

        protected String profileStatus = "PARTIAL";
        protected final Set<String> cteNames = new LinkedHashSet<String>();
        protected final List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> projections = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> predicates = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> joinGraph = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> aggregations = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> groupBy = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> orderBy = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> ctes = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> subqueries = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> timeFunctions = new ArrayList<Map<String, Object>>();
        protected final List<Map<String, Object>> nonDeterministicFunctions = new ArrayList<Map<String, Object>>();
        protected Map<String, Object> limit = Collections.emptyMap();

        protected void markAvailable() {
            profileStatus = "AVAILABLE";
        }

        protected void markPartial() {
            profileStatus = "PARTIAL";
        }

        protected void recordCte(SqlWithItem withItem) {
            if (withItem == null || withItem.name == null) {
                return;
            }
            String name = cleanIdentifier(withItem.name.toString());
            cteNames.add(name.toUpperCase(Locale.ROOT));
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", name);
            item.put("recursive", Boolean.valueOf(withItem.recursive != null && Boolean.TRUE.equals(withItem.recursive.getValue())));
            item.put("query", normalizeText(withItem.query));
            item.put("columns", nodeListTexts(withItem.columnList));
            addUnique(ctes, item);
        }

        protected void recordTable(String tableName, String alias, String sourceType) {
            if (!StringUtils.hasText(tableName)) {
                return;
            }
            String cleanTableName = cleanIdentifier(tableName);
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("tableName", cleanTableName);
            item.put("schemaName", schemaName(cleanTableName));
            item.put("alias", cleanIdentifier(alias));
            item.put("sourceType", StringUtils.hasText(sourceType) ? sourceType : (isCteName(cleanTableName) ? "CTE_REFERENCE" : "BASE_TABLE"));
            addUnique(tables, item);
        }

        protected void recordProjection(SqlNode selectItem, List<String> sourceColumns) {
            if (selectItem == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("expression", normalizeText(projectionExpression(selectItem)));
            item.put("alias", projectionAlias(selectItem));
            item.put("expressionType", projectionType(selectItem));
            item.put("sourceColumns", safeList(sourceColumns));
            addUnique(projections, item);
        }

        protected void recordPredicate(String clause,
                                     SqlNode predicate,
                                     List<String> sourceColumns,
                                     List<String> functionNames,
                                     String logicalContext,
                                     String groupId) {
            if (predicate == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("clause", clause);
            item.put("expression", normalizeText(predicate));
            item.put("predicateType", predicateType(predicate));
            item.put("sourceColumns", safeList(sourceColumns));
            item.put("functionNames", safeList(functionNames));
            item.put("logicalContext", StringUtils.hasText(logicalContext) ? logicalContext : "AND");
            item.put("groupId", StringUtils.hasText(groupId) ? groupId : "");
            addUnique(predicates, item);
        }

        protected void recordJoin(SqlNode leftItem, SqlJoin join) {
            if (join == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("joinType", join.getJoinType() == null ? "JOIN" : join.getJoinType().name());
            item.put("left", relationName(leftItem));
            item.put("right", relationName(join.getRight()));
            item.put("rightAlias", relationAlias(join.getRight()));
            item.put("condition", normalizeText(join.getCondition()));
            item.put("usingColumns", Collections.emptyList());
            addUnique(joinGraph, item);
        }

        protected void recordGroupBy(SqlNode expression, List<String> sourceColumns) {
            if (expression == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("expression", normalizeText(expression));
            item.put("sourceColumns", safeList(sourceColumns));
            addUnique(groupBy, item);
        }

        protected void recordOrderBy(SqlNode orderByElement, List<String> sourceColumns) {
            if (orderByElement == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("expression", normalizeText(orderByElement));
            item.put("direction", orderByDirection(orderByElement));
            item.put("directionExplicit", Boolean.valueOf(orderByElement.getKind() == SqlKind.DESCENDING));
            item.put("nullOrdering", orderByElement.toString().toUpperCase(Locale.ROOT).contains("NULLS LAST") ? "LAST" : "");
            item.put("sourceColumns", safeList(sourceColumns));
            addUnique(orderBy, item);
        }

        protected void recordLimit(SqlNode fetch, SqlNode offset) {
            if (fetch == null && offset == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("present", Boolean.TRUE);
            item.put("rowCount", fetch == null ? "" : normalizeText(fetch));
            item.put("offset", offset == null ? "" : normalizeText(offset));
            item.put("limitAll", Boolean.FALSE);
            this.limit = item;
        }

        protected void recordSubquery(SqlNode subSelect,
                                    String location,
                                    String alias,
                                    int nestedLevel,
                                    boolean correlated) {
            if (subSelect == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("subqueryId", "SQ" + (subqueries.size() + 1));
            item.put("location", location);
            item.put("alias", cleanIdentifier(alias));
            item.put("nestedLevel", Integer.valueOf(nestedLevel));
            item.put("correlated", Boolean.valueOf(correlated));
            item.put("query", normalizeText(subSelect));
            addUnique(subqueries, item);
        }

        protected void recordFunctionSignal(SqlBasicCall function, List<String> sourceColumns) {
            if (function == null || function.getOperator() == null || !StringUtils.hasText(function.getOperator().getName())) {
                return;
            }
            String functionName = function.getOperator().getName().toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(functionName)) {
                LinkedHashMap<String, Object> aggregation = functionEntry(functionName, function, sourceColumns);
                aggregation.put("distinct", Boolean.valueOf(function.getFunctionQuantifier() != null));
                addUnique(aggregations, aggregation);
            }
            if (TIME_FUNCTIONS.contains(functionName)) {
                addUnique(timeFunctions, functionEntry(functionName, function, sourceColumns));
            }
            if (NON_DETERMINISTIC_FUNCTIONS.contains(functionName)) {
                addUnique(nonDeterministicFunctions, functionEntry(functionName, function, sourceColumns));
            }
        }

        protected void recordSqlLevelFunctions(String sql) {
            if (!StringUtils.hasText(sql)) {
                return;
            }
            Matcher matcher = SQL_LEVEL_FUNCTION_PATTERN.matcher(sql);
            while (matcher.find()) {
                String functionName = matcher.group(1).toUpperCase(Locale.ROOT);
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("functionName", functionName);
                item.put("expression", functionName);
                item.put("sourceColumns", Collections.emptyList());
                if (TIME_FUNCTIONS.contains(functionName)) {
                    addUnique(timeFunctions, item);
                }
                if (NON_DETERMINISTIC_FUNCTIONS.contains(functionName)) {
                    addUnique(nonDeterministicFunctions, item);
                }
            }
        }

        protected Map<String, Object> toMap(String parserEngine) {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("profileStatus", profileStatus);
            payload.put("parserEngine", parserEngine);
            payload.put("tables", immutableList(tables));
            payload.put("projections", immutableList(projections));
            payload.put("predicates", immutableList(predicates));
            payload.put("joinGraph", immutableList(joinGraph));
            payload.put("aggregations", immutableList(aggregations));
            payload.put("groupBy", immutableList(groupBy));
            payload.put("orderBy", immutableList(orderBy));
            payload.put("limit", limit.isEmpty() ? Collections.emptyMap() : new LinkedHashMap<String, Object>(limit));
            payload.put("ctes", immutableList(ctes));
            payload.put("subqueries", immutableList(subqueries));
            payload.put("timeFunctions", immutableList(timeFunctions));
            payload.put("nonDeterministicFunctions", immutableList(nonDeterministicFunctions));
            return payload;
        }

        protected boolean isCteName(String tableName) {
            return StringUtils.hasText(tableName) && cteNames.contains(tableName.toUpperCase(Locale.ROOT));
        }

        protected LinkedHashMap<String, Object> functionEntry(String functionName,
                                                            Object expression,
                                                            List<String> sourceColumns) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("functionName", functionName);
            item.put("expression", normalizeText(expression));
            item.put("sourceColumns", safeList(sourceColumns));
            return item;
        }

        protected String projectionAlias(SqlNode selectItem) {
            if (selectItem instanceof SqlBasicCall && selectItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) selectItem).getOperandList();
                if (operands.size() >= 2) {
                    return cleanIdentifier(operands.get(1).toString());
                }
            }
            return "";
        }

        protected SqlNode projectionExpression(SqlNode selectItem) {
            if (selectItem instanceof SqlBasicCall && selectItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) selectItem).getOperandList();
                if (!operands.isEmpty()) {
                    return operands.get(0);
                }
            }
            return selectItem;
        }

        protected String projectionType(SqlNode selectItem) {
            if (selectItem == null) {
                return "EXPRESSION";
            }
            if (selectItem instanceof SqlIdentifier && ((SqlIdentifier) selectItem).isStar()) {
                return "STAR";
            }
            SqlNode expression = selectItem;
            if (selectItem instanceof SqlBasicCall && selectItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) selectItem).getOperandList();
                if (!operands.isEmpty()) {
                    expression = operands.get(0);
                }
            }
            if (expression instanceof SqlSelect || expression instanceof SqlWith || expression instanceof SqlOrderBy) {
                return "SCALAR_SUBQUERY";
            }
            if (expression instanceof SqlBasicCall) {
                SqlBasicCall call = (SqlBasicCall) expression;
                String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
                return AGGREGATE_FUNCTIONS.contains(functionName.toUpperCase(Locale.ROOT)) ? "AGGREGATION" : "FUNCTION";
            }
            if (expression instanceof SqlIdentifier) {
                return "COLUMN";
            }
            return "EXPRESSION";
        }

        protected String predicateType(SqlNode predicate) {
            if (predicate instanceof SqlBasicCall) {
                SqlKind kind = predicate.getKind();
                if (kind == SqlKind.EQUALS
                    || kind == SqlKind.NOT_EQUALS
                    || kind == SqlKind.LESS_THAN
                    || kind == SqlKind.LESS_THAN_OR_EQUAL
                    || kind == SqlKind.GREATER_THAN
                    || kind == SqlKind.GREATER_THAN_OR_EQUAL) {
                    return "COMPARISON";
                }
                if (kind == SqlKind.IN) {
                    return "IN";
                }
                if (kind == SqlKind.EXISTS) {
                    return "EXISTS";
                }
                if (kind == SqlKind.LIKE) {
                    return "LIKE";
                }
                if (((SqlBasicCall) predicate).getOperator() != null) {
                    return "FUNCTION";
                }
            }
            return "EXPRESSION";
        }

        protected String relationName(SqlNode fromItem) {
            if (fromItem instanceof SqlIdentifier) {
                return cleanIdentifier(fromItem.toString());
            }
            if (fromItem instanceof SqlBasicCall && fromItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) fromItem).getOperandList();
                if (operands.size() >= 2) {
                    SqlNode relation = operands.get(0);
                    if (relation instanceof SqlIdentifier) {
                        return cleanIdentifier(relation.toString());
                    }
                    return cleanIdentifier(operands.get(1).toString());
                }
            }
            if (fromItem instanceof SqlSelect || fromItem instanceof SqlWith || fromItem instanceof SqlOrderBy) {
                return "SUBQUERY";
            }
            return fromItem == null ? "UNKNOWN" : normalizeText(fromItem);
        }

        protected String relationAlias(SqlNode fromItem) {
            if (fromItem instanceof SqlBasicCall && fromItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) fromItem).getOperandList();
                if (operands.size() >= 2) {
                    return cleanIdentifier(operands.get(1).toString());
                }
            }
            return "";
        }

        protected String orderByDirection(SqlNode orderByElement) {
            if (orderByElement == null) {
                return "ASC";
            }
            String text = orderByElement.toString().toUpperCase(Locale.ROOT);
            return text.endsWith(" DESC") || orderByElement.getKind() == SqlKind.DESCENDING ? "DESC" : "ASC";
        }

        protected String schemaName(String tableName) {
            if (!StringUtils.hasText(tableName) || !tableName.contains(".")) {
                return "";
            }
            return tableName.substring(0, tableName.lastIndexOf('.'));
        }

        protected List<String> nodeListTexts(SqlNodeList nodeList) {
            if (nodeList == null || nodeList.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<String>();
            for (SqlNode node : nodeList.getList()) {
                result.add(normalizeText(node));
            }
            return result;
        }

        protected List<String> safeList(List<String> input) {
            if (input == null || input.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<String>(input);
        }

        protected String cleanIdentifier(String value) {
            if (!StringUtils.hasText(value)) {
                return "";
            }
            return value.replace("\"", "").replace("`", "").trim();
        }

        protected String normalizeText(Object value) {
            if (value == null) {
                return "";
            }
            return removeSimpleIdentifierQuotes(value.toString())
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
                .replaceAll("(?i)\\bBETWEEN\\s+ASYMMETRIC\\b", "BETWEEN")
                .replaceAll("\\s+", " ");
        }

        protected void addUnique(List<Map<String, Object>> target, Map<String, Object> item) {
            if (target != null && item != null && !target.contains(item)) {
                target.add(item);
            }
        }

        protected List<Map<String, Object>> immutableList(List<Map<String, Object>> source) {
            if (source == null || source.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(source.size());
            for (Map<String, Object> item : source) {
                result.add(new LinkedHashMap<String, Object>(item));
            }
            return result;
        }
    }

    protected static final class RewriteOutcome {

        protected final String rewrittenSql;
        protected final List<String> appliedRules;

        protected RewriteOutcome(String rewrittenSql, List<String> appliedRules) {
            this.rewrittenSql = rewrittenSql;
            this.appliedRules = appliedRules;
        }

        protected static RewriteOutcome empty() {
            return new RewriteOutcome("", Collections.<String>emptyList());
        }
    }

    public static final class RecommendationRuleOutputModel {

        protected final List<Map<String, Object>> ruleChain;
        protected final List<Map<String, Object>> unappliedRules;
        protected final List<Map<String, Object>> preconditions;
        protected final List<Map<String, Object>> semanticRisks;
        protected final Map<String, Object> expectedBenefit;
        protected final Map<String, Object> estimatedCost;
        protected final Integer confidence;
        protected final String validationMethod;
        protected final boolean autoApplyAllowed;
        protected final boolean manualReviewRequired;

        protected RecommendationRuleOutputModel(List<Map<String, Object>> ruleChain,
                                              List<Map<String, Object>> unappliedRules,
                                              List<Map<String, Object>> preconditions,
                                              List<Map<String, Object>> semanticRisks,
                                              Map<String, Object> expectedBenefit,
                                              Map<String, Object> estimatedCost,
                                              Integer confidence,
                                              String validationMethod,
                                              boolean autoApplyAllowed,
                                              boolean manualReviewRequired) {
            this.ruleChain = immutableListCopy(ruleChain);
            this.unappliedRules = immutableListCopy(unappliedRules);
            this.preconditions = immutableListCopy(preconditions);
            this.semanticRisks = immutableListCopy(semanticRisks);
            this.expectedBenefit = immutableMapCopy(expectedBenefit);
            this.estimatedCost = immutableMapCopy(estimatedCost);
            this.confidence = confidence;
            this.validationMethod = validationMethod;
            this.autoApplyAllowed = autoApplyAllowed;
            this.manualReviewRequired = manualReviewRequired;
        }

        public static RecommendationRuleOutputModel empty() {
            return new RecommendationRuleOutputModel(
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<Map<String, Object>>emptyList(),
                Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap(),
                Integer.valueOf(0),
                "RESULT_DIFF_REQUIRED",
                false,
                true
            );
        }

        protected static Map<String, Object> immutableMapCopy(Map<String, Object> value) {
            if (value == null || value.isEmpty()) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
        }

        protected static List<Map<String, Object>> immutableListCopy(List<Map<String, Object>> value) {
            if (value == null || value.isEmpty()) {
                return Collections.emptyList();
            }
            List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(value.size());
            for (Map<String, Object> item : value) {
                result.add(immutableMapCopy(item));
            }
            return Collections.unmodifiableList(result);
        }

        public List<Map<String, Object>> getRuleChain() { return ruleChain; }
        public List<Map<String, Object>> getUnappliedRules() { return unappliedRules; }
        public List<Map<String, Object>> getPreconditions() { return preconditions; }
        public List<Map<String, Object>> getSemanticRisks() { return semanticRisks; }
        public Map<String, Object> getExpectedBenefit() { return expectedBenefit; }
        public Map<String, Object> getEstimatedCost() { return estimatedCost; }
        public Integer getConfidence() { return confidence; }
        public String getValidationMethod() { return validationMethod; }
        public boolean isAutoApplyAllowed() { return autoApplyAllowed; }
        public boolean isManualReviewRequired() { return manualReviewRequired; }
    }

    public static final class SqlFailurePosition {

        protected final Integer line;
        protected final Integer column;
        protected final Integer offset;
        protected final String token;
        protected final String snippet;

        protected SqlFailurePosition(Integer line, Integer column, Integer offset, String token, String snippet) {
            this.line = line;
            this.column = column;
            this.offset = offset;
            this.token = token;
            this.snippet = snippet;
        }

        public Integer getLine() {
            return line;
        }

        public Integer getColumn() {
            return column;
        }

        public Integer getOffset() {
            return offset;
        }

        public String getToken() {
            return token;
        }

        public String getSnippet() {
            return snippet;
        }
    }

    public static final class SqlOptimizationExecutionException extends RuntimeException {

        protected final int code;
        protected final String suggestedAction;
        protected final boolean retryable;
        protected final OptimizationTaskPhase failedPhase;
        protected final List<OptimizationTaskRisk> risks;
        protected final SqlFailurePosition failurePosition;

        protected SqlOptimizationExecutionException(int code,
                                                  String message,
                                                  String suggestedAction,
                                                  boolean retryable,
                                                  OptimizationTaskPhase failedPhase,
                                                  List<OptimizationTaskRisk> risks,
                                                  Throwable cause,
                                                  SqlFailurePosition failurePosition) {
            super(message, cause);
            this.code = code;
            this.suggestedAction = suggestedAction;
            this.retryable = retryable;
            this.failedPhase = failedPhase;
            this.risks = risks == null ? Collections.<OptimizationTaskRisk>emptyList() : risks;
            this.failurePosition = failurePosition;
        }

        public int getCode() {
            return code;
        }

        public String getSuggestedAction() {
            return suggestedAction;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public OptimizationTaskPhase getFailedPhase() {
            return failedPhase;
        }

        public List<OptimizationTaskRisk> getRisks() {
            return risks;
        }

        public SqlFailurePosition getFailurePosition() {
            return failurePosition;
        }
    }
}
