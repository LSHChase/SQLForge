package com.company.queryexecution.application.service;

import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.HetuClusterEvidenceSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationModeSnapshot;
import com.company.queryexecution.domain.query.HetuRouteCalibrationSnapshot;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.infrastructure.adapter.HetuExecutionModeAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HetuRouteCalibrationService {

    private final QueryExecutionHetuProperties properties;
    private final Map<QueryExecutionAccessMode, HetuExecutionModeAdapter> modeAdapters;

    public HetuRouteCalibrationService(QueryExecutionHetuProperties properties,
                                       List<HetuExecutionModeAdapter> adapters) {
        this.properties = properties;
        Map<QueryExecutionAccessMode, HetuExecutionModeAdapter> adapterIndex =
            new LinkedHashMap<QueryExecutionAccessMode, HetuExecutionModeAdapter>();
        for (HetuExecutionModeAdapter adapter : adapters) {
            if (adapter != null && adapter.getMode() != null) {
                adapterIndex.put(adapter.getMode(), adapter);
            }
        }
        this.modeAdapters = Collections.unmodifiableMap(adapterIndex);
    }

    public HetuRouteCalibrationSnapshot currentSnapshot() {
        List<QueryExecutionAccessMode> declaredAllowedModes = deduplicate(properties.getAllowedModes());
        List<QueryExecutionAccessMode> calibratedPriority =
            deduplicate(properties.getCalibration().getRouteOrder());
        List<QueryExecutionAccessMode> effectiveRouteOrder = mergeRouteOrder(declaredAllowedModes, calibratedPriority);
        List<HetuRouteCalibrationModeSnapshot> modeCalibrations =
            new ArrayList<HetuRouteCalibrationModeSnapshot>(QueryExecutionAccessMode.values().length);
        for (QueryExecutionAccessMode mode : QueryExecutionAccessMode.values()) {
            modeCalibrations.add(buildModeSnapshot(
                mode,
                declaredAllowedModes,
                calibratedPriority,
                effectiveRouteOrder
            ));
        }
        HetuClusterEvidenceSnapshot clusterEvidence = buildClusterEvidence();
        return new HetuRouteCalibrationSnapshot(
            properties.isEnabled(),
            properties.getCalibration().getProfile(),
            declaredAllowedModes,
            effectiveRouteOrder,
            properties.getCalibration().isSkipUnreadyModes(),
            clusterEvidence.getEvidenceSource(),
            clusterEvidence.getLiveVerificationStatus(),
            clusterEvidence.getReadonlyBoundary(),
            buildSummary(effectiveRouteOrder, modeCalibrations, clusterEvidence),
            clusterEvidence,
            modeCalibrations
        );
    }

    public String classifyFailure(RuntimeException exception) {
        String message = messageOf(exception).toLowerCase(Locale.ROOT);
        if (message.contains("timeout")) {
            return "FAILED_TIMEOUT";
        }
        if (message.contains("auth") || message.contains("unauthorized") || message.contains("forbidden")) {
            return "FAILED_AUTH";
        }
        if (message.contains("not configured")
            || message.contains("disabled")
            || message.contains("missing")) {
            return "FAILED_CONFIGURATION";
        }
        if (message.contains("connection")
            || message.contains("connect")
            || message.contains("refused")
            || message.contains("unreachable")
            || message.contains("i/o error")) {
            return "FAILED_CONNECTIVITY";
        }
        return "FAILED_EXECUTION";
    }

    private HetuRouteCalibrationModeSnapshot buildModeSnapshot(QueryExecutionAccessMode mode,
                                                               List<QueryExecutionAccessMode> declaredAllowedModes,
                                                               List<QueryExecutionAccessMode> calibratedPriority,
                                                               List<QueryExecutionAccessMode> effectiveRouteOrder) {
        boolean allowed = declaredAllowedModes.contains(mode);
        boolean calibrationPreferred = calibratedPriority.contains(mode);
        boolean adapterAvailable = modeAdapters.containsKey(mode);
        boolean configured = isConfigured(mode);
        String readinessStatus = readinessStatus(mode, allowed, adapterAvailable, configured);
        String readinessReason = readinessReason(mode, allowed, adapterAvailable, configured);
        boolean ready = allowed && adapterAvailable && configured;
        boolean willAttemptInCurrentPolicy = effectiveRouteOrder.contains(mode)
            && adapterAvailable
            && (configured || !properties.getCalibration().isSkipUnreadyModes());
        return new HetuRouteCalibrationModeSnapshot(
            mode,
            effectiveRouteOrder.indexOf(mode) >= 0 ? effectiveRouteOrder.indexOf(mode) + 1 : 0,
            allowed,
            calibrationPreferred,
            adapterAvailable,
            configured,
            ready,
            willAttemptInCurrentPolicy,
            readinessStatus,
            readinessReason,
            routeParameters(mode)
        );
    }

    private HetuClusterEvidenceSnapshot buildClusterEvidence() {
        QueryExecutionHetuProperties.ClusterEvidence clusterEvidence = properties.getClusterEvidence();
        return new HetuClusterEvidenceSnapshot(
            clusterEvidence.getEvidenceSource(),
            clusterEvidence.getEnvironmentLabel(),
            clusterEvidence.getClusterName(),
            clusterEvidence.getCoordinatorEndpoint(),
            clusterEvidence.getRunbookRef(),
            clusterEvidence.getEvidenceRef(),
            clusterEvidence.getReadonlyBoundary(),
            clusterEvidence.getLiveVerificationStatus(),
            clusterEvidence.getEvidenceNotes()
        );
    }

    private String buildSummary(List<QueryExecutionAccessMode> effectiveRouteOrder,
                                List<HetuRouteCalibrationModeSnapshot> modeCalibrations,
                                HetuClusterEvidenceSnapshot clusterEvidence) {
        List<String> readyModes = new ArrayList<String>();
        for (HetuRouteCalibrationModeSnapshot snapshot : modeCalibrations) {
            if (snapshot != null && snapshot.isReady()) {
                readyModes.add(snapshot.getMode().name());
            }
        }
        return "profile=" + properties.getCalibration().getProfile()
            + ";effectiveRouteOrder=" + effectiveRouteOrder
            + ";skipUnreadyModes=" + properties.getCalibration().isSkipUnreadyModes()
            + ";readyModes=" + readyModes
            + ";evidenceSource=" + clusterEvidence.getEvidenceSource()
            + ";liveVerificationStatus=" + clusterEvidence.getLiveVerificationStatus();
    }

    private Map<String, Object> routeParameters(QueryExecutionAccessMode mode) {
        Map<String, Object> parameters = new LinkedHashMap<String, Object>();
        if (mode == QueryExecutionAccessMode.JDBC) {
            parameters.put("urlConfigured", Boolean.valueOf(StringUtils.hasText(properties.getJdbc().getUrl())));
            parameters.put("queryTimeoutSeconds", Integer.valueOf(properties.getJdbc().getQueryTimeoutSeconds()));
            parameters.put("maxRows", Integer.valueOf(properties.getJdbc().getMaxRows()));
            return parameters;
        }
        if (mode == QueryExecutionAccessMode.REST) {
            parameters.put("endpointConfigured", Boolean.valueOf(StringUtils.hasText(properties.getRest().getEndpoint())));
            parameters.put("authTokenConfigured", Boolean.valueOf(StringUtils.hasText(properties.getRest().getAuthToken())));
            parameters.put("connectTimeoutMs", Integer.valueOf(properties.getRest().getConnectTimeoutMs()));
            parameters.put("readTimeoutMs", Integer.valueOf(properties.getRest().getReadTimeoutMs()));
            parameters.put("maxRows", Integer.valueOf(properties.getRest().getMaxRows()));
            return parameters;
        }
        parameters.put("clientEnabled", Boolean.valueOf(properties.getClient().isEnabled()));
        parameters.put("endpointConfigured", Boolean.valueOf(StringUtils.hasText(properties.getClient().getEndpoint())));
        parameters.put("userConfigured", Boolean.valueOf(StringUtils.hasText(properties.getClient().getUser())));
        parameters.put("catalogConfigured", Boolean.valueOf(StringUtils.hasText(properties.getClient().getCatalog())));
        parameters.put("schemaConfigured", Boolean.valueOf(StringUtils.hasText(properties.getClient().getSchema())));
        parameters.put("authTokenConfigured", Boolean.valueOf(StringUtils.hasText(properties.getClient().getAuthToken())));
        parameters.put("connectTimeoutMs", Integer.valueOf(properties.getClient().getConnectTimeoutMs()));
        parameters.put("readTimeoutMs", Integer.valueOf(properties.getClient().getReadTimeoutMs()));
        parameters.put("maxRows", Integer.valueOf(properties.getClient().getMaxRows()));
        parameters.put("maxPages", Integer.valueOf(properties.getClient().getMaxPages()));
        return parameters;
    }

    private boolean isConfigured(QueryExecutionAccessMode mode) {
        if (mode == QueryExecutionAccessMode.JDBC) {
            return StringUtils.hasText(properties.getJdbc().getUrl());
        }
        if (mode == QueryExecutionAccessMode.REST) {
            return StringUtils.hasText(properties.getRest().getEndpoint());
        }
        return properties.getClient().isEnabled()
            && StringUtils.hasText(properties.getClient().getEndpoint())
            && StringUtils.hasText(properties.getClient().getUser());
    }

    private String readinessStatus(QueryExecutionAccessMode mode,
                                   boolean allowed,
                                   boolean adapterAvailable,
                                   boolean configured) {
        if (!allowed) {
            return "NOT_ALLOWED";
        }
        if (!adapterAvailable) {
            return "ADAPTER_UNAVAILABLE";
        }
        if (mode == QueryExecutionAccessMode.CLIENT && !properties.getClient().isEnabled()) {
            return "DISABLED";
        }
        if (!configured) {
            return "UNCONFIGURED";
        }
        return "READY";
    }

    private String readinessReason(QueryExecutionAccessMode mode,
                                   boolean allowed,
                                   boolean adapterAvailable,
                                   boolean configured) {
        if (!allowed) {
            return "该模式未包含在 query-execution.hetu.allowed-modes 中。";
        }
        if (!adapterAvailable) {
            return "该模式未注册 HetuExecutionModeAdapter Bean。";
        }
        if (mode == QueryExecutionAccessMode.JDBC && !configured) {
            return "query-execution.hetu.jdbc.url 为空。";
        }
        if (mode == QueryExecutionAccessMode.REST && !configured) {
            return "query-execution.hetu.rest.endpoint 为空。";
        }
        if (mode == QueryExecutionAccessMode.CLIENT && !properties.getClient().isEnabled()) {
            return "query-execution.hetu.client.enabled=false，客户端模式已禁用。";
        }
        if (mode == QueryExecutionAccessMode.CLIENT && !configured) {
            return "query-execution.hetu.client.endpoint 或 user 为空。";
        }
        return "模式已准备好用于校准路由。";
    }

    private List<QueryExecutionAccessMode> mergeRouteOrder(List<QueryExecutionAccessMode> declaredAllowedModes,
                                                           List<QueryExecutionAccessMode> calibratedPriority) {
        List<QueryExecutionAccessMode> merged = new ArrayList<QueryExecutionAccessMode>();
        for (QueryExecutionAccessMode mode : calibratedPriority) {
            if (declaredAllowedModes.contains(mode) && !merged.contains(mode)) {
                merged.add(mode);
            }
        }
        for (QueryExecutionAccessMode mode : declaredAllowedModes) {
            if (!merged.contains(mode)) {
                merged.add(mode);
            }
        }
        return Collections.unmodifiableList(merged);
    }

    private List<QueryExecutionAccessMode> deduplicate(List<QueryExecutionAccessMode> modes) {
        List<QueryExecutionAccessMode> deduplicated = new ArrayList<QueryExecutionAccessMode>();
        if (modes == null) {
            return deduplicated;
        }
        for (QueryExecutionAccessMode mode : modes) {
            if (mode != null && !deduplicated.contains(mode)) {
                deduplicated.add(mode);
            }
        }
        return deduplicated;
    }

    private String messageOf(RuntimeException exception) {
        if (exception == null) {
            return "";
        }
        if (StringUtils.hasText(exception.getMessage())) {
            return exception.getMessage();
        }
        Throwable cause = exception.getCause();
        return cause == null || !StringUtils.hasText(cause.getMessage()) ? "" : cause.getMessage();
    }
}
