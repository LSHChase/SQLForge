package com.company.governance.domain.tenant.logic;

public interface TenantAccessLogic {

    boolean validateDataSourceAccess(String tenantId, String dataSourceId);
}
