package com.company.queryexecution.application.service;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.application.controller.vo.QueryErrorDetailVO;
import com.company.queryexecution.application.controller.vo.QueryExecuteResponse;
import com.company.queryexecution.application.controller.vo.QueryExecutionMetadataVO;
import com.company.queryexecution.application.controller.vo.QueryRetryStepVO;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class QueryExecutionMetricsRecorder {

    private static final String METRIC_REQUESTS = "sqlforge.query.execution.requests";
    private static final String METRIC_LATENCY = "sqlforge.query.execution.latency";
    private static final String METRIC_MODE_HITS = "sqlforge.query.execution.mode.hits";
    private static final String METRIC_MODE_ATTEMPTS = "sqlforge.query.execution.mode.attempts";
    private static final String METRIC_TIMEOUTS = "sqlforge.query.execution.timeouts";
    private static final String METRIC_FALLBACKS = "sqlforge.query.execution.fallbacks";
    private static final String METRIC_ROUTE_UNAVAILABLE = "sqlforge.query.execution.route_不可用";
    private static final String METRIC_CACHE_GOVERNANCE = "sqlforge.query.execution.cache.governance";
    private static final String UNKNOWN_VALUE = "UNKNOWN";
    private static final String NONE_VALUE = "NONE";

    private final MeterRegistry meterRegistry;

    public QueryExecutionMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    static QueryExecutionMetricsRecorder noop() {
        return new QueryExecutionMetricsRecorder(new SimpleMeterRegistry());
    }

    public void recordResponse(QueryExecuteRequest request, QueryExecuteResponse response, long costMs) {
        String requestedDatasource = datasourceName(request == null ? null : request.getDatasourceType());
        String faultTolerance = faultToleranceName(request);
        String targetEngine = targetEngine(response);
        String executionMode = executionMode(response);
        String resultStatus = response == null || response.getStatus() == null
            ? UNKNOWN_VALUE
            : response.getStatus().name();
        String degraded = Boolean.toString(response != null && response.isDegraded());

        Counter.builder(METRIC_REQUESTS)
            .description("query-execution 终态响应总数。")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", targetEngine,
                "result_status", resultStatus,
                "degraded", degraded,
                "execution_mode", executionMode,
                "fault_tolerance", faultTolerance
            )
            .register(meterRegistry)
            .increment();

        Timer.builder(METRIC_LATENCY)
            .description("query-execution 端到端延迟。")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", targetEngine,
                "result_status", resultStatus,
                "degraded", degraded
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);

        Counter.builder(METRIC_MODE_HITS)
            .description("query-execution 返回的最终执行模式。")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", targetEngine,
                "mode", executionMode
            )
            .register(meterRegistry)
            .increment();

        recordAttemptedModes(requestedDatasource, attemptedModes(response));
        recordCacheGovernance(targetEngine, response);

        QueryErrorDetailVO error = response == null ? null : response.getError();
        if (isTimeoutResponse(response)) {
            Counter.builder(METRIC_TIMEOUTS)
                .description("query-execution 到达终态前的超时次数。")
                .tags(
                    "requested_datasource", requestedDatasource,
                    "target_engine", targetEngine,
                    "fault_tolerance", faultTolerance
                )
                .register(meterRegistry)
                .increment();
        }
        if (response != null && response.isDegraded()) {
            Counter.builder(METRIC_FALLBACKS)
                .description("query-execution 降级或回退的终态响应数。")
                .tags(
                    "requested_datasource", requestedDatasource,
                    "target_engine", targetEngine,
                    "fault_tolerance", faultTolerance,
                    "execution_mode", executionMode
                )
                .register(meterRegistry)
                .increment();
        }
        if (error != null && error.getCode() == ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE) {
            Counter.builder(METRIC_ROUTE_UNAVAILABLE)
                .description("query-execution 路由不可用的终态响应数。")
                .tags(
                    "requested_datasource", requestedDatasource,
                    "target_engine", targetEngine,
                    "fault_tolerance", faultTolerance
                )
                .register(meterRegistry)
                .increment();
        }
    }

    public void recordException(QueryExecuteRequest request, long costMs) {
        String requestedDatasource = datasourceName(request == null ? null : request.getDatasourceType());
        Counter.builder(METRIC_REQUESTS)
            .description("query-execution 终态响应总数。")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", resolveRequestedTarget(request),
                "result_status", "FAILED_EXCEPTION",
                "degraded", "false",
                "execution_mode", NONE_VALUE,
                "fault_tolerance", faultToleranceName(request)
            )
            .register(meterRegistry)
            .increment();
        Timer.builder(METRIC_LATENCY)
            .description("query-execution 端到端延迟。")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", resolveRequestedTarget(request),
                "result_status", "FAILED_EXCEPTION",
                "degraded", "false"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    private void recordCacheGovernance(String targetEngine, QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        if (metadata == null || metadata.getCacheGovernanceStatus() == null) {
            return;
        }
        String status = normalizeTag(metadata.getCacheGovernanceStatus());
        String evidence = metadata.getCacheGovernanceEvidence();
        recordCacheEvent(targetEngine, status, cacheEvent(status, evidence), riskCode(evidence), evictionReason(evidence));
        if ("BACKFILLED".equals(status) && containsEvidence(evidence, "providerReadStatus=MISS")) {
            recordCacheEvent(targetEngine, status, "MISS", riskCode(evidence), evictionReason(evidence));
        }
        if (containsEvidence(evidence, "invalidatedEntryCount=")) {
            recordCacheEvent(targetEngine, status, "INVALIDATE", riskCode(evidence), evictionReason(evidence));
        }
        if (containsEvidence(evidence, "riskCode=DISTRIBUTED_BACKEND_UNAVAILABLE")) {
            recordCacheEvent(targetEngine, status, "BACKEND_UNAVAILABLE", "DISTRIBUTED_BACKEND_UNAVAILABLE", evictionReason(evidence));
        }
    }

    private void recordCacheEvent(String targetEngine,
                                  String status,
                                  String event,
                                  String riskCode,
                                  String evictionReason) {
        Counter.builder(METRIC_CACHE_GOVERNANCE)
            .description("受治理查询结果缓存事件数。")
            .tags(
                "target_engine", targetEngine == null ? NONE_VALUE : targetEngine,
                "status", normalizeTag(status),
                "event", normalizeTag(event),
                "risk_code", normalizeTag(riskCode),
                "eviction_reason", normalizeTag(evictionReason)
            )
            .register(meterRegistry)
            .increment();
    }

    private String cacheEvent(String status, String evidence) {
        if ("HIT".equals(status)) {
            return "HIT";
        }
        if ("BYPASSED".equals(status)) {
            return "BYPASS";
        }
        if ("BACKFILLED".equals(status)) {
            return "BACKFILL";
        }
        if ("INVALIDATED".equals(status) || containsEvidence(evidence, "invalidatedEntryCount=")) {
            return "INVALIDATE";
        }
        return status;
    }

    private boolean containsEvidence(String evidence, String pattern) {
        return evidence != null && evidence.contains(pattern);
    }

    private String riskCode(String evidence) {
        return evidenceValue(evidence, "riskCode");
    }

    private String evictionReason(String evidence) {
        return evidenceValue(evidence, "evictionReason");
    }

    private String evidenceValue(String evidence, String key) {
        if (evidence == null || key == null) {
            return NONE_VALUE;
        }
        String prefix = key + "=";
        String[] parts = evidence.split(";");
        for (String part : parts) {
            if (part != null && part.startsWith(prefix)) {
                String value = part.substring(prefix.length()).trim();
                return value.isEmpty() ? NONE_VALUE : value;
            }
        }
        return NONE_VALUE;
    }

    private String normalizeTag(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE_VALUE;
        }
        return value.trim().toUpperCase();
    }

    private void recordAttemptedModes(String requestedDatasource, List<String> attemptedModes) {
        if (attemptedModes == null || attemptedModes.isEmpty()) {
            return;
        }
        for (String attemptedMode : attemptedModes) {
            ModeAttempt modeAttempt = parseAttemptedMode(attemptedMode);
            Counter.builder(METRIC_MODE_ATTEMPTS)
                .description("观测到的 query-execution 模式链路尝试次数。")
                .tags(
                    "requested_datasource", requestedDatasource,
                    "mode", modeAttempt.mode,
                    "outcome", modeAttempt.outcome
                )
                .register(meterRegistry)
                .increment();
        }
    }

    private ModeAttempt parseAttemptedMode(String attemptedMode) {
        if (attemptedMode == null || attemptedMode.trim().isEmpty()) {
            return new ModeAttempt(UNKNOWN_VALUE, UNKNOWN_VALUE);
        }
        String normalized = attemptedMode.trim().toUpperCase();
        if (normalized.startsWith("CHAIN_")) {
            return new ModeAttempt("CHAIN", normalized.substring("CHAIN_".length()));
        }
        int separatorIndex = normalized.indexOf(':');
        if (separatorIndex < 0) {
            return new ModeAttempt(normalized, "SELECTED");
        }
        return new ModeAttempt(
            normalized.substring(0, separatorIndex),
            normalized.substring(separatorIndex + 1)
        );
    }

    private List<String> attemptedModes(QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        return metadata == null ? Collections.<String>emptyList() : metadata.getAttemptedModes();
    }

    private boolean isTimeoutResponse(QueryExecuteResponse response) {
        QueryErrorDetailVO error = response == null ? null : response.getError();
        if (error != null && error.getCode() == ErrorCodeConstants.QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT) {
            return true;
        }
        if (response == null || response.getRetryPath() == null) {
            return false;
        }
        for (QueryRetryStepVO retryStep : response.getRetryPath()) {
            if (retryStep != null && "LOCAL_TIMEOUT_ROLLBACK_MARKED".equals(retryStep.getLocalRecoveryMarker())) {
                return true;
            }
        }
        return false;
    }

    private String targetEngine(QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        return metadata == null || metadata.getTargetEngine() == null ? NONE_VALUE : metadata.getTargetEngine();
    }

    private String executionMode(QueryExecuteResponse response) {
        QueryExecutionMetadataVO metadata = response == null ? null : response.getMetadata();
        return metadata == null || metadata.getExecutionMode() == null ? NONE_VALUE : metadata.getExecutionMode();
    }

    private String faultToleranceName(QueryExecuteRequest request) {
        return request == null || request.getFaultToleranceStrategy() == null
            ? UNKNOWN_VALUE
            : request.getFaultToleranceStrategy().name();
    }

    private String datasourceName(DataSourceTypeEnum datasourceType) {
        return datasourceType == null ? UNKNOWN_VALUE : datasourceType.name();
    }

    private String resolveRequestedTarget(QueryExecuteRequest request) {
        return request == null || request.getDatasourceType() == null
            ? UNKNOWN_VALUE
            : request.getDatasourceType().name();
    }

    private static final class ModeAttempt {

        private final String mode;
        private final String outcome;

        private ModeAttempt(String mode, String outcome) {
            this.mode = mode;
            this.outcome = outcome;
        }
    }
}
