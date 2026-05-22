package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingActivationRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.config.RewriteProductionGateProperties;
import com.company.sqloptimization.application.controller.dto.AccelerationCandidateCreateRequest;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordActivationActionRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordReviewRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationCandidateVO;
import com.company.sqloptimization.application.controller.vo.RewriteActivationEligibilityVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteActivationStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteReviewStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.domain.recommendation.AccelerationRecommendation;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionResultDigestClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionRuntimeRewriteBindingClient;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationCandidateRepository;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationRecommendationRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlRewriteRecordRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AccelerationRewriteContractApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldCreateCandidateWithSourceAndEvidenceContract() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());

        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.STRUCTURE_PARSE);
        request.setSourceId("parse-history-001");
        request.setParseHistoryId("parse-history-001");
        request.setSqlFingerprint("fp-001");
        request.setDatasourceCode("hetu-main");
        request.setCandidateType(CandidateType.ACCELERATION_AND_REWRITE);
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);
        request.setSchemaVersion("schema-v1");

        AccelerationCandidateVO created = service.createCandidate(request);
        AccelerationCandidateVO detail = service.getCandidate(created.getCandidateId());

        assertEquals("PARSE", created.getSourceType());
        assertEquals("STRUCTURE_PARSE", created.getSourceKind());
        assertEquals("STATIC_PARSE", created.getEvidenceLevel());
        assertEquals("DRAFT", created.getStatus());
        assertEquals("ACCELERATION_REWRITE_CONTRACT_BASELINE", created.getImplementationStage());
        assertEquals(created.getCandidateId(), detail.getCandidateId());
    }

    @Test
    void shouldNormalizeParseCandidateSourceIdFromTraceKeys() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());

        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.STRUCTURE_PARSE);
        request.setSourceId("explicit-source");
        request.setParseHistoryId("parse-history-001");
        request.setParseTaskId("parse-task-001");
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);

        AccelerationCandidateVO created = service.createCandidate(request);

        assertEquals("PARSE", created.getSourceType());
        assertEquals("STRUCTURE_PARSE", created.getSourceKind());
        assertEquals("parse-history-001", created.getSourceId());
        assertEquals("STATIC_PARSE", created.getEvidenceLevel());
    }

    @Test
    void shouldNormalizeQueryCandidateSourceIdFromHistory() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());

        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("manual-source");
        request.setHistoryId("history-001");
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);

        AccelerationCandidateVO created = service.createCandidate(request);

        assertEquals("QUERY", created.getSourceType());
        assertEquals("QUERY_HISTORY", created.getSourceKind());
        assertEquals("history-001", created.getSourceId());
        assertEquals("RUNTIME_HISTORY", created.getEvidenceLevel());
    }

    @Test
    void shouldRejectCandidateSourceKindOutsideSourceType() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());
        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("history-001");
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);

        BizException exception = assertThrows(BizException.class, () -> service.createCandidate(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectEvidenceLevelOutsideSourceType() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());
        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setHistoryId("history-001");
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);

        BizException exception = assertThrows(BizException.class, () -> service.createCandidate(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectMissingCandidateTraceKey() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());
        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.STRUCTURE_PARSE);
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);

        BizException exception = assertThrows(BizException.class, () -> service.createCandidate(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectRuntimeEvidenceOnStaticParseCandidate() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());
        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-a");
        request.setSourceType(GovernanceSourceType.PARSE);
        request.setSourceKind(GovernanceSourceKind.STRUCTURE_PARSE);
        request.setParseHistoryId("parse-history-001");
        request.setEvidenceLevel(EvidenceLevel.STATIC_PARSE);
        request.setRuntimeEvidence(Collections.<String, Object>singletonMap("elapsedMs", Integer.valueOf(1200)));

        BizException exception = assertThrows(BizException.class, () -> service.createCandidate(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectCrossTenantCandidateCreation() {
        setTenant("tenant-a");
        AccelerationCandidateApplicationService service =
            new AccelerationCandidateApplicationService(new InMemoryAccelerationCandidateRepository());
        AccelerationCandidateCreateRequest request = new AccelerationCandidateCreateRequest();
        request.setTenantId("tenant-b");
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("history-001");
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);

        assertThrows(AccessDeniedException.class, () -> service.createCandidate(request));
    }

    @Test
    void shouldCreateRewriteRecordAndValidationRunWithoutMarkingAppliedAsActive() {
        setTenant("tenant-a");
        StubResultDigestClient digestClient = new StubResultDigestClient(
            digest("schema-001", Long.valueOf(1L), "checksum-original", row("id", "1")),
            digest("schema-001", Long.valueOf(1L), "checksum-recommended", row("id", "2"))
        );
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(
                new InMemorySqlRewriteRecordRepository(),
                digestClient,
                new ResultDigestComparisonEngine()
            );

        SqlRewriteRecordCreateRequest request = new SqlRewriteRecordCreateRequest();
        request.setTenantId("tenant-a");
        request.setRecommendationId("rec-001");
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("history-001");
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setHistoryId("history-001");
        request.setSqlFingerprint("fp-001");
        request.setStatus(RewriteRecordStatus.READY);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setAutoApplyAllowed(Boolean.TRUE);
        request.setManualReviewRequired(Boolean.TRUE);
        request.setOriginalSqlText("SELECT * FROM orders");
        request.setRecommendedSqlText("SELECT id FROM orders");

        SqlRewriteRecordVO created = service.createRewriteRecord(request);
        assertEquals("READY", created.getStatus());
        assertEquals("NOT_VALIDATED", created.getValidationStatus());
        assertEquals(Boolean.TRUE, created.getAutoApplyAllowed());
        assertEquals(Boolean.TRUE, created.getManualReviewRequired());
        assertEquals("PENDING_REVIEW", created.getReviewStatus());
        assertEquals("INACTIVE", created.getActivationStatus());

        RewriteValidationRunCreateRequest runRequest = new RewriteValidationRunCreateRequest();
        runRequest.setStatus(ValidationRunStatus.SUCCEEDED);
        runRequest.setComparisonStatus(ComparisonStatus.EQUIVALENT);
        runRequest.setDifferenceType(DifferenceType.NONE);
        runRequest.setAutoApplyPaused(Boolean.FALSE);
        runRequest.setStartedAt(Instant.parse("2026-05-10T00:00:00Z"));
        runRequest.setFinishedAt(Instant.parse("2026-05-10T00:00:03Z"));

        RewriteValidationRunVO run = service.createValidationRun(created.getRewriteRecordId(), runRequest);
        SqlRewriteRecordVO updated = service.getRewriteRecord(created.getRewriteRecordId());
        List<RewriteValidationRunVO> runs = service.listValidationRuns(created.getRewriteRecordId());

        assertEquals("DIVERGED", run.getComparisonStatus());
        assertEquals("VALUE_DIFF", run.getDifferenceType());
        assertEquals(Boolean.TRUE, run.getAutoApplyPaused());
        assertEquals("PAUSED", updated.getStatus());
        assertEquals("DIVERGED", updated.getValidationStatus());
        assertEquals("OPEN", updated.getAlertStatus());
        assertEquals(Boolean.FALSE, updated.getAutoApplyAllowed());
        assertEquals("PENDING_REVIEW", updated.getReviewStatus());
        assertEquals("INACTIVE", updated.getActivationStatus());
        assertEquals(1, runs.size());
        assertEquals(2, digestClient.getRequestCount());
    }

    @Test
    void shouldListRewriteRecordsByHistoryWithinTenant() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);

        setTenant("tenant-a");
        SqlRewriteRecordVO historyRecord = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-001"));
        service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-002"));
        setTenant("tenant-b");
        service.createRewriteRecord(rewriteRecordRequest("tenant-b", "history-001"));

        setTenant("tenant-a");
        List<SqlRewriteRecordVO> records = service.listRewriteRecords("history-001", null, null, null);

        assertEquals(1, records.size());
        assertEquals(historyRecord.getRewriteRecordId(), records.get(0).getRewriteRecordId());
        assertEquals("tenant-a", records.get(0).getTenantId());
        assertEquals("history-001", records.get(0).getHistoryId());
    }

    @Test
    void shouldIgnoreClientSubmittedReviewFieldsOnCreateAndCarryPublishRuntimeFields() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-003");
        request.setReviewStatus(RewriteReviewStatus.CHANGES_REQUESTED);
        request.setReviewNote("needs indexed predicate proof");
        request.setReviewedBy("reviewer-001");
        request.setReviewedAt(Instant.parse("2026-05-10T12:00:00Z"));
        request.setActivationStatus(RewriteActivationStatus.ACTIVATE_FAILED);
        request.setRuntimeBindingId("binding-003");
        request.setRuntimeBindingAt(Instant.parse("2026-05-10T12:01:00Z"));
        request.setRuntimeBindingBy("operator-003");
        request.setRuntimeBindingScope("tenant-a:fp-003");
        request.setActivatedSqlFingerprint("fp-published-003");
        request.setRuntimeRuleVersion("rule-v3");

        SqlRewriteRecordVO created = service.createRewriteRecord(request);
        SqlRewriteRecordVO detail = service.getRewriteRecord(created.getRewriteRecordId());

        assertEquals("PENDING_REVIEW", detail.getReviewStatus());
        assertNull(detail.getReviewNote());
        assertNull(detail.getReviewedBy());
        assertNull(detail.getReviewedAt());
        assertEquals("ACTIVATE_FAILED", detail.getActivationStatus());
        assertEquals("binding-003", detail.getRuntimeBindingId());
        assertEquals(Instant.parse("2026-05-10T12:01:00Z"), detail.getRuntimeBindingAt());
        assertEquals("operator-003", detail.getRuntimeBindingBy());
        assertEquals("tenant-a:fp-003", detail.getRuntimeBindingScope());
        assertEquals("fp-published-003", detail.getActivatedSqlFingerprint());
        assertEquals("rule-v3", detail.getRuntimeRuleVersion());
    }

    @Test
    void shouldReviewRewriteRecordThroughAllowedStateMachineWithoutPublishing() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-004");
        request.setManualReviewRequired(Boolean.TRUE);
        request.setActivationStatus(RewriteActivationStatus.INACTIVE);
        request.setRuntimeBindingId("binding-should-stay");
        SqlRewriteRecordVO created = service.createRewriteRecord(request);

        SqlRewriteRecordReviewRequest approveRequest = reviewRequest(
            "tenant-a",
            RewriteReviewStatus.APPROVED,
            null
        );
        SqlRewriteRecordVO approved = service.reviewRewriteRecord(created.getRewriteRecordId(), approveRequest);

        assertEquals("APPROVED", approved.getReviewStatus());
        assertEquals("operator-001", approved.getReviewedBy());
        assertNotNull(approved.getReviewedAt());
        assertEquals(Boolean.TRUE, approved.getAutoApplyAllowed());
        assertEquals(Boolean.TRUE, approved.getManualReviewRequired());
        assertEquals("INACTIVE", approved.getActivationStatus());
        assertEquals("binding-should-stay", approved.getRuntimeBindingId());
        assertEquals("APPROVED", ((Map<?, ?>) approved.getTraceRefs().get("lastReviewTrace")).get("reviewStatus"));
        assertEquals("trace-001", ((Map<?, ?>) approved.getTraceRefs().get("lastReviewTrace")).get("traceId"));
    }

    @Test
    void shouldReturnPublishEligibilityFromCentralPolicy() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-eligibility");
        request.setSqlFingerprint("fp-eligibility");
        request.setDatasourceCode("HETU");
        request.setAutoApplyAllowed(Boolean.TRUE);
        SqlRewriteRecordVO created = service.createRewriteRecord(request);
        service.reviewRewriteRecord(
            created.getRewriteRecordId(),
            reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "equivalent and approved")
        );
        RewriteValidationRunCreateRequest runRequest = new RewriteValidationRunCreateRequest();
        runRequest.setStatus(ValidationRunStatus.SUCCEEDED);
        runRequest.setComparisonStatus(ComparisonStatus.EQUIVALENT);
        runRequest.setDifferenceType(DifferenceType.NONE);
        runRequest.setAutoApplyPaused(Boolean.FALSE);
        runRequest.setExecutionEvidence(positiveRuntimeExecutionEvidence());
        service.createValidationRun(created.getRewriteRecordId(), runRequest);

        RewriteActivationEligibilityVO eligibility = service.getActivationEligibility(created.getRewriteRecordId());

        assertEquals(Boolean.TRUE, eligibility.getEligible());
        assertEquals("DEFAULT_REWRITE_ACTIVATION_ELIGIBILITY", eligibility.getPolicyId());
        assertEquals("APPROVED", eligibility.getReviewStatus());
        assertEquals("EQUIVALENT", eligibility.getValidationStatus());
        assertEquals("INACTIVE", eligibility.getActivationStatus());
        assertTrue(eligibility.getRefusalReasons().isEmpty());
    }

    @Test
    void shouldPublishApprovedRewriteRecordThroughRuntimeBinding() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient
        );
        setTenant("tenant-a");
        SqlRewriteRecordVO ready = createPublishableRewriteRecord(service, "history-publish", "fp-publish");

        SqlRewriteRecordVO published = service.activateRewriteRecord(
            ready.getRewriteRecordId(),
            publishActionRequest("release approved rewrite")
        );

        assertEquals("ACTIVE", published.getActivationStatus());
        assertEquals("rwb-001", published.getRuntimeBindingId());
        assertEquals("runtime-rewrite-v1", published.getRuntimeRuleVersion());
        assertEquals("tenant-a:fp-publish", published.getRuntimeBindingScope());
        assertEquals("fp-publish", published.getActivatedSqlFingerprint());
        assertEquals(1, runtimeClient.activateCount);
        Map<?, ?> publishTrace = (Map<?, ?>) published.getTraceRefs().get("activationEvidence");
        assertEquals("ACTIVATE", publishTrace.get("action"));
        assertEquals("ACTIVE", publishTrace.get("activationStatus"));
        assertEquals(Boolean.TRUE, publishTrace.get("runtimeBinding"));
        assertEquals("ACTIVE", publishTrace.get("runtimeStatus"));
        assertEquals("rwb-001", publishTrace.get("runtimeBindingId"));
        assertEquals("fp-publish", runtimeClient.lastActivateRequest.getSqlFingerprint());
    }

    @Test
    void shouldDirectActivateRewriteRecordWithoutValidationEvidenceInDevelopment() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient,
            developmentDirectActivationProperties()
        );
        setTenant("tenant-a");
        SqlRewriteRecordVO created = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-not-ready"));

        SqlRewriteRecordVO activated =
            service.activateRewriteRecord(created.getRewriteRecordId(), publishActionRequest("dev direct activate"));

        assertEquals("ACTIVE", activated.getActivationStatus());
        assertEquals("ACTIVE", activated.getStatus());
        assertEquals(1, runtimeClient.activateCount);
        assertEquals(
            com.company.sqlforge.common.utils.SqlFingerprintUtils.fingerprint("SELECT * FROM orders"),
            runtimeClient.lastActivateRequest.getSqlFingerprint()
        );
        assertEquals("hetu_main", runtimeClient.lastActivateRequest.getDatasourceCode());
        Map<?, ?> publishTrace = (Map<?, ?>) activated.getTraceRefs().get("activationEvidence");
        assertEquals(Boolean.TRUE, publishTrace.get("developmentDirectActivation"));
        assertEquals("BYPASSED_FOR_DEVELOPMENT_DEBUG", publishTrace.get("validationGate"));
    }

    @Test
    void shouldRejectDirectActivateRewriteRecordWithoutValidationWhenSwitchDisabled() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient
        );
        setTenant("tenant-a");
        SqlRewriteRecordVO created = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-not-ready"));

        BizException exception = assertThrows(
            BizException.class,
            () -> service.activateRewriteRecord(created.getRewriteRecordId(), publishActionRequest("prod gated activate"))
        );

        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID, exception.getCode());
        assertEquals(0, runtimeClient.activateCount);
        assertTrue(exception.getMessage().contains("VALIDATION_STATUS_NOT_EQUIVALENT"));
    }

    @Test
    void shouldFailPublishWhenRuntimeBindingFails() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        runtimeClient.failActivate = true;
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient
        );
        setTenant("tenant-a");
        SqlRewriteRecordVO ready = createPublishableRewriteRecord(service, "history-publish-fail", "fp-publish-fail");

        BizException ex = assertThrows(
            BizException.class,
            () -> service.activateRewriteRecord(
                ready.getRewriteRecordId(),
                publishActionRequest("runtime outage must block publish")
            )
        );
        SqlRewriteRecordVO failed = service.getRewriteRecord(ready.getRewriteRecordId());

        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE, ex.getCode());
        assertEquals("ACTIVATE_FAILED", failed.getActivationStatus());
        assertEquals(1, runtimeClient.activateCount);
        assertEquals("ACTIVATE_FAILED", ((Map<?, ?>) failed.getTraceRefs().get("activationEvidence")).get("activationStatus"));
    }

    @Test
    void shouldPauseRepublishAndUnpublishThroughRuntimeBinding() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient
        );
        setTenant("tenant-a");
        SqlRewriteRecordVO ready = createPublishableRewriteRecord(service, "history-lifecycle", "fp-lifecycle");
        SqlRewriteRecordVO published = service.activateRewriteRecord(
            ready.getRewriteRecordId(),
            publishActionRequest("release")
        );

        SqlRewriteRecordVO paused = service.pauseRewriteRecord(
            published.getRewriteRecordId(),
            publishActionRequest("scheduled validation divergence")
        );
        SqlRewriteRecordVO republished = service.activateRewriteRecord(
            paused.getRewriteRecordId(),
            publishActionRequest("resume")
        );
        SqlRewriteRecordVO pausedAgain = service.pauseRewriteRecord(
            republished.getRewriteRecordId(),
            publishActionRequest("operator pause")
        );

        assertEquals("PAUSED", paused.getActivationStatus());
        assertEquals("ACTIVE", republished.getActivationStatus());
        assertEquals("PAUSED", pausedAgain.getActivationStatus());
        assertEquals(2, runtimeClient.activateCount);
        assertEquals("rwb-001", runtimeClient.lastPauseRequest.getRuntimeBindingId());
        assertEquals("PAUSE", ((Map<?, ?>) pausedAgain.getTraceRefs().get("pauseEvidence")).get("action"));
        assertEquals(Boolean.TRUE, ((Map<?, ?>) pausedAgain.getTraceRefs().get("pauseEvidence")).get("runtimeBinding"));
    }

    @Test
    void shouldRejectIllegalReviewTransitionsAndSupportExplicitReopen() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordVO created = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-005"));
        SqlRewriteRecordVO rejected = service.reviewRewriteRecord(
            created.getRewriteRecordId(),
            reviewRequest("tenant-a", RewriteReviewStatus.REJECTED, "semantic risk")
        );
        assertEquals("REJECTED", rejected.getReviewStatus());

        BizException directApprove = assertThrows(
            BizException.class,
            () -> service.reviewRewriteRecord(
                created.getRewriteRecordId(),
                reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "fixed")
            )
        );
        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID, directApprove.getCode());
        assertEquals(HttpStatus.CONFLICT, directApprove.getHttpStatus());

        SqlRewriteRecordVO reopened = service.reviewRewriteRecord(
            created.getRewriteRecordId(),
            reviewRequest("tenant-a", RewriteReviewStatus.PENDING_REVIEW, "regenerated candidate proof")
        );
        assertEquals("PENDING_REVIEW", reopened.getReviewStatus());
        assertEquals("regenerated candidate proof", reopened.getReviewNote());

        SqlRewriteRecordVO approved = service.reviewRewriteRecord(
            created.getRewriteRecordId(),
            reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "equivalent after revision")
        );
        assertEquals("APPROVED", approved.getReviewStatus());

        BizException duplicateApprove = assertThrows(
            BizException.class,
            () -> service.reviewRewriteRecord(
                created.getRewriteRecordId(),
                reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "repeat")
            )
        );
        assertEquals(ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID, duplicateApprove.getCode());
    }

    @Test
    void shouldRequireReviewNoteForRejectChangeRequestAndReopen() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordVO created = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-006"));

        BizException missingRejectNote = assertThrows(
            BizException.class,
            () -> service.reviewRewriteRecord(
                created.getRewriteRecordId(),
                reviewRequest("tenant-a", RewriteReviewStatus.CHANGES_REQUESTED, " ")
            )
        );
        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, missingRejectNote.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, missingRejectNote.getHttpStatus());
    }

    @Test
    void shouldRejectGeneratedMvArtifactCreateWhenRecommendedSqlDoesNotUseRewriteSql() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-mv-create");
        request.setSqlFingerprint("fp-mv-create");
        request.setDatasourceCode("hetu_main");
        request.setRecommendedSqlText("SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id");
        request.setTraceRefs(Collections.<String, Object>singletonMap(
            "accelerationArtifact",
            generatedMvArtifact("SELECT customer_id, SUM(total_amount) AS total_amount "
                + "FROM mv_sales_daily GROUP BY customer_id;")
        ));

        BizException exception = assertThrows(BizException.class, () -> service.createRewriteRecord(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldCreateGeneratedMvRewriteRecordFromStoredRecommendationArtifactWhenTraceRefsMissing() {
        InMemorySqlRewriteRecordRepository rewriteRepository = new InMemorySqlRewriteRecordRepository();
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        String rewriteSql = "SELECT customer_id, SUM(total_amount) AS total_amount "
            + "FROM mv_sales_daily GROUP BY customer_id;";
        recommendationRepository.save(mvRecommendation("rec-mv-authority", "tenant-a", rewriteSql));
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(rewriteRepository, recommendationRepository);
        setTenant("tenant-a");
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-mv-authority");
        request.setRecommendationId("rec-mv-authority");
        request.setRecommendedSqlText(rewriteSql);
        request.setTraceRefs(null);

        SqlRewriteRecordVO created = service.createRewriteRecord(request);

        assertEquals(rewriteSql, created.getRecommendedSqlText());
        assertEquals("PARAMETERIZED_AGG_MV", created.getTraceRefs().get("mvType"));
        assertEquals("mv_sales_daily", created.getTraceRefs().get("mvName"));
        Map<?, ?> artifactTrace = (Map<?, ?>) created.getTraceRefs().get("accelerationArtifact");
        assertEquals("ACCELERATION_RECOMMENDATION_SNAPSHOT", artifactTrace.get("artifactAuthority"));
        assertEquals("PRECOMPUTE_MV", artifactTrace.get("rule"));
        assertEquals("GENERATED", artifactTrace.get("artifactStatus"));
        assertEquals(rewriteSql, artifactTrace.get("rewriteSql"));
    }

    @Test
    void shouldRejectGeneratedMvRewriteRecordWhenRecommendedSqlDriftsFromStoredRecommendationArtifact() {
        InMemorySqlRewriteRecordRepository rewriteRepository = new InMemorySqlRewriteRecordRepository();
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        recommendationRepository.save(mvRecommendation(
            "rec-mv-drift-create",
            "tenant-a",
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales_daily GROUP BY customer_id"
        ));
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(rewriteRepository, recommendationRepository);
        setTenant("tenant-a");
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-mv-drift-create");
        request.setRecommendationId("rec-mv-drift-create");
        request.setRecommendedSqlText("SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id");
        request.setTraceRefs(null);

        BizException exception = assertThrows(BizException.class, () -> service.createRewriteRecord(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectStaleMvTraceRefsWhenStoredRecommendationArtifactIsAuthoritative() {
        InMemorySqlRewriteRecordRepository rewriteRepository = new InMemorySqlRewriteRecordRepository();
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        String rewriteSql = "SELECT customer_id, SUM(total_amount) AS total_amount "
            + "FROM mv_sales_daily GROUP BY customer_id";
        recommendationRepository.save(mvRecommendation("rec-mv-stale-trace", "tenant-a", rewriteSql));
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(rewriteRepository, recommendationRepository);
        setTenant("tenant-a");
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-mv-stale-trace");
        request.setRecommendationId("rec-mv-stale-trace");
        request.setRecommendedSqlText(rewriteSql);
        Map<String, Object> staleTraceRefs = new LinkedHashMap<String, Object>();
        staleTraceRefs.put("mvType", "PREJOIN_MV");
        staleTraceRefs.put("accelerationArtifact", generatedMvArtifact(
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_old_sales GROUP BY customer_id"
        ));
        request.setTraceRefs(staleTraceRefs);

        BizException exception = assertThrows(BizException.class, () -> service.createRewriteRecord(request));

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    }

    @Test
    void shouldRejectCrossTenantRecommendationArtifactOnRewriteRecordCreate() {
        InMemorySqlRewriteRecordRepository rewriteRepository = new InMemorySqlRewriteRecordRepository();
        InMemoryAccelerationRecommendationRepository recommendationRepository =
            new InMemoryAccelerationRecommendationRepository();
        recommendationRepository.save(mvRecommendation(
            "rec-mv-cross-tenant",
            "tenant-b",
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales_daily GROUP BY customer_id"
        ));
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(rewriteRepository, recommendationRepository);
        setTenant("tenant-a");
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", "history-mv-cross-tenant");
        request.setRecommendationId("rec-mv-cross-tenant");
        request.setRecommendedSqlText(
            "SELECT customer_id, SUM(total_amount) AS total_amount FROM mv_sales_daily GROUP BY customer_id"
        );

        assertThrows(AccessDeniedException.class, () -> service.createRewriteRecord(request));
    }

    @Test
    void shouldRejectGeneratedMvArtifactPublishWhenStoredRecommendedSqlDriftedFromRewriteSql() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        StubRuntimeRewriteBindingClient runtimeClient = new StubRuntimeRewriteBindingClient();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(
            repository,
            null,
            new ResultDigestComparisonEngine(),
            runtimeClient
        );
        setTenant("tenant-a");
        repository.saveRecord(SqlRewriteRecord.builder()
            .rewriteRecordId("rewrite-mv-drift")
            .tenantId("tenant-a")
            .sourceType(GovernanceSourceType.QUERY)
            .sourceKind(GovernanceSourceKind.QUERY_HISTORY)
            .sourceId("history-mv-drift")
            .evidenceLevel(EvidenceLevel.RUNTIME_HISTORY)
            .historyId("history-mv-drift")
            .sqlFingerprint("fp-mv-drift")
            .datasourceCode("hetu_main")
            .status(RewriteRecordStatus.READY)
            .validationStatus(RewriteValidationStatus.EQUIVALENT)
            .autoApplyAllowed(true)
            .manualReviewRequired(true)
            .reviewStatus(RewriteReviewStatus.APPROVED)
            .activationStatus(RewriteActivationStatus.INACTIVE)
            .originalSqlText("SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id")
            .recommendedSqlText("SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id")
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .traceRefs(Collections.<String, Object>singletonMap(
                "accelerationArtifact",
                generatedMvArtifact("SELECT customer_id, SUM(total_amount) AS total_amount "
                    + "FROM mv_sales_daily GROUP BY customer_id;")
            ))
            .build());

        BizException exception = assertThrows(
            BizException.class,
            () -> service.activateRewriteRecord("rewrite-mv-drift", publishActionRequest("release mv rewrite"))
        );

        assertEquals(ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT, exception.getCode());
        assertEquals(0, runtimeClient.activateCount);
    }

    @Test
    void shouldRejectReviewWhenContextTenantOrRequestTenantMismatch() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        SqlRewriteRecordApplicationService service = new SqlRewriteRecordApplicationService(repository);
        setTenant("tenant-a");

        SqlRewriteRecordVO created = service.createRewriteRecord(rewriteRecordRequest("tenant-a", "history-007"));

        assertThrows(
            AccessDeniedException.class,
            () -> service.reviewRewriteRecord(
                created.getRewriteRecordId(),
                reviewRequest("tenant-b", RewriteReviewStatus.APPROVED, "equivalent")
            )
        );

        RequestContext.clear();
        BizException missingContext = assertThrows(
            BizException.class,
            () -> service.reviewRewriteRecord(
                created.getRewriteRecordId(),
                reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "equivalent")
            )
        );
        assertEquals(ErrorCodeConstants.SYSTEM_CONTEXT_MISSING, missingContext.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, missingContext.getHttpStatus());
    }

    private SqlRewriteRecordCreateRequest rewriteRecordRequest(String tenantId, String historyId) {
        SqlRewriteRecordCreateRequest request = new SqlRewriteRecordCreateRequest();
        request.setTenantId(tenantId);
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId(historyId);
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setHistoryId(historyId);
        request.setOriginalSqlText("SELECT * FROM orders");
        request.setRecommendedSqlText("SELECT id FROM orders");
        return request;
    }

    private SqlRewriteRecordVO createPublishableRewriteRecord(SqlRewriteRecordApplicationService service,
                                                              String historyId,
                                                              String sqlFingerprint) {
        SqlRewriteRecordCreateRequest request = rewriteRecordRequest("tenant-a", historyId);
        request.setRecommendationId("recommendation-" + historyId);
        request.setSqlFingerprint(sqlFingerprint);
        request.setDatasourceCode("hetu_main");
        request.setAutoApplyAllowed(Boolean.TRUE);
        SqlRewriteRecordVO created = service.createRewriteRecord(request);
        service.reviewRewriteRecord(
            created.getRewriteRecordId(),
            reviewRequest("tenant-a", RewriteReviewStatus.APPROVED, "equivalent and approved")
        );
        RewriteValidationRunCreateRequest runRequest = new RewriteValidationRunCreateRequest();
        runRequest.setStatus(ValidationRunStatus.SUCCEEDED);
        runRequest.setComparisonStatus(ComparisonStatus.EQUIVALENT);
        runRequest.setDifferenceType(DifferenceType.NONE);
        runRequest.setAutoApplyPaused(Boolean.FALSE);
        runRequest.setExecutionEvidence(positiveRuntimeExecutionEvidence());
        service.createValidationRun(created.getRewriteRecordId(), runRequest);
        return service.getRewriteRecord(created.getRewriteRecordId());
    }

    private Map<String, Object> positiveRuntimeExecutionEvidence() {
        Map<String, Object> runtimeDelta = new LinkedHashMap<String, Object>();
        runtimeDelta.put("benefitStatus", "POSITIVE");
        runtimeDelta.put("elapsedImprovementPercent", Double.valueOf(20D));
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("runtimeDelta", runtimeDelta);
        return evidence;
    }

    private RewriteProductionGateProperties developmentDirectActivationProperties() {
        RewriteProductionGateProperties properties = new RewriteProductionGateProperties();
        properties.setDevelopmentDirectActivationEnabled(true);
        return properties;
    }

    private Map<String, Object> generatedMvArtifact(String rewriteSql) {
        Map<String, Object> artifact = new LinkedHashMap<String, Object>();
        artifact.put("rule", "PRECOMPUTE_MV");
        artifact.put("mvType", "PARAMETERIZED_AGG_MV");
        artifact.put("artifactStatus", "GENERATED");
        artifact.put("mvName", "mv_sales_daily");
        artifact.put("targetDatasource", "hetu_main");
        artifact.put("ddlSql", "CREATE MATERIALIZED VIEW mv_sales_daily AS\n"
            + "SELECT customer_id, dt, SUM(amount) AS total_amount FROM orders GROUP BY customer_id, dt");
        artifact.put("refreshSql", "REFRESH MATERIALIZED VIEW mv_sales_daily");
        artifact.put("validationSql", "WITH rewrite_result AS (" + rewriteSql + ") "
            + "SELECT COUNT(*) FROM rewrite_result");
        artifact.put("rollbackSql", "DROP MATERIALIZED VIEW mv_sales_daily");
        artifact.put("rewriteSql", rewriteSql);
        artifact.put("blockingReasons", Collections.emptyList());
        artifact.put("governanceBoundary", "PULL_ONLY_NOT_EXECUTED_BY_SQLFORGE");
        return artifact;
    }

    private AccelerationRecommendation mvRecommendation(String recommendationId,
                                                        String tenantId,
                                                        String rewriteSql) {
        return AccelerationRecommendation.builder()
            .recommendationId(recommendationId)
            .tenantId(tenantId)
            .recommendationType(AccelerationRecommendation.RecommendationType.ACCELERATION)
            .historyId("history-" + recommendationId)
            .sourceSqlText("SELECT customer_id, SUM(amount) FROM orders GROUP BY customer_id")
            .recommendedSqlText("CREATE MATERIALIZED VIEW mv_sales_daily AS SELECT ...")
            .targetDatasource("hetu_main")
            .accelerationArtifact(generatedMvArtifact(rewriteSql))
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T00:00:00Z"))
            .build();
    }

    private SqlRewriteRecordActivationActionRequest publishActionRequest(String reason) {
        SqlRewriteRecordActivationActionRequest request = new SqlRewriteRecordActivationActionRequest();
        request.setTenantId("tenant-a");
        request.setReason(reason);
        return request;
    }

    private SqlRewriteRecordReviewRequest reviewRequest(String tenantId,
                                                        RewriteReviewStatus status,
                                                        String note) {
        SqlRewriteRecordReviewRequest request = new SqlRewriteRecordReviewRequest();
        request.setTenantId(tenantId);
        request.setReviewStatus(status);
        request.setReviewNote(note);
        return request;
    }

    private QueryExecutionResultDigestResponse digest(String schemaDigest,
                                                      Long rowCount,
                                                      String checksumDigest,
                                                      Map<String, Object> sampleRow) {
        QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
        response.setStatus("SUCCESS");
        response.setResultDigest(digestMap(schemaDigest, rowCount, checksumDigest));
        response.setLimitedSample(Collections.singletonList(sampleRow));
        response.setExecutionEvidence(Collections.<String, Object>singletonMap("readonlyDigestOnly", Boolean.TRUE));
        return response;
    }

    private Map<String, Object> digestMap(String schemaDigest, Long rowCount, String checksumDigest) {
        Map<String, Object> digest = new LinkedHashMap<String, Object>();
        digest.put("schemaDigest", schemaDigest);
        digest.put("rowCount", rowCount);
        digest.put("keySetDigest", "");
        digest.put("orderDigest", checksumDigest);
        digest.put("checksumDigest", checksumDigest);
        return digest;
    }

    private Map<String, Object> row(String key, String value) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put(key, value);
        return row;
    }

    private static final class StubResultDigestClient implements QueryExecutionResultDigestClient {

        private final QueryExecutionResultDigestResponse original;
        private final QueryExecutionResultDigestResponse recommended;
        private int requestCount;

        private StubResultDigestClient(QueryExecutionResultDigestResponse original,
                                       QueryExecutionResultDigestResponse recommended) {
            this.original = original;
            this.recommended = recommended;
        }

        @Override
        public QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request) {
            requestCount++;
            return requestCount == 1 ? original : recommended;
        }

        int getRequestCount() {
            return requestCount;
        }
    }

    private static final class StubRuntimeRewriteBindingClient implements QueryExecutionRuntimeRewriteBindingClient {

        private int activateCount;
        private boolean failActivate;
        private RuntimeRewriteBindingActivationRequest lastActivateRequest;
        private RuntimeRewriteBindingStateChangeRequest lastPauseRequest;
        private String activatedRewriteRecordId;

        @Override
        public RuntimeRewriteBindingResponse activate(RuntimeRewriteBindingActivationRequest request) {
            activateCount++;
            lastActivateRequest = request;
            if (failActivate) {
                throw new BizException(
                    ErrorCodeConstants.SQL_OPTIMIZATION_SYSTEM_REWRITE_FAILURE,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "runtime route 不可用"
                );
            }
            activatedRewriteRecordId = request.getRewriteRecordId();
            RuntimeRewriteBindingResponse response = baseResponse(request.getTenantId(), request.getSqlFingerprint());
            response.setRuntimeBindingId("rwb-001");
            response.setRewriteRecordId(request.getRewriteRecordId());
            response.setRecommendationId(request.getRecommendationId());
            response.setSourceType(request.getSourceType());
            response.setSourceKind(request.getSourceKind());
            response.setSourceId(request.getSourceId());
            response.setOriginalSqlDigest(request.getOriginalSqlDigest());
            response.setRecommendedSqlText(request.getRecommendedSqlText());
            response.setDatasourceCode(request.getDatasourceCode());
            response.setStatus("ACTIVE");
            response.setActive(true);
            response.setRuleVersion(Long.valueOf(1L));
            response.setRuntimeRuleVersion("runtime-rewrite-v1");
            response.setRuntimeSummary("active");
            return response;
        }

        @Override
        public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
            lastPauseRequest = request;
            RuntimeRewriteBindingResponse response = baseResponse(request.getTenantId(), request.getSqlFingerprint());
            response.setRuntimeBindingId(request.getRuntimeBindingId());
            response.setRewriteRecordId(activatedRewriteRecordId);
            response.setStatus("PAUSED");
            response.setActive(false);
            response.setRuleVersion(Long.valueOf(1L));
            response.setRuntimeRuleVersion("runtime-rewrite-v1");
            response.setRuntimeSummary("paused");
            return response;
        }

        private RuntimeRewriteBindingResponse baseResponse(String tenantId, String sqlFingerprint) {
            RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
            response.setTenantId(tenantId);
            response.setSqlFingerprint(sqlFingerprint);
            return response;
        }
    }

    private void setTenant(String tenantId) {
        RequestContext.set(
            tenantId,
            "operator-001",
            Arrays.asList("TENANT_ADMIN", "OPERATOR"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );
    }
}
