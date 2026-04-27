package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
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
import java.util.Collections;
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

    private List<ParseBatchItem> tenantItems() {
        String tenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new BizException(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, HttpStatus.UNAUTHORIZED,
                "tenantId is missing from authenticated request context");
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
}
