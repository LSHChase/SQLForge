package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.application.controller.dto.BenchmarkThresholdDTO;
import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.config.BenchmarkTaskQueueProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdMetric;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdOperator;
import com.company.benchmarkengine.domain.benchmark.BenchmarkThresholdSeverity;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.queryexecution.QueryExecutionBenchmarkWorkloadClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadEngineSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;

class BenchmarkTaskWorkerTest {

    @Test
    void shouldAdvanceQueuedTaskAndGenerateReport() {
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = modelService.createQueuedTask(baseRequest(), "benchmark-task-async-001", Instant.now().minusSeconds(1L));
        repository.saveTask(task);

        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkArtifactStorageProperties storageProperties = new BenchmarkArtifactStorageProperties();
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        QueryExecutionBenchmarkWorkloadClient workloadClient = mock(QueryExecutionBenchmarkWorkloadClient.class);
        QueryExecutionBenchmarkWorkloadEngineSnapshot engineSnapshot = new QueryExecutionBenchmarkWorkloadEngineSnapshot();
        engineSnapshot.setTargetEngine(DataSourceTypeEnum.HETU);
        engineSnapshot.setWorkloadSource("QUERY_EXECUTION_SYNC");
        engineSnapshot.setExecutionMode("CLIENT");
        engineSnapshot.setElapsedMs(Long.valueOf(42L));
        engineSnapshot.setScannedRows(Long.valueOf(1024L));
        engineSnapshot.setRowCount(Integer.valueOf(2));
        engineSnapshot.setWorkloadDigest("qe-digest-001");
        QueryExecutionBenchmarkWorkloadResponse workloadResponse = new QueryExecutionBenchmarkWorkloadResponse();
        workloadResponse.setWorkloadDigest("aggregate-qe-digest");
        workloadResponse.setWorkloadSource("LIVE_WITH_COMPENSATED_REPLAY");
        workloadResponse.setBackfillApplied(true);
        workloadResponse.setCompensationApplied(true);
        workloadResponse.setCompensationStrategy("PRIMARY_LIVE_ENGINE_REPLAY");
        workloadResponse.setImplementationStage("BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE");
        workloadResponse.setEngineSnapshots(Collections.singletonList(engineSnapshot));
        org.mockito.Mockito.when(workloadClient.captureWorkload(org.mockito.ArgumentMatchers.any())).thenReturn(workloadResponse);
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        BenchmarkTaskQueueService queueService = new BenchmarkTaskQueueService(
            repository,
            new BenchmarkTaskQueueProperties(),
            properties
        );
        BenchmarkTaskWorker worker = new BenchmarkTaskWorker(
            modelService,
            new BenchmarkIsolatedExecutionService(properties, modelService, workloadClient),
            new BenchmarkReportExportService(),
            new BenchmarkArtifactStorageService(storageProperties, governanceCapabilityClient),
            new BenchmarkGovernanceTraceService(governanceCapabilityClient),
            new BenchmarkRegressionAlertService(governanceCapabilityClient),
            repository,
            properties,
            new BenchmarkMetricsRecorder(meterRegistry),
            queueService
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findTaskByTaskId("benchmark-task-async-001").getStatus().name());
        assertEquals("report-benchmark-task-async-001", repository.findTaskByTaskId("benchmark-task-async-001").getReportId());
        assertEquals("tenant-a", repository.findReportByTaskId("benchmark-task-async-001").getTenantId());
        assertNotNull(repository.findReportByTaskId("benchmark-task-async-001").getExecutionSummary());
        assertEquals(
            "QUERY_EXECUTION_WORKLOAD_ORCHESTRATED_REPLAY",
            repository.findReportByTaskId("benchmark-task-async-001").getExecutionSummary().getExecutionMode()
        );
        assertEquals(
            "aggregate-qe-digest",
            repository.findReportByTaskId("benchmark-task-async-001").getExecutionSummary().getWorkloadDigest()
        );
        assertEquals(
            true,
            repository.findReportByTaskId("benchmark-task-async-001").getExecutionSummary().getPhaseNotes()
                .contains("queryExecutionCompensationApplied=true")
        );
        assertNotNull(repository.findReportByTaskId("benchmark-task-async-001").findArtifact(BenchmarkReportFormat.PDF));
        assertNotNull(repository.findReportByTaskId("benchmark-task-async-001").findRawDataArtifact());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.tasks.terminal").tags(
            "task_type", "BASELINE",
            "result_status", "SUCCEEDED"
        ).counter().count());
        assertEquals(1.0D, meterRegistry.get("sqlforge.benchmark.engine.reports.generated").tags(
            "task_type", "BASELINE"
        ).counter().count());
    }

    @Test
    void shouldConsumeExternalFileQueueAndCleanupCarrierMessage(@TempDir Path tempDir) throws Exception {
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTask task = modelService.createQueuedTask(baseRequest(), "benchmark-task-async-queue-001", Instant.now().minusSeconds(1L));
        repository.saveTask(task);

        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkTaskQueueProperties queueProperties = new BenchmarkTaskQueueProperties();
        queueProperties.setMode("external-file-queue");
        queueProperties.setExternalFileQueueDir(tempDir.resolve("external-file-queue").toString());
        BenchmarkTaskQueueService queueService = new BenchmarkTaskQueueService(repository, queueProperties, properties);
        queueService.dispatch(task);

        BenchmarkArtifactStorageProperties storageProperties = new BenchmarkArtifactStorageProperties();
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        QueryExecutionBenchmarkWorkloadClient workloadClient = mock(QueryExecutionBenchmarkWorkloadClient.class);
        QueryExecutionBenchmarkWorkloadResponse workloadResponse = new QueryExecutionBenchmarkWorkloadResponse();
        workloadResponse.setWorkloadDigest("aggregate-qe-digest");
        workloadResponse.setWorkloadSource("LIVE_WITH_COMPENSATED_REPLAY");
        workloadResponse.setImplementationStage("BENCHMARK_WORKLOAD_ORCHESTRATION_BASELINE");
        workloadResponse.setEngineSnapshots(Collections.<QueryExecutionBenchmarkWorkloadEngineSnapshot>emptyList());
        org.mockito.Mockito.when(workloadClient.captureWorkload(org.mockito.ArgumentMatchers.any())).thenReturn(workloadResponse);
        BenchmarkTaskWorker worker = new BenchmarkTaskWorker(
            modelService,
            new BenchmarkIsolatedExecutionService(properties, modelService, workloadClient),
            new BenchmarkReportExportService(),
            new BenchmarkArtifactStorageService(storageProperties, governanceCapabilityClient),
            new BenchmarkGovernanceTraceService(governanceCapabilityClient),
            new BenchmarkRegressionAlertService(governanceCapabilityClient),
            repository,
            properties,
            new BenchmarkMetricsRecorder(new SimpleMeterRegistry()),
            queueService
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findTaskByTaskId("benchmark-task-async-queue-001").getStatus().name());
        assertEquals(0L, Files.list(tempDir.resolve("external-file-queue")).count());
        assertEquals(
            "external-file-queue",
            BenchmarkTaskQueueService.resolveQueueEvidence(
                repository.findTaskByTaskId("benchmark-task-async-queue-001")
            ).getQueueMode()
        );
    }

    @Test
    void shouldAttachRegressionAlertLinkageForFailedGuard() {
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskModelApplicationService modelService = new BenchmarkTaskModelApplicationService();
        BenchmarkTaskSubmitRequest request = baseRequest();
        request.setTaskType(BenchmarkTaskType.REGRESSION_GUARD);
        request.getTaskContext().setTargetEngines(Collections.singletonList(DataSourceTypeEnum.HETU));
        request.getTaskContext().setThresholds(Arrays.asList(
            threshold(
                BenchmarkThresholdMetric.P99_LATENCY_MS,
                BenchmarkThresholdOperator.LESS_THAN_OR_EQUAL,
                "70",
                BenchmarkThresholdSeverity.CRITICAL,
                "P99 regression gate"
            )
        ));
        BenchmarkTask task = modelService.createQueuedTask(request, "benchmark-task-regression-001", Instant.now().minusSeconds(1L));
        repository.saveTask(task);

        BenchmarkTaskExecutionProperties properties = new BenchmarkTaskExecutionProperties();
        properties.setQueueVisibilityDelayMs(0L);
        properties.setPhaseDelayMs(0L);
        properties.setIsolationSampleCount(4);
        properties.setIsolationWorkIterations(24);
        BenchmarkArtifactStorageProperties storageProperties = new BenchmarkArtifactStorageProperties();
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        GovernanceBenchmarkRegressionAlertResponse alertResponse = new GovernanceBenchmarkRegressionAlertResponse();
        GovernanceBenchmarkRegressionAlertLinkage linkage = new GovernanceBenchmarkRegressionAlertLinkage();
        linkage.setAlertId("alert-benchmark-regression-001");
        linkage.setAlertType("BENCHMARK_REGRESSION_FAILED");
        linkage.setAlertLevel("HIGH");
        linkage.setAlertStatus("OPEN");
        linkage.setNotifyStatus("SIMULATED_NOTIFIED");
        linkage.setSummary("Regression guard hit 1 threshold(s): failed=1, warning=0.");
        linkage.setDetailPath("");
        linkage.setLinkageMode("EMITTED");
        alertResponse.setAlertTriggered(Boolean.TRUE);
        alertResponse.setAlertLinkages(Collections.singletonList(linkage));
        org.mockito.Mockito.when(
            governanceCapabilityClient.emitBenchmarkRegressionAlert(org.mockito.ArgumentMatchers.any())
        ).thenReturn(alertResponse);
        BenchmarkTaskQueueService queueService = new BenchmarkTaskQueueService(
            repository,
            new BenchmarkTaskQueueProperties(),
            properties
        );
        BenchmarkTaskWorker worker = new BenchmarkTaskWorker(
            modelService,
            new BenchmarkIsolatedExecutionService(properties, modelService),
            new BenchmarkReportExportService(),
            new BenchmarkArtifactStorageService(storageProperties, governanceCapabilityClient),
            new BenchmarkGovernanceTraceService(governanceCapabilityClient),
            new BenchmarkRegressionAlertService(governanceCapabilityClient),
            repository,
            properties,
            new BenchmarkMetricsRecorder(new SimpleMeterRegistry()),
            queueService
        );

        worker.processQueuedTasks();

        assertEquals("SUCCEEDED", repository.findTaskByTaskId("benchmark-task-regression-001").getStatus().name());
        assertEquals(1, repository.findReportByTaskId("benchmark-task-regression-001").getAlertLinkages().size());
        assertEquals(
            "alert-benchmark-regression-001",
            repository.findReportByTaskId("benchmark-task-regression-001").getAlertLinkages().get(0).getAlertId()
        );
    }

    private BenchmarkTaskSubmitRequest baseRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }

    private BenchmarkThresholdDTO threshold(BenchmarkThresholdMetric metric,
                                            BenchmarkThresholdOperator operator,
                                            String targetValue,
                                            BenchmarkThresholdSeverity severity,
                                            String description) {
        BenchmarkThresholdDTO threshold = new BenchmarkThresholdDTO();
        threshold.setMetric(metric);
        threshold.setOperator(operator);
        threshold.setTargetValue(new BigDecimal(targetValue));
        threshold.setSeverity(severity);
        threshold.setDescription(description);
        return threshold;
    }
}
