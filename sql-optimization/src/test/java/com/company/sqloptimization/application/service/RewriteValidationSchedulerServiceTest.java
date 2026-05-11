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
import com.company.sqloptimization.config.RewriteValidationSchedulerProperties;
import com.company.sqloptimization.domain.governance.EvidenceLevel;
import com.company.sqloptimization.domain.governance.GovernanceSourceKind;
import com.company.sqloptimization.domain.governance.GovernanceSourceType;
import com.company.sqloptimization.domain.governance.RewriteAlertStatus;
import com.company.sqloptimization.domain.governance.RewriteRecordStatus;
import com.company.sqloptimization.domain.governance.RewriteValidationStatus;
import com.company.sqloptimization.domain.rewrite.SqlRewriteRecord;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqloptimization.infrastructure.queryexecution.QueryExecutionResultDigestClient;
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
        GovernanceCapabilityClient governanceCapabilityClient = Mockito.mock(GovernanceCapabilityClient.class);
        when(governanceCapabilityClient.emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        )).thenReturn(alertResponse());
        RewriteValidationSchedulerService service = service(repository, governanceCapabilityClient, true);

        RewriteValidationSchedulerService.RewriteValidationSchedulerResult result =
            service.runScheduledValidationCycle();

        SqlRewriteRecord updated = repository.findRecordById("rewrite-001");
        assertEquals(1, result.getProcessedCount());
        assertEquals(1, result.getAlertEmissionCount());
        assertEquals(RewriteRecordStatus.PAUSED, updated.getStatus());
        assertEquals(RewriteValidationStatus.DIVERGED, updated.getValidationStatus());
        assertFalse(updated.isAutoApplyAllowed());
        assertEquals(RewriteAlertStatus.OPEN, updated.getAlertStatus());
        Map<String, Object> divergenceAlert = castMap(updated.getTraceRefs().get("divergenceAlert"));
        assertEquals("EMITTED_OR_DEDUPED", divergenceAlert.get("emissionStatus"));
        assertEquals("SQL_REWRITE_RESULT_DIVERGENCE", divergenceAlert.get("alertType"));

        ArgumentCaptor<GovernanceSqlRewriteDivergenceAlertRequest> captor =
            ArgumentCaptor.forClass(GovernanceSqlRewriteDivergenceAlertRequest.class);
        verify(governanceCapabilityClient).emitSqlRewriteDivergenceAlert(captor.capture());
        assertEquals("tenant-a", captor.getValue().getTenantId());
        assertEquals("rewrite-001", captor.getValue().getRewriteRecordId());
        assertEquals("DIVERGED", captor.getValue().getComparisonStatus());
        assertEquals("VALUE_DIFF", captor.getValue().getDifferenceType());
        assertEquals(Boolean.TRUE, captor.getValue().getAutoApplyPaused());
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
        verify(governanceCapabilityClient, never()).emitSqlRewriteDivergenceAlert(
            org.mockito.ArgumentMatchers.any(GovernanceSqlRewriteDivergenceAlertRequest.class)
        );
    }

    private RewriteValidationSchedulerService service(InMemorySqlRewriteRecordRepository repository,
                                                      GovernanceCapabilityClient governanceCapabilityClient,
                                                      boolean enabled) {
        RewriteValidationSchedulerProperties properties = new RewriteValidationSchedulerProperties();
        properties.setEnabled(enabled);
        properties.setBatchSize(10);
        properties.setMaxAgeMinutes(60L);
        SqlRewriteRecordApplicationService rewriteRecordApplicationService =
            new SqlRewriteRecordApplicationService(
                repository,
                new FakeResultDigestClient(),
                new ResultDigestComparisonEngine()
            );
        return new RewriteValidationSchedulerService(
            properties,
            repository,
            rewriteRecordApplicationService,
            governanceCapabilityClient
        );
    }

    private SqlRewriteRecord rewriteRecord(String rewriteRecordId, String originalSql, String recommendedSql) {
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
            .validationStatus(RewriteValidationStatus.NOT_VALIDATED)
            .autoApplyAllowed(true)
            .manualReviewRequired(false)
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
}
