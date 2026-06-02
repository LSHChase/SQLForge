package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.StructureParseFeatureSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIntentProfileVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResourceEstimateVO;
import com.company.sqloptimization.application.controller.vo.StructureParseRiskVO;
import com.company.sqloptimization.domain.parse.StructureParseComplexityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseLogicalObjectHit;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateStatus;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateSummary;
import com.company.sqloptimization.domain.parse.StructureParseResult;
import com.company.sqloptimization.domain.parse.ParseAnalysisStatus;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataClient;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

abstract class StructureParseApplicationServiceSupport {

    protected static final Logger LOGGER = LoggerFactory.getLogger(StructureParseApplicationService.class);
    protected static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    protected static final Pattern VIEW_DEFINITION_BODY_PATTERN = Pattern.compile("(?is)\\bAS\\s+((SELECT|WITH)\\b.*)$");
    protected static final Pattern HEURISTIC_SQL_TYPE_PATTERN =
        Pattern.compile("(?is)^\\s*(?:/\\*.*?\\*/\\s*|--[^\\r\\n]*(?:\\r?\\n|$)\\s*)*(WITH|SELECT|INSERT|UPDATE|DELETE|MERGE)\\b");
    protected static final Pattern HEURISTIC_OBJECT_PATTERN =
        Pattern.compile("(?is)\\b(?:FROM|JOIN)\\s+((?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)(?:\\s*\\.\\s*(?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)){0,2})");
    protected static final Pattern HEURISTIC_JOIN_PATTERN = Pattern.compile("(?i)\\bJOIN\\b");
    protected static final Pattern HEURISTIC_WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");
    protected static final Pattern HEURISTIC_BOOLEAN_PATTERN = Pattern.compile("(?i)\\b(AND|OR)\\b");
    protected static final Pattern HEURISTIC_OR_PATTERN = Pattern.compile("(?i)\\bOR\\b");
    protected static final Pattern HEURISTIC_DATE_PREDICATE_PATTERN =
        Pattern.compile("(?i)([A-Za-z0-9_\\.]*?(DATE|TIME|DT|DAY))\\s*(>=|<=|=|>|<|BETWEEN|IN)");
    protected static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    protected static final String MATCH_SOURCE_SQL = "SQL_TABLE_SCAN";
    protected static final String MATCH_SOURCE_HEURISTIC = "NAME_HEURISTIC";
    protected static final String MATCH_SOURCE_HEURISTIC_FALLBACK = "HEURISTIC_FALLBACK";
    protected static final String MATCH_SOURCE_DB_VIEW_METADATA = "LIVE_DB_VIEW_METADATA";
    protected static final String MATCH_SOURCE_DB_VIEW_DEFINITION = "DB_VIEW_DEFINITION";
    protected static final String MATCH_SOURCE_DB_VIEW_CATALOG_FALLBACK = "DB_VIEW_CATALOG_FALLBACK";
    protected static final String RISK_DB_VIEW_DEFINITION_UNRESOLVED = "DB_VIEW_DEFINITION_UNRESOLVED";
    protected static final String RISK_SQL_SYNTAX_INVALID = "SQL_SYNTAX_INVALID";
    protected static final String RISK_SQL_TOO_LONG = "SQL_TOO_LONG";
    protected static final String HEURISTIC_FALLBACK_ENGINE = "HEURISTIC_FALLBACK";
    protected static final String SQL_TOO_LONG_FAILURE_REASON =
        "SQL 过长；语法解析器已失败或为限制诊断范围而跳过。";
    protected static final String HISTORY_RESULT_SUCCESS = "SUCCESS";
    protected static final String HISTORY_RESULT_FAILED = "FAILED";
    protected static final int MAX_DB_VIEW_EXPANSION_DEPTH = 5;
    protected static final int DEFAULT_SQL_TEXT_LENGTH_LIMIT = 10 * 1024 * 1024;
    protected static final int HEURISTIC_SQL_WINDOW_LIMIT = 64 * 1024;
    protected static final int FAILURE_REASON_TEXT_LIMIT = 512;
    protected static final int FAILURE_DETAIL_TEXT_LIMIT = 512;
    protected static final int FAILURE_TOKEN_TEXT_LIMIT = 64;
    protected static final int FAILURE_SNIPPET_TEXT_LIMIT = 120;

    @Value("${sql-optimization.parser.max-sql-length:10485760}")
    protected int maxSqlTextLength = DEFAULT_SQL_TEXT_LENGTH_LIMIT;

    protected final SqlOptimizationPipelineService sqlOptimizationPipelineService;
    protected final GovernanceCapabilityClient governanceCapabilityClient;
    protected final SqlParseHistoryApplicationService sqlParseHistoryApplicationService;
    protected final DatasourceViewMetadataClient datasourceViewMetadataClient;
    protected final HetuPlanAnalysisClient hetuPlanAnalysisClient;
    protected final ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService;


    public StructureParseApplicationServiceSupport(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            SqlParseHistoryApplicationService sqlParseHistoryApplicationService,
                                            DatasourceViewMetadataClient datasourceViewMetadataClient,
                                            HetuPlanAnalysisClient hetuPlanAnalysisClient,
                                            ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService) {
        this.sqlOptimizationPipelineService = sqlOptimizationPipelineService;
        this.governanceCapabilityClient = governanceCapabilityClient;
        this.sqlParseHistoryApplicationService = sqlParseHistoryApplicationService;
        this.datasourceViewMetadataClient = datasourceViewMetadataClient == null
            ? DatasourceViewMetadataClient.unavailable()
            : datasourceViewMetadataClient;
        this.hetuPlanAnalysisClient = hetuPlanAnalysisClient == null
            ? HetuPlanAnalysisClient.unavailable()
            : hetuPlanAnalysisClient;
        this.parseTriggeredRewriteRecommendationService = parseTriggeredRewriteRecommendationService;
    }

    protected StructureParseComplexityLevel resolveComplexity(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        int score = profile.getJoinCount() * 2
            + profile.getPredicateCount()
            + aggregateShape(profile).getComplexityWeight()
            + profile.getSubqueryCount() * 2
            + profile.getOrPredicateCount()
            + profile.getFunctionWrappedPredicateCount()
            + profile.getRandomOrderCount() * 2
            + profile.getRepeatedSubqueryCount() * 2
            + profile.getStringConcatenationCount()
            + profile.getLargeStringAggregateCount() * 2
            + (profile.isSetOperation() ? 3 : 0);
        if (score >= 10) {
            return StructureParseComplexityLevel.EXTREME;
        }
        if (score >= 6) {
            return StructureParseComplexityLevel.COMPLEX;
        }
        if (score >= 3) {
            return StructureParseComplexityLevel.MODERATE;
        }
        return StructureParseComplexityLevel.SIMPLE;
    }

    protected abstract HeuristicFallbackProfile analyzeHeuristicFallback(String sqlText, boolean sqlTooLong);

    protected void enrichQueryIntent(StructureParseResponseVO response,
                                   String sqlText,
                                   SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        response.setSqlFingerprint(SqlFingerprintUtils.fingerprint(sqlText));
        if (profile == null) {
            HeuristicFallbackProfile heuristicProfile = analyzeHeuristicFallback(
                sqlText,
                containsValue(response == null ? null : response.getRiskTags(), RISK_SQL_TOO_LONG)
            );
            enrichHeuristicQueryIntent(response, heuristicProfile);
            response.setAdvancedStructureProfile(unavailableAdvancedStructureProfile(HEURISTIC_FALLBACK_ENGINE));
            response.setRiskChecklist(Collections.<StructureParseRiskVO>emptyList());
            return;
        }
        String scanMode = resolveScanMode(profile);
        String joinType = resolveJoinType(profile);
        String computeDensity = resolveComputeDensity(profile);
        String resourceType = resolveResourceType(profile, scanMode, computeDensity);
        String slaLevel = resolveSlaLevel(profile, scanMode, computeDensity);
        List<StructureParseRiskVO> risks = buildRiskChecklist(profile);
        int finalTableCount = resolveFinalTableCount(response, profile);

        StructureParseIntentProfileVO intentProfile = new StructureParseIntentProfileVO();
        intentProfile.setScanMode(scanMode);
        intentProfile.setJoinType(joinType);
        intentProfile.setComputeDensity(computeDensity);
        intentProfile.setResourceType(resourceType);
        intentProfile.setSlaLevel(slaLevel);
        intentProfile.setConfidence(profile.getParserEngine().equals("APACHE_CALCITE") ? "MEDIUM" : "HIGH");
        intentProfile.setClassificationLabels(buildClassificationLabels(scanMode, joinType, computeDensity, resourceType, slaLevel));
        response.setIntentProfile(intentProfile);

        StructureParseFeatureSummaryVO featureSummary = new StructureParseFeatureSummaryVO();
        featureSummary.setParserEngine(profile.getParserEngine());
        featureSummary.setScanMode(scanMode);
        featureSummary.setJoinType(joinType);
        featureSummary.setComputeDensity(computeDensity);
        featureSummary.setResourceType(resourceType);
        featureSummary.setSlaLevel(slaLevel);
        featureSummary.setTableCount(Integer.valueOf(finalTableCount));
        featureSummary.setJoinCount(Integer.valueOf(profile.getJoinCount()));
        featureSummary.setPredicateCount(Integer.valueOf(profile.getPredicateCount()));
        featureSummary.setWindowFunctionCount(Integer.valueOf(profile.getWindowFunctionCount()));
        featureSummary.setUdfFunctionCount(Integer.valueOf(profile.getUdfFunctionCount()));
        featureSummary.setRepeatedExpressionCount(Integer.valueOf(profile.getRepeatedExpressionCount()));
        featureSummary.setSubqueryCount(Integer.valueOf(profile.getSubqueryCount()));
        featureSummary.setScalarSubqueryCount(Integer.valueOf(profile.getScalarSubqueryCount()));
        featureSummary.setNestedSubqueryDepth(Integer.valueOf(profile.getNestedSubqueryDepth()));
        featureSummary.setCorrelatedSubqueryCount(Integer.valueOf(profile.getCorrelatedSubqueryCount()));
        featureSummary.setOrPredicateCount(Integer.valueOf(profile.getOrPredicateCount()));
        featureSummary.setFunctionWrappedPredicateCount(Integer.valueOf(profile.getFunctionWrappedPredicateCount()));
        featureSummary.setLeadingWildcardLikeCount(Integer.valueOf(profile.getLeadingWildcardLikeCount()));
        featureSummary.setRandomOrderCount(Integer.valueOf(profile.getRandomOrderCount()));
        featureSummary.setRepeatedTableScanCount(Integer.valueOf(profile.getRepeatedTableScanCount()));
        featureSummary.setOrderByExpressionCount(Integer.valueOf(profile.getOrderByExpressionCount()));
        featureSummary.setDuplicateOrderByKeyCount(Integer.valueOf(profile.getDuplicateOrderByKeyCount()));
        featureSummary.setDuplicateGroupByKeyCount(Integer.valueOf(profile.getDuplicateGroupByKeyCount()));
        featureSummary.setGroupByWithoutAggregate(Boolean.valueOf(profile.isGroupByWithoutAggregate()));
        featureSummary.setAggregateFunctionCount(Integer.valueOf(profile.getAggregateFunctionCount()));
        featureSummary.setStringProjectionCount(Integer.valueOf(profile.getStringProjectionCount()));
        featureSummary.setStringConcatenationCount(Integer.valueOf(profile.getStringConcatenationCount()));
        featureSummary.setLargeStringAggregateCount(Integer.valueOf(profile.getLargeStringAggregateCount()));
        featureSummary.setRepeatedSubqueryCount(Integer.valueOf(profile.getRepeatedSubqueryCount()));
        featureSummary.setEvidence(buildFeatureEvidence(profile, finalTableCount));
        response.setFeatureSummary(featureSummary);
        response.setAdvancedStructureProfile(profile.toAdvancedStructureProfile());
        response.setRiskChecklist(risks);
        response.setEstimatedResourceCost(buildResourceEstimate(profile, risks, finalTableCount));
    }

    protected void enrichHeuristicQueryIntent(StructureParseResponseVO response,
                                            HeuristicFallbackProfile profile) {
        String scanMode = resolveHeuristicScanMode(profile);
        String joinType = resolveHeuristicJoinType(profile);
        String computeDensity = "UNKNOWN";
        String resourceType = "UNKNOWN";
        String slaLevel = profile.getJoinCount() > 0 || profile.getPredicateCount() == 0
            ? "REPORT_LT_30S"
            : "UNKNOWN";

        StructureParseIntentProfileVO intentProfile = new StructureParseIntentProfileVO();
        intentProfile.setScanMode(scanMode);
        intentProfile.setJoinType(joinType);
        intentProfile.setComputeDensity(computeDensity);
        intentProfile.setResourceType(resourceType);
        intentProfile.setSlaLevel(slaLevel);
        intentProfile.setConfidence("LOW");
        intentProfile.setClassificationLabels(buildClassificationLabels(scanMode, joinType, computeDensity, resourceType, slaLevel));
        response.setIntentProfile(intentProfile);

        int finalTableCount = extractFinalTableKeys(response == null ? null : response.getLogicalObjectHits()).size();
        if (finalTableCount <= 0) {
            finalTableCount = profile.getTables().size();
        }
        StructureParseFeatureSummaryVO featureSummary = new StructureParseFeatureSummaryVO();
        featureSummary.setParserEngine(HEURISTIC_FALLBACK_ENGINE);
        featureSummary.setScanMode(scanMode);
        featureSummary.setJoinType(joinType);
        featureSummary.setComputeDensity(computeDensity);
        featureSummary.setResourceType(resourceType);
        featureSummary.setSlaLevel(slaLevel);
        featureSummary.setTableCount(Integer.valueOf(finalTableCount));
        featureSummary.setJoinCount(Integer.valueOf(profile.getJoinCount()));
        featureSummary.setPredicateCount(Integer.valueOf(profile.getPredicateCount()));
        featureSummary.setWindowFunctionCount(Integer.valueOf(0));
        featureSummary.setUdfFunctionCount(Integer.valueOf(0));
        featureSummary.setRepeatedExpressionCount(Integer.valueOf(0));
        featureSummary.setSubqueryCount(Integer.valueOf(0));
        featureSummary.setScalarSubqueryCount(Integer.valueOf(0));
        featureSummary.setNestedSubqueryDepth(Integer.valueOf(0));
        featureSummary.setCorrelatedSubqueryCount(Integer.valueOf(0));
        featureSummary.setOrPredicateCount(Integer.valueOf(profile.getOrPredicateCount()));
        featureSummary.setFunctionWrappedPredicateCount(Integer.valueOf(0));
        featureSummary.setLeadingWildcardLikeCount(Integer.valueOf(0));
        featureSummary.setRandomOrderCount(Integer.valueOf(0));
        featureSummary.setRepeatedTableScanCount(Integer.valueOf(0));
        featureSummary.setOrderByExpressionCount(Integer.valueOf(0));
        featureSummary.setDuplicateOrderByKeyCount(Integer.valueOf(0));
        featureSummary.setDuplicateGroupByKeyCount(Integer.valueOf(0));
        featureSummary.setGroupByWithoutAggregate(Boolean.FALSE);
        featureSummary.setAggregateFunctionCount(Integer.valueOf(0));
        featureSummary.setStringProjectionCount(Integer.valueOf(0));
        featureSummary.setStringConcatenationCount(Integer.valueOf(0));
        featureSummary.setLargeStringAggregateCount(Integer.valueOf(0));
        featureSummary.setRepeatedSubqueryCount(Integer.valueOf(0));
        featureSummary.setEvidence(buildHeuristicFeatureEvidence(profile, finalTableCount));
        response.setFeatureSummary(featureSummary);
        response.setAdvancedStructureProfile(unavailableAdvancedStructureProfile(HEURISTIC_FALLBACK_ENGINE));
        response.setEstimatedResourceCost(heuristicResourceEstimate(profile));
    }

    protected Map<String, Object> unavailableAdvancedStructureProfile(String parserEngine) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("profileStatus", "UNAVAILABLE");
        payload.put("parserEngine", parserEngine);
        payload.put("tables", Collections.emptyList());
        payload.put("projections", Collections.emptyList());
        payload.put("predicates", Collections.emptyList());
        payload.put("joinGraph", Collections.emptyList());
        payload.put("aggregations", Collections.emptyList());
        payload.put("groupBy", Collections.emptyList());
        payload.put("orderBy", Collections.emptyList());
        payload.put("limit", Collections.emptyMap());
        payload.put("ctes", Collections.emptyList());
        payload.put("subqueries", Collections.emptyList());
        payload.put("timeFunctions", Collections.emptyList());
        payload.put("nonDeterministicFunctions", Collections.emptyList());
        return payload;
    }

    protected String resolveHeuristicScanMode(HeuristicFallbackProfile profile) {
        if (profile.getPredicateCount() == 0) {
            return "FULL_TABLE_SCAN";
        }
        if (!profile.getDatePredicateColumns().isEmpty() || !profile.getDates().isEmpty()) {
            return profile.getPredicateCount() == 1 ? "RANGE_SCAN" : "PARTITION_RANGE_SCAN";
        }
        return "CROSS_PARTITION_SCAN";
    }

    protected String resolveHeuristicJoinType(HeuristicFallbackProfile profile) {
        if (profile.getJoinCount() == 0) {
            return "NONE";
        }
        return profile.getJoinCount() == 1 ? "CHAIN" : "MANY_TO_MANY";
    }

    protected List<String> buildHeuristicFeatureEvidence(HeuristicFallbackProfile profile, int finalTableCount) {
        List<String> evidence = new ArrayList<String>();
        evidence.add("parser=" + HEURISTIC_FALLBACK_ENGINE);
        evidence.add("staticOnly=true");
        evidence.add("fallback=AST_UNAVAILABLE");
        evidence.add("sqlTooLong=" + profile.isSqlTooLong());
        evidence.add("sqlLength=" + profile.getSqlLength());
        evidence.add("boundedSqlLength=" + profile.getBoundedSqlLength());
        evidence.add("tables=" + finalTableCount);
        evidence.add("predicates=" + profile.getPredicateCount());
        evidence.add("joins=" + profile.getJoinCount());
        evidence.add("orPredicates=" + profile.getOrPredicateCount());
        if (!profile.getDatePredicateColumns().isEmpty()) {
            evidence.add("datePredicateColumns=" + String.join(",", profile.getDatePredicateColumns()));
        }
        return evidence;
    }

    protected StructureParseResourceEstimateVO heuristicResourceEstimate(HeuristicFallbackProfile profile) {
        StructureParseResourceEstimateVO estimate = unknownResourceEstimate();
        List<String> evidence = new ArrayList<String>();
        evidence.add("由于 AST 解析器未生成有效画像，资源估算被限定为保守结果。");
        evidence.add("parser=" + HEURISTIC_FALLBACK_ENGINE);
        evidence.add("staticOnly=true");
        evidence.add("tables=" + profile.getTables().size());
        evidence.add("predicates=" + profile.getPredicateCount());
        estimate.setEvidence(evidence);
        return estimate;
    }

    protected StructureParseIntentProfileVO unknownIntentProfile() {
        StructureParseIntentProfileVO profile = new StructureParseIntentProfileVO();
        profile.setClassificationLabels(Collections.singletonList("UNSUPPORTED_SQL"));
        profile.setScanMode("UNKNOWN");
        profile.setJoinType("UNKNOWN");
        profile.setComputeDensity("UNKNOWN");
        profile.setResourceType("UNKNOWN");
        profile.setSlaLevel("UNKNOWN");
        profile.setConfidence("LOW");
        return profile;
    }

    protected StructureParseFeatureSummaryVO unknownFeatureSummary() {
        StructureParseFeatureSummaryVO summary = new StructureParseFeatureSummaryVO();
        summary.setParserEngine("UNKNOWN");
        summary.setScanMode("UNKNOWN");
        summary.setJoinType("UNKNOWN");
        summary.setComputeDensity("UNKNOWN");
        summary.setResourceType("UNKNOWN");
        summary.setSlaLevel("UNKNOWN");
        summary.setEvidence(Collections.singletonList("解析器无法生成受支持的 AST 画像。"));
        return summary;
    }

    protected StructureParseResourceEstimateVO unknownResourceEstimate() {
        StructureParseResourceEstimateVO estimate = new StructureParseResourceEstimateVO();
        estimate.setOverall("UNKNOWN");
        estimate.setCpu("UNKNOWN");
        estimate.setIo("UNKNOWN");
        estimate.setMemory("UNKNOWN");
        estimate.setNetwork("UNKNOWN");
        estimate.setResultSize("UNKNOWN");
        estimate.setEvidence(Collections.singletonList("无效 SQL 无法生成资源估算。"));
        return estimate;
    }

    protected String resolveScanMode(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile.getPredicateCount() == 0) {
            return "FULL_TABLE_SCAN";
        }
        if (profile.getDatePredicateColumns().isEmpty()) {
            return profile.isLimitPresent() && profile.getPredicateCount() == 1 ? "POINT_LOOKUP" : "CROSS_PARTITION_SCAN";
        }
        return profile.getPredicateCount() == 1 ? "RANGE_SCAN" : "PARTITION_RANGE_SCAN";
    }

    protected String resolveJoinType(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile.getJoinCount() == 0) {
            return "NONE";
        }
        if (profile.getJoinCount() == 1) {
            return "CHAIN";
        }
        if (profile.getJoinCriteriaCount() <= profile.getJoinCount()) {
            return "MANY_TO_MANY";
        }
        return profile.getJoinCount() >= 3 ? "SNOWFLAKE" : "STAR";
    }

    protected String resolveComputeDensity(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile.getWindowFunctionCount() > 0) {
            return "WINDOW";
        }
        AggregateShape aggregateShape = aggregateShape(profile);
        if (profile.getJoinCount() >= 3
            || profile.getSubqueryCount() >= 3
            || profile.getNestedSubqueryDepth() >= 2
            || profile.getFunctionWrappedPredicateCount() > 0
            || profile.getRandomOrderCount() > 0
            || profile.getRepeatedSubqueryCount() > 0
            || profile.getLargeStringAggregateCount() > 0
            || aggregateShape.isHeavy()) {
            return "HEAVY";
        }
        if (profile.getUdfFunctionCount() > 0) {
            return "UDF";
        }
        if (aggregateShape.isModerate()) {
            return "MODERATE";
        }
        return "LIGHT";
    }

    protected AggregateShape aggregateShape(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        int aggregateCount = profile.getAggregateFunctionCount();
        int groupByCount = profile.getGroupByCount();
        int orderByCount = profile.getOrderByCount();
        int groupingWeight = cappedShapeWeight(groupByCount, 3, 8);
        int aggregateWeight = cappedShapeWeight(aggregateCount, 2, 4);
        int orderingWeight = cappedShapeWeight(orderByCount, 1, 3);
        int complexityWeight = Math.min(3, groupingWeight + aggregateWeight);
        boolean compactFilteredReport = profile.getTables().size() <= 1
            && profile.getJoinCount() == 0
            && profile.getSubqueryCount() == 0
            && profile.getPredicateCount() > 0
            && profile.isLimitPresent()
            && groupByCount <= 3
            && aggregateCount <= 2
            && orderByCount <= 1;
        boolean heavy = !compactFilteredReport
            && (groupByCount > 8
                || aggregateCount > 4
                || groupingWeight + aggregateWeight + orderingWeight >= 5);
        boolean moderate = groupByCount > 1
            || aggregateCount > 1
            || (groupByCount > 0 && aggregateCount > 0)
            || (orderByCount > 0 && (groupByCount > 0 || aggregateCount > 0));
        return new AggregateShape(complexityWeight, heavy, moderate);
    }

    protected int cappedShapeWeight(int count, int smallUpperBound, int mediumUpperBound) {
        if (count <= 0) {
            return 0;
        }
        if (count <= smallUpperBound) {
            return 1;
        }
        if (count <= mediumUpperBound) {
            return 2;
        }
        return 3;
    }

    protected String resolveResourceType(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                       String scanMode,
                                       String computeDensity) {
        if (profile.getJoinCount() > 0 || profile.isSetOperation() || profile.getSubqueryCount() > 0) {
            return "NETWORK_MIXED";
        }
        if ("FULL_TABLE_SCAN".equals(scanMode) || "CROSS_PARTITION_SCAN".equals(scanMode)) {
            return "IO";
        }
        if ("WINDOW".equals(computeDensity) || profile.getOrderByCount() > 0) {
            return "MEMORY";
        }
        if ("HEAVY".equals(computeDensity) || "UDF".equals(computeDensity)) {
            return "CPU";
        }
        return "CPU_IO_MIXED";
    }

    protected String resolveSlaLevel(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                   String scanMode,
                                   String computeDensity) {
        if ("FULL_TABLE_SCAN".equals(scanMode)
            || "CROSS_PARTITION_SCAN".equals(scanMode)
            || "WINDOW".equals(computeDensity)
            || "HEAVY".equals(computeDensity)
            || profile.getJoinCount() >= 2
            || profile.getSubqueryCount() > 0) {
            return "REPORT_LT_30S";
        }
        return "INTERACTIVE_LT_3S";
    }

    protected List<String> buildClassificationLabels(String scanMode,
                                                   String joinType,
                                                   String computeDensity,
                                                   String resourceType,
                                                   String slaLevel) {
        return Arrays.asList(scanMode, joinType, computeDensity, resourceType, slaLevel);
    }

    protected List<String> buildFeatureEvidence(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<String> evidence = new ArrayList<String>();
        evidence.add("parser=" + profile.getParserEngine());
        evidence.add("staticOnly=true");
        evidence.add("tables=" + profile.getTables().size());
        evidence.add("predicates=" + profile.getPredicateCount());
        evidence.add("joins=" + profile.getJoinCount());
        evidence.add("aggregates=" + profile.getAggregateFunctionCount());
        evidence.add("orderByExpressions=" + profile.getOrderByExpressionCount());
        evidence.add("duplicateGroupByKeys=" + profile.getDuplicateGroupByKeyCount());
        evidence.add("duplicateOrderByKeys=" + profile.getDuplicateOrderByKeyCount());
        evidence.add("groupByWithoutAggregate=" + profile.isGroupByWithoutAggregate());
        evidence.add("windows=" + profile.getWindowFunctionCount());
        evidence.add("repeatedExpressions=" + profile.getRepeatedExpressionCount());
        evidence.add("subqueries=" + profile.getSubqueryCount());
        evidence.add("repeatedSubqueries=" + profile.getRepeatedSubqueryCount());
        evidence.add("scalarSubqueries=" + profile.getScalarSubqueryCount());
        evidence.add("nestedSubqueryDepth=" + profile.getNestedSubqueryDepth());
        evidence.add("correlatedSubqueries=" + profile.getCorrelatedSubqueryCount());
        evidence.add("orPredicates=" + profile.getOrPredicateCount());
        evidence.add("functionWrappedPredicates=" + profile.getFunctionWrappedPredicateCount());
        evidence.add("leadingWildcardLikes=" + profile.getLeadingWildcardLikeCount());
        evidence.add("randomOrders=" + profile.getRandomOrderCount());
        evidence.add("repeatedTableScans=" + profile.getRepeatedTableScanCount());
        evidence.add("stringProjections=" + profile.getStringProjectionCount());
        evidence.add("stringConcatenations=" + profile.getStringConcatenationCount());
        evidence.add("largeStringAggregates=" + profile.getLargeStringAggregateCount());
        return evidence;
    }

    protected List<String> buildFeatureEvidence(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                              int finalTableCount) {
        List<String> evidence = buildFeatureEvidence(profile);
        if (finalTableCount != profile.getTables().size()) {
            evidence.add("expandedTables=" + finalTableCount);
        }
        return evidence;
    }

    protected int resolveFinalTableCount(StructureParseResponseVO response,
                                       SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<String> tableKeys = extractFinalTableKeys(response == null ? null : response.getLogicalObjectHits());
        return tableKeys.isEmpty() ? profile.getTables().size() : tableKeys.size();
    }

    protected List<String> extractFinalTableKeys(List<LogicalObjectSurface> logicalObjectHits) {
        if (logicalObjectHits == null || logicalObjectHits.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        for (LogicalObjectSurface hit : logicalObjectHits) {
            if (hit == null) {
                continue;
            }
            addTableKey(keys, hit.getObjectKey());
            if (hit.getMappedPhysicalTargets() != null) {
                for (String target : hit.getMappedPhysicalTargets()) {
                    addTableKey(keys, target);
                }
            }
        }
        return new ArrayList<String>(keys);
    }

    protected void addTableKey(Set<String> keys, String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized != null && normalized.toUpperCase(Locale.ROOT).startsWith("TABLE:")) {
            keys.add(normalized);
        }
    }

    protected boolean containsValue(List<String> values, String expected) {
        if (values == null || !StringUtils.hasText(expected)) {
            return false;
        }
        for (String value : values) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return false;
    }

    protected List<StructureParseRiskVO> buildRiskChecklist(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<StructureParseRiskVO> risks = new ArrayList<StructureParseRiskVO>();
        Set<String> emitted = new LinkedHashSet<String>();
        for (String warning : profile.getWarnings()) {
            if ("NO_PREDICATE".equals(warning)) {
                addRisk(risks, emitted, risk("FULL_TABLE_SCAN_RISK", "HIGH", "全表扫描风险",
                    "predicateCount=0", "请添加租户、时间、分区或业务键谓词。"));
            } else if ("LARGE_JOIN_PAIR_RISK".equals(warning) || "HEAVY_JOIN_GRAPH".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_TABLE_JOIN_RISK", "HIGH", "静态 join 证据风险",
                    "staticOnly=true, joinCount=" + profile.getJoinCount()
                        + ", joinCriteriaCount=" + profile.getJoinCriteriaCount(),
                    "请通过访问解析或压测证据确认 join key、过滤位置和选择性。"));
            } else if ("ORDER_BY_WITHOUT_LIMIT".equals(warning)) {
                addRisk(risks, emitted, risk("UNNECESSARY_SORT_RISK", "MEDIUM", "潜在不必要排序",
                    "orderByCount=" + profile.getOrderByCount() + ", limitPresent=false", "请添加 LIMIT，或将排序迁移到服务对象。"));
            } else if ("REPEATED_EXPRESSION_COMPUTE".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_EXPRESSION_RISK", "MEDIUM", "重复表达式计算",
                    "repeatedExpressionCount=" + profile.getRepeatedExpressionCount(), "请去重或物化共享表达式。"));
            } else if ("LARGE_RESULT_SET_RISK".equals(warning) || "SELECT_STAR".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_RESULT_SET_RISK", "HIGH", "超大结果集风险",
                    "selectStar=" + profile.isSelectStar() + ", limitPresent=" + profile.isLimitPresent(),
                    "交互路径请使用显式列、过滤条件或 LIMIT。"));
            } else if ("SCALAR_SUBQUERY_IN_SELECT".equals(warning)) {
                addRisk(risks, emitted, risk("SCALAR_SUBQUERY_IN_SELECT", "HIGH", "SELECT 中的标量子查询",
                    "scalarSubqueryCount=" + profile.getScalarSubqueryCount(), "请将标量子查询改写为 join 或分阶段聚合。"));
            } else if ("NESTED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("NESTED_SUBQUERY_RISK", "HIGH", "嵌套子查询风险",
                    "subqueryCount=" + profile.getSubqueryCount() + ", depth=" + profile.getNestedSubqueryDepth(),
                    "请将嵌套子查询展开为具名 CTE 阶段。"));
            } else if ("CORRELATED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("CORRELATED_SUBQUERY_RISK", "HIGH", "关联子查询风险",
                    "correlatedSubqueryCount=" + profile.getCorrelatedSubqueryCount(), "请将关联子查询改写为 join 或预计算阶段。"));
            } else if ("FUNCTION_WRAPPED_PREDICATE".equals(warning)) {
                addRisk(risks, emitted, risk("FUNCTION_WRAPPED_PREDICATE", "MEDIUM", "函数包裹谓词",
                    "functionWrappedPredicateCount=" + profile.getFunctionWrappedPredicateCount(), "请改写为范围条件或标准化列比较。"));
            } else if ("NOT_EXISTS_ANTI_JOIN_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("NOT_EXISTS_ANTI_JOIN_RISK", "MEDIUM", "NOT EXISTS anti-join 风险",
                    "notExistsCount=" + profile.getNotExistsCount(), "请评审 anti-join 语义与分阶段替代方案。"));
            } else if ("LEADING_WILDCARD_LIKE_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("LEADING_WILDCARD_LIKE_RISK", "MEDIUM", "前置通配符 LIKE 风险",
                    "leadingWildcardLikeCount=" + profile.getLeadingWildcardLikeCount(), "请使用可搜索键或可前缀匹配的谓词。"));
            } else if ("OR_PREDICATE_INDEX_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("OR_PREDICATE_INDEX_RISK", "MEDIUM", "OR 谓词裁剪风险",
                    "orPredicateCount=" + profile.getOrPredicateCount(), "请考虑 UNION ALL 分支或分阶段过滤。"));
            } else if ("ORDER_BY_RANDOM_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("ORDER_BY_RANDOM_RISK", "HIGH", "随机排序风险",
                    "randomOrderCount=" + profile.getRandomOrderCount(), "请使用确定性抽样替代 ORDER BY RAND/RANDOM。"));
            } else if ("REPEATED_TABLE_SCAN_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_TABLE_SCAN_RISK", "HIGH", "重复表扫描风险",
                    "repeatedTableScanCount=" + profile.getRepeatedTableScanCount(), "请预先分阶段处理重复输入，并显式复用。"));
            } else if ("ORDER_BY_COMPLEXITY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("ORDER_BY_COMPLEXITY_RISK", "MEDIUM", "ORDER BY 复杂度风险",
                    "staticOnly=true, orderByExpressionCount=" + profile.getOrderByExpressionCount()
                        + ", duplicateOrderByKeyCount=" + profile.getDuplicateOrderByKeyCount(),
                    "请减少排序键、移除重复排序，或用计划证据校验排序输出。"));
            } else if ("JOIN_LATENCY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("JOIN_LATENCY_RISK", "HIGH", "join 延迟风险",
                    "staticOnly=true, joinCount=" + profile.getJoinCount()
                        + ", joinCriteriaCount=" + profile.getJoinCriteriaCount()
                        + ", subqueryCount=" + profile.getSubqueryCount(),
                    "请通过访问解析或压测确认 join 键、过滤位置、扫描量和行移动。"));
            } else if ("AGGREGATION_COMPLEXITY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("AGGREGATION_COMPLEXITY_RISK", "MEDIUM", "聚合复杂度风险",
                    "staticOnly=true, aggregateFunctionCount=" + profile.getAggregateFunctionCount()
                        + ", groupByCount=" + profile.getGroupByCount()
                        + ", largeStringAggregateCount=" + profile.getLargeStringAggregateCount(),
                    "请预聚合可复用阶段，或将重型聚合输出迁移到服务对象。"));
            } else if ("GROUP_BY_WITHOUT_AGGREGATE_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("GROUP_BY_WITHOUT_AGGREGATE_RISK", "MEDIUM", "无聚合函数的 GROUP BY",
                    "staticOnly=true, groupByCount=" + profile.getGroupByCount() + ", aggregateFunctionCount=0",
                    "若意图是去重请使用 DISTINCT，或移除冗余分组。"));
            } else if ("DUPLICATE_GROUP_OR_ORDER_KEY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("DUPLICATE_GROUP_OR_ORDER_KEY_RISK", "MEDIUM", "重复分组或排序键风险",
                    "staticOnly=true, duplicateGroupByKeyCount=" + profile.getDuplicateGroupByKeyCount()
                        + ", duplicateOrderByKeyCount=" + profile.getDuplicateOrderByKeyCount(),
                    "改写评审前请移除重复分组或排序键。"));
            } else if ("REPEATED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_SUBQUERY_RISK", "HIGH", "重复子查询风险",
                    "staticOnly=true, repeatedSubqueryCount=" + profile.getRepeatedSubqueryCount(),
                    "请将重复子查询抽取为具名 CTE 或已评审的服务对象。"));
            } else if ("LARGE_STRING_RESULT_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_STRING_RESULT_RISK", "HIGH", "大字符串结果风险",
                    "staticOnly=true, stringProjectionCount=" + profile.getStringProjectionCount()
                        + ", stringConcatenationCount=" + profile.getStringConcatenationCount()
                        + ", largeStringAggregateCount=" + profile.getLargeStringAggregateCount(),
                    "请限制字符串投影，或通过访问解析/压测校验返回字节数。"));
            } else if ("COMPLEX_QUERY_GRAPH_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("COMPLEX_QUERY_GRAPH_RISK", "HIGH", "复杂查询图风险",
                    "subqueryCount=" + profile.getSubqueryCount() + "，谓词数=" + profile.getPredicateCount(),
                    "在线使用前请将查询拆成已评审阶段。"));
            }
        }
        return risks;
    }

    protected void addRisk(List<StructureParseRiskVO> risks, Set<String> emitted, StructureParseRiskVO risk) {
        if (risk != null && emitted.add(risk.getRiskCode())) {
            risks.add(risk);
        }
    }

    protected StructureParseRiskVO risk(String code,
                                      String severity,
                                      String summary,
                                      String evidence,
                                      String suggestedAction) {
        StructureParseRiskVO risk = new StructureParseRiskVO();
        risk.setRiskCode(code);
        risk.setSeverity(severity);
        risk.setSummary(summary);
        risk.setEvidence(evidence);
        risk.setSuggestedAction(suggestedAction);
        return risk;
    }

    protected StructureParseResourceEstimateVO buildResourceEstimate(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                                   List<StructureParseRiskVO> risks,
                                                                   int finalTableCount) {
        StructureParseResourceEstimateVO estimate = new StructureParseResourceEstimateVO();
        estimate.setCpu(level(profile.getAggregateFunctionCount() + profile.getUdfFunctionCount()
            + profile.getRepeatedExpressionCount()
            + profile.getFunctionWrappedPredicateCount()
            + profile.getRandomOrderCount()
            + profile.getStringConcatenationCount()
            + profile.getRepeatedSubqueryCount()));
        estimate.setIo(profile.getPredicateCount() == 0 || profile.getRepeatedTableScanCount() > 0
            || finalTableCount > profile.getTables().size() ? "HIGH" : "MEDIUM");
        estimate.setMemory(profile.getOrderByExpressionCount() + profile.getWindowFunctionCount()
            + profile.getRandomOrderCount() + profile.getLargeStringAggregateCount() > 0 ? "HIGH" : "LOW");
        estimate.setNetwork(profile.getJoinCount() > 0 || profile.isSetOperation() || profile.getSubqueryCount() > 0
            || finalTableCount > 1 || profile.getStringProjectionCount() >= 4
            || profile.getLargeStringAggregateCount() > 0 ? "HIGH" : "LOW");
        estimate.setResultSize(profile.isSelectStar() || !profile.isLimitPresent() || profile.getComplexGraphScore() >= 8
            || finalTableCount > profile.getTables().size()
            || profile.getStringProjectionCount() >= 4
            || profile.getLargeStringAggregateCount() > 0
            || profile.getStringConcatenationCount() >= 2 ? "HIGH" : "MEDIUM");
        estimate.setOverall(resolveOverallEstimate(estimate, risks));
        estimate.setEvidence(buildFeatureEvidence(profile, finalTableCount));
        return estimate;
    }

    protected String level(int score) {
        if (score >= 3) {
            return "HIGH";
        }
        if (score > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    protected String resolveOverallEstimate(StructureParseResourceEstimateVO estimate, List<StructureParseRiskVO> risks) {
        if (!risks.isEmpty()
            || "HIGH".equals(estimate.getCpu())
            || "HIGH".equals(estimate.getIo())
            || "HIGH".equals(estimate.getMemory())
            || "HIGH".equals(estimate.getNetwork())
            || "HIGH".equals(estimate.getResultSize())) {
            return "HIGH";
        }
        if ("MEDIUM".equals(estimate.getCpu()) || "MEDIUM".equals(estimate.getIo())) {
            return "MEDIUM";
        }
        return "LOW";
    }

    protected StructureParseResponseVO toResponse(StructureParseResult result) {
        StructureParseResponseVO response = new StructureParseResponseVO();
        response.setParseTaskId(result.getParseTaskId());
        response.setSyntaxStatus(result.getSyntaxStatus().name());
        response.setComplexityLevel(result.getComplexityLevel().name());
        response.setSqlType(result.getSqlType());
        response.setQueryDateSummary(toQueryDateSummaryVO(result.getQueryDateSummary()));
        response.setLogicalObjectHits(toLogicalObjectHitVOs(result.getLogicalObjectHits()));
        response.setSurfaceObjectRefs(toLogicalObjectHitVOs(result.getSurfaceObjectRefs()));
        response.setExpandedPhysicalObjectRefs(toLogicalObjectHitVOs(result.getExpandedPhysicalObjectRefs()));
        response.setRiskTags(result.getRiskTags());
        response.setRewriteCandidates(result.getRewriteCandidates());
        response.setIssues(toIssueVOs(result.getIssues()));
        response.setPriorityScore(result.getPriorityScore());
        response.setPriorityLevel(result.getPriorityLevel().name());
        response.setImportant(result.getImportant());
        response.setUrgent(result.getUrgent());
        response.setFailureReason(result.getFailureReason());
        response.setFailureLine(result.getFailureLine());
        response.setFailureColumn(result.getFailureColumn());
        response.setFailureOffset(result.getFailureOffset());
        response.setFailureToken(result.getFailureToken());
        response.setFailureSnippet(result.getFailureSnippet());
        return response;
    }

    protected void writeParseHistory(StructureParseResponseVO response,
                                   StructureParseRequest request,
                                   StructureParseResult result) {
        if (sqlParseHistoryApplicationService == null || response == null || request == null || result == null) {
            return;
        }
        try {
            SqlParseHistoryWriteResult writeResult = sqlParseHistoryApplicationService.writeStructureHistory(
                response,
                null,
                request,
                resolveHistoryResultStatus(response)
            );
            applyHistoryWriteResult(response, writeResult);
            triggerRewriteRecommendation(
                response,
                request,
                SqlParseHistoryApplicationService.SOURCE_STRUCTURE_PARSE,
                response.getParseTaskId(),
                null
            );
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=STRUCTURE_PARSE_HISTORY_WRITE entity={} tenantId={} status=DEGRADED reason={}",
                response.getParseTaskId(),
                RequestContext.getTenantId(),
                ex.getMessage()
            );
            response.setHistoryPersisted(Boolean.FALSE);
            response.setHistoryPersistenceStatus("WRITE_FAILED");
        }
    }

    public void writeParseHistoryWithAccess(StructureParseResponseVO structureParse,
                                            AccessParseResponseVO accessParse,
                                            StructureParseRequest request,
                                            String resultStatus) {
        writeParseHistoryWithAccess(
            structureParse,
            accessParse,
            request,
            resultStatus,
            SqlParseHistoryApplicationService.SOURCE_STRUCTURE_PARSE,
            null,
            null
        );
    }

    public void writeParseHistoryWithAccess(StructureParseResponseVO structureParse,
                                            AccessParseResponseVO accessParse,
                                            StructureParseRequest request,
                                            String resultStatus,
                                            String sourceType,
                                            String sourceId,
                                            String batchKey) {
        if (sqlParseHistoryApplicationService == null || structureParse == null || request == null) {
            if (structureParse != null) {
                structureParse.setHistoryPersisted(Boolean.FALSE);
                structureParse.setHistoryPersistenceStatus("WRITE_SKIPPED");
            }
            return;
        }
        try {
            SqlParseHistoryWriteResult writeResult = sqlParseHistoryApplicationService.writeStructureHistory(
                structureParse,
                accessParse,
                request,
                StringUtils.hasText(resultStatus) ? resultStatus.trim() : resolveHistoryResultStatus(structureParse),
                sourceType,
                sourceId,
                batchKey
            );
            applyHistoryWriteResult(structureParse, writeResult);
            triggerRewriteRecommendation(structureParse, request, sourceType, sourceId, batchKey);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=STRUCTURE_ACCESS_PARSE_HISTORY_WRITE entity={} tenantId={} status=DEGRADED reason={}",
                structureParse.getParseTaskId(),
                RequestContext.getTenantId(),
                ex.getMessage()
            );
            structureParse.setHistoryPersisted(Boolean.FALSE);
            structureParse.setHistoryPersistenceStatus("WRITE_FAILED");
        }
    }

    protected String resolveHistoryResultStatus(StructureParseResponseVO response) {
        if (response == null) {
            return HISTORY_RESULT_FAILED;
        }
        if (ParseAnalysisStatus.SUCCESS.name().equals(response.getAnalysisStatus())) {
            return HISTORY_RESULT_SUCCESS;
        }
        if (ParseAnalysisStatus.PARTIAL_SUCCESS.name().equals(response.getAnalysisStatus())) {
            return "PARTIAL";
        }
        return HISTORY_RESULT_FAILED;
    }

    protected void applyHistoryWriteResult(StructureParseResponseVO response, SqlParseHistoryWriteResult writeResult) {
        if (writeResult == null) {
            response.setHistoryPersisted(Boolean.FALSE);
            response.setHistoryPersistenceStatus("NO_RESPONSE");
            return;
        }
        response.setHistoryId(writeResult.getParseHistoryId());
        response.setHistoryPersisted(writeResult.getPersisted());
        response.setHistoryPersistenceStatus(writeResult.getPersistenceStatus());
    }

    protected void triggerRewriteRecommendation(StructureParseResponseVO structureParse,
                                              StructureParseRequest request,
                                              String sourceType,
                                              String sourceId,
                                              String batchId) {
        if (parseTriggeredRewriteRecommendationService == null) {
            return;
        }
        parseTriggeredRewriteRecommendationService.triggerAfterHistoryWrite(
            structureParse,
            request,
            sourceType,
            sourceId,
            batchId
        );
    }

    protected StructureParseQueryDateSummaryVO toQueryDateSummaryVO(StructureParseQueryDateSummary summary) {
        StructureParseQueryDateSummaryVO vo = new StructureParseQueryDateSummaryVO();
        if (summary == null) {
            vo.setQueryDateStatus(StructureParseQueryDateStatus.UNRESOLVED.name());
            return vo;
        }
        vo.setQueryDateStart(summary.getQueryDateStart());
        vo.setQueryDateEnd(summary.getQueryDateEnd());
        vo.setQueryDateFields(summary.getQueryDateFields());
        vo.setQueryDateStatus(summary.getQueryDateStatus().name());
        return vo;
    }

    protected List<LogicalObjectSurface> toLogicalObjectHitVOs(List<StructureParseLogicalObjectHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<LogicalObjectSurface> vos = new ArrayList<LogicalObjectSurface>();
        for (StructureParseLogicalObjectHit hit : hits) {
            LogicalObjectSurface vo = new LogicalObjectSurface();
            vo.setObjectType(hit.getObjectType().name());
            vo.setObjectKey(hit.getObjectKey());
            vo.setObjectName(hit.getObjectName());
            vo.setCatalogName(hit.getCatalogName());
            vo.setSchemaName(hit.getSchemaName());
            vo.setMatchSource(hit.getMatchSource());
            vo.setResolved(hit.getResolved());
            vo.setMappedPhysicalTargets(hit.getMappedPhysicalTargets());
            vos.add(vo);
        }
        return vos;
    }

    protected List<StructureParseLogicalObjectHit> filterPhysicalObjectRefs(List<StructureParseLogicalObjectHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<StructureParseLogicalObjectHit> result = new ArrayList<StructureParseLogicalObjectHit>();
        for (StructureParseLogicalObjectHit hit : hits) {
            if (hit != null && hit.getObjectType() == LogicalObjectType.TABLE) {
                result.add(hit);
            }
        }
        return result;
    }

    protected List<StructureParseIssueVO> toIssueVOs(List<StructureParseIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return Collections.emptyList();
        }
        List<StructureParseIssueVO> vos = new ArrayList<StructureParseIssueVO>();
        for (StructureParseIssue issue : issues) {
            StructureParseIssueVO vo = new StructureParseIssueVO();
            vo.setIssueCode(issue.getIssueCode());
            vo.setIssueDomain(issue.getIssueDomain().name());
            vo.setIssueScene(issue.getIssueScene());
            vo.setSeverity(issue.getSeverity().name());
            vo.setSummary(issue.getSummary());
            vo.setDetail(issue.getDetail());
            vo.setSuggestedAction(issue.getSuggestedAction());
            vo.setImportant(issue.getImportant());
            vo.setUrgent(issue.getUrgent());
            vo.setAffectedSqlCount(issue.getAffectedSqlCount());
            vo.setAffectedReportCount(issue.getAffectedReportCount());
            vo.setPriorityScore(issue.getPriorityScore());
            vo.setPriorityLevel(issue.getPriorityLevel().name());
            vo.setFailureLine(issue.getFailureLine());
            vo.setFailureColumn(issue.getFailureColumn());
            vo.setFailureOffset(issue.getFailureOffset());
            vo.setFailureToken(issue.getFailureToken());
            vo.setFailureSnippet(issue.getFailureSnippet());
            vos.add(vo);
        }
        return vos;
    }

    protected static final class HeuristicFallbackProfile {

        protected String sqlType = "UNKNOWN";
        protected List<String> tables = Collections.emptyList();
        protected List<String> datePredicateColumns = Collections.emptyList();
        protected List<LocalDate> dates = Collections.emptyList();
        protected int joinCount;
        protected int predicateCount;
        protected int orPredicateCount;
        protected boolean sqlTooLong;
        protected int sqlLength;
        protected int boundedSqlLength;

        protected String getSqlType() {
            return sqlType;
        }

        protected void setSqlType(String sqlType) {
            this.sqlType = StringUtils.hasText(sqlType) ? sqlType : "UNKNOWN";
        }

        protected List<String> getTables() {
            return tables;
        }

        protected void setTables(List<String> tables) {
            this.tables = tables == null ? Collections.<String>emptyList() : tables;
        }

        protected List<String> getDatePredicateColumns() {
            return datePredicateColumns;
        }

        protected void setDatePredicateColumns(List<String> datePredicateColumns) {
            this.datePredicateColumns = datePredicateColumns == null
                ? Collections.<String>emptyList()
                : datePredicateColumns;
        }

        protected List<LocalDate> getDates() {
            return dates;
        }

        protected void setDates(List<LocalDate> dates) {
            this.dates = dates == null ? Collections.<LocalDate>emptyList() : dates;
        }

        protected int getJoinCount() {
            return joinCount;
        }

        protected void setJoinCount(int joinCount) {
            this.joinCount = Math.max(0, joinCount);
        }

        protected int getPredicateCount() {
            return predicateCount;
        }

        protected void setPredicateCount(int predicateCount) {
            this.predicateCount = Math.max(0, predicateCount);
        }

        protected int getOrPredicateCount() {
            return orPredicateCount;
        }

        protected void setOrPredicateCount(int orPredicateCount) {
            this.orPredicateCount = Math.max(0, orPredicateCount);
        }

        protected boolean isSqlTooLong() {
            return sqlTooLong;
        }

        protected void setSqlTooLong(boolean sqlTooLong) {
            this.sqlTooLong = sqlTooLong;
        }

        protected int getSqlLength() {
            return sqlLength;
        }

        protected void setSqlLength(int sqlLength) {
            this.sqlLength = Math.max(0, sqlLength);
        }

        protected int getBoundedSqlLength() {
            return boundedSqlLength;
        }

        protected void setBoundedSqlLength(int boundedSqlLength) {
            this.boundedSqlLength = Math.max(0, boundedSqlLength);
        }
    }

    protected static final class LogicalObjectExpansionResult {

        protected final Map<String, StructureParseLogicalObjectHit> hitsByKey =
            new LinkedHashMap<String, StructureParseLogicalObjectHit>();
        protected final Map<String, StructureParseLogicalObjectHit> surfaceRefsByKey =
            new LinkedHashMap<String, StructureParseLogicalObjectHit>();
        protected final Map<String, StructureParseLogicalObjectHit> expandedPhysicalRefsByKey =
            new LinkedHashMap<String, StructureParseLogicalObjectHit>();
        protected final List<String> unresolvedReasons = new ArrayList<String>();

        protected void addHit(StructureParseLogicalObjectHit hit) {
            if (hit == null || !StringUtils.hasText(hit.getObjectKey())) {
                return;
            }
            if (hit.getObjectType() == LogicalObjectType.TABLE) {
                expandedPhysicalRefsByKey.put(hit.getObjectKey(), hit);
            }
            StructureParseLogicalObjectHit existing = hitsByKey.get(hit.getObjectKey());
            if (existing == null) {
                hitsByKey.put(hit.getObjectKey(), hit);
                return;
            }
            if (Boolean.TRUE.equals(hit.getResolved())) {
                existing.setResolved(Boolean.TRUE);
            } else if (Boolean.FALSE.equals(hit.getResolved())) {
                existing.setResolved(Boolean.FALSE);
            }
            if (!StringUtils.hasText(existing.getMatchSource()) && StringUtils.hasText(hit.getMatchSource())) {
                existing.setMatchSource(hit.getMatchSource());
            }
            existing.setMappedPhysicalTargets(mergeTargets(existing.getMappedPhysicalTargets(), hit.getMappedPhysicalTargets()));
        }

        protected void addSurfaceRef(StructureParseLogicalObjectHit hit) {
            if (hit != null && StringUtils.hasText(hit.getObjectKey())) {
                surfaceRefsByKey.put(hit.getObjectKey(), hit);
            }
        }

        protected List<StructureParseLogicalObjectHit> getHits() {
            return new ArrayList<StructureParseLogicalObjectHit>(hitsByKey.values());
        }

        protected List<StructureParseLogicalObjectHit> getSurfaceRefs() {
            return new ArrayList<StructureParseLogicalObjectHit>(surfaceRefsByKey.values());
        }

        protected List<StructureParseLogicalObjectHit> getExpandedPhysicalRefs() {
            return new ArrayList<StructureParseLogicalObjectHit>(expandedPhysicalRefsByKey.values());
        }

        protected void addUnresolvedReason(String reason) {
            if (StringUtils.hasText(reason)) {
                unresolvedReasons.add(reason);
            }
        }

        protected boolean hasUnresolvedViewDefinition() {
            return !unresolvedReasons.isEmpty();
        }

        protected List<String> getUnresolvedReasons() {
            return unresolvedReasons;
        }

        protected static List<String> mergeTargets(List<String> first, List<String> second) {
            LinkedHashSet<String> values = new LinkedHashSet<String>();
            if (first != null) {
                values.addAll(first);
            }
            if (second != null) {
                values.addAll(second);
            }
            return new ArrayList<String>(values);
        }
    }

    protected static final class ViewExpansionContext {

        protected final String tenantId;
        protected final String datasourceCode;
        protected final DataSourceTypeEnum datasourceType;
        protected final SqlParserMode parserMode;
        protected final LogicalObjectExpansionResult result;
        protected final Set<String> visited = new LinkedHashSet<String>();

        protected ViewExpansionContext(String tenantId,
                                     String datasourceCode,
                                     DataSourceTypeEnum datasourceType,
                                     SqlParserMode parserMode,
                                     LogicalObjectExpansionResult result) {
            this.tenantId = tenantId;
            this.datasourceCode = datasourceCode;
            this.datasourceType = datasourceType;
            this.parserMode = parserMode;
            this.result = result;
        }

        protected String getTenantId() {
            return tenantId;
        }

        protected String getDatasourceCode() {
            return datasourceCode;
        }

        protected DataSourceTypeEnum getDatasourceType() {
            return datasourceType;
        }

        protected SqlParserMode getParserMode() {
            return parserMode;
        }

        protected LogicalObjectExpansionResult getResult() {
            return result;
        }

        protected String visitKey(String objectKey) {
            return (datasourceCode == null ? "" : datasourceCode.toLowerCase(Locale.ROOT))
                + "|" + (objectKey == null ? "" : objectKey.toLowerCase(Locale.ROOT));
        }

        protected boolean isVisited(String visitKey) {
            return visited.contains(visitKey);
        }

        protected void pushVisited(String visitKey) {
            visited.add(visitKey);
        }

        protected void popVisited(String visitKey) {
            visited.remove(visitKey);
        }
    }

    protected static final class QualifiedObjectName {

        protected final String catalogName;
        protected final String schemaName;
        protected final String objectName;
        protected final String qualifiedName;

        protected QualifiedObjectName(String catalogName, String schemaName, String objectName) {
            this.catalogName = catalogName;
            this.schemaName = schemaName;
            this.objectName = objectName;
            this.qualifiedName = buildQualifiedName(catalogName, schemaName, objectName);
        }

        protected static QualifiedObjectName parse(String value) {
            if (!StringUtils.hasText(value)) {
                return new QualifiedObjectName(null, null, "");
            }
            String[] parts = value.trim().split("\\.");
            if (parts.length >= 3) {
                return new QualifiedObjectName(clean(parts[0]), clean(parts[1]), clean(parts[2]));
            }
            if (parts.length == 2) {
                return new QualifiedObjectName(null, clean(parts[0]), clean(parts[1]));
            }
            return new QualifiedObjectName(null, null, clean(parts[0]));
        }

        protected static String clean(String value) {
            String normalized = value == null ? "" : value.trim();
            if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("`") && normalized.endsWith("`"))
                || (normalized.startsWith("[") && normalized.endsWith("]"))) {
                return normalized.substring(1, normalized.length() - 1);
            }
            return normalized;
        }

        protected static String buildQualifiedName(String catalogName, String schemaName, String objectName) {
            List<String> parts = new ArrayList<String>();
            if (StringUtils.hasText(catalogName)) {
                parts.add(catalogName);
            }
            if (StringUtils.hasText(schemaName)) {
                parts.add(schemaName);
            }
            if (StringUtils.hasText(objectName)) {
                parts.add(objectName);
            }
            return String.join(".", parts);
        }

        protected String getCatalogName() {
            return catalogName;
        }

        protected String getSchemaName() {
            return schemaName;
        }

        protected String getObjectName() {
            return objectName;
        }

        protected String getQualifiedName() {
            return qualifiedName;
        }
    }

    protected static final class AggregateShape {

        protected final int complexityWeight;
        protected final boolean heavy;
        protected final boolean moderate;

        protected AggregateShape(int complexityWeight, boolean heavy, boolean moderate) {
            this.complexityWeight = complexityWeight;
            this.heavy = heavy;
            this.moderate = moderate;
        }

        protected int getComplexityWeight() {
            return complexityWeight;
        }

        protected boolean isHeavy() {
            return heavy;
        }

        protected boolean isModerate() {
            return moderate;
        }
    }

    protected String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    protected String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }
}
