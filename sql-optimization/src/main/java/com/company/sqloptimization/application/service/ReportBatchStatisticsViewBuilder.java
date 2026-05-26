package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneLogicalObjectDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneReportDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchSqlStatisticVO;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueScoringSnapshot;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

public final class ReportBatchStatisticsViewBuilder {

    private static final int SQL_STATISTIC_PREVIEW_LIMIT = 500;
    private static final int ISSUE_SCENE_DETAIL_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 500;

    private ReportBatchStatisticsViewBuilder() {
    }

    public static ReportBatchParseStatisticsVO pageStatistics(ReportBatchParseStatisticsVO complete,
                                                              Integer pageNumber,
                                                              Integer pageSize,
                                                              String reportCode) {
        if (complete == null) {
            return null;
        }
        PageSelection pageSelection = PageSelection.from(pageNumber, pageSize, reportCode, SQL_STATISTIC_PREVIEW_LIMIT);
        List<ReportBatchSqlStatisticVO> sorted = sortSqlStatistics(complete.getSqlStatistics());
        List<ReportBatchSqlStatisticVO> filtered = filterSqlStatistics(sorted, pageSelection.reportCode);
        List<ReportBatchSqlStatisticVO> pageItems = pageItems(filtered, pageSelection);
        ReportBatchParseStatisticsVO result = new ReportBatchParseStatisticsVO();
        result.setOverview(complete.getOverview());
        result.setIssueSceneStatistics(complete.getIssueSceneStatistics());
        result.setSeverityDistribution(complete.getSeverityDistribution());
        result.setImportanceStatistics(complete.getImportanceStatistics());
        result.setReportStatistics(complete.getReportStatistics());
        result.setMergeCandidateReportCount(complete.getMergeCandidateReportCount());
        result.setSqlStatisticLimit(Integer.valueOf(pageSelection.pageSize));
        result.setSqlStatisticTruncated(Boolean.valueOf(filtered.size() > pageItems.size()));
        result.setOmittedSqlStatisticCount(Integer.valueOf(Math.max(0, filtered.size() - pageItems.size())));
        result.setSqlStatisticPageNumber(Integer.valueOf(pageSelection.pageNumber));
        result.setSqlStatisticPageSize(Integer.valueOf(pageSelection.pageSize));
        result.setSqlStatisticPageCount(Integer.valueOf(pageCount(filtered.size(), pageSelection.pageSize)));
        result.setSqlStatisticTotalCount(Integer.valueOf(filtered.size()));
        result.setSqlStatisticReportCodeFilter(pageSelection.reportCode);
        result.setSqlStatistics(pageItems);
        result.setPriorityMatrix(complete.getPriorityMatrix());
        result.setLogicalObjectStatistics(complete.getLogicalObjectStatistics());
        return result;
    }

    public static ReportBatchIssueSceneDetailVO issueSceneDetail(ReportBatchParseStatisticsVO complete,
                                                                 String issueScene,
                                                                 Integer pageNumber,
                                                                 Integer pageSize,
                                                                 Integer reportDetailPageNumber,
                                                                 Integer reportDetailPageSize,
                                                                 Integer logicalObjectDetailPageNumber,
                                                                 Integer logicalObjectDetailPageSize,
                                                                 String reportCode,
                                                                 String logicalObjectKey) {
        String normalizedIssueScene = trimToNull(issueScene);
        String reportCodeFilter = trimToNull(reportCode);
        String logicalObjectKeyFilter = trimToNull(logicalObjectKey);
        PageSelection sqlPage = PageSelection.from(pageNumber, pageSize, reportCodeFilter, SQL_STATISTIC_PREVIEW_LIMIT);
        PageSelection reportPage = PageSelection.from(reportDetailPageNumber, reportDetailPageSize, null, ISSUE_SCENE_DETAIL_PAGE_SIZE);
        PageSelection logicalObjectPage = PageSelection.from(
            logicalObjectDetailPageNumber,
            logicalObjectDetailPageSize,
            null,
            ISSUE_SCENE_DETAIL_PAGE_SIZE
        );
        List<ReportBatchSqlStatisticVO> sourceSqlStats = complete == null || complete.getSqlStatistics() == null
            ? Collections.<ReportBatchSqlStatisticVO>emptyList()
            : complete.getSqlStatistics();
        List<ReportBatchSqlStatisticVO> sqlStatistics = new ArrayList<ReportBatchSqlStatisticVO>();
        Map<String, IssueSceneReportAccumulator> reportAccumulators =
            new LinkedHashMap<String, IssueSceneReportAccumulator>();
        Map<String, LogicalObjectAccumulator> logicalObjectAccumulators =
            new LinkedHashMap<String, LogicalObjectAccumulator>();
        int affectedIssueCount = 0;
        for (ReportBatchSqlStatisticVO sqlStatistic : sourceSqlStats) {
            int issueCount = issueCount(sqlStatistic == null ? null : sqlStatistic.getIssueScenes(), normalizedIssueScene);
            if (issueCount <= 0 || !matchesReportCode(sqlStatistic, reportCodeFilter)
                || !matchesLogicalObject(sqlStatistic, logicalObjectKeyFilter)) {
                continue;
            }
            affectedIssueCount += issueCount;
            sqlStatistics.add(toIssueSceneSqlStatistic(sqlStatistic, normalizedIssueScene, issueCount));
            accumulateIssueSceneReport(sqlStatistic, issueCount, reportAccumulators);
            accumulateIssueSceneLogicalObjects(sqlStatistic, issueCount, logicalObjectAccumulators);
        }

        StructureParseIssueScoringSnapshot snapshot = snapshot(normalizedIssueScene);
        List<ReportBatchSqlStatisticVO> sortedSqlStatistics = sortSqlStatistics(sqlStatistics);
        List<ReportBatchSqlStatisticVO> pageSqlStatistics = pageItems(sortedSqlStatistics, sqlPage);
        List<ReportBatchIssueSceneReportDetailVO> reportDetails = toIssueSceneReportDetails(reportAccumulators);
        List<ReportBatchIssueSceneLogicalObjectDetailVO> logicalObjectDetails =
            toIssueSceneLogicalObjectDetails(logicalObjectAccumulators);
        List<ReportBatchIssueSceneReportDetailVO> pageReportDetails = pageItems(reportDetails, reportPage);
        List<ReportBatchIssueSceneLogicalObjectDetailVO> pageLogicalObjectDetails =
            pageItems(logicalObjectDetails, logicalObjectPage);

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
        detail.setReportDetailPageNumber(Integer.valueOf(reportPage.pageNumber));
        detail.setReportDetailPageSize(Integer.valueOf(reportPage.pageSize));
        detail.setReportDetailPageCount(Integer.valueOf(pageCount(reportDetails.size(), reportPage.pageSize)));
        detail.setReportDetailTotalCount(Integer.valueOf(reportDetails.size()));
        detail.setLogicalObjectDetailPageNumber(Integer.valueOf(logicalObjectPage.pageNumber));
        detail.setLogicalObjectDetailPageSize(Integer.valueOf(logicalObjectPage.pageSize));
        detail.setLogicalObjectDetailPageCount(Integer.valueOf(pageCount(logicalObjectDetails.size(), logicalObjectPage.pageSize)));
        detail.setLogicalObjectDetailTotalCount(Integer.valueOf(logicalObjectDetails.size()));
        detail.setSqlStatisticPageNumber(Integer.valueOf(sqlPage.pageNumber));
        detail.setSqlStatisticPageSize(Integer.valueOf(sqlPage.pageSize));
        detail.setSqlStatisticPageCount(Integer.valueOf(pageCount(sortedSqlStatistics.size(), sqlPage.pageSize)));
        detail.setSqlStatisticTotalCount(Integer.valueOf(sortedSqlStatistics.size()));
        detail.setReportDetails(pageReportDetails);
        detail.setLogicalObjectDetails(pageLogicalObjectDetails);
        detail.setSqlStatistics(pageSqlStatistics);
        return detail;
    }

    private static ReportBatchSqlStatisticVO toIssueSceneSqlStatistic(ReportBatchSqlStatisticVO source,
                                                                      String issueScene,
                                                                      int issueCount) {
        ReportBatchSqlStatisticVO result = copySqlStatistic(source);
        result.setIssueScenes(StringUtils.hasText(issueScene)
            ? Collections.singletonList(issueScene)
            : Collections.<String>emptyList());
        result.setIssueCount(Integer.valueOf(issueCount));
        return result;
    }

    public static ReportBatchSqlStatisticVO copySqlStatistic(ReportBatchSqlStatisticVO source) {
        ReportBatchSqlStatisticVO result = new ReportBatchSqlStatisticVO();
        if (source == null) {
            return result;
        }
        result.setItemId(source.getItemId());
        result.setBatchId(source.getBatchId());
        result.setParseTaskId(source.getParseTaskId());
        result.setReportCode(source.getReportCode());
        result.setReportName(source.getReportName());
        result.setDatasourceCode(source.getDatasourceCode());
        result.setStage(source.getStage());
        result.setSqlColumnName(source.getSqlColumnName());
        result.setSqlOrdinalInReport(source.getSqlOrdinalInReport());
        result.setStatus(source.getStatus());
        result.setSqlDigest(source.getSqlDigest());
        result.setIssueCount(source.getIssueCount());
        result.setHighestPriorityLevel(source.getHighestPriorityLevel());
        result.setHighestPriorityScore(source.getHighestPriorityScore());
        result.setImportant(source.getImportant());
        result.setUrgent(source.getUrgent());
        result.setIssueScenes(source.getIssueScenes() == null
            ? Collections.<String>emptyList()
            : new ArrayList<String>(source.getIssueScenes()));
        result.setIssueLocations(source.getIssueLocations() == null
            ? Collections.emptyList()
            : new ArrayList<com.company.sqloptimization.application.controller.vo.ReportBatchIssueLocationVO>(source.getIssueLocations()));
        result.setLogicalObjectKeys(source.getLogicalObjectKeys() == null
            ? Collections.<String>emptyList()
            : new ArrayList<String>(source.getLogicalObjectKeys()));
        return result;
    }

    private static void accumulateIssueSceneReport(ReportBatchSqlStatisticVO item,
                                                   int issueCount,
                                                   Map<String, IssueSceneReportAccumulator> accumulators) {
        String reportCode = firstNonBlank(item == null ? null : item.getReportCode(), "UNSPECIFIED_REPORT");
        IssueSceneReportAccumulator accumulator = accumulators.get(reportCode);
        if (accumulator == null) {
            accumulator = new IssueSceneReportAccumulator(reportCode, item == null ? null : item.getReportName());
            accumulators.put(reportCode, accumulator);
        }
        accumulator.sqlCount++;
        accumulator.issueCount += issueCount;
        if (item != null && item.getLogicalObjectKeys() != null) {
            accumulator.logicalObjectKeys.addAll(item.getLogicalObjectKeys());
        }
    }

    private static void accumulateIssueSceneLogicalObjects(ReportBatchSqlStatisticVO item,
                                                          int issueCount,
                                                          Map<String, LogicalObjectAccumulator> accumulators) {
        Set<String> uniqueObjectKeys = new LinkedHashSet<String>(item == null || item.getLogicalObjectKeys() == null
            ? Collections.<String>emptyList()
            : item.getLogicalObjectKeys());
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
            if (item != null && StringUtils.hasText(item.getReportCode())) {
                accumulator.reportCodes.add(item.getReportCode());
            }
        }
    }

    private static List<ReportBatchIssueSceneReportDetailVO> toIssueSceneReportDetails(
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

    private static List<ReportBatchIssueSceneLogicalObjectDetailVO> toIssueSceneLogicalObjectDetails(
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

    public static List<ReportBatchSqlStatisticVO> sortSqlStatistics(List<ReportBatchSqlStatisticVO> statistics) {
        List<ReportBatchSqlStatisticVO> result = statistics == null
            ? new ArrayList<ReportBatchSqlStatisticVO>()
            : new ArrayList<ReportBatchSqlStatisticVO>(statistics);
        result.sort(Comparator
            .comparing(ReportBatchSqlStatisticVO::getHighestPriorityScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ReportBatchSqlStatisticVO::getIssueCount, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ReportBatchSqlStatisticVO::getItemId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public static List<ReportBatchSqlStatisticVO> filterSqlStatistics(List<ReportBatchSqlStatisticVO> statistics,
                                                                      String reportCode) {
        if (!StringUtils.hasText(reportCode)) {
            return statistics == null ? Collections.<ReportBatchSqlStatisticVO>emptyList() : statistics;
        }
        List<ReportBatchSqlStatisticVO> result = new ArrayList<ReportBatchSqlStatisticVO>();
        if (statistics != null) {
            for (ReportBatchSqlStatisticVO statistic : statistics) {
                if (statistic != null && reportCode.equals(statistic.getReportCode())) {
                    result.add(statistic);
                }
            }
        }
        return result;
    }

    private static <T> List<T> pageItems(List<T> items, PageSelection pageSelection) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        int start = Math.min(items.size(), (pageSelection.pageNumber - 1) * pageSelection.pageSize);
        int end = Math.min(items.size(), start + pageSelection.pageSize);
        return new ArrayList<T>(items.subList(start, end));
    }

    private static int pageCount(int totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 0;
        }
        return (totalCount + pageSize - 1) / pageSize;
    }

    private static int issueCount(List<String> issueScenes, String issueScene) {
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

    private static boolean matchesReportCode(ReportBatchSqlStatisticVO item, String reportCode) {
        return !StringUtils.hasText(reportCode) || reportCode.equals(item == null ? null : item.getReportCode());
    }

    private static boolean matchesLogicalObject(ReportBatchSqlStatisticVO item, String logicalObjectKey) {
        return !StringUtils.hasText(logicalObjectKey)
            || (item != null && item.getLogicalObjectKeys() != null && item.getLogicalObjectKeys().contains(logicalObjectKey));
    }

    private static StructureParseIssueScoringSnapshot snapshot(String issueScene) {
        StructureParseIssue issue = new StructureParseIssue();
        issue.setIssueScene(issueScene);
        return StructureParsePriorityScorer.snapshot(issue);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String firstNonBlank(String... values) {
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

    private static final class LogicalObjectAccumulator {
        private final String objectKey;
        private int sqlCount;
        private int issueCount;
        private final Set<String> reportCodes = new LinkedHashSet<String>();

        private LogicalObjectAccumulator(String objectKey) {
            this.objectKey = objectKey;
        }
    }

    public static final class PageSelection {
        private final int pageNumber;
        private final int pageSize;
        private final String reportCode;

        private PageSelection(int pageNumber, int pageSize, String reportCode) {
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.reportCode = reportCode;
        }

        public static PageSelection from(Integer pageNumber,
                                         Integer pageSize,
                                         String reportCode,
                                         int defaultPageSize) {
            int normalizedPageNumber = pageNumber == null ? 1 : Math.max(1, pageNumber.intValue());
            int normalizedPageSize = pageSize == null ? defaultPageSize : pageSize.intValue();
            normalizedPageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, normalizedPageSize));
            String normalizedReportCode = StringUtils.hasText(reportCode) ? reportCode.trim() : null;
            return new PageSelection(normalizedPageNumber, normalizedPageSize, normalizedReportCode);
        }
    }
}
