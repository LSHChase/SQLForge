package com.sqlforge.backend.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TenantProfile {

    private final String id;
    private final int maxConcurrency;
    private final int maxMemoryGb;
    private final int maxScanTbPerHour;

    public TenantProfile(String id, int maxConcurrency, int maxMemoryGb, int maxScanTbPerHour) {
        this.id = id;
        this.maxConcurrency = maxConcurrency;
        this.maxMemoryGb = maxMemoryGb;
        this.maxScanTbPerHour = maxScanTbPerHour;
    }

    public String getId() {
        return id;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public int getMaxMemoryGb() {
        return maxMemoryGb;
    }

    public int getMaxScanTbPerHour() {
        return maxScanTbPerHour;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> profile = new LinkedHashMap<String, Object>();
        profile.put("id", id);
        profile.put("maxConcurrency", Integer.valueOf(maxConcurrency));
        profile.put("maxMemoryGb", Integer.valueOf(maxMemoryGb));
        profile.put("maxScanTbPerHour", Integer.valueOf(maxScanTbPerHour));
        return profile;
    }
}
