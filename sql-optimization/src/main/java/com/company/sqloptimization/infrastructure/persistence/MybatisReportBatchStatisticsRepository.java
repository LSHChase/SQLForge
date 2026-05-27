package com.company.sqloptimization.infrastructure.persistence;

import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.vo.ParseBatchStageStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ParseIssueSceneStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParsePriorityMatrixCellVO;
import com.company.sqloptimization.application.controller.vo.ParseReportStatisticVO;
import com.company.sqloptimization.application.controller.vo.ParseStatisticsOverviewVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchImportanceStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueLocationVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchIssueSceneDetailVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchLogicalObjectStatisticVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchParseStatisticsVO;
import com.company.sqloptimization.application.controller.vo.ReportBatchSqlStatisticVO;
import com.company.sqloptimization.application.service.ReportBatchStatisticsViewBuilder;
import com.company.sqloptimization.domain.parse.StructureParsePriorityLevel;
import com.company.sqloptimization.domain.parse.StructureParseIssueSeverity;
import com.company.sqloptimization.domain.reportbatch.ReportBatch;
import com.company.sqloptimization.domain.reportbatch.ReportBatchStatisticsSummary;
import com.company.sqloptimization.domain.reportbatch.repository.ReportBatchStatisticsRepository;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.ImportanceRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.IssueSceneRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.LogicalObjectRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.PriorityRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.ReportRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SeverityRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlIssueSceneRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlLogicalObjectRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SqlRecord;
import com.company.sqloptimization.infrastructure.persistence.entity.ReportBatchStatisticsRecords.SummaryRecord;
import com.company.sqloptimization.infrastructure.persistence.mapper.ReportBatchStatisticsMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.company.sqlforge.common.utils.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Repository
@ConditionalOnProperty(prefix = "sql-optimization.report-batch", name = "repository", havingValue = "database")
public class MybatisReportBatchStatisticsRepository implements ReportBatchStatisticsRepository {
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };
    private static final TypeReference<List<ReportBatchIssueLocationVO>> LIST_OF_ISSUE_LOCATIONS =
        new TypeReference<List<ReportBatchIssueLocationVO>>() {
        };

    private final ReportBatchStatisticsMapper mapper;

    public MybatisReportBatchStatisticsRepository(ReportBatchStatisticsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void replaceStatistics(ReportBatch batch,
                                  ReportBatchParseStatisticsVO statistics,
                                  ParseBatchStageStatisticsVO planAnalysisStatistics,
                                  Instant occurredAt) {
        if (batch == null || statistics == null) {
            return;
        }
        LocalDateTime now = toLocalDateTime(occurredAt == null ? Instant.now() : occurredAt);
        deleteExistingStatistics(batch.getBatchId());
        mapper.insertSummary(toSummaryRecord(batch, statistics, planAnalysisStatistics, now));
        insertIfNotEmptyIssueScenes(toIssueSceneRecords(batch, statistics, now));
        insertIfNotEmptySeverities(toSeverityRecords(batch, statistics, now));
        insertIfNotEmptyReports(toReportRecords(batch, statistics, now));
        insertIfNotEmptySqlStats(toSqlRecords(batch, statistics, now));
        insertIfNotEmptySqlIssueScenes(toSqlIssueSceneRecords(batch, statistics, now));
        insertIfNotEmptySqlLogicalObjects(toSqlLogicalObjectRecords(batch, statistics, now));
        insertIfNotEmptyPriorities(toPriorityRecords(batch, statistics, now));
        insertIfNotEmptyImportances(toImportanceRecords(batch, statistics, now));
        insertIfNotEmptyLogicalObjects(toLogicalObjectRecords(batch, statistics, now));
    }

    private void deleteExistingStatistics(String batchId) {
        mapper.deleteSqlIssueScenesByBatchId(batchId);
        mapper.deleteSqlLogicalObjectsByBatchId(batchId);
        mapper.deleteSqlStatsByBatchId(batchId);
        mapper.deleteIssueScenesByBatchId(batchId);
        mapper.deleteSeveritiesByBatchId(batchId);
        mapper.deleteReportsByBatchId(batchId);
        mapper.deletePrioritiesByBatchId(batchId);
        mapper.deleteImportancesByBatchId(batchId);
        mapper.deleteLogicalObjectsByBatchId(batchId);
        mapper.deleteSummaryByBatchId(batchId);
    }

    @Override
    public ReportBatchStatisticsSummary findSummaryByBatchId(String batchId) {
        SummaryRecord record = mapper.selectSummaryByBatchId(batchId);
        return record == null ? null : toSummary(record);
    }

    @Override
    public Map<String, ReportBatchStatisticsSummary> findSummariesByBatchIds(List<String> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SummaryRecord> records = mapper.selectSummariesByBatchIds(batchIds);
        Map<String, ReportBatchStatisticsSummary> result = new LinkedHashMap<String, ReportBatchStatisticsSummary>();
        for (SummaryRecord record : records) {
            result.put(record.getBatchId(), toSummary(record));
        }
        return result;
    }

    @Override
    public ReportBatchParseStatisticsVO findParseStatistics(String batchId,
                                                           Integer pageNumber,
                                                           Integer pageSize,
                                                           String reportCode) {
        ReportBatchParseStatisticsVO complete = loadCompleteStatistics(batchId);
        return complete == null
            ? null
            : ReportBatchStatisticsViewBuilder.pageStatistics(complete, pageNumber, pageSize, reportCode);
    }

    @Override
    public ReportBatchIssueSceneDetailVO findIssueSceneDetail(String batchId,
                                                             String issueScene,
                                                             Integer pageNumber,
                                                             Integer pageSize,
                                                             Integer reportDetailPageNumber,
                                                             Integer reportDetailPageSize,
                                                             Integer logicalObjectDetailPageNumber,
                                                             Integer logicalObjectDetailPageSize,
                                                             String reportCode,
                                                             String logicalObjectKey) {
        ReportBatchParseStatisticsVO complete = loadCompleteStatistics(batchId);
        return complete == null
            ? null
            : ReportBatchStatisticsViewBuilder.issueSceneDetail(
                complete,
                issueScene,
                pageNumber,
                pageSize,
                reportDetailPageNumber,
                reportDetailPageSize,
                logicalObjectDetailPageNumber,
                logicalObjectDetailPageSize,
                reportCode,
                logicalObjectKey
            );
    }

    private ReportBatchParseStatisticsVO loadCompleteStatistics(String batchId) {
        SummaryRecord summary = mapper.selectSummaryByBatchId(batchId);
        if (summary == null) {
            return null;
        }
        List<PriorityRecord> priorityRecords = mapper.selectPrioritiesByBatchId(batchId);
        ReportBatchParseStatisticsVO statistics = new ReportBatchParseStatisticsVO();
        statistics.setOverview(toOverview(summary, priorityRecords));
        statistics.setIssueSceneStatistics(toIssueSceneVos(mapper.selectIssueScenesByBatchId(batchId)));
        statistics.setSeverityDistribution(toSeverityDistribution(mapper.selectSeveritiesByBatchId(batchId)));
        statistics.setImportanceStatistics(toImportanceVos(mapper.selectImportancesByBatchId(batchId)));
        statistics.setReportStatistics(toReportVos(mapper.selectReportsByBatchId(batchId)));
        statistics.setMergeCandidateReportCount(defaultInteger(summary.getMergeCandidateReportCount()));
        statistics.setSqlStatistics(toSqlStatisticVos(
            mapper.selectSqlStatsByBatchId(batchId),
            mapper.selectSqlIssueScenesByBatchId(batchId),
            mapper.selectSqlLogicalObjectsByBatchId(batchId)
        ));
        statistics.setPriorityMatrix(toPriorityVos(priorityRecords));
        statistics.setLogicalObjectStatistics(toLogicalObjectVos(mapper.selectLogicalObjectsByBatchId(batchId)));
        return statistics;
    }

    private SummaryRecord toSummaryRecord(ReportBatch batch,
                                          ReportBatchParseStatisticsVO statistics,
                                          ParseBatchStageStatisticsVO planAnalysisStatistics,
                                          LocalDateTime now) {
        int resolved = 0;
        int partial = 0;
        int failed = 0;
        if (statistics.getSqlStatistics() != null) {
            for (ReportBatchSqlStatisticVO statistic : statistics.getSqlStatistics()) {
                if ("RESOLVED".equals(statistic.getStatus())) {
                    resolved++;
                } else if ("PARTIAL_RESOLVED".equals(statistic.getStatus())) {
                    partial++;
                } else if ("FAILED".equals(statistic.getStatus())) {
                    failed++;
                }
            }
        }
        ParseStatisticsOverviewVO overview = statistics.getOverview();
        SummaryRecord record = new SummaryRecord();
        record.setBatchId(batch.getBatchId());
        record.setTenantId(batch.getTenantId());
        record.setTotalSqlCount(defaultInteger(overview == null ? null : overview.getTotalSqlCount()));
        record.setResolvedSqlCount(Integer.valueOf(resolved));
        record.setPartialResolvedSqlCount(Integer.valueOf(partial));
        record.setFailedSqlCount(Integer.valueOf(failed));
        record.setIssueSqlCount(defaultInteger(overview == null ? null : overview.getIssueSqlCount()));
        record.setTotalIssueCount(defaultInteger(overview == null ? null : overview.getTotalIssueCount()));
        record.setIssueSceneCount(defaultInteger(overview == null ? null : overview.getIssueSceneCount()));
        record.setImportantSqlCount(defaultInteger(overview == null ? null : overview.getImportantSqlCount()));
        record.setUrgentSqlCount(defaultInteger(overview == null ? null : overview.getUrgentSqlCount()));
        record.setMergeCandidateReportCount(defaultInteger(statistics.getMergeCandidateReportCount()));
        record.setPlanAnalysisSuccessCount(defaultInteger(planAnalysisStatistics == null ? null : planAnalysisStatistics.getSuccessRecords()));
        record.setPlanAnalysisPartialCount(defaultInteger(planAnalysisStatistics == null ? null : planAnalysisStatistics.getPartialSuccessRecords()));
        record.setPlanAnalysisFailedCount(defaultInteger(planAnalysisStatistics == null ? null : planAnalysisStatistics.getFailedRecords()));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private List<IssueSceneRecord> toIssueSceneRecords(ReportBatch batch,
                                                       ReportBatchParseStatisticsVO statistics,
                                                       LocalDateTime now) {
        List<ParseIssueSceneStatisticVO> source = statistics.getIssueSceneStatistics();
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<IssueSceneRecord> records = new ArrayList<IssueSceneRecord>(source.size());
        for (ParseIssueSceneStatisticVO vo : source) {
            IssueSceneRecord record = new IssueSceneRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setIssueScene(vo.getIssueScene());
            record.setIssueDomain(vo.getIssueDomain());
            record.setSeverity(vo.getSeverity());
            record.setPriorityLevel(vo.getPriorityLevel());
            record.setPriorityScore(vo.getPriorityScore());
            record.setAffectedSqlCount(vo.getAffectedSqlCount());
            record.setAffectedIssueCount(vo.getAffectedIssueCount());
            record.setSqlRatio(vo.getSqlRatio());
            record.setImportant(vo.getImportant());
            record.setUrgent(vo.getUrgent());
            record.setReportCount(vo.getReportCount());
            record.setLogicalObjectCount(vo.getLogicalObjectCount());
            record.setSampleReportCodesJson(JsonUtils.toJson(vo.getSampleReportCodes()));
            record.setSampleLogicalObjectKeysJson(JsonUtils.toJson(vo.getSampleLogicalObjectKeys()));
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<SeverityRecord> toSeverityRecords(ReportBatch batch,
                                                   ReportBatchParseStatisticsVO statistics,
                                                   LocalDateTime now) {
        if (statistics.getSeverityDistribution() == null || statistics.getSeverityDistribution().isEmpty()) {
            return Collections.emptyList();
        }
        List<SeverityRecord> records = new ArrayList<SeverityRecord>(statistics.getSeverityDistribution().size());
        for (Map.Entry<String, Integer> entry : statistics.getSeverityDistribution().entrySet()) {
            SeverityRecord record = new SeverityRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setSeverity(entry.getKey());
            record.setIssueCount(defaultInteger(entry.getValue()));
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<ReportRecord> toReportRecords(ReportBatch batch,
                                               ReportBatchParseStatisticsVO statistics,
                                               LocalDateTime now) {
        List<ParseReportStatisticVO> source = statistics.getReportStatistics();
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<ReportRecord> records = new ArrayList<ReportRecord>(source.size());
        for (ParseReportStatisticVO vo : source) {
            ReportRecord record = new ReportRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setReportCode(vo.getReportCode());
            record.setSqlCount(vo.getSqlCount());
            record.setIssueSqlCount(vo.getIssueSqlCount());
            record.setIssueCount(vo.getIssueCount());
            record.setIssueSqlRatio(vo.getIssueSqlRatio());
            record.setHighestPriorityLevel(vo.getHighestPriorityLevel());
            record.setHighestPriorityScore(vo.getHighestPriorityScore());
            record.setImportant(vo.getImportant());
            record.setUrgent(vo.getUrgent());
            record.setIssueScenesJson(JsonUtils.toJson(vo.getIssueScenes()));
            record.setMergeCandidate(vo.getMergeCandidate());
            record.setMergeCandidateSqlCount(vo.getMergeCandidateSqlCount());
            record.setMergeCandidateReason(vo.getMergeCandidateReason());
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<SqlRecord> toSqlRecords(ReportBatch batch,
                                         ReportBatchParseStatisticsVO statistics,
                                         LocalDateTime now) {
        List<ReportBatchSqlStatisticVO> source = statistics.getSqlStatistics();
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<SqlRecord> records = new ArrayList<SqlRecord>(source.size());
        for (ReportBatchSqlStatisticVO vo : source) {
            SqlRecord record = new SqlRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setItemId(vo.getItemId());
            record.setParseTaskId(vo.getParseTaskId());
            record.setReportCode(vo.getReportCode());
            record.setReportName(vo.getReportName());
            record.setDatasourceCode(vo.getDatasourceCode());
            record.setStage(vo.getStage());
            record.setSqlColumnName(vo.getSqlColumnName());
            record.setSqlOrdinalInReport(vo.getSqlOrdinalInReport());
            record.setStatus(vo.getStatus());
            record.setSqlDigest(vo.getSqlDigest());
            record.setIssueCount(vo.getIssueCount());
            record.setHighestPriorityLevel(vo.getHighestPriorityLevel());
            record.setHighestPriorityScore(vo.getHighestPriorityScore());
            record.setImportant(vo.getImportant());
            record.setUrgent(vo.getUrgent());
            record.setIssueLocationsJson(JsonUtils.toJson(vo.getIssueLocations()));
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<SqlIssueSceneRecord> toSqlIssueSceneRecords(ReportBatch batch,
                                                             ReportBatchParseStatisticsVO statistics,
                                                             LocalDateTime now) {
        List<SqlIssueSceneRecord> records = new ArrayList<SqlIssueSceneRecord>();
        if (statistics.getSqlStatistics() == null) {
            return records;
        }
        for (ReportBatchSqlStatisticVO vo : statistics.getSqlStatistics()) {
            if (vo.getIssueScenes() == null) {
                continue;
            }
            for (String issueScene : vo.getIssueScenes()) {
                if (!StringUtils.hasText(issueScene)) {
                    continue;
                }
                SqlIssueSceneRecord record = new SqlIssueSceneRecord();
                record.setBatchId(batch.getBatchId());
                record.setTenantId(batch.getTenantId());
                record.setItemId(vo.getItemId());
                record.setIssueScene(issueScene);
                record.setCreatedAt(now);
                records.add(record);
            }
        }
        return records;
    }

    private List<SqlLogicalObjectRecord> toSqlLogicalObjectRecords(ReportBatch batch,
                                                                   ReportBatchParseStatisticsVO statistics,
                                                                   LocalDateTime now) {
        List<SqlLogicalObjectRecord> records = new ArrayList<SqlLogicalObjectRecord>();
        if (statistics.getSqlStatistics() == null) {
            return records;
        }
        for (ReportBatchSqlStatisticVO vo : statistics.getSqlStatistics()) {
            if (vo.getLogicalObjectKeys() == null) {
                continue;
            }
            for (String logicalObjectKey : vo.getLogicalObjectKeys()) {
                if (!StringUtils.hasText(logicalObjectKey)) {
                    continue;
                }
                SqlLogicalObjectRecord record = new SqlLogicalObjectRecord();
                record.setBatchId(batch.getBatchId());
                record.setTenantId(batch.getTenantId());
                record.setItemId(vo.getItemId());
                record.setLogicalObjectKey(logicalObjectKey);
                record.setCreatedAt(now);
                records.add(record);
            }
        }
        return records;
    }

    private List<PriorityRecord> toPriorityRecords(ReportBatch batch,
                                                   ReportBatchParseStatisticsVO statistics,
                                                   LocalDateTime now) {
        if (statistics.getPriorityMatrix() == null || statistics.getPriorityMatrix().isEmpty()) {
            return Collections.emptyList();
        }
        List<PriorityRecord> records = new ArrayList<PriorityRecord>(statistics.getPriorityMatrix().size());
        for (ParsePriorityMatrixCellVO vo : statistics.getPriorityMatrix()) {
            PriorityRecord record = new PriorityRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setPriorityLevel(vo.getPriorityLevel());
            record.setUrgencyBucket(vo.getUrgencyBucket());
            record.setSqlCount(vo.getSqlCount());
            record.setIssueCount(vo.getIssueCount());
            record.setReportCount(vo.getReportCount());
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<ImportanceRecord> toImportanceRecords(ReportBatch batch,
                                                       ReportBatchParseStatisticsVO statistics,
                                                       LocalDateTime now) {
        if (statistics.getImportanceStatistics() == null || statistics.getImportanceStatistics().isEmpty()) {
            return Collections.emptyList();
        }
        List<ImportanceRecord> records = new ArrayList<ImportanceRecord>(statistics.getImportanceStatistics().size());
        for (ReportBatchImportanceStatisticVO vo : statistics.getImportanceStatistics()) {
            ImportanceRecord record = new ImportanceRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setImportanceBucket(vo.getImportanceBucket());
            record.setSqlCount(vo.getSqlCount());
            record.setIssueCount(vo.getIssueCount());
            record.setReportCount(vo.getReportCount());
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private List<LogicalObjectRecord> toLogicalObjectRecords(ReportBatch batch,
                                                             ReportBatchParseStatisticsVO statistics,
                                                             LocalDateTime now) {
        if (statistics.getLogicalObjectStatistics() == null || statistics.getLogicalObjectStatistics().isEmpty()) {
            return Collections.emptyList();
        }
        List<LogicalObjectRecord> records = new ArrayList<LogicalObjectRecord>(statistics.getLogicalObjectStatistics().size());
        for (ReportBatchLogicalObjectStatisticVO vo : statistics.getLogicalObjectStatistics()) {
            LogicalObjectRecord record = new LogicalObjectRecord();
            record.setBatchId(batch.getBatchId());
            record.setTenantId(batch.getTenantId());
            record.setLogicalObjectKey(vo.getObjectKey());
            record.setSqlCount(vo.getSqlCount());
            record.setIssueCount(vo.getIssueCount());
            record.setReportCount(vo.getReportCount());
            record.setReportCodesJson(JsonUtils.toJson(vo.getReportCodes()));
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            records.add(record);
        }
        return records;
    }

    private ReportBatchStatisticsSummary toSummary(SummaryRecord record) {
        return new ReportBatchStatisticsSummary(
            record.getBatchId(),
            record.getTenantId(),
            intValue(record.getTotalSqlCount()),
            intValue(record.getResolvedSqlCount()),
            intValue(record.getPartialResolvedSqlCount()),
            intValue(record.getFailedSqlCount()),
            intValue(record.getIssueSqlCount()),
            intValue(record.getTotalIssueCount()),
            intValue(record.getIssueSceneCount()),
            intValue(record.getImportantSqlCount()),
            intValue(record.getUrgentSqlCount()),
            intValue(record.getMergeCandidateReportCount()),
            intValue(record.getPlanAnalysisSuccessCount()),
            intValue(record.getPlanAnalysisPartialCount()),
            intValue(record.getPlanAnalysisFailedCount()),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt())
        );
    }

    private ParseStatisticsOverviewVO toOverview(SummaryRecord summary, List<PriorityRecord> priorities) {
        ParseStatisticsOverviewVO overview = new ParseStatisticsOverviewVO();
        overview.setTotalSqlCount(defaultInteger(summary.getTotalSqlCount()));
        overview.setIssueSqlCount(defaultInteger(summary.getIssueSqlCount()));
        overview.setTotalIssueCount(defaultInteger(summary.getTotalIssueCount()));
        overview.setIssueSceneCount(defaultInteger(summary.getIssueSceneCount()));
        overview.setImportantSqlCount(defaultInteger(summary.getImportantSqlCount()));
        overview.setUrgentSqlCount(defaultInteger(summary.getUrgentSqlCount()));
        Map<String, Integer> priorityDistribution = new LinkedHashMap<String, Integer>();
        for (StructureParsePriorityLevel level : StructureParsePriorityLevel.values()) {
            priorityDistribution.put(level.name(), Integer.valueOf(0));
        }
        if (priorities != null) {
            for (PriorityRecord record : priorities) {
                Integer current = priorityDistribution.get(record.getPriorityLevel());
                if (current != null) {
                    priorityDistribution.put(record.getPriorityLevel(), Integer.valueOf(current.intValue() + intValue(record.getSqlCount())));
                }
            }
        }
        overview.setPriorityDistribution(priorityDistribution);
        return overview;
    }

    private List<ParseIssueSceneStatisticVO> toIssueSceneVos(List<IssueSceneRecord> records) {
        List<ParseIssueSceneStatisticVO> result = new ArrayList<ParseIssueSceneStatisticVO>(records.size());
        for (IssueSceneRecord record : records) {
            ParseIssueSceneStatisticVO vo = new ParseIssueSceneStatisticVO();
            vo.setIssueScene(record.getIssueScene());
            vo.setIssueDomain(record.getIssueDomain());
            vo.setSeverity(record.getSeverity());
            vo.setPriorityLevel(record.getPriorityLevel());
            vo.setPriorityScore(record.getPriorityScore());
            vo.setAffectedSqlCount(record.getAffectedSqlCount());
            vo.setAffectedIssueCount(record.getAffectedIssueCount());
            vo.setSqlRatio(record.getSqlRatio());
            vo.setImportant(record.getImportant());
            vo.setUrgent(record.getUrgent());
            vo.setReportCount(record.getReportCount());
            vo.setLogicalObjectCount(record.getLogicalObjectCount());
            vo.setSampleReportCodes(readStringList(record.getSampleReportCodesJson()));
            vo.setSampleLogicalObjectKeys(readStringList(record.getSampleLogicalObjectKeysJson()));
            result.add(vo);
        }
        return result;
    }

    private Map<String, Integer> toSeverityDistribution(List<SeverityRecord> records) {
        Map<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (StructureParseIssueSeverity severity : StructureParseIssueSeverity.values()) {
            result.put(severity.name(), Integer.valueOf(0));
        }
        for (SeverityRecord record : records) {
            result.put(record.getSeverity(), defaultInteger(record.getIssueCount()));
        }
        return result;
    }

    private List<ReportBatchImportanceStatisticVO> toImportanceVos(List<ImportanceRecord> records) {
        List<ReportBatchImportanceStatisticVO> result = new ArrayList<ReportBatchImportanceStatisticVO>(records.size());
        for (ImportanceRecord record : records) {
            ReportBatchImportanceStatisticVO vo = new ReportBatchImportanceStatisticVO();
            vo.setImportanceBucket(record.getImportanceBucket());
            vo.setSqlCount(record.getSqlCount());
            vo.setIssueCount(record.getIssueCount());
            vo.setReportCount(record.getReportCount());
            result.add(vo);
        }
        return result;
    }

    private List<ParseReportStatisticVO> toReportVos(List<ReportRecord> records) {
        List<ParseReportStatisticVO> result = new ArrayList<ParseReportStatisticVO>(records.size());
        for (ReportRecord record : records) {
            ParseReportStatisticVO vo = new ParseReportStatisticVO();
            vo.setReportCode(record.getReportCode());
            vo.setSqlCount(record.getSqlCount());
            vo.setIssueSqlCount(record.getIssueSqlCount());
            vo.setIssueCount(record.getIssueCount());
            vo.setIssueSqlRatio(record.getIssueSqlRatio());
            vo.setHighestPriorityLevel(record.getHighestPriorityLevel());
            vo.setHighestPriorityScore(record.getHighestPriorityScore());
            vo.setImportant(record.getImportant());
            vo.setUrgent(record.getUrgent());
            vo.setIssueScenes(readStringList(record.getIssueScenesJson()));
            vo.setMergeCandidate(record.getMergeCandidate());
            vo.setMergeCandidateSqlCount(record.getMergeCandidateSqlCount());
            vo.setMergeCandidateReason(record.getMergeCandidateReason());
            result.add(vo);
        }
        return result;
    }

    private List<ReportBatchSqlStatisticVO> toSqlStatisticVos(List<SqlRecord> sqlRecords,
                                                              List<SqlIssueSceneRecord> issueSceneRecords,
                                                              List<SqlLogicalObjectRecord> logicalObjectRecords) {
        Map<String, List<String>> issueScenesByItemId = new LinkedHashMap<String, List<String>>();
        for (SqlIssueSceneRecord record : issueSceneRecords) {
            List<String> issueScenes = issueScenesByItemId.get(record.getItemId());
            if (issueScenes == null) {
                issueScenes = new ArrayList<String>();
                issueScenesByItemId.put(record.getItemId(), issueScenes);
            }
            issueScenes.add(record.getIssueScene());
        }
        Map<String, List<String>> logicalObjectsByItemId = new LinkedHashMap<String, List<String>>();
        for (SqlLogicalObjectRecord record : logicalObjectRecords) {
            List<String> logicalObjects = logicalObjectsByItemId.get(record.getItemId());
            if (logicalObjects == null) {
                logicalObjects = new ArrayList<String>();
                logicalObjectsByItemId.put(record.getItemId(), logicalObjects);
            }
            logicalObjects.add(record.getLogicalObjectKey());
        }
        List<ReportBatchSqlStatisticVO> result = new ArrayList<ReportBatchSqlStatisticVO>(sqlRecords.size());
        for (SqlRecord record : sqlRecords) {
            ReportBatchSqlStatisticVO vo = new ReportBatchSqlStatisticVO();
            vo.setItemId(record.getItemId());
            vo.setBatchId(record.getBatchId());
            vo.setParseTaskId(record.getParseTaskId());
            vo.setReportCode(record.getReportCode());
            vo.setReportName(record.getReportName());
            vo.setDatasourceCode(record.getDatasourceCode());
            vo.setStage(record.getStage());
            vo.setSqlColumnName(record.getSqlColumnName());
            vo.setSqlOrdinalInReport(record.getSqlOrdinalInReport());
            vo.setStatus(record.getStatus());
            vo.setSqlDigest(record.getSqlDigest());
            vo.setIssueCount(record.getIssueCount());
            vo.setHighestPriorityLevel(record.getHighestPriorityLevel());
            vo.setHighestPriorityScore(record.getHighestPriorityScore());
            vo.setImportant(record.getImportant());
            vo.setUrgent(record.getUrgent());
            vo.setIssueScenes(listOrEmpty(issueScenesByItemId.get(record.getItemId())));
            vo.setIssueLocations(readIssueLocations(record.getIssueLocationsJson()));
            vo.setLogicalObjectKeys(listOrEmpty(logicalObjectsByItemId.get(record.getItemId())));
            result.add(vo);
        }
        return result;
    }

    private List<ParsePriorityMatrixCellVO> toPriorityVos(List<PriorityRecord> records) {
        List<ParsePriorityMatrixCellVO> result = new ArrayList<ParsePriorityMatrixCellVO>(records.size());
        for (PriorityRecord record : records) {
            ParsePriorityMatrixCellVO vo = new ParsePriorityMatrixCellVO();
            vo.setPriorityLevel(record.getPriorityLevel());
            vo.setUrgencyBucket(record.getUrgencyBucket());
            vo.setSqlCount(record.getSqlCount());
            vo.setIssueCount(record.getIssueCount());
            vo.setReportCount(record.getReportCount());
            result.add(vo);
        }
        return result;
    }

    private List<ReportBatchLogicalObjectStatisticVO> toLogicalObjectVos(List<LogicalObjectRecord> records) {
        List<ReportBatchLogicalObjectStatisticVO> result = new ArrayList<ReportBatchLogicalObjectStatisticVO>(records.size());
        for (LogicalObjectRecord record : records) {
            ReportBatchLogicalObjectStatisticVO vo = new ReportBatchLogicalObjectStatisticVO();
            vo.setObjectKey(record.getLogicalObjectKey());
            vo.setSqlCount(record.getSqlCount());
            vo.setIssueCount(record.getIssueCount());
            vo.setReportCount(record.getReportCount());
            vo.setReportCodes(readStringList(record.getReportCodesJson()));
            result.add(vo);
        }
        return result;
    }

    private void insertIfNotEmptyIssueScenes(List<IssueSceneRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertIssueScenes(records);
        }
    }

    private void insertIfNotEmptySeverities(List<SeverityRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertSeverities(records);
        }
    }

    private void insertIfNotEmptyReports(List<ReportRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertReports(records);
        }
    }

    private void insertIfNotEmptySqlStats(List<SqlRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertSqlStats(records);
        }
    }

    private void insertIfNotEmptySqlIssueScenes(List<SqlIssueSceneRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertSqlIssueScenes(records);
        }
    }

    private void insertIfNotEmptySqlLogicalObjects(List<SqlLogicalObjectRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertSqlLogicalObjects(records);
        }
    }

    private void insertIfNotEmptyPriorities(List<PriorityRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertPriorities(records);
        }
    }

    private void insertIfNotEmptyImportances(List<ImportanceRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertImportances(records);
        }
    }

    private void insertIfNotEmptyLogicalObjects(List<LogicalObjectRecord> records) {
        if (!records.isEmpty()) {
            mapper.insertLogicalObjects(records);
        }
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? Integer.valueOf(0) : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private List<String> listOrEmpty(List<String> values) {
        return values == null ? Collections.<String>emptyList() : values;
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.objectMapper().readValue(json, LIST_OF_STRINGS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("报表批次统计列表载荷反序列化失败", ex);
        }
    }

    private List<ReportBatchIssueLocationVO> readIssueLocations(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return JsonUtils.objectMapper().readValue(json, LIST_OF_ISSUE_LOCATIONS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("报表批次问题定位载荷反序列化失败", ex);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return DateUtils.toBeijingDateTime(instant);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return DateUtils.toInstant(localDateTime);
    }
}
