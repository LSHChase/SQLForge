package com.company.sqloptimization.domain.rewrite.production;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.config.RewriteProductionGateProperties;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.parse.PlanAnalysisStatus;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.AnsiSqlDialect;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.springframework.util.StringUtils;

public class RewriteProductionCapabilityAnalyzer {

    private final RewriteProductionGateProperties properties;
    private final HetuPlanAnalysisClient hetuPlanAnalysisClient;

    public RewriteProductionCapabilityAnalyzer(RewriteProductionGateProperties properties,
                                               HetuPlanAnalysisClient hetuPlanAnalysisClient) {
        this.properties = properties == null ? new RewriteProductionGateProperties() : properties;
        this.hetuPlanAnalysisClient = hetuPlanAnalysisClient == null
            ? HetuPlanAnalysisClient.unavailable()
            : hetuPlanAnalysisClient;
    }

    public RewriteProductionCapabilityReport assess(String sqlText,
                                                    String tenantId,
                                                    String datasourceCode,
                                                    DataSourceTypeEnum datasourceType) {
        String normalizedSql = text(sqlText);
        List<RewriteProductionAdapterStatus> adapters = new ArrayList<RewriteProductionAdapterStatus>();
        adapters.add(assessCalciteRelNode(normalizedSql));
        adapters.add(assessRelToSql(normalizedSql));
        adapters.add(assessJsqlParserMetadata(normalizedSql));
        adapters.add(assessSmtZ3());
        adapters.add(assessHetuExplain(normalizedSql, tenantId, datasourceCode, datasourceType));
        adapters.add(assessStatisticsCost());

        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("defaultRuntimeBoundary", "STATIC_CHAIN_COMPATIBLE_BY_DEFAULT");
        attributes.put("claimBoundary", "ADAPTER_STATUS_NOT_PRODUCTION_SCALE_EVIDENCE");
        attributes.put("sqlExecutionBoundary", "ONLY_HETU_EXPLAIN_ADAPTER_MAY_EXECUTE_EXPLAIN_WHEN_ENABLED");
        attributes.put("autoApplyAllowed", Boolean.FALSE);
        attributes.put("developmentDirectActivationEnabled",
            Boolean.valueOf(properties.isDevelopmentDirectActivationEnabled()));
        return new RewriteProductionCapabilityReport(capabilityStatus(adapters), adapters, attributes);
    }

    private RewriteProductionAdapterStatus assessCalciteRelNode(String sqlText) {
        if (!properties.getCalciteRelNode().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "CALCITE_RELNODE",
                "REAL_CALCITE_RELNODE_NOT_REQUESTED"
            );
        }
        CalciteRelNodeAttempt attempt = tryBuildCalciteRelNode(sqlText);
        if (attempt.relNode != null) {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("relNodeClass", attempt.relNode.getClass().getName());
            attributes.put("sqlNodeClass", attempt.sqlNodeClass);
            attributes.put("rowType", String.valueOf(attempt.relNode.getRowType()));
            return new RewriteProductionAdapterStatus(
                "CALCITE_RELNODE",
                true,
                "REAL_RELNODE_AVAILABLE",
                "REAL_CALCITE_RELNODE",
                "RELNODE_CONVERTED_WITH_REPO_SCHEMA_CONTEXT",
                "CALCITE_FRAMEWORK_PLANNER",
                "",
                Arrays.asList("planner=Frameworks.getPlanner", "relConversion=true"),
                attributes
            );
        }
        return new RewriteProductionAdapterStatus(
            "CALCITE_RELNODE",
            true,
            "BLOCKED_SCHEMA_OR_DIALECT_REQUIRED",
            "CALCITE_SQL_NODE_ONLY",
            "REAL_RELNODE_NOT_AVAILABLE_FOR_THIS_SQL",
            "CALCITE_FRAMEWORK_PLANNER",
            attempt.failureReason,
            attempt.evidence(),
            attempt.attributes()
        );
    }

    private RewriteProductionAdapterStatus assessRelToSql(String sqlText) {
        if (!properties.getRelToSql().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "CALCITE_REL_TO_SQL",
                "REAL_REL_TO_SQL_NOT_REQUESTED"
            );
        }
        CalciteRelNodeAttempt attempt = tryBuildCalciteRelNode(sqlText);
        if (attempt.relNode == null) {
            return new RewriteProductionAdapterStatus(
                "CALCITE_REL_TO_SQL",
                true,
                "BLOCKED_RELNODE_REQUIRED",
                "CALCITE_SQL_NODE_ONLY",
                "REL_TO_SQL_NOT_AVAILABLE_WITHOUT_RELNODE",
                "CALCITE_REL_TO_SQL_CONVERTER",
                attempt.failureReason,
                attempt.evidence(),
                attempt.attributes()
            );
        }
        try {
            SqlNode converted = new RelToSqlConverter(AnsiSqlDialect.DEFAULT).visit(attempt.relNode).asStatement();
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("relNodeClass", attempt.relNode.getClass().getName());
            attributes.put("convertedSqlPreview", compact(converted.toSqlString(AnsiSqlDialect.DEFAULT).getSql()));
            return new RewriteProductionAdapterStatus(
                "CALCITE_REL_TO_SQL",
                true,
                "REAL_REL_TO_SQL_AVAILABLE",
                "REAL_CALCITE_REL_TO_SQL",
                "CONVERTED_SQL_IS_NOT_AUTO_APPROVED",
                "CALCITE_REL_TO_SQL_CONVERTER",
                "",
                Arrays.asList("converter=RelToSqlConverter", "sqlDialect=ANSI"),
                attributes
            );
        } catch (RuntimeException ex) {
            return failedAdapter(
                "CALCITE_REL_TO_SQL",
                "REL_TO_SQL_CONVERSION_FAILED",
                "REL_TO_SQL_NOT_AVAILABLE_FOR_THIS_RELNODE",
                "CALCITE_REL_TO_SQL_CONVERTER",
                ex
            );
        }
    }

    private RewriteProductionAdapterStatus assessJsqlParserMetadata(String sqlText) {
        if (!properties.getJsqlParserMetadata().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "JSQLPARSER_METADATA_INJECTION",
                "JSQLPARSER_TAG_INJECTION_REPORT_NOT_REQUESTED"
            );
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sqlText);
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("statementClass", statement.getClass().getName());
            attributes.put("metadataInjectionTarget", "CALCITE_L4_REWRITE_CONSTRAINTS");
            return new RewriteProductionAdapterStatus(
                "JSQLPARSER_METADATA_INJECTION",
                true,
                "REAL_JSQLPARSER_METADATA_AVAILABLE",
                "REAL_JSQLPARSER_AST",
                "METADATA_TAGS_STILL_REQUIRE_REWRITE_VALIDATION",
                "JSQLPARSER_PARSE_AND_TAG",
                "",
                Arrays.asList("parser=JSqlParser", "tagInjection=enabled"),
                attributes
            );
        } catch (Exception ex) {
            return failedAdapter(
                "JSQLPARSER_METADATA_INJECTION",
                "JSQLPARSER_METADATA_PARSE_FAILED",
                "JSQLPARSER_TAGS_NOT_AVAILABLE_FOR_THIS_SQL",
                "JSQLPARSER_PARSE_AND_TAG",
                ex
            );
        }
    }

    private RewriteProductionAdapterStatus assessSmtZ3() {
        if (!properties.getSmtZ3().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "SMT_Z3_EQUIVALENCE",
                "OPTIONAL_SOLVER_NOT_REQUESTED"
            );
        }
        String command = text(properties.getSmtZ3().getCommand());
        if (!StringUtils.hasText(command)) {
            return new RewriteProductionAdapterStatus(
                "SMT_Z3_EQUIVALENCE",
                true,
                "BLOCKED_SOLVER_COMMAND_MISSING",
                "STATIC_PROOF_OBLIGATIONS",
                "SMT_PROOF_NOT_EXECUTED",
                "EXTERNAL_Z3_OPTIONAL_ADAPTER",
                "z3 command 未配置",
                Collections.singletonList("solverCommand=missing"),
                Collections.<String, Object>emptyMap()
            );
        }
        return new RewriteProductionAdapterStatus(
            "SMT_Z3_EQUIVALENCE",
            true,
            "READY_EXTERNAL_SOLVER_NOT_EXECUTED",
            "STATIC_PROOF_OBLIGATIONS",
            "SOLVER_EXECUTION_REQUIRES_EXPLICIT_VALIDATION_RUN",
            "EXTERNAL_Z3_OPTIONAL_ADAPTER",
            "",
            Arrays.asList("solverCommand=" + command, "solverExecution=false"),
            Collections.<String, Object>emptyMap()
        );
    }

    private RewriteProductionAdapterStatus assessHetuExplain(String sqlText,
                                                             String tenantId,
                                                             String datasourceCode,
                                                             DataSourceTypeEnum datasourceType) {
        if (!properties.getHetuExplainCost().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "HETU_EXPLAIN_COST",
                "HETU_EXPLAIN_NOT_REQUESTED"
            );
        }
        HetuPlanAnalysisResult result = hetuPlanAnalysisClient.explain(
            sqlText,
            tenantId,
            datasourceCode,
            datasourceType == null ? DataSourceTypeEnum.HETU : datasourceType
        );
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
        attributes.put("planAnalysisStatus", result.getStatus() == null ? "" : result.getStatus().name());
        attributes.put("datasourceCode", result.getDatasourceCode());
        attributes.put("costMs", result.getCostMs());
        attributes.put("planTextAvailable", Boolean.valueOf(StringUtils.hasText(result.getPlanText())));
        if (result.getStatus() == PlanAnalysisStatus.SUCCESS) {
            return new RewriteProductionAdapterStatus(
                "HETU_EXPLAIN_COST",
                true,
                "REAL_HETU_EXPLAIN_AVAILABLE",
                "EXPLAIN_PLAN",
                "EXPLAIN_ONLY_NOT_RESULT_EQUIVALENCE_OR_PRODUCTION_SCALE",
                "HETU_EXPLAIN_JDBC_ADAPTER",
                "",
                result.getEvidence(),
                attributes
            );
        }
        return new RewriteProductionAdapterStatus(
            "HETU_EXPLAIN_COST",
            true,
            "HETU_EXPLAIN_UNAVAILABLE",
            "EXPLAIN_PLAN_ATTEMPTED",
            "EXPLAIN_COST_NOT_AVAILABLE_FOR_THIS_CONTEXT",
            "HETU_EXPLAIN_JDBC_ADAPTER",
            result.getFailureReason(),
            result.getEvidence(),
            attributes
        );
    }

    private RewriteProductionAdapterStatus assessStatisticsCost() {
        if (!properties.getStatisticsCost().isEnabled()) {
            return RewriteProductionAdapterStatus.disabled(
                "STATISTICS_COST_MODEL",
                "REAL_STATISTICS_COST_NOT_REQUESTED"
            );
        }
        String source = text(properties.getStatisticsCost().getStatisticsSource());
        if (!StringUtils.hasText(source)) {
            return new RewriteProductionAdapterStatus(
                "STATISTICS_COST_MODEL",
                true,
                "BLOCKED_STATISTICS_SOURCE_MISSING",
                "ABSTRACT_STATIC_COST",
                "REAL_TABLE_STATISTICS_NOT_AVAILABLE",
                "EXTERNAL_STATISTICS_ADAPTER",
                "statisticsSource 未配置",
                Collections.singletonList("statisticsSource=missing"),
                Collections.<String, Object>emptyMap()
            );
        }
        return new RewriteProductionAdapterStatus(
            "STATISTICS_COST_MODEL",
            true,
            "READY_EXTERNAL_STATISTICS_NOT_LOADED",
            "EXTERNAL_STATISTICS_CONFIG",
            "STATISTICS_LOAD_REQUIRES_EXPLICIT_VALIDATION_CONTEXT",
            "EXTERNAL_STATISTICS_ADAPTER",
            "",
            Collections.singletonList("statisticsSource=" + source),
            Collections.<String, Object>emptyMap()
        );
    }

    private CalciteRelNodeAttempt tryBuildCalciteRelNode(String sqlText) {
        CalciteRelNodeAttempt attempt = new CalciteRelNodeAttempt();
        if (!StringUtils.hasText(sqlText)) {
            attempt.failureReason = "SQL_TEXT_EMPTY";
            return attempt;
        }
        Planner planner = null;
        try {
            SqlParser.Config parserConfig =
                SqlParser.config().withConformance(SqlConformanceEnum.LENIENT);
            FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder()
                .parserConfig(parserConfig)
                .defaultSchema(Frameworks.createRootSchema(true))
                .build();
            planner = Frameworks.getPlanner(frameworkConfig);
            SqlNode parsed = planner.parse(sqlText);
            attempt.sqlNodeClass = parsed == null ? "" : parsed.getClass().getName();
            SqlNode validated = planner.validate(parsed);
            RelRoot relRoot = planner.rel(validated);
            attempt.relNode = relRoot == null ? null : relRoot.rel;
            return attempt;
        } catch (Exception ex) {
            attempt.failureReason = compact(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return attempt;
        } finally {
            if (planner != null) {
                planner.close();
            }
        }
    }

    private RewriteProductionAdapterStatus failedAdapter(String adapterName,
                                                         String status,
                                                         String claimBoundary,
                                                         String integrationMode,
                                                         Exception ex) {
        return new RewriteProductionAdapterStatus(
            adapterName,
            true,
            status,
            "ADAPTER_ATTEMPTED",
            claimBoundary,
            integrationMode,
            compact(ex.getMessage()),
            Collections.singletonList("failureType=" + ex.getClass().getSimpleName()),
            Collections.<String, Object>emptyMap()
        );
    }

    private String capabilityStatus(List<RewriteProductionAdapterStatus> adapters) {
        int enabledCount = 0;
        int liveCount = 0;
        for (RewriteProductionAdapterStatus adapter : adapters) {
            if (adapter.isEnabled()) {
                enabledCount++;
            }
            if (adapter.isLiveAvailable()) {
                liveCount++;
            }
        }
        if (enabledCount == 0) {
            return "DEFAULT_STATIC_CHAIN";
        }
        if (liveCount > 0) {
            return "LIVE_ADAPTER_AVAILABLE_WITH_GATES";
        }
        return "ADAPTERS_ENABLED_BUT_BLOCKED_OR_READY";
    }

    private String compact(String value) {
        String normalized = text(value);
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class CalciteRelNodeAttempt {
        private RelNode relNode;
        private String sqlNodeClass = "";
        private String failureReason = "";

        private List<String> evidence() {
            List<String> evidence = new ArrayList<String>();
            evidence.add("planner=Frameworks.getPlanner");
            evidence.add("sqlNodeClass=" + sqlNodeClass);
            if (StringUtils.hasText(failureReason)) {
                evidence.add("failureReason=" + failureReason);
            }
            return evidence;
        }

        private Map<String, Object> attributes() {
            LinkedHashMap<String, Object> attributes = new LinkedHashMap<String, Object>();
            attributes.put("sqlNodeClass", sqlNodeClass);
            attributes.put("schemaCatalogRequired", Boolean.TRUE);
            return attributes;
        }
    }
}
