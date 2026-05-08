package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceDbViewDependencyRef;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectSurface;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.PlanAnalysisVO;
import com.company.sqloptimization.application.controller.vo.StructureParseFeatureSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIntentProfileVO;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResourceEstimateVO;
import com.company.sqloptimization.application.controller.vo.StructureParseRiskVO;
import com.company.sqloptimization.domain.parse.StructureParseComplexityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueDomain;
import com.company.sqloptimization.domain.parse.StructureParseIssueSeverity;
import com.company.sqloptimization.domain.parse.StructureParseLogicalObjectHit;
import com.company.sqloptimization.domain.parse.StructureParsePriorityAssessment;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateStatus;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateSummary;
import com.company.sqloptimization.domain.parse.StructureParseResult;
import com.company.sqloptimization.domain.parse.StructureParseSyntaxStatus;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.parse.ParseAnalysisStatus;
import com.company.sqloptimization.domain.parse.PlanAnalysisStatus;
import com.company.sqloptimization.domain.parse.SqlParserMode;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataClient;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataRequest;
import com.company.sqloptimization.infrastructure.metadata.DatasourceViewMetadataResponse;
import com.company.sqloptimization.infrastructure.plananalysis.HetuPlanAnalysisClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StructureParseApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureParseApplicationService.class);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern VIEW_DEFINITION_BODY_PATTERN = Pattern.compile("(?is)\\bAS\\s+((SELECT|WITH)\\b.*)$");
    private static final Pattern HEURISTIC_SQL_TYPE_PATTERN =
        Pattern.compile("(?is)^\\s*(?:/\\*.*?\\*/\\s*|--[^\\r\\n]*(?:\\r?\\n|$)\\s*)*(WITH|SELECT|INSERT|UPDATE|DELETE|MERGE)\\b");
    private static final Pattern HEURISTIC_OBJECT_PATTERN =
        Pattern.compile("(?is)\\b(?:FROM|JOIN)\\s+((?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)(?:\\s*\\.\\s*(?:\"[^\"]+\"|`[^`]+`|\\[[^\\]]+\\]|[A-Za-z_][A-Za-z0-9_$]*)){0,2})");
    private static final Pattern HEURISTIC_JOIN_PATTERN = Pattern.compile("(?i)\\bJOIN\\b");
    private static final Pattern HEURISTIC_WHERE_PATTERN = Pattern.compile("(?i)\\bWHERE\\b");
    private static final Pattern HEURISTIC_BOOLEAN_PATTERN = Pattern.compile("(?i)\\b(AND|OR)\\b");
    private static final Pattern HEURISTIC_OR_PATTERN = Pattern.compile("(?i)\\bOR\\b");
    private static final Pattern HEURISTIC_DATE_PREDICATE_PATTERN =
        Pattern.compile("(?i)([A-Za-z0-9_\\.]*?(DATE|TIME|DT|DAY))\\s*(>=|<=|=|>|<|BETWEEN|IN)");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String MATCH_SOURCE_SQL = "SQL_TABLE_SCAN";
    private static final String MATCH_SOURCE_HEURISTIC = "NAME_HEURISTIC";
    private static final String MATCH_SOURCE_HEURISTIC_FALLBACK = "HEURISTIC_FALLBACK";
    private static final String MATCH_SOURCE_DB_VIEW_METADATA = "LIVE_DB_VIEW_METADATA";
    private static final String MATCH_SOURCE_DB_VIEW_DEFINITION = "DB_VIEW_DEFINITION";
    private static final String MATCH_SOURCE_DB_VIEW_CATALOG_FALLBACK = "DB_VIEW_CATALOG_FALLBACK";
    private static final String RISK_DB_VIEW_DEFINITION_UNRESOLVED = "DB_VIEW_DEFINITION_UNRESOLVED";
    private static final String RISK_SQL_SYNTAX_INVALID = "SQL_SYNTAX_INVALID";
    private static final String RISK_SQL_TOO_LONG = "SQL_TOO_LONG";
    private static final String HEURISTIC_FALLBACK_ENGINE = "HEURISTIC_FALLBACK";
    private static final String SQL_TOO_LONG_FAILURE_REASON =
        "SQL is too long; syntax parser failed or was skipped for bounded diagnostics.";
    private static final String HISTORY_RESULT_SUCCESS = "SUCCESS";
    private static final String HISTORY_RESULT_FAILED = "FAILED";
    private static final int MAX_DB_VIEW_EXPANSION_DEPTH = 5;
    private static final int DEFAULT_SQL_TEXT_LENGTH_LIMIT = 10 * 1024 * 1024;
    private static final int HEURISTIC_SQL_WINDOW_LIMIT = 64 * 1024;
    private static final int FAILURE_REASON_TEXT_LIMIT = 512;
    private static final int FAILURE_DETAIL_TEXT_LIMIT = 512;
    private static final int FAILURE_TOKEN_TEXT_LIMIT = 64;
    private static final int FAILURE_SNIPPET_TEXT_LIMIT = 120;

    @Value("${sql-optimization.parser.max-sql-length:10485760}")
    private int maxSqlTextLength = DEFAULT_SQL_TEXT_LENGTH_LIMIT;

    private final SqlOptimizationPipelineService sqlOptimizationPipelineService;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final SqlParseHistoryApplicationService sqlParseHistoryApplicationService;
    private final DatasourceViewMetadataClient datasourceViewMetadataClient;
    private final HetuPlanAnalysisClient hetuPlanAnalysisClient;
    private final ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService;

    @Autowired
    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
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

    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            SqlParseHistoryApplicationService sqlParseHistoryApplicationService,
                                            DatasourceViewMetadataClient datasourceViewMetadataClient,
                                            HetuPlanAnalysisClient hetuPlanAnalysisClient) {
        this(
            sqlOptimizationPipelineService,
            governanceCapabilityClient,
            sqlParseHistoryApplicationService,
            datasourceViewMetadataClient,
            hetuPlanAnalysisClient,
            null
        );
    }

    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            SqlParseHistoryApplicationService sqlParseHistoryApplicationService,
                                            DatasourceViewMetadataClient datasourceViewMetadataClient) {
        this(
            sqlOptimizationPipelineService,
            governanceCapabilityClient,
            sqlParseHistoryApplicationService,
            datasourceViewMetadataClient,
            HetuPlanAnalysisClient.unavailable(),
            null
        );
    }

    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            SqlParseHistoryApplicationService sqlParseHistoryApplicationService) {
        this(
            sqlOptimizationPipelineService,
            governanceCapabilityClient,
            sqlParseHistoryApplicationService,
            DatasourceViewMetadataClient.unavailable(),
            HetuPlanAnalysisClient.unavailable(),
            null
        );
    }

    public StructureParseResponseVO parse(StructureParseRequest request) {
        long start = System.currentTimeMillis();
        String parseTaskId = UUID.randomUUID().toString();
        String tenantId = RequestContext.getTenantId();
        String datasourceCode = trimToNull(request.getDatasourceCode());
        LOGGER.info(
            "operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} status=START",
            parseTaskId,
            tenantId,
            datasourceCode
        );
        StructureParseResult result;
        SqlOptimizationPipelineService.ParsedSqlProfile profile = null;
        SqlParserMode requestedParserMode = SqlParserMode.resolve(request.getParserMode());
        SqlParserMode structureParserMode = requestedParserMode.structureMode();
        boolean sqlTooLong = isSqlTooLong(request.getSqlText());
        if (sqlTooLong) {
            result = buildInvalidResult(parseTaskId, request.getSqlText(), null, true);
            LOGGER.info(
                "operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} degradedReason={}",
                parseTaskId,
                tenantId,
                datasourceCode,
                System.currentTimeMillis() - start,
                result.getSyntaxStatus(),
                SQL_TOO_LONG_FAILURE_REASON
            );
        } else {
            try {
                profile = sqlOptimizationPipelineService.analyze(
                    request.getSqlText(),
                    DataSourceTypeEnum.AUTO,
                    structureParserMode
                );
                result = new StructureParseResult();
                result.setParseTaskId(parseTaskId);
                result.setSyntaxStatus(StructureParseSyntaxStatus.VALID);
                result.setComplexityLevel(resolveComplexity(profile));
                result.setSqlType("SELECT");
                result.setQueryDateSummary(extractQueryDateSummary(request.getSqlText(), profile));
                LogicalObjectExpansionResult logicalObjectExpansion = buildLogicalObjectHits(
                    profile,
                    tenantId,
                    datasourceCode,
                    structureParserMode
                );
                result.setLogicalObjectHits(logicalObjectExpansion.getHits());
                result.setRiskTags(buildRiskTags(profile, logicalObjectExpansion));
                result.setRewriteCandidates(sqlOptimizationPipelineService.deriveRewriteCandidateRules(profile));
                result.setIssues(buildIssues(profile, logicalObjectExpansion));
                result.applyAssessment(StructureParsePriorityScorer.assessAll(result.getIssues()));
                LOGGER.info(
                    "operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} priorityLevel={}",
                    parseTaskId,
                    tenantId,
                    datasourceCode,
                    System.currentTimeMillis() - start,
                    result.getSyntaxStatus(),
                    result.getPriorityLevel()
                );
            } catch (SqlOptimizationPipelineService.SqlOptimizationExecutionException ex) {
                result = buildInvalidResult(parseTaskId, request.getSqlText(), ex, false);
                LOGGER.info(
                    "operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} degradedReason={}",
                    parseTaskId,
                    tenantId,
                    datasourceCode,
                    System.currentTimeMillis() - start,
                    result.getSyntaxStatus(),
                    ex.getMessage()
                );
            }
        }
        StructureParseResponseVO response = toResponse(result);
        enrichQueryIntent(response, request.getSqlText(), profile);
        HetuPlanAnalysisResult planAnalysis = resolvePlanAnalysis(request, requestedParserMode, datasourceCode, response);
        response.setPlanAnalysis(toPlanAnalysisVO(planAnalysis));
        response.setStructureAnalysisStatus(resolveStructureAnalysisStatus(response).name());
        response.setAnalysisStatus(resolveAnalysisStatus(response, planAnalysis, requestedParserMode).name());
        if (!Boolean.FALSE.equals(request.getHistoryWriteEnabled())) {
            writeParseHistory(response, request, result);
        }
        return response;
    }

    private StructureParseResult buildInvalidResult(String parseTaskId,
                                                    String sqlText,
                                                    SqlOptimizationPipelineService.SqlOptimizationExecutionException ex,
                                                    boolean sqlTooLong) {
        HeuristicFallbackProfile heuristicProfile = analyzeHeuristicFallback(sqlText, sqlTooLong);
        SqlOptimizationPipelineService.SqlFailurePosition position = ex == null ? null : ex.getFailurePosition();
        String diagnosticFailureToken = sqlTooLong
            ? RISK_SQL_TOO_LONG
            : diagnosticFailureToken(position);
        String diagnosticFailureSnippet = sqlTooLong
            ? snippetAround(sqlText, 0, FAILURE_SNIPPET_TEXT_LIMIT)
            : diagnosticFailureSnippet(position);
        String failureReason = sqlTooLong
            ? SQL_TOO_LONG_FAILURE_REASON
            : compactDiagnosticText(failureReason(ex, position, diagnosticFailureToken), FAILURE_REASON_TEXT_LIMIT);
        String failureDetail = sqlTooLong
            ? compactDiagnosticText(
                SQL_TOO_LONG_FAILURE_REASON
                    + " sqlLength="
                    + heuristicProfile.getSqlLength()
                    + ", threshold="
                    + resolveSqlTextLengthLimit()
                    + ".",
                FAILURE_DETAIL_TEXT_LIMIT
            )
            : compactDiagnosticText(failureDetail(ex, diagnosticFailureToken, diagnosticFailureSnippet), FAILURE_DETAIL_TEXT_LIMIT);
        String failureToken = compactDiagnosticText(diagnosticFailureToken, FAILURE_TOKEN_TEXT_LIMIT);
        String failureSnippet = compactDiagnosticText(diagnosticFailureSnippet, FAILURE_SNIPPET_TEXT_LIMIT);
        List<StructureParseIssue> issues = new ArrayList<StructureParseIssue>();
        StructureParseIssue syntaxIssue = buildSyntaxInvalidIssue(
            failureReason,
            failureDetail,
            ex == null ? "Fix SQL syntax or reduce the statement before running full structure parse." : ex.getSuggestedAction(),
            position,
            failureToken,
            failureSnippet,
            sqlTooLong
        );
        issues.add(syntaxIssue);
        if (sqlTooLong) {
            issues.add(buildSqlTooLongIssue(failureReason, failureDetail, failureToken, failureSnippet));
        }
        StructureParseResult result = new StructureParseResult();
        result.setParseTaskId(parseTaskId);
        result.setSyntaxStatus(StructureParseSyntaxStatus.INVALID);
        result.setComplexityLevel(resolveHeuristicComplexity(heuristicProfile));
        result.setSqlType(heuristicProfile.getSqlType());
        result.setFailureReason(failureReason);
        if (position != null) {
            result.setFailureLine(position.getLine());
            result.setFailureColumn(position.getColumn());
            result.setFailureOffset(position.getOffset());
            result.setFailureToken(failureToken);
            result.setFailureSnippet(failureSnippet);
        } else if (sqlTooLong) {
            result.setFailureLine(Integer.valueOf(1));
            result.setFailureColumn(Integer.valueOf(1));
            result.setFailureOffset(Integer.valueOf(0));
            result.setFailureToken(failureToken);
            result.setFailureSnippet(failureSnippet);
        }
        result.setQueryDateSummary(buildHeuristicQueryDateSummary(heuristicProfile));
        result.setLogicalObjectHits(buildHeuristicLogicalObjectHits(heuristicProfile));
        result.setRiskTags(buildInvalidRiskTags(sqlTooLong));
        result.setRewriteCandidates(Collections.<String>emptyList());
        result.setIssues(issues);
        result.applyAssessment(StructureParsePriorityScorer.assessAll(issues));
        return result;
    }

    private StructureParseIssue buildSyntaxInvalidIssue(String failureReason,
                                                        String failureDetail,
                                                        String suggestedAction,
                                                        SqlOptimizationPipelineService.SqlFailurePosition position,
                                                        String failureToken,
                                                        String failureSnippet,
                                                        boolean sqlTooLong) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueCode(RISK_SQL_SYNTAX_INVALID);
        issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
        issue.setIssueScene(RISK_SQL_SYNTAX_INVALID);
        issue.setSeverity(StructureParseIssueSeverity.HIGH);
        issue.setSummary(failureReason);
        issue.setDetail(failureDetail);
        issue.setSuggestedAction(suggestedAction);
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.FALSE);
        issue.setAffectedSqlCount(Integer.valueOf(1));
        issue.setAffectedReportCount(Integer.valueOf(1));
        if (position != null) {
            issue.setFailureLine(position.getLine());
            issue.setFailureColumn(position.getColumn());
            issue.setFailureOffset(position.getOffset());
        } else if (sqlTooLong) {
            issue.setFailureLine(Integer.valueOf(1));
            issue.setFailureColumn(Integer.valueOf(1));
            issue.setFailureOffset(Integer.valueOf(0));
        }
        issue.setFailureToken(failureToken);
        issue.setFailureSnippet(failureSnippet);
        StructureParsePriorityScorer.assess(issue);
        return issue;
    }

    private StructureParseIssue buildSqlTooLongIssue(String failureReason,
                                                     String failureDetail,
                                                     String failureToken,
                                                     String failureSnippet) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueCode(RISK_SQL_TOO_LONG);
        issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
        issue.setIssueScene(RISK_SQL_TOO_LONG);
        issue.setSeverity(StructureParseIssueSeverity.HIGH);
        issue.setSummary(failureReason);
        issue.setDetail(failureDetail);
        issue.setSuggestedAction("Reduce SQL size, split the statement into smaller reviewed stages, or raise the parser limit explicitly.");
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.FALSE);
        issue.setAffectedSqlCount(Integer.valueOf(1));
        issue.setAffectedReportCount(Integer.valueOf(1));
        issue.setFailureLine(Integer.valueOf(1));
        issue.setFailureColumn(Integer.valueOf(1));
        issue.setFailureOffset(Integer.valueOf(0));
        issue.setFailureToken(failureToken);
        issue.setFailureSnippet(failureSnippet);
        StructureParsePriorityScorer.assess(issue);
        return issue;
    }

    private List<String> buildInvalidRiskTags(boolean sqlTooLong) {
        List<String> riskTags = new ArrayList<String>();
        riskTags.add(RISK_SQL_SYNTAX_INVALID);
        if (sqlTooLong) {
            riskTags.add(RISK_SQL_TOO_LONG);
        }
        return riskTags;
    }

    private boolean isSqlTooLong(String sqlText) {
        return sqlText != null && sqlText.length() > resolveSqlTextLengthLimit();
    }

    private int resolveSqlTextLengthLimit() {
        return maxSqlTextLength <= 0 ? DEFAULT_SQL_TEXT_LENGTH_LIMIT : maxSqlTextLength;
    }

    private HeuristicFallbackProfile analyzeHeuristicFallback(String sqlText, boolean sqlTooLong) {
        String boundedSql = boundedHeuristicSql(sqlText, sqlTooLong);
        String diagnosticSql = normalizeDiagnosticSqlForStaticScan(boundedSql == null ? "" : boundedSql);
        String commentStrippedSql = stripSqlComments(diagnosticSql);
        String scanSql = maskSingleQuotedLiterals(commentStrippedSql);
        HeuristicFallbackProfile profile = new HeuristicFallbackProfile();
        profile.setSqlTooLong(sqlTooLong);
        profile.setSqlLength(sqlText == null ? 0 : sqlText.length());
        profile.setBoundedSqlLength(boundedSql == null ? 0 : boundedSql.length());
        profile.setSqlType(inferSqlType(scanSql));
        profile.setTables(extractHeuristicObjects(scanSql));
        profile.setJoinCount(countMatches(HEURISTIC_JOIN_PATTERN, scanSql));
        profile.setPredicateCount(countHeuristicPredicates(scanSql));
        profile.setOrPredicateCount(countMatches(HEURISTIC_OR_PATTERN, scanSql));
        profile.setDatePredicateColumns(extractHeuristicDatePredicateColumns(scanSql));
        profile.setDates(extractDates(commentStrippedSql));
        return profile;
    }

    private String boundedHeuristicSql(String sqlText, boolean sqlTooLong) {
        if (sqlText == null) {
            return "";
        }
        if (!sqlTooLong || sqlText.length() <= HEURISTIC_SQL_WINDOW_LIMIT) {
            return sqlText;
        }
        return sqlText.substring(0, HEURISTIC_SQL_WINDOW_LIMIT);
    }

    private String inferSqlType(String sqlText) {
        String normalized = trimToNull(sqlText);
        if (normalized == null) {
            return "UNKNOWN";
        }
        Matcher matcher = HEURISTIC_SQL_TYPE_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return "UNKNOWN";
        }
        String token = matcher.group(1).toUpperCase(Locale.ROOT);
        return "WITH".equals(token) ? "SELECT" : token;
    }

    private List<String> extractHeuristicObjects(String scanSql) {
        if (!StringUtils.hasText(scanSql)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> objects = new LinkedHashSet<String>();
        Matcher matcher = HEURISTIC_OBJECT_PATTERN.matcher(scanSql);
        while (matcher.find() && objects.size() < 64) {
            String candidate = normalizeHeuristicObjectName(matcher.group(1));
            if (isLikelyHeuristicObjectName(candidate)) {
                objects.add(candidate);
            }
        }
        return new ArrayList<String>(objects);
    }

    private String normalizeHeuristicObjectName(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return normalized.replaceAll("\\s*\\.\\s*", ".").trim();
    }

    private boolean isLikelyHeuristicObjectName(String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized == null) {
            return false;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        String firstToken = upper.split("\\s+", 2)[0];
        return !"SELECT".equals(firstToken)
            && !"WHERE".equals(firstToken)
            && !"JOIN".equals(firstToken)
            && !"ON".equals(firstToken)
            && !"GROUP".equals(firstToken)
            && !"ORDER".equals(firstToken)
            && !"HAVING".equals(firstToken)
            && !"LIMIT".equals(firstToken)
            && !"UNNEST".equals(firstToken)
            && !"VALUES".equals(firstToken)
            && !"LATERAL".equals(firstToken);
    }

    private int countHeuristicPredicates(String scanSql) {
        Matcher whereMatcher = HEURISTIC_WHERE_PATTERN.matcher(scanSql == null ? "" : scanSql);
        if (!whereMatcher.find()) {
            return 0;
        }
        String predicateTail = scanSql.substring(whereMatcher.end());
        return 1 + countMatches(HEURISTIC_BOOLEAN_PATTERN, predicateTail);
    }

    private int countMatches(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private List<String> extractHeuristicDatePredicateColumns(String scanSql) {
        LinkedHashSet<String> columns = new LinkedHashSet<String>();
        Matcher matcher = HEURISTIC_DATE_PREDICATE_PATTERN.matcher(scanSql == null ? "" : scanSql.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String column = trimToNull(matcher.group(1));
            if (column != null) {
                columns.add(column);
            }
        }
        return new ArrayList<String>(columns);
    }

    private List<LocalDate> extractDates(String sqlText) {
        List<LocalDate> dates = new ArrayList<LocalDate>();
        Matcher matcher = ISO_DATE_PATTERN.matcher(sqlText == null ? "" : sqlText);
        while (matcher.find()) {
            LocalDate parsed = tryParseDate(matcher.group(1));
            if (parsed != null) {
                dates.add(parsed);
            }
        }
        dates.sort(Comparator.naturalOrder());
        return dates;
    }

    private String normalizeDiagnosticSqlForStaticScan(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int index = 0;
        while (index < sqlText.length()) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                while (index < sqlText.length()) {
                    char item = sqlText.charAt(index);
                    builder.append(item);
                    index++;
                    if (item == '\n' || item == '\r') {
                        break;
                    }
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '/' && next == '*') {
                builder.append(current);
                builder.append(next);
                index += 2;
                while (index < sqlText.length()) {
                    char item = sqlText.charAt(index);
                    char following = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
                    builder.append(item);
                    index++;
                    if (item == '*' && following == '/') {
                        builder.append(following);
                        index++;
                        break;
                    }
                }
                continue;
            }
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                builder.append(current);
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                index++;
                continue;
            }
            if (current == '"' && !inSingleQuote && !inBacktick) {
                builder.append(current);
                if (inDoubleQuote && next == '"') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inDoubleQuote = !inDoubleQuote;
                index++;
                continue;
            }
            if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                builder.append(current);
                if (inBacktick && next == '`') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inBacktick = !inBacktick;
                index++;
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && isDiagnosticNoiseCharacter(current)) {
                builder.append(' ');
            } else {
                builder.append(current);
            }
            index++;
        }
        return builder.toString();
    }

    private boolean isDiagnosticNoiseCharacter(char value) {
        return value == '@' || Character.UnicodeScript.of(value) == Character.UnicodeScript.HAN;
    }

    private StructureParseQueryDateSummary buildHeuristicQueryDateSummary(HeuristicFallbackProfile profile) {
        StructureParseQueryDateSummary summary = new StructureParseQueryDateSummary();
        summary.setQueryDateFields(profile.getDatePredicateColumns());
        if (!profile.getDates().isEmpty()) {
            summary.setQueryDateStart(profile.getDates().get(0).format(ISO_DATE));
            summary.setQueryDateEnd(profile.getDates().get(profile.getDates().size() - 1).format(ISO_DATE));
            summary.setQueryDateStatus(StructureParseQueryDateStatus.RESOLVED);
            return summary;
        }
        if (!profile.getDatePredicateColumns().isEmpty()) {
            summary.setQueryDateStatus(StructureParseQueryDateStatus.PARTIAL);
            return summary;
        }
        summary.setQueryDateStatus(StructureParseQueryDateStatus.UNRESOLVED);
        return summary;
    }

    private List<StructureParseLogicalObjectHit> buildHeuristicLogicalObjectHits(HeuristicFallbackProfile profile) {
        if (profile == null || profile.getTables().isEmpty()) {
            return Collections.emptyList();
        }
        LogicalObjectExpansionResult result = new LogicalObjectExpansionResult();
        for (String table : profile.getTables()) {
            QualifiedObjectName qualifiedObject = QualifiedObjectName.parse(table);
            LogicalObjectType objectType = isDbViewHeuristic(qualifiedObject.getQualifiedName())
                ? LogicalObjectType.DB_VIEW
                : LogicalObjectType.TABLE;
            StructureParseLogicalObjectHit hit = buildLogicalObjectHit(
                objectType,
                qualifiedObject,
                MATCH_SOURCE_HEURISTIC_FALLBACK,
                Boolean.FALSE
            );
            hit.setMappedPhysicalTargets(objectType == LogicalObjectType.TABLE
                ? Collections.singletonList(hit.getObjectKey())
                : Collections.<String>emptyList());
            result.addHit(hit);
        }
        return result.getHits();
    }

    private StructureParseComplexityLevel resolveHeuristicComplexity(HeuristicFallbackProfile profile) {
        int score = profile.getJoinCount() * 2 + profile.getPredicateCount() + profile.getTables().size();
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

    private String stripSqlComments(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        int index = 0;
        while (index < sqlText.length()) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '-' && next == '-') {
                builder.append(' ');
                index += 2;
                while (index < sqlText.length()) {
                    char item = sqlText.charAt(index);
                    if (item == '\n' || item == '\r') {
                        builder.append(item);
                        break;
                    }
                    builder.append(' ');
                    index++;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick && current == '/' && next == '*') {
                builder.append(' ');
                index += 2;
                while (index + 1 < sqlText.length()) {
                    if (sqlText.charAt(index) == '*' && sqlText.charAt(index + 1) == '/') {
                        builder.append(' ');
                        index += 2;
                        break;
                    }
                    builder.append(sqlText.charAt(index) == '\n' ? '\n' : ' ');
                    index++;
                }
                continue;
            }
            builder.append(current);
            if (current == '\'' && !inDoubleQuote && !inBacktick) {
                if (inSingleQuote && next == '\'') {
                    builder.append(next);
                    index += 2;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
            } else if (current == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
            } else if (current == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
            }
            index++;
        }
        return builder.toString();
    }

    private String maskSingleQuotedLiterals(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sqlText.length());
        boolean inSingleQuote = false;
        for (int index = 0; index < sqlText.length(); index++) {
            char current = sqlText.charAt(index);
            char next = index + 1 < sqlText.length() ? sqlText.charAt(index + 1) : '\0';
            if (current == '\'') {
                builder.append(' ');
                if (inSingleQuote && next == '\'') {
                    builder.append(' ');
                    index++;
                    continue;
                }
                inSingleQuote = !inSingleQuote;
                continue;
            }
            builder.append(inSingleQuote && current != '\n' && current != '\r' ? ' ' : current);
        }
        return builder.toString();
    }

    private String snippetAround(String sqlText, int offset, int limit) {
        if (!StringUtils.hasText(sqlText)) {
            return null;
        }
        int safeOffset = Math.max(0, Math.min(offset, sqlText.length()));
        int radius = Math.max(8, limit / 2);
        int start = Math.max(0, safeOffset - radius);
        int end = Math.min(sqlText.length(), safeOffset + radius);
        return compactDiagnosticText(sqlText.substring(start, end), limit);
    }

    private String compactDiagnosticText(String value, int limit) {
        return SqlParseDiagnosticSupport.compactDiagnosticText(value, limit);
    }

    private HetuPlanAnalysisResult resolvePlanAnalysis(StructureParseRequest request,
                                                       SqlParserMode requestedParserMode,
                                                       String datasourceCode,
                                                       StructureParseResponseVO structureParse) {
        if (requestedParserMode == null || !requestedParserMode.requiresPlanAnalysis()) {
            return HetuPlanAnalysisResult.skipped(
                datasourceCode,
                "PARSER_MODE_WITHOUT_PLAN",
                Collections.singletonList("parserMode=" + (requestedParserMode == null ? SqlParserMode.DEFAULT_VALUE : requestedParserMode.name()))
            );
        }
        if (structureParse != null
            && !"VALID".equals(structureParse.getSyntaxStatus())
            && containsValue(structureParse.getRiskTags(), RISK_SQL_TOO_LONG)) {
            return HetuPlanAnalysisResult.failed(
                datasourceCode,
                "SQL_TOO_LONG_PLAN_ANALYSIS_SKIPPED",
                0L,
                Arrays.asList(
                    "parserMode=" + requestedParserMode.name(),
                    "reason=" + SQL_TOO_LONG_FAILURE_REASON
                )
            );
        }
        long start = System.currentTimeMillis();
        try {
            HetuPlanAnalysisResult result = hetuPlanAnalysisClient.explain(request.getSqlText(), datasourceCode);
            if (result == null) {
                return HetuPlanAnalysisResult.failed(
                    datasourceCode,
                    "HETU_PLAN_CLIENT_RETURNED_NULL",
                    System.currentTimeMillis() - start,
                    Collections.singletonList("parserMode=" + requestedParserMode.name())
                );
            }
            return result;
        } catch (RuntimeException ex) {
            return HetuPlanAnalysisResult.failed(
                datasourceCode,
                "HETU_PLAN_EXPLAIN_FAILED: " + ex.getMessage(),
                System.currentTimeMillis() - start,
                Collections.singletonList("parserMode=" + requestedParserMode.name())
            );
        }
    }

    private ParseAnalysisStatus resolveStructureAnalysisStatus(StructureParseResponseVO response) {
        return response != null && "VALID".equals(response.getSyntaxStatus())
            ? ParseAnalysisStatus.SUCCESS
            : ParseAnalysisStatus.FAILED;
    }

    private ParseAnalysisStatus resolveAnalysisStatus(StructureParseResponseVO response,
                                                      HetuPlanAnalysisResult planAnalysis,
                                                      SqlParserMode requestedParserMode) {
        boolean structureSuccess = resolveStructureAnalysisStatus(response) == ParseAnalysisStatus.SUCCESS;
        if (requestedParserMode == null || !requestedParserMode.requiresPlanAnalysis()) {
            return structureSuccess ? ParseAnalysisStatus.SUCCESS : ParseAnalysisStatus.FAILED;
        }
        boolean planSuccess = planAnalysis != null && planAnalysis.getStatus() == PlanAnalysisStatus.SUCCESS;
        if (structureSuccess && planSuccess) {
            return ParseAnalysisStatus.SUCCESS;
        }
        if (structureSuccess || planSuccess) {
            return ParseAnalysisStatus.PARTIAL_SUCCESS;
        }
        return ParseAnalysisStatus.FAILED;
    }

    private PlanAnalysisVO toPlanAnalysisVO(HetuPlanAnalysisResult result) {
        PlanAnalysisVO vo = new PlanAnalysisVO();
        if (result == null) {
            vo.setStatus(PlanAnalysisStatus.FAILED.name());
            vo.setFailureReason("HETU_PLAN_RESULT_MISSING");
            vo.setEvidence(Collections.singletonList("result=missing"));
            return vo;
        }
        vo.setStatus(result.getStatus() == null ? null : result.getStatus().name());
        vo.setPlanText(result.getPlanText());
        vo.setDatasourceCode(result.getDatasourceCode());
        vo.setCostMs(result.getCostMs());
        vo.setFailureReason(result.getFailureReason());
        vo.setEvidence(result.getEvidence());
        return vo;
    }

    private String failureReason(SqlOptimizationPipelineService.SqlOptimizationExecutionException ex,
                                 SqlOptimizationPipelineService.SqlFailurePosition position,
                                 String diagnosticFailureToken) {
        StringBuilder builder = new StringBuilder();
        builder.append(isNoiseFailureToken(position, diagnosticFailureToken)
            ? "SQL syntax parser rejected the statement."
            : normalizeDiagnosticText(ex.getMessage()));
        if (position != null && position.getLine() != null && position.getColumn() != null) {
            builder.append(" Failure position: line ")
                .append(position.getLine())
                .append(", column ")
                .append(position.getColumn())
                .append('.');
        }
        return builder.toString();
    }

    private String failureDetail(SqlOptimizationPipelineService.SqlOptimizationExecutionException ex,
                                 String diagnosticFailureToken,
                                 String diagnosticFailureSnippet) {
        String parserMessage = normalizeDiagnosticText(
            ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage()
        );
        StringBuilder builder = new StringBuilder();
        builder.append(parserMessage);
        if (diagnosticFailureToken != null) {
            builder.append(" Token: ").append(diagnosticFailureToken).append('.');
        }
        if (diagnosticFailureSnippet != null) {
            builder.append(" Near: ").append(diagnosticFailureSnippet).append('.');
        }
        return builder.toString();
    }

    private String diagnosticFailureToken(SqlOptimizationPipelineService.SqlFailurePosition position) {
        if (position == null || !StringUtils.hasText(position.getToken())) {
            return null;
        }
        String normalized = trimToNull(normalizeDiagnosticSqlForStaticScan(position.getToken()));
        return normalized == null ? RISK_SQL_SYNTAX_INVALID : normalized;
    }

    private String diagnosticFailureSnippet(SqlOptimizationPipelineService.SqlFailurePosition position) {
        if (position == null || !StringUtils.hasText(position.getSnippet())) {
            return null;
        }
        return trimToNull(normalizeDiagnosticSqlForStaticScan(position.getSnippet()));
    }

    private boolean isNoiseFailureToken(SqlOptimizationPipelineService.SqlFailurePosition position,
                                        String diagnosticFailureToken) {
        return position != null
            && StringUtils.hasText(position.getToken())
            && RISK_SQL_SYNTAX_INVALID.equals(diagnosticFailureToken)
            && trimToNull(normalizeDiagnosticSqlForStaticScan(position.getToken())) == null;
    }

    private String normalizeDiagnosticText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "SQL syntax parser rejected the statement.";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            builder.append(isDiagnosticNoiseCharacter(current) ? ' ' : current);
        }
        String diagnosticText = compactDiagnosticText(builder.toString(), FAILURE_DETAIL_TEXT_LIMIT);
        return diagnosticText == null ? "SQL syntax parser rejected the statement." : diagnosticText;
    }

    private List<String> buildRiskTags(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                       LogicalObjectExpansionResult logicalObjectExpansion) {
        List<String> riskTags = new ArrayList<String>(profile.getWarnings());
        if (logicalObjectExpansion != null && logicalObjectExpansion.hasUnresolvedViewDefinition()) {
            riskTags.add(RISK_DB_VIEW_DEFINITION_UNRESOLVED);
        }
        return riskTags;
    }

    private List<StructureParseIssue> buildIssues(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                  LogicalObjectExpansionResult logicalObjectExpansion) {
        List<StructureParseIssue> issues = new ArrayList<StructureParseIssue>();
        for (String warning : profile.getWarnings()) {
            issues.add(buildIssueFromWarning(warning, profile));
        }
        if (logicalObjectExpansion != null && logicalObjectExpansion.hasUnresolvedViewDefinition()) {
            issues.add(buildDbViewDefinitionUnresolvedIssue(logicalObjectExpansion));
        }
        return issues;
    }

    private StructureParseIssue buildDbViewDefinitionUnresolvedIssue(LogicalObjectExpansionResult logicalObjectExpansion) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueCode(RISK_DB_VIEW_DEFINITION_UNRESOLVED);
        issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
        issue.setIssueScene("METADATA_UNRESOLVED");
        issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
        issue.setSummary("A database view definition could not be fully expanded from live metadata.");
        issue.setDetail(String.join("; ", logicalObjectExpansion.getUnresolvedReasons()));
        issue.setSuggestedAction("Verify datasource metadata permissions or refresh the governed DB view catalog fallback.");
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.FALSE);
        issue.setAffectedSqlCount(Integer.valueOf(1));
        issue.setAffectedReportCount(Integer.valueOf(1));
        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);
        issue.setPriorityScore(Integer.valueOf(assessment.getPriorityScore()));
        issue.setPriorityLevel(assessment.getPriorityLevel());
        return issue;
    }

    private StructureParseIssue buildIssueFromWarning(String warning,
                                                      SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        StructureParseIssue issue = new StructureParseIssue();
        if ("SELECT_STAR".equals(warning)) {
            issue.setIssueCode("SELECT_STAR");
            issue.setIssueDomain(StructureParseIssueDomain.GOVERNANCE);
            issue.setIssueScene("WIDE_PROJECTION");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement uses SELECT * and hides the actual column footprint.");
            issue.setDetail("Wide projections weaken rewrite determinism and increase unnecessary data movement.");
            issue.setSuggestedAction("Replace star projection with explicit columns before optimization review.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("NO_PREDICATE".equals(warning)) {
            issue.setIssueCode("FULL_SCAN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement has no filtering predicate and is likely to scan the full dataset.");
            issue.setDetail("A full scan on a read path usually indicates missing partition or business-key filters.");
            issue.setSuggestedAction("Add tenant, time, or business-key predicates before online execution.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("ORDER_BY_WITHOUT_LIMIT".equals(warning)) {
            issue.setIssueCode("ORDER_BY_WITHOUT_LIMIT");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement sorts without a limiting clause.");
            issue.setDetail("Unbounded sort operations can create unnecessary shuffle or memory pressure.");
            issue.setSuggestedAction("Add LIMIT or move ordering work to a serving object.");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("HEAVY_JOIN_GRAPH".equals(warning)) {
            issue.setIssueCode("HEAVY_JOIN_GRAPH");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement joins multiple datasets and may require staged execution.");
            issue.setDetail("Large join graphs should be reviewed for filter placement, join keys, and serving alternatives.");
            issue.setSuggestedAction("Review join graph, key selectivity, and acceleration opportunities.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LARGE_JOIN_PAIR_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_JOIN_PAIR_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement has limited static evidence for join conditions or selectivity.");
            issue.setDetail("Static structure parsing cannot prove table size or runtime cost; it only flags that join evidence is incomplete.");
            issue.setSuggestedAction("Confirm join keys, filter placement, and selectivity with Access Parse or benchmark evidence before online use.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("REPEATED_EXPRESSION_COMPUTE".equals(warning)) {
            issue.setIssueCode("REPEATED_EXPRESSION_COMPUTE");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement repeats expression fragments that may be computed more than once.");
            issue.setDetail("Repeated projection, filter, grouping, or ordering expressions can increase CPU cost.");
            issue.setSuggestedAction("Deduplicate expressions or move shared calculations into a CTE or serving object.");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LARGE_RESULT_SET_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_RESULT_SET_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("WIDE_PROJECTION");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement may return an oversized result set.");
            issue.setDetail("Wide projection or missing LIMIT/filter evidence can produce too much data for interactive use.");
            issue.setSuggestedAction("Add explicit projection, filters, or LIMIT before using the query in an interactive path.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("SCALAR_SUBQUERY_IN_SELECT".equals(warning)) {
            issue.setIssueCode("SCALAR_SUBQUERY_IN_SELECT");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The SELECT list contains scalar subqueries.");
            issue.setDetail("Scalar subqueries in projection can repeatedly execute lookup or aggregate logic per output row.");
            issue.setSuggestedAction("Rewrite scalar subqueries as joins, pre-aggregated CTEs, or serving objects.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("NESTED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("NESTED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement contains multiple nested subqueries.");
            issue.setDetail("Deep subquery nesting makes filter placement, join order, and runtime cost harder to reason about statically.");
            issue.setSuggestedAction("Flatten the query into named CTE stages and review each stage independently.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("CORRELATED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("CORRELATED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement contains correlated subquery references.");
            issue.setDetail("Correlated subqueries can create repeated lookups or decorrelation pressure during planning.");
            issue.setSuggestedAction("Rewrite correlated subqueries into explicit joins or pre-aggregated CTEs.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("FUNCTION_WRAPPED_PREDICATE".equals(warning)) {
            issue.setIssueCode("FUNCTION_WRAPPED_PREDICATE");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("A filtering predicate wraps a column in a function.");
            issue.setDetail("Function-wrapped predicates can block partition pruning or index-style pushdown.");
            issue.setSuggestedAction("Rewrite the predicate as a range or normalized column comparison when possible.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("NOT_EXISTS_ANTI_JOIN_RISK".equals(warning)) {
            issue.setIssueCode("NOT_EXISTS_ANTI_JOIN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement uses NOT EXISTS anti-join logic.");
            issue.setDetail("Anti-join logic should be reviewed for null semantics, selectivity, and join placement.");
            issue.setSuggestedAction("Consider a staged LEFT JOIN ... IS NULL rewrite only after semantic validation.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LEADING_WILDCARD_LIKE_RISK".equals(warning)) {
            issue.setIssueCode("LEADING_WILDCARD_LIKE_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("A LIKE predicate starts with a wildcard.");
            issue.setDetail("Leading wildcard filters usually cannot use prefix pruning and can force wider scans.");
            issue.setSuggestedAction("Use a normalized search key, inverted index, or prefixable predicate when possible.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("OR_PREDICATE_INDEX_RISK".equals(warning)) {
            issue.setIssueCode("OR_PREDICATE_INDEX_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The WHERE clause combines alternatives with OR.");
            issue.setDetail("OR predicates can weaken static pruning and may need union-based staging for predictable access paths.");
            issue.setSuggestedAction("Review whether UNION ALL branches or staged filters make the query easier to optimize.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("ORDER_BY_RANDOM_RISK".equals(warning)) {
            issue.setIssueCode("ORDER_BY_RANDOM_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement orders rows by a random function.");
            issue.setDetail("ORDER BY RAND/RANDOM forces expensive randomization and sort-style work before limiting results.");
            issue.setSuggestedAction("Use sampled source data or deterministic sampling keys instead of random ordering.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("REPEATED_TABLE_SCAN_RISK".equals(warning)) {
            issue.setIssueCode("REPEATED_TABLE_SCAN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement references the same table multiple times.");
            issue.setDetail("Repeated table scans can amplify IO, CPU, and shuffle cost when subqueries are not staged.");
            issue.setSuggestedAction("Pre-stage repeated inputs with CTEs or serving objects and reuse them explicitly.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("ORDER_BY_COMPLEXITY_RISK".equals(warning)) {
            issue.setIssueCode("ORDER_BY_COMPLEXITY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement contains multiple or redundant ORDER BY keys.");
            issue.setDetail("Static analysis found sort-key breadth, duplicate ordering, or sort plus aggregation/grouping pressure.");
            issue.setSuggestedAction("Reduce sort keys, remove duplicate ordering, or validate sorted serving output with plan evidence.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("JOIN_LATENCY_RISK".equals(warning)) {
            issue.setIssueCode("JOIN_LATENCY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The join graph has static signals for long-running execution.");
            issue.setDetail("Multiple joins, weak static join-condition evidence, or join plus subquery composition can increase latency.");
            issue.setSuggestedAction("Confirm join keys, filter placement, scanned volume, and row movement with access parse or benchmark evidence.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("AGGREGATION_COMPLEXITY_RISK".equals(warning)) {
            issue.setIssueCode("AGGREGATION_COMPLEXITY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement combines enough aggregation work to need review.");
            issue.setDetail("Aggregate count, grouping breadth, ordering, or string aggregation can increase CPU and memory pressure.");
            issue.setSuggestedAction("Pre-aggregate reusable stages or move heavy aggregate output into reviewed serving objects.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("GROUP_BY_WITHOUT_AGGREGATE_RISK".equals(warning)) {
            issue.setIssueCode("GROUP_BY_WITHOUT_AGGREGATE_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement uses GROUP BY without aggregate functions.");
            issue.setDetail("A GROUP BY without aggregation is often a DISTINCT-style de-duplication or redundant grouping operation.");
            issue.setSuggestedAction("Use DISTINCT for de-duplication intent or remove the grouping if it is redundant.");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("DUPLICATE_GROUP_OR_ORDER_KEY_RISK".equals(warning)) {
            issue.setIssueCode("DUPLICATE_GROUP_OR_ORDER_KEY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("The statement repeats GROUP BY or ORDER BY keys.");
            issue.setDetail("Duplicate grouping or ordering keys add unnecessary logical-plan work and can obscure query intent.");
            issue.setSuggestedAction("Remove duplicate grouping or ordering keys before rewrite or acceleration review.");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("REPEATED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("REPEATED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement repeats a normalized subquery shape.");
            issue.setDetail("Repeated nested or scalar subqueries can re-plan or re-execute the same lookup/aggregate work.");
            issue.setSuggestedAction("Extract repeated subqueries into a named CTE or reviewed serving object.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("LARGE_STRING_RESULT_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_STRING_RESULT_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("WIDE_PROJECTION");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The statement may produce a large string-heavy result.");
            issue.setDetail("String projections, concatenation, or string aggregation can expand returned bytes even when row count is unknown.");
            issue.setSuggestedAction("Limit string projections, avoid unbounded string aggregation, or validate returned bytes through access parse or benchmark.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("COMPLEX_QUERY_GRAPH_RISK".equals(warning)) {
            issue.setIssueCode("COMPLEX_QUERY_GRAPH_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("The query graph is too complex for a lightweight interactive path.");
            issue.setDetail("The static structure combines joins, predicates, and nested subqueries into a high-risk graph.");
            issue.setSuggestedAction("Break the SQL into reviewed stages and run access parse or benchmark before online use.");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else {
            issue.setIssueCode(warning);
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.LOW);
            issue.setSummary("The parser recorded a conservative warning flag.");
            issue.setDetail("The warning is preserved so later governance phases can refine it with stronger context.");
            issue.setSuggestedAction("Review the parse artifact and decide whether deeper analysis is needed.");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        }
        issue.setAffectedSqlCount(Integer.valueOf(1));
        issue.setAffectedReportCount(Integer.valueOf(resolveAffectedReportCount(profile)));
        StructureParsePriorityAssessment assessment = StructureParsePriorityScorer.assess(issue);
        issue.setPriorityScore(Integer.valueOf(assessment.getPriorityScore()));
        issue.setPriorityLevel(assessment.getPriorityLevel());
        return issue;
    }

    private int resolveAffectedReportCount(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        return profile.getDatePredicateColumns().isEmpty() ? 1 : Math.min(3, profile.getDatePredicateColumns().size());
    }

    private StructureParseQueryDateSummary extractQueryDateSummary(String sqlText,
                                                                   SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<LocalDate> dates = new ArrayList<LocalDate>();
        Matcher matcher = ISO_DATE_PATTERN.matcher(sqlText == null ? "" : sqlText);
        while (matcher.find()) {
            LocalDate parsed = tryParseDate(matcher.group(1));
            if (parsed != null) {
                dates.add(parsed);
            }
        }
        dates.sort(Comparator.naturalOrder());
        StructureParseQueryDateSummary summary = new StructureParseQueryDateSummary();
        summary.setQueryDateFields(new ArrayList<String>(profile.getDatePredicateColumns()));
        if (!dates.isEmpty()) {
            summary.setQueryDateStart(dates.get(0).format(ISO_DATE));
            summary.setQueryDateEnd(dates.get(dates.size() - 1).format(ISO_DATE));
            summary.setQueryDateStatus(StructureParseQueryDateStatus.RESOLVED);
            return summary;
        }
        if (!profile.getDatePredicateColumns().isEmpty()) {
            summary.setQueryDateStatus(StructureParseQueryDateStatus.PARTIAL);
            return summary;
        }
        summary.setQueryDateStatus(StructureParseQueryDateStatus.UNRESOLVED);
        return summary;
    }

    private LocalDate tryParseDate(String candidate) {
        if (!StringUtils.hasText(candidate)) {
            return null;
        }
        try {
            return LocalDate.parse(candidate, ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LogicalObjectExpansionResult buildLogicalObjectHits(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                               String tenantId,
                                                               String datasourceCode,
                                                               SqlParserMode parserMode) {
        LogicalObjectExpansionResult result = new LogicalObjectExpansionResult();
        ViewExpansionContext context = new ViewExpansionContext(tenantId, datasourceCode, parserMode, result);
        for (String table : profile.getTables()) {
            expandObjectReference(table, MATCH_SOURCE_SQL, 0, context);
        }
        return result;
    }

    private List<String> expandObjectReference(String objectName,
                                               String matchSource,
                                               int depth,
                                               ViewExpansionContext context) {
        QualifiedObjectName qualifiedObject = QualifiedObjectName.parse(objectName);
        DatasourceViewMetadataResponse metadata = resolveLiveViewMetadata(qualifiedObject, context);
        if (metadata != null && Boolean.TRUE.equals(metadata.getView())) {
            return expandLiveDbView(qualifiedObject, metadata, depth, context);
        }
        GovernanceDbViewResolveResponse fallback = null;
        if (metadata == null || !Boolean.TRUE.equals(metadata.getResolved())) {
            fallback = resolveGovernanceDbViewFallback(qualifiedObject, context);
        }
        if (fallback != null && Boolean.TRUE.equals(fallback.getResolved())) {
            return applyGovernanceDbViewFallback(qualifiedObject, fallback, context);
        }
        if (isDbViewHeuristic(qualifiedObject.getQualifiedName())) {
            StructureParseLogicalObjectHit hit = buildLogicalObjectHit(
                LogicalObjectType.DB_VIEW,
                qualifiedObject,
                MATCH_SOURCE_HEURISTIC,
                Boolean.FALSE
            );
            hit.setMappedPhysicalTargets(Collections.<String>emptyList());
            context.getResult().addHit(hit);
            context.getResult().addUnresolvedReason(
                "view=" + hit.getObjectKey() + ", reason="
                    + metadataFailureReason(metadata, "DB_VIEW_HEURISTIC_WITHOUT_DEFINITION")
            );
            return Collections.emptyList();
        }
        StructureParseLogicalObjectHit tableHit = buildLogicalObjectHit(
            LogicalObjectType.TABLE,
            qualifiedObject,
            matchSource,
            Boolean.valueOf(metadata == null || Boolean.TRUE.equals(metadata.getResolved()))
        );
        tableHit.setMappedPhysicalTargets(Collections.singletonList(tableHit.getObjectKey()));
        context.getResult().addHit(tableHit);
        return Collections.singletonList(tableHit.getObjectKey());
    }

    private List<String> expandLiveDbView(QualifiedObjectName qualifiedObject,
                                          DatasourceViewMetadataResponse metadata,
                                          int depth,
                                          ViewExpansionContext context) {
        StructureParseLogicalObjectHit viewHit = buildLogicalObjectHit(
            LogicalObjectType.DB_VIEW,
            qualifiedObject,
            MATCH_SOURCE_DB_VIEW_METADATA,
            Boolean.TRUE
        );
        context.getResult().addHit(viewHit);
        String visitKey = context.visitKey(viewHit.getObjectKey());
        if (context.isVisited(visitKey)) {
            viewHit.setResolved(Boolean.FALSE);
            context.getResult().addUnresolvedReason("view=" + viewHit.getObjectKey() + ", reason=DB_VIEW_DEFINITION_CYCLE");
            return Collections.emptyList();
        }
        if (depth >= MAX_DB_VIEW_EXPANSION_DEPTH) {
            viewHit.setResolved(Boolean.FALSE);
            context.getResult().addUnresolvedReason("view=" + viewHit.getObjectKey() + ", reason=DB_VIEW_DEFINITION_MAX_DEPTH");
            return Collections.emptyList();
        }
        String viewDefinitionSql = extractViewDefinitionBody(metadata.getViewDefinitionSql());
        if (!StringUtils.hasText(viewDefinitionSql)) {
            return expandDbViewFromFallbackOrMarkUnresolved(qualifiedObject, viewHit, context, "DB_VIEW_DEFINITION_EMPTY");
        }
        context.pushVisited(visitKey);
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile viewProfile = sqlOptimizationPipelineService.analyze(
                viewDefinitionSql,
                DataSourceTypeEnum.AUTO,
                context.getParserMode()
            );
            LinkedHashSet<String> leafTableKeys = new LinkedHashSet<String>();
            for (String dependency : viewProfile.getTables()) {
                leafTableKeys.addAll(expandObjectReference(dependency, MATCH_SOURCE_DB_VIEW_DEFINITION, depth + 1, context));
            }
            viewHit.setMappedPhysicalTargets(new ArrayList<String>(leafTableKeys));
            if (leafTableKeys.isEmpty()) {
                viewHit.setResolved(Boolean.FALSE);
                context.getResult().addUnresolvedReason(
                    "view=" + viewHit.getObjectKey() + ", reason=DB_VIEW_DEFINITION_NO_TABLE_DEPENDENCIES"
                );
            }
            return new ArrayList<String>(leafTableKeys);
        } catch (SqlOptimizationPipelineService.SqlOptimizationExecutionException ex) {
            return expandDbViewFromFallbackOrMarkUnresolved(
                qualifiedObject,
                viewHit,
                context,
                "DB_VIEW_DEFINITION_PARSE_FAILED"
            );
        } finally {
            context.popVisited(visitKey);
        }
    }

    private List<String> expandDbViewFromFallbackOrMarkUnresolved(QualifiedObjectName qualifiedObject,
                                                                  StructureParseLogicalObjectHit viewHit,
                                                                  ViewExpansionContext context,
                                                                  String reason) {
        GovernanceDbViewResolveResponse fallback = resolveGovernanceDbViewFallback(qualifiedObject, context);
        if (fallback != null && Boolean.TRUE.equals(fallback.getResolved())) {
            List<String> leafTableKeys = applyGovernanceDbViewDependencies(viewHit, fallback, context);
            if (!leafTableKeys.isEmpty()) {
                return leafTableKeys;
            }
        }
        viewHit.setResolved(Boolean.FALSE);
        viewHit.setMappedPhysicalTargets(Collections.<String>emptyList());
        context.getResult().addUnresolvedReason("view=" + viewHit.getObjectKey() + ", reason=" + reason);
        return Collections.emptyList();
    }

    private List<String> applyGovernanceDbViewFallback(QualifiedObjectName qualifiedObject,
                                                       GovernanceDbViewResolveResponse fallback,
                                                       ViewExpansionContext context) {
        StructureParseLogicalObjectHit viewHit = buildLogicalObjectHit(
            LogicalObjectType.DB_VIEW,
            qualifiedObject,
            MATCH_SOURCE_DB_VIEW_CATALOG_FALLBACK,
            Boolean.TRUE
        );
        if (StringUtils.hasText(fallback.getObjectKey())) {
            viewHit.setObjectKey(fallback.getObjectKey());
        }
        context.getResult().addHit(viewHit);
        return applyGovernanceDbViewDependencies(viewHit, fallback, context);
    }

    private List<String> applyGovernanceDbViewDependencies(StructureParseLogicalObjectHit viewHit,
                                                           GovernanceDbViewResolveResponse fallback,
                                                           ViewExpansionContext context) {
        LinkedHashSet<String> dependencyKeys = new LinkedHashSet<String>();
        LinkedHashSet<String> leafTableKeys = new LinkedHashSet<String>();
        if (fallback.getDependencies() != null) {
            for (GovernanceDbViewDependencyRef dependency : fallback.getDependencies()) {
                if (dependency == null) {
                    continue;
                }
                String dependencyKey = dependencyObjectKey(dependency);
                if (StringUtils.hasText(dependencyKey)) {
                    dependencyKeys.add(dependencyKey);
                }
                if ("TABLE".equalsIgnoreCase(trimToNull(dependency.getObjectType()))) {
                    StructureParseLogicalObjectHit tableHit = buildLogicalObjectHit(
                        LogicalObjectType.TABLE,
                        QualifiedObjectName.parse(firstNonBlank(dependency.getObjectName(), stripObjectKeyType(dependencyKey))),
                        MATCH_SOURCE_DB_VIEW_CATALOG_FALLBACK,
                        Boolean.TRUE
                    );
                    if (StringUtils.hasText(dependencyKey)) {
                        tableHit.setObjectKey(dependencyKey);
                    }
                    tableHit.setMappedPhysicalTargets(Collections.singletonList(tableHit.getObjectKey()));
                    context.getResult().addHit(tableHit);
                    leafTableKeys.add(tableHit.getObjectKey());
                }
            }
        }
        viewHit.setMappedPhysicalTargets(new ArrayList<String>(leafTableKeys.isEmpty() ? dependencyKeys : leafTableKeys));
        return new ArrayList<String>(leafTableKeys);
    }

    private DatasourceViewMetadataResponse resolveLiveViewMetadata(QualifiedObjectName qualifiedObject,
                                                                   ViewExpansionContext context) {
        DatasourceViewMetadataRequest request = new DatasourceViewMetadataRequest();
        request.setTenantId(context.getTenantId());
        request.setDatasourceCode(context.getDatasourceCode());
        request.setCatalogName(qualifiedObject.getCatalogName());
        request.setSchemaName(qualifiedObject.getSchemaName());
        request.setObjectName(qualifiedObject.getObjectName());
        try {
            return datasourceViewMetadataClient.resolveView(request);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "operation=STRUCTURE_PARSE_DB_VIEW_METADATA tenantId={} datasourceCode={} objectName={} status=DEGRADED reason={}",
                context.getTenantId(),
                context.getDatasourceCode(),
                qualifiedObject.getQualifiedName(),
                ex.getMessage()
            );
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_QUERY_FAILED");
        }
    }

    private GovernanceDbViewResolveResponse resolveGovernanceDbViewFallback(QualifiedObjectName qualifiedObject,
                                                                            ViewExpansionContext context) {
        if (!StringUtils.hasText(context.getDatasourceCode())) {
            return null;
        }
        try {
            GovernanceDbViewResolveRequest request = new GovernanceDbViewResolveRequest();
            request.setTenantId(context.getTenantId());
            request.setDatasourceCode(context.getDatasourceCode());
            request.setViewName(qualifiedObject.getObjectName());
            return governanceCapabilityClient.resolveDbView(request);
        } catch (Exception ex) {
            LOGGER.warn(
                "operation=STRUCTURE_PARSE_DB_VIEW_CATALOG_FALLBACK tenantId={} datasourceCode={} viewName={} status=DEGRADED reason={}",
                context.getTenantId(),
                context.getDatasourceCode(),
                qualifiedObject.getQualifiedName(),
                ex.getMessage()
            );
            return null;
        }
    }

    private StructureParseLogicalObjectHit buildLogicalObjectHit(LogicalObjectType objectType,
                                                                 QualifiedObjectName qualifiedObject,
                                                                 String matchSource,
                                                                 Boolean resolved) {
        StructureParseLogicalObjectHit hit = new StructureParseLogicalObjectHit();
        hit.setObjectType(objectType);
        hit.setObjectName(qualifiedObject.getQualifiedName());
        hit.setCatalogName(qualifiedObject.getCatalogName());
        hit.setSchemaName(qualifiedObject.getSchemaName());
        hit.setMatchSource(matchSource);
        hit.setResolved(resolved);
        fillLogicalObjectReferenceFields(hit);
        return hit;
    }

    private String dependencyObjectKey(GovernanceDbViewDependencyRef dependency) {
        String objectKey = trimToNull(dependency.getObjectKey());
        if (objectKey != null) {
            return objectKey;
        }
        String objectType = trimToNull(dependency.getObjectType());
        String objectName = trimToNull(dependency.getObjectName());
        if (!StringUtils.hasText(objectType) || !StringUtils.hasText(objectName)) {
            return objectName;
        }
        try {
            return LogicalObjectRef.buildObjectKey(LogicalObjectType.valueOf(objectType.toUpperCase(Locale.ROOT)), objectName);
        } catch (IllegalArgumentException ex) {
            return objectName;
        }
    }

    private String stripObjectKeyType(String objectKey) {
        String normalized = trimToNull(objectKey);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(':');
        return separator >= 0 && separator + 1 < normalized.length() ? normalized.substring(separator + 1) : normalized;
    }

    private String metadataFailureReason(DatasourceViewMetadataResponse metadata, String defaultReason) {
        if (metadata != null && StringUtils.hasText(metadata.getFailureReason())) {
            return metadata.getFailureReason();
        }
        return defaultReason;
    }

    private String extractViewDefinitionBody(String definitionSql) {
        String normalized = trimToNull(definitionSql);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("SELECT") || upper.startsWith("WITH")) {
            return normalized;
        }
        Matcher matcher = VIEW_DEFINITION_BODY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return normalized;
    }

    private void fillLogicalObjectReferenceFields(StructureParseLogicalObjectHit hit) {
        if (hit == null) {
            return;
        }
        hit.setObjectKey(LogicalObjectRef.buildObjectKey(hit.getObjectType(), hit.getObjectName()));
        String[] parts = splitQualifiedName(hit.getObjectName());
        if (parts.length == 3) {
            hit.setCatalogName(parts[0]);
            hit.setSchemaName(parts[1]);
        } else if (parts.length == 2) {
            hit.setSchemaName(parts[0]);
        }
    }

    private String[] splitQualifiedName(String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return new String[0];
        }
        String[] parts = objectName.split("\\.");
        if (parts.length >= 3) {
            return new String[] {parts[0], parts[1], parts[2]};
        }
        if (parts.length == 2) {
            return new String[] {parts[0], parts[1]};
        }
        return new String[] {parts[0]};
    }

    private boolean isDbViewHeuristic(String objectName) {
        String normalized = objectName == null ? "" : objectName.toLowerCase(Locale.ROOT);
        return normalized.startsWith("vw_")
            || normalized.contains(".vw_")
            || normalized.endsWith("_view")
            || normalized.contains(".view_");
    }

    private StructureParseComplexityLevel resolveComplexity(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private void enrichQueryIntent(StructureParseResponseVO response,
                                   String sqlText,
                                   SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        response.setSqlFingerprint(SqlFingerprintUtils.fingerprint(sqlText));
        if (profile == null) {
            HeuristicFallbackProfile heuristicProfile = analyzeHeuristicFallback(
                sqlText,
                containsValue(response == null ? null : response.getRiskTags(), RISK_SQL_TOO_LONG)
            );
            enrichHeuristicQueryIntent(response, heuristicProfile);
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
        response.setRiskChecklist(risks);
        response.setEstimatedResourceCost(buildResourceEstimate(profile, risks, finalTableCount));
    }

    private void enrichHeuristicQueryIntent(StructureParseResponseVO response,
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
        response.setEstimatedResourceCost(heuristicResourceEstimate(profile));
    }

    private String resolveHeuristicScanMode(HeuristicFallbackProfile profile) {
        if (profile.getPredicateCount() == 0) {
            return "FULL_TABLE_SCAN";
        }
        if (!profile.getDatePredicateColumns().isEmpty() || !profile.getDates().isEmpty()) {
            return profile.getPredicateCount() == 1 ? "RANGE_SCAN" : "PARTITION_RANGE_SCAN";
        }
        return "CROSS_PARTITION_SCAN";
    }

    private String resolveHeuristicJoinType(HeuristicFallbackProfile profile) {
        if (profile.getJoinCount() == 0) {
            return "NONE";
        }
        return profile.getJoinCount() == 1 ? "CHAIN" : "MANY_TO_MANY";
    }

    private List<String> buildHeuristicFeatureEvidence(HeuristicFallbackProfile profile, int finalTableCount) {
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

    private StructureParseResourceEstimateVO heuristicResourceEstimate(HeuristicFallbackProfile profile) {
        StructureParseResourceEstimateVO estimate = unknownResourceEstimate();
        List<String> evidence = new ArrayList<String>();
        evidence.add("Resource estimate is bounded because the AST parser did not produce a valid profile.");
        evidence.add("parser=" + HEURISTIC_FALLBACK_ENGINE);
        evidence.add("staticOnly=true");
        evidence.add("tables=" + profile.getTables().size());
        evidence.add("predicates=" + profile.getPredicateCount());
        estimate.setEvidence(evidence);
        return estimate;
    }

    private StructureParseIntentProfileVO unknownIntentProfile() {
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

    private StructureParseFeatureSummaryVO unknownFeatureSummary() {
        StructureParseFeatureSummaryVO summary = new StructureParseFeatureSummaryVO();
        summary.setParserEngine("UNKNOWN");
        summary.setScanMode("UNKNOWN");
        summary.setJoinType("UNKNOWN");
        summary.setComputeDensity("UNKNOWN");
        summary.setResourceType("UNKNOWN");
        summary.setSlaLevel("UNKNOWN");
        summary.setEvidence(Collections.singletonList("Parser could not produce a supported AST profile."));
        return summary;
    }

    private StructureParseResourceEstimateVO unknownResourceEstimate() {
        StructureParseResourceEstimateVO estimate = new StructureParseResourceEstimateVO();
        estimate.setOverall("UNKNOWN");
        estimate.setCpu("UNKNOWN");
        estimate.setIo("UNKNOWN");
        estimate.setMemory("UNKNOWN");
        estimate.setNetwork("UNKNOWN");
        estimate.setResultSize("UNKNOWN");
        estimate.setEvidence(Collections.singletonList("Resource estimate is unavailable for invalid SQL."));
        return estimate;
    }

    private String resolveScanMode(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        if (profile.getPredicateCount() == 0) {
            return "FULL_TABLE_SCAN";
        }
        if (profile.getDatePredicateColumns().isEmpty()) {
            return profile.isLimitPresent() && profile.getPredicateCount() == 1 ? "POINT_LOOKUP" : "CROSS_PARTITION_SCAN";
        }
        return profile.getPredicateCount() == 1 ? "RANGE_SCAN" : "PARTITION_RANGE_SCAN";
    }

    private String resolveJoinType(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private String resolveComputeDensity(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private AggregateShape aggregateShape(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private int cappedShapeWeight(int count, int smallUpperBound, int mediumUpperBound) {
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

    private String resolveResourceType(SqlOptimizationPipelineService.ParsedSqlProfile profile,
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

    private String resolveSlaLevel(SqlOptimizationPipelineService.ParsedSqlProfile profile,
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

    private List<String> buildClassificationLabels(String scanMode,
                                                   String joinType,
                                                   String computeDensity,
                                                   String resourceType,
                                                   String slaLevel) {
        return Arrays.asList(scanMode, joinType, computeDensity, resourceType, slaLevel);
    }

    private List<String> buildFeatureEvidence(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
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

    private List<String> buildFeatureEvidence(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                              int finalTableCount) {
        List<String> evidence = buildFeatureEvidence(profile);
        if (finalTableCount != profile.getTables().size()) {
            evidence.add("expandedTables=" + finalTableCount);
        }
        return evidence;
    }

    private int resolveFinalTableCount(StructureParseResponseVO response,
                                       SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<String> tableKeys = extractFinalTableKeys(response == null ? null : response.getLogicalObjectHits());
        return tableKeys.isEmpty() ? profile.getTables().size() : tableKeys.size();
    }

    private List<String> extractFinalTableKeys(List<LogicalObjectSurface> logicalObjectHits) {
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

    private void addTableKey(Set<String> keys, String candidate) {
        String normalized = trimToNull(candidate);
        if (normalized != null && normalized.toUpperCase(Locale.ROOT).startsWith("TABLE:")) {
            keys.add(normalized);
        }
    }

    private boolean containsValue(List<String> values, String expected) {
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

    private List<StructureParseRiskVO> buildRiskChecklist(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<StructureParseRiskVO> risks = new ArrayList<StructureParseRiskVO>();
        Set<String> emitted = new LinkedHashSet<String>();
        for (String warning : profile.getWarnings()) {
            if ("NO_PREDICATE".equals(warning)) {
                addRisk(risks, emitted, risk("FULL_TABLE_SCAN_RISK", "HIGH", "Full table scan risk",
                    "predicateCount=0", "Add tenant, time, partition, or business-key predicates."));
            } else if ("LARGE_JOIN_PAIR_RISK".equals(warning) || "HEAVY_JOIN_GRAPH".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_TABLE_JOIN_RISK", "HIGH", "Static join evidence risk",
                    "staticOnly=true, joinCount=" + profile.getJoinCount()
                        + ", joinCriteriaCount=" + profile.getJoinCriteriaCount(),
                    "Confirm join keys, filter placement, and selectivity with Access Parse or benchmark evidence."));
            } else if ("ORDER_BY_WITHOUT_LIMIT".equals(warning)) {
                addRisk(risks, emitted, risk("UNNECESSARY_SORT_RISK", "MEDIUM", "Potentially unnecessary sort",
                    "orderByCount=" + profile.getOrderByCount() + ", limitPresent=false", "Add LIMIT or move ordering to a serving object."));
            } else if ("REPEATED_EXPRESSION_COMPUTE".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_EXPRESSION_RISK", "MEDIUM", "Repeated expression computation",
                    "repeatedExpressionCount=" + profile.getRepeatedExpressionCount(), "Deduplicate or materialize shared expressions."));
            } else if ("LARGE_RESULT_SET_RISK".equals(warning) || "SELECT_STAR".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_RESULT_SET_RISK", "HIGH", "Oversized result set risk",
                    "selectStar=" + profile.isSelectStar() + ", limitPresent=" + profile.isLimitPresent(),
                    "Use explicit columns, filters, or LIMIT for interactive paths."));
            } else if ("SCALAR_SUBQUERY_IN_SELECT".equals(warning)) {
                addRisk(risks, emitted, risk("SCALAR_SUBQUERY_IN_SELECT", "HIGH", "Scalar subquery in SELECT",
                    "scalarSubqueryCount=" + profile.getScalarSubqueryCount(), "Rewrite scalar subqueries into joins or staged aggregates."));
            } else if ("NESTED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("NESTED_SUBQUERY_RISK", "HIGH", "Nested subquery risk",
                    "subqueryCount=" + profile.getSubqueryCount() + ", depth=" + profile.getNestedSubqueryDepth(),
                    "Flatten nested subqueries into named CTE stages."));
            } else if ("CORRELATED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("CORRELATED_SUBQUERY_RISK", "HIGH", "Correlated subquery risk",
                    "correlatedSubqueryCount=" + profile.getCorrelatedSubqueryCount(), "Rewrite correlated subqueries into joins or precomputed stages."));
            } else if ("FUNCTION_WRAPPED_PREDICATE".equals(warning)) {
                addRisk(risks, emitted, risk("FUNCTION_WRAPPED_PREDICATE", "MEDIUM", "Function-wrapped predicate",
                    "functionWrappedPredicateCount=" + profile.getFunctionWrappedPredicateCount(), "Rewrite as range or normalized column comparison."));
            } else if ("NOT_EXISTS_ANTI_JOIN_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("NOT_EXISTS_ANTI_JOIN_RISK", "MEDIUM", "NOT EXISTS anti-join risk",
                    "notExistsCount=" + profile.getNotExistsCount(), "Review anti-join semantics and staged alternatives."));
            } else if ("LEADING_WILDCARD_LIKE_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("LEADING_WILDCARD_LIKE_RISK", "MEDIUM", "Leading wildcard LIKE risk",
                    "leadingWildcardLikeCount=" + profile.getLeadingWildcardLikeCount(), "Use searchable keys or prefixable predicates."));
            } else if ("OR_PREDICATE_INDEX_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("OR_PREDICATE_INDEX_RISK", "MEDIUM", "OR predicate pruning risk",
                    "orPredicateCount=" + profile.getOrPredicateCount(), "Consider UNION ALL branches or staged filters."));
            } else if ("ORDER_BY_RANDOM_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("ORDER_BY_RANDOM_RISK", "HIGH", "Random order risk",
                    "randomOrderCount=" + profile.getRandomOrderCount(), "Use deterministic sampling instead of ORDER BY RAND/RANDOM."));
            } else if ("REPEATED_TABLE_SCAN_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_TABLE_SCAN_RISK", "HIGH", "Repeated table scan risk",
                    "repeatedTableScanCount=" + profile.getRepeatedTableScanCount(), "Pre-stage repeated inputs and reuse them explicitly."));
            } else if ("ORDER_BY_COMPLEXITY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("ORDER_BY_COMPLEXITY_RISK", "MEDIUM", "ORDER BY complexity risk",
                    "staticOnly=true, orderByExpressionCount=" + profile.getOrderByExpressionCount()
                        + ", duplicateOrderByKeyCount=" + profile.getDuplicateOrderByKeyCount(),
                    "Reduce sort keys, remove duplicate ordering, or validate sorted output with plan evidence."));
            } else if ("JOIN_LATENCY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("JOIN_LATENCY_RISK", "HIGH", "Join latency risk",
                    "staticOnly=true, joinCount=" + profile.getJoinCount()
                        + ", joinCriteriaCount=" + profile.getJoinCriteriaCount()
                        + ", subqueryCount=" + profile.getSubqueryCount(),
                    "Confirm join keys, filter placement, scanned volume, and row movement with access parse or benchmark."));
            } else if ("AGGREGATION_COMPLEXITY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("AGGREGATION_COMPLEXITY_RISK", "MEDIUM", "Aggregation complexity risk",
                    "staticOnly=true, aggregateFunctionCount=" + profile.getAggregateFunctionCount()
                        + ", groupByCount=" + profile.getGroupByCount()
                        + ", largeStringAggregateCount=" + profile.getLargeStringAggregateCount(),
                    "Pre-aggregate reusable stages or move heavy aggregate output into serving objects."));
            } else if ("GROUP_BY_WITHOUT_AGGREGATE_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("GROUP_BY_WITHOUT_AGGREGATE_RISK", "MEDIUM", "GROUP BY without aggregate",
                    "staticOnly=true, groupByCount=" + profile.getGroupByCount() + ", aggregateFunctionCount=0",
                    "Use DISTINCT for de-duplication intent or remove redundant grouping."));
            } else if ("DUPLICATE_GROUP_OR_ORDER_KEY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("DUPLICATE_GROUP_OR_ORDER_KEY_RISK", "MEDIUM", "Duplicate group/order key risk",
                    "staticOnly=true, duplicateGroupByKeyCount=" + profile.getDuplicateGroupByKeyCount()
                        + ", duplicateOrderByKeyCount=" + profile.getDuplicateOrderByKeyCount(),
                    "Remove duplicate grouping or ordering keys before rewrite review."));
            } else if ("REPEATED_SUBQUERY_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("REPEATED_SUBQUERY_RISK", "HIGH", "Repeated subquery risk",
                    "staticOnly=true, repeatedSubqueryCount=" + profile.getRepeatedSubqueryCount(),
                    "Extract repeated subqueries into a named CTE or reviewed serving object."));
            } else if ("LARGE_STRING_RESULT_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_STRING_RESULT_RISK", "HIGH", "Large string result risk",
                    "staticOnly=true, stringProjectionCount=" + profile.getStringProjectionCount()
                        + ", stringConcatenationCount=" + profile.getStringConcatenationCount()
                        + ", largeStringAggregateCount=" + profile.getLargeStringAggregateCount(),
                    "Limit string projections or validate returned bytes through access parse or benchmark."));
            } else if ("COMPLEX_QUERY_GRAPH_RISK".equals(warning)) {
                addRisk(risks, emitted, risk("COMPLEX_QUERY_GRAPH_RISK", "HIGH", "Complex query graph risk",
                    "subqueryCount=" + profile.getSubqueryCount() + ", predicateCount=" + profile.getPredicateCount(),
                    "Break the query into reviewed stages before online use."));
            }
        }
        return risks;
    }

    private void addRisk(List<StructureParseRiskVO> risks, Set<String> emitted, StructureParseRiskVO risk) {
        if (risk != null && emitted.add(risk.getRiskCode())) {
            risks.add(risk);
        }
    }

    private StructureParseRiskVO risk(String code,
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

    private StructureParseResourceEstimateVO buildResourceEstimate(SqlOptimizationPipelineService.ParsedSqlProfile profile,
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

    private String level(int score) {
        if (score >= 3) {
            return "HIGH";
        }
        if (score > 0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String resolveOverallEstimate(StructureParseResourceEstimateVO estimate, List<StructureParseRiskVO> risks) {
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

    private StructureParseResponseVO toResponse(StructureParseResult result) {
        StructureParseResponseVO response = new StructureParseResponseVO();
        response.setParseTaskId(result.getParseTaskId());
        response.setSyntaxStatus(result.getSyntaxStatus().name());
        response.setComplexityLevel(result.getComplexityLevel().name());
        response.setSqlType(result.getSqlType());
        response.setQueryDateSummary(toQueryDateSummaryVO(result.getQueryDateSummary()));
        response.setLogicalObjectHits(toLogicalObjectHitVOs(result.getLogicalObjectHits()));
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

    private void writeParseHistory(StructureParseResponseVO response,
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
                "operation=STRUCTURE_PARSE_HISTORY_WRITE entity={} tenantId={} status=DEGRADED reason={}",
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
                "operation=STRUCTURE_ACCESS_PARSE_HISTORY_WRITE entity={} tenantId={} status=DEGRADED reason={}",
                structureParse.getParseTaskId(),
                RequestContext.getTenantId(),
                ex.getMessage()
            );
            structureParse.setHistoryPersisted(Boolean.FALSE);
            structureParse.setHistoryPersistenceStatus("WRITE_FAILED");
        }
    }

    private String resolveHistoryResultStatus(StructureParseResponseVO response) {
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

    private void applyHistoryWriteResult(StructureParseResponseVO response, SqlParseHistoryWriteResult writeResult) {
        if (writeResult == null) {
            response.setHistoryPersisted(Boolean.FALSE);
            response.setHistoryPersistenceStatus("NO_RESPONSE");
            return;
        }
        response.setHistoryId(writeResult.getParseHistoryId());
        response.setHistoryPersisted(writeResult.getPersisted());
        response.setHistoryPersistenceStatus(writeResult.getPersistenceStatus());
    }

    private void triggerRewriteRecommendation(StructureParseResponseVO structureParse,
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

    private StructureParseQueryDateSummaryVO toQueryDateSummaryVO(StructureParseQueryDateSummary summary) {
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

    private List<LogicalObjectSurface> toLogicalObjectHitVOs(List<StructureParseLogicalObjectHit> hits) {
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

    private List<StructureParseIssueVO> toIssueVOs(List<StructureParseIssue> issues) {
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

    private static final class HeuristicFallbackProfile {

        private String sqlType = "UNKNOWN";
        private List<String> tables = Collections.emptyList();
        private List<String> datePredicateColumns = Collections.emptyList();
        private List<LocalDate> dates = Collections.emptyList();
        private int joinCount;
        private int predicateCount;
        private int orPredicateCount;
        private boolean sqlTooLong;
        private int sqlLength;
        private int boundedSqlLength;

        private String getSqlType() {
            return sqlType;
        }

        private void setSqlType(String sqlType) {
            this.sqlType = StringUtils.hasText(sqlType) ? sqlType : "UNKNOWN";
        }

        private List<String> getTables() {
            return tables;
        }

        private void setTables(List<String> tables) {
            this.tables = tables == null ? Collections.<String>emptyList() : tables;
        }

        private List<String> getDatePredicateColumns() {
            return datePredicateColumns;
        }

        private void setDatePredicateColumns(List<String> datePredicateColumns) {
            this.datePredicateColumns = datePredicateColumns == null
                ? Collections.<String>emptyList()
                : datePredicateColumns;
        }

        private List<LocalDate> getDates() {
            return dates;
        }

        private void setDates(List<LocalDate> dates) {
            this.dates = dates == null ? Collections.<LocalDate>emptyList() : dates;
        }

        private int getJoinCount() {
            return joinCount;
        }

        private void setJoinCount(int joinCount) {
            this.joinCount = Math.max(0, joinCount);
        }

        private int getPredicateCount() {
            return predicateCount;
        }

        private void setPredicateCount(int predicateCount) {
            this.predicateCount = Math.max(0, predicateCount);
        }

        private int getOrPredicateCount() {
            return orPredicateCount;
        }

        private void setOrPredicateCount(int orPredicateCount) {
            this.orPredicateCount = Math.max(0, orPredicateCount);
        }

        private boolean isSqlTooLong() {
            return sqlTooLong;
        }

        private void setSqlTooLong(boolean sqlTooLong) {
            this.sqlTooLong = sqlTooLong;
        }

        private int getSqlLength() {
            return sqlLength;
        }

        private void setSqlLength(int sqlLength) {
            this.sqlLength = Math.max(0, sqlLength);
        }

        private int getBoundedSqlLength() {
            return boundedSqlLength;
        }

        private void setBoundedSqlLength(int boundedSqlLength) {
            this.boundedSqlLength = Math.max(0, boundedSqlLength);
        }
    }

    private static final class LogicalObjectExpansionResult {

        private final Map<String, StructureParseLogicalObjectHit> hitsByKey =
            new LinkedHashMap<String, StructureParseLogicalObjectHit>();
        private final List<String> unresolvedReasons = new ArrayList<String>();

        private void addHit(StructureParseLogicalObjectHit hit) {
            if (hit == null || !StringUtils.hasText(hit.getObjectKey())) {
                return;
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

        private List<StructureParseLogicalObjectHit> getHits() {
            return new ArrayList<StructureParseLogicalObjectHit>(hitsByKey.values());
        }

        private void addUnresolvedReason(String reason) {
            if (StringUtils.hasText(reason)) {
                unresolvedReasons.add(reason);
            }
        }

        private boolean hasUnresolvedViewDefinition() {
            return !unresolvedReasons.isEmpty();
        }

        private List<String> getUnresolvedReasons() {
            return unresolvedReasons;
        }

        private static List<String> mergeTargets(List<String> first, List<String> second) {
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

    private static final class ViewExpansionContext {

        private final String tenantId;
        private final String datasourceCode;
        private final SqlParserMode parserMode;
        private final LogicalObjectExpansionResult result;
        private final Set<String> visited = new LinkedHashSet<String>();

        private ViewExpansionContext(String tenantId,
                                     String datasourceCode,
                                     SqlParserMode parserMode,
                                     LogicalObjectExpansionResult result) {
            this.tenantId = tenantId;
            this.datasourceCode = datasourceCode;
            this.parserMode = parserMode;
            this.result = result;
        }

        private String getTenantId() {
            return tenantId;
        }

        private String getDatasourceCode() {
            return datasourceCode;
        }

        private SqlParserMode getParserMode() {
            return parserMode;
        }

        private LogicalObjectExpansionResult getResult() {
            return result;
        }

        private String visitKey(String objectKey) {
            return (datasourceCode == null ? "" : datasourceCode.toLowerCase(Locale.ROOT))
                + "|" + (objectKey == null ? "" : objectKey.toLowerCase(Locale.ROOT));
        }

        private boolean isVisited(String visitKey) {
            return visited.contains(visitKey);
        }

        private void pushVisited(String visitKey) {
            visited.add(visitKey);
        }

        private void popVisited(String visitKey) {
            visited.remove(visitKey);
        }
    }

    private static final class QualifiedObjectName {

        private final String catalogName;
        private final String schemaName;
        private final String objectName;
        private final String qualifiedName;

        private QualifiedObjectName(String catalogName, String schemaName, String objectName) {
            this.catalogName = catalogName;
            this.schemaName = schemaName;
            this.objectName = objectName;
            this.qualifiedName = buildQualifiedName(catalogName, schemaName, objectName);
        }

        private static QualifiedObjectName parse(String value) {
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

        private static String clean(String value) {
            String normalized = value == null ? "" : value.trim();
            if ((normalized.startsWith("\"") && normalized.endsWith("\""))
                || (normalized.startsWith("`") && normalized.endsWith("`"))
                || (normalized.startsWith("[") && normalized.endsWith("]"))) {
                return normalized.substring(1, normalized.length() - 1);
            }
            return normalized;
        }

        private static String buildQualifiedName(String catalogName, String schemaName, String objectName) {
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

        private String getCatalogName() {
            return catalogName;
        }

        private String getSchemaName() {
            return schemaName;
        }

        private String getObjectName() {
            return objectName;
        }

        private String getQualifiedName() {
            return qualifiedName;
        }
    }

    private static final class AggregateShape {

        private final int complexityWeight;
        private final boolean heavy;
        private final boolean moderate;

        private AggregateShape(int complexityWeight, boolean heavy, boolean moderate) {
            this.complexityWeight = complexityWeight;
            this.heavy = heavy;
            this.moderate = moderate;
        }

        private int getComplexityWeight() {
            return complexityWeight;
        }

        private boolean isHeavy() {
            return heavy;
        }

        private boolean isModerate() {
            return moderate;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
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
