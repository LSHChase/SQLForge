package com.company.governance.application.service;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GovernanceBenchmarkRegressionAlertApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "REGRESSION_ALERT_LINKAGE_BASELINE";

    private final AlertEmissionApplicationService alertEmissionApplicationService;
    private final AlertEventMapper alertEventMapper;

    public GovernanceBenchmarkRegressionAlertApplicationService(
        AlertEmissionApplicationService alertEmissionApplicationService,
        AlertEventMapper alertEventMapper
    ) {
        this.alertEmissionApplicationService = alertEmissionApplicationService;
        this.alertEventMapper = alertEventMapper;
    }

    public GovernanceBenchmarkRegressionAlertResponse emit(GovernanceBenchmarkRegressionAlertRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        String reportId = requireText(request == null ? null : request.getReportId(), "reportId");
        String taskId = requireText(request == null ? null : request.getTaskId(), "taskId");
        int thresholdHitCount = normalizeCount(request == null ? null : request.getThresholdHitCount());
        int failedThresholdCount = normalizeCount(request == null ? null : request.getFailedThresholdCount());
        int warningThresholdCount = normalizeCount(request == null ? null : request.getWarningThresholdCount());

        GovernanceBenchmarkRegressionAlertResponse response = new GovernanceBenchmarkRegressionAlertResponse();
        response.setReportId(reportId);
        response.setTaskId(taskId);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);

        AlertSignalSnapshot.BenchmarkRegressionSignal signal = new AlertSignalSnapshot.BenchmarkRegressionSignal(
            reportId,
            taskId,
            trimToNull(request.getHistoryId()),
            trimToNull(request.getSqlFingerprint()),
            trimToNull(request.getVerdict()),
            thresholdHitCount,
            failedThresholdCount,
            warningThresholdCount,
            trimToNull(request.getSummary()),
            trimToNull(request.getReportQueryPath()),
            trimToNull(request.getRawDataDownloadPath()),
            trimToNull(request.getThresholdAssessmentsJson()),
            trimToNull(request.getExecutionSummaryJson())
        );
        if (!signal.shouldAlert()) {
            response.setAlertTriggered(Boolean.FALSE);
            response.setAlertLinkages(Collections.<GovernanceBenchmarkRegressionAlertLinkage>emptyList());
            return response;
        }

        AlertEmissionApplicationService.AlertEmissionResult result = alertEmissionApplicationService.emit(
            AlertSignalSnapshot.builder()
                .tenantId(tenantId)
                .addBenchmarkRegressionSignal(signal)
                .build(),
            "benchmark-regression-guard",
            Instant.now()
        );

        response.setAlertTriggered(Boolean.TRUE);
        response.setAlertLinkages(buildLinkages(result));
        return response;
    }

    private List<GovernanceBenchmarkRegressionAlertLinkage> buildLinkages(
        AlertEmissionApplicationService.AlertEmissionResult result
    ) {
        if (result == null) {
            return Collections.emptyList();
        }
        Map<String, GovernanceBenchmarkRegressionAlertLinkage> linkages =
            new LinkedHashMap<String, GovernanceBenchmarkRegressionAlertLinkage>();
        for (AlertEvent alert : result.getEmittedAlerts()) {
            GovernanceBenchmarkRegressionAlertLinkage linkage = new GovernanceBenchmarkRegressionAlertLinkage();
            linkage.setAlertId(alert.getAlertId());
            linkage.setAlertType(alert.getAlertType().name());
            linkage.setAlertLevel(alert.getAlertLevel().name());
            linkage.setAlertStatus(alert.getAlertStatus().name());
            linkage.setNotifyStatus(alert.getNotifyStatus().name());
            linkage.setSummary(alert.getSummary());
            linkage.setDetailPath("/api/governance/alerts/" + alert.getAlertId());
            linkage.setLinkageMode("EMITTED");
            linkages.put(alert.getAlertId(), linkage);
        }
        for (AlertNotificationLogRecord log : result.getNotificationLogs()) {
            if (!"DEDUPE_SUPPRESSED".equals(log.getDeliveryStatus()) || !StringUtils.hasText(log.getSourceAlertId())) {
                if (StringUtils.hasText(log.getAlertId()) && linkages.containsKey(log.getAlertId())) {
                    linkages.get(log.getAlertId()).setNotificationLogId(log.getNotificationLogId());
                }
                continue;
            }
            AlertEventRecord source = alertEventMapper.selectByAlertId(log.getSourceAlertId());
            if (source == null) {
                continue;
            }
            GovernanceBenchmarkRegressionAlertLinkage linkage = new GovernanceBenchmarkRegressionAlertLinkage();
            linkage.setAlertId(source.getAlertId());
            linkage.setAlertType(source.getAlertType());
            linkage.setAlertLevel(source.getAlertLevel());
            linkage.setAlertStatus(source.getAlertStatus());
            linkage.setNotifyStatus(source.getNotifyStatus());
            linkage.setSummary(source.getSummary());
            linkage.setDetailPath("/api/governance/alerts/" + source.getAlertId());
            linkage.setLinkageMode("DEDUPED_TO_EXISTING");
            linkage.setNotificationLogId(log.getNotificationLogId());
            linkages.put(source.getAlertId(), linkage);
        }
        return Collections.unmodifiableList(new ArrayList<GovernanceBenchmarkRegressionAlertLinkage>(linkages.values()));
    }

    private int normalizeCount(Integer value) {
        return value == null ? 0 : Math.max(0, value.intValue());
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.BAD_REQUEST,
                fieldName + " 不能为空"
            );
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
