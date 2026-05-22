package com.company.sqloptimization.domain.rewrite.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RewriteProductionAdapterStatus {

    private final String adapterName;
    private final boolean enabled;
    private final String status;
    private final String evidenceLevel;
    private final String claimBoundary;
    private final String integrationMode;
    private final String failureReason;
    private final List<String> evidence;
    private final Map<String, Object> attributes;

    public RewriteProductionAdapterStatus(String adapterName,
                                          boolean enabled,
                                          String status,
                                          String evidenceLevel,
                                          String claimBoundary,
                                          String integrationMode,
                                          String failureReason,
                                          List<String> evidence,
                                          Map<String, Object> attributes) {
        this.adapterName = text(adapterName);
        this.enabled = enabled;
        this.status = text(status);
        this.evidenceLevel = text(evidenceLevel);
        this.claimBoundary = text(claimBoundary);
        this.integrationMode = text(integrationMode);
        this.failureReason = text(failureReason);
        this.evidence = immutableStrings(evidence);
        this.attributes = immutableMap(attributes);
    }

    public static RewriteProductionAdapterStatus disabled(String adapterName, String claimBoundary) {
        return new RewriteProductionAdapterStatus(
            adapterName,
            false,
            "DISABLED_DEFAULT_STATIC_CHAIN",
            "STATIC_PARSE",
            claimBoundary,
            "CONFIG_SWITCH_DISABLED",
            "",
            Collections.singletonList("enabled=false"),
            Collections.<String, Object>emptyMap()
        );
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("adapterName", adapterName);
        result.put("enabled", Boolean.valueOf(enabled));
        result.put("status", status);
        result.put("evidenceLevel", evidenceLevel);
        result.put("claimBoundary", claimBoundary);
        result.put("integrationMode", integrationMode);
        result.put("failureReason", failureReason);
        result.put("evidence", evidence);
        result.put("attributes", attributes);
        return result;
    }

    public boolean isLiveAvailable() {
        return status.endsWith("_AVAILABLE") || "AVAILABLE".equals(status);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAdapterName() {
        return adapterName;
    }

    public String getStatus() {
        return status;
    }

    private static List<String> immutableStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            String text = text(value);
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
