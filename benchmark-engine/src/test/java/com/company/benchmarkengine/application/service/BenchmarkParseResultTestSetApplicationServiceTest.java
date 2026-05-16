package com.company.benchmarkengine.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.benchmarkengine.application.controller.dto.BenchmarkParseResultTestSetCreateRequest;
import com.company.benchmarkengine.application.controller.vo.BenchmarkTestSetResponse;
import com.company.benchmarkengine.domain.benchmark.BenchmarkSourceReferenceType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTemplateType;
import com.company.benchmarkengine.domain.benchmark.BenchmarkTestSetSource;
import com.company.benchmarkengine.infrastructure.governance.GovernanceCapabilityClient;
import com.company.benchmarkengine.infrastructure.repository.InMemoryBenchmarkTaskRepository;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationCombinedParseStatus;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseBatchItem;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseBatchStatus;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseResultClient;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationParseSqlIssueStatistic;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseIssue;
import com.company.benchmarkengine.infrastructure.sqloptimization.SqlOptimizationStructureParseStatus;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BenchmarkParseResultTestSetApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldGenerateTestSetFromImportantUrgentParseBatchAndPreserveRejectedEvidence() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SqlOptimizationParseResultClient parseResultClient = mock(SqlOptimizationParseResultClient.class);
        InMemoryBenchmarkTaskRepository repository = new InMemoryBenchmarkTaskRepository();
        BenchmarkParseResultTestSetApplicationService service = new BenchmarkParseResultTestSetApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            repository,
            governanceCapabilityClient,
            parseResultClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        when(parseResultClient.getParseBatch("batch-001")).thenReturn(batchStatus());
        when(parseResultClient.getImportantUrgentSqls()).thenReturn(batchStatistics());

        BenchmarkTestSetResponse created = service.createFromParseResults(batchRequest());

        assertEquals(BenchmarkTestSetSource.PARSE_RESULT_GENERATION, created.getTestSetSource());
        assertEquals("PARTIAL_READY", created.getStatus().name());
        assertEquals(Integer.valueOf(2), created.getTotalCases());
        assertEquals(Integer.valueOf(1), created.getAcceptedCases());
        assertEquals(Integer.valueOf(1), created.getRejectedCases());
        assertTrue(created.getTestSetSourceRefs().stream().anyMatch(ref -> ref.getType() == BenchmarkSourceReferenceType.PARSE_TASK));
        assertTrue(created.getTestSetSourceRefs().stream().anyMatch(ref -> ref.getType() == BenchmarkSourceReferenceType.REPORT));
        assertEquals("report-002", created.getCases().get(1).getCaseName());
        assertTrue(created.getCases().get(1).getRejectionReason().contains("只读压测边界"));
        assertEquals(created.getTestSetId(), repository.findTestSetByTestSetId(created.getTestSetId()).getTestSetId());

        verify(governanceCapabilityClient).assertAuthorization(
            org.mockito.Mockito.eq("tenant-a"),
            org.mockito.Mockito.any(),
            org.mockito.Mockito.eq("BENCHMARK_ENGINE_TEST_SET"),
            org.mockito.Mockito.anyString(),
            org.mockito.Mockito.eq("BENCHMARK_TEST_SET_PARSE_GENERATE")
        );
        verify(governanceCapabilityClient).writeAudit(any());
    }

    @Test
    void shouldGenerateSingleParseTaskTestSetWhenIssueSceneMatches() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SqlOptimizationParseResultClient parseResultClient = mock(SqlOptimizationParseResultClient.class);
        BenchmarkParseResultTestSetApplicationService service = new BenchmarkParseResultTestSetApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            governanceCapabilityClient,
            parseResultClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        when(parseResultClient.getCombinedParseStatus("parse-001")).thenReturn(combinedParseStatus());

        BenchmarkTestSetResponse created = service.createFromParseResults(singleParseTaskRequest(Collections.singletonList("comparison")));

        assertEquals("READY", created.getStatus().name());
        assertEquals(Integer.valueOf(1), created.getAcceptedCases());
        assertTrue(created.getTestSetSourceRefs().stream().anyMatch(ref -> ref.getType() == BenchmarkSourceReferenceType.SQL_FINGERPRINT));
        assertTrue(created.getCases().get(0).getTags().contains("IMPORTANT"));
    }

    @Test
    void shouldRejectSingleParseTaskWhenFiltersEliminateAllCandidates() {
        GovernanceCapabilityClient governanceCapabilityClient = mockGovernanceClient();
        SqlOptimizationParseResultClient parseResultClient = mock(SqlOptimizationParseResultClient.class);
        BenchmarkParseResultTestSetApplicationService service = new BenchmarkParseResultTestSetApplicationService(
            new BenchmarkTestSetModelApplicationService(),
            new InMemoryBenchmarkTaskRepository(),
            governanceCapabilityClient,
            parseResultClient
        );
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        when(parseResultClient.getCombinedParseStatus("parse-001")).thenReturn(combinedParseStatus());

        BizException ex = org.junit.jupiter.api.Assertions.assertThrows(
            BizException.class,
            () -> service.createFromParseResults(singleParseTaskRequest(Collections.singletonList("route")))
        );

        assertTrue(ex.getMessage().contains("没有解析结果匹配"));
    }

    private BenchmarkParseResultTestSetCreateRequest batchRequest() {
        BenchmarkParseResultTestSetCreateRequest request = new BenchmarkParseResultTestSetCreateRequest();
        request.setTenantId("tenant-a");
        request.setTestSetName("parse-batch-generated");
        request.setTemplateId("comparison-dual-engine");
        request.setTemplateType(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON);
        request.setTemplateVersion("v2026.04");
        request.setParseBatchId("batch-001");
        request.setIncludeIssueScenes(Collections.singletonList("comparison"));
        return request;
    }

    private BenchmarkParseResultTestSetCreateRequest singleParseTaskRequest(List<String> issueScenes) {
        BenchmarkParseResultTestSetCreateRequest request = new BenchmarkParseResultTestSetCreateRequest();
        request.setTenantId("tenant-a");
        request.setTestSetName("single-parse-generated");
        request.setTemplateId("comparison-dual-engine");
        request.setTemplateType(BenchmarkTemplateType.CROSS_ENGINE_COMPARISON);
        request.setTemplateVersion("v2026.04");
        request.setParseTaskId("parse-001");
        request.setSqlText("SELECT * FROM orders");
        request.setIncludeIssueScenes(issueScenes);
        return request;
    }

    private SqlOptimizationParseBatchStatus batchStatus() {
        SqlOptimizationParseBatchStatus batch = new SqlOptimizationParseBatchStatus();
        batch.setBatchId("batch-001");
        batch.setTenantId("tenant-a");
        batch.setImportedRecords(Arrays.asList(
            batchItem("item-001", 1, "parse-001", "report-001", "SELECT * FROM orders", "VALID", Arrays.asList("comparison", "route")),
            batchItem("item-002", 2, "parse-002", "report-002", "DELETE FROM orders", "VALID", Collections.singletonList("comparison")),
            batchItem("item-003", 3, "parse-003", "report-003", "SELECT * FROM skip_me", "VALID", Collections.singletonList("route"))
        ));
        return batch;
    }

    private List<SqlOptimizationParseSqlIssueStatistic> batchStatistics() {
        return Arrays.asList(
            statistic("item-001", "batch-001", "parse-001", "report-001", Boolean.TRUE, Boolean.TRUE, Collections.singletonList("comparison")),
            statistic("item-002", "batch-001", "parse-002", "report-002", Boolean.TRUE, Boolean.FALSE, Collections.singletonList("comparison")),
            statistic("item-999", "batch-002", "parse-999", "report-999", Boolean.TRUE, Boolean.TRUE, Collections.singletonList("comparison"))
        );
    }

    private SqlOptimizationCombinedParseStatus combinedParseStatus() {
        SqlOptimizationCombinedParseStatus status = new SqlOptimizationCombinedParseStatus();
        status.setParseTaskId("parse-001");
        status.setStatus("SUCCEEDED");
        SqlOptimizationStructureParseStatus structure = new SqlOptimizationStructureParseStatus();
        structure.setSyntaxStatus("VALID");
        structure.setImportant(Boolean.TRUE);
        structure.setUrgent(Boolean.FALSE);
        structure.setPriorityLevel("P1");
        structure.setIssues(Collections.singletonList(issue("comparison")));
        status.setStructureParse(structure);
        return status;
    }

    private SqlOptimizationParseBatchItem batchItem(String itemId,
                                                    int sequenceNumber,
                                                    String parseTaskId,
                                                    String reportCode,
                                                    String sqlText,
                                                    String syntaxStatus,
                                                    List<String> issueScenes) {
        SqlOptimizationParseBatchItem item = new SqlOptimizationParseBatchItem();
        item.setItemId(itemId);
        item.setSequenceNumber(Integer.valueOf(sequenceNumber));
        item.setParseTaskId(parseTaskId);
        item.setReportCode(reportCode);
        item.setDatasourceCode("HETU");
        item.setSqlText(sqlText);
        item.setStructureSyntaxStatus(syntaxStatus);
        item.setIssueScenes(issueScenes);
        return item;
    }

    private SqlOptimizationParseSqlIssueStatistic statistic(String itemId,
                                                            String batchId,
                                                            String parseTaskId,
                                                            String reportCode,
                                                            Boolean important,
                                                            Boolean urgent,
                                                            List<String> issueScenes) {
        SqlOptimizationParseSqlIssueStatistic item = new SqlOptimizationParseSqlIssueStatistic();
        item.setItemId(itemId);
        item.setBatchId(batchId);
        item.setParseTaskId(parseTaskId);
        item.setReportCode(reportCode);
        item.setDatasourceCode("HETU");
        item.setHighestPriorityLevel(Boolean.TRUE.equals(urgent) ? "P0" : "P1");
        item.setImportant(important);
        item.setUrgent(urgent);
        item.setIssueScenes(issueScenes);
        return item;
    }

    private SqlOptimizationStructureParseIssue issue(String issueScene) {
        SqlOptimizationStructureParseIssue item = new SqlOptimizationStructureParseIssue();
        item.setIssueScene(issueScene);
        item.setImportant(Boolean.TRUE);
        item.setUrgent(Boolean.FALSE);
        item.setPriorityLevel("P1");
        return item;
    }

    private GovernanceCapabilityClient mockGovernanceClient() {
        GovernanceCapabilityClient governanceCapabilityClient = mock(GovernanceCapabilityClient.class);
        doNothing().when(governanceCapabilityClient).assertAuthorization(any(), any(), any(), any(), any());
        doNothing().when(governanceCapabilityClient).writeAudit(any());
        return governanceCapabilityClient;
    }
}
