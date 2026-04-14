package com.sqlforge.backend.service;

import com.sqlforge.backend.model.TenantProfile;

public interface TenantProfileProvider {

    TenantProfile resolve(String tenantId);
}
