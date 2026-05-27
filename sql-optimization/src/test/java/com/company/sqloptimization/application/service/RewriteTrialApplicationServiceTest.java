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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        RequestContext.set("tenant-a", "user-001", "request-001", "trace-001", "header", 1L, 2L);
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
        Map<String, Object> productionBenefitGate = nestedMap(recommendation.getExpectedBenefit(), "productionScaleGate");
        assertEquals("EXTERNAL_EVIDENCE_REQUIRED", productionBenefitGate.get("status"));
        assertEquals("PRODUCTION_SCALE_NOT_PROVEN_BY_STATIC_REWRITE", productionBenefitGate.get("claimBoundary"));
        assertEquals("THIRTY_PB", productionBenefitGate.get("targetDatasetSizeLabel"));
        assertTrue(((List<?>) productionBenefitGate.get("requiredEvidence")).contains("VERIFIED_30PB_DATA_LAYOUT"));
        assertEquals("EXTERNAL_EVIDENCE_REQUIRED",
            nestedMap(recommendation.getEstimatedCost(), "productionScaleGate").get("status"));

        ArgumentCaptor<OptimizationTaskSubmitRequest> submitCaptor =
            ArgumentCaptor.forClass(OptimizationTaskSubmitRequest.class);
        verify(taskApplicationService).submitInternalTaskIfAbsent(submitCaptor.capture(), anyString());
        assertEquals("tenant-a", submitCaptor.getValue().getTenantId());
        assertEquals(Arrays.asList("COUNT_LITERAL_TO_COUNT_STAR", "DEDUPLICATE_WHERE_PREDICATES", "DEDUPLICATE_ORDER_BY_KEYS"),
            submitCaptor.getValue().getTaskContext().getIssueScenes());
    }

    @Test
    void shouldCreateRecommendationForReportRewriteTrialSqlFixture() throws Exception {
        String expectedRecommendedSql = readSqlFixture("docs/test01_mv.sql");
        RewriteTrialRequest request = new RewriteTrialRequest();
        request.setTenantId("tenant-a");
        request.setSqlText(readSqlFixture("docs/test01.sql"));
        request.setDatasourceCode("hetu_main");
        request.setSourceKind("STRUCTURE_PARSE");
        request.setParseTaskId("parse-task-test01");
        request.setParseHistoryId("parse-history-test01");

        RewriteTrialRunVO run = service.createTrial(request);

        assertEquals("RECOMMENDED", run.getTrialStatus());
        assertEquals(Integer.valueOf(1), run.getCandidateGeneratedCount());
        assertEquals("RECOMMENDED", run.getItems().get(0).getTrialStatus());
        assertNotNull(run.getItems().get(0).getCandidateSql());
        assertNotNull(run.getItems().get(0).getRecommendationId());
        assertEquals(normalizeExecutableSql(expectedRecommendedSql),
            normalizeExecutableSql(run.getItems().get(0).getCandidateSql()));
        assertTrue(run.getItems().get(0).getCandidateSql().contains("base_100_anchor"));
        assertTrue(run.getItems().get(0).getCandidateSql().contains("report_customer_snapshot"));
        assertTrue(run.getItems().get(0).getCandidateSql().contains("raw_customer_snapshot"));
        assertTrue(containsProblem(run.getItems().get(0).getSourceProblems(),
            "REPORT_REPEATED_SCAN_TO_SNAPSHOT_AGG"));
        assertTrue(containsLink(run.getItems().get(0).getIssueRuleLinks(),
            "REPORT_REPEATED_SCAN_TO_SNAPSHOT_AGG"));
        assertTrue(containsProblem(run.getItems().get(0).getSourceProblems(), "PRECOMPUTE_MV"));

        AccelerationRecommendation recommendation =
            recommendationRepository.findByRecommendationId(run.getItems().get(0).getRecommendationId());
        assertNotNull(recommendation);
        assertEquals(normalizeExecutableSql(expectedRecommendedSql),
            normalizeExecutableSql(recommendation.getRecommendedSqlText()));
        assertTrue(recommendation.getRecommendedSqlText().contains("base_100_anchor"));
        assertTrue(recommendation.getRecommendedSqlText().contains("report_customer_snapshot"));
        assertTrue(recommendation.getRecommendedSqlText().contains("raw_customer_snapshot"));
        assertEquals("NOT_VALIDATED", recommendation.getValidationStatus().name());
        assertFalse(recommendation.isAutoApplyAllowed());
        assertTrue(recommendation.isManualReviewRequired());
        assertEquals("HETU", recommendation.getTargetEngine());
        assertFalse(recommendation.getAccelerationArtifact().isEmpty());
        assertEquals("PRECOMPUTE_MV", recommendation.getAccelerationArtifact().get("rule"));
        assertEquals("HETU", recommendation.getAccelerationArtifact().get("targetEngine"));
        assertEquals(MaterializedViewRecommendationPlanner.SOURCE_AST_IR,
            recommendation.getAccelerationArtifact().get("generationSource"));
        assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("candidateId"))
            .startsWith("mv_candidate_"));
        assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("coverageProof"))
            .contains("MV_COVERAGE_PROOF_ENGINE_V1"));
        assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("explainEvidence"))
            .contains("EXPLAIN_UNAVAILABLE"));
        assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("dynamicSnapshotRewriteEvidence"))
            .contains("DYNAMIC_AST_PROFILE_SNAPSHOT_AGGREGATE"));
        String artifactStatus = String.valueOf(recommendation.getAccelerationArtifact().get("artifactStatus"));
        if ("BLOCKED".equals(artifactStatus)) {
            assertTrue(recommendation.getAccelerationArtifact().get("rewriteSql") == null,
                String.valueOf(recommendation.getAccelerationArtifact()));
        } else {
            assertTrue("GENERATED".equals(artifactStatus) || "REVIEW_REQUIRED".equals(artifactStatus),
                String.valueOf(recommendation.getAccelerationArtifact()));
            assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("rewriteSql"))
                .contains("FROM " + recommendation.getAccelerationArtifact().get("mvName")));
            assertTrue(String.valueOf(recommendation.getAccelerationArtifact().get("ddlSql"))
                .contains("CREATE MATERIALIZED VIEW"));
        }
        assertEquals("TARGET_DATASOURCE_HINT",
            nestedMap(recommendation.getAccelerationArtifact(), "targetEngineResolution").get("resolutionSource"));
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
    void shouldDeriveExpandedRuleCatalogIntoTrialSourceProblems() {
        RewriteTrialRequest request = new RewriteTrialRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT order_id FROM orders");

        RewriteTrialRunVO run = service.createTrial(request);

        assertEquals("NO_SAFE_REWRITE", run.getTrialStatus());
        assertEquals(Integer.valueOf(1), service.overview().getManualReviewRequiredCount());
        assertTrue(containsProblem(run.getItems().get(0).getSourceProblems(), "FULL_SCAN_FILTER_GUARD"));
        assertTrue(containsLink(run.getItems().get(0).getIssueRuleLinks(), "FULL_SCAN_FILTER_GUARD"));
        assertNull(run.getItems().get(0).getRecommendationId());
    }

    @Test
    void shouldKeepExpandedRuleCatalogWhenSourceProblemsAreProvided() {
        RewriteTrialRequest request = new RewriteTrialRequest();
        request.setTenantId("tenant-a");
        request.setSqlText("SELECT APPROX_DISTINCT(user_id) FROM events WHERE dt = DATE '2026-05-01'");
        request.setSourceProblems(Collections.singletonList(sourceProblem("APPROX_DISTINCT_SKETCH_MV")));

        RewriteTrialRunVO run = service.createTrial(request);

        assertEquals("NO_SAFE_REWRITE", run.getTrialStatus());
        assertEquals("APPROX_DISTINCT_SKETCH_MV",
            run.getItems().get(0).getSourceProblems().get(0).get("issueScene"));
        assertEquals("APPROX_DISTINCT_SKETCH_MV",
            run.getItems().get(0).getIssueRuleLinks().get(0).get("ruleCode"));
        assertEquals("NO_SAFE_REWRITE", run.getItems().get(0).getIssueRuleLinks().get(0).get("trialConclusion"));
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
        assertEquals(Integer.valueOf(2), run.getAcceptedCount());
        assertEquals(Integer.valueOf(1), run.getSkippedCount());
        assertEquals(Integer.valueOf(1), run.getRecommendedCount());
        assertEquals(Integer.valueOf(1), run.getCandidateGeneratedCount());
        assertEquals("RECOMMENDED", run.getTrialStatus());
    }

    @Test
    void shouldCarryExplainPlanEvidenceFromParseBatchIntoRecommendation() {
        batchRepository.save(batch("batch-001"));
        ParseBatchItem item = item("item-001", 1,
            "SELECT count(1) FROM orders WHERE id = 1 AND id = 1", "VALID", ParseBatchItemStatus.SUCCESS);
        item.recordPlanAnalysis(
            "SUCCESS",
            "SUCCESS",
            "{\"status\":\"SUCCESS\",\"planText\":\"Fragment 0: scan orders\",\"datasourceCode\":\"hetu_main\","
                + "\"costMs\":12,\"evidence\":[\"sqlExecution=EXPLAIN_ONLY\"]}",
            Instant.now()
        );
        batchItemRepository.save(item);

        RewriteTrialBatchRequest request = new RewriteTrialBatchRequest();
        request.setTenantId("tenant-a");
        RewriteTrialRunVO run = service.createBatchTrial("batch-001", request);

        assertEquals("RECOMMENDED", run.getTrialStatus());
        Map<String, Object> planEvidence = nestedMap(run.getItems().get(0).getSourceProblems().get(0), "planEvidence");
        assertEquals("EXPLAIN_PLAN", planEvidence.get("evidenceLevel"));
        assertEquals("SUCCESS", planEvidence.get("planAnalysisStatus"));
        assertEquals("EXPLAIN_ONLY_NOT_RESULT_EQUIVALENCE", planEvidence.get("claimBoundary"));

        AccelerationRecommendation recommendation =
            recommendationRepository.findByRecommendationId(run.getItems().get(0).getRecommendationId());
        assertNotNull(recommendation);
        assertEquals("MIXED", recommendation.getEvidenceLevel().name());
        assertEquals("SUCCESS", recommendation.getExpectedBenefit().get("planEvidenceStatus"));
        assertEquals("EXTERNAL_EVIDENCE_REQUIRED",
            nestedMap(recommendation.getExpectedBenefit(), "productionScaleGate").get("status"));
        Map<String, Object> costPlanEvidence = nestedMap(recommendation.getEstimatedCost(), "planEvidence");
        assertEquals("EXPLAIN_PLAN", costPlanEvidence.get("evidenceLevel"));
        assertEquals(Boolean.TRUE, recommendation.getEstimatedCost().get("explainPlanAvailable"));
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
        assertEquals(Integer.valueOf(2), service.overview().getManualReviewRequiredCount());

        List<RewriteTrialSourceIssueStatisticVO> byIssue = service.bySourceIssue();
        assertTrue(byIssue.size() >= 2);
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

    private boolean containsProblem(List<Map<String, Object>> values, String issueScene) {
        for (Map<String, Object> value : values) {
            if (issueScene.equals(value.get("issueScene"))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLink(List<Map<String, Object>> values, String ruleCode) {
        for (Map<String, Object> value : values) {
            if (ruleCode.equals(value.get("ruleCode"))) {
                return true;
            }
        }
        return false;
    }

    private String readSqlFixture(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        if (!Files.exists(path)) {
            path = Paths.get("..").resolve(relativePath).normalize();
        }
        assertTrue(Files.exists(path), "未找到 SQL 测试文件：" + path.toAbsolutePath());
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private String normalizeExecutableSql(String sql) {
        StringBuilder builder = new StringBuilder();
        String[] lines = sql == null ? new String[0] : sql.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.startsWith("--") || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.length() > 0) {
                builder.append(trimmed).append('\n');
            }
        }
        return builder.toString()
            .replaceAll("\\s+", " ")
            .replaceAll("\\(\\s+", "(")
            .replaceAll("\\s+\\)", ")")
            .replaceAll("\\s*,\\s*", ", ")
            .replaceAll("\\s*;\\s*$", "")
            .trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> value, String key) {
        Object nested = value.get(key);
        assertTrue(nested instanceof Map);
        return (Map<String, Object>) nested;
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
            "APACHE_CALCITE",
            true,
            "user-001",
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
