package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskContextDTO;
import com.company.benchmarkengine.application.controller.dto.BenchmarkTaskSubmitRequest;
import com.company.benchmarkengine.config.BenchmarkArtifactStorageProperties;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkReportFormat;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTask;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.queryexecution.QueryExecutionBenchmarkWorkloadClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadEngineSnapshot;
import com.company.sqlforge.common.queryexecution.QueryExecutionBenchmarkWorkloadResponse;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.Test;

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
        BenchmarkTaskWorker worker = new BenchmarkTaskWorker(
            modelService,
            new BenchmarkIsolatedExecutionService(properties, modelService, workloadClient),
            new BenchmarkReportExportService(),
            new BenchmarkArtifactStorageService(storageProperties, governanceCapabilityClient),
            new BenchmarkGovernanceTraceService(governanceCapabilityClient),
            repository,
            properties,
            new BenchmarkMetricsRecorder(meterRegistry)
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

    private BenchmarkTaskSubmitRequest baseRequest() {
        BenchmarkTaskSubmitRequest request = new BenchmarkTaskSubmitRequest();
        request.setTenantId("tenant-a");
        request.setTaskType(BenchmarkTaskType.BASELINE);
        request.setSqlText("SELECT * FROM orders");
        request.setTaskContext(new BenchmarkTaskContextDTO());
        return request;
    }
}
