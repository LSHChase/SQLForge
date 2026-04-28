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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;

@Service
public class StructureParseApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureParseApplicationService.class);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String MATCH_SOURCE_SQL = "SQL_TABLE_SCAN";
    private static final String MATCH_SOURCE_HEURISTIC = "NAME_HEURISTIC";

    private final SqlOptimizationPipelineService sqlOptimizationPipelineService;
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService,
                                            GovernanceCapabilityClient governanceCapabilityClient) {
        this.sqlOptimizationPipelineService = sqlOptimizationPipelineService;
        this.governanceCapabilityClient = governanceCapabilityClient;
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
        try {
            profile = sqlOptimizationPipelineService.analyze(request.getSqlText(), DataSourceTypeEnum.AUTO);
            result = new StructureParseResult();
            result.setParseTaskId(parseTaskId);
            result.setSyntaxStatus(StructureParseSyntaxStatus.VALID);
            result.setComplexityLevel(resolveComplexity(profile));
            result.setSqlType("SELECT");
            result.setQueryDateSummary(extractQueryDateSummary(request.getSqlText(), profile));
            result.setLogicalObjectHits(buildLogicalObjectHits(profile, tenantId, datasourceCode));
            result.setRiskTags(new ArrayList<String>(profile.getWarnings()));
            result.setRewriteCandidates(sqlOptimizationPipelineService.deriveRewriteCandidateRules(profile));
            result.setIssues(buildIssues(profile));
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
            result = buildInvalidResult(parseTaskId, ex);
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
        StructureParseResponseVO response = toResponse(result);
        enrichQueryIntent(response, request.getSqlText(), profile);
        return response;
    }

    private StructureParseResult buildInvalidResult(String parseTaskId,
                                                    SqlOptimizationPipelineService.SqlOptimizationExecutionException ex) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueCode("SQL_SYNTAX_INVALID");
        issue.setIssueDomain(StructureParseIssueDomain.STRUCTURE);
        issue.setIssueScene("PARSER_FAILURE");
        issue.setSeverity(StructureParseIssueSeverity.HIGH);
        issue.setSummary(ex.getMessage());
        issue.setDetail(ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
        issue.setSuggestedAction(ex.getSuggestedAction());
        issue.setImportant(Boolean.TRUE);
        issue.setUrgent(Boolean.FALSE);

        StructureParseResult result = new StructureParseResult();
        result.setParseTaskId(parseTaskId);
        result.setSyntaxStatus(StructureParseSyntaxStatus.INVALID);
        result.setComplexityLevel(StructureParseComplexityLevel.SIMPLE);
        result.setSqlType("UNKNOWN");
        StructureParseQueryDateSummary queryDateSummary = new StructureParseQueryDateSummary();
        queryDateSummary.setQueryDateStatus(StructureParseQueryDateStatus.UNRESOLVED);
        result.setQueryDateSummary(queryDateSummary);
        result.setLogicalObjectHits(Collections.<StructureParseLogicalObjectHit>emptyList());
        result.setRiskTags(Collections.singletonList("SQL_SYNTAX_INVALID"));
        result.setRewriteCandidates(Collections.<String>emptyList());
        result.setIssues(Collections.singletonList(issue));
        result.applyAssessment(StructureParsePriorityScorer.assess(issue));
        return result;
    }

    private List<StructureParseIssue> buildIssues(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<StructureParseIssue> issues = new ArrayList<StructureParseIssue>();
        for (String warning : profile.getWarnings()) {
            issues.add(buildIssueFromWarning(warning, profile));
        }
        return issues;
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
            issue.setSummary("The statement joins multiple datasets with limited static join-selectivity evidence.");
            issue.setDetail("Without metadata, the structure parser treats this as a conservative large-table join risk.");
            issue.setSuggestedAction("Confirm join keys and filter placement in access parse or benchmark before online use.");
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

    private List<StructureParseLogicalObjectHit> buildLogicalObjectHits(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                                                       String tenantId,
                                                                       String datasourceCode) {
        List<StructureParseLogicalObjectHit> hits = new ArrayList<StructureParseLogicalObjectHit>();
        for (String table : profile.getTables()) {
            StructureParseLogicalObjectHit hit = new StructureParseLogicalObjectHit();
            hit.setObjectName(table);
            hit.setMappedPhysicalTargets(Collections.singletonList(table));
            if (isDbViewHeuristic(table)) {
                hit.setObjectType(LogicalObjectType.DB_VIEW);
                hit.setMatchSource(MATCH_SOURCE_HEURISTIC);
                hit.setResolved(Boolean.FALSE);
            } else {
                hit.setObjectType(LogicalObjectType.TABLE);
                hit.setMatchSource(MATCH_SOURCE_SQL);
                hit.setResolved(Boolean.TRUE);
            }
            fillLogicalObjectReferenceFields(hit);
            enrichDbViewDependencyEvidence(hit, tenantId, datasourceCode);
            hits.add(hit);
        }
        return hits;
    }

    private void enrichDbViewDependencyEvidence(StructureParseLogicalObjectHit hit, String tenantId, String datasourceCode) {
        if (hit == null || hit.getObjectType() != LogicalObjectType.DB_VIEW || !StringUtils.hasText(datasourceCode)) {
            return;
        }
        try {
            GovernanceDbViewResolveRequest request = new GovernanceDbViewResolveRequest();
            request.setTenantId(tenantId);
            request.setDatasourceCode(datasourceCode);
            request.setViewName(hit.getObjectName());
            GovernanceDbViewResolveResponse response = governanceCapabilityClient.resolveDbView(request);
            if (response == null) {
                return;
            }
            if (Boolean.TRUE.equals(response.getResolved())) {
                hit.setResolved(Boolean.TRUE);
            }
            if (StringUtils.hasText(response.getObjectKey())) {
                hit.setObjectKey(response.getObjectKey());
            }
            if (response.getDependencies() != null && !response.getDependencies().isEmpty()) {
                List<String> targets = new ArrayList<String>();
                for (GovernanceDbViewDependencyRef dependency : response.getDependencies()) {
                    if (dependency == null) {
                        continue;
                    }
                    String key = StringUtils.hasText(dependency.getObjectKey())
                        ? dependency.getObjectKey()
                        : dependency.getObjectName();
                    if (StringUtils.hasText(key)) {
                        targets.add(key);
                    }
                }
                if (!targets.isEmpty()) {
                    hit.setMappedPhysicalTargets(targets);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn(
                "operation=STRUCTURE_PARSE_DB_VIEW_ENRICH entity={} tenantId={} datasourceCode={} viewName={} status=DEGRADED reason={}",
                hit.getObjectKey(),
                tenantId,
                datasourceCode,
                hit.getObjectName(),
                ex.getMessage()
            );
        }
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
            + profile.getGroupByCount()
            + profile.getAggregateFunctions().size()
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
            response.setIntentProfile(unknownIntentProfile());
            response.setFeatureSummary(unknownFeatureSummary());
            response.setEstimatedResourceCost(unknownResourceEstimate());
            response.setRiskChecklist(Collections.<StructureParseRiskVO>emptyList());
            return;
        }
        String scanMode = resolveScanMode(profile);
        String joinType = resolveJoinType(profile);
        String computeDensity = resolveComputeDensity(profile);
        String resourceType = resolveResourceType(profile, scanMode, computeDensity);
        String slaLevel = resolveSlaLevel(profile, scanMode, computeDensity);
        List<StructureParseRiskVO> risks = buildRiskChecklist(profile);

        StructureParseIntentProfileVO intentProfile = new StructureParseIntentProfileVO();
        intentProfile.setScanMode(scanMode);
        intentProfile.setJoinType(joinType);
        intentProfile.setComputeDensity(computeDensity);
        intentProfile.setResourceType(resourceType);
        intentProfile.setSlaLevel(slaLevel);
        intentProfile.setConfidence(profile.getParserEngine().equals("TRINO") ? "MEDIUM" : "HIGH");
        intentProfile.setClassificationLabels(buildClassificationLabels(scanMode, joinType, computeDensity, resourceType, slaLevel));
        response.setIntentProfile(intentProfile);

        StructureParseFeatureSummaryVO featureSummary = new StructureParseFeatureSummaryVO();
        featureSummary.setParserEngine(profile.getParserEngine());
        featureSummary.setScanMode(scanMode);
        featureSummary.setJoinType(joinType);
        featureSummary.setComputeDensity(computeDensity);
        featureSummary.setResourceType(resourceType);
        featureSummary.setSlaLevel(slaLevel);
        featureSummary.setTableCount(Integer.valueOf(profile.getTables().size()));
        featureSummary.setJoinCount(Integer.valueOf(profile.getJoinCount()));
        featureSummary.setPredicateCount(Integer.valueOf(profile.getPredicateCount()));
        featureSummary.setWindowFunctionCount(Integer.valueOf(profile.getWindowFunctionCount()));
        featureSummary.setUdfFunctionCount(Integer.valueOf(profile.getUdfFunctionCount()));
        featureSummary.setRepeatedExpressionCount(Integer.valueOf(profile.getRepeatedExpressionCount()));
        featureSummary.setEvidence(buildFeatureEvidence(profile));
        response.setFeatureSummary(featureSummary);
        response.setRiskChecklist(risks);
        response.setEstimatedResourceCost(buildResourceEstimate(profile, risks));
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
        if (profile.getUdfFunctionCount() > 0) {
            return "UDF";
        }
        if (profile.getAggregateFunctions().size() + profile.getGroupByCount() + profile.getOrderByCount() >= 3
            || profile.getJoinCount() >= 3) {
            return "HEAVY";
        }
        return "LIGHT";
    }

    private String resolveResourceType(SqlOptimizationPipelineService.ParsedSqlProfile profile,
                                       String scanMode,
                                       String computeDensity) {
        if (profile.getJoinCount() > 0 || profile.isSetOperation()) {
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
            || profile.getJoinCount() >= 2) {
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
        evidence.add("tables=" + profile.getTables().size());
        evidence.add("predicates=" + profile.getPredicateCount());
        evidence.add("joins=" + profile.getJoinCount());
        evidence.add("aggregates=" + profile.getAggregateFunctions().size());
        evidence.add("windows=" + profile.getWindowFunctionCount());
        evidence.add("repeatedExpressions=" + profile.getRepeatedExpressionCount());
        return evidence;
    }

    private List<StructureParseRiskVO> buildRiskChecklist(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<StructureParseRiskVO> risks = new ArrayList<StructureParseRiskVO>();
        Set<String> emitted = new LinkedHashSet<String>();
        for (String warning : profile.getWarnings()) {
            if ("NO_PREDICATE".equals(warning)) {
                addRisk(risks, emitted, risk("FULL_TABLE_SCAN_RISK", "HIGH", "Full table scan risk",
                    "predicateCount=0", "Add tenant, time, partition, or business-key predicates."));
            } else if ("LARGE_JOIN_PAIR_RISK".equals(warning) || "HEAVY_JOIN_GRAPH".equals(warning)) {
                addRisk(risks, emitted, risk("LARGE_TABLE_JOIN_RISK", "HIGH", "Large table join risk",
                    "joinCount=" + profile.getJoinCount(), "Confirm join keys and data volume with access parse or benchmark."));
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
                                                                   List<StructureParseRiskVO> risks) {
        StructureParseResourceEstimateVO estimate = new StructureParseResourceEstimateVO();
        estimate.setCpu(level(profile.getAggregateFunctions().size() + profile.getUdfFunctionCount()
            + profile.getRepeatedExpressionCount()));
        estimate.setIo(profile.getPredicateCount() == 0 ? "HIGH" : "MEDIUM");
        estimate.setMemory(profile.getOrderByCount() + profile.getWindowFunctionCount() > 0 ? "HIGH" : "LOW");
        estimate.setNetwork(profile.getJoinCount() > 0 || profile.isSetOperation() ? "HIGH" : "LOW");
        estimate.setResultSize(profile.isSelectStar() || !profile.isLimitPresent() ? "HIGH" : "MEDIUM");
        estimate.setOverall(resolveOverallEstimate(estimate, risks));
        estimate.setEvidence(buildFeatureEvidence(profile));
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
        return response;
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
            vos.add(vo);
        }
        return vos;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
