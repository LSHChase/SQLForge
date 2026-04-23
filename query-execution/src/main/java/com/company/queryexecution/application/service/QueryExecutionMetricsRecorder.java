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
    private static final String METRIC_ROUTE_UNAVAILABLE = "sqlforge.query.execution.route_unavailable";
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
            .description("Total query-execution terminal responses.")
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
            .description("End-to-end query-execution latency.")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", targetEngine,
                "result_status", resultStatus,
                "degraded", degraded
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);

        Counter.builder(METRIC_MODE_HITS)
            .description("Final execution modes returned by query-execution.")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", targetEngine,
                "mode", executionMode
            )
            .register(meterRegistry)
            .increment();

        recordAttemptedModes(requestedDatasource, attemptedModes(response));

        QueryErrorDetailVO error = response == null ? null : response.getError();
        if (isTimeoutResponse(response)) {
            Counter.builder(METRIC_TIMEOUTS)
                .description("Query-execution timeouts before a terminal response.")
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
                .description("Query-execution degraded or fallback terminal responses.")
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
                .description("Query-execution route-unavailable terminal responses.")
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
            .description("Total query-execution terminal responses.")
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
            .description("End-to-end query-execution latency.")
            .tags(
                "requested_datasource", requestedDatasource,
                "target_engine", resolveRequestedTarget(request),
                "result_status", "FAILED_EXCEPTION",
                "degraded", "false"
            )
            .register(meterRegistry)
            .record(costMs, TimeUnit.MILLISECONDS);
    }

    private void recordAttemptedModes(String requestedDatasource, List<String> attemptedModes) {
        if (attemptedModes == null || attemptedModes.isEmpty()) {
            return;
        }
        for (String attemptedMode : attemptedModes) {
            ModeAttempt modeAttempt = parseAttemptedMode(attemptedMode);
            Counter.builder(METRIC_MODE_ATTEMPTS)
                .description("Observed query-execution mode-chain attempts.")
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
