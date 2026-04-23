package com.company.governance.domain.tenant.logic.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAccessLogicImplTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldAllowGovernanceDatasourceForTenantAdmin() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());
        RequestContext.set(
            "tenant-a",
            "tenant-admin-001",
            Arrays.asList("TENANT_ADMIN"),
            "request-001",
            "trace-001",
            "header",
            1L,
            2L
        );

        assertTrue(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config"));
    }

    @Test
    void shouldRejectGovernanceDatasourceForUnauthorizedRole() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());
        RequestContext.set(
            "tenant-a",
            "operator-001",
            Arrays.asList("OPERATOR"),
            "request-002",
            "trace-002",
            "header",
            1L,
            2L
        );

        assertFalse(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config"));
    }

    @Test
    void shouldAllowTenantDatasourceWhenRoleHasMatchingPermission() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(new GovernanceAccessProperties());
        RequestContext.set(
            "tenant-a",
            "analyst-001",
            Arrays.asList("ANALYST"),
            "request-003",
            "trace-003",
            "header",
            1L,
            2L
        );

        assertTrue(logic.validateDataSourceAccess("tenant-a", "query-hetu"));
    }

    @Test
    void shouldRejectRevokedOrUnknownDatasource() {
        GovernanceAccessProperties properties = new GovernanceAccessProperties();
        properties.getDatasourceAuthorizationMatrix().get("tenant-a").get("query-hetu").setState("REVOKED");
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(properties);
        RequestContext.set(
            "tenant-a",
            "analyst-001",
            Arrays.asList("ANALYST"),
            "request-004",
            "trace-004",
            "header",
            1L,
            2L
        );

        assertFalse(logic.validateDataSourceAccess("tenant-a", "query-hetu"));
        assertFalse(logic.validateDataSourceAccess("tenant-a", "missing-datasource"));
        assertFalse(logic.validateDataSourceAccess("", "query-hetu"));
    }
}
