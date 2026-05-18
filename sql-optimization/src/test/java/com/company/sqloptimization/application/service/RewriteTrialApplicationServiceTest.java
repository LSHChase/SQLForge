package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqloptimization.application.controller.dto.OptimizationTaskSubmitRequest;
import com.company.sqloptimization.application.controller.dto.RewriteTrialBatchRequest;
import com.company.sqloptimization.application.controller.dto.RewriteTrialRequest;
import com.company.sqloptimization.application.controller.vo.RewriteTrialRunVO;
import com.company.sqloptimization.application.controller.vo.RewriteTrialSourceIssueStatisticVO;
import com.company.sqloptimization.domain.batch.ParseBatch;
import com.company.sqloptimization.domain.batch.ParseBatchFileType;
import com.company.sqloptimization.domain.batch.ParseBatchImportMode;
import com.company.sqloptimization.domain.batch.ParseBatchItem;
import com.company.sqloptimization.domain.batch.ParseBatchItemStatus;
import com.company.sqloptimization.domain.batch.ParseBatchSourceType;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchItemRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryParseBatchRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryRewriteTrialRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RewriteTrialApplicationServiceTest {

    private InMemoryRewriteTrialRepository trialRepository;
    private InMemoryParseBatchRepository batchRepository;
    private InMemoryParseBatchItemRepository batchItemRepository;
    private InMemoryAccelerationRecommendationRepository recommendationRepository;
    private OptimizationTaskApplicationService taskApplicationService;
    private RewriteTrialApplicationService service;

    @BeforeEach
    void setUp() {
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);
        trialRepository = new InMemoryRewriteTrialRepository();
        batchRepository = new InMemoryParseBatchRepository();
        batchItemRepository = new InMemoryParseBatchItemRepository();
        recommendationRepository = new InMemoryAccelerationRecommendationRepository();
        taskApplicationService = mock(OptimizationTaskApplicationService.class);
        service = new RewriteTrialApplicationService(
            trialRepository,
            batchRepository,
            batchItemRepository,
            recommendationRepository,
            new SqlOptimizationPipelineService(),
            taskApplicationService
        );
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateSingleTrialRecommendationFromSafeParseProblems() {
        RewriteTrialRequest request = new RewriteTrialRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT count(1) FROM orders WHERE id = 1 AND id = 1 ORDER BY id, id");
        request.setDatasourceCode("hetu_main");
        request.setSourceKind("STRUCTURE_PARSE");
        request.setParseTaskId("parse-task-001");
        request.setParseHistoryId("parse-history-001");

        RewriteTrialRunVO run = service.createTrial(request);

        assertEquals("RECOMMENDED", run.getTrialStatus());
        assertEquals(Integer.valueOf(1), run.getAcceptedCount());
        assertEquals(Integer.valueOf(1), run.getCandidateGeneratedCount());
        assertEquals("RECOMMENDED", run.getItems().get(0).getTrialStatus());
        assertTrue(run.getItems().get(0).getCandidateSql().toUpperCase().contains("COUNT"));
        assertNotNull(run.getItems().get(0).getRecommendationId());
        assertEquals("COUNT_LITERAL_TO_COUNT_STAR",
            run.getItems().get(0).getSourceProblems().get(0).get("issueScene"));
        assertEquals("APPLIED", run.getItems().get(0).getIssueRuleLinks().get(0).get("ruleAction"));

        AccelerationRecommendation recommendation =
            recommendationRepository.findByRecommendationId(run.getItems().get(0).getRecommendationId());
        assertNotNull(recommendation);
        assertFalse(recommendation.getSourceProblems().isEmpty());
        assertFalse(recommendation.getIssueRuleLinks().isEmpty());

        ArgumentCaptor<OptimizationTaskSubmitRequest> submitCaptor =
            ArgumentCaptor.forClass(OptimizationTaskSubmitRequest.class);
        verify(taskApplicationService).submitInternalTaskIfAbsent(submitCaptor.capture(), anyString());
        assertEquals("tenant-a", submitCaptor.getValue().getTenantId());
        assertEquals(Arrays.asList("COUNT_LITERAL_TO_COUNT_STAR", "DEDUPLICATE_WHERE_PREDICATES", "DEDUPLICATE_ORDER_BY_KEYS"),
            submitCaptor.getValue().getTaskContext().getIssueScenes());
    }

    @Test
    void shouldKeepNoSafeRewriteWhenSelectedProblemRequiresMetadata() {
        RewriteTrialRequest request = new RewriteTrialRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT * FROM orders WHERE status = 'PAID'");
        request.setSourceProblems(Collections.singletonList(sourceProblem("SELECT_STAR")));

        RewriteTrialRunVO run = service.createTrial(request);

        assertEquals("NO_SAFE_REWRITE", run.getTrialStatus());
        assertEquals(Integer.valueOf(1), run.getNoSafeRewriteCount());
        assertEquals("NO_SAFE_REWRITE", run.getItems().get(0).getTrialStatus());
        assertNull(run.getItems().get(0).getCandidateSql());
        assertNull(run.getItems().get(0).getRecommendationId());
        assertEquals("REQUIRES_METADATA", run.getItems().get(0).getIssueRuleLinks().get(0).get("trialConclusion"));
    }

    @Test
    void shouldCreateBatchTrialOnlyForValidEligibleItems() {
        batchRepository.save(batch("batch-001"));
        batchItemRepository.save(item("item-001", 1,
            "SELECT count(1) FROM orders WHERE id = 1 AND id = 1", "VALID", ParseBatchItemStatus.SUCCESS));
        batchItemRepository.save(item("item-002", 2,
            "SELECT id FROM orders WHERE id = 1", "VALID", ParseBatchItemStatus.SUCCESS));
        batchItemRepository.save(item("item-003", 3,
            "SELECT FROM", "INVALID", ParseBatchItemStatus.FAILED));

        RewriteTrialBatchRequest request = new RewriteTrialBatchRequest();
        request.setTenantId("tenant-a");
        RewriteTrialRunVO run = service.createBatchTrial("batch-001", request);

        assertEquals(Integer.valueOf(3), run.getTotalCount());
        assertEquals(Integer.valueOf(1), run.getAcceptedCount());
        assertEquals(Integer.valueOf(2), run.getSkippedCount());
        assertEquals(Integer.valueOf(1), run.getRecommendedCount());
        assertEquals(Integer.valueOf(1), run.getCandidateGeneratedCount());
        assertEquals("RECOMMENDED", run.getTrialStatus());
    }

    @Test
    void shouldAggregateRewriteTrialStatisticsBySourceIssue() {
        RewriteTrialRequest candidate = new RewriteTrialRequest();
        candidate.setTenantId("tenant-a");
        candidate.setSqlText("SELECT count(1) FROM orders WHERE id = 1");
        service.createTrial(candidate);

        RewriteTrialRequest noSafe = new RewriteTrialRequest();
        noSafe.setTenantId("tenant-a");
        noSafe.setSqlText("SELECT * FROM orders WHERE status = 'PAID'");
        noSafe.setSourceProblems(Collections.singletonList(sourceProblem("SELECT_STAR")));
        service.createTrial(noSafe);

        assertEquals(Integer.valueOf(2), service.overview().getEligibleSqlCount());
        assertEquals(Integer.valueOf(2), service.overview().getTrialedSqlCount());
        assertEquals(Integer.valueOf(1), service.overview().getCandidateGeneratedCount());
        assertEquals(Integer.valueOf(1), service.overview().getNoSafeRewriteCount());
        assertEquals(Integer.valueOf(1), service.overview().getManualReviewRequiredCount());

        List<RewriteTrialSourceIssueStatisticVO> byIssue = service.bySourceIssue();
        assertEquals(2, byIssue.size());
        assertTrue(containsIssue(byIssue, "COUNT_LITERAL_TO_COUNT_STAR"));
        assertTrue(containsIssue(byIssue, "SELECT_STAR"));
    }

    private Map<String, Object> sourceProblem(String issueScene) {
        Map<String, Object> problem = new LinkedHashMap<String, Object>();
        problem.put("problemType", "ISSUE_SCENE");
        problem.put("issueScene", issueScene);
        problem.put("summary", issueScene);
        return problem;
    }

    private boolean containsIssue(List<RewriteTrialSourceIssueStatisticVO> values, String issueScene) {
        for (RewriteTrialSourceIssueStatisticVO value : values) {
            if (issueScene.equals(value.getSourceIssueScene())) {
                return true;
            }
        }
        return false;
    }

    private ParseBatch batch(String batchId) {
        return ParseBatch.initialize(
            batchId,
            "tenant-a",
            "batch",
            ParseBatchImportMode.TABULAR_FILE,
            ParseBatchSourceType.FILE_UPLOAD,
            ParseBatchFileType.CSV,
            "v1",
            "hetu_main",
            "JSQLPARSER",
            true,
            "operator-001",
            Instant.now()
        );
    }

    private ParseBatchItem item(String itemId,
                                int sequence,
                                String sqlText,
                                String syntaxStatus,
                                ParseBatchItemStatus status) {
        ParseBatchItem item = ParseBatchItem.create(
            itemId,
            "batch-001",
            sequence,
            "RPT_A",
            "Report A",
            "hetu_main",
            "DEV",
            "2026-05-18",
            "P2",
            "owner",
            null,
            sqlText,
            null,
            null,
            null,
            Instant.now()
        );
        item.complete(
            "parse-task-" + sequence,
            syntaxStatus,
            "SKIPPED",
            "SKIPPED",
            status,
            "INVALID".equals(syntaxStatus) ? "syntax invalid" : null,
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            Instant.now()
        );
        item.recordHistory("parse-history-" + sequence, Boolean.TRUE, "SAVED", Instant.now());
        return item;
    }
}
