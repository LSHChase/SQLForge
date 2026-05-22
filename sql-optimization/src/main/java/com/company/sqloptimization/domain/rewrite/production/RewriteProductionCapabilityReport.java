package com.company.sqloptimization.domain.rewrite.production;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RewriteProductionCapabilityReport {

    public static final String SCHEMA_VERSION = "rewrite-production-capability/v1";

    private final String schemaVersion;
    private final String capabilityStatus;
    private final List<RewriteProductionAdapterStatus> adapters;
    private final Map<String, Object> attributes;

    public RewriteProductionCapabilityReport(String capabilityStatus,
                                             List<RewriteProductionAdapterStatus> adapters,
                                             Map<String, Object> attributes) {
        this.schemaVersion = SCHEMA_VERSION;
        this.capabilityStatus = text(capabilityStatus);
        this.adapters = immutableAdapters(adapters);
        this.attributes = immutableMap(attributes);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", schemaVersion);
        result.put("capabilityStatus", capabilityStatus);
        result.put("adapters", adapterMaps());
        result.put("attributes", attributes);
        return result;
    }

    public Map<String, Object> toSummaryMap() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", schemaVersion);
        result.put("capabilityStatus", capabilityStatus);
        result.put("enabledAdapterCount", Integer.valueOf(enabledAdapterCount()));
        result.put("liveAvailableAdapterCount", Integer.valueOf(liveAvailableAdapterCount()));
        result.put("claimBoundary", attributes.get("claimBoundary"));
        result.put("defaultRuntimeBoundary", attributes.get("defaultRuntimeBoundary"));
        return result;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public String getCapabilityStatus() {
        return capabilityStatus;
    }

    public List<RewriteProductionAdapterStatus> getAdapters() {
        return adapters;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    private List<Map<String, Object>> adapterMaps() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (RewriteProductionAdapterStatus adapter : adapters) {
            result.add(adapter.toMap());
        }
        return Collections.unmodifiableList(result);
    }

    private int enabledAdapterCount() {
        int count = 0;
        for (RewriteProductionAdapterStatus adapter : adapters) {
            if (adapter.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private int liveAvailableAdapterCount() {
        int count = 0;
        for (RewriteProductionAdapterStatus adapter : adapters) {
            if (adapter.isLiveAvailable()) {
                count++;
            }
        }
        return count;
    }

    private static List<RewriteProductionAdapterStatus> immutableAdapters(List<RewriteProductionAdapterStatus> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<RewriteProductionAdapterStatus>(values));
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
