package com.company.governance.application.service;

import com.company.governance.domain.alert.AlertEvent;
import com.company.governance.domain.alert.AlertSignalSnapshot;
import com.company.governance.infrastructure.persistence.entity.AlertEventRecord;
import com.company.governance.infrastructure.persistence.entity.AlertNotificationLogRecord;
import com.company.governance.infrastructure.persistence.mapper.AlertEventMapper;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.exception.BizException;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertLinkage;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertRequest;
import com.company.sqlforge.common.governance.GovernanceSqlRewriteDivergenceAlertResponse;
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
public class GovernanceSqlRewriteDivergenceAlertApplicationService {

    private static final String CONTRACT_STAGE = "LONG_TERM_BASELINE";
    private static final String IMPLEMENTATION_STAGE = "SQL_REWRITE_DIVERGENCE_ALERT_LINKAGE_BASELINE";

    private final AlertEmissionApplicationService alertEmissionApplicationService;
    private final AlertEventMapper alertEventMapper;

    public GovernanceSqlRewriteDivergenceAlertApplicationService(
        AlertEmissionApplicationService alertEmissionApplicationService,
        AlertEventMapper alertEventMapper
    ) {
        this.alertEmissionApplicationService = alertEmissionApplicationService;
        this.alertEventMapper = alertEventMapper;
    }

    public GovernanceSqlRewriteDivergenceAlertResponse emit(GovernanceSqlRewriteDivergenceAlertRequest request) {
        String tenantId = requireText(request == null ? null : request.getTenantId(), "tenantId");
        requireProtectedTenantContext(tenantId);
        String rewriteRecordId = requireText(request == null ? null : request.getRewriteRecordId(), "rewriteRecordId");
        String validationRunId = requireText(request == null ? null : request.getValidationRunId(), "validationRunId");
        GovernanceSqlRewriteDivergenceAlertResponse response = new GovernanceSqlRewriteDivergenceAlertResponse();
        response.setRewriteRecordId(rewriteRecordId);
        response.setValidationRunId(validationRunId);
        response.setContractStage(CONTRACT_STAGE);
        response.setImplementationStage(IMPLEMENTATION_STAGE);

        AlertSignalSnapshot.SqlRewriteDivergenceSignal signal =
            new AlertSignalSnapshot.SqlRewriteDivergenceSignal(
                trimToNull(request.getSourceType()),
                trimToNull(request.getSourceKind()),
                trimToNull(request.getSourceId()),
                trimToNull(request.getEvidenceLevel()),
                trimToNull(request.getHistoryId()),
                trimToNull(request.getParseHistoryId()),
                trimToNull(request.getRecommendationId()),
                rewriteRecordId,
                validationRunId,
                trimToNull(request.getPlanId()),
                trimToNull(request.getSqlFingerprint()),
                trimToNull(request.getComparisonStatus()),
                trimToNull(request.getDifferenceType()),
                trimToNull(request.getSampleEvidenceJson()),
                Boolean.TRUE.equals(request.getAutoApplyPaused()),
                trimToNull(request.getSummary())
            );
        if (!signal.shouldAlert()) {
            response.setAlertTriggered(Boolean.FALSE);
            response.setAlertLinkages(Collections.<GovernanceSqlRewriteDivergenceAlertLinkage>emptyList());
            return response;
        }

        AlertEmissionApplicationService.AlertEmissionResult result = alertEmissionApplicationService.emit(
            AlertSignalSnapshot.builder()
                .tenantId(tenantId)
                .addSqlRewriteDivergenceSignal(signal)
                .build(),
            "sql-rewrite-validation-scheduler",
            Instant.now()
        );

        response.setAlertTriggered(Boolean.TRUE);
        response.setAlertLinkages(buildLinkages(result));
        return response;
    }

    private List<GovernanceSqlRewriteDivergenceAlertLinkage> buildLinkages(
        AlertEmissionApplicationService.AlertEmissionResult result
    ) {
        if (result == null) {
            return Collections.emptyList();
        }
        Map<String, GovernanceSqlRewriteDivergenceAlertLinkage> linkages =
            new LinkedHashMap<String, GovernanceSqlRewriteDivergenceAlertLinkage>();
        for (AlertEvent alert : result.getEmittedAlerts()) {
            GovernanceSqlRewriteDivergenceAlertLinkage linkage = new GovernanceSqlRewriteDivergenceAlertLinkage();
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
            GovernanceSqlRewriteDivergenceAlertLinkage linkage = new GovernanceSqlRewriteDivergenceAlertLinkage();
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
        return Collections.unmodifiableList(
            new ArrayList<GovernanceSqlRewriteDivergenceAlertLinkage>(linkages.values())
        );
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

    private void requireProtectedTenantContext(String tenantId) {
        String contextTenantId = RequestContext.getTenantId();
        if (!StringUtils.hasText(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_CONTEXT_MISSING,
                HttpStatus.UNAUTHORIZED,
                "受保护治理请求上下文缺失"
            );
        }
        if (!tenantId.equals(contextTenantId)) {
            throw new BizException(
                ErrorCodeConstants.SYSTEM_INVALID_ARGUMENT,
                HttpStatus.FORBIDDEN,
                "请求 tenantId 与受保护上下文不一致"
            );
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
