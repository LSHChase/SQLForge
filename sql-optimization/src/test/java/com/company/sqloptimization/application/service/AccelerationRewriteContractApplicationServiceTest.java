package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.AccessDeniedException;
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
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
