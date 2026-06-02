package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.cost.RewriteCostEstimate;
import com.company.sqloptimization.domain.rewrite.ir.RelationalOperator;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.ir.RewriteIrConflict;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionReport;
import com.company.sqloptimization.domain.rewrite.production.RewriteProductionAdapterStatus;
import com.company.sqloptimization.domain.rewrite.production.RewriteProductionCapabilityReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagIssue;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockEdge;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockNode;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteCandidate;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewriteRuleType;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendation;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleDependencyEdge;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheck;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceCheckType;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

abstract class SqlOptimizationPipelineServiceTestSupport {

    protected final SqlOptimizationPipelineService service = new SqlOptimizationPipelineService();

    protected String complexAntiPatternSql() {
        return "-- complex anti-pattern query\n"
            + "SELECT c.customer_id, c.customer_name, c.state,\n"
            + "(SELECT COUNT(*) FROM orders o WHERE o.customer_id = c.customer_id) AS total_orders,\n"
            + "(SELECT SUM(order_amount) FROM orders o WHERE o.customer_id = c.customer_id) AS total_spent,\n"
            + "(SELECT GROUP_CONCAT(product_name) FROM order_items oi JOIN products p ON oi.product_id = p.product_id "
            + "WHERE oi.customer_id = c.customer_id) AS all_products\n"
            + "FROM customers c\n"
            + "WHERE c.is_active = 1 AND c.customer_id IN (\n"
            + "SELECT o1.customer_id FROM orders o1 WHERE YEAR(o1.order_date) = 2025\n"
            + "AND NOT EXISTS (SELECT 1 FROM customer_tags ct WHERE ct.customer_id = o1.customer_id AND ct.tag_name = 'VIP')\n"
            + "AND o1.order_amount > (SELECT AVG(o2.order_amount) FROM orders o2 "
            + "WHERE o2.state = (SELECT state FROM customers WHERE customer_id = o1.customer_id))\n"
            + "AND EXISTS (SELECT 1 FROM order_items oi2 WHERE oi2.order_id = o1.order_id "
            + "AND oi2.product_id IN (SELECT product_id FROM products WHERE category LIKE '%电子%')))\n"
            + "OR c.customer_id IN (SELECT customer_id FROM orders WHERE order_amount > 10000)\n"
            + "ORDER BY RAND() LIMIT 10";
    }

    protected String reportSnapshotNamingVariantSql() {
        return "SELECT b.org_code_l2, b.org_name_l2, b.org_label, b.base_100, c.current_100, "
            + "x.base_600, y.current_600, n.new_100\n"
            + "FROM (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    FACT_CUSTOMER_ASSET__ORG_NAME_L2 AS org_name_l2,\n"
            + "    '深圳分行' AS org_label,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS base_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-04-30'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2, FACT_CUSTOMER_ASSET__ORG_NAME_L2\n"
            + ") b\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS current_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") c ON b.org_code_l2 = c.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS base_600\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-04-30'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 6000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") x ON b.org_code_l2 = x.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L2 AS org_code_l2,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS current_600\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 6000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L2\n"
            + ") y ON b.org_code_l2 = y.org_code_l2\n"
            + "LEFT JOIN (\n"
            + "  SELECT FACT_CUSTOMER_ASSET__ORG_CODE_L3 AS org_code_l3,\n"
            + "    FACT_CUSTOMER_ASSET__ORG_SHORT_NAME_L3 AS branch_name,\n"
            + "    COUNT(DISTINCT FACT_CUSTOMER_ASSET__CUSTOMER_ID) AS new_100\n"
            + "  FROM rpt_customer_asset_snapshot\n"
            + "  WHERE (FACT_CUSTOMER_ASSET__ORG_CODE_L2 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L3 = 'SZ001'\n"
            + "    OR FACT_CUSTOMER_ASSET__ORG_CODE_L4 = 'SZ001')\n"
            + "    AND FACT_CUSTOMER_ASSET__ORG_LEVEL = 4\n"
            + "    AND FACT_CUSTOMER_ASSET__BIZ_DATE = '2026-05-31'\n"
            + "    AND FACT_CUSTOMER_ASSET__AVG_BALANCE >= 1000000\n"
            + "  GROUP BY FACT_CUSTOMER_ASSET__ORG_CODE_L3, FACT_CUSTOMER_ASSET__ORG_SHORT_NAME_L3\n"
            + ") n ON b.org_code_l2 = n.org_code_l3";
    }

    protected boolean containsRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return true;
            }
        }
        return false;
    }

    protected Map<String, Object> rule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return entry;
            }
        }
        throw new AssertionError("Missing rule " + rule);
    }

    protected Set<String> allRuleNames(SqlOptimizationPipelineService.RecommendationRuleOutputModel model) {
        Set<String> names = new LinkedHashSet<String>();
        addRuleNames(names, model.getRuleChain());
        addRuleNames(names, model.getUnappliedRules());
        return names;
    }

    protected void addRuleNames(Set<String> names, List<Map<String, Object>> entries) {
        for (Map<String, Object> entry : entries) {
            Object rule = entry.get("rule");
            if (rule != null) {
                names.add(String.valueOf(rule));
            }
        }
    }

    protected boolean containsText(Iterable<String> values, String expectedText) {
        for (String value : values) {
            if (value != null && value.contains(expectedText)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasCode(List<Map<String, Object>> entries, String code) {
        for (Map<String, Object> entry : entries) {
            if (code.equals(entry.get("code"))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?>)) {
            return java.util.Collections.emptyList();
        }
        return (List<Map<String, Object>>) value;
    }

    protected void assertYonghongRewriteRecommendationGenerated(String caseName, String sql) {
        SqlOptimizationPipelineService.ParsedSqlProfile profile = service.analyze(sql, DataSourceTypeEnum.HETU);
        SqlOptimizationPipelineService.RecommendationRuleOutputModel model =
            service.buildRecommendationRuleOutputModel(profile);
        Map<String, Object> accelerationArtifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
            new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                sql,
                "HETU",
                "datasource-yonghong",
                "fingerprint-" + caseName,
                "SZ_0000003772",
                "million-customer-growth",
                null
            ),
            profile
        );

        assertEquals("APACHE_CALCITE", profile.getParserEngine(), caseName);
        assertTrue(containsText(profile.getTables(), "BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"), caseName);
        assertTrue(profile.getSubqueryCount() >= 20, caseName + " subqueryCount=" + profile.getSubqueryCount());
        assertTrue(containsRule(model.getRuleChain(), "PRECOMPUTE_MV"), caseName);
        assertTrue(containsRule(model.getRuleChain(), "REPORT_SQL_MERGE"), caseName);
        assertEquals(MaterializedViewRecommendationPlanner.SOURCE_AST_IR,
            rule(model.getRuleChain(), "PRECOMPUTE_MV").get("evidenceLevel"), caseName);
        assertNotNull(accelerationArtifact, caseName);
        assertFalse("EXACT_QUERY_MV".equals(accelerationArtifact.get("mvType")), caseName);
        assertTrue(String.valueOf(accelerationArtifact.get("commonSubgraphEvidence"))
            .contains("CALCITE_AST_QBDAG_STRUCTURAL_REUSE"), caseName + " " + accelerationArtifact);
        if (!"BLOCKED".equals(accelerationArtifact.get("artifactStatus"))) {
            assertTrue(String.valueOf(accelerationArtifact.get("rewriteSql"))
                .contains(String.valueOf(accelerationArtifact.get("mvName"))), caseName + " " + accelerationArtifact);
            assertFalse(String.valueOf(accelerationArtifact.get("rewriteSql"))
                .contains("FROM \"BI_HQX00_V\".BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"), caseName);
            assertFalse(String.valueOf(accelerationArtifact.get("rewriteSql"))
                .contains("FROM BI_HQX00_V.BIM_PB_W_00_I_WDM_PF_IDV_CUST_FA_SUM"), caseName);
        }
    }

    protected String frontendFormatSql(String sql) throws Exception {
        String formatted = runFrontendFormatter(sql);
        if (formatted != null) {
            return formatted;
        }
        return formatYonghongLikeFrontendFallback(sql);
    }

    protected String runFrontendFormatter(String sql) throws Exception {
        Path root = repositoryRoot();
        Path input = Files.createTempFile("sqlforge-test01-input", ".sql");
        Path output = Files.createTempFile("sqlforge-test01-formatted", ".sql");
        Path error = Files.createTempFile("sqlforge-test01-format-error", ".log");
        try {
            Files.write(input, sql.getBytes(StandardCharsets.UTF_8));
            String script =
                "import fs from 'node:fs';"
                    + "import { pathToFileURL } from 'node:url';"
                    + "const root = process.cwd();"
                    + "const { formatSqlText } = await import(pathToFileURL(root + '/src/views/common/sqlFormatting.mjs').href);"
                    + "const input = fs.readFileSync(process.argv[process.argv.length - 1], 'utf8');"
                    + "process.stdout.write(formatSqlText(input));";
            Process process = new ProcessBuilder(
                "node",
                "--input-type=module",
                "-e",
                script,
                input.toString()
            )
                .directory(root.toFile())
                .redirectOutput(output.toFile())
                .redirectError(error.toFile())
                .start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            return new String(Files.readAllBytes(output), StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(input);
            Files.deleteIfExists(output);
            Files.deleteIfExists(error);
        }
    }

    protected String formatYonghongLikeFrontendFallback(String sql) {
        return sql
            .replace("FROM (\n(SELECT", "FROM (\n  (\n    SELECT")
            .replace("LEFT JOIN (\n(SELECT", "LEFT JOIN (\n  (\n    SELECT")
            .replace("RIGHT JOIN (\n(SELECT", "RIGHT JOIN (\n  (\n    SELECT")
            .replace("FULL JOIN (\n(SELECT", "FULL JOIN (\n  (\n    SELECT")
            .replace("INNER JOIN (\n(SELECT", "INNER JOIN (\n  (\n    SELECT");
    }

    protected boolean containsJoinDoubleSelectWrapper(String sql) {
        return java.util.regex.Pattern.compile("(?is)\\bJOIN\\s*\\(\\s*\\(\\s*SELECT\\b")
            .matcher(sql)
            .find();
    }

    protected String stripLineCommentsForRegression(String sql) {
        StringBuilder builder = new StringBuilder(sql.length());
        String[] lines = sql.split("\\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (!line.trim().startsWith("--")) {
                builder.append(line);
            }
            if (index < lines.length - 1) {
                builder.append('\n');
            }
        }
        return builder.toString();
    }

    protected String readRepositorySqlFixture(String relativePath) throws Exception {
        Path root = repositoryRoot();
        Path fixture = root.resolve(relativePath);
        assertTrue(Files.exists(fixture), "缺少 SQL fixture：" + fixture);
        return new String(Files.readAllBytes(fixture), StandardCharsets.UTF_8);
    }

    protected Path repositoryRoot() {
        Path root = Paths.get("").toAbsolutePath();
        if (!Files.exists(root.resolve("docs/test01.sql"))) {
            root = root.resolve("..").normalize();
        }
        return root;
    }

    protected String normalizeExecutableSql(String sql) {
        StringBuilder builder = new StringBuilder();
        String[] lines = sql == null ? new String[0] : sql.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.length() > 0) {
                builder.append(trimmed).append('\n');
            }
        }
        return builder.toString()
            .replaceAll("\\s+", " ")
            .replaceAll("\\(\\s+", "(")
            .replaceAll("\\s+\\)", ")")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("\\s*;\\s*$", "")
            .trim();
    }

    protected String artifact(OptimizationTaskSuggestion suggestion, String category, String name) {
        for (OptimizationTaskArtifact artifact : suggestion.getArtifacts()) {
            if (category.equals(artifact.getCategory()) && name.equals(artifact.getName())) {
                return artifact.getContent();
            }
        }
        assertNotNull(null, "缺少优化任务制品：" + category + "/" + name);
        return "";
    }

    protected Sample sample(String name, String sql, String expectedRule) {
        return new Sample(name, sql, expectedRule);
    }

    protected Map<String, Object> findRule(List<Map<String, Object>> entries, String rule) {
        for (Map<String, Object> entry : entries) {
            if (rule.equals(entry.get("rule"))) {
                return entry;
            }
        }
        assertNotNull(null, "缺少预期规则：" + rule);
        return null;
    }

    protected boolean containsPrecondition(List<Map<String, Object>> entries, String code) {
        for (Map<String, Object> entry : entries) {
            if (code.equals(entry.get("code"))) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsOperator(RewriteCoreIrSnapshot snapshot, RelationalOperator operator) {
        for (com.company.sqloptimization.domain.rewrite.ir.RelationalAlgebraNode node : snapshot.getRelationalAlgebra()) {
            if (operator == node.getOperator()) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsConflict(RewriteCoreIrSnapshot snapshot, String conflictCode) {
        for (RewriteIrConflict conflict : snapshot.getArchitectureConflicts()) {
            if (conflictCode.equals(conflict.getConflictCode())) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsDagEdge(QueryBlockDag dag, String edgeType) {
        for (QueryBlockEdge edge : dag.getEdges()) {
            if (edgeType.equals(edge.getEdgeType())) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsDagIssue(QueryBlockDag dag, String issueCode) {
        for (QueryBlockDagIssue issue : dag.getIssues()) {
            if (issueCode.equals(issue.getCode())) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasExternalReference(QueryBlockDag dag, String expected) {
        for (QueryBlockNode block : dag.getBlocks()) {
            for (String reference : block.getExternalReferences()) {
                if (expected.equalsIgnoreCase(reference)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean hasEquivalentQueryBlock(RewriteCoreIrSnapshot snapshot) {
        for (com.company.sqloptimization.domain.rewrite.ir.QueryBlockIr block : snapshot.getQueryBlocks()) {
            if (Boolean.TRUE.equals(block.getAttributes().get("equivalentToRepresentative"))) {
                return true;
            }
        }
        return false;
    }

    protected RelationalRewriteCandidate firstCandidate(RelationalRewritePlan plan, RelationalRewriteRuleType ruleType) {
        List<RelationalRewriteCandidate> candidates = plan.candidatesOf(ruleType);
        assertFalse(candidates.isEmpty(), "缺少关系代数改写候选：" + ruleType);
        return candidates.get(0);
    }

    protected SemanticEquivalenceCheck firstSemanticCheck(SemanticEquivalenceReport report,
                                                        SemanticEquivalenceCheckType checkType,
                                                        RelationalRewriteRuleType ruleType) {
        for (SemanticEquivalenceCheck check : report.getChecks()) {
            if (checkType == check.getCheckType() && ruleType == check.getRuleType()) {
                return check;
            }
        }
        assertNotNull(null, "缺少语义等价验证：" + checkType + " / " + ruleType);
        return null;
    }

    protected double minParetoScanCost(CostBasedRewriteSelectionReport report) {
        double min = Double.MAX_VALUE;
        for (RewriteCostEstimate estimate : report.getEstimates()) {
            if (estimate.isParetoOptimal()) {
                min = Math.min(min, estimate.getCostVector().getScanCost());
            }
        }
        return min;
    }

    protected double minParetoMemoryCost(CostBasedRewriteSelectionReport report) {
        double min = Double.MAX_VALUE;
        for (RewriteCostEstimate estimate : report.getEstimates()) {
            if (estimate.isParetoOptimal()) {
                min = Math.min(min, estimate.getCostVector().getMemoryCost());
            }
        }
        return min;
    }

    protected boolean containsDependency(RuleConflictResolutionReport report, String sourceRuleId, String targetRuleId) {
        for (RuleDependencyEdge edge : report.getDependencyEdges()) {
            if (sourceRuleId.equals(edge.getSourceRuleId()) && targetRuleId.equals(edge.getTargetRuleId())) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsParserRole(ParserStackFusionReport report, String parser) {
        for (Map<String, Object> role : report.getParserRoles()) {
            if (parser.equals(role.get("parser"))) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsPlannerStage(ParserStackFusionReport report, String stage) {
        for (Map<String, Object> item : report.getCalcitePlannerStages()) {
            if (stage.equals(item.get("stage"))) {
                return true;
            }
        }
        return false;
    }

    protected boolean containsReviewRequirement(RewriteRecommendationReport report, String requirement) {
        for (RewriteRecommendation recommendation : report.getRecommendations()) {
            if (recommendation.getReviewRequirements().contains(requirement)) {
                return true;
            }
        }
        return false;
    }

    protected RewriteProductionAdapterStatus adapter(RewriteProductionCapabilityReport report, String adapterName) {
        for (RewriteProductionAdapterStatus adapter : report.getAdapters()) {
            if (adapterName.equals(adapter.getAdapterName())) {
                return adapter;
            }
        }
        assertNotNull(null, "缺少生产化适配层状态：" + adapterName);
        return null;
    }

    protected HetuPlanAnalysisClient successfulHetuPlanAnalysisClient() {
        return new HetuPlanAnalysisClient() {
            @Override
            public HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode) {
                return HetuPlanAnalysisResult.success(
                    datasourceCode,
                    "Fragment 0 [SOURCE]\n  TableScan[orders]",
                    12L,
                    Collections.singletonList("hetuExplain=success")
                );
            }
        };
    }

    protected String readFixture(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("..").resolve(relativePath);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    protected String repeatedAggregateLeftJoinSql() {
        return "SELECT b.customer_id, s1.base_100, s2.current_100, s3.base_600 "
            + "FROM customers b "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS base_100 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-04-30' AND avg_balance >= 1000000 "
            + "  GROUP BY customer_id"
            + ") s1 ON b.customer_id = s1.customer_id "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS current_100 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-05-31' AND avg_balance >= 1000000 "
            + "  GROUP BY customer_id"
            + ") s2 ON b.customer_id = s2.customer_id "
            + "LEFT JOIN ("
            + "  SELECT customer_id, COUNT(DISTINCT customer_id) AS base_600 "
            + "  FROM customer_snapshot "
            + "  WHERE dt = DATE '2026-04-30' AND avg_balance >= 6000000 "
            + "  GROUP BY customer_id"
            + ") s3 ON b.customer_id = s3.customer_id";
    }

    protected String fanruanRepeatedAggregateSql() {
        return "SELECT /* Sub1_分组和汇总 */ a.customer_id, a.total_amount AS base_amount, "
            + "b.total_amount AS current_amount "
            + "FROM ("
            + "  SELECT customer_id, DTE, SUM(amount) AS total_amount "
            + "  FROM orders "
            + "  WHERE DTE = DATE '2026-05-01' "
            + "  GROUP BY customer_id, DTE"
            + ") a "
            + "JOIN ("
            + "  SELECT customer_id, DTE, SUM(amount) AS total_amount "
            + "  FROM orders "
            + "  WHERE DTE = DATE '2026-05-31' "
            + "  GROUP BY customer_id, DTE"
            + ") b ON a.customer_id = b.customer_id";
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> nestedMap(Map<String, Object> value, String key) {
        Object nested = value.get(key);
        assertTrue(nested instanceof Map, "缺少嵌套 map：" + key);
        return (Map<String, Object>) nested;
    }

    protected static final class Sample {
        protected final String name;
        protected final String sql;
        protected final String expectedRule;

        protected Sample(String name, String sql, String expectedRule) {
            this.name = name;
            this.sql = sql;
            this.expectedRule = expectedRule;
        }
    }
}
