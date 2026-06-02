package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.config.RewriteProductionGateProperties;
import com.company.sqloptimization.domain.task.AccelerationSuggestionType;
import com.company.sqloptimization.domain.task.OptimizationTaskArtifact;
import com.company.sqloptimization.domain.task.OptimizationTaskBenefit;
import com.company.sqloptimization.domain.task.OptimizationTaskCost;
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
import io.trino.sql.tree.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SqlOptimizationPipelineService extends SqlOptimizationPipelineRecommendationSupport {

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
        List<String> appliedRules = new ArrayList<String>(outcome.appliedRules);
        String candidateSql = outcome.rewrittenSql;
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

}
