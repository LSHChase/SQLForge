package com.company.governance.domain.tenant.logic.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.config.GovernanceAccessProperties;
import org.junit.jupiter.api.Test;

class TenantAccessLogicImplTest {

    @Test
    void shouldAllowGovernanceDatasourceForConfiguredReadAction() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());

        assertTrue(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config", "READ"));
    }

    @Test
    void shouldRejectGovernanceDatasourceForUnconfiguredAction() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());

        assertFalse(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config", "EXPORT"));
    }

    @Test
    void shouldAllowTenantDatasourceWhenRoleHasMatchingPermission() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());

        assertTrue(logic.validateDataSourceAccess("tenant-a", "query-hetu", "USE"));
    }

    @Test
    void shouldRejectRevokedOrUnknownDatasource() {
        GovernanceAccessProperties properties = new GovernanceAccessProperties();
        properties.getDatasourceScopes().get("tenant-a").get("query-hetu").setState("REVOKED");
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(properties);

        assertFalse(logic.validateDataSourceAccess("tenant-a", "query-hetu", "USE"));
        assertFalse(logic.validateDataSourceAccess("tenant-a", "missing-datasource", "USE"));
        assertFalse(logic.validateDataSourceAccess("", "query-hetu", "USE"));
    }
}
