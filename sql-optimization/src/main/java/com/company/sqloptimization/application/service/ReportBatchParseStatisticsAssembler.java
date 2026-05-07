package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
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
            SqlIssueAssessment assessment = assessItem(item);
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
            sqlStatistics.add(toSqlStatistic(item, assessment));
            accumulateScenes(item, items.size(), sceneAccumulators, severityDistribution, issueSceneKeys);
            accumulateReport(item, assessment, reportAccumulators);
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
                                  int totalSqlCount,
                                  Map<String, SceneAccumulator> accumulators,
                                  Map<String, Integer> severityDistribution,
                                  Set<String> issueSceneKeys) {
        Set<String> uniqueScenes = new LinkedHashSet<String>(item.getIssueScenes());
        for (String scene : uniqueScenes) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            issueSceneKeys.add(snapshot.getIssueScene());
            SceneAccumulator accumulator = accumulators.get(snapshot.getIssueScene());
            if (accumulator == null) {
                accumulator = new SceneAccumulator(snapshot);
                accumulators.put(snapshot.getIssueScene(), accumulator);
            }
            accumulator.affectedSqlCount++;
        }
        for (String scene : item.getIssueScenes()) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            SceneAccumulator accumulator = accumulators.get(snapshot.getIssueScene());
            if (accumulator != null) {
                accumulator.affectedIssueCount++;
            }
            String severity = snapshot.getSeverity().name();
            severityDistribution.put(severity, Integer.valueOf(severityDistribution.get(severity).intValue() + 1));
        }
    }

    private void accumulateReport(ReportBatchItem item,
                                  SqlIssueAssessment assessment,
                                  Map<String, ReportAccumulator> accumulators) {
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
        accumulator.issueScenes.addAll(item.getIssueScenes());
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
            vo.setIssueScene(accumulator.snapshot.getIssueScene());
            vo.setIssueDomain(accumulator.snapshot.getIssueDomain().name());
            vo.setSeverity(accumulator.snapshot.getSeverity().name());
            vo.setPriorityLevel(accumulator.snapshot.getPriorityLevel().name());
            vo.setPriorityScore(Integer.valueOf(accumulator.snapshot.getPriorityScore()));
            vo.setAffectedSqlCount(Integer.valueOf(accumulator.affectedSqlCount));
            vo.setAffectedIssueCount(Integer.valueOf(accumulator.affectedIssueCount));
            vo.setSqlRatio(Double.valueOf(totalSqlCount == 0 ? 0D : (double) accumulator.affectedSqlCount / totalSqlCount));
            vo.setImportant(Boolean.valueOf(accumulator.snapshot.isImportant()));
            vo.setUrgent(Boolean.valueOf(accumulator.snapshot.isUrgent()));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ParseIssueSceneStatisticVO::getAffectedSqlCount, Comparator.reverseOrder())
            .thenComparing(ParseIssueSceneStatisticVO::getPriorityScore, Comparator.reverseOrder())
            .thenComparing(ParseIssueSceneStatisticVO::getIssueScene));
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
        if (statistics.isEmpty()) {
            return Collections.emptyList();
        }
        int start = Math.min(statistics.size(), (pageSelection.pageNumber - 1) * pageSelection.pageSize);
        int end = Math.min(statistics.size(), start + pageSelection.pageSize);
        return new ArrayList<ReportBatchSqlStatisticVO>(statistics.subList(start, end));
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

    private ReportBatchSqlStatisticVO toSqlStatistic(ReportBatchItem item, SqlIssueAssessment assessment) {
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
        vo.setIssueCount(Integer.valueOf(assessment.issueCount));
        vo.setHighestPriorityLevel(assessment.highestPriorityLevel);
        vo.setHighestPriorityScore(Integer.valueOf(assessment.highestPriorityScore));
        vo.setImportant(Boolean.valueOf(assessment.important));
        vo.setUrgent(Boolean.valueOf(assessment.urgent));
        vo.setIssueScenes(new ArrayList<String>(item.getIssueScenes()));
        vo.setIssueLocations(ReportBatchIssueLocationSupport.fromItem(
            item,
            SqlParseDiagnosticSupport.fromFailureReason(item.getFailureReason())
        ));
        vo.setLogicalObjectKeys(new ArrayList<String>(item.getLogicalObjectKeys()));
        return vo;
    }

    private SqlIssueAssessment assessItem(ReportBatchItem item) {
        if (item == null || item.getIssueScenes().isEmpty()) {
            return new SqlIssueAssessment(0, 0, StructureParsePriorityLevel.P4.name(), false, false);
        }
        int highestScore = 0;
        String highestLevel = StructureParsePriorityLevel.P4.name();
        boolean important = false;
        boolean urgent = false;
        for (String scene : item.getIssueScenes()) {
            StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
            if (snapshot.getPriorityScore() > highestScore) {
                highestScore = snapshot.getPriorityScore();
                highestLevel = snapshot.getPriorityLevel().name();
            }
            important = important || snapshot.isImportant();
            urgent = urgent || snapshot.isUrgent();
        }
        return new SqlIssueAssessment(item.getIssueScenes().size(), highestScore, highestLevel, important, urgent);
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

    private static final class SceneAccumulator {
        private final StructureParseIssueScoringSnapshot snapshot;
        private int affectedSqlCount;
        private int affectedIssueCount;

        private SceneAccumulator(StructureParseIssueScoringSnapshot snapshot) {
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
        private final Set<String> issueScenes = new LinkedHashSet<String>();

        private ReportAccumulator(String reportCode) {
            this.reportCode = reportCode;
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
