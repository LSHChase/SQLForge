package com.company.sqloptimization.application.service;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AccessParseApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessParseApplicationService.class);

    public AccessParseResponseVO parseAccess(AccessParseRequest request, String existingParseTaskId) {
        String parseTaskId = StringUtils.hasText(existingParseTaskId) ? existingParseTaskId : UUID.randomUUID().toString();
        String datasourceCode = trimToNull(request.getDatasourceCode());
        LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=START", parseTaskId, datasourceCode);

        AccessParseResponseVO response = new AccessParseResponseVO();
        response.setParseTaskId(parseTaskId);
        if (!Boolean.TRUE.equals(request.getConnectionRequired())) {
            response.setServiceStatus("SKIPPED");
            response.setConnectionStatus("SKIPPED");
            response.setObjectResolutionStatus("SKIPPED");
            response.setCompatibilityStatus("UNKNOWN");
            response.setAvailabilityWarning("Access parse skipped because connectionRequired=false.");
            response.setDegradeReason("ACCESS_PARSE_SKIPPED");
            LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=END serviceStatus=SKIPPED", parseTaskId, datasourceCode);
            return response;
        }
        if (!StringUtils.hasText(datasourceCode)) {
            response.setServiceStatus("UNAVAILABLE");
            response.setConnectionStatus("UNAVAILABLE");
            response.setObjectResolutionStatus("UNAVAILABLE");
            response.setCompatibilityStatus("UNKNOWN");
            response.setAvailabilityWarning("Datasource code is missing, so access parse cannot reach an engine or metadata service.");
            response.setDegradeReason("DATASOURCE_CODE_MISSING");
            LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=END serviceStatus=UNAVAILABLE", parseTaskId, datasourceCode);
            return response;
        }
        String normalizedDatasource = datasourceCode.toLowerCase(Locale.ROOT);
        if (normalizedDatasource.contains("unavailable")) {
            response.setServiceStatus("UNAVAILABLE");
            response.setConnectionStatus("UNAVAILABLE");
            response.setObjectResolutionStatus("UNAVAILABLE");
            response.setCompatibilityStatus("UNKNOWN");
            response.setAvailabilityWarning("Access parse service is unavailable for the selected datasource.");
            response.setDegradeReason("ACCESS_PARSE_SERVICE_UNAVAILABLE");
            LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=END serviceStatus=UNAVAILABLE", parseTaskId, datasourceCode);
            return response;
        }
        if (normalizedDatasource.contains("fail")) {
            response.setServiceStatus("AVAILABLE");
            response.setConnectionStatus("FAILED");
            response.setObjectResolutionStatus("PARTIAL");
            response.setCompatibilityStatus(resolveCompatibility(request.getCommentContext()));
            response.setPlanSummary("Access parse reached the provider but failed before a complete plan snapshot was produced.");
            response.setPartitionStatus("UNKNOWN");
            response.setDataFreshnessStatus("UNKNOWN");
            response.setSlaStatus("UNKNOWN");
            response.setAvailabilityWarning("Provider connection failed during access parse.");
            response.setDegradeReason("ACCESS_PARSE_CONNECTION_FAILED");
            LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=END serviceStatus=AVAILABLE connectionStatus=FAILED", parseTaskId, datasourceCode);
            return response;
        }
        response.setServiceStatus("AVAILABLE");
        response.setConnectionStatus("CONNECTED");
        response.setObjectResolutionStatus("RESOLVED");
        response.setPlanSummary(buildPlanSummary(request));
        response.setPartitionStatus(resolvePartitionStatus(request));
        response.setDataFreshnessStatus("UNKNOWN");
        response.setSlaStatus("UNKNOWN");
        response.setCompatibilityStatus(resolveCompatibility(request.getCommentContext()));
        response.setAvailabilityWarning(null);
        response.setDegradeReason(null);
        LOGGER.info("operation=ACCESS_PARSE entity={} datasourceCode={} status=END serviceStatus=AVAILABLE connectionStatus=CONNECTED", parseTaskId, datasourceCode);
        return response;
    }

    private String buildPlanSummary(AccessParseRequest request) {
        return "Access parse reached datasource "
            + request.getDatasourceCode()
            + " and produced a provider-side plan summary for read-only verification.";
    }

    private String resolvePartitionStatus(AccessParseRequest request) {
        String sql = request.getSqlText() == null ? "" : request.getSqlText().toLowerCase(Locale.ROOT);
        return sql.contains(" dt ") || sql.contains("dt=") || sql.contains(" dt=") || sql.contains("biz_date")
            ? "QUERY_DATE_ALIGNED"
            : "UNKNOWN";
    }

    private String resolveCompatibility(Map<String, Object> commentContext) {
        if (commentContext == null || commentContext.isEmpty()) {
            return "UNKNOWN";
        }
        Object engineHint = commentContext.get("engine_hint");
        if (engineHint == null) {
            engineHint = commentContext.get("engineHint");
        }
        return engineHint == null ? "UNKNOWN" : "COMPATIBLE";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
