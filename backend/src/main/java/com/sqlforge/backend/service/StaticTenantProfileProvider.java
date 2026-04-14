package com.sqlforge.backend.service;

import com.sqlforge.backend.model.TenantProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StaticTenantProfileProvider implements TenantProfileProvider {

    private final Map<String, TenantProfile> profiles = new LinkedHashMap<String, TenantProfile>();

    public StaticTenantProfileProvider() {
        profiles.put("tenant-a", new TenantProfile("tenant-a", 20, 100, 1));
        profiles.put("tenant-b", new TenantProfile("tenant-b", 50, 200, 2));
    }

    @Override
    public TenantProfile resolve(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            return defaultProfile("default-tenant");
        }

        TenantProfile profile = profiles.get(tenantId);
        return profile == null ? defaultProfile(tenantId) : profile;
    }

    private TenantProfile defaultProfile(String tenantId) {
        return new TenantProfile(tenantId, 20, 100, 1);
    }
}
