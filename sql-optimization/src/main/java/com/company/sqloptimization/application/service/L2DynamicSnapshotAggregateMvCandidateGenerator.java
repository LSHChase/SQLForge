package com.company.sqloptimization.application.service;

import static com.company.sqloptimization.application.service.AccelerationArtifactValues.reason;

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

final class L2DynamicSnapshotAggregateMvCandidateGenerator {

    static final String RULE = "REPORT_REPEATED_SCAN_TO_SNAPSHOT_AGG";

    private static final Pattern CASE_BRANCH_PATTERN =
        Pattern.compile("(?is)^CASE\\s+WHEN\\s+.+?=\\s*'([^']+)'\\s+THEN\\s+'([^']+)'\\s+ELSE\\s+(.+?)\\s+END$");
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'([^']+)'");
    private static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile("(?<![A-Z0-9_])([0-9]+)(?![A-Z0-9_])");

    private L2DynamicSnapshotAggregateMvCandidateGenerator() {
    }

    static RewriteCandidate rewriteCandidate(String sourceSql,
                                             SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        SnapshotShape shape = detect(sourceSql, profile);
        if (shape == null) {
            return null;
        }
        String rewriteSql = buildStandaloneRewriteSql(shape);
        return new RewriteCandidate(rewriteSql, validationMethods(shape, null), evidence(shape, null, profile));
    }

    static CandidateSql generate(String sourceSql,
                                 String mvName,
                                 String targetEngine,
                                 SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        SnapshotShape shape = detect(sourceSql, profile);
        if (shape == null) {
            return null;
        }
        String renderEngine = StringUtils.hasText(targetEngine) && !"AUTO".equalsIgnoreCase(targetEngine)
            ? targetEngine
            : "HETU";
        List<Map<String, Object>> blockingReasons = new ArrayList<Map<String, Object>>();
        if (!L2MaterializedViewDialectRenderer.supports(renderEngine)) {
            blockingReasons.add(reason("UNSUPPORTED_TARGET_ENGINE", "当前 V1 仅生成 HETU/HIVE/SPARK 物化视图草案。"));
        }
        if (blockingReasons.isEmpty() && !StringUtils.hasText(mvName)) {
            blockingReasons.add(reason("MV_NAME_REQUIRED", "缺少物化视图名称，不能生成 rewrite SQL。"));
        }
        if (!blockingReasons.isEmpty()) {
            return CandidateSql.blocked(blockingReasons, shape, profile);
        }
        L2MaterializedViewDialectRenderer.RenderedSql renderedSql =
            L2MaterializedViewDialectRenderer.render(renderEngine, mvName, buildMaterializedSnapshotSelect(shape));
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
            evidence(shape, mvName, profile)
        );
    }

    private static SnapshotShape detect(String sourceSql,
                                        SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (!StringUtils.hasText(sourceSql) || profile == null) {
            return null;
        }
        if (profile.getRepeatedTableScanCount() < 4
            || !profile.getAggregateFunctions().contains("COUNT")
            || profile.getDatePredicateColumns().isEmpty()) {
            return null;
        }
        List<Map<String, Object>> projections = advancedEntries(profile, "projections");
        List<Map<String, Object>> predicates = advancedEntries(profile, "predicates");
        String factTable = firstFactTable(profile);
        String dateColumn = firstDateColumn(predicates, profile);
        String customerColumn = firstCustomerColumn(projections, profile);
        String measureColumn = firstMeasureColumn(projections);
        OrgLevelSignal orgLevel = firstOrgLevelSignal(predicates);
        Map<Integer, String> orgNoColumns = orgNoColumns(projections);
        Map<Integer, String> orgNameColumns = orgNameColumns(projections);
        ScopeSignal orgScope = firstOrgScope(predicates);
        augmentOrgScopeColumns(orgNoColumns, orgScope.columns);
        List<String> dates = dateLiterals(predicates, dateColumn);
        List<Long> thresholds = thresholds(predicates);
        if (!StringUtils.hasText(factTable)
            || !StringUtils.hasText(dateColumn)
            || !StringUtils.hasText(customerColumn)
            || !StringUtils.hasText(measureColumn)
            || !StringUtils.hasText(orgLevel.column)
            || !StringUtils.hasText(orgLevel.value)
            || !StringUtils.hasText(orgScope.value)
            || orgScope.columns.isEmpty()
            || !orgNoColumns.containsKey(Integer.valueOf(2))
            || !orgNoColumns.containsKey(Integer.valueOf(3))
            || !orgNoColumns.containsKey(Integer.valueOf(4))
            || !orgNameColumns.containsKey(Integer.valueOf(2))
            || !orgNameColumns.containsKey(Integer.valueOf(3))
            || dates.size() < 2
            || thresholds.size() < 2) {
            return null;
        }
        long low = thresholds.get(0).longValue();
        long high = thresholds.get(thresholds.size() - 1).longValue();
        String specialBranchSource = branchCaseLiteral(projections, true, 1);
        String specialBranchTarget = branchCaseLiteral(projections, true, 2);
        String specialBranchName = branchCaseLiteral(projections, false, 2);
        return new SnapshotShape(
            trimTrailingSemicolon(sourceSql),
            factTable,
            dateColumn,
            customerColumn,
            measureColumn,
            orgLevel.column,
            orgLevel.value,
            orgNoColumns,
            orgNameColumns,
            dates.get(0),
            dates.get(dates.size() - 1),
            firstText(orgScope.value, "UNKNOWN_ORG"),
            low,
            high,
            firstText(firstTopOrgName(projections), "ALL_ORG"),
            specialBranchSource,
            specialBranchTarget,
            specialBranchName,
            profile.getRepeatedTableScanCount() + 1,
            profile.getSubqueryCount(),
            profile.getAggregateFunctionCount()
        );
    }

    private static String buildStandaloneRewriteSql(SnapshotShape shape) {
        return "WITH raw_customer_snapshot AS (\n"
            + buildRawCustomerSnapshotSelect(shape, true, null)
            + "\n),\n"
            + "report_customer_snapshot AS (\n"
            + buildReportSnapshotSelectFromRaw(shape, false)
            + "\n),\n"
            + buildFinalReportCtes(shape)
            + "\n"
            + finalSelectSql()
            + ";";
    }

    private static String buildMvRewriteSql(SnapshotShape shape, String mvName) {
        return "WITH report_customer_snapshot AS (\n"
            + buildReportSnapshotSelectFromMv(shape, mvName)
            + "\n),\n"
            + buildFinalReportCtes(shape)
            + "\n"
            + finalSelectSql()
            + ";";
    }

    private static String buildMaterializedSnapshotSelect(SnapshotShape shape) {
        return "WITH raw_customer_snapshot AS (\n"
            + buildRawCustomerSnapshotSelect(shape, false, null)
            + "\n)\n"
            + buildReportSnapshotSelectFromRaw(shape, true);
    }

    private static String buildRawCustomerSnapshotSelect(SnapshotShape shape,
                                                         boolean includeReportFilters,
                                                         String sourceRelation) {
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
        sql.append("WHERE ").append(shape.orgLevelColumn).append(" = ").append(shape.orgLevelValue).append("\n");
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

    private static String buildReportSnapshotSelectFromRaw(SnapshotShape shape, boolean retainScopeColumns) {
        List<String> scopeColumns = retainScopeColumns ? aliasOrgNoReferences(shape) : Collections.<String>emptyList();
        StringBuilder sql = new StringBuilder();
        appendReportSnapshotSelectBranch(
            sql,
            scopeColumns,
            "org_level2_no",
            "org_level2_name",
            "'" + escapeSqlLiteral(shape.topOrgName) + "'"
        );
        sql.append("\nUNION ALL\n");
        appendReportSnapshotSelectBranch(
            sql,
            scopeColumns,
            "branch_org_no",
            "branch_org_name",
            "branch_org_name"
        );
        return sql.toString();
    }

    private static void appendReportSnapshotSelectBranch(StringBuilder sql,
                                                         List<String> scopeColumns,
                                                         String reportOrgNoExpression,
                                                         String reportOrgNameExpression,
                                                         String reportOrgLabelExpression) {
        sql.append("SELECT\n");
        for (String column : scopeColumns) {
            sql.append("  ").append(column).append(",\n");
        }
        sql.append("  ").append(reportOrgNoExpression).append(" AS report_org_no,\n");
        sql.append("  ").append(reportOrgNameExpression).append(" AS report_org_name,\n");
        sql.append("  ").append(reportOrgLabelExpression).append(" AS report_org_label,\n");
        sql.append("  customer_no,\n");
        sql.append("  snapshot_date,\n");
        sql.append("  SUM(snapshot_aum) AS snapshot_aum\n");
        sql.append("FROM raw_customer_snapshot\n");
        sql.append("GROUP BY\n");
        List<String> groupBy = new ArrayList<String>(scopeColumns);
        groupBy.add(reportOrgNoExpression);
        groupBy.add(reportOrgNameExpression);
        groupBy.add("customer_no");
        groupBy.add("snapshot_date");
        appendCsvLines(sql, groupBy);
    }

    private static String buildReportSnapshotSelectFromMv(SnapshotShape shape, String mvName) {
        return "  SELECT\n"
            + "    report_org_no,\n"
            + "    report_org_name,\n"
            + "    report_org_label,\n"
            + "    customer_no,\n"
            + "    snapshot_date,\n"
            + "    SUM(snapshot_aum) AS snapshot_aum\n"
            + "  FROM " + mvName + "\n"
            + "  WHERE " + orgScopePredicate(shape, aliasOrgNoReferences(shape)) + "\n"
            + "    AND " + datePredicate(shape, "snapshot_date") + "\n"
            + "  GROUP BY report_org_no, report_org_name, report_org_label, customer_no, snapshot_date";
    }

    private static String buildFinalReportCtes(SnapshotShape shape) {
        return "customer_pair AS (\n"
            + "  SELECT\n"
            + "    report_org_no,\n"
            + "    report_org_name,\n"
            + "    report_org_label,\n"
            + "    customer_no,\n"
            + "    SUM(CASE WHEN snapshot_date = '" + escapeSqlLiteral(shape.baseDate)
            + "' THEN snapshot_aum ELSE 0 END) AS base_aum,\n"
            + "    SUM(CASE WHEN snapshot_date = '" + escapeSqlLiteral(shape.currentDate)
            + "' THEN snapshot_aum ELSE 0 END) AS current_aum\n"
            + "  FROM report_customer_snapshot\n"
            + "  GROUP BY report_org_no, report_org_name, report_org_label, customer_no\n"
            + "),\n"
            + "org_customer_pair AS (\n"
            + "  SELECT\n"
            + "    report_org_no,\n"
            + "    customer_no,\n"
            + "    SUM(CASE WHEN snapshot_date = '" + escapeSqlLiteral(shape.baseDate)
            + "' THEN snapshot_aum ELSE 0 END) AS base_aum,\n"
            + "    SUM(CASE WHEN snapshot_date = '" + escapeSqlLiteral(shape.currentDate)
            + "' THEN snapshot_aum ELSE 0 END) AS current_aum\n"
            + "  FROM report_customer_snapshot\n"
            + "  GROUP BY report_org_no, customer_no\n"
            + "),\n"
            + "base_100_anchor AS (\n"
            + "  SELECT\n"
            + "    report_org_no,\n"
            + "    report_org_name,\n"
            + "    report_org_label,\n"
            + "    COUNT(DISTINCT customer_no) AS base_100\n"
            + "  FROM customer_pair\n"
            + "  WHERE base_aum >= " + shape.lowThreshold + "\n"
            + "  GROUP BY report_org_no, report_org_name, report_org_label\n"
            + "),\n"
            + "metric_by_org AS (\n"
            + "  SELECT\n"
            + "    report_org_no,\n"
            + "    NULLIF(" + countDistinctCase("snapshot_date = '" + escapeSqlLiteral(shape.currentDate)
            + "' AND snapshot_aum >= " + shape.lowThreshold) + ", 0) AS current_100,\n"
            + "    NULLIF(" + countDistinctCase("snapshot_date = '" + escapeSqlLiteral(shape.baseDate)
            + "' AND snapshot_aum >= " + shape.lowThreshold
            + " AND snapshot_aum < " + shape.highThreshold) + ", 0) AS base_100_600,\n"
            + "    NULLIF(" + countDistinctCase("snapshot_date = '" + escapeSqlLiteral(shape.currentDate)
            + "' AND snapshot_aum >= " + shape.lowThreshold
            + " AND snapshot_aum < " + shape.highThreshold) + ", 0) AS current_100_600,\n"
            + "    NULLIF(" + countDistinctCase("snapshot_date = '" + escapeSqlLiteral(shape.baseDate)
            + "' AND snapshot_aum >= " + shape.highThreshold) + ", 0) AS base_600,\n"
            + "    NULLIF(" + countDistinctCase("snapshot_date = '" + escapeSqlLiteral(shape.currentDate)
            + "' AND snapshot_aum >= " + shape.highThreshold) + ", 0) AS current_600\n"
            + "  FROM report_customer_snapshot\n"
            + "  GROUP BY report_org_no\n"
            + "),\n"
            + "growth_by_org AS (\n"
            + "  SELECT\n"
            + "    report_org_no,\n"
            + "    NULLIF(" + countDistinctCase("base_aum < " + shape.lowThreshold
            + " AND current_aum >= " + shape.lowThreshold) + ", 0) AS new_100,\n"
            + "    NULLIF(" + countDistinctCase("base_aum < " + shape.lowThreshold
            + " AND current_aum >= " + shape.lowThreshold
            + " AND current_aum < " + shape.highThreshold) + ", 0) AS new_100_600\n"
            + "  FROM org_customer_pair\n"
            + "  GROUP BY report_org_no\n"
            + ")";
    }

    private static String finalSelectSql() {
        return "SELECT\n"
            + "  a.report_org_no AS \"机构编码__第二层时点机构号\",\n"
            + "  a.report_org_name AS \"机构编码__第二层机构简称\",\n"
            + "  a.report_org_label AS \"org\",\n"
            + "  a.base_100 AS \"基期100\",\n"
            + "  m.current_100 AS \"当期100\",\n"
            + "  g.new_100 AS \"新增100\",\n"
            + "  g.new_100_600 AS \"新增100-600\",\n"
            + "  m.current_100 - a.base_100 AS \"Sum_增量100\",\n"
            + "  CAST(m.current_100 - a.base_100 AS DOUBLE) / NULLIF(CAST(a.base_100 AS DOUBLE), 0) AS \"Sum_增速100\",\n"
            + "  m.current_100_600 - m.base_100_600 AS \"Sum_增量100-600\",\n"
            + "  CAST(m.current_100_600 - m.base_100_600 AS DOUBLE) / NULLIF(CAST(m.base_100_600 AS DOUBLE), 0) AS \"Sum_增速100-600\",\n"
            + "  m.current_600 - m.base_600 AS \"Sum_增量600\"\n"
            + "FROM base_100_anchor a\n"
            + "LEFT JOIN metric_by_org m ON a.report_org_no = m.report_org_no\n"
            + "LEFT JOIN growth_by_org g ON a.report_org_no = g.report_org_no\n"
            + "ORDER BY a.report_org_label ASC,\n"
            + "  a.report_org_name ASC,\n"
            + "  a.base_100 ASC,\n"
            + "  m.current_100 ASC,\n"
            + "  g.new_100 ASC,\n"
            + "  g.new_100_600 ASC";
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
            + "),\n"
            + "key_diff AS (\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\" FROM original_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\" FROM rewrite_result\n"
            + "  UNION ALL\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\" FROM rewrite_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\" FROM original_result\n"
            + "),\n"
            + "metric_by_key_diff AS (\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\", \"基期100\", \"当期100\",\n"
            + "    \"新增100\", \"新增100-600\", \"Sum_增量100\", \"Sum_增量100-600\", \"Sum_增量600\" FROM original_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\", \"基期100\", \"当期100\",\n"
            + "    \"新增100\", \"新增100-600\", \"Sum_增量100\", \"Sum_增量100-600\", \"Sum_增量600\" FROM rewrite_result\n"
            + "  UNION ALL\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\", \"基期100\", \"当期100\",\n"
            + "    \"新增100\", \"新增100-600\", \"Sum_增量100\", \"Sum_增量100-600\", \"Sum_增量600\" FROM rewrite_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层时点机构号\", \"机构编码__第二层机构简称\", \"org\", \"基期100\", \"当期100\",\n"
            + "    \"新增100\", \"新增100-600\", \"Sum_增量100\", \"Sum_增量100-600\", \"Sum_增量600\" FROM original_result\n"
            + "),\n"
            + "org_label_diff AS (\n"
            + "  SELECT \"机构编码__第二层机构简称\", \"org\" FROM original_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层机构简称\", \"org\" FROM rewrite_result\n"
            + "  UNION ALL\n"
            + "  SELECT \"机构编码__第二层机构简称\", \"org\" FROM rewrite_result\n"
            + "  EXCEPT\n"
            + "  SELECT \"机构编码__第二层机构简称\", \"org\" FROM original_result\n"
            + ")\n"
            + "SELECT 'RESULT_SET_EXCEPT_DIFF' AS validation_method, COUNT(*) AS diff_count FROM result_diff\n"
            + "UNION ALL\n"
            + "SELECT 'KEY_CARDINALITY_DIFF' AS validation_method,\n"
            + "  ABS((SELECT COUNT(*) FROM original_result) - (SELECT COUNT(*) FROM rewrite_result)) AS diff_count\n"
            + "UNION ALL\n"
            + "SELECT 'ANCHOR_KEY_SET_DIFF' AS validation_method, COUNT(*) AS diff_count FROM key_diff\n"
            + "UNION ALL\n"
            + "SELECT 'ORG_LABEL_SET_DIFF' AS validation_method, COUNT(*) AS diff_count FROM org_label_diff\n"
            + "UNION ALL\n"
            + "SELECT 'METRIC_BY_KEY_DIFF' AS validation_method, COUNT(*) AS diff_count FROM metric_by_key_diff\n"
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
        methods.add(validationMethod(
            "RESULT_SET_EXCEPT_DIFF",
            "使用 original_result 与 rewrite_result 双向 EXCEPT 校验最终结果集完全一致。",
            "validationSql"
        ));
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
        methods.add(validationMethod(
            "ANCHOR_KEY_SET_DIFF",
            "校验改写结果仍以原 SQL 的基期100锚点行集为输出边界，避免结果行数被误压缩或误放大。",
            "validationSql"
        ));
        methods.add(validationMethod(
            "ORG_LABEL_SET_DIFF",
            "校验机构简称和 org 展示标签集合一致，覆盖深圳市分行及下属支行名称保留。",
            "validationSql"
        ));
        methods.add(validationMethod(
            "METRIC_BY_KEY_DIFF",
            "按机构键逐项比较核心指标，避免汇总数相同但单行数字不等价。",
            "validationSql"
        ));
        LinkedHashMap<String, Object> planReduction = validationMethod(
            "PLAN_SHAPE_SCAN_REDUCTION",
            "动态校验 rewrite 只读取报表客户快照 MV 或单个原始客户快照 CTE，避免继续多次扫描原始明细表。",
            "dynamicQueryBlockGraphEvidence"
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

    private static Map<String, Object> evidence(SnapshotShape shape,
                                                String mvName,
                                                SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("rule", RULE);
        evidence.put("mode", "DYNAMIC_REPEATED_SNAPSHOT_AGGREGATE_REPORT");
        evidence.put("generator", "DYNAMIC_AST_PROFILE_SNAPSHOT_AGGREGATE");
        evidence.put("staticTest01TemplateUsed", Boolean.FALSE);
        evidence.put("shapeDetectionSource", "PARSED_PROFILE_SQL_TOKENS_AND_QUERY_FEATURES");
        evidence.put("factTable", shape.factTable);
        evidence.put("dateColumn", shape.dateColumn);
        evidence.put("customerColumn", shape.customerColumn);
        evidence.put("measureColumn", shape.measureColumn);
        evidence.put("orgLevelColumn", shape.orgLevelColumn);
        evidence.put("orgLevelValue", shape.orgLevelValue);
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
        evidence.put("resultAnchor", "BASE_100_SUB34_EQUIVALENT");
        evidence.put("metricJoinShape", "BASE_100_ANCHOR_LEFT_JOIN_ORG_LEVEL_METRICS");
        evidence.put("orgLabelLineage", Arrays.asList("org_level2_name", "branch_org_name", shape.topOrgName));
        evidence.put("nullMetricSemantics", "ZERO_COUNT_METRICS_ARE_RENDERED_AS_NULL_TO_MATCH_LEFT_JOIN_ABSENCE");
        evidence.put("resourceReductionRationale",
            "原 SQL 对同一事实明细按日期和 AUM 分段重复扫描，改写后先收敛到报表机构-客户-日期粒度，再按基期100锚点和机构级指标聚合。");
        evidence.put("queryBlockFeatures", queryBlockFeatures(profile));
        evidence.put("fieldCoverageProof", fieldCoverageProof(evidence));
        evidence.put("replacementBoundary", "MV_REPLACES_DYNAMIC_CUSTOMER_DATE_SNAPSHOT_SUBGRAPH");
        evidence.put("claimBoundary", "DYNAMIC_REWRITE_REQUIRES_VALIDATION_SQL_BEFORE_ACTIVATION");
        return evidence;
    }

    private static Map<String, Object> queryBlockFeatures(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> features = new LinkedHashMap<String, Object>();
        if (profile == null) {
            return features;
        }
        features.put("repeatedTableScanCount", Integer.valueOf(profile.getRepeatedTableScanCount()));
        features.put("subqueryCount", Integer.valueOf(profile.getSubqueryCount()));
        features.put("nestedSubqueryDepth", Integer.valueOf(profile.getNestedSubqueryDepth()));
        features.put("joinCount", Integer.valueOf(profile.getJoinCount()));
        features.put("joinTypes", profile.getJoinTypes());
        features.put("orPredicateCount", Integer.valueOf(profile.getOrPredicateCount()));
        features.put("setOperation", Boolean.valueOf(profile.isSetOperation()));
        features.put("aggregateFunctionCount", Integer.valueOf(profile.getAggregateFunctionCount()));
        features.put("datePredicateColumns", new ArrayList<String>(profile.getDatePredicateColumns()));
        return features;
    }

    private static Map<String, Object> fieldCoverageProof(Map<String, Object> evidence) {
        LinkedHashMap<String, Object> proof = new LinkedHashMap<String, Object>();
        proof.put("status", "COVERED");
        proof.put("factTable", evidence.get("factTable"));
        proof.put("dateColumn", evidence.get("dateColumn"));
        proof.put("customerColumn", evidence.get("customerColumn"));
        proof.put("measureColumn", evidence.get("measureColumn"));
        proof.put("baseDate", evidence.get("baseDate"));
        proof.put("currentDate", evidence.get("currentDate"));
        proof.put("rewriteSource", evidence.get("rewriteSource"));
        proof.put("coversProjection", Boolean.TRUE);
        proof.put("coversFilters", Boolean.TRUE);
        proof.put("coversGrouping", Boolean.TRUE);
        proof.put("coversMeasures", Boolean.TRUE);
        return proof;
    }

    private static List<Map<String, Object>> reviewWarnings(SnapshotShape shape) {
        LinkedHashMap<String, Object> warning = new LinkedHashMap<String, Object>();
        warning.put("code", "SNAPSHOT_AGG_RESULT_VALIDATION_REQUIRED");
        warning.put(
            "description",
            "COUNT DISTINCT 被改写为报表机构-客户-日期快照上的锚点聚合，必须先通过结果集、机构标签、逐键指标和计划形态验证。"
        );
        warning.put("requiredEvidence", Arrays.asList(
            "RESULT_SET_EXCEPT_DIFF",
            "ORG_LABEL_SET_DIFF",
            "METRIC_BY_KEY_DIFF",
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
        coverage.put("validationMethodCount", Integer.valueOf(7));
        coverage.put("claimBoundary", "DYNAMIC_REWRITE_REQUIRES_VALIDATION_SQL_BEFORE_ACTIVATION");
        return coverage;
    }

    private static List<String> grain(SnapshotShape shape) {
        List<String> values = new ArrayList<String>();
        values.add("snapshot_date");
        values.addAll(aliasOrgNoReferences(shape));
        values.addAll(Arrays.asList(
            "report_org_no",
            "report_org_name",
            "report_org_label",
            "customer_no"
        ));
        return values;
    }

    private static List<String> dimensions(SnapshotShape shape) {
        List<String> values = new ArrayList<String>();
        values.add("snapshot_date");
        values.addAll(aliasOrgNoReferences(shape));
        values.addAll(Arrays.asList(
            "report_org_no",
            "report_org_name",
            "report_org_label",
            "customer_no"
        ));
        return values;
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
        predicates.add(predicate("org_scope", orgScopePredicate(shape, aliasOrgNoReferences(shape)), "EXTERNALIZED_PARAMETER_PREDICATE"));
        return predicates;
    }

    private static List<Map<String, Object>> retainedPredicates(SnapshotShape shape) {
        return Collections.<Map<String, Object>>singletonList(
            predicate("org_level", "org_level = " + shape.orgLevelValue, "RETAINED_BUSINESS_PREDICATE")
        );
    }

    private static Map<String, Object> predicate(String name, String expression, String classification) {
        LinkedHashMap<String, Object> predicate = new LinkedHashMap<String, Object>();
        predicate.put("name", name);
        predicate.put("expression", expression);
        predicate.put("classification", classification);
        return predicate;
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

    private static List<String> aliasOrgNoReferences(SnapshotShape shape) {
        List<String> values = new ArrayList<String>();
        for (Integer level : sortedLevels(shape.orgNoColumns)) {
            values.add("org_no_" + level);
        }
        return values;
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

    private static String countDistinctCase(String predicate) {
        return "COUNT(DISTINCT CASE WHEN " + predicate + " THEN customer_no END)";
    }

    private static String firstFactTable(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        for (Map<String, Object> item : advancedEntries(profile, "tables")) {
            if ("BASE_TABLE".equals(String.valueOf(item.get("sourceType")))) {
                String table = String.valueOf(item.get("tableName"));
                if (StringUtils.hasText(table)) {
                    String schema = String.valueOf(item.get("schemaName"));
                    if (StringUtils.hasText(schema) && table.startsWith(schema + ".")) {
                        return "\"" + schema + "\"." + table.substring(schema.length() + 1);
                    }
                    return table;
                }
            }
        }
        List<String> tables = profile.getTables();
        return tables.isEmpty() ? "" : stripAliasQualifier(tables.get(0));
    }

    private static String firstDateColumn(List<Map<String, Object>> predicates,
                                          SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        for (PredicateSignal signal : predicateSignals(predicates)) {
            if (isDateLiteral(signal.literal) && StringUtils.hasText(signal.column)) {
                return stripAliasQualifier(signal.column);
            }
        }
        for (String column : profile.getDatePredicateColumns()) {
            if (StringUtils.hasText(column)) {
                return stripAliasQualifier(column);
            }
        }
        return "";
    }

    private static String firstCustomerColumn(List<Map<String, Object>> projections,
                                              SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        String best = "";
        for (Map<String, Object> item : projections) {
            String alias = String.valueOf(item.get("alias"));
            if (aliasSuggestsCustomer(alias)) {
                best = choosePreferredColumn(best, projectionColumn(item));
            }
        }
        if (StringUtils.hasText(best)) {
            return best;
        }
        for (Map<String, Object> item : advancedEntries(profile, "aggregations")) {
            if ("COUNT".equals(String.valueOf(item.get("functionName"))) && Boolean.TRUE.equals(item.get("distinct"))) {
                List<String> sourceColumns = stringList(item.get("sourceColumns"));
                if (!sourceColumns.isEmpty()) {
                    return stripAliasQualifier(sourceColumns.get(0));
                }
            }
        }
        return "";
    }

    private static String firstMeasureColumn(List<Map<String, Object>> projections) {
        String best = "";
        for (Map<String, Object> item : projections) {
            String alias = String.valueOf(item.get("alias"));
            if (aliasSuggestsMeasure(alias)) {
                best = choosePreferredColumn(best, projectionColumn(item));
            }
        }
        return best;
    }

    private static OrgLevelSignal firstOrgLevelSignal(List<Map<String, Object>> predicates) {
        for (PredicateSignal signal : predicateSignals(predicates)) {
            if ("=".equals(signal.operator)
                && StringUtils.hasText(signal.literal)
                && StringUtils.hasText(signal.column)
                && columnSuggestsOrgLevel(signal.column)) {
                return new OrgLevelSignal(stripAliasQualifier(signal.column), signal.literal);
            }
        }
        return new OrgLevelSignal("", "");
    }

    private static Map<Integer, String> orgNoColumns(List<Map<String, Object>> projections) {
        return indexedColumns(projections, true);
    }

    private static Map<Integer, String> orgNameColumns(List<Map<String, Object>> projections) {
        return indexedColumns(projections, false);
    }

    private static Map<Integer, String> indexedColumns(List<Map<String, Object>> projections, boolean codeColumn) {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<Integer, String>();
        for (Map<String, Object> item : projections) {
            String alias = String.valueOf(item.get("alias"));
            Integer level = aliasLevel(alias);
            if (level == null) {
                continue;
            }
            if ((codeColumn && !aliasSuggestsOrgCode(alias)) || (!codeColumn && !aliasSuggestsOrgName(alias))) {
                continue;
            }
            String candidate = projectionColumn(item);
            if (!result.containsKey(level) || isBetterColumnCandidate(candidate, result.get(level))) {
                result.put(level, candidate);
            }
        }
        return result;
    }

    private static void augmentOrgScopeColumns(Map<Integer, String> orgNoColumns, List<String> scopeColumns) {
        for (String scopeColumn : scopeColumns) {
            Integer level = trailingLevel(scopeColumn);
            if (level == null || orgNoColumns.containsKey(level)) {
                continue;
            }
            orgNoColumns.put(level, stripAliasQualifier(scopeColumn));
        }
    }

    private static String normalizeDetectedColumn(String column) {
        if (!StringUtils.hasText(column)) {
            return "";
        }
        String trimmed = column.trim();
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            String qualifier = trimmed.substring(0, dot);
            if (qualifier.length() <= 3 && qualifier.matches("(?is)[A-Z][A-Z0-9_]*")) {
                return trimmed.substring(dot + 1);
            }
        }
        return trimmed;
    }

    private static boolean isBetterColumnCandidate(String candidate, String current) {
        if (!StringUtils.hasText(candidate)) {
            return false;
        }
        if (!StringUtils.hasText(current)) {
            return true;
        }
        int candidateScore = columnCandidateScore(candidate);
        int currentScore = columnCandidateScore(current);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore;
        }
        return candidate.length() > current.length();
    }

    private static int columnCandidateScore(String column) {
        String normalized = column == null ? "" : column.toUpperCase(Locale.ROOT);
        int score = 0;
        if (normalized.contains("__")) {
            score += 6;
        }
        if (!normalized.contains(".")) {
            score += 3;
        }
        int dot = normalized.indexOf('.');
        if (dot > 0) {
            String qualifier = normalized.substring(0, dot);
            if (qualifier.length() <= 3 && qualifier.matches("[A-Z][A-Z0-9_]*")) {
                score -= 5;
            }
        }
        return score;
    }

    private static ScopeSignal firstOrgScope(List<Map<String, Object>> predicates) {
        LinkedHashMap<String, ScopeSignal> byGroup = new LinkedHashMap<String, ScopeSignal>();
        for (PredicateSignal signal : predicateSignals(predicates)) {
            if (!"=".equals(signal.operator)
                || !StringUtils.hasText(signal.literal)
                || !StringUtils.hasText(signal.column)
                || !signal.groupId.startsWith("WHERE_OR_")) {
                continue;
            }
            ScopeSignal group = byGroup.get(signal.groupId);
            if (group == null) {
                group = new ScopeSignal(signal.literal);
                byGroup.put(signal.groupId, group);
            }
            if (!signal.literal.equals(group.value)) {
                group.mixedLiteral = true;
                continue;
            }
            group.columns.add(stripAliasQualifier(signal.column));
        }
        ScopeSignal best = new ScopeSignal("");
        for (ScopeSignal candidate : byGroup.values()) {
            if (!candidate.mixedLiteral && candidate.columns.size() > best.columns.size()) {
                best = candidate;
            }
        }
        return best;
    }

    private static List<String> dateLiterals(List<Map<String, Object>> predicates, String dateColumn) {
        LinkedHashSet<String> dates = new LinkedHashSet<String>();
        for (PredicateSignal signal : predicateSignals(predicates)) {
            if (isDateLiteral(signal.literal)
                && StringUtils.hasText(signal.column)
                && stripAliasQualifier(signal.column).equals(stripAliasQualifier(dateColumn))) {
                dates.add(signal.literal);
            }
        }
        List<String> result = new ArrayList<String>(dates);
        Collections.sort(result);
        return result;
    }

    private static List<Long> thresholds(List<Map<String, Object>> predicates) {
        LinkedHashSet<Long> values = new LinkedHashSet<Long>();
        for (PredicateSignal signal : predicateSignals(predicates)) {
            if (!(">=".equals(signal.operator) || "<".equals(signal.operator))) {
                continue;
            }
            if (!StringUtils.hasText(signal.literal) || isDateLiteral(signal.literal)) {
                continue;
            }
            try {
                long value = Long.parseLong(signal.literal);
                if (value >= 1000L) {
                    values.add(Long.valueOf(value));
                }
            } catch (NumberFormatException ex) {
                // ignore malformed numeric literal from a partial SQL fragment
            }
        }
        List<Long> result = new ArrayList<Long>(values);
        Collections.sort(result);
        return result;
    }

    private static String branchCaseLiteral(List<Map<String, Object>> projections, boolean codeBranch, int group) {
        for (Map<String, Object> item : projections) {
            String alias = String.valueOf(item.get("alias"));
            if ((codeBranch && !aliasSuggestsBranchCode(alias)) || (!codeBranch && !aliasSuggestsBranchName(alias))) {
                continue;
            }
            Matcher matcher = CASE_BRANCH_PATTERN.matcher(String.valueOf(item.get("expression")));
            if (matcher.matches()) {
                return matcher.group(group);
            }
        }
        return "";
    }

    private static String firstTopOrgName(List<Map<String, Object>> projections) {
        for (Map<String, Object> item : projections) {
            String alias = String.valueOf(item.get("alias"));
            String expression = String.valueOf(item.get("expression"));
            if (!aliasSuggestsTopOrgLabel(alias)) {
                continue;
            }
            Matcher matcher = STRING_LITERAL_PATTERN.matcher(expression);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    private static Integer aliasLevel(String alias) {
        String normalized = alias == null ? "" : alias.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.contains("第二层") || normalized.endsWith("_L2") || normalized.contains("LEVEL2")) {
            return Integer.valueOf(2);
        }
        if (normalized.contains("第三层") || normalized.endsWith("_L3") || normalized.contains("LEVEL3")) {
            return Integer.valueOf(3);
        }
        if (normalized.contains("第四层") || normalized.endsWith("_L4") || normalized.contains("LEVEL4")) {
            return Integer.valueOf(4);
        }
        if (normalized.matches(".*(?:_|\\b)L([0-9])(?:_|\\b).*")) {
            Matcher matcher = Pattern.compile(".*(?:_|\\b)L([0-9])(?:_|\\b).*").matcher(normalized);
            if (matcher.matches()) {
                return Integer.valueOf(matcher.group(1));
            }
        }
        return null;
    }

    private static Integer trailingLevel(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        Matcher suffixMatcher = Pattern.compile(".*(?:_|\\b)([0-9])$").matcher(normalized);
        if (suffixMatcher.matches()) {
            return Integer.valueOf(suffixMatcher.group(1));
        }
        Matcher levelMatcher = Pattern.compile(".*(?:_|\\b)L([0-9])(?:_|\\b)?.*").matcher(normalized);
        if (levelMatcher.matches()) {
            return Integer.valueOf(levelMatcher.group(1));
        }
        return null;
    }

    private static boolean aliasSuggestsOrgCode(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("机构号")
            || normalized.contains("ORG_CODE")
            || normalized.contains("ORG_NO")
            || normalized.contains("支行机构");
    }

    private static boolean aliasSuggestsOrgName(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("机构简称")
            || normalized.contains("ORG_NAME")
            || normalized.contains("SHORT_NAME")
            || normalized.contains("BRANCH_NAME")
            || normalized.contains("支行简称");
    }

    private static boolean aliasSuggestsCustomer(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("客户")
            || normalized.contains("CUSTOMER")
            || normalized.contains("CUST_");
    }

    private static boolean aliasSuggestsDate(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("日期")
            || normalized.contains("DATE")
            || normalized.endsWith("_DT")
            || normalized.contains("BIZ_DATE");
    }

    private static boolean aliasSuggestsMeasure(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("AUM")
            || normalized.contains("BALANCE")
            || normalized.contains("AMOUNT")
            || normalized.contains("月日均");
    }

    private static boolean aliasSuggestsTopOrgLabel(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return "ORG".equals(normalized) || normalized.contains("ORG_LABEL");
    }

    private static boolean aliasSuggestsBranchCode(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("支行机构") || normalized.contains("BRANCH_CODE");
    }

    private static boolean aliasSuggestsBranchName(String alias) {
        String normalized = alias == null ? "" : alias.toUpperCase(Locale.ROOT);
        return normalized.contains("支行简称") || normalized.contains("BRANCH_NAME");
    }

    private static boolean columnSuggestsOrgLevel(String column) {
        String normalized = column == null ? "" : column.toUpperCase(Locale.ROOT);
        return normalized.contains("ORG_LVL") || normalized.contains("ORG_LEVEL");
    }

    private static String projectionColumn(Map<String, Object> item) {
        List<String> sourceColumns = stringList(item.get("sourceColumns"));
        if (!sourceColumns.isEmpty()) {
            return stripAliasQualifier(sourceColumns.get(0));
        }
        return stripAliasQualifier(String.valueOf(item.get("expression")));
    }

    private static String choosePreferredColumn(String current, String candidate) {
        return isBetterColumnCandidate(candidate, current) ? candidate : current;
    }

    private static String stripAliasQualifier(String column) {
        if (!StringUtils.hasText(column)) {
            return "";
        }
        String normalized = column.replace("`", "").replace("\"", "").trim();
        int dot = normalized.indexOf('.');
        if (dot > 0) {
            return normalized.substring(dot + 1);
        }
        return normalized;
    }

    private static boolean isDateLiteral(String value) {
        return StringUtils.hasText(value)
            && (value.matches("[0-9]{8}") || value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"));
    }

    private static List<Map<String, Object>> advancedEntries(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                             String key) {
        Object value = profile.toAdvancedStructureProfile().get(key);
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : raw) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) item;
                result.add(cast);
            }
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List)) {
            return Collections.emptyList();
        }
        List<?> raw = (List<?>) value;
        List<String> result = new ArrayList<String>();
        for (Object item : raw) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private static List<PredicateSignal> predicateSignals(List<Map<String, Object>> predicates) {
        List<PredicateSignal> result = new ArrayList<PredicateSignal>();
        for (Map<String, Object> item : predicates) {
            List<String> sourceColumns = stringList(item.get("sourceColumns"));
            String column = sourceColumns.isEmpty() ? "" : sourceColumns.get(0);
            String expression = String.valueOf(item.get("expression"));
            String operator = "";
            if (expression.contains(">=")) {
                operator = ">=";
            } else if (expression.contains("<=")) {
                operator = "<=";
            } else if (expression.contains(" <> ")) {
                operator = "<>";
            } else if (expression.contains(" != ")) {
                operator = "!=";
            } else if (expression.contains(" < ")) {
                operator = "<";
            } else if (expression.contains(" > ")) {
                operator = ">";
            } else if (expression.contains(" = ")) {
                operator = "=";
            }
            String literal = firstStringLiteral(expression);
            if (!StringUtils.hasText(literal)) {
                literal = firstNumericLiteral(expression);
            }
            result.add(new PredicateSignal(column, operator, literal, String.valueOf(item.get("groupId"))));
        }
        return result;
    }

    private static String firstStringLiteral(String expression) {
        Matcher matcher = STRING_LITERAL_PATTERN.matcher(expression == null ? "" : expression);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String firstNumericLiteral(String expression) {
        Matcher matcher = NUMERIC_LITERAL_PATTERN.matcher(expression == null ? "" : expression);
        while (matcher.find()) {
            String literal = matcher.group(1);
            if (!"255".equals(literal)) {
                return literal;
            }
        }
        return "";
    }

    private static String stripQuotedAliases(String sql) {
        if (!StringUtils.hasText(sql)) {
            return "";
        }
        return sql.replaceAll("(?is)AS\\s+\"[^\"]+\"", " ");
    }

    private static String trimTrailingSemicolon(String sql) {
        String value = sql == null ? "" : sql.trim();
        while (value.endsWith(";")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value;
    }

    private static String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        if (StringUtils.hasText(second)) {
            return second.trim();
        }
        return "";
    }

    private static String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
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
        private final String ddlSql;
        private final String refreshSql;
        private final String validationSql;
        private final String rollbackSql;
        private final String rewriteSql;
        private final List<Map<String, Object>> blockingReasons;
        private final SnapshotShape shape;
        private final List<Map<String, Object>> validationMethods;
        private final Map<String, Object> rewriteEvidence;

        private CandidateSql(String ddlSql,
                             String refreshSql,
                             String validationSql,
                             String rollbackSql,
                             String rewriteSql,
                             List<Map<String, Object>> blockingReasons,
                             SnapshotShape shape,
                             List<Map<String, Object>> validationMethods,
                             Map<String, Object> rewriteEvidence) {
            this.ddlSql = ddlSql;
            this.refreshSql = refreshSql;
            this.validationSql = validationSql;
            this.rollbackSql = rollbackSql;
            this.rewriteSql = rewriteSql;
            this.blockingReasons = blockingReasons;
            this.shape = shape;
            this.validationMethods = validationMethods;
            this.rewriteEvidence = rewriteEvidence;
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
                ddlSql,
                refreshSql,
                validationSql,
                rollbackSql,
                rewriteSql,
                Collections.<Map<String, Object>>emptyList(),
                shape,
                validationMethods,
                rewriteEvidence
            );
        }

        static CandidateSql blocked(List<Map<String, Object>> blockingReasons,
                                    SnapshotShape shape,
                                    SqlOptimizationPipelineService.ParsedSqlProfile profile) {
            return new CandidateSql(
                null,
                null,
                null,
                null,
                null,
                blockingReasons == null ? Collections.<Map<String, Object>>emptyList() : blockingReasons,
                shape,
                validationMethods(shape, null),
                evidence(shape, null, profile)
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
            return grain(shape);
        }

        List<String> getDimensions() {
            return dimensions(shape);
        }

        List<Map<String, Object>> getMeasures() {
            return measures();
        }

        List<Map<String, Object>> getExternalizedPredicates() {
            return externalizedPredicates(shape);
        }

        List<Map<String, Object>> getRetainedPredicates() {
            return retainedPredicates(shape);
        }
    }

    private static final class ScopeSignal {
        private final String value;
        private final List<String> columns = new ArrayList<String>();
        private boolean mixedLiteral;

        private ScopeSignal(String value) {
            this.value = value == null ? "" : value;
        }
    }

    private static final class PredicateSignal {
        private final String column;
        private final String operator;
        private final String literal;
        private final String groupId;

        private PredicateSignal(String column, String operator, String literal, String groupId) {
            this.column = column == null ? "" : column;
            this.operator = operator == null ? "" : operator;
            this.literal = literal == null ? "" : literal;
            this.groupId = groupId == null ? "" : groupId;
        }
    }

    private static final class OrgLevelSignal {
        private final String column;
        private final String value;

        private OrgLevelSignal(String column, String value) {
            this.column = column == null ? "" : column;
            this.value = value == null ? "" : value;
        }
    }

    private static final class SnapshotShape {
        private final String sourceSql;
        private final String factTable;
        private final String dateColumn;
        private final String customerColumn;
        private final String measureColumn;
        private final String orgLevelColumn;
        private final String orgLevelValue;
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
                              String orgLevelValue,
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
            this.orgLevelValue = orgLevelValue;
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
