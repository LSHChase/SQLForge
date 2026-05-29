package com.company.sqloptimization.application.service;

import com.company.sqlforge.common.context.RequestContext;
import com.company.sqlforge.common.queryexecution.QueryExecutionMaterializedViewCreateResponse;
import com.company.sqlforge.common.utils.JsonUtils;
import com.company.sqloptimization.application.controller.dto.MaterializedViewCreateRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

final class MaterializedViewCreateEvidenceBuilder {

    private static final String TRACE_KEY = "materializedViewCreateEvidence";

    private MaterializedViewCreateEvidenceBuilder() {
    }

    static Map<String, Object> withEvidence(Map<String, Object> traceRefs,
                                            QueryExecutionMaterializedViewCreateResponse response,
                                            Map<String, Object> artifact,
                                            MaterializedViewCreateRequest request) {
        Map<String, Object> result = traceRefs == null
            ? new LinkedHashMap<String, Object>()
            : new LinkedHashMap<String, Object>(traceRefs);
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("recommendationId", response.getRecommendationId());
        evidence.put("rewriteRecordId", response.getRewriteRecordId());
        evidence.put("mvName", response.getMvName());
        evidence.put("targetEngine", response.getTargetEngine());
        evidence.put("targetDatasource", response.getTargetDatasource());
        evidence.put("status", response.getStatus());
        evidence.put("ddlStatus", response.getDdlStatus());
        evidence.put("refreshStatus", response.getRefreshStatus());
        evidence.put("runtimeSummary", response.getRuntimeSummary());
        evidence.put("runtimeDetails", parseRuntimeDetails(response.getRuntimeDetailsJson()));
        evidence.put("artifactStatus", textValue(artifact.get("artifactStatus")));
        evidence.put("mvType", textValue(artifact.get("mvType")));
        evidence.put("createdAt", Instant.now().toString());
        evidence.put("operator", RequestContext.getUserId());
        String reason = trimToNull(request == null ? null : request.getReason());
        if (reason != null) {
            evidence.put("reason", reason);
        }
        result.put(TRACE_KEY, evidence);
        return result;
    }

    private static Object parseRuntimeDetails(String runtimeDetailsJson) {
        if (!StringUtils.hasText(runtimeDetailsJson)) {
            return null;
        }
        try {
            return JsonUtils.fromJson(runtimeDetailsJson, Map.class);
        } catch (RuntimeException ex) {
            return runtimeDetailsJson;
        }
    }

    private static String textValue(Object value) {
        return value == null ? null : trimToNull(String.valueOf(value));
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
