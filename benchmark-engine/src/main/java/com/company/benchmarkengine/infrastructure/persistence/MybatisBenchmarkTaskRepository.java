package com.company.benchmarkengine.infrastructure.persistence;

import com.company.benchmarkengine.domain.benchmark.BenchmarkEngineProfile;
import com.company.benchmarkengine.domain.benchmark.BenchmarkExecutionSummary;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendation;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationRiskLevel;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifactKind;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatusTransition;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskSubmission;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThreshold;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdAssessment;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.domain.benchmark.repository.BenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkReportMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTaskMapper;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(prefix = "benchmark-engine.queues", name = "mode", havingValue = "database-worker")
public class MybatisBenchmarkTaskRepository implements BenchmarkTaskRepository {

    private static final ZoneOffset DATABASE_ZONE_OFFSET = ZoneOffset.UTC;
    private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<List<Map<String, Object>>>() {
    };
    private static final TypeReference<List<String>> LIST_OF_STRINGS = new TypeReference<List<String>>() {
    };
    private static final TypeReference<Map<String, Object>> MAP_OF_OBJECTS = new TypeReference<Map<String, Object>>() {
    };

    private final BenchmarkTaskMapper benchmarkTaskMapper;
    private final BenchmarkReportMapper benchmarkReportMapper;
    private final ObjectMapper objectMapper;

    public MybatisBenchmarkTaskRepository(BenchmarkTaskMapper benchmarkTaskMapper,
                                          BenchmarkReportMapper benchmarkReportMapper) {
        this.benchmarkTaskMapper = benchmarkTaskMapper;
        this.benchmarkReportMapper = benchmarkReportMapper;
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
            readExportArtifacts(record.getExportArtifactsJson())
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
            throw new IllegalArgumentException("Failed to deserialize benchmark target engines", ex);
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
            throw new IllegalArgumentException("Failed to deserialize benchmark thresholds", ex);
        }
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
            throw new IllegalArgumentException("Failed to deserialize benchmark task status history", ex);
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
            throw new IllegalArgumentException("Failed to deserialize benchmark engine profiles", ex);
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
            throw new IllegalArgumentException("Failed to deserialize benchmark threshold assessments", ex);
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
            throw new IllegalArgumentException("Failed to deserialize benchmark recommendations", ex);
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
                readStringList(item.get("phaseNotes"))
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize benchmark execution summary", ex);
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
            throw new IllegalArgumentException("Failed to deserialize benchmark export artifacts", ex);
        }
    }

    private List<String> readStringList(Object raw) {
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
            throw new IllegalArgumentException("Failed to serialize benchmark persistence payload", ex);
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

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, DATABASE_ZONE_OFFSET);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.toInstant(DATABASE_ZONE_OFFSET);
    }
}
