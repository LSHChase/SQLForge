package com.company.benchmarkengine.infrastructure.persistence;

import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkAlertLinkage;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendation;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationRiskLevel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRegressionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleEvidenceManifest;
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleTarget;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatusTransition;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskSubmission;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFieldMapping;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThreshold;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTestSetRepository;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetCaseRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetRecord;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkReportMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTaskMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTestSetCaseMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTestSetMapper;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnExpression("'${benchmark-engine.queues.mode:database-worker}' != 'local-placeholder'")
public class MybatisBenchmarkTaskRepository implements BenchmarkTaskRepository, BenchmarkTestSetRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<Map<String, Object>>() {
    };

    private final BenchmarkTaskMapper benchmarkTaskMapper;
    private final BenchmarkReportMapper benchmarkReportMapper;
    private final BenchmarkTestSetMapper benchmarkTestSetMapper;
    private final BenchmarkTestSetCaseMapper benchmarkTestSetCaseMapper;
    private final ObjectMapper objectMapper;

    public MybatisBenchmarkTaskRepository(BenchmarkTaskMapper benchmarkTaskMapper,
                                          BenchmarkReportMapper benchmarkReportMapper,
                                          BenchmarkTestSetMapper benchmarkTestSetMapper,
                                          BenchmarkTestSetCaseMapper benchmarkTestSetCaseMapper) {
        this.benchmarkTaskMapper = benchmarkTaskMapper;
        this.benchmarkReportMapper = benchmarkReportMapper;
        this.benchmarkTestSetMapper = benchmarkTestSetMapper;
        this.benchmarkTestSetCaseMapper = benchmarkTestSetCaseMapper;
        this.objectMapper = JsonUtils.objectMapper();
    }

    @Override
    public BenchmarkTask saveTask(BenchmarkTask task) {
        BenchmarkTaskRecord record = toTaskRecord(task);
        if (benchmarkTaskMapper.selectByTaskId(task.getTaskId()) == null) {
            benchmarkTaskMapper.insert(record);
        } else {
            benchmarkTaskMapper.update(record);
        }
        return task;
    }

    @Override
    public BenchmarkTask findTaskByTaskId(String taskId) {
        BenchmarkTaskRecord record = benchmarkTaskMapper.selectByTaskId(taskId);
        return record == null ? null : toTask(record);
    }

    @Override
    public BenchmarkReport saveReport(BenchmarkReport report) {
        BenchmarkReportRecord record = toReportRecord(report);
        if (benchmarkReportMapper.selectByReportId(report.getReportId()) == null) {
            benchmarkReportMapper.insert(record);
        } else {
            benchmarkReportMapper.update(record);
        }
        return report;
    }

    @Override
    public BenchmarkReport findReportByTaskId(String taskId) {
        BenchmarkReportRecord record = benchmarkReportMapper.selectByTaskId(taskId);
        return record == null ? null : toReport(record);
    }

    @Override
    public BenchmarkReport findReportByReportId(String reportId) {
        BenchmarkReportRecord record = benchmarkReportMapper.selectByReportId(reportId);
        return record == null ? null : toReport(record);
    }

    @Override
    public List<BenchmarkTask> findQueuedTasksSubmittedBefore(Instant cutoff) {
        if (cutoff == null) {
            return Collections.emptyList();
        }
        List<BenchmarkTaskRecord> records = benchmarkTaskMapper.selectQueuedTasksSubmittedBefore(toLocalDateTime(cutoff));
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTask> tasks = new ArrayList<BenchmarkTask>(records.size());
        for (BenchmarkTaskRecord record : records) {
            tasks.add(toTask(record));
        }
        return tasks;
    }

    @Override
    public BenchmarkTestSet saveTestSet(BenchmarkTestSet testSet) {
        BenchmarkTestSetRecord record = toTestSetRecord(testSet);
        if (benchmarkTestSetMapper.selectByTestSetId(testSet.getTestSetId()) == null) {
            benchmarkTestSetMapper.insert(record);
        } else {
            benchmarkTestSetMapper.update(record);
        }
        benchmarkTestSetCaseMapper.deleteByTestSetId(testSet.getTestSetId());
        for (BenchmarkTestSetCase item : testSet.getCases()) {
            benchmarkTestSetCaseMapper.insert(toTestSetCaseRecord(item));
        }
        return testSet;
    }

    @Override
    public BenchmarkTestSet findTestSetByTestSetId(String testSetId) {
        BenchmarkTestSetRecord record = benchmarkTestSetMapper.selectByTestSetId(testSetId);
        if (record == null) {
            return null;
        }
        List<BenchmarkTestSetCaseRecord> caseRecords = benchmarkTestSetCaseMapper.selectByTestSetId(testSetId);
        return toTestSet(record, caseRecords);
    }

    private BenchmarkTaskRecord toTaskRecord(BenchmarkTask task) {
        BenchmarkTaskRecord record = new BenchmarkTaskRecord();
        record.setTaskId(task.getTaskId());
        record.setTenantId(task.getTenantId());
        record.setTaskType(task.getTaskType().name());
        record.setSqlText(task.getSqlText());
        record.setSqlFingerprint(task.getSqlFingerprint());
        record.setPriority(task.getPriority().name());
        record.setTargetEnginesJson(writeJson(task.getTargetEngines()));
        record.setConcurrency(task.getConcurrency());
        record.setDurationSeconds(task.getDurationSeconds());
        record.setRampUpSeconds(task.getRampUpSeconds());
        record.setDatasetSizeLabel(task.getDatasetSizeLabel());
        record.setScaleTargetJson(writeJson(task.getScaleTarget()));
        record.setTemplateId(task.getTemplateId());
        record.setTemplateType(task.getTemplateType() == null ? null : task.getTemplateType().name());
        record.setTemplateVersion(task.getTemplateVersion());
        record.setTestSetId(task.getTestSetId());
        record.setTestSetSource(task.getTestSetSource() == null ? null : task.getTestSetSource().name());
        record.setTestSetLabelsJson(writeJson(task.getTestSetLabels()));
        record.setTestSetSourceRefsJson(writeJson(task.getTestSetSourceRefs()));
        record.setReadonlyRequired(task.getReadonlyRequired());
        record.setShadowEnvironmentMode(task.getShadowEnvironmentMode().name());
        record.setDesensitizationRequirement(task.getDesensitizationRequirement().name());
        record.setThresholdsJson(writeJson(task.getThresholds()));
        record.setStatus(task.getStatus().name());
        record.setCurrentPhase(task.getCurrentPhase().name());
        record.setProgressPercent(task.getProgressPercent());
        record.setReportId(task.getReportId());
        if (task.getError() != null) {
            record.setErrorCode(Integer.valueOf(task.getError().getCode()));
            record.setErrorMessage(task.getError().getMessage());
            record.setErrorSuggestedAction(task.getError().getSuggestedAction());
            record.setErrorRetryable(Boolean.valueOf(task.getError().isRetryable()));
        }
        record.setStatusHistoryJson(writeJson(task.getStatusHistory()));
        record.setSubmittedAt(toLocalDateTime(task.getSubmittedAt()));
        record.setStartedAt(toLocalDateTime(task.getStartedAt()));
        record.setFinishedAt(toLocalDateTime(task.getFinishedAt()));
        return record;
    }

    private BenchmarkTestSetRecord toTestSetRecord(BenchmarkTestSet testSet) {
        BenchmarkTestSetRecord record = new BenchmarkTestSetRecord();
        record.setTestSetId(testSet.getTestSetId());
        record.setTenantId(testSet.getTenantId());
        record.setTestSetName(testSet.getTestSetName());
        record.setTemplateId(testSet.getTemplateId());
        record.setTemplateType(testSet.getTemplateType() == null ? null : testSet.getTemplateType().name());
        record.setTemplateVersion(testSet.getTemplateVersion());
        record.setTestSetSource(testSet.getTestSetSource().name());
        record.setStatus(testSet.getStatus().name());
        record.setTotalCases(testSet.getTotalCases());
        record.setAcceptedCases(testSet.getAcceptedCases());
        record.setRejectedCases(testSet.getRejectedCases());
        record.setFileType(testSet.getFileType() == null ? null : testSet.getFileType().name());
        record.setFileName(testSet.getFileName());
        record.setImportBatchId(testSet.getImportBatchId());
        record.setFieldMappingsJson(writeJson(testSet.getFieldMappings()));
        record.setTestSetLabelsJson(writeJson(testSet.getTestSetLabels()));
        record.setTestSetSourceRefsJson(writeJson(testSet.getTestSetSourceRefs()));
        record.setCreatedBy(testSet.getCreatedBy());
        record.setCreatedAt(toLocalDateTime(testSet.getCreatedAt()));
        record.setUpdatedAt(toLocalDateTime(testSet.getUpdatedAt()));
        return record;
    }

    private BenchmarkTestSetCaseRecord toTestSetCaseRecord(BenchmarkTestSetCase item) {
        BenchmarkTestSetCaseRecord record = new BenchmarkTestSetCaseRecord();
        record.setCaseId(item.getCaseId());
        record.setTestSetId(item.getTestSetId());
        record.setSequenceNumber(item.getSequenceNumber());
        record.setSourceLineNumber(item.getSourceLineNumber());
        record.setCaseName(item.getCaseName());
        record.setSqlText(item.getSqlText());
        record.setSqlFingerprint(item.getSqlFingerprint());
        record.setDatasourceCode(item.getDatasourceCode());
        record.setReportCode(item.getReportCode());
        record.setTagsJson(writeJson(item.getTags()));
        record.setBindParametersJson(item.getBindParametersJson());
        record.setStatus(item.getStatus().name());
        record.setRejectionReason(item.getRejectionReason());
        record.setRawCaseDataJson(item.getRawCaseDataJson());
        return record;
    }

    private BenchmarkReportRecord toReportRecord(BenchmarkReport report) {
        BenchmarkReportRecord record = new BenchmarkReportRecord();
        record.setReportId(report.getReportId());
        record.setTaskId(report.getTaskId());
        record.setTenantId(report.getTenantId());
        record.setTaskType(report.getTaskType().name());
        record.setSqlFingerprint(report.getSqlFingerprint());
        record.setGeneratedAt(toLocalDateTime(report.getGeneratedAt()));
        record.setVerdict(report.getVerdict().name());
        record.setEngineProfilesJson(writeJson(report.getEngineProfiles()));
        record.setThresholdAssessmentsJson(writeJson(report.getThresholdAssessments()));
        record.setRecommendationsJson(writeJson(report.getRecommendations()));
        record.setExecutionSummaryJson(writeJson(report.getExecutionSummary()));
        record.setRegressionSummaryJson(writeJson(report.getRegressionSummary()));
        record.setAlertLinkagesJson(writeJson(report.getAlertLinkages()));
        record.setExportArtifactsJson(writeJson(report.getExportArtifacts()));
        return record;
    }

    private BenchmarkTask toTask(BenchmarkTaskRecord record) {
        BenchmarkTaskSubmission submission = new BenchmarkTaskSubmission(
            record.getTenantId(),
            BenchmarkTaskType.valueOf(record.getTaskType()),
            record.getSqlText(),
            record.getSqlFingerprint(),
            BenchmarkTaskPriority.valueOf(record.getPriority()),
            readTargetEngines(record.getTargetEnginesJson()),
            record.getConcurrency(),
            record.getDurationSeconds(),
            record.getRampUpSeconds(),
            record.getDatasetSizeLabel(),
            readScaleTarget(record.getScaleTargetJson()),
            record.getTemplateId(),
            record.getTemplateType() == null ? null : BenchmarkTemplateType.valueOf(record.getTemplateType()),
            record.getTemplateVersion(),
            record.getTestSetId(),
            record.getTestSetSource() == null ? null : BenchmarkTestSetSource.valueOf(record.getTestSetSource()),
            readTestSetLabels(record.getTestSetLabelsJson()),
            readSourceReferences(record.getTestSetSourceRefsJson()),
            record.getReadonlyRequired(),
            record.getShadowEnvironmentMode() == null ? null : ShadowEnvironmentMode.valueOf(record.getShadowEnvironmentMode()),
            record.getDesensitizationRequirement() == null
                ? null
                : DesensitizationRequirement.valueOf(record.getDesensitizationRequirement()),
            readThresholds(record.getThresholdsJson())
        );
        return BenchmarkTask.restore(
            record.getTaskId(),
            submission,
            toInstant(record.getSubmittedAt()),
            readStatusHistory(record.getStatusHistoryJson()),
            BenchmarkTaskStatus.valueOf(record.getStatus()),
            BenchmarkTaskPhase.valueOf(record.getCurrentPhase()),
            record.getProgressPercent(),
            toInstant(record.getStartedAt()),
            toInstant(record.getFinishedAt()),
            record.getReportId(),
            toError(record)
        );
    }

    private BenchmarkReport toReport(BenchmarkReportRecord record) {
        return new BenchmarkReport(
            record.getReportId(),
            record.getTaskId(),
            BenchmarkTaskType.valueOf(record.getTaskType()),
            record.getTenantId(),
            record.getSqlFingerprint(),
            toInstant(record.getGeneratedAt()),
            readEngineProfiles(record.getEngineProfilesJson()),
            readThresholdAssessments(record.getThresholdAssessmentsJson()),
            readRecommendations(record.getRecommendationsJson()),
            readExecutionSummary(record.getExecutionSummaryJson()),
            readRegressionSummary(record.getRegressionSummaryJson()),
            readAlertLinkages(record.getAlertLinkagesJson()),
            readExportArtifacts(record.getExportArtifactsJson())
        );
    }

    private BenchmarkTestSet toTestSet(BenchmarkTestSetRecord record, List<BenchmarkTestSetCaseRecord> caseRecords) {
        return new BenchmarkTestSet(
            record.getTestSetId(),
            record.getTenantId(),
            record.getTestSetName(),
            record.getTemplateId(),
            record.getTemplateType() == null ? null : BenchmarkTemplateType.valueOf(record.getTemplateType()),
            record.getTemplateVersion(),
            BenchmarkTestSetSource.valueOf(record.getTestSetSource()),
            BenchmarkTestSetStatus.valueOf(record.getStatus()),
            record.getTotalCases(),
            record.getAcceptedCases(),
            record.getRejectedCases(),
            record.getFileType() == null ? null : BenchmarkTestSetFileType.valueOf(record.getFileType()),
            record.getFileName(),
            record.getImportBatchId(),
            readFieldMappings(record.getFieldMappingsJson()),
            readTestSetLabels(record.getTestSetLabelsJson()),
            readSourceReferences(record.getTestSetSourceRefsJson()),
            readTestSetCases(caseRecords),
            record.getCreatedBy(),
            toInstant(record.getCreatedAt()),
            toInstant(record.getUpdatedAt())
        );
    }

    private BenchmarkTaskError toError(BenchmarkTaskRecord record) {
        if (record.getErrorCode() == null) {
            return null;
        }
        return new BenchmarkTaskError(
            record.getErrorCode().intValue(),
            record.getErrorMessage(),
            record.getErrorSuggestedAction(),
            Boolean.TRUE.equals(record.getErrorRetryable())
        );
    }

    private List<DataSourceTypeEnum> readTargetEngines(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, LIST_OF_STRINGS);
            List<DataSourceTypeEnum> engines = new ArrayList<DataSourceTypeEnum>(values.size());
            for (String value : values) {
                engines.add(DataSourceTypeEnum.valueOf(value));
            }
            return engines;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测目标引擎反序列化失败", ex);
        }
    }

    private List<BenchmarkThreshold> readThresholds(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkThreshold> thresholds = new ArrayList<BenchmarkThreshold>(items.size());
            for (Map<String, Object> item : items) {
                thresholds.add(
                    new BenchmarkThreshold(
                        readEnum(item.get("metric"), BenchmarkThresholdMetric.class),
                        readEnum(item.get("operator"), BenchmarkThresholdOperator.class),
                        readBigDecimal(item.get("targetValue")),
                        readEnum(item.get("severity"), BenchmarkThresholdSeverity.class),
                        item.get("description") == null ? null : String.valueOf(item.get("description"))
                    )
                );
            }
            return thresholds;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测阈值反序列化失败", ex);
        }
    }

    private BenchmarkScaleTarget readScaleTarget(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            Map<String, Object> item = objectMapper.readValue(json, MAP_OF_OBJECTS);
            return readScaleTargetMap(item);
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测规模目标反序列化失败", ex);
        }
    }

    private BenchmarkScaleTarget readScaleTargetMap(Map<String, Object> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        return new BenchmarkScaleTarget(
            readInteger(item.get("targetConcurrency")),
            item.get("targetDatasetSizeLabel") == null ? null : String.valueOf(item.get("targetDatasetSizeLabel")),
            readLong(item.get("targetDailyQueryVolume")),
            item.get("targetComplexityProfile") == null ? null : String.valueOf(item.get("targetComplexityProfile")),
            item.get("targetCostEfficiency") == null ? null : String.valueOf(item.get("targetCostEfficiency")),
            item.get("evidenceStatus") == null ? null : String.valueOf(item.get("evidenceStatus")),
            item.get("evidenceBoundary") == null ? null : String.valueOf(item.get("evidenceBoundary")),
            readStringList(item.get("requiredEvidence")),
            readScaleEvidenceManifest(readObjectMap(item.get("evidenceManifest")))
        );
    }

    private BenchmarkScaleEvidenceManifest readScaleEvidenceManifest(Map<String, Object> item) {
        if (item == null || item.isEmpty()) {
            return null;
        }
        BenchmarkScaleEvidenceManifest evidenceManifest = new BenchmarkScaleEvidenceManifest(
            item.get("evidenceSource") == null ? null : String.valueOf(item.get("evidenceSource")),
            item.get("concurrencyProofRef") == null ? null : String.valueOf(item.get("concurrencyProofRef")),
            item.get("dataLayoutProofRef") == null ? null : String.valueOf(item.get("dataLayoutProofRef")),
            item.get("workloadReplayProofRef") == null ? null : String.valueOf(item.get("workloadReplayProofRef")),
            item.get("workloadReplayWindow") == null ? null : String.valueOf(item.get("workloadReplayWindow")),
            item.get("p95P99MetricProofRef") == null ? null : String.valueOf(item.get("p95P99MetricProofRef")),
            item.get("scanCpuQueueMetricProofRef") == null ? null : String.valueOf(item.get("scanCpuQueueMetricProofRef")),
            item.get("costBillProofRef") == null ? null : String.valueOf(item.get("costBillProofRef")),
            item.get("externalVerificationStatus") == null ? null : String.valueOf(item.get("externalVerificationStatus"))
        );
        return evidenceManifest.hasAnyEvidence() ? evidenceManifest : null;
    }

    private List<BenchmarkTestSetLabel> readTestSetLabels(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkTestSetLabel> labels = new ArrayList<BenchmarkTestSetLabel>(items.size());
            for (Map<String, Object> item : items) {
                labels.add(
                    new BenchmarkTestSetLabel(
                        readEnum(item.get("type"), BenchmarkTestSetLabelType.class),
                        item.get("value") == null ? null : String.valueOf(item.get("value"))
                    )
                );
            }
            return labels;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测测试集标签反序列化失败", ex);
        }
    }

    private List<BenchmarkTestSetFieldMapping> readFieldMappings(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkTestSetFieldMapping> mappings = new ArrayList<BenchmarkTestSetFieldMapping>(items.size());
            for (Map<String, Object> item : items) {
                mappings.add(
                    new BenchmarkTestSetFieldMapping(
                        readEnum(item.get("field"), BenchmarkTestSetField.class),
                        item.get("columnName") == null ? null : String.valueOf(item.get("columnName"))
                    )
                );
            }
            return mappings;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测测试集字段映射反序列化失败", ex);
        }
    }

    private List<BenchmarkSourceReference> readSourceReferences(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkSourceReference> refs = new ArrayList<BenchmarkSourceReference>(items.size());
            for (Map<String, Object> item : items) {
                refs.add(
                    new BenchmarkSourceReference(
                        readEnum(item.get("type"), BenchmarkSourceReferenceType.class),
                        item.get("referenceId") == null ? null : String.valueOf(item.get("referenceId"))
                    )
                );
            }
            return refs;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测来源引用反序列化失败", ex);
        }
    }

    private List<BenchmarkTestSetCase> readTestSetCases(List<BenchmarkTestSetCaseRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<BenchmarkTestSetCase> items = new ArrayList<BenchmarkTestSetCase>(records.size());
        for (BenchmarkTestSetCaseRecord record : records) {
            items.add(
                new BenchmarkTestSetCase(
                    record.getCaseId(),
                    record.getTestSetId(),
                    record.getSequenceNumber(),
                    record.getSourceLineNumber(),
                    record.getCaseName(),
                    record.getSqlText(),
                    record.getSqlFingerprint(),
                    record.getDatasourceCode(),
                    record.getReportCode(),
                    readStringList(record.getTagsJson()),
                    record.getBindParametersJson(),
                    BenchmarkTestSetCaseStatus.valueOf(record.getStatus()),
                    record.getRejectionReason(),
                    record.getRawCaseDataJson()
                )
            );
        }
        return items;
    }

    private List<BenchmarkTaskStatusTransition> readStatusHistory(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkTaskStatusTransition> history = new ArrayList<BenchmarkTaskStatusTransition>(items.size());
            for (Map<String, Object> item : items) {
                history.add(
                    new BenchmarkTaskStatusTransition(
                        readEnum(item.get("previousStatus"), BenchmarkTaskStatus.class),
                        readEnum(item.get("currentStatus"), BenchmarkTaskStatus.class),
                        readEnum(item.get("previousPhase"), BenchmarkTaskPhase.class),
                        readEnum(item.get("currentPhase"), BenchmarkTaskPhase.class),
                        item.get("occurredAt") == null ? null : Instant.parse(String.valueOf(item.get("occurredAt"))),
                        item.get("note") == null ? null : String.valueOf(item.get("note"))
                    )
                );
            }
            return history;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测任务状态历史反序列化失败", ex);
        }
    }

    private List<BenchmarkEngineProfile> readEngineProfiles(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkEngineProfile> profiles = new ArrayList<BenchmarkEngineProfile>(items.size());
            for (Map<String, Object> item : items) {
                profiles.add(
                    new BenchmarkEngineProfile(
                        readEnum(item.get("engine"), DataSourceTypeEnum.class),
                        readBigDecimal(item.get("targetQps")),
                        readBigDecimal(item.get("actualQps")),
                        readBigDecimal(item.get("p50LatencyMs")),
                        readBigDecimal(item.get("p95LatencyMs")),
                        readBigDecimal(item.get("p99LatencyMs")),
                        readBigDecimal(item.get("cpuUsagePercent")),
                        readBigDecimal(item.get("memoryUsageMb")),
                        readBigDecimal(item.get("scannedDataBytes")),
                        readEnum(item.get("verdict"), BenchmarkThresholdVerdict.class),
                        item.get("notes") == null ? null : String.valueOf(item.get("notes"))
                    )
                );
            }
            return profiles;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测引擎画像反序列化失败", ex);
        }
    }

    private List<BenchmarkThresholdAssessment> readThresholdAssessments(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkThresholdAssessment> assessments = new ArrayList<BenchmarkThresholdAssessment>(items.size());
            for (Map<String, Object> item : items) {
                assessments.add(
                    new BenchmarkThresholdAssessment(
                        readEnum(item.get("metric"), BenchmarkThresholdMetric.class),
                        readEnum(item.get("verdict"), BenchmarkThresholdVerdict.class),
                        readBigDecimal(item.get("actualValue")),
                        readBigDecimal(item.get("targetValue")),
                        item.get("summary") == null ? null : String.valueOf(item.get("summary"))
                    )
                );
            }
            return assessments;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测阈值评估反序列化失败", ex);
        }
    }

    private List<BenchmarkRecommendation> readRecommendations(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkRecommendation> recommendations = new ArrayList<BenchmarkRecommendation>(items.size());
            for (Map<String, Object> item : items) {
                recommendations.add(
                    new BenchmarkRecommendation(
                        item.get("category") == null ? null : String.valueOf(item.get("category")),
                        item.get("title") == null ? null : String.valueOf(item.get("title")),
                        item.get("summary") == null ? null : String.valueOf(item.get("summary")),
                        item.get("expectedBenefit") == null ? null : String.valueOf(item.get("expectedBenefit")),
                        readEnum(item.get("riskLevel"), BenchmarkRecommendationRiskLevel.class)
                    )
                );
            }
            return recommendations;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测建议反序列化失败", ex);
        }
    }

    private BenchmarkExecutionSummary readExecutionSummary(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            Map<String, Object> item = objectMapper.readValue(json, MAP_OF_OBJECTS);
            return new BenchmarkExecutionSummary(
                item.get("executionMode") == null ? null : String.valueOf(item.get("executionMode")),
                item.get("isolationSummary") == null ? null : String.valueOf(item.get("isolationSummary")),
                item.get("sampleCount") == null ? null : Integer.valueOf(String.valueOf(item.get("sampleCount"))),
                item.get("executionDurationMs") == null ? null : Long.valueOf(String.valueOf(item.get("executionDurationMs"))),
                item.get("workloadDigest") == null ? null : String.valueOf(item.get("workloadDigest")),
                readScaleReadiness(item.get("scaleReadiness")),
                readStringList(item.get("phaseNotes"))
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测执行摘要反序列化失败", ex);
        }
    }

    private BenchmarkScaleReadinessAssessment readScaleReadiness(Object rawValue) {
        Map<String, Object> item = readObjectMap(rawValue);
        if (item == null || item.isEmpty()) {
            return null;
        }
        return new BenchmarkScaleReadinessAssessment(
            readEnum(item.get("readinessStatus"), BenchmarkScaleReadinessStatus.class, BenchmarkScaleReadinessStatus.NOT_PROVEN),
            readScaleTargetMap(readObjectMap(item.get("scaleTarget"))),
            readBigDecimal(item.get("observedMaxP95LatencyMs")),
            readBigDecimal(item.get("observedMaxP99LatencyMs")),
            readBigDecimal(item.get("observedMaxCpuUsagePercent")),
            readBigDecimal(item.get("observedMaxMemoryUsageMb")),
            readBigDecimal(item.get("observedMaxScannedDataBytes")),
            readBigDecimal(item.get("observedQueueWaitMs")),
            readBigDecimal(item.get("projectedDailyQueryCapacity")),
            readBigDecimal(item.get("estimatedResourceUnitPerMillionQueries")),
            item.get("workloadEvidenceStatus") == null ? null : String.valueOf(item.get("workloadEvidenceStatus")),
            readStringList(item.get("satisfiedEvidence")),
            readStringList(item.get("missingEvidence")),
            item.get("summary") == null ? null : String.valueOf(item.get("summary"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readObjectMap(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Map<?, ?>) {
            return (Map<String, Object>) rawValue;
        }
        try {
            return objectMapper.readValue(String.valueOf(rawValue), MAP_OF_OBJECTS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测对象载荷反序列化失败", ex);
        }
    }

    private List<BenchmarkReportArtifact> readExportArtifacts(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkReportArtifact> artifacts = new ArrayList<BenchmarkReportArtifact>(items.size());
            for (Map<String, Object> item : items) {
                artifacts.add(
                    new BenchmarkReportArtifact(
                        item.get("artifactKey") == null ? null : String.valueOf(item.get("artifactKey")),
                        readEnum(item.get("artifactKind"), BenchmarkReportArtifactKind.class, BenchmarkReportArtifactKind.REPORT_EXPORT),
                        readEnum(item.get("format"), BenchmarkReportFormat.class),
                        item.get("fileName") == null ? null : String.valueOf(item.get("fileName")),
                        item.get("mediaType") == null ? null : String.valueOf(item.get("mediaType")),
                        item.get("contentLength") == null ? null : Integer.valueOf(String.valueOf(item.get("contentLength"))),
                        item.get("checksumSha256") == null ? null : String.valueOf(item.get("checksumSha256")),
                        item.get("storageType") == null ? null : String.valueOf(item.get("storageType")),
                        item.get("storageUri") == null ? null : String.valueOf(item.get("storageUri")),
                        item.get("storageEvidence") == null ? null : String.valueOf(item.get("storageEvidence")),
                        item.get("exportId") == null ? null : String.valueOf(item.get("exportId")),
                        item.get("retentionDays") == null ? null : Integer.valueOf(String.valueOf(item.get("retentionDays"))),
                        item.get("retentionPolicySource") == null ? null : String.valueOf(item.get("retentionPolicySource")),
                        item.get("retentionDeleteAfter") == null ? null : String.valueOf(item.get("retentionDeleteAfter")),
                        item.get("content") == null ? null : String.valueOf(item.get("content"))
                    )
                );
            }
            return artifacts;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测导出产物反序列化失败", ex);
        }
    }

    private BenchmarkRegressionSummary readRegressionSummary(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return null;
        }
        try {
            Map<String, Object> item = objectMapper.readValue(json, MAP_OF_OBJECTS);
            return new BenchmarkRegressionSummary(
                item.get("thresholdHitCount") == null ? null : Integer.valueOf(String.valueOf(item.get("thresholdHitCount"))),
                item.get("failedThresholdCount") == null ? null : Integer.valueOf(String.valueOf(item.get("failedThresholdCount"))),
                item.get("warningThresholdCount") == null ? null : Integer.valueOf(String.valueOf(item.get("warningThresholdCount"))),
                item.get("alertRequired") == null ? null : Boolean.valueOf(String.valueOf(item.get("alertRequired"))),
                item.get("summary") == null ? null : String.valueOf(item.get("summary"))
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测回归摘要反序列化失败", ex);
        }
    }

    private List<BenchmarkAlertLinkage> readAlertLinkages(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> items = objectMapper.readValue(json, LIST_OF_MAPS);
            List<BenchmarkAlertLinkage> linkages = new ArrayList<BenchmarkAlertLinkage>(items.size());
            for (Map<String, Object> item : items) {
                linkages.add(
                    new BenchmarkAlertLinkage(
                        item.get("alertId") == null ? null : String.valueOf(item.get("alertId")),
                        item.get("alertType") == null ? null : String.valueOf(item.get("alertType")),
                        item.get("alertLevel") == null ? null : String.valueOf(item.get("alertLevel")),
                        item.get("alertStatus") == null ? null : String.valueOf(item.get("alertStatus")),
                        item.get("notifyStatus") == null ? null : String.valueOf(item.get("notifyStatus")),
                        item.get("summary") == null ? null : String.valueOf(item.get("summary")),
                        item.get("detailPath") == null ? null : String.valueOf(item.get("detailPath")),
                        item.get("linkageMode") == null ? null : String.valueOf(item.get("linkageMode")),
                        item.get("notificationLogId") == null ? null : String.valueOf(item.get("notificationLogId"))
                    )
                );
            }
            return linkages;
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测告警关联反序列化失败", ex);
        }
    }

    private List<String> readStringList(Object raw) {
        if (raw instanceof String) {
            String json = String.valueOf(raw);
            if (json.trim().isEmpty() || "null".equals(json.trim())) {
                return Collections.emptyList();
            }
            try {
                List<String> values = objectMapper.readValue(json, LIST_OF_STRINGS);
                return values == null ? Collections.<String>emptyList() : values;
            } catch (Exception ex) {
                throw new IllegalArgumentException("压测字符串列表反序列化失败", ex);
            }
        }
        if (!(raw instanceof List<?>)) {
            return Collections.emptyList();
        }
        List<?> rawItems = (List<?>) raw;
        List<String> items = new ArrayList<String>(rawItems.size());
        for (Object rawItem : rawItems) {
            items.add(rawItem == null ? null : String.valueOf(rawItem));
        }
        return items;
    }

    private <T extends Enum<T>> T readEnum(Object raw, Class<T> enumType, T defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        return Enum.valueOf(enumType, String.valueOf(raw));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("压测持久化载荷序列化失败", ex);
        }
    }

    private <T extends Enum<T>> T readEnum(Object value, Class<T> enumType) {
        if (value == null) {
            return null;
        }
        return Enum.valueOf(enumType, String.valueOf(value));
    }

    private BigDecimal readBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Integer readInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(DATABASE_ZONE_OFFSET);
    }
}
