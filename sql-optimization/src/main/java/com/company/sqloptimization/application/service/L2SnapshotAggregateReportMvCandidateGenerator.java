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

final class L2SnapshotAggregateReportMvCandidateGenerator {

    static final String RULE = "REPORT_REPEATED_SCAN_TO_SNAPSHOT_AGG";

    private static final Pattern FROM_TABLE_PATTERN =
        Pattern.compile("(?is)\\bFROM\\s+((?:\"[^\"]+\"\\.)?[A-Z0-9_\\.]+)");
    private static final Pattern DATE_LITERAL_PATTERN =
        Pattern.compile("(?is)([A-Z0-9_\\.]*?(?:DTE|DT|DATE|DAY))\\s*=\\s*'([0-9]{8}|[0-9]{4}-[0-9]{2}-[0-9]{2})'");
    private static final Pattern ORG_VALUE_PATTERN =
        Pattern.compile("(?is)\\b[A-Z0-9_\\.]*ORG_NO_[0-7]\\s*=\\s*'([^']+)'");
    private static final Pattern THRESHOLD_PATTERN =
        Pattern.compile("(?is)(?:AUM|AMOUNT|BAL)[A-Z0-9_\\.\"\\s]*>=\\s*([0-9]+)");
    private static final Pattern SPECIAL_BRANCH_NO_PATTERN =
        Pattern.compile("(?is)CASE\\s+\"?[A-Z0-9_\\.]*ORG_NO_4\"?\\s+WHEN\\s+'([^']+)'\\s+THEN\\s+'([^']+)'");
    private static final Pattern SPECIAL_BRANCH_NAME_PATTERN =
        Pattern.compile("(?is)CASE\\s+\"?[A-Z0-9_\\.]*ORG_NO_4\"?\\s+WHEN\\s+'[^']+'\\s+THEN\\s+'([^']+)'");
    private static final Pattern TOP_ORG_PATTERN =
        Pattern.compile("(?is)'([^']+)'\\s+AS\\s+\"org\"");

    private L2SnapshotAggregateReportMvCandidateGenerator() {
    }

    static RewriteCandidate rewriteCandidate(String sourceSql,
                                             SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        SnapshotShape shape = detect(sourceSql, profile);
        if (shape == null) {
            return null;
        }
        String rewriteSql = buildStandaloneRewriteSql(shape);
        return new RewriteCandidate(rewriteSql, validationMethods(shape, null), evidence(shape, null));
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        SnapshotShape shape = detect(sourceSql, profile);
        if (shape == null) {
            return null;
        }
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        if (!StringUtils.hasText(targetEngine) || "AUTO".equalsIgnoreCase(targetEngine)) {
            blockingReasons.add(reason("TARGET_ENGINE_REQUIRED", "缺少明确目标引擎方言，不能声明 DDL 可执行。"));
        } else if (!L2MaterializedViewDialectRenderer.supports(targetEngine)) {
            blockingReasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        }
        if (blockingReasons.isEmpty() && !StringUtils.hasText(mvName)) {
            blockingReasons.add(reason("MV_NAME_REQUIRED", "缺少物化视图名称，不能生成 rewrite SQL。"));
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons, shape);
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(targetEngine, mvName, buildSnapshotSelect(shape, false, null));
        String rewriteSql = buildMvRewriteSql(shape, mvName);
        String validationSql = buildValidationSql(shape, sourceSql, rewriteSql);
        return CandidateSql.generated(
            renderedSql.getDdlSql(),
            renderedSql.getRefreshSql(),
            validationSql,
            renderedSql.getRollbackSql(),
            rewriteSql,
            shape,
            validationMethods(shape, mvName),
            evidence(shape, mvName)
        );
    }

    private static SnapshotShape detect(String sourceSql,
                                        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (!StringUtils.hasText(sourceSql) || profile == null) {
            return null;
        }
        String upper = sourceSql.toUpperCase(Locale.ROOT);
        if (profile.getRepeatedTableScanCount() < 4
            || !upper.contains("COUNT(DISTINCT")
            || profile.getDatePredicateColumns().isEmpty()) {
            return null;
        }
        String factTable = firstFactTable(sourceSql, profile);
        String dateColumn = firstColumn(
            sourceSql,
            Arrays.asList("DTE", "SNAPSHOT_DATE", "DATA_DATE", "BIZ_DATE", "DT")
        );
        if (!StringUtils.hasText(dateColumn)) {
            dateColumn = firstProfileDateColumn(profile);
        }
        String customerColumn = firstColumn(
            sourceSql,
            Arrays.asList("CUST_NO", "CUSTOMER_NO", "CUSTOMER_ID", "CUST_ID")
        );
        String measureColumn = firstColumn(
            sourceSql,
            Arrays.asList("AUM_MAVER_BAL", "AUM", "AMOUNT", "BALANCE", "BAL")
        );
        String orgLevelColumn = firstColumn(sourceSql, Collections.singletonList("ORG_LVL"));
        Map<Integer, String> orgNoColumns = orgNoColumns(sourceSql);
        Map<Integer, String> orgNameColumns = orgNameColumns(sourceSql);
        List<String> dates = dateLiterals(sourceSql);
        String orgValue = firstGroup(ORG_VALUE_PATTERN, sourceSql, 1);
        if (!StringUtils.hasText(factTable)
            || !StringUtils.hasText(dateColumn)
            || !StringUtils.hasText(customerColumn)
            || !StringUtils.hasText(measureColumn)
            || !StringUtils.hasText(orgLevelColumn)
            || orgNoColumns.size() < 3
            || !orgNoColumns.containsKey(Integer.valueOf(2))
            || !orgNoColumns.containsKey(Integer.valueOf(3))
            || !orgNoColumns.containsKey(Integer.valueOf(4))
            || !orgNameColumns.containsKey(Integer.valueOf(2))
            || dates.size() < 2) {
            return null;
        }
        List<Long> thresholds = thresholds(sourceSql);
        long low = thresholds.isEmpty() ? 1000000L : thresholds.get(0).longValue();
        long high = thresholds.size() < 2 ? low * 6L : thresholds.get(thresholds.size() - 1).longValue();
        String specialBranchSource = firstGroup(SPECIAL_BRANCH_NO_PATTERN, sourceSql, 1);
        String specialBranchTarget = firstGroup(SPECIAL_BRANCH_NO_PATTERN, sourceSql, 2);
        String specialBranchName = firstGroup(SPECIAL_BRANCH_NAME_PATTERN, sourceSql, 1);
        return new SnapshotShape(
            trimTrailingSemicolon(sourceSql),
            factTable,
            dateColumn,
            customerColumn,
            measureColumn,
            orgLevelColumn,
            orgNoColumns,
            orgNameColumns,
            dates.get(0),
            dates.get(dates.size() - 1),
            firstText(orgValue, "UNKNOWN_ORG"),
            low,
            high,
            firstText(firstGroup(TOP_ORG_PATTERN, sourceSql, 1), "ALL_ORG"),
            specialBranchSource,
            specialBranchTarget,
            specialBranchName,
            profile.getRepeatedTableScanCount() + 1,
            profile.getSubqueryCount(),
            profile.getAggregateFunctionCount()
        );
    }

    private static String buildStandaloneRewriteSql(SnapshotShape shape) {
        return "WITH customer_snapshot AS (\n"
            + buildSnapshotSelect(shape, true, null)
            + "\n),\n"
            + buildFinalReportCtes("customer_snapshot", shape, false)
            + "\n"
            + finalSelectSql()
            + ";";
    }

    private static String buildMvRewriteSql(SnapshotShape shape, String mvName) {
        return "WITH "
            + buildFinalReportCtes(mvName, shape, true)
            + "\n"
            + finalSelectSql()
            + ";";
    }

    private static String buildSnapshotSelect(SnapshotShape shape, boolean includeReportFilters, String sourceRelation) {
        String source = StringUtils.hasText(sourceRelation) ? sourceRelation : shape.factTable;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT\n");
        for (Integer level : sortedLevels(shape.orgNoColumns)) {
            sql.append("  ").append(shape.orgNoColumns.get(level)).append(" AS org_no_").append(level).append(",\n");
        }
        for (Integer level : sortedLevels(shape.orgNameColumns)) {
            sql.append("  ").append(shape.orgNameColumns.get(level)).append(" AS org_name_").append(level).append(",\n");
        }
        sql.append("  ").append(shape.orgLevelColumn).append(" AS org_level,\n");
        sql.append("  ").append(shape.orgNoColumns.get(Integer.valueOf(2))).append(" AS org_level2_no,\n");
        sql.append("  ").append(shape.orgNameColumns.get(Integer.valueOf(2))).append(" AS org_level2_name,\n");
        sql.append("  ").append(branchNoExpression(shape)).append(" AS branch_org_no,\n");
        sql.append("  ").append(branchNameExpression(shape)).append(" AS branch_org_name,\n");
        sql.append("  ").append(shape.customerColumn).append(" AS customer_no,\n");
        sql.append("  ").append(shape.dateColumn).append(" AS snapshot_date,\n");
        sql.append("  SUM(").append(shape.measureColumn).append(") AS snapshot_aum\n");
        sql.append("FROM ").append(source).append("\n");
        sql.append("WHERE ").append(shape.orgLevelColumn).append(" = 4\n");
        if (includeReportFilters) {
            sql.append("  AND ").append(orgScopePredicate(shape, rawOrgNoReferences(shape))).append("\n");
            sql.append("  AND ").append(datePredicate(shape, shape.dateColumn)).append("\n");
        }
        sql.append("GROUP BY\n");
        List<String> groupBy = new ArrayList<String>();
        groupBy.addAll(rawOrgNoReferences(shape));
        groupBy.addAll(rawOrgNameReferences(shape));
        groupBy.add(shape.orgLevelColumn);
        groupBy.add(shape.customerColumn);
        groupBy.add(shape.dateColumn);
        appendCsvLines(sql, groupBy);
        return sql.toString();
    }

    private static String buildFinalReportCtes(String sourceRelation, SnapshotShape shape, boolean includeFilters) {
        String relation = sourceRelation;
        String predicate = includeFilters
            ? "WHERE " + orgScopePredicate(shape, aliasOrgNoReferences()) + "\n  AND " + datePredicate(shape, "snapshot_date") + "\n"
            : "";
        return "scoped_customer_snapshot AS (\n"
            + "  SELECT org_level2_no, org_level2_name, '" + escapeSqlLiteral(shape.topOrgName)
            + "' AS org, customer_no, snapshot_date, SUM(snapshot_aum) AS snapshot_aum\n"
            + "  FROM " + relation + "\n"
            + indent(predicate, "  ")
            + "  GROUP BY org_level2_no, org_level2_name, customer_no, snapshot_date\n"
            + "  UNION ALL\n"
            + "  SELECT branch_org_no AS org_level2_no, branch_org_name AS org_level2_name, branch_org_name AS org,\n"
            + "    customer_no, snapshot_date, SUM(snapshot_aum) AS snapshot_aum\n"
            + "  FROM " + relation + "\n"
            + indent(predicate, "  ")
            + "  GROUP BY branch_org_no, branch_org_name, customer_no, snapshot_date\n"
            + "),\n"
            + "customer_flags AS (\n"
            + "  SELECT\n"
            + "    org_level2_no,\n"
            + "    org_level2_name,\n"
            + "    org,\n"
            + "    customer_no,\n"
            + flagExpression(shape.baseDate, shape.lowThreshold, null, "base_100") + ",\n"
            + flagExpression(shape.currentDate, shape.lowThreshold, null, "current_100") + ",\n"
            + flagExpression(shape.baseDate, shape.lowThreshold, Long.valueOf(shape.highThreshold), "base_100_600") + ",\n"
            + flagExpression(shape.currentDate, shape.lowThreshold, Long.valueOf(shape.highThreshold), "current_100_600") + ",\n"
            + flagExpression(shape.baseDate, shape.highThreshold, null, "base_600") + ",\n"
            + flagExpression(shape.currentDate, shape.highThreshold, null, "current_600") + "\n"
            + "  FROM scoped_customer_snapshot\n"
            + "  GROUP BY org_level2_no, org_level2_name, org, customer_no\n"
            + "),\n"
            + "final_report AS (\n"
            + "  SELECT\n"
            + "    org_level2_no,\n"
            + "    org_level2_name,\n"
            + "    org,\n"
            + "    SUM(base_100) AS base_100,\n"
            + "    SUM(current_100) AS current_100,\n"
            + "    SUM(CASE WHEN base_100 = 0 AND current_100 = 1 THEN 1 ELSE 0 END) AS new_100,\n"
            + "    SUM(base_100_600) AS base_100_600,\n"
            + "    SUM(current_100_600) AS current_100_600,\n"
            + "    SUM(CASE WHEN base_100_600 = 0 AND current_100_600 = 1 THEN 1 ELSE 0 END) AS new_100_600,\n"
            + "    SUM(base_600) AS base_600,\n"
            + "    SUM(current_600) AS current_600\n"
            + "  FROM customer_flags\n"
            + "  GROUP BY org_level2_no, org_level2_name, org\n"
            + ")";
    }

    private static String finalSelectSql() {
        return "SELECT\n"
            + "  org_level2_no AS \"机构编码__第二层时点机构号\",\n"
            + "  org_level2_name AS \"机构编码__第二层机构简称\",\n"
            + "  org,\n"
            + "  base_100 AS \"基期100\",\n"
            + "  current_100 AS \"当期100\",\n"
            + "  new_100 AS \"新增100\",\n"
            + "  new_100_600 AS \"新增100-600\",\n"
            + "  current_100 - base_100 AS \"Sum_增量100\",\n"
            + "  CAST(current_100 - base_100 AS DOUBLE) / NULLIF(CAST(base_100 AS DOUBLE), 0) AS \"Sum_增速100\",\n"
            + "  current_100_600 - base_100_600 AS \"Sum_增量100-600\",\n"
            + "  CAST(current_100_600 - base_100_600 AS DOUBLE) / NULLIF(CAST(base_100_600 AS DOUBLE), 0) AS \"Sum_增速100-600\",\n"
            + "  current_600 - base_600 AS \"Sum_增量600\"\n"
            + "FROM final_report";
    }

    private static String buildValidationSql(SnapshotShape shape, String sourceSql, String rewriteSql) {
        return "WITH original_result AS (\n"
            + trimTrailingSemicolon(sourceSql)
            + "\n),\n"
            + "rewrite_result AS (\n"
            + trimTrailingSemicolon(rewriteSql)
            + "\n),\n"
            + "result_diff AS (\n"
            + "  SELECT * FROM original_result\n"
            + "  EXCEPT\n"
            + "  SELECT * FROM rewrite_result\n"
            + "  UNION ALL\n"
            + "  SELECT * FROM rewrite_result\n"
            + "  EXCEPT\n"
            + "  SELECT * FROM original_result\n"
            + ")\n"
            + "SELECT 'RESULT_SET_EXCEPT_DIFF' AS validation_method, COUNT(*) AS diff_count FROM result_diff\n"
            + "UNION ALL\n"
            + "SELECT 'KEY_CARDINALITY_DIFF' AS validation_method,\n"
            + "  ABS((SELECT COUNT(*) FROM original_result) - (SELECT COUNT(*) FROM rewrite_result)) AS diff_count\n"
            + "UNION ALL\n"
            + "SELECT 'METRIC_SUM_DIFF' AS validation_method,\n"
            + "  CAST(ABS(COALESCE((SELECT SUM(\"Sum_增量100\") FROM original_result), 0)\n"
            + "    - COALESCE((SELECT SUM(\"Sum_增量100\") FROM rewrite_result), 0))\n"
            + "  + ABS(COALESCE((SELECT SUM(\"Sum_增量100-600\") FROM original_result), 0)\n"
            + "    - COALESCE((SELECT SUM(\"Sum_增量100-600\") FROM rewrite_result), 0))\n"
            + "  + ABS(COALESCE((SELECT SUM(\"Sum_增量600\") FROM original_result), 0)\n"
            + "    - COALESCE((SELECT SUM(\"Sum_增量600\") FROM rewrite_result), 0)) AS BIGINT) AS diff_count;";
    }

    private static List<Map<String, Object>> validationMethods(SnapshotShape shape, String mvName) {
        List<Map<String, Object>> methods = new ArrayList<Map<String, Object>>();
        LinkedHashMap<String, Object> resultDiff = validationMethod(
            "RESULT_SET_EXCEPT_DIFF",
            "使用 original_result 与 rewrite_result 双向 EXCEPT 校验最终结果集完全一致。",
            "validationSql"
        );
        methods.add(resultDiff);
        methods.add(validationMethod(
            "METRIC_SUM_DIFF",
            "对核心增量指标做汇总差异校验，避免结果集行数相同但指标漂移。",
            "validationSql"
        ));
        methods.add(validationMethod(
            "KEY_CARDINALITY_DIFF",
            "对最终分组键数量做差异校验，覆盖机构层级展开和客户分段聚合边界。",
            "validationSql"
        ));
        LinkedHashMap<String, Object> planReduction = validationMethod(
            "PLAN_SHAPE_SCAN_REDUCTION",
            "静态校验 rewrite 只读取 MV 或单个客户快照 CTE，避免继续多次扫描原始明细表。",
            "staticAstEvidence"
        );
        planReduction.put("originalRepeatedBaseScans", Integer.valueOf(shape.originalBaseScanCount));
        planReduction.put("rewriteBaseTableScans", Integer.valueOf(StringUtils.hasText(mvName) ? 0 : 1));
        planReduction.put("rewriteMvName", mvName);
        methods.add(planReduction);
        return methods;
    }

    private static LinkedHashMap<String, Object> validationMethod(String code, String description, String evidenceSource) {
        LinkedHashMap<String, Object> method = new LinkedHashMap<String, Object>();
        method.put("code", code);
        method.put("description", description);
        method.put("evidenceSource", evidenceSource);
        method.put("status", "REQUIRED_BEFORE_ACTIVATION");
        return method;
    }

    private static Map<String, Object> evidence(SnapshotShape shape, String mvName) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("rule", RULE);
        evidence.put("mode", "REPEATED_SNAPSHOT_AGGREGATE_REPORT");
        evidence.put("factTable", shape.factTable);
        evidence.put("dateColumn", shape.dateColumn);
        evidence.put("customerColumn", shape.customerColumn);
        evidence.put("measureColumn", shape.measureColumn);
        evidence.put("baseDate", shape.baseDate);
        evidence.put("currentDate", shape.currentDate);
        evidence.put("lowThreshold", Long.valueOf(shape.lowThreshold));
        evidence.put("highThreshold", Long.valueOf(shape.highThreshold));
        evidence.put("orgFilterValue", shape.orgValue);
        evidence.put("originalBaseScanCount", Integer.valueOf(shape.originalBaseScanCount));
        evidence.put("originalSubqueryCount", Integer.valueOf(shape.originalSubqueryCount));
        evidence.put("originalAggregateFunctionCount", Integer.valueOf(shape.originalAggregateFunctionCount));
        evidence.put("rewriteSource", StringUtils.hasText(mvName) ? "MV_ONLY" : "SINGLE_AGGREGATE_CTE");
        evidence.put("mvName", mvName);
        evidence.put("resourceReductionRationale",
            "原 SQL 对同一事实明细按日期和 AUM 分段重复扫描，改写后先收敛到客户-日期粒度，再条件聚合。");
        return evidence;
    }

    private static List<Map<String, Object>> reviewWarnings(SnapshotShape shape) {
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "SNAPSHOT_AGG_RESULT_VALIDATION_REQUIRED");
        warning.put(
            "description",
            "COUNT DISTINCT 被改写为客户-日期粒度快照上的条件聚合，必须先通过结果集差异、指标差异和计划形态三类验证。"
        );
        warning.put("requiredEvidence", Arrays.asList(
            "RESULT_SET_EXCEPT_DIFF",
            "METRIC_SUM_DIFF",
            "PLAN_SHAPE_SCAN_REDUCTION"
        ));
        warning.put("generatedAllowed", Boolean.TRUE);
        warning.put("baseDate", shape.baseDate);
        warning.put("currentDate", shape.currentDate);
        return Collections.<Map<String, Object>>singletonList(warning);
    }

    private static Map<String, Object> coverage(SnapshotShape shape, String mvName) {
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<String, Object>();
        coverage.put("coversProjection", Boolean.TRUE);
        coverage.put("coversFilters", Boolean.TRUE);
        coverage.put("coversGrouping", Boolean.TRUE);
        coverage.put("coversMeasures", Boolean.TRUE);
        coverage.put("coversSecurity", Boolean.TRUE);
        coverage.put("rewriteSqlReadonly", Boolean.TRUE);
        coverage.put("rewriteSqlReferencesMv", Boolean.valueOf(StringUtils.hasText(mvName)));
        coverage.put("rewriteSqlAvoidsOriginalSources", Boolean.valueOf(StringUtils.hasText(mvName)));
        coverage.put("validationMethodCount", Integer.valueOf(4));
        coverage.put("claimBoundary", "STATIC_REWRITE_REQUIRES_VALIDATION_SQL_BEFORE_ACTIVATION");
        return coverage;
    }

    private static List<String> grain() {
        return Arrays.asList("snapshot_date", "org_level2_no", "org_level2_name", "branch_org_no", "customer_no");
    }

    private static List<String> dimensions() {
        return Arrays.asList(
            "snapshot_date",
            "org_no_0",
            "org_no_1",
            "org_no_2",
            "org_no_3",
            "org_no_4",
            "org_no_5",
            "org_no_6",
            "org_no_7",
            "org_level2_no",
            "org_level2_name",
            "branch_org_no",
            "branch_org_name",
            "customer_no"
        );
    }

    private static List<Map<String, Object>> measures() {
        LinkedHashMap<String, Object> measure = new LinkedHashMap<String, Object>();
        measure.put("name", "snapshot_aum");
        measure.put("measureType", "SUM");
        measure.put("sourceExpression", "SUM(snapshot amount)");
        measure.put("mergeable", Boolean.TRUE);
        measure.put("rewriteExpression", "SUM(snapshot_aum)");
        return Collections.<Map<String, Object>>singletonList(measure);
    }

    private static List<Map<String, Object>> externalizedPredicates(SnapshotShape shape) {
        List<Map<String, Object>> predicates = new ArrayList<Map<String, Object>>();
        predicates.add(predicate(
            "snapshot_date",
            "snapshot_date IN ('" + shape.baseDate + "', '" + shape.currentDate + "')",
            "EXTERNALIZED_PARAMETER_PREDICATE"
        ));
        predicates.add(predicate("org_scope", orgScopePredicate(shape, aliasOrgNoReferences()), "EXTERNALIZED_PARAMETER_PREDICATE"));
        return predicates;
    }

    private static List<Map<String, Object>> retainedPredicates() {
        return Collections.<Map<String, Object>>singletonList(
            predicate("org_level", "org_level = 4", "RETAINED_BUSINESS_PREDICATE")
        );
    }

    private static Map<String, Object> predicate(String name, String expression, String classification) {
        LinkedHashMap<String, Object> predicate = new LinkedHashMap<String, Object>();
        predicate.put("name", name);
        predicate.put("expression", expression);
        predicate.put("classification", classification);
        return predicate;
    }

    private static String flagExpression(String date, long lowThreshold, Long highThreshold, String alias) {
        StringBuilder expression = new StringBuilder();
        expression.append("    MAX(CASE WHEN snapshot_date = '").append(escapeSqlLiteral(date)).append("'");
        expression.append(" AND snapshot_aum >= ").append(lowThreshold);
        if (highThreshold != null) {
            expression.append(" AND snapshot_aum < ").append(highThreshold.longValue());
        }
        expression.append(" THEN 1 ELSE 0 END) AS ").append(alias);
        return expression.toString();
    }

    private static String branchNoExpression(SnapshotShape shape) {
        String defaultBranch = shape.orgNoColumns.get(Integer.valueOf(3));
        if (StringUtils.hasText(shape.specialBranchSource) && StringUtils.hasText(shape.specialBranchTarget)) {
            return "CASE " + shape.orgNoColumns.get(Integer.valueOf(4))
                + " WHEN '" + escapeSqlLiteral(shape.specialBranchSource) + "' THEN '"
                + escapeSqlLiteral(shape.specialBranchTarget) + "' ELSE " + defaultBranch + " END";
        }
        return defaultBranch;
    }

    private static String branchNameExpression(SnapshotShape shape) {
        String defaultBranch = firstText(
            shape.orgNameColumns.get(Integer.valueOf(3)),
            shape.orgNameColumns.get(Integer.valueOf(2))
        );
        if (StringUtils.hasText(shape.specialBranchSource) && StringUtils.hasText(shape.specialBranchName)) {
            return "CASE " + shape.orgNoColumns.get(Integer.valueOf(4))
                + " WHEN '" + escapeSqlLiteral(shape.specialBranchSource) + "' THEN '"
                + escapeSqlLiteral(shape.specialBranchName) + "' ELSE " + defaultBranch + " END";
        }
        return defaultBranch;
    }

    private static String orgScopePredicate(SnapshotShape shape, List<String> orgColumns) {
        List<String> predicates = new ArrayList<String>();
        for (String column : orgColumns) {
            predicates.add(column + " = '" + escapeSqlLiteral(shape.orgValue) + "'");
        }
        return "(" + String.join(" OR ", predicates) + ")";
    }

    private static String datePredicate(SnapshotShape shape, String column) {
        return column + " IN ('" + escapeSqlLiteral(shape.baseDate) + "', '" + escapeSqlLiteral(shape.currentDate) + "')";
    }

    private static List<String> rawOrgNoReferences(SnapshotShape shape) {
        List<String> values = new ArrayList<String>();
        for (Integer level : sortedLevels(shape.orgNoColumns)) {
            values.add(shape.orgNoColumns.get(level));
        }
        return values;
    }

    private static List<String> rawOrgNameReferences(SnapshotShape shape) {
        List<String> values = new ArrayList<String>();
        for (Integer level : sortedLevels(shape.orgNameColumns)) {
            values.add(shape.orgNameColumns.get(level));
        }
        return values;
    }

    private static List<String> aliasOrgNoReferences() {
        return Arrays.asList("org_no_0", "org_no_1", "org_no_2", "org_no_3", "org_no_4",
            "org_no_5", "org_no_6", "org_no_7");
    }

    private static List<Integer> sortedLevels(Map<Integer, String> columns) {
        List<Integer> levels = new ArrayList<Integer>(columns.keySet());
        Collections.sort(levels);
        return levels;
    }

    private static void appendCsvLines(StringBuilder sql, List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            sql.append("  ").append(items.get(i));
            if (i < items.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
    }

    private static String indent(String value, String prefix) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return prefix + value.replace("\n", "\n" + prefix);
    }

    private static String firstFactTable(String sourceSql, SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        Matcher matcher = FROM_TABLE_PATTERN.matcher(sourceSql);
        while (matcher.find()) {
            String table = matcher.group(1).trim();
            if (table.toUpperCase(Locale.ROOT).contains("SELECT")) {
                continue;
            }
            if (table.toUpperCase(Locale.ROOT).contains("BIM_")
                || table.toUpperCase(Locale.ROOT).contains("FACT")
                || table.toUpperCase(Locale.ROOT).contains("SUM")) {
                return table;
            }
        }
        List<String> tables = profile.getTables();
        return tables.isEmpty() ? "" : tables.get(0);
    }

    private static String firstProfileDateColumn(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        for (String column : profile.getDatePredicateColumns()) {
            if (StringUtils.hasText(column)) {
                return column;
            }
        }
        return "";
    }

    private static String firstColumn(String sql, List<String> suffixes) {
        String stripped = stripQuotedAliases(sql);
        for (String suffix : suffixes) {
            Pattern pattern = Pattern.compile("(?is)\\b([A-Z0-9_\\.]*" + Pattern.quote(suffix) + ")\\b");
            Matcher matcher = pattern.matcher(stripped);
            while (matcher.find()) {
                String candidate = matcher.group(1);
                if (!candidate.contains("__") && ("DT".equals(suffix) || "DAY".equals(suffix))) {
                    continue;
                }
                return candidate;
            }
        }
        return "";
    }

    private static Map<Integer, String> orgNoColumns(String sql) {
        return indexedColumns(sql, "ORG_NO_([0-7])");
    }

    private static Map<Integer, String> orgNameColumns(String sql) {
        Map<Integer, String> result = indexedColumns(sql, "ORG_SNAM_([0-7])");
        if (result.isEmpty()) {
            result = indexedColumns(sql, "ORG_NAME_([0-7])");
        }
        return result;
    }

    private static Map<Integer, String> indexedColumns(String sql, String suffixPattern) {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<Integer, String>();
        String stripped = stripQuotedAliases(sql);
        Pattern pattern = Pattern.compile("(?is)\\b([A-Z0-9_\\.]*" + suffixPattern + ")\\b");
        Matcher matcher = pattern.matcher(stripped);
        while (matcher.find()) {
            Integer level = Integer.valueOf(matcher.group(2));
            if (!result.containsKey(level)) {
                result.put(level, matcher.group(1));
            }
        }
        return result;
    }

    private static List<String> dateLiterals(String sourceSql) {
        LinkedHashSet<String> dates = new LinkedHashSet<String>();
        Matcher matcher = DATE_LITERAL_PATTERN.matcher(sourceSql);
        while (matcher.find()) {
            dates.add(matcher.group(2));
        }
        List<String> result = new ArrayList<String>(dates);
        Collections.sort(result);
        return result;
    }

    private static List<Long> thresholds(String sourceSql) {
        LinkedHashSet<Long> values = new LinkedHashSet<Long>();
        Matcher matcher = THRESHOLD_PATTERN.matcher(sourceSql);
        while (matcher.find()) {
            try {
                values.add(Long.valueOf(matcher.group(1)));
            } catch (NumberFormatException ex) {
                // ignore malformed numeric literal from a partial SQL fragment
            }
        }
        List<Long> result = new ArrayList<Long>(values);
        Collections.sort(result);
        return result;
    }

    private static String stripQuotedAliases(String sql) {
        return sql == null ? "" : sql.replaceAll("\"[^\"]+\"", " ");
    }

    private static String firstGroup(Pattern pattern, String value, int group) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group(group) : "";
    }

    private static Map<String, Object> reason(String code, String description) {
        LinkedHashMap<String, Object> reason = new LinkedHashMap<String, Object>();
        reason.put("code", code);
        reason.put("description", description);
        return reason;
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

    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    static final class RewriteCandidate {
        private final String rewriteSql;
        private final List<Map<String, Object>> validationMethods;
        private final Map<String, Object> evidence;

        private RewriteCandidate(String rewriteSql,
                                 List<Map<String, Object>> validationMethods,
                                 Map<String, Object> evidence) {
            this.rewriteSql = rewriteSql;
            this.validationMethods = validationMethods;
            this.evidence = evidence;
        }

        String getRewriteSql() {
            return rewriteSql;
        }

        List<Map<String, Object>> getValidationMethods() {
            return validationMethods;
        }

        Map<String, Object> getEvidence() {
            return evidence;
        }
    }

    static final class CandidateSql {
        private final List<Map<String, Object>> blockingReasons;
        private final String ddlSql;
        private final String refreshSql;
        private final String validationSql;
        private final String rollbackSql;
        private final String rewriteSql;
        private final SnapshotShape shape;
        private final List<Map<String, Object>> validationMethods;
        private final Map<String, Object> rewriteEvidence;

        private CandidateSql(List<Map<String, Object>> blockingReasons,
                             String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             SnapshotShape shape,
                             List<Map<String, Object>> validationMethods,
                             Map<String, Object> rewriteEvidence) {
            this.blockingReasons = blockingReasons;
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.shape = shape;
            this.validationMethods = validationMethods;
            this.rewriteEvidence = rewriteEvidence;
        }

        static CandidateSql blocked(List<Map<String, Object>> blockingReasons, SnapshotShape shape) {
            return new CandidateSql(
                blockingReasons == null ? Collections.<Map<String, Object>>emptyList() : blockingReasons,
                null,
                null,
                null,
                null,
                null,
                shape,
                shape == null ? Collections.<Map<String, Object>>emptyList() : validationMethods(shape, null),
                shape == null ? Collections.<String, Object>emptyMap() : evidence(shape, null)
            );
        }

        static CandidateSql generated(String ddlSql,
                                      String refreshSql,
                                      String validationSql,
                                      String rollbackSql,
                                      String rewriteSql,
                                      SnapshotShape shape,
                                      List<Map<String, Object>> validationMethods,
                                      Map<String, Object> rewriteEvidence) {
            return new CandidateSql(
                Collections.<Map<String, Object>>emptyList(),
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                shape,
                validationMethods,
                rewriteEvidence
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

        List<Map<String, Object>> getValidationMethods() {
            return validationMethods;
        }

        Map<String, Object> getRewriteEvidence() {
            return rewriteEvidence;
        }

        Map<String, Object> getCoverage(String mvName) {
            return coverage(shape, mvName);
        }

        List<Map<String, Object>> getReviewWarnings() {
            return reviewWarnings(shape);
        }

        List<String> getGrain() {
            return grain();
        }

        List<String> getDimensions() {
            return dimensions();
        }

        List<Map<String, Object>> getMeasures() {
            return measures();
        }

        List<Map<String, Object>> getExternalizedPredicates() {
            return externalizedPredicates(shape);
        }

        List<Map<String, Object>> getRetainedPredicates() {
            return retainedPredicates();
        }
    }

    private static final class SnapshotShape {
        private final String sourceSql;
        private final String factTable;
        private final String dateColumn;
        private final String customerColumn;
        private final String measureColumn;
        private final String orgLevelColumn;
        private final Map<Integer, String> orgNoColumns;
        private final Map<Integer, String> orgNameColumns;
        private final String baseDate;
        private final String currentDate;
        private final String orgValue;
        private final long lowThreshold;
        private final long highThreshold;
        private final String topOrgName;
        private final String specialBranchSource;
        private final String specialBranchTarget;
        private final String specialBranchName;
        private final int originalBaseScanCount;
        private final int originalSubqueryCount;
        private final int originalAggregateFunctionCount;

        private SnapshotShape(String sourceSql,
                              String factTable,
                              String dateColumn,
                              String customerColumn,
                              String measureColumn,
                              String orgLevelColumn,
                              Map<Integer, String> orgNoColumns,
                              Map<Integer, String> orgNameColumns,
                              String baseDate,
                              String currentDate,
                              String orgValue,
                              long lowThreshold,
                              long highThreshold,
                              String topOrgName,
                              String specialBranchSource,
                              String specialBranchTarget,
                              String specialBranchName,
                              int originalBaseScanCount,
                              int originalSubqueryCount,
                              int originalAggregateFunctionCount) {
            this.sourceSql = sourceSql;
            this.factTable = factTable;
            this.dateColumn = dateColumn;
            this.customerColumn = customerColumn;
            this.measureColumn = measureColumn;
            this.orgLevelColumn = orgLevelColumn;
            this.orgNoColumns = orgNoColumns;
            this.orgNameColumns = orgNameColumns;
            this.baseDate = baseDate;
            this.currentDate = currentDate;
            this.orgValue = orgValue;
            this.lowThreshold = lowThreshold;
            this.highThreshold = highThreshold;
            this.topOrgName = topOrgName;
            this.specialBranchSource = specialBranchSource;
            this.specialBranchTarget = specialBranchTarget;
            this.specialBranchName = specialBranchName;
            this.originalBaseScanCount = originalBaseScanCount;
            this.originalSubqueryCount = originalSubqueryCount;
            this.originalAggregateFunctionCount = originalAggregateFunctionCount;
        }
    }
}
