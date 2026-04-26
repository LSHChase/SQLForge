package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseLogicalObjectHitVO;
import com.company.sqloptimization.application.controller.vo.StructureParseQueryDateSummaryVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.parse.StructureLogicalObjectType;
import com.company.sqloptimization.domain.parse.StructureParseComplexityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueDomain;
import com.company.sqloptimization.domain.parse.StructureParseIssueSeverity;
import com.company.sqloptimization.domain.parse.StructureParseLogicalObjectHit;
import com.company.sqloptimization.domain.parse.StructureParsePriorityAssessment;
import com.company.sqloptimization.domain.parse.StructureParsePriorityLevel;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateStatus;
import com.company.sqloptimization.domain.parse.StructureParseQueryDateSummary;
import com.company.sqloptimization.domain.parse.StructureParseResult;
import com.company.sqloptimization.domain.parse.StructureParseSyntaxStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class StructureParseApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructureParseApplicationService.class);
    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String MATCH_SOURCE_SQL = "SQL_TABLE_SCAN";
    private static final String MATCH_SOURCE_HEURISTIC = "NAME_HEURISTIC";

    private final SqlOptimizationPipelineService sqlOptimizationPipelineService;

    public StructureParseApplicationService(SqlOptimizationPipelineService sqlOptimizationPipelineService) {
        this.sqlOptimizationPipelineService = sqlOptimizationPipelineService;
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
        try {
            SqlOptimizationPipelineService.ParsedSqlProfile profile =
                sqlOptimizationPipelineService.analyze(request.getSqlText(), DataSourceTypeEnum.AUTO);
            result = new StructureParseResult();
            result.setParseTaskId(parseTaskId);
            result.setSyntaxStatus(StructureParseSyntaxStatus.VALID);
            result.setComplexityLevel(resolveComplexity(profile));
            result.setSqlType("SELECT");
            result.setQueryDateSummary(extractQueryDateSummary(request.getSqlText(), profile));
            result.setLogicalObjectHits(buildLogicalObjectHits(profile));
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
        return toResponse(result);
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

    private List<StructureParseLogicalObjectHit> buildLogicalObjectHits(SqlOptimizationPipelineService.ParsedSqlProfile profile) {
        List<StructureParseLogicalObjectHit> hits = new ArrayList<StructureParseLogicalObjectHit>();
        for (String table : profile.getTables()) {
            StructureParseLogicalObjectHit hit = new StructureParseLogicalObjectHit();
            hit.setObjectName(table);
            hit.setMappedPhysicalTargets(Collections.singletonList(table));
            if (isDbViewHeuristic(table)) {
                hit.setObjectType(StructureLogicalObjectType.DB_VIEW);
                hit.setMatchSource(MATCH_SOURCE_HEURISTIC);
                hit.setResolved(Boolean.FALSE);
            } else {
                hit.setObjectType(StructureLogicalObjectType.TABLE);
                hit.setMatchSource(MATCH_SOURCE_SQL);
                hit.setResolved(Boolean.TRUE);
            }
            hits.add(hit);
        }
        return hits;
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

    private List<StructureParseLogicalObjectHitVO> toLogicalObjectHitVOs(List<StructureParseLogicalObjectHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return Collections.emptyList();
        }
        List<StructureParseLogicalObjectHitVO> vos = new ArrayList<StructureParseLogicalObjectHitVO>();
        for (StructureParseLogicalObjectHit hit : hits) {
            StructureParseLogicalObjectHitVO vo = new StructureParseLogicalObjectHitVO();
            vo.setObjectType(hit.getObjectType().name());
            vo.setObjectName(hit.getObjectName());
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
