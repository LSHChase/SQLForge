package com.company.benchmarkengine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTestSetLabelDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportRawDataResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkReportResponse;
import com.company.benchmarkengine.application.service.BenchmarkIsolatedExecutionResult;
import com.company.benchmarkengine.application.service.BenchmarkIsolatedExecutionService;
import com.company.benchmarkengine.application.service.BenchmarkReportExportService;
import com.company.benchmarkengine.application.service.BenchmarkTaskModelApplicationService;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReport;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkReportMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTaskMapper;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class MybatisBenchmarkTaskRepositoryTest {

    @Test
    void shouldPersistAndRestoreBenchmarkTaskThroughMybatisRecordMapping() {
        BenchmarkTaskMapper taskMapper = org.mockito.Mockito.mock(BenchmarkTaskMapper.class);
        BenchmarkReportMapper reportMapper = org.mockito.Mockito.mock(BenchmarkReportMapper.class);
        AtomicReference<BenchmarkTaskRecord> storedTask = new AtomicReference<BenchmarkTaskRecord>();
        when(taskMapper.selectByTaskId("task-db-001")).thenAnswer(invocation -> storedTask.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            storedTask.set((BenchmarkTaskRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(taskMapper).insert(any(BenchmarkTaskRecord.class));

        MybatisBenchmarkTaskRepository repository = new MybatisBenchmarkTaskRepository(taskMapper, reportMapper);
        BenchmarkTask task = createQueuedTask("task-db-001", BenchmarkTaskType.COMPARISON, Instant.parse("2026-04-22T05:00:00Z"));
        task.markRunning(Instant.parse("2026-04-22T05:00:05Z"));
        task.advancePhase(BenchmarkTaskPhase.SHADOW_VALIDATING, 25, "BASELINE_PREPARED");
        task.advancePhase(BenchmarkTaskPhase.WARMING_UP, 40, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 60, "WARMUP_FINISHED");
        task.markFailed(
            new BenchmarkTaskError(14000, "Benchmark worker failed", "Retry after worker recovery", true),
            Instant.parse("2026-04-22T05:01:00Z")
        );

        repository.saveTask(task);

        BenchmarkTaskRecord record = storedTask.get();
        assertNotNull(record);
        assertEquals("task-db-001", record.getTaskId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("COMPARISON", record.getTaskType());
        assertEquals("FAILED", record.getStatus());
        assertEquals("FINISHED", record.getCurrentPhase());
        assertEquals(Integer.valueOf(60), record.getProgressPercent());
        assertEquals(LocalDateTime.of(2026, 4, 22, 5, 0, 0), record.getSubmittedAt());
        assertEquals(LocalDateTime.of(2026, 4, 22, 5, 0, 5), record.getStartedAt());
        assertEquals(LocalDateTime.of(2026, 4, 22, 5, 1, 0), record.getFinishedAt());
        assertTrue(record.getTargetEnginesJson().contains("HETU"));
        assertTrue(record.getThresholdsJson().contains("P99_LATENCY_MS"));
        assertEquals("comparison-dual-engine", record.getTemplateId());
        assertEquals("CROSS_ENGINE_COMPARISON", record.getTemplateType());
        assertEquals("RECOMMENDATION_GENERATION", record.getTestSetSource());
        assertTrue(record.getTestSetLabelsJson().contains("ROUTE_GOVERNANCE"));
        assertTrue(record.getTestSetSourceRefsJson().contains("RECOMMENDATION"));
        assertTrue(record.getStatusHistoryJson().contains("TASK_FAILED"));
        assertEquals(Integer.valueOf(14000), record.getErrorCode());
        assertEquals("Benchmark worker failed", record.getErrorMessage());
        assertEquals("Retry after worker recovery", record.getErrorSuggestedAction());
        assertEquals(Boolean.TRUE, record.getErrorRetryable());

        BenchmarkTask restored = repository.findTaskByTaskId("task-db-001");

        assertNotNull(restored);
        assertEquals("task-db-001", restored.getTaskId());
        assertEquals(BenchmarkTaskType.COMPARISON, restored.getTaskType());
        assertEquals(BenchmarkTaskStatus.FAILED, restored.getStatus());
        assertEquals(BenchmarkTaskPhase.FINISHED, restored.getCurrentPhase());
        assertEquals(2, restored.getTargetEngines().size());
        assertEquals(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON, restored.getTemplateType());
        assertEquals(BenchmarkTestSetSource.RECOMMENDATION_GENERATION, restored.getTestSetSource());
        assertEquals(2, restored.getTestSetLabels().size());
        assertEquals(2, restored.getTestSetSourceRefs().size());
        assertEquals(3, restored.getThresholds().size());
        assertEquals(Instant.parse("2026-04-22T05:00:00Z"), restored.getSubmittedAt());
        assertEquals(Instant.parse("2026-04-22T05:00:05Z"), restored.getStartedAt());
        assertEquals(Instant.parse("2026-04-22T05:01:00Z"), restored.getFinishedAt());
        assertEquals(Integer.valueOf(14000), Integer.valueOf(restored.getError().getCode()));
        assertEquals("Retry after worker recovery", restored.getError().getSuggestedAction());
        assertTrue(restored.getError().isRetryable());
        assertFalse(restored.getStatusHistory().isEmpty());
        assertEquals("TASK_FAILED", restored.getStatusHistory().get(restored.getStatusHistory().size() - 1).getNote());
    }

    @Test
    void shouldUpdateExistingTaskAndFindQueuedTasksBeforeCutoff() {
        BenchmarkTaskMapper taskMapper = org.mockito.Mockito.mock(BenchmarkTaskMapper.class);
        BenchmarkReportMapper reportMapper = org.mockito.Mockito.mock(BenchmarkReportMapper.class);
        AtomicReference<BenchmarkTaskRecord> storedTask = new AtomicReference<BenchmarkTaskRecord>();
        when(taskMapper.selectByTaskId("task-db-002")).thenAnswer(invocation -> storedTask.get());
        when(taskMapper.selectQueuedTasksSubmittedBefore(any(LocalDateTime.class))).thenAnswer(invocation -> {
            BenchmarkTaskRecord value = storedTask.get();
            return value == null ? Collections.<BenchmarkTaskRecord>emptyList() : Collections.singletonList(value);
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            storedTask.set((BenchmarkTaskRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(taskMapper).insert(any(BenchmarkTaskRecord.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            storedTask.set((BenchmarkTaskRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(taskMapper).update(any(BenchmarkTaskRecord.class));

        MybatisBenchmarkTaskRepository repository = new MybatisBenchmarkTaskRepository(taskMapper, reportMapper);
        BenchmarkTask task = createQueuedTask("task-db-002", BenchmarkTaskType.BASELINE, Instant.parse("2026-04-22T04:00:00Z"));

        repository.saveTask(task);
        task.markRunning(Instant.parse("2026-04-22T04:00:10Z"));
        repository.saveTask(task);

        verify(taskMapper).insert(any(BenchmarkTaskRecord.class));
        verify(taskMapper).update(any(BenchmarkTaskRecord.class));
        assertTrue(repository.findQueuedTasksSubmittedBefore(null).isEmpty());

        List<BenchmarkTask> queuedTasks =
            repository.findQueuedTasksSubmittedBefore(Instant.parse("2026-04-22T05:00:00Z"));

        assertEquals(1, queuedTasks.size());
        assertEquals("task-db-002", queuedTasks.get(0).getTaskId());
        assertEquals(BenchmarkTaskStatus.RUNNING, queuedTasks.get(0).getStatus());
        assertEquals(Instant.parse("2026-04-22T04:00:10Z"), queuedTasks.get(0).getStartedAt());
    }

    @Test
    void shouldPersistAndRestoreBenchmarkReportAndSupportLookupVariants() {
        BenchmarkTaskMapper taskMapper = org.mockito.Mockito.mock(BenchmarkTaskMapper.class);
        BenchmarkReportMapper reportMapper = org.mockito.Mockito.mock(BenchmarkReportMapper.class);
        AtomicReference<BenchmarkReportRecord> storedReport = new AtomicReference<BenchmarkReportRecord>();
        when(reportMapper.selectByTaskId("task-db-003")).thenAnswer(invocation -> storedReport.get());
        when(reportMapper.selectByReportId("report-task-db-003")).thenAnswer(invocation -> storedReport.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            storedReport.set((BenchmarkReportRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(reportMapper).insert(any(BenchmarkReportRecord.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            storedReport.set((BenchmarkReportRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(reportMapper).update(any(BenchmarkReportRecord.class));

        MybatisBenchmarkTaskRepository repository = new MybatisBenchmarkTaskRepository(taskMapper, reportMapper);
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = createQueuedTask("task-db-003", BenchmarkTaskType.REGRESSION_GUARD, Instant.parse("2026-04-22T06:00:00Z"));
        task.markRunning(Instant.parse("2026-04-22T06:00:05Z"));
        task.advancePhase(BenchmarkTaskPhase.EXECUTING, 75, "SHADOW_VALIDATED");
        task.advancePhase(BenchmarkTaskPhase.THRESHOLD_EVALUATING, 90, "RUN_FINISHED");
        task.advancePhase(BenchmarkTaskPhase.REPORTING, 95, "THRESHOLDS_EVALUATED");
        task.markSucceeded("report-task-db-003", Instant.parse("2026-04-22T06:01:00Z"));
        BenchmarkReport report = createExecutedReport(modelService, task, Instant.parse("2026-04-22T06:01:10Z"));

        repository.saveReport(report);
        repository.saveReport(report);

        BenchmarkReportRecord record = storedReport.get();
        assertNotNull(record);
        assertEquals("report-task-db-003", record.getReportId());
        assertEquals("task-db-003", record.getTaskId());
        assertEquals(LocalDateTime.of(2026, 4, 22, 6, 1, 10), record.getGeneratedAt());
        assertTrue(record.getEngineProfilesJson().contains("HETU"));
        assertTrue(record.getThresholdAssessmentsJson().contains("P99_LATENCY_MS"));
        assertTrue(record.getRecommendationsJson().contains("REGRESSION_GATE"));
        assertTrue(record.getExecutionSummaryJson().contains("REPO_CLOSED_ISOLATED_EXECUTOR"));
        assertTrue(record.getExportArtifactsJson().contains("\"format\":\"PDF\""));
        verify(reportMapper).insert(any(BenchmarkReportRecord.class));
        verify(reportMapper).update(any(BenchmarkReportRecord.class));

        BenchmarkReport byTaskId = repository.findReportByTaskId("task-db-003");
        BenchmarkReport byReportId = repository.findReportByReportId("report-task-db-003");

        assertNotNull(byTaskId);
        assertNotNull(byReportId);
        assertEquals(BenchmarkTaskType.REGRESSION_GUARD, byTaskId.getTaskType());
        assertEquals(BenchmarkThresholdVerdict.FAIL, byTaskId.getVerdict());
        assertEquals(1, byTaskId.getEngineProfiles().size());
        assertEquals(1, byTaskId.getThresholdAssessments().size());
        assertEquals("REGRESSION_GATE", byTaskId.getRecommendations().get(0).getCategory());
        assertNotNull(byTaskId.getExecutionSummary());
        assertEquals(4, byTaskId.getExportArtifacts().size());
        assertEquals(BenchmarkReportFormat.HTML, byTaskId.findArtifact(BenchmarkReportFormat.HTML).getFormat());
        assertNotNull(byTaskId.findRawDataArtifact());
        assertEquals(byTaskId.getReportId(), byReportId.getReportId());
        assertNull(repository.findTaskByTaskId("missing-task"));
        assertNull(repository.findReportByTaskId("missing-task"));
        assertNull(repository.findReportByReportId("missing-report"));
    }

    private BenchmarkReport createExecutedReport(BenchmarkTaskModelApplicationService modelService,
                                                 BenchmarkTask task,
                                                 Instant generatedAt) {
        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkIsolatedExecutionResult executionResult =
            new BenchmarkIsolatedExecutionService(properties, modelService).execute(task, generatedAt);
        BenchmarkReport report = modelService.buildExecutedReport(task, executionResult, generatedAt);
        BenchmarkReportResponse reportResponse = modelService.buildReportResponse(report);
        BenchmarkReportRawDataResponse rawDataResponse = modelService.buildRawDataResponse(report);
        java.util.List<com.company.benchmarkengine.domain.benchmark.BenchmarkReportArtifact> artifacts =
            new BenchmarkReportExportService().buildArtifacts(reportResponse);
        artifacts.add(new BenchmarkReportExportService().buildRawDataArtifact(rawDataResponse));
        return report.withExportArtifacts(artifacts);
    }

    private BenchmarkTask createQueuedTask(String taskId, BenchmarkTaskType taskType, Instant submittedAt) {
        return new BenchmarkTaskModelApplicationService().createQueuedTask(baseRequest(taskType), taskId, submittedAt);
    }

    private BenchmarkTaskSubmitRequest baseRequest(BenchmarkTaskType taskType) {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(taskType);
        request.setSqlText("SELECT * FROM orders");
        request.setSqlFingerprint("benchmark-fingerprint");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        request.getTaskContext().setPriority(BenchmarkTaskPriority.NORMAL);
        request.getTaskContext().setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE));
        request.getTaskContext().setConcurrency(Integer.valueOf(16));
        request.getTaskContext().setDurationSeconds(Integer.valueOf(300));
        request.getTaskContext().setRampUpSeconds(Integer.valueOf(30));
        request.getTaskContext().setDatasetSizeLabel("TEN_GB");
        request.getTaskContext().setTemplateId(templateId(taskType));
        request.getTaskContext().setTemplateType(templateType(taskType));
        request.getTaskContext().setTemplateVersion("v2026.04");
        request.getTaskContext().setTestSetId(testSetId(taskType));
        request.getTaskContext().setTestSetSource(testSetSource(taskType));
        request.getTaskContext().setTestSetLabels(
            Arrays.asList(
                label(BenchmarkTestSetLabelType.SCENARIO, taskType.name()),
                label(BenchmarkTestSetLabelType.DOMAIN, taskType == BenchmarkTaskType.COMPARISON ? "ROUTE_GOVERNANCE" : "REGRESSION")
            )
        );
        request.getTaskContext().setTestSetSourceRefs(testSetSourceRefs(taskType));
        request.getTaskContext().setReadonlyRequired(Boolean.TRUE);
        request.getTaskContext().setShadowEnvironmentMode(ShadowEnvironmentMode.REQUIRED);
        request.getTaskContext().setDesensitizationRequirement(DesensitizationRequirement.REQUIRED);
        request.getTaskContext().setThresholds(
            Arrays.asList(
                threshold(
                    BenchmarkThresholdMetric.QPS,
                    BenchmarkThresholdOperator.GREATER_THAN_OR_EQUAL,
                    "120",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Throughput baseline"
                ),
                threshold(
                    BenchmarkThresholdMetric.P99_LATENCY_MS,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "90",
                    BenchmarkThresholdSeverity.CRITICAL,
                    "Tail latency"
                ),
                threshold(
                    BenchmarkThresholdMetric.CPU_USAGE_PERCENT,
                    BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                    "50",
                    BenchmarkThresholdSeverity.WARNING,
                    "CPU pressure"
                )
            )
        );
        if (taskType == BenchmarkTaskType.REGRESSION_GUARD) {
            request.getTaskContext().setTargetEngines(Collections.singletonList(DataSourceTypeEnum.HETU));
            request.getTaskContext().setThresholds(
                Collections.singletonList(
                    threshold(
                        BenchmarkThresholdMetric.P99_LATENCY_MS,
                        BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                        "70",
                        BenchmarkThresholdSeverity.CRITICAL,
                        "Regression gate"
                    )
                )
            );
        }
        return request;
    }

    private BenchmarkTestSetLabelDTO label(BenchmarkTestSetLabelType type, String value) {
        BenchmarkTestSetLabelDTO item = new BenchmarkTestSetLabelDTO();
        item.setType(type);
        item.setValue(value);
        return item;
    }

    private List<BenchmarkSourceReferenceDTO> testSetSourceRefs(BenchmarkTaskType taskType) {
        if (taskType == BenchmarkTaskType.COMPARISON) {
            return Arrays.asList(
                sourceRef(BenchmarkSourceReferenceType.RECOMMENDATION, "rec-001"),
                sourceRef(BenchmarkSourceReferenceType.SQL_FINGERPRINT, "benchmark-fingerprint")
            );
        }
        if (taskType == BenchmarkTaskType.BASELINE) {
            return Arrays.asList(sourceRef(BenchmarkSourceReferenceType.IMPORT_BATCH, "batch-001"));
        }
        return Arrays.asList(
            sourceRef(BenchmarkSourceReferenceType.QUERY_HISTORY, "history-001"),
            sourceRef(BenchmarkSourceReferenceType.REPORT, "report-001")
        );
    }

    private BenchmarkSourceReferenceDTO sourceRef(BenchmarkSourceReferenceType type, String referenceId) {
        BenchmarkSourceReferenceDTO item = new BenchmarkSourceReferenceDTO();
        item.setType(type);
        item.setReferenceId(referenceId);
        return item;
    }

    private String templateId(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return "baseline-snapshot";
            case COMPARISON:
                return "comparison-dual-engine";
            case REGRESSION_GUARD:
                return "regression-guard";
            default:
                return "baseline-snapshot";
        }
    }

    private BenchmarkTemplateType templateType(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return BenchmarkTemplateType.BASELINE_SNAPSHOT;
            case COMPARISON:
                return BenchmarkTemplateType.CROSS_ENGINE_COMPARISON;
            case REGRESSION_GUARD:
                return BenchmarkTemplateType.REGRESSION_GUARD;
            default:
                return BenchmarkTemplateType.BASELINE_SNAPSHOT;
        }
    }

    private String testSetId(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return "set-baseline-capture";
            case COMPARISON:
                return "set-route-comparison";
            case REGRESSION_GUARD:
                return "set-regression-gate";
            default:
                return "set-baseline-capture";
        }
    }

    private BenchmarkTestSetSource testSetSource(BenchmarkTaskType taskType) {
        switch (taskType) {
            case BASELINE:
                return BenchmarkTestSetSource.BATCH_IMPORT;
            case COMPARISON:
                return BenchmarkTestSetSource.RECOMMENDATION_GENERATION;
            case REGRESSION_GUARD:
                return BenchmarkTestSetSource.PARSE_RESULT_GENERATION;
            default:
                return BenchmarkTestSetSource.MANUAL_CURATION;
        }
    }

    private BenchmarkThresholdDTO threshold(BenchmarkThresholdMetric metric,
                                            BenchmarkThresholdOperator operator,
                                            String targetValue,
                                            BenchmarkThresholdSeverity severity,
                                            String description) {
        BenchmarkThresholdDTO item = new BenchmarkThresholdDTO();
        item.setMetric(metric);
        item.setOperator(operator);
        item.setTargetValue(new BigDecimal(targetValue));
        item.setSeverity(severity);
        item.setDescription(description);
        return item;
    }
}
