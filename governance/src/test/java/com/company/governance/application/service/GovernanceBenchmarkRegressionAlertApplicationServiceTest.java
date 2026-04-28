package com.company.governance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class GovernanceBenchmarkRegressionAlertApplicationServiceTest {

    @Test
    void shouldEmitBenchmarkRegressionAlertAndReturnLinkage() {
        AlertEmissionApplicationService alertEmissionApplicationService = mock(AlertEmissionApplicationService.class);
        AlertEventMapper alertEventMapper = mock(AlertEventMapper.class);
        GovernanceBenchmarkRegressionAlertApplicationService service =
            new GovernanceBenchmarkRegressionAlertApplicationService(alertEmissionApplicationService, alertEventMapper);

        AlertEvent emittedAlert = AlertEvent.builder()
            .alertId("alert-regression-001")
            .tenantId("tenant-a")
            .alertType(AlertEvent.AlertType.BENCHMARK_REGRESSION_FAILED)
            .summary("Regression guard hit 1 threshold(s): failed=1, warning=0.")
            .sqlFingerprint("fp-001")
            .logicalObjectKey("report-001")
            .createdBy("benchmark-regression-guard")
            .createdAt(Instant.parse("2026-04-27T19:00:00Z"))
            .build();
        AlertNotificationLogRecord notificationLog = new AlertNotificationLogRecord();
        notificationLog.setNotificationLogId("alert-notify-001");
        notificationLog.setAlertId("alert-regression-001");
        when(alertEmissionApplicationService.emit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new AlertEmissionApplicationService.AlertEmissionResult(
                1,
                1,
                0,
                Collections.singletonList(emittedAlert),
                Collections.singletonList(notificationLog)
            ));

        GovernanceBenchmarkRegressionAlertRequest request = new GovernanceBenchmarkRegressionAlertRequest();
        request.setTenantId("tenant-a");
        request.setReportId("report-001");
        request.setTaskId("task-001");
        request.setSqlFingerprint("fp-001");
        request.setVerdict("FAIL");
        request.setThresholdHitCount(Integer.valueOf(1));
        request.setFailedThresholdCount(Integer.valueOf(1));
        request.setWarningThresholdCount(Integer.valueOf(0));
        request.setSummary("Regression guard hit 1 threshold(s): failed=1, warning=0.");

        GovernanceBenchmarkRegressionAlertResponse response = service.emit(request);

        assertEquals(Boolean.TRUE, response.getAlertTriggered());
        assertEquals(1, response.getAlertLinkages().size());
        assertEquals("alert-regression-001", response.getAlertLinkages().get(0).getAlertId());
        assertEquals("EMITTED", response.getAlertLinkages().get(0).getLinkageMode());
        assertEquals("alert-notify-001", response.getAlertLinkages().get(0).getNotificationLogId());
        verify(alertEmissionApplicationService).emit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldReturnDedupedExistingAlertLinkage() {
        AlertEmissionApplicationService alertEmissionApplicationService = mock(AlertEmissionApplicationService.class);
        AlertEventMapper alertEventMapper = mock(AlertEventMapper.class);
        GovernanceBenchmarkRegressionAlertApplicationService service =
            new GovernanceBenchmarkRegressionAlertApplicationService(alertEmissionApplicationService, alertEventMapper);

        AlertNotificationLogRecord notificationLog = new AlertNotificationLogRecord();
        notificationLog.setNotificationLogId("alert-dedupe-001");
        notificationLog.setDeliveryStatus("DEDUPE_SUPPRESSED");
        notificationLog.setSourceAlertId("alert-existing-001");
        AlertEventRecord existing = new AlertEventRecord();
        existing.setAlertId("alert-existing-001");
        existing.setAlertType("BENCHMARK_REGRESSION_FAILED");
        existing.setAlertLevel("HIGH");
        existing.setAlertStatus("OPEN");
        existing.setNotifyStatus("SIMULATED_NOTIFIED");
        existing.setSummary("Existing regression alert");
        existing.setCreatedAt(LocalDateTime.ofInstant(Instant.parse("2026-04-27T19:10:00Z"), ZoneOffset.UTC));
        when(alertEventMapper.selectByAlertId("alert-existing-001")).thenReturn(existing);
        when(alertEmissionApplicationService.emit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new AlertEmissionApplicationService.AlertEmissionResult(
                1,
                0,
                1,
                Collections.<AlertEvent>emptyList(),
                Collections.singletonList(notificationLog)
            ));

        GovernanceBenchmarkRegressionAlertRequest request = new GovernanceBenchmarkRegressionAlertRequest();
        request.setTenantId("tenant-a");
        request.setReportId("report-002");
        request.setTaskId("task-002");
        request.setVerdict("FAIL");
        request.setThresholdHitCount(Integer.valueOf(1));
        request.setFailedThresholdCount(Integer.valueOf(1));

        GovernanceBenchmarkRegressionAlertResponse response = service.emit(request);

        assertEquals(1, response.getAlertLinkages().size());
        assertEquals("alert-existing-001", response.getAlertLinkages().get(0).getAlertId());
        assertEquals("DEDUPED_TO_EXISTING", response.getAlertLinkages().get(0).getLinkageMode());
        assertEquals("alert-dedupe-001", response.getAlertLinkages().get(0).getNotificationLogId());
    }
}
