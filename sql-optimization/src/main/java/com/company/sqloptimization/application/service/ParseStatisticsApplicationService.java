package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseSqlIssueStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.repository.ParseBatchItemRepository;
import com.company.sqloptimization.domain.batch.repository.ParseBatchRepository;
import com.company.sqloptimization.domain.parse.StructureParseIssue;
import com.company.sqloptimization.domain.parse.StructureParseIssueScoringSnapshot;
import com.company.sqloptimization.domain.parse.StructureParsePriorityLevel;
import com.company.sqloptimization.domain.parse.StructureParsePriorityScorer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ParseStatisticsApplicationService {

    private final ParseBatchRepository parseBatchRepository;
    private final ParseBatchItemRepository parseBatchItemRepository;

    public ParseStatisticsApplicationService(ParseBatchRepository parseBatchRepository,
                                             ParseBatchItemRepository parseBatchItemRepository) {
        this.parseBatchRepository = parseBatchRepository;
        this.parseBatchItemRepository = parseBatchItemRepository;
    }

    public ParseStatisticsOverviewVO overview() {
        List<ParseBatchItem> items = tenantItems();
        ParseStatisticsOverviewVO overview = new ParseStatisticsOverviewVO();
        overview.setTotalSqlCount(Integer.valueOf(items.size()));
        int issueSqlCount = 0;
        int totalIssueCount = 0;
        int importantSqlCount = 0;
        int urgentSqlCount = 0;
        Set<String> issueScenes = new LinkedHashSet<String>();
        Map<String, Integer> priorityDistribution = initialPriorityDistribution();
        for (ParseBatchItem item : items) {
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
            issueScenes.addAll(item.getIssueScenes());
            String level = assessment.highestPriorityLevel == null ? StructureParsePriorityLevel.P4.name() : assessment.highestPriorityLevel;
            priorityDistribution.put(level, Integer.valueOf(priorityDistribution.get(level).intValue() + 1));
        }
        overview.setIssueSqlCount(Integer.valueOf(issueSqlCount));
        overview.setTotalIssueCount(Integer.valueOf(totalIssueCount));
        overview.setIssueSceneCount(Integer.valueOf(issueScenes.size()));
        overview.setImportantSqlCount(Integer.valueOf(importantSqlCount));
        overview.setUrgentSqlCount(Integer.valueOf(urgentSqlCount));
        overview.setPriorityDistribution(priorityDistribution);
        return overview;
    }

    public List<ParseIssueSceneStatisticVO> byIssueScene() {
        List<ParseBatchItem> items = tenantItems();
        Map<String, SceneAccumulator> accumulators = new LinkedHashMap<String, SceneAccumulator>();
        for (ParseBatchItem item : items) {
            Set<String> uniqueScenes = new LinkedHashSet<String>(item.getIssueScenes());
            for (String scene : uniqueScenes) {
                StructureParseIssueScoringSnapshot snapshot = snapshot(scene);
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
            }
        }
        List<ParseIssueSceneStatisticVO> result = new ArrayList<ParseIssueSceneStatisticVO>();
        for (SceneAccumulator accumulator : accumulators.values()) {
            ParseIssueSceneStatisticVO vo = new ParseIssueSceneStatisticVO();
            vo.setIssueScene(accumulator.snapshot.getIssueScene());
            vo.setIssueDomain(accumulator.snapshot.getIssueDomain().name());
            vo.setSeverity(accumulator.snapshot.getSeverity().name());
            vo.setPriorityLevel(accumulator.snapshot.getPriorityLevel().name());
            vo.setPriorityScore(Integer.valueOf(accumulator.snapshot.getPriorityScore()));
            vo.setAffectedSqlCount(Integer.valueOf(accumulator.affectedSqlCount));
            vo.setAffectedIssueCount(Integer.valueOf(accumulator.affectedIssueCount));
            vo.setSqlRatio(Double.valueOf(items.isEmpty() ? 0D : (double) accumulator.affectedSqlCount / items.size()));
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

    public List<ParseSqlIssueStatisticVO> bySql() {
        List<ParseBatchItem> items = tenantItems();
        List<ParseSqlIssueStatisticVO> result = new ArrayList<ParseSqlIssueStatisticVO>(items.size());
        for (ParseBatchItem item : items) {
            SqlIssueAssessment assessment = assessItem(item);
            ParseSqlIssueStatisticVO vo = new ParseSqlIssueStatisticVO();
            vo.setItemId(item.getItemId());
            vo.setBatchId(item.getBatchId());
            vo.setParseTaskId(item.getParseTaskId());
            vo.setReportCode(item.getReportCode());
            vo.setDatasourceCode(item.getDatasourceCode());
            vo.setStage(item.getStage());
            vo.setSqlDigest(digest(item.getSqlText()));
            vo.setIssueCount(Integer.valueOf(assessment.issueCount));
            vo.setHighestPriorityLevel(assessment.highestPriorityLevel);
            vo.setHighestPriorityScore(Integer.valueOf(assessment.highestPriorityScore));
            vo.setImportant(Boolean.valueOf(assessment.important));
            vo.setUrgent(Boolean.valueOf(assessment.urgent));
            vo.setIssueScenes(new ArrayList<String>(item.getIssueScenes()));
            result.add(vo);
        }
        result.sort(Comparator
            .comparing(ParseSqlIssueStatisticVO::getHighestPriorityScore, Comparator.reverseOrder())
            .thenComparing(ParseSqlIssueStatisticVO::getIssueCount, Comparator.reverseOrder())
            .thenComparing(ParseSqlIssueStatisticVO::getItemId));
        return result;
    }

    public List<ParseReportStatisticVO> byReport() {
        List<ParseBatchItem> items = tenantItems();
        Map<String, ReportAccumulator> accumulators = new LinkedHashMap<String, ReportAccumulator>();
        for (ParseBatchItem item : items) {
            String reportCode = StringUtils.hasText(item.getReportCode()) ? item.getReportCode() : "UNSPECIFIED_REPORT";
            ReportAccumulator accumulator = accumulators.get(reportCode);
            if (accumulator == null) {
                accumulator = new ReportAccumulator(reportCode);
                accumulators.put(reportCode, accumulator);
            }
            accumulator.sqlCount++;
            SqlIssueAssessment assessment = assessItem(item);
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

    public List<ParsePriorityMatrixCellVO> priorityMatrix() {
        Map<String, MatrixAccumulator> matrix = new LinkedHashMap<String, MatrixAccumulator>();
        for (ParseBatchItem item : tenantItems()) {
            SqlIssueAssessment assessment = assessItem(item);
            String priorityLevel = assessment.highestPriorityLevel == null
                ? StructureParsePriorityLevel.P4.name()
                : assessment.highestPriorityLevel;
            String bucket = urgencyBucket(assessment.important, assessment.urgent);
            String key = priorityLevel + "|" + bucket;
            MatrixAccumulator accumulator = matrix.get(key);
            if (accumulator == null) {
                accumulator = new MatrixAccumulator(priorityLevel, bucket);
                matrix.put(key, accumulator);
            }
            accumulator.sqlCount++;
            accumulator.issueCount += assessment.issueCount;
            if (StringUtils.hasText(item.getReportCode())) {
                accumulator.reportCodes.add(item.getReportCode());
            }
        }
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

    public List<ParseSqlIssueStatisticVO> importantUrgentList() {
        List<ParseSqlIssueStatisticVO> result = new ArrayList<ParseSqlIssueStatisticVO>();
        for (ParseSqlIssueStatisticVO sqlStatistic : bySql()) {
            if (Boolean.TRUE.equals(sqlStatistic.getImportant()) || Boolean.TRUE.equals(sqlStatistic.getUrgent())) {
                result.add(sqlStatistic);
            }
        }
        return result;
    }

    private List<ParseBatchItem> tenantItems() {
        String tenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "已认证请求上下文缺少 tenantId");
        }
        List<ParseBatchItem> result = new ArrayList<ParseBatchItem>();
        for (ParseBatchItem item : parseBatchItemRepository.findAll()) {
            ParseBatch batch = parseBatchRepository.findByBatchId(item.getBatchId());
            if (batch != null && tenantId.equals(batch.getTenantId())) {
                result.add(item);
            }
        }
        return result;
    }

    private SqlIssueAssessment assessItem(ParseBatchItem item) {
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
}
