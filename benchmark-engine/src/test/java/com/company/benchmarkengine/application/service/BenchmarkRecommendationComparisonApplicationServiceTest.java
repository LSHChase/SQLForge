package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkRecommendationComparisonCreateRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkRecommendationComparisonResponse;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTaskStatusResponse;
import com.company.benchmarkengine.config.BenchmarkTaskExecutionProperties;
import com.company.benchmarkengine.config.BenchmarkTaskQueueProperties;
import com.company.benchmarkengine.domain.benchmark.BenchmarkRecommendationSqlRole;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTaskType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationAccelerationRecommendation;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationRecommendationClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BenchmarkRecommendationComparisonApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateRecommendationGeneratedTestSetAndSubmitComparisonTask() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SqlOptimizationRecommendationClient recommendationClient = mock(SqlOptimizationRecommendationClient.class);
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskApplicationService benchmarkTaskApplicationService = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            repository,
            governanceCapabilityClient,
            new BenchmarkMetricsRecorder(new SimpleMeterRegistry()),
            queueService(repository)
        );
        BenchmarkRecommendationComparisonApplicationService service = new BenchmarkRecommendationComparisonApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            repository,
            benchmarkTaskApplicationService,
            governanceCapabilityClient,
            recommendationClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(recommendationClient.getRecommendation("rec-001")).thenReturn(safeRecommendation());

        BenchmarkRecommendationComparisonResponse response = service.submitComparisonBenchmark("rec-001", baseRequest());

        assertEquals("rec-001", response.getRecommendationId());
        assertEquals(BenchmarkRecommendationSqlRole.RECOMMENDED_SQL, response.getBenchmarkSqlRole());
        assertEquals(BenchmarkTestSetSource.RECOMMENDATION_GENERATION, response.getTestSet().getTestSetSource());
        assertEquals(Integer.valueOf(2), response.getTestSet().getTotalCases());
        assertEquals("QUEUED", response.getBenchmarkTask().getStatus().name());
        assertNotNull(repository.findTestSetByTestSetId(response.getTestSet().getTestSetId()));

        BenchmarkTaskStatusResponse status = benchmarkTaskApplicationService.getTaskStatus(response.getBenchmarkTask().getTaskId());
        assertEquals(BenchmarkTaskType.COMPARISON, status.getTaskType());
        assertEquals(BenchmarkTestSetSource.RECOMMENDATION_GENERATION, status.getTestSetSource());
        assertTrue(status.getTestSetSourceRefs().stream().anyMatch(ref -> ref.getType() == BenchmarkSourceReferenceType.RECOMMENDATION));
        verify(governanceCapabilityClient, org.mockito.Mockito.atLeast(2)).writeAudit(any());
    }

    @Test
    void shouldRejectUnsafeRecommendationSqlBeforeCreatingBenchmarkTask() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SqlOptimizationRecommendationClient recommendationClient = mock(SqlOptimizationRecommendationClient.class);
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkTaskApplicationService benchmarkTaskApplicationService = new BenchmarkTaskApplicationService(
            new BenchmarkTaskModelApplicationService(),
            repository,
            governanceCapabilityClient,
            new BenchmarkMetricsRecorder(new SimpleMeterRegistry()),
            queueService(repository)
        );
        BenchmarkRecommendationComparisonApplicationService service = new BenchmarkRecommendationComparisonApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            repository,
            benchmarkTaskApplicationService,
            governanceCapabilityClient,
            recommendationClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        when(recommendationClient.getRecommendation("rec-unsafe")).thenReturn(unsafeRecommendation());

        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(
            BizException.class,
            () -> service.submitComparisonBenchmark("rec-unsafe", baseRequest())
        );

        assertTrue(ex.getMessage().contains("recommendedSqlText"));
        verify(governanceCapabilityClient).writeAudit(any());
    }

    private BenchmarkRecommendationComparisonCreateRequest baseRequest() {
        BenchmarkRecommendationComparisonCreateRequest request = new BenchmarkRecommendationComparisonCreateRequest();
        request.setTenantId("tenant-a");
        request.setTargetEngines(Arrays.asList(DataSourceTypeEnum.HETU, DataSourceTypeEnum.HIVE));
        request.setBenchmarkSqlRole(BenchmarkRecommendationSqlRole.RECOMMENDED_SQL);
        request.setConcurrency(Integer.valueOf(12));
        request.setDurationSeconds(Integer.valueOf(180));
        request.setRampUpSeconds(Integer.valueOf(20));
        request.setDatasetSizeLabel("TEN_GB");
        return request;
    }

    private SqlOptimizationAccelerationRecommendation safeRecommendation() {
        SqlOptimizationAccelerationRecommendation recommendation = new SqlOptimizationAccelerationRecommendation();
        recommendation.setRecommendationId("rec-001");
        recommendation.setTenantId("tenant-a");
        recommendation.setRecommendationType("REWRITE");
        recommendation.setHistoryId("history-001");
        recommendation.setParseTaskId("parse-001");
        recommendation.setSqlFingerprint("fp-source-001");
        recommendation.setSourceSqlText("SELECT * FROM orders");
        recommendation.setRecommendedSqlText("SELECT order_id, total_amount FROM orders");
        recommendation.setTargetEngine("HIVE");
        recommendation.setTargetDatasource("HIVE");
        recommendation.setReportCode("report-001");
        recommendation.setLogicalObjectKey("agg.orders");
        recommendation.setExpectedGain("Lower scanned rows");
        recommendation.setBenefitLevel("HIGH");
        recommendation.setRiskLevel("LOW");
        recommendation.setStatus("RECOMMENDED");
        recommendation.setRequiresDispatch(Boolean.FALSE);
        return recommendation;
    }

    private SqlOptimizationAccelerationRecommendation unsafeRecommendation() {
        SqlOptimizationAccelerationRecommendation recommendation = safeRecommendation();
        recommendation.setRecommendationId("rec-unsafe");
        recommendation.setRecommendedSqlText("INSERT INTO agg_orders SELECT * FROM orders");
        return recommendation;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }

    private BenchmarkTaskQueueService queueService(InMemoryBenchmarkTaskRepository repository) {
        BenchmarkTaskQueueProperties queueProperties = new BenchmarkTaskQueueProperties();
        BenchmarkTaskExecutionProperties executionProperties = new BenchmarkTaskExecutionProperties();
        executionProperties.setQueueVisibilityDelayMs(0L);
        return new BenchmarkTaskQueueService(repository, queueProperties, executionProperties);
    }
}
