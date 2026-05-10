package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneLogicalObjectDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneReportDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchImportanceStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchLogicalObjectStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchSqlStatisticVO;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueScoringSnapshot;
import com.company.sqloptimization.domain.parse.StructureParseIssueSeverity;
import com.company.sqloptimization.domain.parse.StructureParsePriorityLevel;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import com.company.sqloptimization.domain.reportbatch.ReportBatchItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

class ReportBatchParseStatisticsAssembler {

    private static final int SQL_STATISTIC_PREVIEW_LIMIT = 500;
    private static final int ISSUE_SCENE_DETAIL_PAGE_SIZE = 10;
    private static final int MIN_SHARED_ISSUE_SCENES_FOR_MERGE = 2;
    private static final String REPORT_SQL_MERGE_CANDIDATE = "REPORT_SQL_MERGE_CANDIDATE";
    private static final String VALID_STRUCTURE_STATUS = "VALID";

    ReportBatchParseStatisticsVO build(List<ReportBatchItem> sourceItems) {
        return build(sourceItems, null, null, null);
    }

    ReportBatchParseStatisticsVO build(List<ReportBatchItem> sourceItems,
                                       Integer pageNumber,
                                       Integer pageSize,
                                       String reportCode) {
        List<ReportBatchItem> items = sourceItems == null
            ? Collections.<ReportBatchItem>emptyList()
            : sourceItems;
        SqlStatisticPage pageSelection = SqlStatisticPage.from(
            pageNumber,
            pageSize,
            reportCode,
            SQL_STATISTIC_PREVIEW_LIMIT
        );
        MergeCandidateIndex mergeCandidateIndex = buildMergeCandidateIndex(items);
        List<ReportBatchSqlStatisticVO> sqlStatistics = new ArrayList<ReportBatchSqlStatisticVO>(items.size());
        Map<String, SceneAccumulator> sceneAccumulators = new LinkedHashMap<String, SceneAccumulator>();
        Map<String, Integer> severityDistribution = initialSeverityDistribution();
        Map<String, ReportAccumulator> reportAccumulators = new LinkedHashMap<String, ReportAccumulator>();
        Map<String, MatrixAccumulator> priorityMatrix = new LinkedHashMap<String, MatrixAccumulator>();
        Map<String, ImportanceAccumulator> importanceAccumulators = new LinkedHashMap<String, ImportanceAccumulator>();
        Map<String, LogicalObjectAccumulator> logicalObjectAccumulators = new LinkedHashMap<String, LogicalObjectAccumulator>();
        Map<String, Integer> priorityDistribution = initialPriorityDistribution();
        Set<String> issueSceneKeys = new LinkedHashSet<String>();
        int issueSqlCount = 0;
        int totalIssueCount = 0;
        int importantSqlCount = 0;
        int urgentSqlCount = 0;

        for (ReportBatchItem item : items) {
            List<String> effectiveIssueScenes = effectiveIssueScenes(item, mergeCandidateIndex);
            SqlIssueAssessment assessment = assessIssueScenes(effectiveIssueScenes);
            if (assessment.issueCount > 0) {
                issueSqlCount++;
            }
            if (assessment.important) {
                importantSqlCount++;
            }
            if (assessment.urgent) {
                urgentSqlCount++;
            }
            totalIssueCount += assessment.issueCount;
            priorityDistribution.put(
                assessment.highestPriorityLevel,
                Integer.valueOf(priorityDistribution.get(assessment.highestPriorityLevel).intValue() + 1)
            );
            sqlStatistics.add(toSqlStatistic(item, effectiveIssueScenes, assessment));
            accumulateScenes(item, effectiveIssueScenes, items.size(), sceneAccumulators, severityDistribution, issueSceneKeys);
            accumulateReport(item, effectiveIssueScenes, assessment, reportAccumulators, mergeCandidateIndex);
            accumulatePriority(item, assessment, priorityMatrix);
            accumulateImportance(item, assessment, importanceAccumulators);
            accumulateLogicalObjects(item, assessment, logicalObjectAccumulators);
        }

        ReportBatchParseStatisticsVO statistics = new ReportBatchParseStatisticsVO();
        statistics.setOverview(overview(items.size(), issueSqlCount, totalIssueCount, issueSceneKeys.size(), importantSqlCount,
            urgentSqlCount, priorityDistribution));
        statistics.setIssueSceneStatistics(toIssueSceneStatistics(sceneAccumulators, items.size()));
        statistics.setSeverityDistribution(severityDistribution);
        statistics.setImportanceStatistics(toImportanceStatistics(importanceAccumulators));
        statistics.setReportStatistics(toReportStatistics(reportAccumulators));
        statistics.setMergeCandidateReportCount(Integer.valueOf(mergeCandidateIndex.reportCount()));
        List<ReportBatchSqlStatisticVO> sortedSqlStatistics = sortSqlStatistics(sqlStatistics);
        List<ReportBatchSqlStatisticVO> filteredSqlStatistics = filterSqlStatistics(
            sortedSqlStatistics,
            pageSelection.reportCode
        );
        List<ReportBatchSqlStatisticVO> pageSqlStatistics = pageSqlStatistics(filteredSqlStatistics, pageSelection);
        statistics.setSqlStatisticLimit(Integer.valueOf(pageSelection.pageSize));
        statistics.setSqlStatisticTruncated(Boolean.valueOf(filteredSqlStatistics.size() > pageSqlStatistics.size()));
        statistics.setOmittedSqlStatisticCount(Integer.valueOf(Math.max(0,
            filteredSqlStatistics.size() - pageSqlStatistics.size())));
        statistics.setSqlStatisticPageNumber(Integer.valueOf(pageSelection.pageNumber));
        statistics.setSqlStatisticPageSize(Integer.valueOf(pageSelection.pageSize));
        statistics.setSqlStatisticPageCount(Integer.valueOf(pageCount(
            filteredSqlStatistics.size(),
            pageSelection.pageSize
        )));
        statistics.setSqlStatisticTotalCount(Integer.valueOf(filteredSqlStatistics.size()));
        statistics.setSqlStatisticReportCodeFilter(pageSelection.reportCode);
        statistics.setSqlStatistics(pageSqlStatistics);
        statistics.setPriorityMatrix(toPriorityMatrix(priorityMatrix));
        statistics.setLogicalObjectStatistics(toLogicalObjectStatistics(logicalObjectAccumulators));
        return statistics;
    }

    ReportBatchIssueSceneDetailVO buildIssueSceneDetail(List<ReportBatchItem> sourceItems,
                                                        String issueScene,
                                                        Integer pageNumber,
                                                        Integer pageSize,
                                                        Integer reportDetailPageNumber,
                                                        Integer reportDetailPageSize,
                                                        Integer logicalObjectDetailPageNumber,
                                                        Integer logicalObjectDetailPageSize,
                                                        String reportCode,
                                                        String logicalObjectKey) {
        List<ReportBatchItem> items = sourceItems == null
            ? Collections.<ReportBatchItem>emptyList()
            : sourceItems;
        String normalizedIssueScene = trimToNull(issueScene);
        String reportCodeFilter = trimToNull(reportCode);
        String logicalObjectKeyFilter = trimToNull(logicalObjectKey);
        SqlStatisticPage pageSelection = SqlStatisticPage.from(
            pageNumber,
            pageSize,
            reportCodeFilter,
            SQL_STATISTIC_PREVIEW_LIMIT
        );
        SqlStatisticPage reportDetailPageSelection = SqlStatisticPage.from(
            reportDetailPageNumber,
            reportDetailPageSize,
            null,
            ISSUE_SCENE_DETAIL_PAGE_SIZE
        );
        SqlStatisticPage logicalObjectDetailPageSelection = SqlStatisticPage.from(
            logicalObjectDetailPageNumber,
            logicalObjectDetailPageSize,
            null,
            ISSUE_SCENE_DETAIL_PAGE_SIZE
        );
        MergeCandidateIndex mergeCandidateIndex = buildMergeCandidateIndex(items);
        List<ReportBatchSqlStatisticVO> sqlStatistics = new ArrayList<ReportBatchSqlStatisticVO>();
        Map<String, IssueSceneReportAccumulator> reportAccumulators =
            new LinkedHashMap<String, IssueSceneReportAccumulator>();
        Map<String, LogicalObjectAccumulator> logicalObjectAccumulators =
            new LinkedHashMap<String, LogicalObjectAccumulator>();
        int affectedIssueCount = 0;

        for (ReportBatchItem item : items) {
            List<String> effectiveIssueScenes = effectiveIssueScenes(item, mergeCandidateIndex);
            int issueCount = issueCount(effectiveIssueScenes, normalizedIssueScene);
            if (issueCount <= 0 || !matchesReportCode(item, reportCodeFilter) || !matchesLogicalObject(item, logicalObjectKeyFilter)) {
                continue;
            }
            affectedIssueCount += issueCount;
            sqlStatistics.add(toIssueSceneSqlStatistic(item, normalizedIssueScene, issueCount));
            accumulateIssueSceneReport(item, issueCount, reportAccumulators);
            accumulateIssueSceneLogicalObjects(item, issueCount, logicalObjectAccumulators);
        }

        StructureParseIssueScoringSnapshot snapshot = snapshot(normalizedIssueScene);
        List<ReportBatchSqlStatisticVO> sortedSqlStatistics = sortSqlStatistics(sqlStatistics);
        List<ReportBatchSqlStatisticVO> pageSqlStatistics = pageSqlStatistics(sortedSqlStatistics, pageSelection);
        List<ReportBatchIssueSceneReportDetailVO> reportDetails = toIssueSceneReportDetails(reportAccumulators);
        List<ReportBatchIssueSceneLogicalObjectDetailVO> logicalObjectDetails =
            toIssueSceneLogicalObjectDetails(logicalObjectAccumulators);
        List<ReportBatchIssueSceneReportDetailVO> pageReportDetails =
            pageItems(reportDetails, reportDetailPageSelection);
        List<ReportBatchIssueSceneLogicalObjectDetailVO> pageLogicalObjectDetails =
            pageItems(logicalObjectDetails, logicalObjectDetailPageSelection);
        ReportBatchIssueSceneDetailVO detail = new ReportBatchIssueSceneDetailVO();
        detail.setIssueScene(normalizedIssueScene);
        detail.setIssueDomain(snapshot.getIssueDomain().name());
        detail.setSeverity(snapshot.getSeverity().name());
        detail.setPriorityLevel(snapshot.getPriorityLevel().name());
        detail.setPriorityScore(Integer.valueOf(snapshot.getPriorityScore()));
        detail.setAffectedSqlCount(Integer.valueOf(sqlStatistics.size()));
        detail.setAffectedIssueCount(Integer.valueOf(affectedIssueCount));
        detail.setReportCount(Integer.valueOf(reportAccumulators.size()));
        detail.setLogicalObjectCount(Integer.valueOf(logicalObjectAccumulators.size()));
        detail.setReportCodeFilter(reportCodeFilter);
        detail.setLogicalObjectKeyFilter(logicalObjectKeyFilter);
        detail.setReportDetailPageNumber(Integer.valueOf(reportDetailPageSelection.pageNumber));
        detail.setReportDetailPageSize(Integer.valueOf(reportDetailPageSelection.pageSize));
        detail.setReportDetailPageCount(Integer.valueOf(pageCount(reportDetails.size(), reportDetailPageSelection.pageSize)));
        detail.setReportDetailTotalCount(Integer.valueOf(reportDetails.size()));
        detail.setLogicalObjectDetailPageNumber(Integer.valueOf(logicalObjectDetailPageSelection.pageNumber));
        detail.setLogicalObjectDetailPageSize(Integer.valueOf(logicalObjectDetailPageSelection.pageSize));
        detail.setLogicalObjectDetailPageCount(Integer.valueOf(pageCount(
            logicalObjectDetails.size(),
            logicalObjectDetailPageSelection.pageSize
        )));
        detail.setLogicalObjectDetailTotalCount(Integer.valueOf(logicalObjectDetails.size()));
        detail.setSqlStatisticPageNumber(Integer.valueOf(pageSelection.pageNumber));
        detail.setSqlStatisticPageSize(Integer.valueOf(pageSelection.pageSize));
        detail.setSqlStatisticPageCount(Integer.valueOf(pageCount(sortedSqlStatistics.size(), pageSelection.pageSize)));
        detail.setSqlStatisticTotalCount(Integer.valueOf(sortedSqlStatistics.size()));
        detail.setReportDetails(pageReportDetails);
        detail.setLogicalObjectDetails(pageLogicalObjectDetails);
        detail.setSqlStatistics(pageSqlStatistics);
        return detail;
    }

    private ParseStatisticsOverviewVO overview(int totalSqlCount,
                                               int issueSqlCount,
                                               int totalIssueCount,
                                               int issueSceneCount,
                                               int importantSqlCount,
                                               int urgentSqlCount,
                                               Map<String, Integer> priorityDistribution) {
        ParseStatisticsOverviewVO overview = new ParseStatisticsOverviewVO();
        overview.setTotalSqlCount(Integer.valueOf(totalSqlCount));
        overview.setIssueSqlCount(Integer.valueOf(issueSqlCount));
        overview.setTotalIssueCount(Integer.valueOf(totalIssueCount));
        overview.setIssueSceneCount(Integer.valueOf(issueSceneCount));
        overview.setImportantSqlCount(Integer.valueOf(importantSqlCount));
        overview.setUrgentSqlCount(Integer.valueOf(urgentSqlCount));
        overview.setPriorityDistribution(priorityDistribution);
        return overview;
    }

    private void accumulateScenes(ReportBatchItem item,
                                  List<String> issueScenes,
                                  int totalSqlCount,
                                  Map<String, SceneAccumulator> accumulators,
                                  Map<String, Integer> severityDistribution,
                                  Set<String> issueSceneKeys) {
        Set<String> uniqueScenes = new LinkedHashSet<String>(issueScenes);
        for (String scene : uniqueScenes) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            String sceneKey = firstNonBlank(scene, snapshot.getIssueScene());
            issueSceneKeys.add(sceneKey);
            SceneAccumulator accumulator = accumulators.get(sceneKey);
            if (accumulator == null) {
                accumulator = new SceneAccumulator(sceneKey, snapshot);
                accumulators.put(sceneKey, accumulator);
            }
            accumulator.affectedSqlCount++;
            if (StringUtils.hasText(item.getReportCode())) {
                accumulator.reportCodes.add(item.getReportCode());
            }
            accumulator.logicalObjectKeys.addAll(item.getLogicalObjectKeys());
        }
        for (String scene : issueScenes) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            String sceneKey = firstNonBlank(scene, snapshot.getIssueScene());
            SceneAccumulator accumulator = accumulators.get(sceneKey);
            if (accumulator != null) {
                accumulator.affectedIssueCount++;
            }
            String severity = snapshot.getSeverity().name();
            severityDistribution.put(severity, Integer.valueOf(severityDistribution.get(severity).intValue() + 1));
        }
    }

    private void accumulateReport(ReportBatchItem item,
                                  List<String> issueScenes,
                                  SqlIssueAssessment assessment,
                                  Map<String, ReportAccumulator> accumulators,
                                  MergeCandidateIndex mergeCandidateIndex) {
        String reportCode = firstNonBlank(item.getReportCode(), "UNSPECIFIED_REPORT");
        ReportAccumulator accumulator = accumulators.get(reportCode);
        if (accumulator == null) {
            accumulator = new ReportAccumulator(reportCode);
            accumulators.put(reportCode, accumulator);
        }
        accumulator.sqlCount++;
        accumulator.issueCount += assessment.issueCount;
        if (assessment.issueCount > 0) {
            accumulator.issueSqlCount++;
        }
        if (assessment.highestPriorityScore > accumulator.highestPriorityScore) {
            accumulator.highestPriorityScore = assessment.highestPriorityScore;
            accumulator.highestPriorityLevel = assessment.highestPriorityLevel;
        }
        accumulator.important = accumulator.important || assessment.important;
        accumulator.urgent = accumulator.urgent || assessment.urgent;
        accumulator.issueScenes.addAll(issueScenes);
        MergeCandidateAssessment mergeCandidate = mergeCandidateIndex.assessmentFor(reportCode);
        if (mergeCandidate != null) {
            accumulator.mergeCandidate = true;
            accumulator.mergeCandidateSqlCount = mergeCandidate.sqlCount();
            accumulator.mergeCandidateReason = mergeCandidate.reason;
        }
    }

    private void accumulatePriority(ReportBatchItem item,
                                    SqlIssueAssessment assessment,
                                    Map<String, MatrixAccumulator> matrix) {
        String bucket = urgencyBucket(assessment.important, assessment.urgent);
        String key = assessment.highestPriorityLevel + "|" + bucket;
        MatrixAccumulator accumulator = matrix.get(key);
        if (accumulator == null) {
            accumulator = new MatrixAccumulator(assessment.highestPriorityLevel, bucket);
            matrix.put(key, accumulator);
        }
        accumulator.sqlCount++;
        accumulator.issueCount += assessment.issueCount;
        if (StringUtils.hasText(item.getReportCode())) {
            accumulator.reportCodes.add(item.getReportCode());
        }
    }

    private void accumulateImportance(ReportBatchItem item,
                                      SqlIssueAssessment assessment,
                                      Map<String, ImportanceAccumulator> accumulators) {
        String bucket = urgencyBucket(assessment.important, assessment.urgent);
        ImportanceAccumulator accumulator = accumulators.get(bucket);
        if (accumulator == null) {
            accumulator = new ImportanceAccumulator(bucket);
            accumulators.put(bucket, accumulator);
        }
        accumulator.sqlCount++;
        accumulator.issueCount += assessment.issueCount;
        if (StringUtils.hasText(item.getReportCode())) {
            accumulator.reportCodes.add(item.getReportCode());
        }
    }

    private void accumulateLogicalObjects(ReportBatchItem item,
                                          SqlIssueAssessment assessment,
                                          Map<String, LogicalObjectAccumulator> accumulators) {
        Set<String> uniqueObjectKeys = new LinkedHashSet<String>(item.getLogicalObjectKeys());
        for (String objectKey : uniqueObjectKeys) {
            if (!StringUtils.hasText(objectKey)) {
                continue;
            }
            LogicalObjectAccumulator accumulator = accumulators.get(objectKey);
            if (accumulator == null) {
                accumulator = new LogicalObjectAccumulator(objectKey);
                accumulators.put(objectKey, accumulator);
            }
            accumulator.sqlCount++;
            accumulator.issueCount += assessment.issueCount;
            if (StringUtils.hasText(item.getReportCode())) {
                accumulator.reportCodes.add(item.getReportCode());
            }
        }
    }

    private List<ParseIssueSceneStatisticVO> toIssueSceneStatistics(Map<String, SceneAccumulator> accumulators, int totalSqlCount) {
        List<ParseIssueSceneStatisticVO> result = new ArrayList<ParseIssueSceneStatisticVO>(accumulators.size());
        for (SceneAccumulator accumulator : accumulators.values()) {
            ParseIssueSceneStatisticVO vo = new ParseIssueSceneStatisticVO();
            vo.setIssueScene(accumulator.issueScene);
            vo.setIssueDomain(accumulator.snapshot.getIssueDomain().name());
            vo.setSeverity(accumulator.snapshot.getSeverity().name());
            vo.setPriorityLevel(accumulator.snapshot.getPriorityLevel().name());
            vo.setPriorityScore(Integer.valueOf(accumulator.snapshot.getPriorityScore()));
            vo.setAffectedSqlCount(Integer.valueOf(accumulator.affectedSqlCount));
            vo.setAffectedIssueCount(Integer.valueOf(accumulator.affectedIssueCount));
            vo.setSqlRatio(Double.valueOf(totalSqlCount == 0 ? 0D : (double) accumulator.affectedSqlCount / totalSqlCount));
            vo.setImportant(Boolean.valueOf(accumulator.snapshot.isImportant()));
            vo.setUrgent(Boolean.valueOf(accumulator.snapshot.isUrgent()));
            vo.setReportCount(Integer.valueOf(accumulator.reportCodes.size()));
            vo.setLogicalObjectCount(Integer.valueOf(accumulator.logicalObjectKeys.size()));
            vo.setSampleReportCodes(sampleValues(accumulator.reportCodes));
            vo.setSampleLogicalObjectKeys(sampleValues(accumulator.logicalObjectKeys));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ParseIssueSceneStatisticVO::getAffectedSqlCount, Comparator.reverseOrder())
            .thenComparing(ParseIssueSceneStatisticVO::getPriorityScore, Comparator.reverseOrder())
            .thenComparing(ParseIssueSceneStatisticVO::getIssueScene));
        return result;
    }

    private void accumulateIssueSceneReport(ReportBatchItem item,
                                            int issueCount,
                                            Map<String, IssueSceneReportAccumulator> accumulators) {
        String reportCode = firstNonBlank(item.getReportCode(), "UNSPECIFIED_REPORT");
        IssueSceneReportAccumulator accumulator = accumulators.get(reportCode);
        if (accumulator == null) {
            accumulator = new IssueSceneReportAccumulator(reportCode, item.getReportName());
            accumulators.put(reportCode, accumulator);
        }
        accumulator.sqlCount++;
        accumulator.issueCount += issueCount;
        accumulator.logicalObjectKeys.addAll(item.getLogicalObjectKeys());
    }

    private void accumulateIssueSceneLogicalObjects(ReportBatchItem item,
                                                   int issueCount,
                                                   Map<String, LogicalObjectAccumulator> accumulators) {
        Set<String> uniqueObjectKeys = new LinkedHashSet<String>(item.getLogicalObjectKeys());
        for (String objectKey : uniqueObjectKeys) {
            if (!StringUtils.hasText(objectKey)) {
                continue;
            }
            LogicalObjectAccumulator accumulator = accumulators.get(objectKey);
            if (accumulator == null) {
                accumulator = new LogicalObjectAccumulator(objectKey);
                accumulators.put(objectKey, accumulator);
            }
            accumulator.sqlCount++;
            accumulator.issueCount += issueCount;
            if (StringUtils.hasText(item.getReportCode())) {
                accumulator.reportCodes.add(item.getReportCode());
            }
        }
    }

    private List<ReportBatchIssueSceneReportDetailVO> toIssueSceneReportDetails(
        Map<String, IssueSceneReportAccumulator> accumulators) {
        List<ReportBatchIssueSceneReportDetailVO> result =
            new ArrayList<ReportBatchIssueSceneReportDetailVO>(accumulators.size());
        for (IssueSceneReportAccumulator accumulator : accumulators.values()) {
            ReportBatchIssueSceneReportDetailVO vo = new ReportBatchIssueSceneReportDetailVO();
            vo.setReportCode(accumulator.reportCode);
            vo.setReportName(accumulator.reportName);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setLogicalObjectCount(Integer.valueOf(accumulator.logicalObjectKeys.size()));
            vo.setLogicalObjectKeys(new ArrayList<String>(accumulator.logicalObjectKeys));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ReportBatchIssueSceneReportDetailVO::getSqlCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchIssueSceneReportDetailVO::getIssueCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchIssueSceneReportDetailVO::getReportCode));
        return result;
    }

    private List<ReportBatchIssueSceneLogicalObjectDetailVO> toIssueSceneLogicalObjectDetails(
        Map<String, LogicalObjectAccumulator> accumulators) {
        List<ReportBatchIssueSceneLogicalObjectDetailVO> result =
            new ArrayList<ReportBatchIssueSceneLogicalObjectDetailVO>(accumulators.size());
        for (LogicalObjectAccumulator accumulator : accumulators.values()) {
            ReportBatchIssueSceneLogicalObjectDetailVO vo = new ReportBatchIssueSceneLogicalObjectDetailVO();
            vo.setObjectKey(accumulator.objectKey);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setReportCount(Integer.valueOf(accumulator.reportCodes.size()));
            vo.setReportCodes(new ArrayList<String>(accumulator.reportCodes));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ReportBatchIssueSceneLogicalObjectDetailVO::getSqlCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchIssueSceneLogicalObjectDetailVO::getIssueCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchIssueSceneLogicalObjectDetailVO::getObjectKey));
        return result;
    }

    private List<ParseReportStatisticVO> toReportStatistics(Map<String, ReportAccumulator> accumulators) {
        List<ParseReportStatisticVO> result = new ArrayList<ParseReportStatisticVO>(accumulators.size());
        for (ReportAccumulator accumulator : accumulators.values()) {
            ParseReportStatisticVO vo = new ParseReportStatisticVO();
            vo.setReportCode(accumulator.reportCode);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueSqlCount(Integer.valueOf(accumulator.issueSqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setIssueSqlRatio(Double.valueOf(accumulator.sqlCount == 0 ? 0D : (double) accumulator.issueSqlCount / accumulator.sqlCount));
            vo.setHighestPriorityLevel(accumulator.highestPriorityLevel);
            vo.setHighestPriorityScore(Integer.valueOf(accumulator.highestPriorityScore));
            vo.setImportant(Boolean.valueOf(accumulator.important));
            vo.setUrgent(Boolean.valueOf(accumulator.urgent));
            vo.setIssueScenes(new ArrayList<String>(accumulator.issueScenes));
            vo.setMergeCandidate(Boolean.valueOf(accumulator.mergeCandidate));
            vo.setMergeCandidateSqlCount(Integer.valueOf(accumulator.mergeCandidateSqlCount));
            vo.setMergeCandidateReason(accumulator.mergeCandidateReason);
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ParseReportStatisticVO::getIssueSqlCount, Comparator.reverseOrder())
            .thenComparing(ParseReportStatisticVO::getHighestPriorityScore, Comparator.reverseOrder())
            .thenComparing(ParseReportStatisticVO::getReportCode));
        return result;
    }

    private List<ReportBatchSqlStatisticVO> sortSqlStatistics(List<ReportBatchSqlStatisticVO> statistics) {
        statistics.sort(Comparator
            .comparing(ReportBatchSqlStatisticVO::getHighestPriorityScore, Comparator.reverseOrder())
            .thenComparing(ReportBatchSqlStatisticVO::getIssueCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchSqlStatisticVO::getItemId));
        return statistics;
    }

    private List<ReportBatchSqlStatisticVO> filterSqlStatistics(List<ReportBatchSqlStatisticVO> statistics,
                                                               String reportCode) {
        if (!StringUtils.hasText(reportCode)) {
            return statistics;
        }
        List<ReportBatchSqlStatisticVO> result = new ArrayList<ReportBatchSqlStatisticVO>();
        for (ReportBatchSqlStatisticVO statistic : statistics) {
            if (reportCode.equals(statistic.getReportCode())) {
                result.add(statistic);
            }
        }
        return result;
    }

    private List<ReportBatchSqlStatisticVO> pageSqlStatistics(List<ReportBatchSqlStatisticVO> statistics,
                                                             SqlStatisticPage pageSelection) {
        return pageItems(statistics, pageSelection);
    }

    private <T> List<T> pageItems(List<T> items, SqlStatisticPage pageSelection) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        int start = Math.min(items.size(), (pageSelection.pageNumber - 1) * pageSelection.pageSize);
        int end = Math.min(items.size(), start + pageSelection.pageSize);
        return new ArrayList<T>(items.subList(start, end));
    }

    private int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    private List<ParsePriorityMatrixCellVO> toPriorityMatrix(Map<String, MatrixAccumulator> matrix) {
        List<ParsePriorityMatrixCellVO> result = new ArrayList<ParsePriorityMatrixCellVO>(matrix.size());
        for (MatrixAccumulator accumulator : matrix.values()) {
            ParsePriorityMatrixCellVO vo = new ParsePriorityMatrixCellVO();
            vo.setPriorityLevel(accumulator.priorityLevel);
            vo.setUrgencyBucket(accumulator.urgencyBucket);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setReportCount(Integer.valueOf(accumulator.reportCodes.size()));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ParsePriorityMatrixCellVO::getPriorityLevel)
            .thenComparing(ParsePriorityMatrixCellVO::getUrgencyBucket));
        return result;
    }

    private List<ReportBatchImportanceStatisticVO> toImportanceStatistics(Map<String, ImportanceAccumulator> accumulators) {
        List<ReportBatchImportanceStatisticVO> result = new ArrayList<ReportBatchImportanceStatisticVO>(accumulators.size());
        for (ImportanceAccumulator accumulator : accumulators.values()) {
            ReportBatchImportanceStatisticVO vo = new ReportBatchImportanceStatisticVO();
            vo.setImportanceBucket(accumulator.importanceBucket);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setReportCount(Integer.valueOf(accumulator.reportCodes.size()));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ReportBatchImportanceStatisticVO::getSqlCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchImportanceStatisticVO::getImportanceBucket));
        return result;
    }

    private List<ReportBatchLogicalObjectStatisticVO> toLogicalObjectStatistics(Map<String, LogicalObjectAccumulator> accumulators) {
        List<ReportBatchLogicalObjectStatisticVO> result =
            new ArrayList<ReportBatchLogicalObjectStatisticVO>(accumulators.size());
        for (LogicalObjectAccumulator accumulator : accumulators.values()) {
            ReportBatchLogicalObjectStatisticVO vo = new ReportBatchLogicalObjectStatisticVO();
            vo.setObjectKey(accumulator.objectKey);
            vo.setSqlCount(Integer.valueOf(accumulator.sqlCount));
            vo.setIssueCount(Integer.valueOf(accumulator.issueCount));
            vo.setReportCount(Integer.valueOf(accumulator.reportCodes.size()));
            vo.setReportCodes(new ArrayList<String>(accumulator.reportCodes));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ReportBatchLogicalObjectStatisticVO::getSqlCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchLogicalObjectStatisticVO::getIssueCount, Comparator.reverseOrder())
            .thenComparing(ReportBatchLogicalObjectStatisticVO::getObjectKey));
        return result;
    }

    private ReportBatchSqlStatisticVO toSqlStatistic(ReportBatchItem item,
                                                     List<String> issueScenes,
                                                     SqlIssueAssessment assessment) {
        return toSqlStatistic(item, issueScenes, assessment, issueScenes, Integer.valueOf(assessment.issueCount));
    }

    private ReportBatchSqlStatisticVO toIssueSceneSqlStatistic(ReportBatchItem item,
                                                               String issueScene,
                                                               int issueCount) {
        List<String> issueSceneScope = Collections.singletonList(issueScene);
        return toSqlStatistic(
            item,
            issueSceneScope,
            assessIssueScenes(issueSceneScope),
            issueSceneScope,
            Integer.valueOf(issueCount)
        );
    }

    private ReportBatchSqlStatisticVO toSqlStatistic(ReportBatchItem item,
                                                     List<String> issueScenes,
                                                     SqlIssueAssessment assessment,
                                                     List<String> locationIssueScenes,
                                                     Integer issueCountOverride) {
        ReportBatchSqlStatisticVO vo = new ReportBatchSqlStatisticVO();
        vo.setItemId(item.getItemId());
        vo.setBatchId(item.getBatchId());
        vo.setParseTaskId(item.getParseTaskId());
        vo.setReportCode(item.getReportCode());
        vo.setReportName(item.getReportName());
        vo.setDatasourceCode(item.getDatasourceCode());
        vo.setStage(item.getStage());
        vo.setSqlColumnName(item.getSqlColumnName());
        vo.setSqlOrdinalInReport(item.getSqlOrdinalInReport());
        vo.setStatus(item.getStatus() == null ? null : item.getStatus().name());
        vo.setSqlDigest(digest(item.getSqlText()));
        vo.setIssueCount(issueCountOverride == null ? Integer.valueOf(assessment.issueCount) : issueCountOverride);
        vo.setHighestPriorityLevel(assessment.highestPriorityLevel);
        vo.setHighestPriorityScore(Integer.valueOf(assessment.highestPriorityScore));
        vo.setImportant(Boolean.valueOf(assessment.important));
        vo.setUrgent(Boolean.valueOf(assessment.urgent));
        vo.setIssueScenes(new ArrayList<String>(issueScenes));
        vo.setIssueLocations(ReportBatchIssueLocationSupport.fromItem(
            item,
            SqlParseDiagnosticSupport.fromFailureReason(item.getFailureReason()),
            locationIssueScenes
        ));
        vo.setLogicalObjectKeys(new ArrayList<String>(item.getLogicalObjectKeys()));
        return vo;
    }

    private SqlIssueAssessment assessIssueScenes(List<String> issueScenes) {
        if (issueScenes == null || issueScenes.isEmpty()) {
            return new SqlIssueAssessment(0, 0, StructureParsePriorityLevel.P4.name(), false, false);
        }
        int highestScore = 0;
        String highestLevel = StructureParsePriorityLevel.P4.name();
        boolean important = false;
        boolean urgent = false;
        for (String scene : issueScenes) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            if (snapshot.getPriorityScore() > highestScore) {
                highestScore = snapshot.getPriorityScore();
                highestLevel = snapshot.getPriorityLevel().name();
            }
            important = important || snapshot.isImportant();
            urgent = urgent || snapshot.isUrgent();
        }
        return new SqlIssueAssessment(issueScenes.size(), highestScore, highestLevel, important, urgent);
    }

    private StructureParseIssueScoringSnapshot snapshot(String issueScene) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene(issueScene);
        return StructureParsePriorityScorer.snapshot(issue);
    }

    private Map<String, Integer> initialPriorityDistribution() {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (StructureParsePriorityLevel level : StructureParsePriorityLevel.values()) {
            result.put(level.name(), Integer.valueOf(0));
        }
        return result;
    }

    private Map<String, Integer> initialSeverityDistribution() {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (StructureParseIssueSeverity severity : StructureParseIssueSeverity.values()) {
            result.put(severity.name(), Integer.valueOf(0));
        }
        return result;
    }

    private String digest(String sqlText) {
        if (!StringUtils.hasText(sqlText)) {
            return null;
        }
        String normalized = sqlText.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120);
    }

    private String urgencyBucket(boolean important, boolean urgent) {
        if (important && urgent) {
            return "IMPORTANT_URGENT";
        }
        if (important) {
            return "IMPORTANT";
        }
        if (urgent) {
            return "URGENT";
        }
        return "NORMAL";
    }

    private int issueCount(List<String> issueScenes, String issueScene) {
        if (issueScenes == null || !StringUtils.hasText(issueScene)) {
            return 0;
        }
        int count = 0;
        for (String itemIssueScene : issueScenes) {
            if (issueScene.equals(itemIssueScene)) {
                count++;
            }
        }
        return count;
    }

    private boolean matchesReportCode(ReportBatchItem item, String reportCode) {
        return !StringUtils.hasText(reportCode) || reportCode.equals(item == null ? null : item.getReportCode());
    }

    private boolean matchesLogicalObject(ReportBatchItem item, String logicalObjectKey) {
        return !StringUtils.hasText(logicalObjectKey)
            || (item != null && item.getLogicalObjectKeys().contains(logicalObjectKey));
    }

    private List<String> sampleValues(Set<String> values) {
        List<String> result = new ArrayList<String>();
        if (values == null || values.isEmpty()) {
            return result;
        }
        for (String value : values) {
            if (result.size() >= 5) {
                break;
            }
            result.add(value);
        }
        return result;
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
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private MergeCandidateIndex buildMergeCandidateIndex(List<ReportBatchItem> items) {
        MergeCandidateIndex index = new MergeCandidateIndex();
        Map<String, List<ReportBatchItem>> groups = new LinkedHashMap<String, List<ReportBatchItem>>();
        for (ReportBatchItem item : items) {
            if (!isMergeCandidateEligible(item)) {
                continue;
            }
            String reportCode = firstNonBlank(item.getReportCode(), "UNSPECIFIED_REPORT");
            String groupKey = reportCode
                + "|"
                + firstNonBlank(item.getDatasourceCode(), "UNSPECIFIED_DATASOURCE")
                + "|"
                + firstNonBlank(item.getStage(), "UNSPECIFIED_STAGE");
            List<ReportBatchItem> groupItems = groups.get(groupKey);
            if (groupItems == null) {
                groupItems = new ArrayList<ReportBatchItem>();
                groups.put(groupKey, groupItems);
            }
            groupItems.add(item);
        }
        for (List<ReportBatchItem> groupItems : groups.values()) {
            if (groupItems.size() < 2) {
                continue;
            }
            MergeCandidateEvidence evidence = logicalObjectMergeEvidence(groupItems);
            if (evidence == null) {
                evidence = issueSceneMergeEvidence(groupItems);
            }
            if (evidence != null) {
                String reportCode = firstNonBlank(groupItems.get(0).getReportCode(), "UNSPECIFIED_REPORT");
                index.record(reportCode, evidence.itemIds, evidence.reason);
            }
        }
        return index;
    }

    private boolean isMergeCandidateEligible(ReportBatchItem item) {
        return item != null
            && StringUtils.hasText(item.getItemId())
            && StringUtils.hasText(item.getSqlText())
            && VALID_STRUCTURE_STATUS.equalsIgnoreCase(firstNonBlank(item.getStructureSyntaxStatus(), ""));
    }

    private MergeCandidateEvidence logicalObjectMergeEvidence(List<ReportBatchItem> items) {
        Map<String, Set<String>> objectItemIds = new LinkedHashMap<String, Set<String>>();
        for (ReportBatchItem item : items) {
            Set<String> uniqueObjects = new LinkedHashSet<String>(item.getLogicalObjectKeys());
            for (String objectKey : uniqueObjects) {
                if (!StringUtils.hasText(objectKey)) {
                    continue;
                }
                Set<String> itemIds = objectItemIds.get(objectKey);
                if (itemIds == null) {
                    itemIds = new LinkedHashSet<String>();
                    objectItemIds.put(objectKey, itemIds);
                }
                itemIds.add(item.getItemId());
            }
        }
        String bestObjectKey = null;
        Set<String> bestItemIds = Collections.emptySet();
        for (Map.Entry<String, Set<String>> entry : objectItemIds.entrySet()) {
            if (entry.getValue().size() > bestItemIds.size()) {
                bestObjectKey = entry.getKey();
                bestItemIds = entry.getValue();
            }
        }
        if (bestItemIds.size() < 2) {
            return null;
        }
        return new MergeCandidateEvidence(
            bestItemIds,
            mergeReason(items, bestItemIds.size(), "logical object " + bestObjectKey)
        );
    }

    private MergeCandidateEvidence issueSceneMergeEvidence(List<ReportBatchItem> items) {
        Map<String, Set<String>> sceneItemIds = new LinkedHashMap<String, Set<String>>();
        for (ReportBatchItem item : items) {
            Set<String> uniqueScenes = new LinkedHashSet<String>(item.getIssueScenes());
            for (String issueScene : uniqueScenes) {
                if (!isMergeCandidateIssueScene(issueScene)) {
                    continue;
                }
                Set<String> itemIds = sceneItemIds.get(issueScene);
                if (itemIds == null) {
                    itemIds = new LinkedHashSet<String>();
                    sceneItemIds.put(issueScene, itemIds);
                }
                itemIds.add(item.getItemId());
            }
        }
        List<String> sharedScenes = new ArrayList<String>();
        Set<String> candidateItemIds = new LinkedHashSet<String>();
        for (Map.Entry<String, Set<String>> entry : sceneItemIds.entrySet()) {
            if (entry.getValue().size() >= 2) {
                sharedScenes.add(entry.getKey());
                candidateItemIds.addAll(entry.getValue());
            }
        }
        if (sharedScenes.size() < MIN_SHARED_ISSUE_SCENES_FOR_MERGE || candidateItemIds.size() < 2) {
            return null;
        }
        return new MergeCandidateEvidence(
            candidateItemIds,
            mergeReason(items, candidateItemIds.size(), "shared issue scenes " + sharedScenes)
        );
    }

    private boolean isMergeCandidateIssueScene(String issueScene) {
        return StringUtils.hasText(issueScene)
            && !REPORT_SQL_MERGE_CANDIDATE.equals(issueScene)
            && !"SQL_SYNTAX_INVALID".equals(issueScene)
            && !"PARSER_FAILURE".equals(issueScene);
    }

    private String mergeReason(List<ReportBatchItem> items, int sqlCount, String evidence) {
        ReportBatchItem sample = items.get(0);
        String reportCode = firstNonBlank(sample.getReportCode(), "UNSPECIFIED_REPORT");
        String datasourceCode = firstNonBlank(sample.getDatasourceCode(), "UNSPECIFIED_DATASOURCE");
        String stage = firstNonBlank(sample.getStage(), "UNSPECIFIED_STAGE");
        return "Report " + reportCode + " has " + sqlCount + " SQL rows on datasource "
            + datasourceCode + " stage " + stage + " sharing " + evidence
            + "; review whether they can be merged into one query, shared CTE, or serving dataset.";
    }

    private List<String> effectiveIssueScenes(ReportBatchItem item, MergeCandidateIndex index) {
        List<String> result = new ArrayList<String>();
        if (item != null) {
            result.addAll(item.getIssueScenes());
        }
        if (index.isCandidateItem(item) && !result.contains(REPORT_SQL_MERGE_CANDIDATE)) {
            result.add(REPORT_SQL_MERGE_CANDIDATE);
        }
        return result;
    }

    private static final class SceneAccumulator {
        private final String issueScene;
        private final StructureParseIssueScoringSnapshot snapshot;
        private int affectedSqlCount;
        private int affectedIssueCount;
        private final Set<String> reportCodes = new LinkedHashSet<String>();
        private final Set<String> logicalObjectKeys = new LinkedHashSet<String>();

        private SceneAccumulator(String issueScene, StructureParseIssueScoringSnapshot snapshot) {
            this.issueScene = issueScene;
            this.snapshot = snapshot;
        }
    }

    private static final class SqlIssueAssessment {
        private final int issueCount;
        private final int highestPriorityScore;
        private final String highestPriorityLevel;
        private final boolean important;
        private final boolean urgent;

        private SqlIssueAssessment(int issueCount,
                                   int highestPriorityScore,
                                   String highestPriorityLevel,
                                   boolean important,
                                   boolean urgent) {
            this.issueCount = issueCount;
            this.highestPriorityScore = highestPriorityScore;
            this.highestPriorityLevel = highestPriorityLevel;
            this.important = important;
            this.urgent = urgent;
        }
    }

    private static final class ReportAccumulator {
        private final String reportCode;
        private int sqlCount;
        private int issueSqlCount;
        private int issueCount;
        private int highestPriorityScore;
        private String highestPriorityLevel = StructureParsePriorityLevel.P4.name();
        private boolean important;
        private boolean urgent;
        private boolean mergeCandidate;
        private int mergeCandidateSqlCount;
        private String mergeCandidateReason;
        private final Set<String> issueScenes = new LinkedHashSet<String>();

        private ReportAccumulator(String reportCode) {
            this.reportCode = reportCode;
        }
    }

    private static final class MergeCandidateIndex {
        private final Map<String, MergeCandidateAssessment> reportAssessments =
            new LinkedHashMap<String, MergeCandidateAssessment>();
        private final Set<String> candidateItemIds = new LinkedHashSet<String>();

        private void record(String reportCode, Set<String> itemIds, String reason) {
            if (!StringUtils.hasText(reportCode) || itemIds == null || itemIds.isEmpty()) {
                return;
            }
            MergeCandidateAssessment assessment = reportAssessments.get(reportCode);
            if (assessment == null) {
                assessment = new MergeCandidateAssessment(reason);
                reportAssessments.put(reportCode, assessment);
            }
            assessment.itemIds.addAll(itemIds);
            candidateItemIds.addAll(itemIds);
            if (!StringUtils.hasText(assessment.reason) && StringUtils.hasText(reason)) {
                assessment.reason = reason;
            }
        }

        private boolean isCandidateItem(ReportBatchItem item) {
            return item != null && candidateItemIds.contains(item.getItemId());
        }

        private MergeCandidateAssessment assessmentFor(String reportCode) {
            return reportAssessments.get(reportCode);
        }

        private int reportCount() {
            return reportAssessments.size();
        }
    }

    private static final class MergeCandidateAssessment {
        private final Set<String> itemIds = new LinkedHashSet<String>();
        private String reason;

        private MergeCandidateAssessment(String reason) {
            this.reason = reason;
        }

        private int sqlCount() {
            return itemIds.size();
        }
    }

    private static final class MergeCandidateEvidence {
        private final Set<String> itemIds;
        private final String reason;

        private MergeCandidateEvidence(Set<String> itemIds, String reason) {
            this.itemIds = new LinkedHashSet<String>(itemIds);
            this.reason = reason;
        }
    }

    private static final class MatrixAccumulator {
        private final String priorityLevel;
        private final String urgencyBucket;
        private int sqlCount;
        private int issueCount;
        private final Set<String> reportCodes = new LinkedHashSet<String>();

        private MatrixAccumulator(String priorityLevel, String urgencyBucket) {
            this.priorityLevel = priorityLevel;
            this.urgencyBucket = urgencyBucket;
        }
    }

    private static final class ImportanceAccumulator {
        private final String importanceBucket;
        private int sqlCount;
        private int issueCount;
        private final Set<String> reportCodes = new LinkedHashSet<String>();

        private ImportanceAccumulator(String importanceBucket) {
            this.importanceBucket = importanceBucket;
        }
    }

    private static final class LogicalObjectAccumulator {
        private final String objectKey;
        private int sqlCount;
        private int issueCount;
        private final Set<String> reportCodes = new LinkedHashSet<String>();

        private LogicalObjectAccumulator(String objectKey) {
            this.objectKey = objectKey;
        }
    }

    private static final class IssueSceneReportAccumulator {
        private final String reportCode;
        private final String reportName;
        private int sqlCount;
        private int issueCount;
        private final Set<String> logicalObjectKeys = new LinkedHashSet<String>();

        private IssueSceneReportAccumulator(String reportCode, String reportName) {
            this.reportCode = reportCode;
            this.reportName = reportName;
        }
    }

    private static final class SqlStatisticPage {
        private static final int MAX_PAGE_SIZE = 500;

        private final int pageNumber;
        private final int pageSize;
        private final String reportCode;

        private SqlStatisticPage(int pageNumber, int pageSize, String reportCode) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.reportCode = reportCode;
        }

        private static SqlStatisticPage from(Integer pageNumber,
                                             Integer pageSize,
                                             String reportCode,
                                             int defaultPageSize) {
            int normalizedPageNumber = pageNumber == null ? 1 : Math.max(1, pageNumber.intValue());
            int normalizedPageSize = pageSize == null ? defaultPageSize : pageSize.intValue();
            normalizedPageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, normalizedPageSize));
            String normalizedReportCode = StringUtils.hasText(reportCode) ? reportCode.trim() : null;
            return new SqlStatisticPage(normalizedPageNumber, normalizedPageSize, normalizedReportCode);
        }
    }
}
