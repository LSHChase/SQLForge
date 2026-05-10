package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqloptimization.application.controller.dto.AccelerationCandidateCreateRequest;
import com.company.sqloptimization.application.controller.dto.RewriteValidationRunCreateRequest;
import com.company.sqloptimization.application.controller.dto.SqlRewriteRecordCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationCandidateVO;
import com.company.sqloptimization.application.controller.vo.RewriteValidationRunVO;
import com.company.sqloptimization.application.controller.vo.SqlRewriteRecordVO;
import com.company.sqloptimization.domain.governance.CandidateType;
import com.company.sqloptimization.domain.governance.ComparisonStatus;
import com.company.sqloptimization.domain.governance.DifferenceType;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.governance.ValidationRunStatus;
import com.company.sqloptimization.infrastructure.repository.InMemoryAccelerationCandidateRepository;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlRewriteRecordRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        SqlRewriteRecordApplicationService service =
            new SqlRewriteRecordApplicationService(new InMemorySqlRewriteRecordRepository());

        SqlRewriteRecordCreateRequest request = new SqlRewriteRecordCreateRequest();
        request.setTenantId("tenant-a");
        request.setRecommendationId("rec-001");
        request.setSourceType(GovernanceSourceType.QUERY);
        request.setSourceKind(GovernanceSourceKind.QUERY_HISTORY);
        request.setSourceId("history-001");
        request.setEvidenceLevel(EvidenceLevel.RUNTIME_HISTORY);
        request.setHistoryId("history-001");
        request.setSqlFingerprint("fp-001");
        request.setStatus(RewriteRecordStatus.APPLIED);
        request.setValidationStatus(RewriteValidationStatus.NOT_VALIDATED);
        request.setAutoApplyAllowed(Boolean.TRUE);
        request.setOriginalSqlText("SELECT * FROM orders");
        request.setRecommendedSqlText("SELECT id FROM orders");

        SqlRewriteRecordVO created = service.createRewriteRecord(request);
        assertEquals("APPLIED", created.getStatus());
        assertEquals("NOT_VALIDATED", created.getValidationStatus());
        assertEquals(Boolean.TRUE, created.getAutoApplyAllowed());

        RewriteValidationRunCreateRequest runRequest = new RewriteValidationRunCreateRequest();
        runRequest.setStatus(ValidationRunStatus.SUCCEEDED);
        runRequest.setComparisonStatus(ComparisonStatus.DIVERGED);
        runRequest.setDifferenceType(DifferenceType.VALUE_DIFF);
        runRequest.setAutoApplyPaused(Boolean.TRUE);
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
        assertEquals(1, runs.size());
        assertFalse(enumContainsActive(), "rewrite record status must not expose ACTIVE in HARN-128");
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

    private boolean enumContainsActive() {
        for (RewriteRecordStatus status : RewriteRecordStatus.values()) {
            if ("ACTIVE".equals(status.name())) {
                return true;
            }
        }
        return false;
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
