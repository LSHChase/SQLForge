package com.company.sqloptimization.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestRequest;
import com.company.sqlforge.common.queryexecution.QueryExecutionResultDigestResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingPublishRequest;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingResponse;
import com.company.sqlforge.common.queryexecution.RuntimeRewriteBindingStateChangeRequest;
import com.company.sqloptimization.config.RewriteValidationSchedulerProperties;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewritePublishStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.governance.OptimizationAuditRecord;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionResultDigestClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionRuntimeRewriteBindingClient;
import com.company.sqloptimization.infrastructure.repository.InMemorySqlRewriteRecordRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RewriteValidationSchedulerServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldSkipWorkWhenSchedulerIsDisabled() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        repository.saveRecord(rewriteRecord("rewrite-001", "SELECT original", "SELECT recommended"));
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, false);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        assertFalse(result.isEnabled());
        assertEquals(0, result.getProcessedCount());
        verify(governanceCapabilityClient, never()).emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        );
    }

    @Test
    void shouldValidateDivergedRewriteAndEmitAlertLinkage() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        repository.saveRecord(rewriteRecord("rewrite-001", "SELECT original", "SELECT recommended"));
        FakeRuntimeRewriteBindingClient runtimeClient = new FakeRuntimeRewriteBindingClient();
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        when(governanceCapabilityClient.emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        )).thenReturn(alertResponse());
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, runtimeClient, true);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        SqlRewriteRecord updated = repository.findRecordById("rewrite-001");
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getAlertEmissionCount());
        assertEquals(1, result.getAuditWriteCount());
        assertEquals(RewriteRecordStatus.PAUSED, updated.getStatus());
        assertEquals(RewriteValidationStatus.DIVERGED, updated.getValidationStatus());
        assertEquals(RewritePublishStatus.PAUSED, updated.getPublishStatus());
        assertFalse(updated.isAutoApplyAllowed());
        assertEquals(RewriteAlertStatus.OPEN, updated.getAlertStatus());
        assertEquals(0, runtimeClient.pauseCount);
        Map<String, Object> publishStatusTrace = castMap(updated.getTraceRefs().get("lastPublishStatusTrace"));
        assertEquals("AUTO_PAUSE", publishStatusTrace.get("action"));
        assertEquals("PAUSED", publishStatusTrace.get("publishStatus"));
        assertEquals(Boolean.TRUE, publishStatusTrace.get("statusOnly"));
        Map<String, Object> divergenceAlert = castMap(updated.getTraceRefs().get("divergenceAlert"));
        assertEquals("EMITTED_OR_DEDUPED", divergenceAlert.get("emissionStatus"));
        assertEquals("SQL_REWRITE_RESULT_DIVERGENCE", divergenceAlert.get("alertType"));
        Map<String, Object> divergenceAudit = castMap(updated.getTraceRefs().get("divergencePauseAudit"));
        assertEquals("WRITTEN", divergenceAudit.get("auditWriteStatus"));
        assertEquals("SUCCESS", divergenceAudit.get("resultStatus"));

        ArgumentCaptor<GovernanceSqlRewriteDivergenceAlertRequest> captor =
            ArgumentCaptor.forClass(GovernanceSqlRewriteDivergenceAlertRequest.class);
        verify(governanceCapabilityClient).emitSqlRewriteDivergenceAlert(captor.capture());
        verify(governanceCapabilityClient).writeAudit(
            org.mockito.ArgumentMatchers.any(OptimizationAuditRecord.class)
        );
        assertEquals("tenant-a", captor.getValue().getTenantId());
        assertEquals("rewrite-001", captor.getValue().getRewriteRecordId());
        assertEquals("DIVERGED", captor.getValue().getComparisonStatus());
        assertEquals("VALUE_DIFF", captor.getValue().getDifferenceType());
        assertEquals(Boolean.TRUE, captor.getValue().getAutoApplyPaused());
    }

    @Test
    void shouldAutoPauseByStatusWhenRuntimePauseClientFails() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        repository.saveRecord(rewriteRecord("rewrite-001", "SELECT original", "SELECT recommended"));
        FakeRuntimeRewriteBindingClient runtimeClient = new FakeRuntimeRewriteBindingClient();
        runtimeClient.failPause = true;
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        when(governanceCapabilityClient.emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        )).thenReturn(alertResponse());
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, runtimeClient, true);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        SqlRewriteRecord updated = repository.findRecordById("rewrite-001");
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getAlertEmissionCount());
        assertEquals(1, result.getAuditWriteCount());
        assertEquals(RewriteRecordStatus.PAUSED, updated.getStatus());
        assertEquals(RewriteValidationStatus.DIVERGED, updated.getValidationStatus());
        assertEquals(RewritePublishStatus.PAUSED, updated.getPublishStatus());
        assertFalse(updated.isAutoApplyAllowed());
        assertEquals(0, runtimeClient.pauseCount);
        Map<String, Object> publishStatusTrace = castMap(updated.getTraceRefs().get("lastPublishStatusTrace"));
        assertEquals("AUTO_PAUSE", publishStatusTrace.get("action"));
        assertEquals("PAUSED", publishStatusTrace.get("publishStatus"));
        Map<String, Object> divergenceAudit = castMap(updated.getTraceRefs().get("divergencePauseAudit"));
        assertEquals("WRITTEN", divergenceAudit.get("auditWriteStatus"));
        assertEquals("SUCCESS", divergenceAudit.get("resultStatus"));
    }

    @Test
    void shouldNotEmitAlertForEquivalentRewrite() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        repository.saveRecord(rewriteRecord("rewrite-001", "SELECT original", "SELECT original"));
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, true);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        SqlRewriteRecord updated = repository.findRecordById("rewrite-001");
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, result.getAlertEmissionCount());
        assertEquals(RewriteRecordStatus.APPROVED, updated.getStatus());
        assertEquals(RewriteValidationStatus.EQUIVALENT, updated.getValidationStatus());
        assertEquals(RewritePublishStatus.PUBLISHED, updated.getPublishStatus());
        verify(governanceCapabilityClient, never()).emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        );
    }

    @Test
    void shouldRevalidateEquivalentRewriteWhenComparisonAgeIsDue() {
        InMemorySqlRewriteRecordRepository repository = new InMemorySqlRewriteRecordRepository();
        repository.saveRecord(rewriteRecord(
            "rewrite-001",
            "SELECT original",
            "SELECT original",
            RewriteValidationStatus.EQUIVALENT,
            Instant.parse("2026-05-09T10:00:00Z")
        ));
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, true);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        SqlRewriteRecord updated = repository.findRecordById("rewrite-001");
        assertEquals(1, result.getProcessedCount());
        assertEquals(0, result.getAlertEmissionCount());
        assertEquals(RewriteValidationStatus.EQUIVALENT, updated.getValidationStatus());
        verify(governanceCapabilityClient, never()).emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        );
    }

    private RewriteValidationSchedulerService service(InMemorySqlRewriteRecordRepository repository,
                                                      GovernanceCapabilityClient governanceCapabilityClient,
                                                      boolean enabled) {
        return service(repository, governanceCapabilityClient, new FakeRuntimeRewriteBindingClient(), enabled);
    }

    private RewriteValidationSchedulerService service(InMemorySqlRewriteRecordRepository repository,
                                                      GovernanceCapabilityClient governanceCapabilityClient,
                                                      QueryExecutionRuntimeRewriteBindingClient runtimeClient,
                                                      boolean enabled) {
        RewriteValidationSchedulerProperties properties = new RewriteValidationSchedulerProperties();
        properties.setEnabled(enabled);
        properties.setBatchSize(10);
        properties.setMaxAgeMinutes(60L);
        SqlRewriteRecordApplicationService rewriteRecordApplicationService =
            new SqlRewriteRecordApplicationService(
                repository,
                new FakeResultDigestClient(),
                new ResultDigestComparisonEngine(),
                runtimeClient
            );
        return new RewriteValidationSchedulerService(
            properties,
            repository,
            rewriteRecordApplicationService,
            governanceCapabilityClient
        );
    }

    private SqlRewriteRecord rewriteRecord(String rewriteRecordId, String originalSql, String recommendedSql) {
        return rewriteRecord(rewriteRecordId, originalSql, recommendedSql, RewriteValidationStatus.NOT_VALIDATED, null);
    }

    private SqlRewriteRecord rewriteRecord(String rewriteRecordId,
                                           String originalSql,
                                           String recommendedSql,
                                           RewriteValidationStatus validationStatus,
                                           Instant lastComparedAt) {
        return SqlRewriteRecord.builder()
            .rewriteRecordId(rewriteRecordId)
            .tenantId("tenant-a")
            .recommendationId("recommendation-001")
            .sourceType(GovernanceSourceType.QUERY)
            .sourceKind(GovernanceSourceKind.QUERY_HISTORY)
            .sourceId("history-001")
            .evidenceLevel(EvidenceLevel.RUNTIME_HISTORY)
            .historyId("history-001")
            .sqlFingerprint("fp-001")
            .datasourceCode("hetu_main")
            .status(RewriteRecordStatus.APPROVED)
            .validationStatus(validationStatus)
            .autoApplyAllowed(true)
            .manualReviewRequired(false)
            .publishStatus(RewritePublishStatus.PUBLISHED)
            .runtimeBindingId("rwb-" + rewriteRecordId)
            .runtimeRuleVersion("runtime-rewrite-v1")
            .lastComparedAt(lastComparedAt)
            .alertStatus(RewriteAlertStatus.NONE)
            .originalSqlText(originalSql)
            .recommendedSqlText(recommendedSql)
            .createdBy("operator-001")
            .createdAt(Instant.parse("2026-05-10T10:00:00Z"))
            .updatedAt(Instant.parse("2026-05-10T10:00:00Z"))
            .build();
    }

    private GovernanceSqlRewriteDivergenceAlertResponse alertResponse() {
        GovernanceSqlRewriteDivergenceAlertLinkage linkage = new GovernanceSqlRewriteDivergenceAlertLinkage();
        linkage.setAlertId("alert-rewrite-001");
        linkage.setAlertType("SQL_REWRITE_RESULT_DIVERGENCE");
        linkage.setAlertLevel("HIGH");
        linkage.setAlertStatus("OPEN");
        linkage.setNotifyStatus("SIMULATED_NOTIFIED");
        linkage.setDetailPath("/api/governance/alerts/alert-rewrite-001");
        linkage.setLinkageMode("EMITTED");
        linkage.setNotificationLogId("alert-notify-001");
        GovernanceSqlRewriteDivergenceAlertResponse response = new GovernanceSqlRewriteDivergenceAlertResponse();
        response.setRewriteRecordId("rewrite-001");
        response.setValidationRunId("validation-001");
        response.setAlertTriggered(Boolean.TRUE);
        response.setAlertLinkages(Collections.singletonList(linkage));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static final class FakeResultDigestClient implements QueryExecutionResultDigestClient {
        @Override
        public QueryExecutionResultDigestResponse executeDigest(QueryExecutionResultDigestRequest request) {
            String checksum = "checksum-original";
            if ("SELECT recommended".equals(request.getSqlText())) {
                checksum = "checksum-recommended";
            }
            return response(checksum);
        }

        private QueryExecutionResultDigestResponse response(String checksum) {
            QueryExecutionResultDigestResponse response = new QueryExecutionResultDigestResponse();
            response.setStatus("SUCCESS");
            Map<String, Object> digest = new LinkedHashMap<String, Object>();
            digest.put("schemaDigest", "schema");
            digest.put("rowCount", Long.valueOf(1L));
            digest.put("keySetDigest", "key");
            digest.put("orderDigest", "order");
            digest.put("checksumDigest", checksum);
            response.setResultDigest(digest);
            response.setLimitedSample(Collections.singletonList(row("id", checksum)));
            response.setExecutionEvidence(Collections.<String, Object>singletonMap("readonlyDigestOnly", Boolean.TRUE));
            return response;
        }

        private Map<String, Object> row(String key, String value) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put(key, value);
            return row;
        }
    }

    private static final class FakeRuntimeRewriteBindingClient implements QueryExecutionRuntimeRewriteBindingClient {
        private int pauseCount;
        private boolean failPause;
        private RuntimeRewriteBindingStateChangeRequest lastPauseRequest;

        @Override
        public RuntimeRewriteBindingResponse publish(RuntimeRewriteBindingPublishRequest request) {
            throw new UnsupportedOperationException("publish is not used by scheduler validation tests");
        }

        @Override
        public RuntimeRewriteBindingResponse pause(RuntimeRewriteBindingStateChangeRequest request) {
            pauseCount++;
            lastPauseRequest = request;
            if (failPause) {
                throw new IllegalStateException("runtime pause 不可用");
            }
            RuntimeRewriteBindingResponse response = new RuntimeRewriteBindingResponse();
            response.setTenantId(request.getTenantId());
            response.setRuntimeBindingId(request.getRuntimeBindingId());
            response.setRewriteRecordId(request.getRuntimeBindingId().substring("rwb-".length()));
            response.setSqlFingerprint(request.getSqlFingerprint());
            response.setStatus("PAUSED");
            response.setActive(false);
            response.setRuleVersion(Long.valueOf(1L));
            response.setRuntimeRuleVersion("runtime-rewrite-v1");
            response.setRuntimeSummary("paused by scheduled validation divergence");
            return response;
        }

        @Override
        public RuntimeRewriteBindingResponse unpublish(RuntimeRewriteBindingStateChangeRequest request) {
            throw new UnsupportedOperationException("unpublish is not used by scheduler validation tests");
        }
    }
}
