package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.StructureParseIssueVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.domain.task.OptimizationTaskType;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ParseTriggeredRewriteRecommendationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldSubmitRewriteTaskForTargetParseIssueAfterHistoryWrite() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        OptimizationTaskApplicationService taskApplicationService = mock(OptimizationTaskApplicationService.class);
        ParseTriggeredRewriteRecommendationService service = new ParseTriggeredRewriteRecommendationService(
            taskApplicationService,
            new InMemoryAccelerationRecommendationRepository(),
            new SqlOptimizationPipelineService()
        );

        service.triggerAfterHistoryWrite(
            structureParse("SELECT_STAR"),
            request(),
            "PARSE_BATCH",
            "item-001",
            "batch-001"
        );

        ArgumentCaptor<OptimizationTaskSubmitRequest> requestCaptor =
            ArgumentCaptor.forClass(OptimizationTaskSubmitRequest.class);
        verify(taskApplicationService).submitInternalTaskIfAbsent(requestCaptor.capture(), anyString());
        OptimizationTaskSubmitRequest submitted = requestCaptor.getValue();
        assertEquals("tenant-a", submitted.getTenantId());
        assertEquals(OptimizationTaskType.REWRITE, submitted.getTaskType());
        assertEquals("SELECT * FROM orders", submitted.getSqlText());
        assertEquals("parse-history-001", submitted.getTaskContext().getHistoryId());
        assertEquals("parse-task-001", submitted.getTaskContext().getParseTaskId());
        assertEquals("batch-001", submitted.getTaskContext().getBatchId());
        assertEquals("RPT_A", submitted.getTaskContext().getReportCode());
        assertEquals("hetu_main", submitted.getTaskContext().getDatasourceCode());
        assertEquals("SELECT_STAR", submitted.getTaskContext().getIssueScenes().get(0));
    }

    @Test
    void shouldSkipRewriteTaskWhenNoTargetIssueMatched() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        OptimizationTaskApplicationService taskApplicationService = mock(OptimizationTaskApplicationService.class);
        ParseTriggeredRewriteRecommendationService service = new ParseTriggeredRewriteRecommendationService(
            taskApplicationService,
            new InMemoryAccelerationRecommendationRepository(),
            new SqlOptimizationPipelineService()
        );

        service.triggerAfterHistoryWrite(
            structureParse("ORDER_BY_WITHOUT_LIMIT"),
            request(),
            "STRUCTURE_PARSE",
            "parse-task-001",
            null
        );

        verify(taskApplicationService, never()).submitInternalTaskIfAbsent(any(), anyString());
    }

    private StructureParseResponseVO structureParse(String issueCode) {
        StructureParseIssueVO issue = new StructureParseIssueVO();
        issue.setIssueCode(issueCode);
        StructureParseResponseVO response = new StructureParseResponseVO();
        response.setParseTaskId("parse-task-001");
        response.setHistoryId("parse-history-001");
        response.setHistoryPersisted(Boolean.TRUE);
        response.setSyntaxStatus("VALID");
        response.setSqlFingerprint("fp-001");
        response.setIssues(Arrays.asList(issue));
        response.setRiskTags(Arrays.asList(issueCode));
        return response;
    }

    private StructureParseRequest request() {
        StructureParseRequest request = new StructureParseRequest();
        request.setSqlText("SELECT * FROM orders");
        request.setDatasourceCode("hetu_main");
        request.setCommentContext(new java.util.LinkedHashMap<String, Object>());
        request.getCommentContext().put("report_code", "RPT_A");
        return request;
    }
}
