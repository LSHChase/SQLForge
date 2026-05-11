package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GovernanceSqlRewriteDivergenceAlertApplicationServiceTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldEmitDivergenceAlertAndReturnLinkage() {
        AlertEmissionApplicationService alertEmissionApplicationService = mock(AlertEmissionApplicationService.class);
        AlertEventMapper alertEventMapper = mock(AlertEventMapper.class);
        GovernanceSqlRewriteDivergenceAlertApplicationService service =
            new GovernanceSqlRewriteDivergenceAlertApplicationService(alertEmissionApplicationService, alertEventMapper);
        setProtectedContext();

        AlertEvent emittedAlert = AlertEvent.builder()
            .alertId("alert-rewrite-001")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.SQL_REWRITE_RESULT_DIVERGENCE)
            .summary("SQL rewrite result divergence: rewriteRecord=rewrite-001, differenceType=VALUE_DIFF")
            .historyId("history-001")
            .recommendationId("recommendation-001")
            .logicalObjectKey("rewrite-001")
            .sqlFingerprint("fp-001")
            .createdBy("sql-rewrite-validation-scheduler")
            .createdAt(Instant.parse("2026-05-10T19:00:00Z"))
            .build();
        AlertNotificationLogRecord notificationLog = new AlertNotificationLogRecord();
        notificationLog.setNotificationLogId("alert-notify-001");
        notificationLog.setAlertId("alert-rewrite-001");
        when(alertEmissionApplicationService.emit(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(new AlertEmissionApplicationService.AlertEmissionResult(
            1,
            1,
            0,
            Collections.singletonList(emittedAlert),
            Collections.singletonList(notificationLog)
        ));

        GovernanceSqlRewriteDivergenceAlertResponse response = service.emit(divergedRequest());

        assertEquals(Boolean.TRUE, response.getAlertTriggered());
        assertEquals("rewrite-001", response.getRewriteRecordId());
        assertEquals("validation-001", response.getValidationRunId());
        assertEquals("alert-rewrite-001", response.getAlertLinkages().get(0).getAlertId());
        assertEquals("SQL_REWRITE_RESULT_DIVERGENCE", response.getAlertLinkages().get(0).getAlertType());
        assertEquals("EMITTED", response.getAlertLinkages().get(0).getLinkageMode());
        assertEquals("alert-notify-001", response.getAlertLinkages().get(0).getNotificationLogId());
        verify(alertEmissionApplicationService).emit(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void shouldNotEmitWhenComparisonIsNotDiverged() {
        AlertEmissionApplicationService alertEmissionApplicationService = mock(AlertEmissionApplicationService.class);
        GovernanceSqlRewriteDivergenceAlertApplicationService service =
            new GovernanceSqlRewriteDivergenceAlertApplicationService(
                alertEmissionApplicationService,
                mock(AlertEventMapper.class)
            );
        setProtectedContext();
        GovernanceSqlRewriteDivergenceAlertRequest request = divergedRequest();
        request.setComparisonStatus("FAILED");

        GovernanceSqlRewriteDivergenceAlertResponse response = service.emit(request);

        assertEquals(Boolean.FALSE, response.getAlertTriggered());
        assertEquals(0, response.getAlertLinkages().size());
        verify(alertEmissionApplicationService, never()).emit(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }

    private GovernanceSqlRewriteDivergenceAlertRequest divergedRequest() {
        GovernanceSqlRewriteDivergenceAlertRequest request = new GovernanceSqlRewriteDivergenceAlertRequest();
        request.setTenantId("tenant-a");
        request.setSourceType("QUERY");
        request.setSourceKind("QUERY_HISTORY");
        request.setSourceId("history-001");
        request.setEvidenceLevel("RUNTIME_HISTORY");
        request.setHistoryId("history-001");
        request.setRecommendationId("recommendation-001");
        request.setRewriteRecordId("rewrite-001");
        request.setValidationRunId("validation-001");
        request.setSqlFingerprint("fp-001");
        request.setComparisonStatus("DIVERGED");
        request.setDifferenceType("VALUE_DIFF");
        request.setSampleEvidenceJson("{\"rows\":2}");
        request.setAutoApplyPaused(Boolean.TRUE);
        return request;
    }

    private void setProtectedContext() {
        RequestContext.set(
            "tenant-a",
            "service-user",
            Arrays.asList("SERVICE"),
            "request-001",
            "trace-001",
            "header",
            100L,
            200L
        );
    }
}
