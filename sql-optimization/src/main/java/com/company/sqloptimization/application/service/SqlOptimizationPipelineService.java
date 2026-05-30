package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.config.RewriteProductionGateProperties;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
import com.company.sqloptimization.domain.task.OptimizationTaskPhase;
import com.company.sqloptimization.domain.task.OptimizationTaskRisk;
import com.company.sqloptimization.domain.task.OptimizationTaskSuggestion;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.domain.rewrite.conformance.RewriteAlgorithmConformanceAnalyzer;
import com.company.sqloptimization.domain.rewrite.conformance.RewriteAlgorithmConformanceReport;
import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelectionReport;
import com.company.sqloptimization.domain.rewrite.cost.CostBasedRewriteSelector;
import com.company.sqloptimization.domain.rewrite.cost.CostSelectionStrategy;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrAssembler;
import com.company.sqloptimization.domain.rewrite.ir.RewriteCoreIrSnapshot;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionAnalyzer;
import com.company.sqloptimization.domain.rewrite.parser.ParserStackFusionReport;
import com.company.sqloptimization.domain.rewrite.production.RewriteProductionCapabilityAnalyzer;
import com.company.sqloptimization.domain.rewrite.production.RewriteProductionCapabilityReport;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDag;
import com.company.sqloptimization.domain.rewrite.qbdag.QueryBlockDagBuilder;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlan;
import com.company.sqloptimization.domain.rewrite.ra.RelationalRewritePlanBuilder;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationGenerator;
import com.company.sqloptimization.domain.rewrite.recommendation.RewriteRecommendationReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolutionReport;
import com.company.sqloptimization.domain.rewrite.rule.RuleConflictResolver;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceReport;
import com.company.sqloptimization.domain.rewrite.semantic.SemanticEquivalenceVerifier;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import io.trino.sql.parser.ParsingOptions;
import io.trino.sql.parser.SqlParser;
import io.trino.sql.tree.AstVisitor;
import io.trino.sql.tree.DereferenceExpression;
import io.trino.sql.tree.FunctionCall;
import io.trino.sql.tree.JoinCriteria;
import io.trino.sql.tree.Node;
import io.trino.sql.tree.QualifiedName;
import io.trino.sql.tree.Query;
import io.trino.sql.tree.QuerySpecification;
import io.trino.sql.tree.SingleColumn;
import io.trino.sql.tree.Table;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlWith;
import org.apache.calcite.sql.SqlWithItem;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlOptimizationPipelineService {

    private static final Pattern DATE_PREDICATE_PATTERN =
        Pattern.compile("[`\"]?([A-Z0-9_\\.]*?(DATE|TIME|DTE|DT|DAY)[A-Z0-9_\\.]*)[`\"]?\\s*(=|>=|<=|>|<|BETWEEN|IN)");
    private static final Pattern FUNCTION_WRAPPED_PREDICATE_PATTERN =
        Pattern.compile("(?is)\\b[A-Z_][A-Z0-9_]*\\s*\\([^()]*\\)\\s*(?:=|<>|!=|>=|<=|>|<)\\s*(?:DATE\\s+)?(?:'[^']*'|\\d+(?:\\.\\d+)?)");
    private static final Set<String> AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "COUNT", "SUM", "AVG", "MIN", "MAX", "APPROX_DISTINCT", "GROUP_CONCAT", "STRING_AGG", "LISTAGG"
        ));
    private static final Set<String> STRING_AGGREGATE_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("GROUP_CONCAT", "STRING_AGG", "LISTAGG"));
    private static final Set<String> STRING_CONCAT_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList("CONCAT", "CONCAT_WS"));
    private static final Set<String> STRING_SCALAR_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "LOWER", "UPPER", "SUBSTR", "SUBSTRING", "TRIM", "LTRIM", "RTRIM", "REPLACE", "REGEXP_REPLACE", "FORMAT"
        ));
    private static final Set<String> BUILT_IN_SCALAR_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "DATE_TRUNC", "CAST", "COALESCE", "IF", "NULLIF", "LOWER", "UPPER", "CONCAT", "CONCAT_WS",
            "SUBSTR", "SUBSTRING", "TRIM", "LTRIM", "RTRIM", "REPLACE", "REGEXP_REPLACE", "FORMAT"
        ));
    private static final Set<String> TIME_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "DATE_TRUNC", "TRUNC", "TO_DATE", "DATE_FORMAT", "YEAR", "MONTH", "DAY", "DAY_OF_MONTH",
            "HOUR", "MINUTE", "SECOND", "EXTRACT", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "LOCALTIME", "LOCALTIMESTAMP", "NOW"
        ));
    private static final Set<String> NON_DETERMINISTIC_FUNCTIONS =
        new LinkedHashSet<String>(Arrays.asList(
            "RAND", "RANDOM", "UUID", "NOW", "CURRENT_DATE", "CURRENT_TIME", "CURRENT_TIMESTAMP",
            "LOCALTIME", "LOCALTIMESTAMP", "CURRENT_USER", "SESSION_USER"
        ));
    private static final int PRODUCTION_TARGET_CONCURRENCY = 10000;
    private static final long PRODUCTION_TARGET_DAILY_QUERY_VOLUME = 10000000L;
    private static final long PRODUCTION_TARGET_DATASET_SIZE_BYTES = 30000000000000000L;
    private static final int PRODUCTION_TARGET_REPLAY_HOURS = 24;
    private static final List<String> PRODUCTION_SCALE_REQUIRED_EVIDENCE = Collections.unmodifiableList(
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
    private static final Pattern STRING_LIKE_PROJECTION_PATTERN =
        Pattern.compile("(?i)(^|[._])(?:name|title|desc|description|comment|remark|note|text|content|address|email|phone|status|type|code|label)$");
    private static final Pattern STRING_AGGREGATE_PATTERN =
        Pattern.compile("(?i)\\b(GROUP_CONCAT|STRING_AGG|LISTAGG)\\s*\\(");
    private static final Pattern HAVING_PATTERN = Pattern.compile("(?is)\\bHAVING\\b");
    private static final Pattern IN_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bIN\\s*\\(\\s*SELECT\\b");
    private static final Pattern EXISTS_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bEXISTS\\s*\\(\\s*SELECT\\b");
    private static final Pattern NOT_IN_SUBQUERY_PATTERN = Pattern.compile("(?is)\\bNOT\\s+IN\\s*\\(\\s*SELECT\\b");
    private static final Pattern LEFT_JOIN_NULL_PATTERN =
        Pattern.compile("(?is)\\bLEFT\\s+(?:OUTER\\s+)?JOIN\\b.*\\bIS\\s+NULL\\b");
    private static final Pattern CROSS_JOIN_PATTERN = Pattern.compile("(?is)\\bCROSS\\s+JOIN\\b");
    private static final Pattern CAST_COMPARISON_PATTERN =
        Pattern.compile("(?is)(CAST\\s*\\([^)]*\\)\\s*=|=\\s*CAST\\s*\\()");
    private static final Pattern STRING_NUMERIC_COMPARISON_PATTERN =
        Pattern.compile("(?is)\\b[A-Za-z_][A-Za-z0-9_\\.]*\\s*=\\s*'\\d+(?:\\.\\d+)?'");
    private static final Pattern PREFIX_LIKE_PATTERN = Pattern.compile("(?is)\\bLIKE\\s+'[^%_][^']*%'");
    private static final Pattern REGEXP_PATTERN =
        Pattern.compile("(?is)\\b(REGEXP_LIKE|RLIKE|REGEXP)\\b");
    private static final Pattern LONG_IN_LIST_PATTERN =
        Pattern.compile("(?is)\\bIN\\s*\\((?:\\s*[^,()]+\\s*,){8,}[^()]*\\)");
    private static final Pattern ROW_NUMBER_PATTERN =
        Pattern.compile("(?is)\\bROW_NUMBER\\s*\\(\\s*\\)\\s*OVER\\s*\\(");
    private static final Pattern UNION_DISTINCT_PATTERN = Pattern.compile("(?is)\\bUNION\\b(?!\\s+ALL\\b)");
    private static final Pattern INTERSECT_PATTERN = Pattern.compile("(?is)\\bINTERSECT\\b");
    private static final Pattern EXCEPT_PATTERN = Pattern.compile("(?is)\\b(EXCEPT|MINUS)\\b");
    private static final Pattern JSON_EXTRACT_PATTERN =
        Pattern.compile("(?is)\\b(JSON_EXTRACT|JSON_VALUE|GET_JSON_OBJECT|JSON_QUERY)\\s*\\(|->");
    private static final Pattern UNNEST_LATERAL_PATTERN = Pattern.compile("(?is)\\b(UNNEST|LATERAL)\\b");
    private static final Pattern NULL_SAFE_PATTERN =
        Pattern.compile("(?is)\\b(COALESCE|NVL)\\s*\\(|\\bIS\\s+NOT\\s+DISTINCT\\s+FROM\\b");
    private static final Pattern CASE_AGGREGATION_PATTERN =
        Pattern.compile("(?is)\\b(SUM|COUNT|MAX|MIN)\\s*\\(\\s*CASE\\s+WHEN\\b");
    private static final Pattern DATE_TRUNC_PATTERN =
        Pattern.compile("(?is)\\b(DATE_TRUNC|TRUNC|TO_DATE)\\s*\\(");
    private static final Pattern OFFSET_PATTERN = Pattern.compile("(?is)\\bOFFSET\\s+\\d+\\b");
    private static final Pattern SEMI_STRUCTURED_FLATTEN_PATTERN =
        Pattern.compile("(?is)\\b(FLATTEN|EXPLODE)\\s*\\(");
    private static final Pattern WITH_CLAUSE_PATTERN = Pattern.compile("(?is)^\\s*WITH\\b");
    private static final Pattern ORDER_BY_FUNCTION_PATTERN =
        Pattern.compile("(?is)\\bORDER\\s+BY\\b[^;]*(LOWER|UPPER|DATE_TRUNC|TRUNC|CAST|COALESCE|NVL)\\s*\\(");
    private static final Pattern NOT_EQUAL_PATTERN = Pattern.compile("(?is)(<>|!=|\\bNOT\\s+LIKE\\b)");
    private static final Pattern IS_NULL_PATTERN = Pattern.compile("(?is)\\bIS\\s+(?:NOT\\s+)?NULL\\b");
    private static final Pattern ARRAY_FUNCTION_PATTERN =
        Pattern.compile("(?is)\\b(ARRAY_CONTAINS|CONTAINS|ANY_MATCH|CARDINALITY|JSON_ARRAY_LENGTH)\\s*\\(");
    private static final Pattern RANGE_JOIN_PATTERN =
        Pattern.compile("(?is)\\bJOIN\\b.+\\bON\\b.+\\bBETWEEN\\b.+\\bAND\\b");
    private static final Pattern CASE_EXPRESSION_PATTERN = Pattern.compile("(?is)\\bCASE\\s+WHEN\\b");
    private static final Pattern COUNT_DISTINCT_PATTERN = Pattern.compile("(?is)\\bCOUNT\\s*\\(\\s*DISTINCT\\b");
    private static final Pattern APPROX_DISTINCT_PATTERN =
        Pattern.compile("(?is)\\b(APPROX_DISTINCT|HLL|HLL_UNION|HLL_CARDINALITY)\\s*\\(");
    private static final Pattern PERCENTILE_PATTERN =
        Pattern.compile("(?is)\\b(APPROX_PERCENTILE|PERCENTILE_CONT|PERCENTILE_DISC|QUANTILE)\\s*\\(");
    private static final Pattern SQL_LEVEL_FUNCTION_PATTERN =
        Pattern.compile(
            "(?is)\\b(CURRENT_DATE|CURRENT_TIME|CURRENT_TIMESTAMP|LOCALTIME|LOCALTIMESTAMP|NOW|RAND|RANDOM|UUID"
                + "|CURRENT_USER|SESSION_USER)\\b\\s*(?:\\(|\\b)"
        );
    private static final Pattern UNNEST_FUNCTION_PATTERN = Pattern.compile("(?i)\\bUNNEST\\s*\\(");
    private static final Pattern SIMPLE_BACKTICK_IDENTIFIER_PATTERN =
        Pattern.compile("[`\"]([A-Za-z_][A-Za-z0-9_]*)[`\"]");

    @Value("${sql-optimization.parser.strategy:APACHE_CALCITE}")
    private String parserStrategy = "APACHE_CALCITE";

    private final Map<SqlParserMode, SqlStructureParserAdapter> parserAdapters =
        new LinkedHashMap<SqlParserMode, SqlStructureParserAdapter>();
    private final RewriteProductionGateProperties rewriteProductionGateProperties;
    private final HetuPlanAnalysisClient hetuPlanAnalysisClient;

    public SqlOptimizationPipelineService() {
        this(new RewriteProductionGateProperties(), HetuPlanAnalysisClient.unavailable());
    }

    @Autowired
    public SqlOptimizationPipelineService(RewriteProductionGateProperties rewriteProductionGateProperties,
                                          HetuPlanAnalysisClient hetuPlanAnalysisClient) {
        this.rewriteProductionGateProperties = rewriteProductionGateProperties == null
            ? new RewriteProductionGateProperties()
            : rewriteProductionGateProperties;
        this.hetuPlanAnalysisClient = hetuPlanAnalysisClient == null
            ? HetuPlanAnalysisClient.unavailable()
            : hetuPlanAnalysisClient;
        registerParserAdapter(new ApacheCalciteParserAdapter());
    }

    private void registerParserAdapter(SqlStructureParserAdapter adapter) {
        parserAdapters.put(adapter.parserMode(), adapter);
    }

    public ParsedSqlProfile analyze(String sqlText, DataSourceTypeEnum datasourceType) {
        return analyze(sqlText, datasourceType, null);
    }

    public ParsedSqlProfile analyze(String sqlText, DataSourceTypeEnum datasourceType, SqlParserMode parserMode) {
        String normalizedSql = normalizeSql(sqlText);
        if (normalizedSql.trim().isEmpty()) {
            throw invalidTask(
                "真实优化流水线需要 SQL 文本，不能使用空载荷。",
                "请提交原始 SQL 文本，以便执行解析、改写和加速分析。"
            );
        }
        SqlParserMode resolvedMode = (parserMode == null ? resolveDefaultParserMode() : parserMode).structureMode();
        SqlStructureParserAdapter adapter = parserAdapters.get(resolvedMode);
        if (adapter == null) {
            adapter = parserAdapters.get(SqlParserMode.APACHE_CALCITE);
        }
        return adapter.analyze(normalizedSql, datasourceType);
    }

    private SqlParserMode resolveDefaultParserMode() {
        String strategy = parserStrategy == null ? "APACHE_CALCITE" : parserStrategy.trim().toUpperCase(Locale.ROOT);
        return SqlParserMode.resolveDefault(strategy);
    }

    private interface SqlStructureParserAdapter {
        SqlParserMode parserMode();

        ParsedSqlProfile analyze(String normalizedSql, DataSourceTypeEnum datasourceType);
    }

    private final class ApacheCalciteParserAdapter implements SqlStructureParserAdapter {
        @Override
        public SqlParserMode parserMode() {
            return SqlParserMode.APACHE_CALCITE;
        }

        @Override
        public ParsedSqlProfile analyze(String normalizedSql, DataSourceTypeEnum datasourceType) {
            return analyzeWithApacheCalciteParser(normalizedSql, datasourceType);
        }
    }

    private ParsedSqlProfile analyzeWithTrinoParser(String normalizedSql, DataSourceTypeEnum datasourceType) {
        io.trino.sql.tree.Statement statement;
        try {
            statement = new SqlParser().createStatement(normalizedSql, new ParsingOptions());
        } catch (RuntimeException ex) {
            throw parserFailure(
                "Trino 解析器无法为提交语句构建 AST。",
                "请提交单条受支持的 SELECT/WITH 查询，或让该数据源使用 Apache Calcite 解析策略。",
                Collections.singletonList(
                    new OptimizationTaskRisk(
                        "HIGH",
                        "UNSUPPORTED_TRINO_DIALECT",
                        "提交的 SQL 无法由 Trino 解析适配器解析。",
                        "请使用受支持的 Trino SELECT 查询，或改用 Apache Calcite 策略。"
                    )
                ),
                ex,
                normalizedSql
            );
        }
        if (!(statement instanceof Query)) {
            throw invalidTask(
                "Trino 解析适配器当前仅支持 SELECT/WITH 语句。",
                "请提交面向读取的 SELECT 语句用于查询意图分析。"
            );
        }
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql);
        profile.parserEngine = "TRINO";
        profile.markAdvancedProfilePartial();
        new TrinoProfileVisitor().process(statement, profile);
        profile.recordSqlLevelFunctions(normalizedSql);
        finalizeWarnings(profile);
        return profile;
    }

    private ParsedSqlProfile analyzeWithApacheCalciteParser(String normalizedSql, DataSourceTypeEnum datasourceType) {
        SqlNode statement;
        try {
            statement = parseApacheCalciteStatement(normalizedSql);
        } catch (Exception ex) {
            throw parserFailure(
                "Apache Calcite 解析器无法为提交语句构建 AST。",
                "请提交单条受支持的 SELECT/WITH 查询，或为该数据源补充 Calcite 方言扩展。",
                Collections.singletonList(
                    new OptimizationTaskRisk(
                        "HIGH",
                        "UNSUPPORTED_CALCITE_DIALECT",
                        "提交的 SQL 无法由 Apache Calcite 解析适配器解析。",
                        "请使用受支持的 Calcite SELECT 查询，或补充 Calcite Parser.Config / 操作符扩展。"
                    )
                ),
                ex,
                normalizedSql
            );
        }
        if (!isCalciteSelectLike(statement)) {
            throw invalidTask(
                "Apache Calcite 解析适配器当前仅支持 SELECT/WITH 语句。",
                "请提交面向读取的 SELECT 语句用于查询意图分析。"
            );
        }
        ParsedSqlProfile profile = new ParsedSqlProfile(normalizedSql);
        profile.parserEngine = "APACHE_CALCITE";
        profile.markAdvancedProfileAvailable();
        new ApacheCalciteProfileCollector().collect(statement, profile, true);
        augmentCalciteProfileFromSqlText(normalizedSql, profile);
        profile.rewriteOutcome = applyCalciteRewriteRules(statement);
        profile.recordSqlLevelFunctions(normalizedSql);
        finalizeWarnings(profile);
        return profile;
    }

    private SqlNode parseApacheCalciteStatement(String normalizedSql) throws Exception {
        try {
            return org.apache.calcite.sql.parser.SqlParser.create(
                normalizedSql,
                apacheCalciteParserConfig()
            ).parseStmt();
        } catch (Exception ex) {
            String compatibleSql = calciteCompatibleSql(normalizedSql);
            if (compatibleSql.equals(normalizedSql)) {
                throw ex;
            }
            return org.apache.calcite.sql.parser.SqlParser.create(
                compatibleSql,
                apacheCalciteParserConfig()
            ).parseStmt();
        }
    }

    private org.apache.calcite.sql.parser.SqlParser.Config apacheCalciteParserConfig() {
        return org.apache.calcite.sql.parser.SqlParser.config()
            .withConformance(SqlConformanceEnum.LENIENT)
            .withUnquotedCasing(Casing.UNCHANGED);
    }

    private String calciteCompatibleSql(String normalizedSql) {
        if (!StringUtils.hasText(normalizedSql)) {
            return "";
        }
        return UNNEST_FUNCTION_PATTERN.matcher(normalizedSql).replaceAll("EXPLODE(");
    }

    private boolean isCalciteSelectLike(SqlNode node) {
        if (node instanceof SqlSelect || node instanceof SqlWith || node instanceof SqlOrderBy) {
            return true;
        }
        if (node instanceof SqlCall) {
            SqlKind kind = node.getKind();
            return kind == SqlKind.UNION || kind == SqlKind.INTERSECT || kind == SqlKind.EXCEPT;
        }
        return false;
    }

    public OptimizationTaskSuggestion buildParseSuggestion(ParsedSqlProfile profile) {
        List<OptimizationTaskArtifact> artifacts = new ArrayList<OptimizationTaskArtifact>();
        artifacts.add(new OptimizationTaskArtifact("AST_PROFILE", "astProfile", JsonUtils.toJson(profile.toAstProfile())));
        artifacts.add(new OptimizationTaskArtifact("TABLE_LINEAGE", "tables", JsonUtils.toJson(profile.tables)));
        artifacts.add(new OptimizationTaskArtifact("HOTSPOT_FLAGS", "warnings", JsonUtils.toJson(profile.warnings)));

        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "REWRITE_READINESS",
                Integer.valueOf(clamp(40 + profile.predicateCount * 5 + profile.joinCount * 4, 20, 85)),
                "AST 分析会隔离表、谓词和聚合信息，使后续改写规则保持确定性。"
            ),
            new OptimizationTaskBenefit(
                "ACCELERATION_SIGNAL",
                Integer.valueOf(clamp(35 + profile.aggregateFunctions.size() * 10 + profile.datePredicateColumns.size() * 8, 15, 80)),
                "已解析形态会暴露分区、预计算和替换候选信号。"
            )
        );
        List<OptimizationTaskCost> costs = Collections.singletonList(
            new OptimizationTaskCost(
                "PARSER_OVERHEAD",
                "LOW",
                "解析器在进程内运行，并且只为离线优化任务物化语句元数据。"
            )
        );
        List<OptimizationTaskRisk> risks = buildShapeRisks(profile);
        return new OptimizationTaskSuggestion(
            buildParseSummary(profile),
            buildParseRecommendation(profile),
            Integer.valueOf(calculateParseConfidence(profile)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    public OptimizationTaskSuggestion buildRewriteSuggestion(ParsedSqlProfile profile) {
        RewriteOutcome outcome = profile == null ? RewriteOutcome.empty() : profile.getRewriteOutcome();
        L2DynamicSnapshotAggregateMvCandidateGenerator.RewriteCandidate dynamicSnapshotRewrite =
            profile == null ? null : L2DynamicSnapshotAggregateMvCandidateGenerator.rewriteCandidate(
                profile.getNormalizedSql(),
                profile
            );
        List<String> appliedRules = new ArrayList<String>(outcome.appliedRules);
        String candidateSql = outcome.rewrittenSql;
        if (dynamicSnapshotRewrite != null && StringUtils.hasText(dynamicSnapshotRewrite.getRewriteSql())) {
            candidateSql = dynamicSnapshotRewrite.getRewriteSql();
            if (!appliedRules.contains(L2DynamicSnapshotAggregateMvCandidateGenerator.RULE)) {
                appliedRules.add(L2DynamicSnapshotAggregateMvCandidateGenerator.RULE);
            }
        }
        RewriteCoreIrSnapshot coreIrSnapshot = profile == null ? null : buildRewriteCoreIr(profile);
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>(buildShapeRisks(profile));
        if (appliedRules.isEmpty()) {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "NO_SAFE_AUTOMATIC_REWRITE",
                    "提交语句未匹配任何保守 AST 改写规则。",
                    "请使用解析制品和计划追踪人工评审投影、谓词和引擎专属调优。"
                )
            );
        } else {
            risks.add(
                new OptimizationTaskRisk(
                    "MEDIUM",
                    "SEMANTIC_VALIDATION_REQUIRED",
                    "即使是安全的语法改写，批准前仍需要做结果集差异校验。",
                    "请在有代表性的样本数据集上对比改写 SQL 与原始语句。"
                )
            );
        }
        List<OptimizationTaskArtifact> artifacts = new ArrayList<OptimizationTaskArtifact>();
        artifacts.add(new OptimizationTaskArtifact("REWRITTEN_SQL", "candidateSql", candidateSql));
        artifacts.add(new OptimizationTaskArtifact("REWRITE_RULE_TRACE", "appliedRules", JsonUtils.toJson(appliedRules)));
        if (dynamicSnapshotRewrite != null) {
            artifacts.add(new OptimizationTaskArtifact(
                "DYNAMIC_REWRITE_EVIDENCE",
                "dynamicSnapshotRewriteEvidence",
                JsonUtils.toJson(dynamicSnapshotRewrite.getEvidence())
            ));
            artifacts.add(new OptimizationTaskArtifact(
                "DYNAMIC_REWRITE_VALIDATION_METHODS",
                "dynamicSnapshotValidationMethods",
                JsonUtils.toJson(dynamicSnapshotRewrite.getValidationMethods())
            ));
        }
        artifacts.add(new OptimizationTaskArtifact("AST_PROFILE", "astProfile", JsonUtils.toJson(profile.toAstProfile())));
        if (coreIrSnapshot != null) {
            artifacts.add(new OptimizationTaskArtifact(
                "REWRITE_RECOMMENDATION_REPORT",
                "recommendationReport",
                JsonUtils.toJson(coreIrSnapshot.getRewriteRecommendationReport())
            ));
            if (coreIrSnapshot.getRewriteRecommendationReport().firstRecommendation() != null) {
                artifacts.add(new OptimizationTaskArtifact(
                    "REWRITE_RECOMMENDATION_SELECTED",
                    "selectedRecommendation",
                    JsonUtils.toJson(coreIrSnapshot.getRewriteRecommendationReport().firstRecommendation())
                ));
            }
            artifacts.add(new OptimizationTaskArtifact(
                "REWRITE_ALGORITHM_CONFORMANCE",
                "conformanceReport",
                JsonUtils.toJson(coreIrSnapshot.getRewriteAlgorithmConformanceReport())
            ));
        }
        artifacts.add(new OptimizationTaskArtifact(
            "REWRITE_PRODUCTION_CAPABILITY_REPORT",
            "productionCapabilityReport",
            JsonUtils.toJson(assessRewriteProductionCapabilities(profile).toMap())
        ));
        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "PLAN_SIMPLIFICATION",
                Integer.valueOf(clamp(20 + appliedRules.size() * 12, 10, 70)),
                "移除重复谓词、分组键或排序项后，逻辑计划更小且更容易校验。"
            ),
            new OptimizationTaskBenefit(
                "RULE_TRACEABILITY",
                Integer.valueOf(clamp(30 + appliedRules.size() * 10, 15, 75)),
                "每次改写都会记录为确定性规则追踪，而不是不透明的占位摘要。"
            )
        );
        List<OptimizationTaskCost> costs = Arrays.asList(
            new OptimizationTaskCost(
                "VALIDATION",
                appliedRules.isEmpty() ? "LOW" : "MEDIUM",
                "后续批准或应用前，候选 SQL 应与原始语句进行差异校验。"
            ),
            new OptimizationTaskCost(
                "RULE_COVERAGE",
                "LOW",
                "当前改写规则保持保守策略，不尝试依赖 schema 的投影展开。"
            )
        );
        String summary = appliedRules.isEmpty()
            ? "语句解析成功，但未找到保守的自动改写候选。"
            : rewriteSummary(appliedRules);
        String recommendation = appliedRules.isEmpty()
            ? "请使用解析制品人工评审投影宽度、过滤位置和引擎专属提示。"
            : rewriteRecommendation();
        return new OptimizationTaskSuggestion(
            summary,
            recommendation,
            Integer.valueOf(calculateRewriteConfidence(profile, appliedRules)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    public OptimizationTaskSuggestion buildAccelerationSuggestion(ParsedSqlProfile profile,
                                                                 List<AccelerationSuggestionType> requestedTypes) {
        return buildAccelerationSuggestion(profile, requestedTypes, null, null, null, null);
    }

    public OptimizationTaskSuggestion buildAccelerationSuggestion(ParsedSqlProfile profile,
                                                                 List<AccelerationSuggestionType> requestedTypes,
                                                                 DataSourceTypeEnum targetEngine,
                                                                 String targetDatasource,
                                                                 String sqlFingerprint,
                                                                 String reportCode) {
        LinkedHashMap<AccelerationSuggestionType, String> reasons = deriveAccelerationReasons(profile);
        LinkedHashMap<AccelerationSuggestionType, String> filteredReasons = filterRequestedTypes(reasons, requestedTypes);
        if (filteredReasons.isEmpty()) {
            filteredReasons.put(
                AccelerationSuggestionType.REPLACE,
                "未检测到强物理设计信号，安全默认方案是用已治理服务视图替换宽原始查询。"
            );
        }
        List<OptimizationTaskArtifact> artifacts = new ArrayList<OptimizationTaskArtifact>();
        artifacts.add(new OptimizationTaskArtifact("ACCELERATION_PLAN", "recommendedTypes", JsonUtils.toJson(filteredReasons)));
        artifacts.add(new OptimizationTaskArtifact("SIGNAL_PROFILE", "signalProfile", JsonUtils.toJson(profile.toAccelerationSignalProfile())));
        artifacts.add(new OptimizationTaskArtifact("TABLE_LINEAGE", "tables", JsonUtils.toJson(profile.tables)));
        if (filteredReasons.containsKey(AccelerationSuggestionType.PRECOMPUTE)) {
            HetuPlanAnalysisResult explainResult = explainForAccelerationArtifact(
                profile,
                targetEngine,
                targetDatasource
            );
            Map<String, Object> accelerationArtifact = L2AccelerationArtifactBuilder.buildForPrecomputeCandidate(
                new L2AccelerationArtifactBuilder.AccelerationRecommendationInput(
                    profile.getNormalizedSql(),
                    targetEngine == null ? null : targetEngine.name(),
                    targetDatasource,
                    sqlFingerprint,
                    reportCode,
                    null,
                    null
                ),
                profile,
                explainResult
            );
            if (accelerationArtifact != null) {
                artifacts.add(new OptimizationTaskArtifact(
                    "ACCELERATION_ARTIFACT",
                    "accelerationArtifact",
                    JsonUtils.toJson(accelerationArtifact)
                ));
            }
        }
        List<OptimizationTaskBenefit> benefits = Arrays.asList(
            new OptimizationTaskBenefit(
                "LATENCY",
                Integer.valueOf(clamp(25 + filteredReasons.size() * 12 + profile.aggregateFunctions.size() * 6, 20, 85)),
                "派生计划面向主导解析热点和重复重扫描的查询形态。"
            ),
            new OptimizationTaskBenefit(
                "SCANNED_ROWS",
                Integer.valueOf(clamp(20 + profile.datePredicateColumns.size() * 15 + profile.joinCount * 8, 15, 88)),
                "分区、分桶或替换提示可减少重复执行触达的热点数据量。"
            )
        );
        List<OptimizationTaskCost> costs = Arrays.asList(
            new OptimizationTaskCost(
                "STORAGE_OR_REFRESH",
                filteredReasons.containsKey(AccelerationSuggestionType.PRECOMPUTE) ? "HIGH" : "MEDIUM",
                "预计算与替换策略会增加存储或刷新开销，必须由重复查询需求证明其合理性。"
            ),
            new OptimizationTaskCost(
                "GOVERNANCE_FOLLOW_UP",
                "MEDIUM",
                "加速仍是受治理对象，后续仍需要激活、校验和暂停证据。"
            )
        );
        List<OptimizationTaskRisk> risks = new ArrayList<OptimizationTaskRisk>(buildShapeRisks(profile));
        risks.add(
            new OptimizationTaskRisk(
                "MEDIUM",
                "FRESHNESS_AND_ROLLBACK",
                "如果未经过治理就应用，加速计划可能用新鲜度或运维简单性换取速度。",
                "后续任何生效步骤前，都要明确保留激活、校验和暂停证据。"
            )
        );
        String summary = "已从真实 SQL 形态派生 " + filteredReasons.size() + " 条加速推荐。";
        String recommendation = "请先处理信号最强的加速类型，再在后续应用流程前校验收益和新鲜度。";
        return new OptimizationTaskSuggestion(
            summary,
            recommendation,
            Integer.valueOf(calculateAccelerationConfidence(profile, filteredReasons)),
            artifacts,
            benefits,
            costs,
            risks
        );
    }

    private HetuPlanAnalysisResult explainForAccelerationArtifact(ParsedSqlProfile profile,
                                                                  DataSourceTypeEnum targetEngine,
                                                                  String targetDatasource) {
        if (profile == null) {
            return HetuPlanAnalysisResult.skipped(
                targetDatasource,
                "PARSE_PROFILE_REQUIRED",
                Collections.singletonList("profile=missing")
            );
        }
        if (targetEngine != null && targetEngine != DataSourceTypeEnum.HETU && targetEngine != DataSourceTypeEnum.AUTO) {
            return HetuPlanAnalysisResult.skipped(
                targetDatasource,
                "EXPLAIN_SKIPPED_FOR_NON_HETU_TARGET",
                Collections.singletonList("targetEngine=" + targetEngine.name())
            );
        }
        try {
            return hetuPlanAnalysisClient.explain(
                profile.getNormalizedSql(),
                null,
                targetDatasource,
                targetEngine == null ? DataSourceTypeEnum.HETU : targetEngine
            );
        } catch (RuntimeException ex) {
            return HetuPlanAnalysisResult.failed(
                targetDatasource,
                ex.getMessage(),
                0L,
                Collections.singletonList("explain=failed")
            );
        }
    }

    public List<String> deriveRewriteCandidateRules(ParsedSqlProfile profile) {
        if (profile == null) {
            return Collections.emptyList();
        }
        return profile.getRewriteOutcome().appliedRules;
    }

    public RewriteCoreIrSnapshot buildRewriteCoreIr(ParsedSqlProfile profile) {
        if (profile == null) {
            throw invalidTask(
                "构建改写核心 IR 需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成 L1-L5 改写 IR 骨架。"
            );
        }
        return new RewriteCoreIrAssembler().assemble(
            profile.getNormalizedSql(),
            profile.getParserEngine(),
            profile.toAdvancedStructureProfile()
        );
    }

    public QueryBlockDag buildQueryBlockDag(ParsedSqlProfile profile) {
        if (profile == null) {
            throw invalidTask(
                "构建查询块 DAG 需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成第一阶段 QBDAG。"
            );
        }
        return new QueryBlockDagBuilder().build(
            profile.getNormalizedSql(),
            profile.toAdvancedStructureProfile()
        );
    }

    public RelationalRewritePlan buildRelationalRewritePlan(ParsedSqlProfile profile) {
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        return new RelationalRewritePlanBuilder().build(queryBlockDag);
    }

    public SemanticEquivalenceReport verifySemanticEquivalence(ParsedSqlProfile profile) {
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        return new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
    }

    public CostBasedRewriteSelectionReport selectCostBasedRewrite(ParsedSqlProfile profile) {
        return selectCostBasedRewrite(profile, CostSelectionStrategy.DEFAULT_WEIGHTED);
    }

    public CostBasedRewriteSelectionReport selectCostBasedRewrite(ParsedSqlProfile profile,
                                                                  CostSelectionStrategy strategy) {
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticReport = new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
        return new CostBasedRewriteSelector().select(queryBlockDag, plan, semanticReport, strategy);
    }

    public RuleConflictResolutionReport resolveRewriteRuleConflicts(ParsedSqlProfile profile) {
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticReport = new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
        CostBasedRewriteSelectionReport costReport = new CostBasedRewriteSelector().select(
            queryBlockDag,
            plan,
            semanticReport
        );
        return new RuleConflictResolver().resolve(plan, costReport);
    }

    public RewriteProductionCapabilityReport assessRewriteProductionCapabilities(ParsedSqlProfile profile) {
        return assessRewriteProductionCapabilities(profile, null, null, null);
    }

    public RewriteProductionCapabilityReport assessRewriteProductionCapabilities(ParsedSqlProfile profile,
                                                                                 String tenantId,
                                                                                 String datasourceCode,
                                                                                 DataSourceTypeEnum datasourceType) {
        if (profile == null) {
            throw invalidTask(
                "评估改写生产适配能力需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成生产化适配层状态报告。"
            );
        }
        return new RewriteProductionCapabilityAnalyzer(
            rewriteProductionGateProperties,
            hetuPlanAnalysisClient
        ).assess(profile.getNormalizedSql(), tenantId, datasourceCode, datasourceType);
    }

    public ParserStackFusionReport buildParserStackFusionReport(ParsedSqlProfile profile) {
        if (profile == null) {
            throw invalidTask(
                "构建双解析栈融合报告需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成 Calcite 解析融合与 Hetu 适配报告。"
            );
        }
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticReport = new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
        CostBasedRewriteSelectionReport costReport = new CostBasedRewriteSelector().select(
            queryBlockDag,
            plan,
            semanticReport
        );
        RuleConflictResolutionReport ruleReport = new RuleConflictResolver().resolve(plan, costReport);
        return new ParserStackFusionAnalyzer().analyze(
            profile.getNormalizedSql(),
            profile.getParserEngine(),
            profile.toAdvancedStructureProfile(),
            queryBlockDag,
            plan,
            costReport,
            ruleReport
        );
    }

    public RewriteRecommendationReport generateRewriteRecommendations(ParsedSqlProfile profile) {
        if (profile == null) {
            throw invalidTask(
                "生成改写推荐最终输出需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成排序后的 RewriteRecommendation 报告。"
            );
        }
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticReport = new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
        CostBasedRewriteSelectionReport costReport = new CostBasedRewriteSelector().select(
            queryBlockDag,
            plan,
            semanticReport
        );
        RuleConflictResolutionReport ruleReport = new RuleConflictResolver().resolve(plan, costReport);
        ParserStackFusionReport parserReport = new ParserStackFusionAnalyzer().analyze(
            profile.getNormalizedSql(),
            profile.getParserEngine(),
            profile.toAdvancedStructureProfile(),
            queryBlockDag,
            plan,
            costReport,
            ruleReport
        );
        return new RewriteRecommendationGenerator().generate(
            profile.getNormalizedSql(),
            queryBlockDag,
            plan,
            semanticReport,
            costReport,
            ruleReport,
            parserReport
        );
    }

    public RewriteAlgorithmConformanceReport assessRewriteAlgorithmConformance(ParsedSqlProfile profile) {
        if (profile == null) {
            throw invalidTask(
                "评估改写算法链路一致性需要有效 SQL 解析结果。",
                "请先完成 SQL 结构解析，再生成算法链路一致性报告。"
            );
        }
        QueryBlockDag queryBlockDag = buildQueryBlockDag(profile);
        RelationalRewritePlan plan = new RelationalRewritePlanBuilder().build(queryBlockDag);
        SemanticEquivalenceReport semanticReport = new SemanticEquivalenceVerifier().verify(queryBlockDag, plan);
        CostBasedRewriteSelectionReport costReport = new CostBasedRewriteSelector().select(
            queryBlockDag,
            plan,
            semanticReport
        );
        RuleConflictResolutionReport ruleReport = new RuleConflictResolver().resolve(plan, costReport);
        ParserStackFusionReport parserReport = new ParserStackFusionAnalyzer().analyze(
            profile.getNormalizedSql(),
            profile.getParserEngine(),
            profile.toAdvancedStructureProfile(),
            queryBlockDag,
            plan,
            costReport,
            ruleReport
        );
        RewriteRecommendationReport recommendationReport = new RewriteRecommendationGenerator().generate(
            profile.getNormalizedSql(),
            queryBlockDag,
            plan,
            semanticReport,
            costReport,
            ruleReport,
            parserReport
        );
        return new RewriteAlgorithmConformanceAnalyzer().analyze(
            profile.getNormalizedSql(),
            queryBlockDag,
            plan,
            semanticReport,
            costReport,
            ruleReport,
            parserReport,
            recommendationReport
        );
    }

    public RecommendationRuleOutputModel buildRecommendationRuleOutputModel(ParsedSqlProfile profile) {
        if (profile == null) {
            return RecommendationRuleOutputModel.empty();
        }
        List<Map<String, Object>> ruleChain = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> unappliedRules = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> preconditions = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> semanticRisks = new ArrayList<Map<String, Object>>();

        RewriteOutcome outcome = profile.getRewriteOutcome();
        for (String appliedRule : outcome.appliedRules) {
            ruleChain.add(ruleEntry(
                "L0",
                normalizeRecommendationRule(appliedRule),
                "APPLIED_TO_CANDIDATE_SQL",
                "STATIC_PARSE",
                Boolean.TRUE,
                l0RuleDescription(appliedRule)
            ));
        }
        if (profile.isSelectStar()) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "SELECT_STAR_EXPANSION",
                "COLUMN_METADATA_REQUIRED",
                "展开 SELECT * 前必须具备列元数据与投影归属证据。",
                "如果隐藏列被遗漏或重排，投影变更可能影响下游消费者。",
                selectStarEvidence(profile)
            );
        }
        if (profile.getOrPredicateCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "OR_TO_UNION_ALL",
                "PREDICATE_EXCLUSIVITY_OR_DEDUP_REQUIRED",
                "将 OR 改写为 UNION/UNION ALL 前必须具备谓词互斥性或去重策略。",
                "如果 OR 分支不互斥，行可能重复或丢失。"
            );
        }
        if (profile.getFunctionWrappedPredicateCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "FUNCTION_PREDICATE_TO_RANGE",
                "COLUMN_TYPE_TIMEZONE_REQUIRED",
                "范围转换前必须具备列类型、时区和边界精度证据。",
                "时间边界或精度差异可能改变结果集。",
                functionPredicateEvidence(profile)
            );
        }
        if (profile.getScalarSubqueryCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "SCALAR_SUBQUERY_TO_JOIN",
                "UNIQUENESS_PROOF_REQUIRED",
                "将标量子查询改写为 join 前必须具备唯一性证明。",
                "当标量子查询不唯一时，join 改写可能放大行数。"
            );
        }
        if (profile.getRepeatedSubqueryCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "REPEATED_SUBQUERY_TO_CTE",
                "SUBQUERY_SIDE_EFFECT_FREE_REQUIRED",
                "重复子查询必须无副作用，并且需要理解引擎的 CTE 行为。",
                "部分引擎会内联 CTE 或采用不同优化方式，可能改变性能且不保证收益。",
                repeatedSubqueryEvidence(profile)
            );
        }
        if (profile.getNotExistsCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NOT_EXISTS_TO_ANTI_JOIN",
                "NULL_SEMANTICS_PROOF_REQUIRED",
                "将 NOT EXISTS 改为 anti join 前必须证明 NULL 行为。",
                "NULL 语义可能改变反连接结果。"
            );
        }
        if (profile.getLeadingWildcardLikeCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LEADING_LIKE_REVIEW",
                "SEARCH_INDEX_OR_TEXT_CAPABILITY_REQUIRED",
                "修改前导通配符谓词前，需要搜索索引或文本搜索能力证据。",
                "文本匹配语义和排序规则可能发生变化。"
            );
        }
        if (profile.getRandomOrderCount() > 0) {
            addUnappliedRule(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ORDER_RANDOM_REVIEW",
                "SAMPLING_INTENT_REQUIRED",
                "替换随机排序前必须确认业务抽样意图。",
                "随机性和可重复性要求属于语义约束，不是解析器可直接推导的结果。"
            );
        }

        addExtendedStructuralRuleCandidates(profile, unappliedRules, preconditions, semanticRisks);
        addL2RuleCandidates(profile, ruleChain, preconditions, semanticRisks);
        addExtendedPhysicalRuleCandidates(profile, ruleChain, preconditions, semanticRisks);

        boolean manualReviewRequired = !unappliedRules.isEmpty() || containsManualReviewRule(ruleChain);
        String validationMethod = manualReviewRequired ? "RESULT_DIFF_THEN_MANUAL_REVIEW" : "RESULT_DIFF_REQUIRED";
        RewriteProductionCapabilityReport productionCapabilityReport = assessRewriteProductionCapabilities(profile);
        return new RecommendationRuleOutputModel(
            ruleChain,
            unappliedRules,
            preconditions,
            semanticRisks,
            expectedBenefit(profile, outcome, productionCapabilityReport),
            estimatedCost(profile, manualReviewRequired, productionCapabilityReport),
            Integer.valueOf(calculateRecommendationConfidence(profile, outcome, unappliedRules)),
            validationMethod,
            false,
            manualReviewRequired
        );
    }

    private void addExtendedStructuralRuleCandidates(ParsedSqlProfile profile,
                                                     List<Map<String, Object>> unappliedRules,
                                                     List<Map<String, Object>> preconditions,
                                                     List<Map<String, Object>> semanticRisks) {
        String sql = profile.getNormalizedSql();
        if (profile.isDistinctPresent()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "DISTINCT_DEDUP_REVIEW",
                "DUPLICATE_INTENT_REQUIRED",
                "DISTINCT 需要确认是否为业务去重还是掩盖 join 放大。",
                "去重可能隐藏数据质量问题或改变重复行语义。"
            );
        }
        if (profile.isGroupByWithoutAggregate()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "GROUP_BY_TO_DISTINCT",
                "DEDUP_SEMANTICS_REQUIRED",
                "无聚合 GROUP BY 可试算为 DISTINCT，但需确认排序和重复语义。",
                "GROUP BY 与 DISTINCT 在部分引擎的执行计划和 NULL 表现需复核。"
            );
        }
        if (matches(HAVING_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "HAVING_TO_WHERE_PUSHDOWN",
                "AGGREGATE_DEPENDENCY_PROOF_REQUIRED",
                "HAVING 中不依赖聚合的过滤可下推到 WHERE。",
                "误下推聚合过滤会改变分组结果。"
            );
        }
        if (matches(IN_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "IN_SUBQUERY_TO_SEMI_JOIN",
                "NULL_AND_DUPLICATE_SEMANTICS_REQUIRED",
                "IN 子查询可评审为 semi join，以便优化器使用 join 侧过滤。",
                "子查询 NULL、重复值和关联条件可能改变匹配语义。"
            );
        }
        if (matches(EXISTS_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "EXISTS_TO_SEMI_JOIN",
                "CORRELATION_SCOPE_REQUIRED",
                "EXISTS 子查询可评审为 semi join 或动态过滤候选。",
                "关联范围不清会导致行数放大或漏匹配。"
            );
        }
        if (matches(NOT_IN_SUBQUERY_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NOT_IN_TO_ANTI_JOIN",
                "NULL_SEMANTICS_PROOF_REQUIRED",
                "NOT IN 子查询可评审为 null-safe anti join。",
                "NOT IN 遇到 NULL 时语义敏感，不能自动改写。"
            );
        }
        if (matches(LEFT_JOIN_NULL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LEFT_JOIN_NULL_TO_ANTI_JOIN",
                "JOIN_KEY_NULLABILITY_REQUIRED",
                "LEFT JOIN ... IS NULL 可评审为 anti join。",
                "右表重复键或 NULL 过滤位置可能改变反连接结果。"
            );
        }
        if (matches(CROSS_JOIN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CROSS_JOIN_GUARD",
                "CARTESIAN_INTENT_REQUIRED",
                "CROSS JOIN 需要确认是否为真实笛卡尔意图或遗漏 join 条件。",
                "误保留笛卡尔积会造成行数爆炸。"
            );
        }
        if (matches(CAST_COMPARISON_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CAST_JOIN_KEY_NORMALIZE",
                "COLUMN_TYPE_AND_LOSSLESS_CAST_REQUIRED",
                "比较或 join key 上的 CAST 可评审为类型归一后的列比较。",
                "非无损转换或精度差异会改变匹配结果。"
            );
        }
        if (matches(STRING_NUMERIC_COMPARISON_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "IMPLICIT_TYPE_CAST_REVIEW",
                "COLUMN_TYPE_REQUIRED",
                "疑似字符串数字比较需要显式化类型或绑定参数类型。",
                "隐式类型转换在不同引擎之间可能导致结果或性能差异。"
            );
        }
        if (matches(PREFIX_LIKE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LIKE_PREFIX_RANGE_REVIEW",
                "COLLATION_AND_ESCAPE_RULE_REQUIRED",
                "前缀 LIKE 可评审为范围谓词或前缀索引候选。",
                "排序规则、转义字符和大小写规则会影响范围边界。"
            );
        }
        if (matches(REGEXP_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "REGEXP_FILTER_TO_SEARCH_INDEX",
                "TEXT_INDEX_CAPABILITY_REQUIRED",
                "正则过滤可推荐搜索索引或预计算标签列。",
                "正则语义和分词规则依赖引擎，不能静态保证等价。"
            );
        }
        if (matches(LONG_IN_LIST_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LONG_IN_LIST_TO_TEMP_TABLE",
                "VALUE_SET_CARDINALITY_REQUIRED",
                "长 IN 列表可试算为临时值表或半连接。",
                "值集去重、类型绑定和权限边界需要复核。"
            );
        }
        if (matches(ROW_NUMBER_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "WINDOW_TOPN_REWRITE",
                "PARTITION_ORDER_DETERMINISM_REQUIRED",
                "ROW_NUMBER Top-N 形态可评审为分组 Top-N 或服务化预计算。",
                "排序不稳定或并列值处理会改变被保留的行。"
            );
        }
        if (matches(UNION_DISTINCT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UNION_DEDUP_REVIEW",
                "DUPLICATE_POLICY_REQUIRED",
                "UNION 可评审是否改为 UNION ALL 后单独去重或保留重复。",
                "重复行策略属于业务语义，不能自动决定。"
            );
        }
        if (matches(INTERSECT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "INTERSECT_TO_SEMI_JOIN",
                "SET_SEMANTICS_REQUIRED",
                "INTERSECT 可评审为 semi join 或存在性过滤。",
                "集合去重和 NULL 处理必须与原语句一致。"
            );
        }
        if (matches(EXCEPT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "EXCEPT_TO_ANTI_JOIN",
                "SET_DIFFERENCE_SEMANTICS_REQUIRED",
                "EXCEPT/MINUS 可评审为 anti join 或差集预计算。",
                "集合去重、NULL 和列顺序规则必须校验。"
            );
        }
        if (matches(JSON_EXTRACT_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "JSON_EXTRACT_MATERIALIZATION",
                "JSON_PATH_AND_TYPE_CONTRACT_REQUIRED",
                "JSON/VARIANT 提取可推荐物化为治理字段或展开视图。",
                "路径缺失、数组语义和类型转换可能改变结果。"
            );
        }
        if (matches(UNNEST_LATERAL_PATTERN, sql) || matches(SEMI_STRUCTURED_FLATTEN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UNNEST_LATERAL_REVIEW",
                "ARRAY_CARDINALITY_AND_NULL_POLICY_REQUIRED",
                "UNNEST/LATERAL/FLATTEN 可评审为预展开或半结构化索引候选。",
                "数组空值、重复元素和外连接展开语义需要校验。"
            );
        }
        if (matches(NULL_SAFE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NULL_SAFE_EQUALITY_REVIEW",
                "NULL_SENTINEL_AND_TYPE_REQUIRED",
                "COALESCE/NVL/NULL-safe 比较可评审为等价空值规范化。",
                "哨兵值与真实数据冲突会改变匹配或过滤结果。"
            );
        }
        if (matches(OFFSET_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "OFFSET_TO_KEYSET_PAGINATION",
                "STABLE_SORT_KEY_REQUIRED",
                "OFFSET 分页可评审为 keyset/seek 分页。",
                "缺少稳定排序键会改变翻页边界或漏/重复记录。"
            );
        }
        if (profile.getPredicateCount() == 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "FULL_SCAN_FILTER_GUARD",
                "BUSINESS_FILTER_OR_READ_SCOPE_REQUIRED",
                "无过滤 SELECT 必须补充业务范围、租户范围或只读扫描豁免证据。",
                "在 PB 级数据上全表扫描会造成资源争抢，且可能越过预期数据范围。"
            );
        }
        if (profile.getOrderByCount() > 0 && !profile.isLimitPresent()) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ORDER_BY_WITHOUT_LIMIT_GUARD",
                "TOPN_OR_CONSUMER_SORT_REQUIREMENT_REQUIRED",
                "无 LIMIT 排序需要确认消费者是否真的需要全量有序结果。",
                "全量排序可能引入宽 shuffle、spill 和长尾资源占用。"
            );
        }
        if (profile.isLimitPresent() && profile.getOrderByCount() == 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LIMIT_WITHOUT_ORDER_GUARD",
                "STABLE_ORDERING_INTENT_REQUIRED",
                "无 ORDER BY 的 LIMIT 需要确认是否允许非确定性抽样结果。",
                "不同执行计划可能返回不同前 N 行，影响报表可复现性。"
            );
        }
        if (profile.getRepeatedExpressionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "REPEATED_EXPRESSION_TO_CTE",
                "EXPRESSION_DETERMINISM_REQUIRED",
                "重复复杂表达式可评审为 CTE、派生列或服务层预计算。",
                "非确定性函数或引擎 CTE 内联行为可能改变性能和结果稳定性。"
            );
        }
        if (profile.getUdfFunctionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "UDF_EVALUATION_ISOLATION",
                "UDF_DETERMINISM_AND_COST_REQUIRED",
                "自定义函数应评审为前置过滤后执行、离线派生列或受治理服务函数。",
                "UDF 的确定性、权限和资源消耗不能由静态解析证明。"
            );
        }
        if (profile.getStringConcatenationCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "STRING_CONCAT_PRECOMPUTE",
                "OUTPUT_FORMAT_CONTRACT_REQUIRED",
                "高频字符串拼接可评审为展示层格式化或预计算展示字段。",
                "空值、分隔符和字符集规则会影响输出等价性。"
            );
        }
        if (profile.getLargeStringAggregateCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "LARGE_STRING_AGGREGATE_OFFLOAD",
                "AGGREGATE_LENGTH_AND_ORDER_POLICY_REQUIRED",
                "大字符串聚合可评审为明细下钻、离线摘要或受限长度聚合。",
                "聚合顺序、截断策略和超长输出会影响业务可读性与内存占用。"
            );
        }
        if (profile.getWindowFunctionCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "WINDOW_FRAME_PRECOMPUTE",
                "WINDOW_FRAME_AND_ORDER_CONTRACT_REQUIRED",
                "窗口函数可评审为分区预计算、指标层快照或 Top-N 专项改写。",
                "窗口边界、排序并列值和分区基数会影响结果。"
            );
        }
        if (matches(WITH_CLAUSE_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CTE_MATERIALIZATION_POLICY",
                "ENGINE_CTE_MATERIALIZATION_BEHAVIOR_REQUIRED",
                "WITH 查询需要确认引擎会内联还是物化 CTE，并评审复用收益。",
                "不同引擎的 CTE 策略不同，可能重复扫描或提前物化大量中间结果。"
            );
        }
        if (matches(CASE_EXPRESSION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CASE_EXPRESSION_NORMALIZATION",
                "CASE_BRANCH_EXCLUSIVITY_REQUIRED",
                "CASE 表达式可评审为维表映射、派生字段或指标口径统一。",
                "分支顺序、NULL 和兜底值会影响结果等价性。"
            );
        }
        if (countMatches(COUNT_DISTINCT_PATTERN, sql) >= 2) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "MULTI_COUNT_DISTINCT_DECOMPOSITION",
                "DISTINCT_CARDINALITY_AND_DEDUP_POLICY_REQUIRED",
                "多个 COUNT DISTINCT 可评审为分阶段聚合或 sketch 预计算。",
                "去重粒度和近似算法选择会影响精度与成本。"
            );
        }
        if (matches(NOT_EQUAL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NEGATION_FILTER_REVIEW",
                "SELECTIVITY_AND_NULL_POLICY_REQUIRED",
                "否定谓词可评审为正向枚举、排除表或分区剪枝辅助条件。",
                "否定谓词通常选择性弱，且 NULL 语义容易被误改。"
            );
        }
        if (matches(IS_NULL_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NULL_FILTER_INDEX_REVIEW",
                "NULL_DISTRIBUTION_AND_INDEX_SUPPORT_REQUIRED",
                "IS NULL/IS NOT NULL 可评审为空值分布统计、索引或派生标记列。",
                "空值分布高度倾斜时，错误索引或物化策略可能收益很低。"
            );
        }
        if (matches(ORDER_BY_FUNCTION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ORDER_BY_EXPRESSION_PRECOMPUTE",
                "SORT_EXPRESSION_DETERMINISM_REQUIRED",
                "函数排序键可评审为派生排序列或已排序服务对象。",
                "排序表达式的时区、大小写和 NULL 顺序需要与原语句一致。"
            );
        }
        if (matches(ARRAY_FUNCTION_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "ARRAY_CONTAINS_INDEX_REVIEW",
                "ARRAY_PATH_AND_CARDINALITY_REQUIRED",
                "数组包含或基数过滤可评审为预展开、倒排索引或半结构化字段索引。",
                "数组顺序、重复元素和空数组语义会影响过滤结果。"
            );
        }
        if (matches(RANGE_JOIN_PATTERN, sql)) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "RANGE_JOIN_BUCKETIZATION",
                "RANGE_OVERLAP_AND_BUCKET_POLICY_REQUIRED",
                "范围 join 可评审为时间桶、区间索引或预展开桥表。",
                "区间重叠、边界闭开和桶粒度错误会改变匹配关系。"
            );
        }
        if (profile.isDistinctPresent() && profile.getOrderByCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "DISTINCT_ORDER_BY_ALIGNMENT",
                "ORDER_KEY_PROJECTION_COMPATIBILITY_REQUIRED",
                "DISTINCT + ORDER BY 需要确认排序键与去重投影兼容。",
                "排序键不在去重粒度内时，不同引擎可能产生不稳定排序或额外去重成本。"
            );
        }
        if (profile.getCorrelatedSubqueryCount() > 0) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "CORRELATED_SUBQUERY_DECORRELATION",
                "CORRELATION_KEY_UNIQUENESS_REQUIRED",
                "关联子查询可评审为 decorrelation、semi join 或预聚合 join。",
                "关联键不唯一或谓词作用域不清会导致行数放大。"
            );
        }
        if (profile.getNestedSubqueryDepth() >= 2) {
            addUnappliedRuleIfAbsent(
                unappliedRules,
                preconditions,
                semanticRisks,
                "L1",
                "NESTED_SUBQUERY_FLATTENING",
                "NESTED_SCOPE_AND_NULL_POLICY_REQUIRED",
                "多层嵌套子查询可评审为分层 CTE 或显式 join 图。",
                "嵌套作用域、NULL 和聚合边界不清时，扁平化会改变结果。"
            );
        }
    }

    private void addL2RuleCandidates(ParsedSqlProfile profile,
                                     List<Map<String, Object>> ruleChain,
                                     List<Map<String, Object>> preconditions,
                                     List<Map<String, Object>> semanticRisks) {
        if (L2DynamicSnapshotAggregateMvCandidateGenerator.rewriteCandidate(
            profile.getNormalizedSql(),
            profile
        ) != null) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                L2DynamicSnapshotAggregateMvCandidateGenerator.RULE,
                "REPEATED_SNAPSHOT_AGGREGATE_SHAPE_REQUIRED",
                "重复日期快照与客户阈值聚合形态可收敛为客户-日期快照 MV，再按机构层派生结果。",
                "改写必须先通过结果集、逐键指标和计划形态验证，不能仅凭静态解析自动应用。",
                MaterializedViewRecommendationPlanner.SOURCE_AST_IR
            );
        }
        if (!profile.getAggregateFunctions().isEmpty() || profile.getGroupByCount() > 0) {
            ruleChain.add(ruleEntry(
                "L2",
                "PRECOMPUTE_MV",
                "PULL_ONLY_CANDIDATE",
                MaterializedViewRecommendationPlanner.SOURCE_AST_IR,
                Boolean.FALSE,
                "高复用聚合形态进入 AST/IR/QBDAG/关系代数驱动的物化视图候选规划。"
            ));
            preconditions.add(preconditionEntry(
                "PRECOMPUTE_MV",
                "MV_COVERAGE_PROOF_AND_REFRESH_POLICY_REQUIRED",
                "分发前需要覆盖证明、运行时频次、新鲜度目标和刷新责任归属。"
            ));
        }
        if (!profile.getDatePredicateColumns().isEmpty()) {
            ruleChain.add(ruleEntry(
                "L2",
                "PARTITION_PRUNING",
                "PULL_ONLY_CANDIDATE",
                "STATIC_PARSE",
                Boolean.FALSE,
                "日期类谓词表明存在分区裁剪或分区键推荐机会。"
            ));
            preconditions.add(preconditionEntry(
                "PARTITION_PRUNING",
                "PARTITION_METADATA_REQUIRED",
                "外部执行前必须检查分区键元数据和当前存储布局。"
            ));
        }
        if (profile.getJoinCount() > 0) {
            ruleChain.add(ruleEntry(
                "L2",
                "BUCKET_JOIN",
                "PULL_ONLY_CANDIDATE",
                "STATIC_PARSE",
                Boolean.FALSE,
                "join 活动表明存在分桶对齐或共置推荐候选。"
            ));
            preconditions.add(preconditionEntry(
                "BUCKET_JOIN",
                "JOIN_KEY_DISTRIBUTION_REQUIRED",
                "修改分桶前需要 join 键稳定性、数据倾斜和存储责任归属证据。"
            ));
            semanticRisks.add(semanticRiskEntry(
                "BUCKET_JOIN",
                "PHYSICAL_LAYOUT_RISK",
                "MEDIUM",
                "分桶或共置变更属于外部物理协同，不能仅凭静态解析自动应用。"
            ));
        }
    }

    private void addExtendedPhysicalRuleCandidates(ParsedSqlProfile profile,
                                                   List<Map<String, Object>> ruleChain,
                                                   List<Map<String, Object>> preconditions,
                                                   List<Map<String, Object>> semanticRisks) {
        String sql = profile.getNormalizedSql();
        if (profile.getJoinCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "JOIN_REORDER_BY_STATS",
                "TABLE_STATISTICS_REQUIRED",
                "多 join 查询应使用表统计信息评估 join reorder。",
                "统计信息过期会让成本模型选择次优计划。"
            );
        }
        if (profile.getJoinCount() > 0 && profile.getPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "DYNAMIC_FILTERING_JOIN",
                "CONNECTOR_DYNAMIC_FILTER_SUPPORT_REQUIRED",
                "选择性维表过滤可推荐动态过滤或动态分区裁剪。",
                "connector 不支持时该建议不能被写成确定收益。"
            );
        }
        if (profile.getJoinCount() >= 2 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "STAR_SCHEMA_MV",
                "FACT_DIMENSION_GRAIN_REQUIRED",
                "宽表 join 加聚合可推荐星型模型物化视图或指标层。",
                "粒度、刷新和维表缓慢变化处理必须由治理流程确认。"
            );
        }
        if (profile.getComplexGraphScore() >= 5 || profile.isSetOperation()) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SPLIT_SQL",
                "STAGE_BOUNDARY_AND_IDEMPOTENCY_REQUIRED",
                "复杂查询图可拆为 CTE、临时服务对象或多阶段执行。",
                "阶段边界不当会增加中间数据量或破坏一致性。"
            );
        }
        if (profile.isLimitPresent()
            || profile.getAggregateFunctionCount() > 0
            || profile.getDatePredicateColumns().size() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "RESULT_CACHE",
                "SCHEMA_VERSION_AND_FRESHNESS_POLICY_REQUIRED",
                "稳定筛选、聚合或分页查询可推荐受治理结果缓存。",
                "缓存命中必须受 schemaVersion、新鲜度和租户边界约束。"
            );
        }
        if (profile.getRepeatedTableScanCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "REPORT_SQL_MERGE",
                "SOURCE_REPORT_AND_OVERLAP_EVIDENCE_REQUIRED",
                "同源报表或同 SQL 多次扫描可推荐合并查询或共享中间结果。",
                "合并多条 SQL 可能改变审计粒度和失败隔离边界。"
            );
        }
        if (profile.getJoinCount() >= 2
            || profile.getPredicateCount() >= 4
            || profile.getAggregateFunctionCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "STATISTICS_REFRESH",
                "EXPLAIN_OR_RUNTIME_PLAN_EVIDENCE_REQUIRED",
                "复杂 join、谓词或聚合查询应检查统计信息刷新。",
                "没有真实计划证据时只能作为外部执行建议。"
            );
        }
        if (profile.getDatePredicateColumns().size() > 0 && profile.getPredicateCount() >= 2) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "FILE_COMPACTION",
                "FILE_LAYOUT_AND_SMALL_FILE_EVIDENCE_REQUIRED",
                "分区过滤查询可联动检查小文件合并和布局整理。",
                "文件整理属于外部存储操作，不能由 SQL 推荐自动执行。"
            );
        }
        if (profile.getProjectionCount() >= 6 || profile.getStringProjectionCount() >= 3) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PROJECTION_PRUNING",
                "DOWNSTREAM_COLUMN_USAGE_REQUIRED",
                "宽投影可推荐列裁剪或服务层字段白名单。",
                "裁剪列需要证明下游消费者不依赖被移除字段。"
            );
        }
        if (profile.getPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PREDICATE_PUSHDOWN",
                "CONNECTOR_PUSHDOWN_CAPABILITY_REQUIRED",
                "可下推过滤应被保留在扫描侧或外部数据源侧。",
                "connector 能力不同，静态解析不能宣称真实下推成功。"
            );
        }
        if (profile.isLimitPresent() && profile.getOrderByCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "TOPN_PUSHDOWN",
                "ORDER_KEY_AND_CONNECTOR_SUPPORT_REQUIRED",
                "ORDER BY + LIMIT 可推荐 Top-N 下推或服务层有序输出。",
                "排序稳定性和 connector 支持必须通过计划证据确认。"
            );
        }
        if (profile.getJoinCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SMALL_TABLE_BROADCAST_JOIN",
                "BUILD_SIDE_SIZE_STATS_REQUIRED",
                "维表较小时可推荐广播 join 或 replicated layout。",
                "构建侧过大时广播会增加内存压力。"
            );
        }
        if (profile.getJoinCount() >= 2 || profile.getOrPredicateCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SKEW_JOIN_SALTING_REVIEW",
                "KEY_DISTRIBUTION_EVIDENCE_REQUIRED",
                "热点键或多 join 查询可评审倾斜处理、salt 或 AQE 策略。",
                "盲目 salt 会改变 join 代价并增加数据膨胀。"
            );
        }
        if (matches(CASE_AGGREGATION_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PIVOT_AGGREGATE_PRECOMPUTE",
                "METRIC_DEFINITION_AND_CARDINALITY_REQUIRED",
                "CASE WHEN 聚合可推荐指标层预计算或宽表透视。",
                "指标口径和稀疏维度扩展需要治理确认。"
            );
        }
        if (matches(DATE_TRUNC_PATTERN, sql) && profile.getGroupByCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "DATE_GRANULARITY_MV",
                "TIMEZONE_AND_GRAIN_POLICY_REQUIRED",
                "日期粒度聚合可推荐日/周/月指标物化视图。",
                "时区、财务日历和粒度 rollup 会影响结果。"
            );
        }
        if (profile.getDatePredicateColumns().size() > 0 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PARTITION_COMPENSATION_UNION",
                "PARTITION_FRESHNESS_EVIDENCE_REQUIRED",
                "分区聚合可推荐新鲜分区走基表、历史分区走物化结果的补偿 UNION。",
                "分区新鲜度和变更追踪不完整时会产生陈旧结果。"
            );
        }
        if (matches(JSON_EXTRACT_PATTERN, sql) || matches(SEMI_STRUCTURED_FLATTEN_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "SEMISTRUCTURED_COLUMN_INDEX",
                "PATH_FREQUENCY_AND_INDEX_SUPPORT_REQUIRED",
                "半结构化字段热点路径可推荐展开列、搜索优化或索引化。",
                "路径类型漂移和数组展开会影响结果与成本。"
            );
        }
        if (matches(APPROX_DISTINCT_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "APPROX_DISTINCT_SKETCH_MV",
                "SKETCH_ERROR_BOUND_AND_REFRESH_POLICY_REQUIRED",
                "近似去重可推荐 HLL/sketch 物化结果或指标层复用。",
                "近似误差、合并策略和刷新周期必须由业务接受。"
            );
        }
        if (matches(PERCENTILE_PATTERN, sql)) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "PERCENTILE_SKETCH_PRECOMPUTE",
                "PERCENTILE_ACCURACY_AND_MERGE_POLICY_REQUIRED",
                "分位数聚合可推荐 sketch 预计算或离线指标层。",
                "近似分位数不可随意合并，精度与样本边界必须验证。"
            );
        }
        if (profile.getGroupByCount() >= 2 && profile.getAggregateFunctionCount() > 0) {
            addPullOnlyRuleIfAbsent(
                ruleChain,
                preconditions,
                semanticRisks,
                "ROLLUP_AGGREGATE_LATTICE",
                "DIMENSION_HIERARCHY_AND_ROLLUP_POLICY_REQUIRED",
                "多维聚合可推荐 rollup/lattice 物化视图或指标宽表。",
                "维度层级、钻取路径和稀疏组合会影响存储成本与结果口径。"
            );
        }
    }

    private void addUnappliedRuleIfAbsent(List<Map<String, Object>> unappliedRules,
                                          List<Map<String, Object>> preconditions,
                                          List<Map<String, Object>> semanticRisks,
                                          String level,
                                          String rule,
                                          String missingEvidence,
                                          String preconditionDescription,
                                          String riskDescription) {
        if (containsRule(unappliedRules, rule)) {
            return;
        }
        addUnappliedRule(
            unappliedRules,
            preconditions,
            semanticRisks,
            level,
            rule,
            missingEvidence,
            preconditionDescription,
            riskDescription
        );
    }

    private void addPullOnlyRuleIfAbsent(List<Map<String, Object>> ruleChain,
                                         List<Map<String, Object>> preconditions,
                                         List<Map<String, Object>> semanticRisks,
                                         String rule,
                                         String missingEvidence,
                                         String description,
                                         String riskDescription) {
        addPullOnlyRuleIfAbsent(
            ruleChain,
            preconditions,
            semanticRisks,
            rule,
            missingEvidence,
            description,
            riskDescription,
            "STATIC_PARSE"
        );
    }

    private void addPullOnlyRuleIfAbsent(List<Map<String, Object>> ruleChain,
                                         List<Map<String, Object>> preconditions,
                                         List<Map<String, Object>> semanticRisks,
                                         String rule,
                                         String missingEvidence,
                                         String description,
                                         String riskDescription,
                                         String evidenceLevel) {
        if (containsRule(ruleChain, rule)) {
            return;
        }
        ruleChain.add(ruleEntry(
            "L2",
            rule,
            "PULL_ONLY_CANDIDATE",
            evidenceLevel,
            Boolean.FALSE,
            description
        ));
        preconditions.add(preconditionEntry(rule, missingEvidence, description));
        semanticRisks.add(semanticRiskEntry(rule, "PHYSICAL_OR_RUNTIME_BOUNDARY_RISK", "MEDIUM", riskDescription));
    }

    private boolean containsRule(List<Map<String, Object>> entries, String rule) {
        if (entries == null || !StringUtils.hasText(rule)) {
            return false;
        }
        for (Map<String, Object> entry : entries) {
            if (entry != null && rule.equals(entry.get("rule"))) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(Pattern pattern, String sql) {
        return pattern != null && StringUtils.hasText(sql) && pattern.matcher(sql).find();
    }

    private int countMatches(Pattern pattern, String sql) {
        if (pattern == null || !StringUtils.hasText(sql)) {
            return 0;
        }
        int count = 0;
        Matcher matcher = pattern.matcher(sql);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private void addUnappliedRule(List<Map<String, Object>> unappliedRules,
                                  List<Map<String, Object>> preconditions,
                                  List<Map<String, Object>> semanticRisks,
                                  String level,
                                  String rule,
                                  String missingEvidence,
                                  String preconditionDescription,
                                  String riskDescription) {
        addUnappliedRule(
            unappliedRules,
            preconditions,
            semanticRisks,
            level,
            rule,
            missingEvidence,
            preconditionDescription,
            riskDescription,
            Collections.<String, Object>emptyMap()
        );
    }

    private void addUnappliedRule(List<Map<String, Object>> unappliedRules,
                                  List<Map<String, Object>> preconditions,
                                  List<Map<String, Object>> semanticRisks,
                                  String level,
                                  String rule,
                                  String missingEvidence,
                                  String preconditionDescription,
                                  String riskDescription,
                                  Map<String, Object> evidence) {
        LinkedHashMap<String, Object> unapplied = new LinkedHashMap<String, Object>();
        unapplied.put("level", level);
        unapplied.put("rule", rule);
        unapplied.put("status", "NOT_APPLIED");
        unapplied.put("statusZh", RecommendationRuleExplanationService.statusZh("NOT_APPLIED"));
        unapplied.put("reason", missingEvidence);
        unapplied.put("manualReviewRequired", Boolean.TRUE);
        RecommendationRuleExplanationService.enrich(unapplied, rule);
        if (evidence != null && !evidence.isEmpty()) {
            unapplied.put("evidence", evidence);
        }
        unappliedRules.add(unapplied);
        preconditions.add(preconditionEntry(rule, missingEvidence, preconditionDescription));
        semanticRisks.add(semanticRiskEntry(rule, "SEMANTIC_EQUIVALENCE_RISK", "HIGH", riskDescription));
    }

    private Map<String, Object> selectStarEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("starItems", profile.getSelectStarItems());
        evidence.put("knownTables", profile.getTables());
        evidence.put("metadataRequirement", "TRUSTED_COLUMN_LIST_REQUIRED");
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    private Map<String, Object> functionPredicateEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("predicateSamples", profile.getFunctionWrappedPredicateExpressions());
        evidence.put("requiredProofs", Arrays.asList("COLUMN_TYPE", "TIMEZONE", "BOUNDARY_PRECISION"));
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    private Map<String, Object> repeatedSubqueryEvidence(ParsedSqlProfile profile) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("repeatedSubqueryCount", Integer.valueOf(profile.getRepeatedSubqueryCount()));
        evidence.put("subquerySamples", profile.getRepeatedSubquerySamples());
        evidence.put("requiredProofs", Arrays.asList("SIDE_EFFECT_FREE", "ENGINE_CTE_BEHAVIOR"));
        evidence.put("rewritePolicy", "MANUAL_REVIEW_ONLY");
        return evidence;
    }

    private Map<String, Object> ruleEntry(String level,
                                          String rule,
                                          String status,
                                          String evidenceLevel,
                                          Boolean autoApplyEligibleAfterValidation,
                                          String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("level", level);
        entry.put("rule", rule);
        entry.put("status", status);
        entry.put("evidenceLevel", evidenceLevel);
        entry.put("autoApplyEligibleAfterValidation", autoApplyEligibleAfterValidation);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        entry.put("statusZh", RecommendationRuleExplanationService.statusZh(status));
        if (!Boolean.TRUE.equals(autoApplyEligibleAfterValidation)) {
            entry.put("manualReviewRequired", Boolean.TRUE);
        }
        return entry;
    }

    private Map<String, Object> preconditionEntry(String rule, String code, String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("code", code);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        return entry;
    }

    private Map<String, Object> semanticRiskEntry(String rule, String category, String severity, String description) {
        LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("rule", rule);
        entry.put("category", category);
        entry.put("severity", severity);
        entry.put("description", description);
        RecommendationRuleExplanationService.enrich(entry, rule);
        return entry;
    }

    private String normalizeRecommendationRule(String appliedRule) {
        if ("COUNT_LITERAL_TO_COUNT_STAR".equals(appliedRule)) {
            return "COUNT_ONE_TO_COUNT_STAR";
        }
        if ("DEDUPLICATE_GROUP_BY_KEYS".equals(appliedRule) || "DEDUPLICATE_ORDER_BY_KEYS".equals(appliedRule)) {
            return "DUPLICATE_GROUP_ORDER_KEY";
        }
        return appliedRule;
    }

    private String l0RuleDescription(String appliedRule) {
        if ("COUNT_LITERAL_TO_COUNT_STAR".equals(appliedRule)) {
            return "候选 SQL 中非空字面量上的 COUNT 已标准化为 COUNT(*)。";
        }
        if ("DEDUPLICATE_GROUP_BY_KEYS".equals(appliedRule) || "DEDUPLICATE_ORDER_BY_KEYS".equals(appliedRule)) {
            return "已移除重复分组或排序键，并保留首次出现顺序。";
        }
        return "已对候选 SQL 应用保守语法改写。";
    }

    private boolean containsManualReviewRule(List<Map<String, Object>> ruleChain) {
        for (Map<String, Object> rule : ruleChain) {
            if (Boolean.TRUE.equals(rule.get("manualReviewRequired"))) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> expectedBenefit(ParsedSqlProfile profile,
                                                RewriteOutcome outcome,
                                                RewriteProductionCapabilityReport productionCapabilityReport) {
        LinkedHashMap<String, Object> benefit = new LinkedHashMap<String, Object>();
        benefit.put("evidenceType", "STATIC_HEURISTIC");
        benefit.put("claimBoundary", "NOT_REAL_EXECUTION_GAIN");
        benefit.put("summary", "仅基于解析器信号和规则覆盖估算。");
        benefit.put("appliedRuleCount", Integer.valueOf(outcome.appliedRules.size()));
        benefit.put("riskSignalCount", Integer.valueOf(profile.getWarnings().size()));
        benefit.put("level", staticBenefitLevel(profile, outcome));
        benefit.put("productionScaleGate", productionScaleGate());
        benefit.put("productionCapabilityGate", productionCapabilityReport.toSummaryMap());
        return benefit;
    }

    private Map<String, Object> estimatedCost(ParsedSqlProfile profile,
                                              boolean manualReviewRequired,
                                              RewriteProductionCapabilityReport productionCapabilityReport) {
        LinkedHashMap<String, Object> cost = new LinkedHashMap<String, Object>();
        cost.put("evidenceType", "STATIC_HEURISTIC");
        cost.put("validation", "RESULT_DIFF_REQUIRED");
        cost.put("manualReviewRequired", Boolean.valueOf(manualReviewRequired));
        cost.put("followUp", manualReviewRequired ? "REVIEW_RULE_PRECONDITIONS" : "VALIDATE_L0_CANDIDATE");
        cost.put("riskSignalCount", Integer.valueOf(profile.getWarnings().size()));
        cost.put("productionScaleGate", productionScaleGate());
        cost.put("productionCapabilityGate", productionCapabilityReport.toSummaryMap());
        return cost;
    }

    private Map<String, Object> productionScaleGate() {
        LinkedHashMap<String, Object> gate = new LinkedHashMap<String, Object>();
        gate.put("status", "EXTERNAL_EVIDENCE_REQUIRED");
        gate.put("claimBoundary", "PRODUCTION_SCALE_NOT_PROVEN_BY_STATIC_REWRITE");
        gate.put("targetConcurrency", Integer.valueOf(PRODUCTION_TARGET_CONCURRENCY));
        gate.put("targetDatasetSizeLabel", "THIRTY_PB");
        gate.put("targetDatasetSizeBytes", Long.valueOf(PRODUCTION_TARGET_DATASET_SIZE_BYTES));
        gate.put("targetDailyQueryVolume", Long.valueOf(PRODUCTION_TARGET_DAILY_QUERY_VOLUME));
        gate.put("requiredReplayHours", Integer.valueOf(PRODUCTION_TARGET_REPLAY_HOURS));
        gate.put("requiredEvidence", PRODUCTION_SCALE_REQUIRED_EVIDENCE);
        gate.put(
            "verifierCommand",
            "python3 scripts/verify-benchmark-production-evidence.py --evidence-dir <external-evidence-dir> "
                + "--output <external-evidence-dir>/verification-result.json"
        );
        gate.put("blockedTask", "USER-CN-BENCHMARK-PRODUCTION-EVIDENCE-EXTERNAL-ARTIFACTS-20260518");
        return gate;
    }

    private String staticBenefitLevel(ParsedSqlProfile profile, RewriteOutcome outcome) {
        if (outcome.appliedRules.size() >= 2 || profile.getAggregateFunctionCount() >= 4 || profile.getJoinCount() >= 3) {
            return "MEDIUM";
        }
        if (!outcome.appliedRules.isEmpty() || profile.isSelectStar() || profile.getDatePredicateColumns().size() > 0) {
            return "LOW";
        }
        return "UNKNOWN";
    }

    private int calculateRecommendationConfidence(ParsedSqlProfile profile,
                                                  RewriteOutcome outcome,
                                                  List<Map<String, Object>> unappliedRules) {
        return clamp(62 + outcome.appliedRules.size() * 6 - unappliedRules.size() * 5 - profile.getWarnings().size() * 2, 25, 88);
    }

    private RewriteOutcome applyCalciteRewriteRules(SqlNode statement) {
        LinkedHashSet<String> appliedRules = new LinkedHashSet<String>();
        SqlNode rewritten = rewriteCalciteNode(statement, appliedRules);
        String rewrittenSql = rewritten == null
            ? ""
            : removeSimpleIdentifierQuotes(rewritten.toString()).replaceAll("\\s+", " ").trim();
        return new RewriteOutcome(rewrittenSql, new ArrayList<String>(appliedRules));
    }

    private SqlNode rewriteCalciteNode(SqlNode node, Set<String> appliedRules) {
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

    private void rewriteCalciteSelect(SqlSelect select, Set<String> appliedRules) {
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

    private SqlNode rewriteCountLiteralProjection(SqlNode node, Set<String> appliedRules) {
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

    private boolean deduplicateCalciteNodeList(SqlNodeList nodeList, Set<String> appliedRules, String rule) {
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

    private SqlNode deduplicateCalciteAndExpression(SqlNode expression) {
        List<SqlNode> flattened = new ArrayList<SqlNode>();
        flattenCalciteAndExpression(expression, flattened);
        LinkedHashMap<String, SqlNode> unique = new LinkedHashMap<String, SqlNode>();
        for (SqlNode item : flattened) {
            unique.put(normalizeCalciteKey(item), item);
        }
        return rebuildCalciteAndExpression(unique.values(), expression == null ? SqlParserPos.ZERO : expression.getParserPosition());
    }

    private void flattenCalciteAndExpression(SqlNode expression, List<SqlNode> collector) {
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

    private SqlNode rebuildCalciteAndExpression(Collection<SqlNode> expressions, SqlParserPos pos) {
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

    private String normalizeCalciteKey(Object value) {
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

    private static String removeSimpleIdentifierQuotes(String value) {
        if (value == null) {
            return "";
        }
        return SIMPLE_BACKTICK_IDENTIFIER_PATTERN.matcher(value).replaceAll("$1");
    }

    private void finalizeWarnings(ParsedSqlProfile profile) {
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

    private static boolean isStringLikeProjectionKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.replace("\"", "").replace("`", "").trim();
        return STRING_LIKE_PROJECTION_PATTERN.matcher(normalized).find();
    }

    private LinkedHashMap<AccelerationSuggestionType, String> deriveAccelerationReasons(ParsedSqlProfile profile) {
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

    private boolean hasCommonSubgraphSignal(ParsedSqlProfile profile) {
        if (profile == null) {
            return false;
        }
        Map<String, Object> advancedStructureProfile = profile.toAdvancedStructureProfile();
        return !advancedMapList(advancedStructureProfile.get("ctes")).isEmpty()
            || !advancedMapList(advancedStructureProfile.get("subqueries")).isEmpty()
            || profile.getRepeatedSubqueryCount() > 0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> advancedMapList(Object value) {
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

    private LinkedHashMap<AccelerationSuggestionType, String> filterRequestedTypes(
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

    private List<OptimizationTaskRisk> buildShapeRisks(ParsedSqlProfile profile) {
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

    private String buildParseSummary(ParsedSqlProfile profile) {
        return "已解析一条 SELECT 语句，覆盖 "
            + profile.tables.size()
            + " 张表、"
            + profile.joinCount
            + " 个 join、"
            + profile.predicateCount
            + " 个谓词片段。";
    }

    private String buildParseRecommendation(ParsedSqlProfile profile) {
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

    private int calculateParseConfidence(ParsedSqlProfile profile) {
        return clamp(92 - profile.warnings.size() * 8 - profile.joinCount * 2, 45, 96);
    }

    private int calculateRewriteConfidence(ParsedSqlProfile profile, RewriteOutcome outcome) {
        return clamp(58 + outcome.appliedRules.size() * 9 - profile.warnings.size() * 4, 35, 92);
    }

    private int calculateRewriteConfidence(ParsedSqlProfile profile, List<String> appliedRules) {
        int appliedRuleCount = appliedRules == null ? 0 : appliedRules.size();
        return clamp(58 + appliedRuleCount * 9 - profile.warnings.size() * 4, 35, 92);
    }

    private String rewriteSummary(List<String> appliedRules) {
        return "已生成候选改写 SQL，包含 " + appliedRules.size() + " 条安全 AST 规则。";
    }

    private String rewriteRecommendation() {
        return "请先将候选改写结果与原始语句做校验，再把批准后的 SQL 带入下一步治理。";
    }

    private int calculateAccelerationConfidence(ParsedSqlProfile profile,
                                                LinkedHashMap<AccelerationSuggestionType, String> reasons) {
        return clamp(54 + reasons.size() * 8 + profile.aggregateFunctions.size() * 3 - profile.warnings.size() * 3, 40, 90);
    }

    private boolean isNonNullCalciteLiteral(SqlNode expression) {
        if (!(expression instanceof SqlLiteral)) {
            return false;
        }
        return ((SqlLiteral) expression).getValue() != null;
    }

    private SqlOptimizationExecutionException invalidTask(String message, String suggestedAction) {
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

    private SqlOptimizationExecutionException parserFailure(String message,
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

    private String normalizeSql(String sqlText) {
        return SqlDialectNormalizer.normalize(sqlText);
    }

    private void augmentCalciteProfileFromSqlText(String normalizedSql, ParsedSqlProfile profile) {
        if (!StringUtils.hasText(normalizedSql) || profile == null) {
            return;
        }
        profile.datePredicateColumns.addAll(extractDatePredicateColumns(normalizedSql));
        recordSqlLevelFunctionWrappedPredicates(normalizedSql, profile);
    }

    private List<String> extractDatePredicateColumns(String expressionSql) {
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

    private void recordSqlLevelFunctionWrappedPredicates(String normalizedSql, ParsedSqlProfile profile) {
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

    private SqlFailurePosition analyzeSqlFailurePosition(Throwable cause, String sqlText) {
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

    private SqlFailurePosition guessSelectProjectionFailure(String sqlText) {
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

    private String collectExceptionMessage(Throwable cause) {
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

    private Integer findFirstInteger(String message, Pattern pattern, int group) {
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

    private String firstRegexGroup(String message, Pattern pattern) {
        if (message == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Integer findTokenOffset(String sqlText, String token, Integer line, Integer anchorOffset) {
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

    private int[] lineBounds(String sqlText, int line) {
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

    private boolean startsWithToken(String sqlText, int offset, String token) {
        if (sqlText == null || token == null || offset < 0 || offset + token.length() > sqlText.length()) {
            return false;
        }
        return sqlText.regionMatches(true, offset, token, 0, token.length());
    }

    private Integer toOffset(String sqlText, Integer line, Integer column) {
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

    private int[] toLineColumn(String sqlText, int offset) {
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

    private String snippet(String sqlText, Integer offset) {
        if (sqlText == null || offset == null) {
            return null;
        }
        int safeOffset = Math.max(0, Math.min(offset.intValue(), sqlText.length()));
        int start = Math.max(0, safeOffset - 30);
        int end = Math.min(sqlText.length(), safeOffset + 30);
        return sqlText.substring(start, end).replace('\n', ' ').replace('\r', ' ').trim();
    }

    private List<String> deduplicate(List<String> input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(new LinkedHashSet<String>(input));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private final class ApacheCalciteProfileCollector {

        private final Set<String> cteNames = new LinkedHashSet<String>();

        private void collect(SqlNode node, ParsedSqlProfile profile, boolean root) {
            if (node == null) {
                return;
            }
            if (node instanceof SqlOrderBy) {
                collectOrderBy((SqlOrderBy) node, profile, root);
                return;
            }
            if (node instanceof SqlWith) {
                collectWith((SqlWith) node, profile, root);
                return;
            }
            if (node instanceof SqlSelect) {
                collectSelect((SqlSelect) node, profile, root);
                return;
            }
            if (node instanceof SqlJoin) {
                collectJoin((SqlJoin) node, profile);
                return;
            }
            if (node instanceof SqlIdentifier) {
                collectIdentifier((SqlIdentifier) node, profile);
                return;
            }
            if (node instanceof SqlBasicCall) {
                collectCall((SqlBasicCall) node, profile);
                return;
            }
            if (node instanceof SqlCall) {
                collectGenericCall((SqlCall) node, profile);
            }
        }

        private void collectOrderBy(SqlOrderBy orderBy, ParsedSqlProfile profile, boolean root) {
            collect(orderBy.query, profile, root);
            collectOrderList(orderBy.orderList, profile);
            if (orderBy.fetch != null || orderBy.offset != null) {
                profile.limitPresent = true;
                profile.recordLimit(orderBy.fetch, orderBy.offset);
            }
        }

        private void collectWith(SqlWith with, ParsedSqlProfile profile, boolean root) {
            List<String> addedCteNames = new ArrayList<String>();
            try {
                if (with.withList != null) {
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            SqlWithItem withItem = (SqlWithItem) itemNode;
                            profile.recordCte(withItem);
                            String cteName = normalizeCalciteIdentifier(withItem.name);
                            if (StringUtils.hasText(cteName) && cteNames.add(cteName.toUpperCase(Locale.ROOT))) {
                                addedCteNames.add(cteName.toUpperCase(Locale.ROOT));
                            }
                        }
                    }
                    for (SqlNode itemNode : with.withList.getList()) {
                        if (itemNode instanceof SqlWithItem) {
                            SqlWithItem item = (SqlWithItem) itemNode;
                            collect(item.query, profile, true);
                        } else {
                            collect(itemNode, profile, false);
                        }
                    }
                }
                collect(with.body, profile, root);
            } finally {
                for (String cteName : addedCteNames) {
                    cteNames.remove(cteName);
                }
            }
        }

        private void collectSelect(SqlSelect select, ParsedSqlProfile profile, boolean root) {
            if (!root) {
                profile.subqueryCount++;
                profile.recordSubquery(select);
                profile.updateNestedSubqueryDepth();
                boolean correlated = profile.referencesVisibleAlias(select.toString());
                profile.recordSubqueryNode(select, "SUBQUERY", "", correlated);
                if (correlated) {
                    profile.correlatedSubqueryCount++;
                }
                profile.subqueryDepth++;
            }
            profile.pushAliasScope(collectCalciteAliases(select.getFrom()));
            try {
                collectSelectList(select.getSelectList(), profile);
                collectFrom(select.getFrom(), profile);
                if (select.getWhere() != null) {
                    collectPredicate("WHERE", select.getWhere(), profile);
                }
                SqlNodeList group = select.getGroup();
                if (group != null) {
                    profile.groupByCount += group.size();
                    for (SqlNode item : group.getList()) {
                        profile.recordGroupBy(item, collectCalciteColumnReferences(item));
                        profile.recordGroupByKey(item);
                        profile.recordExpression(item);
                        collectExpression(item, profile);
                    }
                }
                if (select.getHaving() != null) {
                    collectPredicate("HAVING", select.getHaving(), profile);
                }
                collectOrderList(select.getOrderList(), profile);
                if (select.getFetch() != null || select.getOffset() != null) {
                    profile.limitPresent = true;
                    profile.recordLimit(select.getFetch(), select.getOffset());
                }
                if (select.isDistinct()) {
                    profile.distinctPresent = true;
                }
            } finally {
                profile.popAliasScope();
                if (!root) {
                    profile.subqueryDepth--;
                }
            }
        }

        private void collectSelectList(SqlNodeList selectList, ParsedSqlProfile profile) {
            if (selectList == null) {
                return;
            }
            profile.projectionCount += selectList.size();
            for (SqlNode item : selectList.getList()) {
                SqlNode expression = calciteExpressionWithoutAlias(item);
                profile.recordProjection(item, collectCalciteColumnReferences(expression));
                if (isCalciteSelectLike(expression) || (expression != null && expression.getKind() == SqlKind.SCALAR_QUERY)) {
                    recordSubqueryOperand(expression, profile, "EXPRESSION", calciteProjectionAlias(item), true);
                    continue;
                }
                if (isStar(expression)) {
                    profile.selectStar = true;
                    profile.recordSelectStar(item);
                    continue;
                }
                if (isCalciteStringProjection(expression)) {
                    profile.stringProjectionCount++;
                }
                profile.recordExpression(expression);
                collectExpression(expression, profile);
            }
        }

        private SqlNode calciteExpressionWithoutAlias(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (!operands.isEmpty()) {
                    return operands.get(0);
                }
            }
            return node;
        }

        private String calciteProjectionAlias(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (operands.size() >= 2) {
                    return normalizeCalciteIdentifier(operands.get(1));
                }
            }
            return "";
        }

        private void collectFrom(SqlNode from, ParsedSqlProfile profile) {
            if (from == null) {
                return;
            }
            if (from instanceof SqlIdentifier) {
                String table = normalizeCalciteIdentifier(from);
                if (isCteReference(table)) {
                    profile.recordTable(table, "", "CTE_REFERENCE");
                    return;
                }
                if (!profile.tables.contains(table)) {
                    profile.tables.add(table);
                }
                profile.recordTableScan(table);
                profile.recordTable(table, "", "BASE_TABLE");
                return;
            }
            if (from instanceof SqlBasicCall && from.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) from).getOperandList();
                if (!operands.isEmpty()) {
                    String alias = operands.size() > 1 ? normalizeCalciteIdentifier(operands.get(1)) : "";
                    SqlNode relation = operands.get(0);
                    if (relation instanceof SqlIdentifier) {
                        String table = normalizeCalciteIdentifier(relation);
                        if (isCteReference(table)) {
                            profile.recordTable(table, alias, "CTE_REFERENCE");
                            return;
                        }
                        if (!profile.tables.contains(table)) {
                            profile.tables.add(table);
                        }
                        profile.recordTableScan(table);
                        profile.recordTable(table, alias, "BASE_TABLE");
                        return;
                    }
                    if (isCalciteSelectLike(relation)) {
                        profile.subqueryCount++;
                        profile.recordSubquery(relation);
                        profile.updateNestedSubqueryDepth();
                        boolean correlated = profile.referencesVisibleAlias(relation.toString());
                        profile.recordSubqueryNode(relation, "FROM", alias, correlated);
                        if (correlated) {
                            profile.correlatedSubqueryCount++;
                        }
                        profile.subqueryDepth++;
                        try {
                            collect(relation, profile, true);
                        } finally {
                            profile.subqueryDepth--;
                        }
                        return;
                    }
                    collectFrom(relation, profile);
                }
                return;
            }
            collect(from, profile, false);
        }

        private Set<String> collectCalciteAliases(SqlNode from) {
            LinkedHashSet<String> aliases = new LinkedHashSet<String>();
            collectCalciteAliases(from, aliases);
            return aliases;
        }

        private void collectCalciteAliases(SqlNode node, Set<String> aliases) {
            if (node == null || aliases == null) {
                return;
            }
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (operands.size() >= 2) {
                    String alias = normalizeCalciteIdentifier(operands.get(1));
                    if (StringUtils.hasText(alias)) {
                        aliases.add(alias.toUpperCase(Locale.ROOT));
                    }
                    collectCalciteAliases(operands.get(0), aliases);
                }
                return;
            }
            if (node instanceof SqlJoin) {
                collectCalciteAliases(((SqlJoin) node).getLeft(), aliases);
                collectCalciteAliases(((SqlJoin) node).getRight(), aliases);
                return;
            }
            if (node instanceof SqlIdentifier) {
                String table = normalizeCalciteIdentifier(node);
                if (StringUtils.hasText(table)) {
                    String[] parts = table.split("\\.");
                    aliases.add(parts[parts.length - 1].toUpperCase(Locale.ROOT));
                }
            }
        }

        private boolean isCteReference(String tableName) {
            if (!StringUtils.hasText(tableName)) {
                return false;
            }
            return cteNames.contains(tableName.toUpperCase(Locale.ROOT));
        }

        private String normalizeCalciteIdentifier(SqlNode node) {
            if (node == null) {
                return "";
            }
            return node.toString()
                .replace("\"", "")
                .replace("`", "")
                .trim();
        }

        private List<String> collectCalciteColumnReferences(SqlNode node) {
            LinkedHashSet<String> columns = new LinkedHashSet<String>();
            collectCalciteColumnReferences(node, columns);
            return new ArrayList<String>(columns);
        }

        private void collectCalciteColumnReferences(SqlNode node, Set<String> columns) {
            if (node == null || columns == null) {
                return;
            }
            if (node instanceof SqlIdentifier) {
                SqlIdentifier identifier = (SqlIdentifier) node;
                if (!identifier.isStar()) {
                    columns.add(removeSimpleIdentifierQuotes(identifier.toString()));
                }
                return;
            }
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                if (!operands.isEmpty()) {
                    collectCalciteColumnReferences(operands.get(0), columns);
                }
                return;
            }
            if (node instanceof SqlCall) {
                for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                    collectCalciteColumnReferences(operand, columns);
                }
            }
        }

        private List<String> collectCalciteFunctionNames(SqlNode node) {
            LinkedHashSet<String> names = new LinkedHashSet<String>();
            collectCalciteFunctionNames(node, names);
            return new ArrayList<String>(names);
        }

        private void collectCalciteFunctionNames(SqlNode node, Set<String> names) {
            if (node == null || names == null) {
                return;
            }
            if (node instanceof SqlBasicCall) {
                SqlBasicCall call = (SqlBasicCall) node;
                if (call.getKind() == SqlKind.AS) {
                    List<SqlNode> operands = call.getOperandList();
                    if (!operands.isEmpty()) {
                        collectCalciteFunctionNames(operands.get(0), names);
                    }
                    return;
                }
                String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
                if (StringUtils.hasText(functionName)) {
                    names.add(functionName.toUpperCase(Locale.ROOT));
                }
            }
            if (node instanceof SqlCall) {
                for (SqlNode operand : ((SqlCall) node).getOperandList()) {
                    collectCalciteFunctionNames(operand, names);
                }
            }
        }

        private void collectJoin(SqlJoin join, ParsedSqlProfile profile) {
            profile.joinCount++;
            if (join.getJoinType() != null) {
                profile.joinTypes.add(join.getJoinType().name());
            }
            profile.recordJoin(join.getLeft(), join);
            collectFrom(join.getLeft(), profile);
            collectFrom(join.getRight(), profile);
            if (join.getCondition() != null) {
                int predicateCount = countCalcitePredicates(join.getCondition().toString());
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordPredicate(
                    "JOIN_ON",
                    join.getCondition(),
                    collectCalciteColumnReferences(join.getCondition()),
                    collectCalciteFunctionNames(join.getCondition()),
                    "AND",
                    ""
                );
                profile.recordExpression(join.getCondition());
                collectExpression(join.getCondition(), profile);
            }
        }

        private void collectIdentifier(SqlIdentifier identifier, ParsedSqlProfile profile) {
            if (identifier != null && !identifier.isStar()) {
                profile.projectedColumns.add(identifier.toString());
            }
        }

        private void collectPredicate(String clause, SqlNode predicate, ParsedSqlProfile profile) {
            String predicateSql = predicate.toString();
            profile.predicateCount += countCalcitePredicates(predicateSql);
            profile.datePredicateColumns.addAll(extractCalciteDatePredicateColumns(predicateSql));
            recordCalcitePredicateNode(clause, predicate, profile, "AND", null, new int[] {0});
            profile.recordExpression(predicateSql);
            collectExpression(predicate, profile);
        }

        private void recordCalcitePredicateNode(String clause,
                                                SqlNode predicate,
                                                ParsedSqlProfile profile,
                                                String logicalContext,
                                                String groupId,
                                                int[] groupCounter) {
            if (predicate == null) {
                return;
            }
            if (predicate instanceof SqlBasicCall && predicate.getKind() == SqlKind.AND) {
                for (SqlNode operand : ((SqlBasicCall) predicate).getOperandList()) {
                    recordCalcitePredicateNode(clause, operand, profile, logicalContext, groupId, groupCounter);
                }
                return;
            }
            if (predicate instanceof SqlBasicCall && predicate.getKind() == SqlKind.OR) {
                String currentGroupId = StringUtils.hasText(groupId)
                    ? groupId
                    : nextCalcitePredicateGroupId(clause, groupCounter);
                for (SqlNode operand : ((SqlBasicCall) predicate).getOperandList()) {
                    recordCalcitePredicateNode(clause, operand, profile, "OR", currentGroupId, groupCounter);
                }
                return;
            }
            profile.recordPredicate(
                clause,
                predicate,
                collectCalciteColumnReferences(predicate),
                collectCalciteFunctionNames(predicate),
                logicalContext,
                groupId
            );
        }

        private String nextCalcitePredicateGroupId(String clause, int[] groupCounter) {
            String prefix = StringUtils.hasText(clause) ? clause.trim().toUpperCase(Locale.ROOT) : "PREDICATE";
            prefix = prefix.replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
            if (!StringUtils.hasText(prefix)) {
                prefix = "PREDICATE";
            }
            groupCounter[0]++;
            return prefix + "_OR_" + groupCounter[0];
        }

        private void collectOrderList(SqlNodeList orderList, ParsedSqlProfile profile) {
            if (orderList == null) {
                return;
            }
            profile.orderByCount += orderList.size();
            profile.orderByExpressionCount += orderList.size();
            for (SqlNode item : orderList.getList()) {
                profile.recordOrderBy(item, collectCalciteColumnReferences(item));
                profile.recordOrderByKey(item);
                profile.recordExpression(item);
                if (isRandomOrder(item)) {
                    profile.randomOrderCount++;
                }
                collectExpression(item, profile);
            }
        }

        private void collectExpression(SqlNode node, ParsedSqlProfile profile) {
            collect(node, profile, false);
        }

        private void collectCall(SqlBasicCall call, ParsedSqlProfile profile) {
            SqlKind kind = call.getKind();
            if (kind == SqlKind.AS) {
                List<SqlNode> operands = call.getOperandList();
                if (!operands.isEmpty()) {
                    if (isCalciteSelectLike(operands.get(0)) || operands.get(0).getKind() == SqlKind.SCALAR_QUERY) {
                        recordSubqueryOperand(operands.get(0), profile, "EXPRESSION", normalizeCalciteIdentifier(
                            operands.size() > 1 ? operands.get(1) : null
                        ), true);
                        return;
                    }
                    collectExpression(operands.get(0), profile);
                }
                return;
            }
            if (kind == SqlKind.SCALAR_QUERY) {
                recordSubqueryOperand(call, profile, "EXPRESSION", "", true);
                return;
            }
            if (kind == SqlKind.OR) {
                profile.orPredicateCount++;
            }
            if (kind == SqlKind.LIKE && isLeadingWildcardCalciteLike(call)) {
                profile.leadingWildcardLikeCount++;
            }
            if (kind == SqlKind.NOT && call.toString().toUpperCase(Locale.ROOT).contains("NOT EXISTS")) {
                profile.notExistsCount++;
            }
            if (isComparisonKind(kind) && hasCalciteFunctionWrappedOperand(call)) {
                profile.functionWrappedPredicateCount++;
                profile.recordFunctionWrappedPredicate(call);
            }
            if (kind == SqlKind.OVER) {
                profile.windowFunctionCount++;
            }
            recordCalciteFunction(call, profile);
            collectGenericCall(call, profile);
        }

        private void recordSubqueryOperand(SqlNode node,
                                           ParsedSqlProfile profile,
                                           String location,
                                           String alias,
                                           boolean scalar) {
            SqlNode query = unwrapScalarQuery(node);
            if (query == null) {
                return;
            }
            profile.subqueryCount++;
            if (scalar) {
                profile.scalarSubqueryCount++;
            }
            profile.recordSubquery(query);
            profile.updateNestedSubqueryDepth();
            boolean correlated = profile.referencesVisibleAlias(query.toString());
            profile.recordSubqueryNode(query, location, alias, correlated);
            if (correlated) {
                profile.correlatedSubqueryCount++;
            }
            profile.subqueryDepth++;
            try {
                collect(query, profile, true);
            } finally {
                profile.subqueryDepth--;
            }
        }

        private SqlNode unwrapScalarQuery(SqlNode node) {
            if (node instanceof SqlBasicCall && node.getKind() == SqlKind.SCALAR_QUERY) {
                List<SqlNode> operands = ((SqlBasicCall) node).getOperandList();
                return operands.isEmpty() ? null : operands.get(0);
            }
            return isCalciteSelectLike(node) ? node : null;
        }

        private void collectGenericCall(SqlCall call, ParsedSqlProfile profile) {
            if (call.getKind() == SqlKind.UNION || call.getKind() == SqlKind.INTERSECT || call.getKind() == SqlKind.EXCEPT) {
                profile.setOperation = true;
            }
            for (SqlNode operand : call.getOperandList()) {
                if (isCalciteSelectLike(operand) || (operand != null && operand.getKind() == SqlKind.SCALAR_QUERY)) {
                    boolean scalar = operand != null && operand.getKind() == SqlKind.SCALAR_QUERY;
                    recordSubqueryOperand(
                        operand,
                        profile,
                        "EXPRESSION",
                        "",
                        scalar
                    );
                    continue;
                }
                collect(operand, profile, false);
            }
        }

        private void recordCalciteFunction(SqlBasicCall call, ParsedSqlProfile profile) {
            String functionName = call.getOperator() == null ? "" : call.getOperator().getName();
            if (!StringUtils.hasText(functionName)) {
                return;
            }
            String upperName = functionName.toUpperCase(Locale.ROOT);
            profile.recordFunctionSignal(call, collectCalciteColumnReferences(call));
            if (AGGREGATE_FUNCTIONS.contains(upperName)) {
                profile.aggregateFunctions.add(upperName);
                profile.aggregateFunctionCount++;
                if (STRING_AGGREGATE_FUNCTIONS.contains(upperName)) {
                    profile.largeStringAggregateCount++;
                }
            } else if (looksLikeScalarFunction(call) && !BUILT_IN_SCALAR_FUNCTIONS.contains(upperName)) {
                profile.udfFunctions.add(upperName);
            }
            if (STRING_CONCAT_FUNCTIONS.contains(upperName)) {
                profile.stringConcatenationCount++;
            }
        }

        private boolean looksLikeScalarFunction(SqlBasicCall call) {
            SqlKind kind = call.getKind();
            return kind == SqlKind.OTHER_FUNCTION || kind == SqlKind.OTHER || kind == SqlKind.CAST;
        }

        private boolean isStar(SqlNode node) {
            if (node instanceof SqlIdentifier) {
                return ((SqlIdentifier) node).isStar();
            }
            return node != null && "*".equals(node.toString().trim());
        }

        private boolean isLeadingWildcardCalciteLike(SqlBasicCall call) {
            List<SqlNode> operands = call.getOperandList();
            if (operands.size() < 2 || operands.get(1) == null) {
                return false;
            }
            String right = operands.get(1).toString().trim();
            return right.startsWith("'%") || right.startsWith("\"%");
        }

        private boolean hasCalciteFunctionWrappedOperand(SqlBasicCall call) {
            for (SqlNode operand : call.getOperandList()) {
                if (operand instanceof SqlBasicCall && ((SqlBasicCall) operand).getKind() != SqlKind.OTHER) {
                    String operatorName = ((SqlBasicCall) operand).getOperator() == null
                        ? ""
                        : ((SqlBasicCall) operand).getOperator().getName();
                    if (StringUtils.hasText(operatorName) && !isComparisonKind(((SqlBasicCall) operand).getKind())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean isComparisonKind(SqlKind kind) {
            return kind == SqlKind.EQUALS
                || kind == SqlKind.NOT_EQUALS
                || kind == SqlKind.LESS_THAN
                || kind == SqlKind.LESS_THAN_OR_EQUAL
                || kind == SqlKind.GREATER_THAN
                || kind == SqlKind.GREATER_THAN_OR_EQUAL;
        }

        private boolean isRandomOrder(SqlNode node) {
            String text = node == null ? "" : node.toString().toUpperCase(Locale.ROOT);
            return text.contains("RAND(") || text.contains("RANDOM(");
        }

        private boolean isCalciteStringProjection(SqlNode node) {
            if (node == null) {
                return false;
            }
            String text = node.toString();
            String upperText = text.toUpperCase(Locale.ROOT);
            if (upperText.contains("||") || upperText.contains("CONCAT(") || upperText.contains("GROUP_CONCAT(")
                || upperText.contains("STRING_AGG(") || upperText.contains("LISTAGG(")) {
                return true;
            }
            if (node instanceof SqlIdentifier) {
                String[] parts = text.split("\\.");
                return parts.length > 0 && isStringLikeProjectionKey(parts[parts.length - 1]);
            }
            return false;
        }

        private int countCalcitePredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        private List<String> extractCalciteDatePredicateColumns(String expressionSql) {
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
    }

    private static final class TrinoProfileVisitor extends AstVisitor<Void, ParsedSqlProfile> {

        @Override
        protected Void visitNode(Node node, ParsedSqlProfile profile) {
            for (Node child : node.getChildren()) {
                process(child, profile);
            }
            return null;
        }

        @Override
        protected Void visitQuery(Query node, ParsedSqlProfile profile) {
            if (node.getWith().isPresent()) {
                process(node.getWith().get(), profile);
            }
            process(node.getQueryBody(), profile);
            if (node.getOrderBy().isPresent()) {
                List<io.trino.sql.tree.SortItem> sortItems = node.getOrderBy().get().getSortItems();
                profile.orderByCount += sortItems.size();
                profile.orderByExpressionCount += sortItems.size();
                for (io.trino.sql.tree.SortItem sortItem : sortItems) {
                    profile.recordOrderByKey(sortItem.getSortKey());
                }
                profile.recordExpression(node.getOrderBy().get());
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitQuerySpecification(QuerySpecification node, ParsedSqlProfile profile) {
            profile.projectionCount += node.getSelect().getSelectItems().size();
            process(node.getSelect(), profile);
            if (node.getFrom().isPresent()) {
                process(node.getFrom().get(), profile);
            }
            if (node.getWhere().isPresent()) {
                String whereSql = node.getWhere().get().toString();
                profile.predicateCount += countTrinoPredicates(whereSql);
                profile.datePredicateColumns.addAll(extractTrinoDatePredicateColumns(whereSql));
                profile.recordExpression(whereSql);
                process(node.getWhere().get(), profile);
            }
            if (node.getGroupBy().isPresent()) {
                List<io.trino.sql.tree.GroupingElement> groupingElements = node.getGroupBy().get().getGroupingElements();
                profile.groupByCount += groupingElements.size();
                for (io.trino.sql.tree.GroupingElement groupingElement : groupingElements) {
                    profile.recordGroupByKey(groupingElement);
                }
                profile.recordExpression(node.getGroupBy().get());
                process(node.getGroupBy().get(), profile);
            }
            if (node.getHaving().isPresent()) {
                profile.predicateCount += countTrinoPredicates(node.getHaving().get().toString());
                profile.recordExpression(node.getHaving().get());
                process(node.getHaving().get(), profile);
            }
            if (node.getOrderBy().isPresent()) {
                List<io.trino.sql.tree.SortItem> sortItems = node.getOrderBy().get().getSortItems();
                profile.orderByCount += sortItems.size();
                profile.orderByExpressionCount += sortItems.size();
                for (io.trino.sql.tree.SortItem sortItem : sortItems) {
                    profile.recordOrderByKey(sortItem.getSortKey());
                }
                profile.recordExpression(node.getOrderBy().get());
                process(node.getOrderBy().get(), profile);
            }
            if (node.getLimit().isPresent()) {
                profile.limitPresent = true;
            }
            return null;
        }

        @Override
        protected Void visitTable(Table node, ParsedSqlProfile profile) {
            profile.tables.add(node.getName().toString());
            profile.recordTableScan(node.getName().toString());
            return null;
        }

        @Override
        protected Void visitJoin(io.trino.sql.tree.Join node, ParsedSqlProfile profile) {
            profile.joinCount++;
            profile.joinTypes.add(node.getType().name());
            if (node.getCriteria().isPresent()) {
                JoinCriteria criteria = node.getCriteria().get();
                String criteriaSql = criteria.toString();
                int predicateCount = countTrinoPredicates(criteriaSql);
                profile.predicateCount += predicateCount;
                profile.joinCriteriaCount += predicateCount;
                profile.recordExpression(criteriaSql);
            }
            process(node.getLeft(), profile);
            process(node.getRight(), profile);
            return null;
        }

        @Override
        protected Void visitSingleColumn(SingleColumn node, ParsedSqlProfile profile) {
            if (isTrinoStringProjection(node.getExpression())) {
                profile.stringProjectionCount++;
            }
            profile.recordExpression(node.getExpression());
            process(node.getExpression(), profile);
            return null;
        }

        @Override
        protected Void visitAllColumns(io.trino.sql.tree.AllColumns node, ParsedSqlProfile profile) {
            profile.selectStar = true;
            profile.recordSelectStar(node);
            return null;
        }

        @Override
        protected Void visitFunctionCall(FunctionCall node, ParsedSqlProfile profile) {
            QualifiedName name = node.getName();
            String functionName = name == null ? "" : name.toString().toUpperCase(Locale.ROOT);
            if (AGGREGATE_FUNCTIONS.contains(functionName)) {
                profile.aggregateFunctions.add(functionName);
                profile.aggregateFunctionCount++;
                if (STRING_AGGREGATE_FUNCTIONS.contains(functionName)) {
                    profile.largeStringAggregateCount++;
                }
            } else if (!BUILT_IN_SCALAR_FUNCTIONS.contains(functionName)) {
                profile.udfFunctions.add(functionName);
            }
            if (STRING_CONCAT_FUNCTIONS.contains(functionName)) {
                profile.stringConcatenationCount++;
            }
            if (node.getWindow().isPresent()) {
                profile.windowFunctionCount++;
            }
            for (io.trino.sql.tree.Expression argument : node.getArguments()) {
                process(argument, profile);
            }
            return null;
        }

        @Override
        protected Void visitDereferenceExpression(DereferenceExpression node, ParsedSqlProfile profile) {
            profile.projectedColumns.add(node.toString());
            return null;
        }

        @Override
        protected Void visitSubqueryExpression(io.trino.sql.tree.SubqueryExpression node, ParsedSqlProfile profile) {
            profile.subqueryCount++;
            profile.recordSubquery(node);
            return visitNode(node, profile);
        }

        private boolean isTrinoStringProjection(io.trino.sql.tree.Expression expression) {
            if (expression == null) {
                return false;
            }
            String text = expression.toString();
            String upperText = text.toUpperCase(Locale.ROOT);
            if (upperText.contains("||") || upperText.contains("CONCAT(") || upperText.contains("GROUP_CONCAT(")
                || upperText.contains("STRING_AGG(") || upperText.contains("LISTAGG(")) {
                return true;
            }
            String[] parts = text.split("\\.");
            return parts.length > 0 && isStringLikeProjectionKey(parts[parts.length - 1]);
        }

        private static int countTrinoPredicates(String expressionSql) {
            if (expressionSql == null || expressionSql.trim().isEmpty()) {
                return 0;
            }
            String normalized = expressionSql.toUpperCase(Locale.ROOT);
            int count = 1;
            Matcher matcher = Pattern.compile("\\bAND\\b|\\bOR\\b").matcher(normalized);
            while (matcher.find()) {
                count++;
            }
            return count;
        }

        private static List<String> extractTrinoDatePredicateColumns(String expressionSql) {
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
    }

    public static final class ParsedSqlProfile {

        private final String normalizedSql;
        private final List<String> tables = new ArrayList<String>();
        private final Set<String> projectedColumns = new LinkedHashSet<String>();
        private final Set<String> aggregateFunctions = new LinkedHashSet<String>();
        private final Set<String> udfFunctions = new LinkedHashSet<String>();
        private final Set<String> datePredicateColumns = new LinkedHashSet<String>();
        private final Set<String> selectStarItems = new LinkedHashSet<String>();
        private final Set<String> functionWrappedPredicateExpressions = new LinkedHashSet<String>();
        private final Set<String> joinTypes = new LinkedHashSet<String>();
        private final List<String> warnings = new ArrayList<String>();
        private final LinkedHashMap<String, Integer> expressionFrequency = new LinkedHashMap<String, Integer>();
        private final LinkedHashMap<String, Integer> tableScanFrequency = new LinkedHashMap<String, Integer>();
        private final LinkedHashMap<String, Integer> groupByKeyFrequency = new LinkedHashMap<String, Integer>();
        private final LinkedHashMap<String, Integer> orderByKeyFrequency = new LinkedHashMap<String, Integer>();
        private final LinkedHashMap<String, Integer> subqueryFrequency = new LinkedHashMap<String, Integer>();
        private final Deque<Set<String>> aliasScopes = new ArrayDeque<Set<String>>();
        private final AdvancedStructureProfile advancedStructureProfile = new AdvancedStructureProfile();
        private String parserEngine = "APACHE_CALCITE";
        private int projectionCount;
        private int predicateCount;
        private int joinCount;
        private int joinCriteriaCount;
        private int groupByCount;
        private int orderByCount;
        private int orderByExpressionCount;
        private int duplicateOrderByKeyCount;
        private int duplicateGroupByKeyCount;
        private int aggregateFunctionCount;
        private int stringProjectionCount;
        private int stringConcatenationCount;
        private int largeStringAggregateCount;
        private int repeatedSubqueryCount;
        private int windowFunctionCount;
        private int subqueryCount;
        private int scalarSubqueryCount;
        private int nestedSubqueryDepth;
        private int correlatedSubqueryCount;
        private int orPredicateCount;
        private int functionWrappedPredicateCount;
        private int leadingWildcardLikeCount;
        private int randomOrderCount;
        private int notExistsCount;
        private int repeatedTableScanCount;
        private int subqueryDepth;
        private int repeatedExpressionCount;
        private boolean selectStar;
        private boolean limitPresent;
        private boolean distinctPresent;
        private boolean setOperation;
        private RewriteOutcome rewriteOutcome = RewriteOutcome.empty();

        private ParsedSqlProfile(String normalizedSql) {
            this.normalizedSql = normalizedSql;
        }

        private Map<String, Object> toAstProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("statementType", "SELECT");
            payload.put("parserEngine", parserEngine);
            payload.put("tables", tables);
            payload.put("projectionCount", Integer.valueOf(projectionCount));
            payload.put("projectedColumns", new ArrayList<String>(projectedColumns));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("joinTypes", new ArrayList<String>(joinTypes));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("orderByCount", Integer.valueOf(orderByCount));
            payload.put("orderByExpressionCount", Integer.valueOf(orderByExpressionCount));
            payload.put("duplicateOrderByKeyCount", Integer.valueOf(duplicateOrderByKeyCount));
            payload.put("duplicateGroupByKeyCount", Integer.valueOf(duplicateGroupByKeyCount));
            payload.put("groupByWithoutAggregate", Boolean.valueOf(isGroupByWithoutAggregate()));
            payload.put("aggregateFunctionCount", Integer.valueOf(aggregateFunctionCount));
            payload.put("stringProjectionCount", Integer.valueOf(stringProjectionCount));
            payload.put("stringConcatenationCount", Integer.valueOf(stringConcatenationCount));
            payload.put("largeStringAggregateCount", Integer.valueOf(largeStringAggregateCount));
            payload.put("repeatedSubqueryCount", Integer.valueOf(repeatedSubqueryCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("scalarSubqueryCount", Integer.valueOf(scalarSubqueryCount));
            payload.put("nestedSubqueryDepth", Integer.valueOf(nestedSubqueryDepth));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("functionWrappedPredicateCount", Integer.valueOf(functionWrappedPredicateCount));
            payload.put("leadingWildcardLikeCount", Integer.valueOf(leadingWildcardLikeCount));
            payload.put("randomOrderCount", Integer.valueOf(randomOrderCount));
            payload.put("notExistsCount", Integer.valueOf(notExistsCount));
            payload.put("repeatedTableScanCount", Integer.valueOf(repeatedTableScanCount));
            payload.put("repeatedExpressionCount", Integer.valueOf(repeatedExpressionCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("selectStarItems", new ArrayList<String>(selectStarItems));
            payload.put("functionWrappedPredicateExpressions", new ArrayList<String>(functionWrappedPredicateExpressions));
            payload.put("limitPresent", Boolean.valueOf(limitPresent));
            payload.put("distinctPresent", Boolean.valueOf(distinctPresent));
            payload.put("setOperation", Boolean.valueOf(setOperation));
            payload.put("advancedStructureProfile", toAdvancedStructureProfile());
            return payload;
        }

        private Map<String, Object> toAccelerationSignalProfile() {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("tables", tables);
            payload.put("parserEngine", parserEngine);
            payload.put("joinCount", Integer.valueOf(joinCount));
            payload.put("predicateCount", Integer.valueOf(predicateCount));
            payload.put("groupByCount", Integer.valueOf(groupByCount));
            payload.put("aggregateFunctionCount", Integer.valueOf(aggregateFunctionCount));
            payload.put("stringProjectionCount", Integer.valueOf(stringProjectionCount));
            payload.put("largeStringAggregateCount", Integer.valueOf(largeStringAggregateCount));
            payload.put("windowFunctionCount", Integer.valueOf(windowFunctionCount));
            payload.put("udfFunctions", new ArrayList<String>(udfFunctions));
            payload.put("subqueryCount", Integer.valueOf(subqueryCount));
            payload.put("repeatedSubqueryCount", Integer.valueOf(repeatedSubqueryCount));
            payload.put("correlatedSubqueryCount", Integer.valueOf(correlatedSubqueryCount));
            payload.put("orPredicateCount", Integer.valueOf(orPredicateCount));
            payload.put("aggregateFunctions", new ArrayList<String>(aggregateFunctions));
            payload.put("datePredicateColumns", new ArrayList<String>(datePredicateColumns));
            payload.put("selectStar", Boolean.valueOf(selectStar));
            payload.put("selectStarItems", new ArrayList<String>(selectStarItems));
            payload.put("functionWrappedPredicateExpressions", new ArrayList<String>(functionWrappedPredicateExpressions));
            payload.put("warnings", warnings);
            payload.put("advancedStructureProfile", toAdvancedStructureProfile());
            return payload;
        }

        public Map<String, Object> toAdvancedStructureProfile() {
            return advancedStructureProfile.toMap(parserEngine);
        }

        public List<String> getTables() {
            return new ArrayList<String>(tables);
        }

        public String getParserEngine() {
            return parserEngine;
        }

        public String getNormalizedSql() {
            return normalizedSql;
        }

        private RewriteOutcome getRewriteOutcome() {
            return rewriteOutcome == null ? RewriteOutcome.empty() : rewriteOutcome;
        }

        public Set<String> getAggregateFunctions() {
            return new LinkedHashSet<String>(aggregateFunctions);
        }

        public List<String> getProjectedColumns() {
            return new ArrayList<String>(projectedColumns);
        }

        public Set<String> getDatePredicateColumns() {
            return new LinkedHashSet<String>(datePredicateColumns);
        }

        public List<String> getSelectStarItems() {
            return new ArrayList<String>(selectStarItems);
        }

        public List<String> getFunctionWrappedPredicateExpressions() {
            return new ArrayList<String>(functionWrappedPredicateExpressions);
        }

        public List<String> getWarnings() {
            return new ArrayList<String>(warnings);
        }

        public int getProjectionCount() {
            return projectionCount;
        }

        public int getPredicateCount() {
            return predicateCount;
        }

        public int getJoinCount() {
            return joinCount;
        }

        public int getJoinCriteriaCount() {
            return joinCriteriaCount;
        }

        public int getGroupByCount() {
            return groupByCount;
        }

        public int getOrderByCount() {
            return orderByCount;
        }

        public int getOrderByExpressionCount() {
            return orderByExpressionCount;
        }

        public int getDuplicateOrderByKeyCount() {
            return duplicateOrderByKeyCount;
        }

        public int getDuplicateGroupByKeyCount() {
            return duplicateGroupByKeyCount;
        }

        public boolean isGroupByWithoutAggregate() {
            return groupByCount > 0 && aggregateFunctionCount == 0;
        }

        public int getAggregateFunctionCount() {
            return aggregateFunctionCount;
        }

        public int getStringProjectionCount() {
            return stringProjectionCount;
        }

        public int getStringConcatenationCount() {
            return stringConcatenationCount;
        }

        public int getLargeStringAggregateCount() {
            return largeStringAggregateCount;
        }

        public int getRepeatedSubqueryCount() {
            return repeatedSubqueryCount;
        }

        public int getWindowFunctionCount() {
            return windowFunctionCount;
        }

        public int getUdfFunctionCount() {
            return udfFunctions.size();
        }

        public int getSubqueryCount() {
            return subqueryCount;
        }

        public int getScalarSubqueryCount() {
            return scalarSubqueryCount;
        }

        public int getNestedSubqueryDepth() {
            return nestedSubqueryDepth;
        }

        public int getCorrelatedSubqueryCount() {
            return correlatedSubqueryCount;
        }

        public int getOrPredicateCount() {
            return orPredicateCount;
        }

        public int getFunctionWrappedPredicateCount() {
            return functionWrappedPredicateCount;
        }

        public int getLeadingWildcardLikeCount() {
            return leadingWildcardLikeCount;
        }

        public int getRandomOrderCount() {
            return randomOrderCount;
        }

        public int getNotExistsCount() {
            return notExistsCount;
        }

        public int getRepeatedTableScanCount() {
            return repeatedTableScanCount;
        }

        public int getComplexGraphScore() {
            return subqueryCount + joinCount + orPredicateCount + functionWrappedPredicateCount + randomOrderCount
                + repeatedSubqueryCount + largeStringAggregateCount;
        }

        public int getRepeatedExpressionCount() {
            return repeatedExpressionCount;
        }

        public boolean isSelectStar() {
            return selectStar;
        }

        public boolean isLimitPresent() {
            return limitPresent;
        }

        public boolean isDistinctPresent() {
            return distinctPresent;
        }

        public boolean isSetOperation() {
            return setOperation;
        }

        public List<String> getJoinTypes() {
            return new ArrayList<String>(joinTypes);
        }

        private void markAdvancedProfileAvailable() {
            advancedStructureProfile.markAvailable();
        }

        private void markAdvancedProfilePartial() {
            advancedStructureProfile.markPartial();
        }

        private void recordCte(SqlWithItem withItem) {
            advancedStructureProfile.recordCte(withItem);
        }

        private void recordTable(String tableName, String alias, String sourceType) {
            advancedStructureProfile.recordTable(tableName, alias, sourceType);
        }

        private void recordProjection(SqlNode selectItem, List<String> sourceColumns) {
            advancedStructureProfile.recordProjection(selectItem, sourceColumns);
        }

        private void recordPredicate(String clause,
                                     SqlNode predicate,
                                     List<String> sourceColumns,
                                     List<String> functionNames,
                                     String logicalContext,
                                     String groupId) {
            advancedStructureProfile.recordPredicate(
                clause,
                predicate,
                sourceColumns,
                functionNames,
                logicalContext,
                groupId
            );
        }

        private void recordJoin(SqlNode leftItem, SqlJoin join) {
            advancedStructureProfile.recordJoin(leftItem, join);
        }

        private void recordGroupBy(SqlNode expression, List<String> sourceColumns) {
            advancedStructureProfile.recordGroupBy(expression, sourceColumns);
        }

        private void recordOrderBy(SqlNode orderByElement, List<String> sourceColumns) {
            advancedStructureProfile.recordOrderBy(orderByElement, sourceColumns);
        }

        private void recordLimit(SqlNode fetch, SqlNode offset) {
            advancedStructureProfile.recordLimit(fetch, offset);
        }

        private void recordSubqueryNode(SqlNode subSelect, String location, String alias, boolean correlated) {
            advancedStructureProfile.recordSubquery(subSelect, location, alias, subqueryDepth + 1, correlated);
        }

        private void recordFunctionSignal(SqlBasicCall function, List<String> sourceColumns) {
            advancedStructureProfile.recordFunctionSignal(function, sourceColumns);
        }

        private void recordSqlLevelFunctions(String sql) {
            advancedStructureProfile.recordSqlLevelFunctions(sql);
        }

        private void recordExpression(Object expression) {
            if (expression == null) {
                return;
            }
            String key = expression.toString().trim().toUpperCase(Locale.ROOT);
            if (key.length() < 4 || isSimpleExpressionReference(expression, key)) {
                return;
            }
            Integer current = expressionFrequency.get(key);
            expressionFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        private void recordSelectStar(Object selectItem) {
            String item = normalizeProfileText(selectItem);
            selectStarItems.add(item.isEmpty() ? "*" : item);
        }

        private void recordFunctionWrappedPredicate(Object expression) {
            String item = normalizeProfileText(expression);
            if (!item.isEmpty()) {
                functionWrappedPredicateExpressions.add(item);
            }
        }

        private boolean isSimpleExpressionReference(Object expression, String key) {
            if (expression instanceof SqlIdentifier
                || expression instanceof DereferenceExpression
                || expression instanceof SqlLiteral) {
                return true;
            }
            return key.matches("[A-Z_][A-Z0-9_]*(\\.[A-Z_][A-Z0-9_]*)*")
                || key.matches("\"[^\"]+\"(\\.\"[^\"]+\")*")
                || key.matches("`[^`]+`(\\.`[^`]+`)*");
        }

        private void recalculateRepeatedExpressions() {
            int repeated = 0;
            for (Integer count : expressionFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeated += count.intValue() - 1;
                }
            }
            repeatedExpressionCount = repeated;
            repeatedTableScanCount = 0;
            for (Integer count : tableScanFrequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeatedTableScanCount += count.intValue() - 1;
                }
            }
            duplicateGroupByKeyCount = repeatedCount(groupByKeyFrequency);
            duplicateOrderByKeyCount = repeatedCount(orderByKeyFrequency);
            repeatedSubqueryCount = repeatedCount(subqueryFrequency);
        }

        private void recordTableScan(String table) {
            if (table == null) {
                return;
            }
            String key = table.trim().toUpperCase(Locale.ROOT);
            if (key.isEmpty()) {
                return;
            }
            Integer current = tableScanFrequency.get(key);
            tableScanFrequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        private void recordGroupByKey(Object expression) {
            recordFrequency(groupByKeyFrequency, normalizeProfileKey(expression));
        }

        private void recordOrderByKey(Object expression) {
            recordFrequency(orderByKeyFrequency, normalizeProfileKey(expression));
        }

        private void recordSubquery(Object subquery) {
            recordFrequency(subqueryFrequency, normalizeProfileKey(subquery));
        }

        private List<String> getRepeatedSubquerySamples() {
            List<String> samples = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : subqueryFrequency.entrySet()) {
                if (entry.getValue() != null && entry.getValue().intValue() > 1) {
                    samples.add(entry.getKey());
                }
            }
            return samples;
        }

        private void recordFrequency(Map<String, Integer> frequency, String key) {
            if (frequency == null || key == null || key.isEmpty()) {
                return;
            }
            Integer current = frequency.get(key);
            frequency.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        private int repeatedCount(Map<String, Integer> frequency) {
            int repeated = 0;
            for (Integer count : frequency.values()) {
                if (count != null && count.intValue() > 1) {
                    repeated += count.intValue() - 1;
                }
            }
            return repeated;
        }

        private String normalizeProfileKey(Object value) {
            if (value == null) {
                return "";
            }
            return normalizeProfileText(value).toUpperCase(Locale.ROOT);
        }

        private String normalizeProfileText(Object value) {
            if (value == null) {
                return "";
            }
            return value.toString()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        }

        private void pushAliasScope(Set<String> aliases) {
            aliasScopes.push(aliases == null ? Collections.<String>emptySet() : aliases);
        }

        private void popAliasScope() {
            if (!aliasScopes.isEmpty()) {
                aliasScopes.pop();
            }
        }

        private boolean referencesVisibleAlias(String sql) {
            if (sql == null || aliasScopes.isEmpty()) {
                return false;
            }
            String normalized = removeSimpleIdentifierQuotes(sql).toUpperCase(Locale.ROOT);
            for (Set<String> scope : aliasScopes) {
                for (String alias : scope) {
                    if (alias != null && !alias.isEmpty() && normalized.contains(alias + ".")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void updateNestedSubqueryDepth() {
            nestedSubqueryDepth = Math.max(nestedSubqueryDepth, subqueryDepth + 1);
        }
    }

    private static final class AdvancedStructureProfile {

        private String profileStatus = "PARTIAL";
        private final Set<String> cteNames = new LinkedHashSet<String>();
        private final List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> projections = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> predicates = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> joinGraph = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> aggregations = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> groupBy = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> orderBy = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> ctes = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> subqueries = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> timeFunctions = new ArrayList<Map<String, Object>>();
        private final List<Map<String, Object>> nonDeterministicFunctions = new ArrayList<Map<String, Object>>();
        private Map<String, Object> limit = Collections.emptyMap();

        private void markAvailable() {
            profileStatus = "AVAILABLE";
        }

        private void markPartial() {
            profileStatus = "PARTIAL";
        }

        private void recordCte(SqlWithItem withItem) {
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

        private void recordTable(String tableName, String alias, String sourceType) {
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

        private void recordProjection(SqlNode selectItem, List<String> sourceColumns) {
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

        private void recordPredicate(String clause,
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

        private void recordJoin(SqlNode leftItem, SqlJoin join) {
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

        private void recordGroupBy(SqlNode expression, List<String> sourceColumns) {
            if (expression == null) {
                return;
            }
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("expression", normalizeText(expression));
            item.put("sourceColumns", safeList(sourceColumns));
            addUnique(groupBy, item);
        }

        private void recordOrderBy(SqlNode orderByElement, List<String> sourceColumns) {
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

        private void recordLimit(SqlNode fetch, SqlNode offset) {
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

        private void recordSubquery(SqlNode subSelect,
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

        private void recordFunctionSignal(SqlBasicCall function, List<String> sourceColumns) {
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

        private void recordSqlLevelFunctions(String sql) {
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

        private Map<String, Object> toMap(String parserEngine) {
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

        private boolean isCteName(String tableName) {
            return StringUtils.hasText(tableName) && cteNames.contains(tableName.toUpperCase(Locale.ROOT));
        }

        private LinkedHashMap<String, Object> functionEntry(String functionName,
                                                            Object expression,
                                                            List<String> sourceColumns) {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("functionName", functionName);
            item.put("expression", normalizeText(expression));
            item.put("sourceColumns", safeList(sourceColumns));
            return item;
        }

        private String projectionAlias(SqlNode selectItem) {
            if (selectItem instanceof SqlBasicCall && selectItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) selectItem).getOperandList();
                if (operands.size() >= 2) {
                    return cleanIdentifier(operands.get(1).toString());
                }
            }
            return "";
        }

        private SqlNode projectionExpression(SqlNode selectItem) {
            if (selectItem instanceof SqlBasicCall && selectItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) selectItem).getOperandList();
                if (!operands.isEmpty()) {
                    return operands.get(0);
                }
            }
            return selectItem;
        }

        private String projectionType(SqlNode selectItem) {
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

        private String predicateType(SqlNode predicate) {
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

        private String relationName(SqlNode fromItem) {
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

        private String relationAlias(SqlNode fromItem) {
            if (fromItem instanceof SqlBasicCall && fromItem.getKind() == SqlKind.AS) {
                List<SqlNode> operands = ((SqlBasicCall) fromItem).getOperandList();
                if (operands.size() >= 2) {
                    return cleanIdentifier(operands.get(1).toString());
                }
            }
            return "";
        }

        private String orderByDirection(SqlNode orderByElement) {
            if (orderByElement == null) {
                return "ASC";
            }
            String text = orderByElement.toString().toUpperCase(Locale.ROOT);
            return text.endsWith(" DESC") || orderByElement.getKind() == SqlKind.DESCENDING ? "DESC" : "ASC";
        }

        private String schemaName(String tableName) {
            if (!StringUtils.hasText(tableName) || !tableName.contains(".")) {
                return "";
            }
            return tableName.substring(0, tableName.lastIndexOf('.'));
        }

        private List<String> nodeListTexts(SqlNodeList nodeList) {
            if (nodeList == null || nodeList.isEmpty()) {
                return Collections.emptyList();
            }
            List<String> result = new ArrayList<String>();
            for (SqlNode node : nodeList.getList()) {
                result.add(normalizeText(node));
            }
            return result;
        }

        private List<String> safeList(List<String> input) {
            if (input == null || input.isEmpty()) {
                return Collections.emptyList();
            }
            return new ArrayList<String>(input);
        }

        private String cleanIdentifier(String value) {
            if (!StringUtils.hasText(value)) {
                return "";
            }
            return value.replace("\"", "").replace("`", "").trim();
        }

        private String normalizeText(Object value) {
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

        private void addUnique(List<Map<String, Object>> target, Map<String, Object> item) {
            if (target != null && item != null && !target.contains(item)) {
                target.add(item);
            }
        }

        private List<Map<String, Object>> immutableList(List<Map<String, Object>> source) {
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

    private static final class RewriteOutcome {

        private final String rewrittenSql;
        private final List<String> appliedRules;

        private RewriteOutcome(String rewrittenSql, List<String> appliedRules) {
            this.rewrittenSql = rewrittenSql;
            this.appliedRules = appliedRules;
        }

        private static RewriteOutcome empty() {
            return new RewriteOutcome("", Collections.<String>emptyList());
        }
    }

    public static final class RecommendationRuleOutputModel {

        private final List<Map<String, Object>> ruleChain;
        private final List<Map<String, Object>> unappliedRules;
        private final List<Map<String, Object>> preconditions;
        private final List<Map<String, Object>> semanticRisks;
        private final Map<String, Object> expectedBenefit;
        private final Map<String, Object> estimatedCost;
        private final Integer confidence;
        private final String validationMethod;
        private final boolean autoApplyAllowed;
        private final boolean manualReviewRequired;

        private RecommendationRuleOutputModel(List<Map<String, Object>> ruleChain,
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

        private static Map<String, Object> immutableMapCopy(Map<String, Object> value) {
            if (value == null || value.isEmpty()) {
                return Collections.emptyMap();
            }
            return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(value));
        }

        private static List<Map<String, Object>> immutableListCopy(List<Map<String, Object>> value) {
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

        private final Integer line;
        private final Integer column;
        private final Integer offset;
        private final String token;
        private final String snippet;

        private SqlFailurePosition(Integer line, Integer column, Integer offset, String token, String snippet) {
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

        private final int code;
        private final String suggestedAction;
        private final boolean retryable;
        private final OptimizationTaskPhase failedPhase;
        private final List<OptimizationTaskRisk> risks;
        private final SqlFailurePosition failurePosition;

        private SqlOptimizationExecutionException(int code,
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
