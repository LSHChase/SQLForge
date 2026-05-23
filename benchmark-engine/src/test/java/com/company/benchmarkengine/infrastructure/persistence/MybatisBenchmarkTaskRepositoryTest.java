package com.company.benchmarkengine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceManifestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceBundleDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleEvidenceFileDigestDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkSourceReferenceDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkScaleTargetDTO;
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
import com.company.benchmarkengine.domain.benchmark.BenchmarkScaleReadinessStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskError;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPhase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskPriority;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReference;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSet;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCase;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetCaseStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetField;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFieldMapping;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetFileType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabelType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetStatus;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdVerdict;
import com.company.benchmarkengine.domain.benchmark.DesensitizationRequirement;
import com.company.benchmarkengine.domain.benchmark.ShadowEnvironmentMode;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkReportRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTaskRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetCaseRecord;
import com.company.benchmarkengine.infrastructure.persistence.entity.BenchmarkTestSetRecord;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkReportMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTaskMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTestSetCaseMapper;
import com.company.benchmarkengine.infrastructure.persistence.mapper.BenchmarkTestSetMapper;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        MybatisBenchmarkTaskRepository repository = repository(taskMapper, reportMapper);
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
        assertTrue(record.getScaleTargetJson().contains("\"targetConcurrency\":10000"));
        assertTrue(record.getScaleTargetJson().contains("\"targetDailyQueryVolume\":10000000"));
        assertTrue(record.getScaleTargetJson().contains("\"concurrencyProofRef\":\"prod-run-20260518/concurrency.log\""));
        assertTrue(record.getScaleTargetJson().contains("\"dailyQueryVolumeProofRef\":\"prod-run-20260518/daily-query-volume.json\""));
        assertTrue(record.getScaleTargetJson().contains("\"environmentId\":\"prod-bi-cn-01\""));
        assertTrue(record.getScaleTargetJson().contains("\"artifactArchiveRef\":\"s3://audit-prod/sqlforge/prod-run-20260518/\""));
        assertTrue(record.getScaleTargetJson().contains("\"evidenceFileDigests\""));
        assertTrue(record.getScaleTargetJson().contains("\"provenance.json\""));
        assertTrue(record.getScaleTargetJson().contains("\"metrics.csv\""));
        assertTrue(record.getScaleTargetJson().contains("\"verificationBundle\""));
        assertTrue(record.getScaleTargetJson().contains("\"observedConcurrency\":10000"));
        assertTrue(record.getScaleTargetJson().contains("\"observedDailyQueryVolume\":10000000"));
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
        assertNotNull(restored.getScaleTarget());
        assertEquals(Integer.valueOf(10000), restored.getScaleTarget().getTargetConcurrency());
        assertEquals("THIRTY_PB", restored.getScaleTarget().getTargetDatasetSizeLabel());
        assertEquals(Long.valueOf(10000000L), restored.getScaleTarget().getTargetDailyQueryVolume());
        assertEquals("TARGET_DECLARED_UNVERIFIED", restored.getScaleTarget().getEvidenceStatus());
        assertNotNull(restored.getScaleTarget().getEvidenceManifest());
        assertEquals("prod-run-20260518/data-layout-30pb.json",
            restored.getScaleTarget().getEvidenceManifest().getDataLayoutProofRef());
        assertEquals("prod-run-20260518/daily-query-volume.json",
            restored.getScaleTarget().getEvidenceManifest().getDailyQueryVolumeProofRef());
        assertEquals("prod-bi-cn-01",
            restored.getScaleTarget().getEvidenceManifest().getEnvironmentId());
        assertEquals("UNVERIFIED", restored.getScaleTarget().getEvidenceManifest().getExternalVerificationStatus());
        assertNotNull(restored.getScaleTarget().getEvidenceManifest().getVerificationBundle());
        assertEquals(Long.valueOf(10000000L),
            restored.getScaleTarget().getEvidenceManifest().getVerificationBundle().getObservedDailyQueryVolume());
        assertEquals(Long.valueOf(128L),
            restored.getScaleTarget().getEvidenceManifest().getEvidenceFileDigests().get("metrics.csv").getSizeBytes());
        assertEquals(Long.valueOf(30000000000000000L),
            restored.getScaleTarget().getEvidenceManifest().getVerificationBundle().getObservedDatasetSizeBytes());
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

        MybatisBenchmarkTaskRepository repository = repository(taskMapper, reportMapper);
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

        MybatisBenchmarkTaskRepository repository = repository(taskMapper, reportMapper);
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
        assertTrue(record.getExecutionSummaryJson().contains("\"scaleReadiness\""));
        assertTrue(record.getExecutionSummaryJson().contains("\"observedQueueWaitMs\":5000"));
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
        assertNotNull(byTaskId.getExecutionSummary().getScaleReadiness());
        assertEquals(
            BenchmarkScaleReadinessStatus.NOT_PROVEN,
            byTaskId.getExecutionSummary().getScaleReadiness().getReadinessStatus()
        );
        assertEquals(
            new BigDecimal("5000"),
            byTaskId.getExecutionSummary().getScaleReadiness().getObservedQueueWaitMs()
        );
        assertTrue(byTaskId.getExecutionSummary().getScaleReadiness().getMissingEvidence().toString()
            .contains("targetConcurrencyCovered"));
        assertEquals(4, byTaskId.getExportArtifacts().size());
        assertEquals(BenchmarkReportFormat.HTML, byTaskId.findArtifact(BenchmarkReportFormat.HTML).getFormat());
        assertNotNull(byTaskId.findRawDataArtifact());
        assertEquals(byTaskId.getReportId(), byReportId.getReportId());
        assertNull(repository.findTaskByTaskId("missing-task"));
        assertNull(repository.findReportByTaskId("missing-task"));
        assertNull(repository.findReportByReportId("missing-report"));
    }

    @Test
    void shouldPersistAndRestoreBenchmarkTestSetAndRejectedCaseEvidence() {
        BenchmarkTaskMapper taskMapper = org.mockito.Mockito.mock(BenchmarkTaskMapper.class);
        BenchmarkReportMapper reportMapper = org.mockito.Mockito.mock(BenchmarkReportMapper.class);
        BenchmarkTestSetMapper testSetMapper = org.mockito.Mockito.mock(BenchmarkTestSetMapper.class);
        BenchmarkTestSetCaseMapper testSetCaseMapper = org.mockito.Mockito.mock(BenchmarkTestSetCaseMapper.class);
        AtomicReference<BenchmarkTestSetRecord> storedTestSet = new AtomicReference<BenchmarkTestSetRecord>();
        AtomicReference<List<BenchmarkTestSetCaseRecord>> storedCases =
            new AtomicReference<List<BenchmarkTestSetCaseRecord>>(Collections.<BenchmarkTestSetCaseRecord>emptyList());
        when(testSetMapper.selectByTestSetId("set-db-001")).thenAnswer(invocation -> storedTestSet.get());
        when(testSetCaseMapper.selectByTestSetId("set-db-001")).thenAnswer(invocation -> storedCases.get());
        org.mockito.Mockito.doAnswer(invocation -> {
            storedTestSet.set((BenchmarkTestSetRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(testSetMapper).insert(any(BenchmarkTestSetRecord.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            storedTestSet.set((BenchmarkTestSetRecord) invocation.getArgument(0));
            return Integer.valueOf(1);
        }).when(testSetMapper).update(any(BenchmarkTestSetRecord.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            storedCases.set(new java.util.ArrayList<BenchmarkTestSetCaseRecord>());
            return Integer.valueOf(2);
        }).when(testSetCaseMapper).deleteByTestSetId("set-db-001");
        org.mockito.Mockito.doAnswer(invocation -> {
            List<BenchmarkTestSetCaseRecord> snapshot = new java.util.ArrayList<BenchmarkTestSetCaseRecord>(storedCases.get());
            snapshot.add((BenchmarkTestSetCaseRecord) invocation.getArgument(0));
            storedCases.set(snapshot);
            return Integer.valueOf(1);
        }).when(testSetCaseMapper).insert(any(BenchmarkTestSetCaseRecord.class));

        MybatisBenchmarkTaskRepository repository =
            new MybatisBenchmarkTaskRepository(taskMapper, reportMapper, testSetMapper, testSetCaseMapper);
        BenchmarkTestSet testSet = new BenchmarkTestSet(
            "set-db-001",
            "tenant-a",
            "route-governance-import",
            "comparison-dual-engine",
            BenchmarkTemplateType.CROSS_ENGINE_COMPARISON,
            "v2026.04",
            BenchmarkTestSetSource.BATCH_IMPORT,
            BenchmarkTestSetStatus.PARTIAL_READY,
            Integer.valueOf(2),
            Integer.valueOf(1),
            Integer.valueOf(1),
            BenchmarkTestSetFileType.CSV,
            "comparison.csv",
            "import-batch-001",
            Arrays.asList(
                new BenchmarkTestSetFieldMapping(BenchmarkTestSetField.CASE_NAME, "case_name"),
                new BenchmarkTestSetFieldMapping(BenchmarkTestSetField.SQL_TEXT, "sql_text")
            ),
            Arrays.asList(
                new com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetLabel(
                    BenchmarkTestSetLabelType.SCENARIO,
                    "COMPARISON"
                )
            ),
            Arrays.asList(
                new BenchmarkSourceReference(BenchmarkSourceReferenceType.IMPORT_BATCH, "import-batch-001")
            ),
            Arrays.asList(
                new BenchmarkTestSetCase(
                    "case-001",
                    "set-db-001",
                    Integer.valueOf(1),
                    Integer.valueOf(2),
                    "primary",
                    "SELECT * FROM orders",
                    "fp-001",
                    "ds-a",
                    "report-001",
                    Arrays.asList("comparison", "route"),
                    "{\"bizDate\":\"2026-04-22\"}",
                    BenchmarkTestSetCaseStatus.ACCEPTED,
                    null,
                    "{\"sql_text\":\"SELECT * FROM orders\"}"
                ),
                new BenchmarkTestSetCase(
                    "case-002",
                    "set-db-001",
                    Integer.valueOf(2),
                    Integer.valueOf(3),
                    "rejected",
                    "DELETE FROM orders",
                    "fp-002",
                    "ds-a",
                    "report-002",
                    Collections.singletonList("unsafe"),
                    null,
                    BenchmarkTestSetCaseStatus.REJECTED,
                    "sqlText contains write operations",
                    "{\"sql_text\":\"DELETE FROM orders\"}"
                )
            ),
            "user-001",
            Instant.parse("2026-04-22T07:00:00Z"),
            Instant.parse("2026-04-22T07:05:00Z")
        );

        repository.saveTestSet(testSet);

        BenchmarkTestSetRecord record = storedTestSet.get();
        assertNotNull(record);
        assertEquals("set-db-001", record.getTestSetId());
        assertEquals("tenant-a", record.getTenantId());
        assertEquals("comparison-dual-engine", record.getTemplateId());
        assertEquals("PARTIAL_READY", record.getStatus());
        assertEquals("CSV", record.getFileType());
        assertTrue(record.getFieldMappingsJson().contains("case_name"));
        assertTrue(record.getTestSetSourceRefsJson().contains("IMPORT_BATCH"));
        assertEquals(2, storedCases.get().size());
        assertEquals("ACCEPTED", storedCases.get().get(0).getStatus());
        assertEquals("REJECTED", storedCases.get().get(1).getStatus());
        assertTrue(storedCases.get().get(0).getTagsJson().contains("comparison"));

        BenchmarkTestSet restored = repository.findTestSetByTestSetId("set-db-001");

        assertNotNull(restored);
        assertEquals(BenchmarkTestSetStatus.PARTIAL_READY, restored.getStatus());
        assertEquals(BenchmarkTestSetFileType.CSV, restored.getFileType());
        assertEquals(2, restored.getCases().size());
        assertEquals(BenchmarkTestSetCaseStatus.ACCEPTED, restored.getCases().get(0).getStatus());
        assertEquals(BenchmarkTestSetCaseStatus.REJECTED, restored.getCases().get(1).getStatus());
        assertEquals("sqlText contains write operations", restored.getCases().get(1).getRejectionReason());
        assertEquals(2, restored.getCases().get(0).getTags().size());
        assertEquals("comparison", restored.getCases().get(0).getTags().get(0));
        assertEquals("import-batch-001", restored.getTestSetSourceRefs().get(0).getReferenceId());
        verify(testSetMapper).insert(any(BenchmarkTestSetRecord.class));
        verify(testSetCaseMapper).deleteByTestSetId("set-db-001");
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

    private MybatisBenchmarkTaskRepository repository(BenchmarkTaskMapper taskMapper, BenchmarkReportMapper reportMapper) {
        return new MybatisBenchmarkTaskRepository(
            taskMapper,
            reportMapper,
            org.mockito.Mockito.mock(BenchmarkTestSetMapper.class),
            org.mockito.Mockito.mock(BenchmarkTestSetCaseMapper.class)
        );
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
        request.getTaskContext().setScaleTarget(productionScaleTarget());
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

    private BenchmarkScaleTargetDTO productionScaleTarget() {
        BenchmarkScaleTargetDTO target = new BenchmarkScaleTargetDTO();
        target.setTargetConcurrency(Integer.valueOf(10000));
        target.setTargetDatasetSizeLabel("THIRTY_PB");
        target.setTargetDailyQueryVolume(Long.valueOf(10000000L));
        target.setTargetComplexityProfile("HIGH_COMPLEXITY_SELECT");
        target.setTargetCostEfficiency("minimize-scan-cpu-and-cost-per-query");
        target.setEvidenceManifest(productionEvidenceManifest());
        return target;
    }

    private BenchmarkScaleEvidenceManifestDTO productionEvidenceManifest() {
        BenchmarkScaleEvidenceManifestDTO manifest = new BenchmarkScaleEvidenceManifestDTO();
        manifest.setEvidenceSource("PROD_REPLAY");
        manifest.setConcurrencyProofRef("prod-run-20260518/concurrency.log");
        manifest.setDailyQueryVolumeProofRef("prod-run-20260518/daily-query-volume.json");
        manifest.setDataLayoutProofRef("prod-run-20260518/data-layout-30pb.json");
        manifest.setWorkloadReplayProofRef("prod-run-20260518/replay-window.log");
        manifest.setWorkloadReplayWindow("2026-05-17T00:00Z/2026-05-18T00:00Z");
        manifest.setP95P99MetricProofRef("prod-run-20260518/p95-p99.csv");
        manifest.setScanCpuQueueMetricProofRef("prod-run-20260518/scan-cpu-queue.csv");
        manifest.setCostBillProofRef("prod-run-20260518/cost-bill.csv");
        manifest.setExternalVerificationStatus("UNVERIFIED");
        manifest.setEnvironmentId("prod-bi-cn-01");
        manifest.setEnvironmentType("PRODUCTION");
        manifest.setEvidenceOwner("bi-platform-owner");
        manifest.setArtifactArchiveRef("s3://audit-prod/sqlforge/prod-run-20260518/");
        manifest.setVerifierOperator("benchmark-sre");
        manifest.setEvidenceFileDigests(productionDigests());
        manifest.setVerificationBundle(productionEvidenceBundle());
        return manifest;
    }

    private Map<String, BenchmarkScaleEvidenceFileDigestDTO> productionDigests() {
        Map<String, BenchmarkScaleEvidenceFileDigestDTO> digests =
            new LinkedHashMap<String, BenchmarkScaleEvidenceFileDigestDTO>();
        addDigest(digests, "provenance.json");
        addDigest(digests, "concurrency.json");
        addDigest(digests, "daily-query-volume.json");
        addDigest(digests, "data-layout.json");
        addDigest(digests, "workload-replay.json");
        addDigest(digests, "metrics.csv");
        addDigest(digests, "cost-bill.json");
        return digests;
    }

    private void addDigest(Map<String, BenchmarkScaleEvidenceFileDigestDTO> digests, String fileName) {
        BenchmarkScaleEvidenceFileDigestDTO digest = new BenchmarkScaleEvidenceFileDigestDTO();
        digest.setSha256(repeat("c", 64));
        digest.setSizeBytes(Long.valueOf(128L));
        digests.put(fileName, digest);
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private BenchmarkScaleEvidenceBundleDTO productionEvidenceBundle() {
        BenchmarkScaleEvidenceBundleDTO bundle = new BenchmarkScaleEvidenceBundleDTO();
        bundle.setObservedConcurrency(Integer.valueOf(10000));
        bundle.setObservedDailyQueryVolume(Long.valueOf(10000000L));
        bundle.setObservedDatasetSizeBytes(Long.valueOf(30000000000000000L));
        bundle.setWorkloadReplayDurationHours(new BigDecimal("24"));
        bundle.setP95LatencyMs(new BigDecimal("120"));
        bundle.setP99LatencyMs(new BigDecimal("240"));
        bundle.setScannedBytes(Long.valueOf(9876543210L));
        bundle.setCpuUsagePercent(new BigDecimal("72.5"));
        bundle.setQueueWaitMs(new BigDecimal("8"));
        bundle.setCostBillAmount(new BigDecimal("12345.67"));
        bundle.setCostBillCurrency("USD");
        bundle.setVerifierRef("prod-run-20260518/verifier.json");
        return bundle;
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
