package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceDbViewDependencyRef;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveRequest;
import com.company.sqlforge.common.governance.GovernanceDbViewResolveResponse;
import com.company.sqlforge.common.logicalobject.LogicalObjectRef;
import com.company.sqlforge.common.logicalobject.LogicalObjectType;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.PlanAnalysisVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StructureParseApplicationService extends StructureParseApplicationServiceSupport {

    @Autowired
    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient,
                                            SqlParseHistoryApplicationService sqlParseHistoryApplicationService,
                                            DatasourceViewMetadataClient datasourceViewMetadataClient,
                                            HetuPlanAnalysisClient hetuPlanAnalysisClient,
                                            ParseTriggeredRewriteRecommendationService parseTriggeredRewriteRecommendationService) {
        super(
            sqlOptimizationPipelineService,
            governanceCapabilityClient,
            sqlParseHistoryApplicationService,
            datasourceViewMetadataClient,
            hetuPlanAnalysisClient,
            parseTriggeredRewriteRecommendationService
        );
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
            "操作日志 operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} status=START",
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
                "操作日志 operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} degradedReason={}",
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
                    request.getDatasourceType(),
                    structureParserMode
                );
                result.setLogicalObjectHits(logicalObjectExpansion.getHits());
                result.setSurfaceObjectRefs(logicalObjectExpansion.getSurfaceRefs());
                result.setExpandedPhysicalObjectRefs(logicalObjectExpansion.getExpandedPhysicalRefs());
                result.setRiskTags(buildRiskTags(profile, logicalObjectExpansion));
                result.setRewriteCandidates(sqlOptimizationPipelineService.deriveRewriteCandidateRules(profile));
                result.setIssues(buildIssues(profile, logicalObjectExpansion));
                result.applyAssessment(StructureParsePriorityScorer.assessAll(result.getIssues()));
                LOGGER.info(
                    "操作日志 operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} priorityLevel={}",
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
                    "操作日志 operation=STRUCTURE_PARSE entity={} tenantId={} datasourceCode={} costMs={} status=END syntaxStatus={} degradedReason={}",
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
        String parserFailureToken = diagnosticFailureToken(position);
        String diagnosticFailureToken = sqlTooLong
            ? RISK_SQL_TOO_LONG
            : (StringUtils.hasText(parserFailureToken) ? parserFailureToken : RISK_SQL_SYNTAX_INVALID);
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
            ex == null ? "运行完整结构解析前，请修复 SQL 语法或缩短语句。" : ex.getSuggestedAction(),
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
        if (StringUtils.hasText(failureToken)) {
            result.setFailureToken(failureToken);
        }
        if (StringUtils.hasText(failureSnippet)) {
            result.setFailureSnippet(failureSnippet);
        }
        if (position != null) {
            result.setFailureLine(position.getLine());
            result.setFailureColumn(position.getColumn());
            result.setFailureOffset(position.getOffset());
        } else if (sqlTooLong) {
            result.setFailureLine(Integer.valueOf(1));
            result.setFailureColumn(Integer.valueOf(1));
            result.setFailureOffset(Integer.valueOf(0));
        }
        result.setQueryDateSummary(buildHeuristicQueryDateSummary(heuristicProfile));
        List<StructureParseLogicalObjectHit> heuristicHits = buildHeuristicLogicalObjectHits(heuristicProfile);
        result.setLogicalObjectHits(heuristicHits);
        result.setSurfaceObjectRefs(heuristicHits);
        result.setExpandedPhysicalObjectRefs(filterPhysicalObjectRefs(heuristicHits));
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
        issue.setSuggestedAction("请减少 SQL 大小、将语句拆分为更小的已评审阶段，或显式提高解析器限制。");
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

    @Override
    protected HeuristicFallbackProfile analyzeHeuristicFallback(String sqlText, boolean sqlTooLong) {
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
            HetuPlanAnalysisResult result = hetuPlanAnalysisClient.explain(
                request.getSqlText(),
                RequestContext.getTenantId(),
                datasourceCode,
                request.getDatasourceType()
            );
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
            ? "SQL 语法解析器拒绝了该语句。"
            : normalizeDiagnosticText(ex.getMessage()));
        if (position != null && position.getLine() != null && position.getColumn() != null) {
            builder.append(" 失败位置：第 ")
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
            return "SQL 语法解析器拒绝了该语句。";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            builder.append(isDiagnosticNoiseCharacter(current) ? ' ' : current);
        }
        String diagnosticText = compactDiagnosticText(builder.toString(), FAILURE_DETAIL_TEXT_LIMIT);
        return diagnosticText == null ? "SQL 语法解析器拒绝了该语句。" : diagnosticText;
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
        issue.setSummary("无法从实时元数据完整展开数据库视图定义。");
        issue.setDetail(String.join("; ", logicalObjectExpansion.getUnresolvedReasons()));
        issue.setSuggestedAction("请校验数据源元数据权限，或刷新受治理 DB 视图目录兜底数据。");
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
            issue.setSummary("该语句使用 SELECT *，会隐藏实际列范围。");
            issue.setDetail("宽投影会削弱改写确定性，并增加不必要的数据移动。");
            issue.setSuggestedAction("优化评审前请用显式列替换星号投影。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("NO_PREDICATE".equals(warning)) {
            issue.setIssueCode("FULL_SCAN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句没有过滤谓词，可能扫描全量数据集。");
            issue.setDetail("读路径上的全表扫描通常表示缺少分区或业务键过滤条件。");
            issue.setSuggestedAction("在线执行前请补充租户、时间或业务键谓词。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("ORDER_BY_WITHOUT_LIMIT".equals(warning)) {
            issue.setIssueCode("ORDER_BY_WITHOUT_LIMIT");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句排序时未限制结果范围。");
            issue.setDetail("无界排序可能产生不必要的 shuffle 或内存压力。");
            issue.setSuggestedAction("请添加 LIMIT，或将排序工作转移到服务对象中。");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("HEAVY_JOIN_GRAPH".equals(warning)) {
            issue.setIssueCode("HEAVY_JOIN_GRAPH");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句连接多个数据集，可能需要分阶段执行。");
            issue.setDetail("大型 join 图需要评审过滤位置、join key 与服务化替代方案。");
            issue.setSuggestedAction("请评审 join 图、键选择性和加速机会。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LARGE_JOIN_PAIR_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_JOIN_PAIR_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句关于 join 条件或选择性的静态证据不足。");
            issue.setDetail("静态结构解析无法证明表规模或运行时成本，只能标记 join 证据不完整。");
            issue.setSuggestedAction("在线使用前，请通过访问解析或压测证据确认 join key、过滤位置与选择性。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("REPEATED_EXPRESSION_COMPUTE".equals(warning)) {
            issue.setIssueCode("REPEATED_EXPRESSION_COMPUTE");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句重复了可能被多次计算的表达式片段。");
            issue.setDetail("重复投影、过滤、分组或排序表达式可能增加 CPU 成本。");
            issue.setSuggestedAction("请去重表达式，或把共享计算移入 CTE 或服务对象。");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LARGE_RESULT_SET_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_RESULT_SET_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("WIDE_PROJECTION");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句可能返回超大结果集。");
            issue.setDetail("宽投影或缺少 LIMIT/过滤证据，可能为交互场景产生过多数据。");
            issue.setSuggestedAction("在交互路径使用该查询前，请补充显式投影、过滤条件或 LIMIT。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("SCALAR_SUBQUERY_IN_SELECT".equals(warning)) {
            issue.setIssueCode("SCALAR_SUBQUERY_IN_SELECT");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("SELECT 列表包含标量子查询。");
            issue.setDetail("投影中的标量子查询可能对每个输出行重复执行查询或聚合逻辑。");
            issue.setSuggestedAction("请将标量子查询改写为 join、预聚合 CTE 或服务对象。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("NESTED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("NESTED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句包含多层嵌套子查询。");
            issue.setDetail("深层子查询嵌套会使过滤位置、join 顺序和运行时成本更难静态推断。");
            issue.setSuggestedAction("请将查询展开为具名 CTE 阶段，并独立评审每个阶段。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("CORRELATED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("CORRELATED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句包含关联子查询引用。");
            issue.setDetail("关联子查询可能在计划阶段产生重复查询或去相关压力。");
            issue.setSuggestedAction("请将关联子查询改写为显式 join 或预聚合 CTE。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("FUNCTION_WRAPPED_PREDICATE".equals(warning)) {
            issue.setIssueCode("FUNCTION_WRAPPED_PREDICATE");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("过滤谓词将列包裹在函数中。");
            issue.setDetail("函数包裹谓词可能阻断分区裁剪或索引式下推。");
            issue.setSuggestedAction("可行时请将谓词改写为范围条件或标准化列比较。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("NOT_EXISTS_ANTI_JOIN_RISK".equals(warning)) {
            issue.setIssueCode("NOT_EXISTS_ANTI_JOIN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句使用 NOT EXISTS anti-join 逻辑。");
            issue.setDetail("anti-join 逻辑需要评审 null 语义、选择性和 join 位置。");
            issue.setSuggestedAction("仅在完成语义校验后，才考虑分阶段 LEFT JOIN ... IS NULL 改写。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("LEADING_WILDCARD_LIKE_RISK".equals(warning)) {
            issue.setIssueCode("LEADING_WILDCARD_LIKE_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("LIKE 谓词以通配符开头。");
            issue.setDetail("前置通配符过滤通常无法使用前缀裁剪，可能导致更大范围扫描。");
            issue.setSuggestedAction("可行时请使用标准化搜索键、倒排索引或可前缀匹配的谓词。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("OR_PREDICATE_INDEX_RISK".equals(warning)) {
            issue.setIssueCode("OR_PREDICATE_INDEX_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MISSING_FILTER");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("WHERE 子句使用 OR 组合多个备选条件。");
            issue.setDetail("OR 谓词会削弱静态裁剪，可能需要基于 UNION 的分阶段处理以获得可预测访问路径。");
            issue.setSuggestedAction("请评审 UNION ALL 分支或分阶段过滤是否能让查询更易优化。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("ORDER_BY_RANDOM_RISK".equals(warning)) {
            issue.setIssueCode("ORDER_BY_RANDOM_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句按随机函数排序。");
            issue.setDetail("ORDER BY RAND/RANDOM 会在限制结果前强制执行昂贵的随机化和排序类工作。");
            issue.setSuggestedAction("请使用抽样源数据或确定性抽样键替代随机排序。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("REPEATED_TABLE_SCAN_RISK".equals(warning)) {
            issue.setIssueCode("REPEATED_TABLE_SCAN_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句多次引用同一张表。");
            issue.setDetail("如果子查询未分阶段处理，重复表扫描会放大 IO、CPU 和 shuffle 成本。");
            issue.setSuggestedAction("请使用 CTE 或服务对象预先分阶段处理重复输入，并显式复用。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("ORDER_BY_COMPLEXITY_RISK".equals(warning)) {
            issue.setIssueCode("ORDER_BY_COMPLEXITY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("UNBOUNDED_SORT");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句包含多个或冗余的 ORDER BY 键。");
            issue.setDetail("静态分析发现排序键过宽、重复排序或排序叠加聚合/分组压力。");
            issue.setSuggestedAction("请减少排序键、移除重复排序，或用计划证据校验已排序服务输出。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("JOIN_LATENCY_RISK".equals(warning)) {
            issue.setIssueCode("JOIN_LATENCY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("join 图存在长时间执行的静态信号。");
            issue.setDetail("多 join、静态 join 条件证据不足，或 join 叠加子查询结构可能增加延迟。");
            issue.setSuggestedAction("请通过访问解析或压测证据确认 join key、过滤位置、扫描量和行移动情况。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("AGGREGATION_COMPLEXITY_RISK".equals(warning)) {
            issue.setIssueCode("AGGREGATION_COMPLEXITY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句包含较多聚合工作，需要评审。");
            issue.setDetail("聚合数量、分组宽度、排序或字符串聚合可能增加 CPU 与内存压力。");
            issue.setSuggestedAction("请预聚合可复用阶段，或将重型聚合输出移入已评审的服务对象。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("GROUP_BY_WITHOUT_AGGREGATE_RISK".equals(warning)) {
            issue.setIssueCode("GROUP_BY_WITHOUT_AGGREGATE_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句使用 GROUP BY 但没有聚合函数。");
            issue.setDetail("不带聚合的 GROUP BY 通常表示 DISTINCT 式去重或冗余分组。");
            issue.setSuggestedAction("若意图是去重请使用 DISTINCT；若分组冗余请移除。");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("DUPLICATE_GROUP_OR_ORDER_KEY_RISK".equals(warning)) {
            issue.setIssueCode("DUPLICATE_GROUP_OR_ORDER_KEY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.MEDIUM);
            issue.setSummary("该语句重复了 GROUP BY 或 ORDER BY 键。");
            issue.setDetail("重复分组或排序键会增加不必要的逻辑计划工作，并模糊查询意图。");
            issue.setSuggestedAction("改写或加速评审前请移除重复分组或排序键。");
            issue.setImportant(Boolean.FALSE);
            issue.setUrgent(Boolean.FALSE);
        } else if ("REPEATED_SUBQUERY_RISK".equals(warning)) {
            issue.setIssueCode("REPEATED_SUBQUERY_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句重复了标准化后的子查询结构。");
            issue.setDetail("重复嵌套或标量子查询可能重复规划或执行相同查询/聚合工作。");
            issue.setSuggestedAction("请将重复子查询抽取为具名 CTE 或已评审的服务对象。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("LARGE_STRING_RESULT_RISK".equals(warning)) {
            issue.setIssueCode("LARGE_STRING_RESULT_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.PERFORMANCE);
            issue.setIssueScene("WIDE_PROJECTION");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该语句可能产生大量字符串型结果。");
            issue.setDetail("即使行数未知，字符串投影、拼接或聚合也可能放大返回字节数。");
            issue.setSuggestedAction("请限制字符串投影、避免无界字符串聚合，或通过访问解析/压测校验返回字节数。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else if ("COMPLEX_QUERY_GRAPH_RISK".equals(warning)) {
            issue.setIssueCode("COMPLEX_QUERY_GRAPH_RISK");
            issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
            issue.setIssueScene("MULTI_JOIN_COMPLEXITY");
            issue.setSeverity(StructureParseIssueSeverity.HIGH);
            issue.setSummary("该查询图对轻量交互路径过于复杂。");
            issue.setDetail("该静态结构把 join、谓词和嵌套子查询组合成高风险查询图。");
            issue.setSuggestedAction("请将 SQL 拆成已评审阶段，并在上线前运行访问解析或压测。");
            issue.setImportant(Boolean.TRUE);
            issue.setUrgent(Boolean.TRUE);
        } else {
            issue.setIssueCode(warning);
            issue.setIssueDomain(StructureParseIssueDomain.CONVENTION);
            issue.setIssueScene("GENERAL_WARNING");
            issue.setSeverity(StructureParseIssueSeverity.LOW);
            issue.setSummary("解析器记录了保守警告标记。");
            issue.setDetail("保留该警告，便于后续治理阶段结合更强上下文继续细化。");
            issue.setSuggestedAction("请评审解析产物，并判断是否需要更深入分析。");
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
                                                               DataSourceTypeEnum datasourceType,
                                                               SqlParserMode parserMode) {
        LogicalObjectExpansionResult result = new LogicalObjectExpansionResult();
        ViewExpansionContext context = new ViewExpansionContext(tenantId, datasourceCode, datasourceType, parserMode, result);
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
            return applyGovernanceDbViewFallback(qualifiedObject, fallback, depth, context);
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
            if (depth == 0) {
                context.getResult().addSurfaceRef(hit);
            }
            context.getResult().addUnresolvedReason(
                "view=" + hit.getObjectKey() + "，原因="
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
        if (depth == 0) {
            context.getResult().addSurfaceRef(tableHit);
        }
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
        if (depth == 0) {
            context.getResult().addSurfaceRef(viewHit);
        }
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
        context.getResult().addUnresolvedReason("view=" + viewHit.getObjectKey() + "，原因=" + reason);
        return Collections.emptyList();
    }

    private List<String> applyGovernanceDbViewFallback(QualifiedObjectName qualifiedObject,
                                                       GovernanceDbViewResolveResponse fallback,
                                                       int depth,
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
        if (depth == 0) {
            context.getResult().addSurfaceRef(viewHit);
        }
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
        request.setDatasourceType(context.getDatasourceType());
        request.setCatalogName(qualifiedObject.getCatalogName());
        request.setSchemaName(qualifiedObject.getSchemaName());
        request.setObjectName(qualifiedObject.getObjectName());
        try {
            return datasourceViewMetadataClient.resolveView(request);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                "操作日志 operation=STRUCTURE_PARSE_DB_VIEW_METADATA tenantId={} datasourceCode={} objectName={} status=DEGRADED reason={}",
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
                "操作日志 operation=STRUCTURE_PARSE_DB_VIEW_CATALOG_FALLBACK tenantId={} datasourceCode={} viewName={} status=DEGRADED reason={}",
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

}
