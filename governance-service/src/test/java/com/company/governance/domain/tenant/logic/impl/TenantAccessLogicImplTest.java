package com.company.governance.domain.tenant.logic.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.governance.config.GovernanceAccessProperties;
import com.company.sqlforge.common.context.RequestContext;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAccessLogicImplTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldAllowGovernanceDatasourceForTenantAdmin() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(defaultProperties());
        RequestContext.set("tenant-a", "tenant-admin-001", Arrays.asList("TENANT_ADMIN"), "request-001", "trace-001", "header", 1L, 2L);

        assertTrue(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config"));
    }

    @Test
    void shouldRejectGovernanceDatasourceForUnauthorizedRole() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(defaultProperties());
        RequestContext.set("tenant-a", "operator-001", Arrays.asList("OPERATOR"), "request-001", "trace-001", "header", 1L, 2L);

        assertFalse(logic.validateDataSourceAccess("tenant-a", "governance-tenant-config"));
    }

    @Test
    void shouldAllowTenantBoundDatasourceForAuthorizedRole() {
        GovernanceAccessProperties properties = defaultProperties();
        properties.getPlaceholder().getTenantDatasourceBindings().put("tenant-a", Collections.singletonList("ds-01"));
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(properties);
        RequestContext.set("tenant-a", "analyst-001", Arrays.asList("ANALYST"), "request-001", "trace-001", "header", 1L, 2L);

        assertTrue(logic.validateDataSourceAccess("tenant-a", "ds-01"));
    }

    @Test
    void shouldRejectWhenBindingMissingOrDatasourceBlank() {
        TenantAccessLogicImpl logic = new TenantAccessLogicImpl(defaultProperties());
        RequestContext.set("tenant-a", "analyst-001", Arrays.asList("ANALYST"), "request-001", "trace-001", "header", 1L, 2L);

        assertFalse(logic.validateDataSourceAccess("tenant-a", "ds-01"));
        assertFalse(logic.validateDataSourceAccess("", "ds-01"));
        assertFalse(logic.validateDataSourceAccess("tenant-a", " "));
    }

    private GovernanceAccessProperties defaultProperties() {
        return new GovernanceAccessProperties();
    }
}
