package com.company.governance.domain.tenant.logic.impl;

import com.company.governance.config.GovernanceAccessProperties;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.context.RequestContext;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantAccessLogicImpl implements TenantAccessLogic {

    private static final String GOVERNANCE_DATASOURCE_ID = "governance-tenant-config";
    private static final String STATE_ACTIVE = "ACTIVE";

    private final GovernanceAccessProperties governanceAccessProperties;

    public TenantAccessLogicImpl(GovernanceAccessProperties governanceAccessProperties) {
        this.governanceAccessProperties = governanceAccessProperties;
    }

    @Override
    public boolean validateDataSourceAccess(String tenantId, String dataSourceId) {
        if (!governanceAccessProperties.isEnabled()
            || !StringUtils.hasText(tenantId)
            || !StringUtils.hasText(dataSourceId)) {
            return false;
        }
        Map<String, GovernanceAccessProperties.DatasourceAuthorizationProperties> datasourcePolicies =
            governanceAccessProperties.getDatasourceAuthorizationMatrix().get(tenantId);
        if (datasourcePolicies == null) {
            return false;
        }
        GovernanceAccessProperties.DatasourceAuthorizationProperties datasourcePolicy = datasourcePolicies.get(dataSourceId);
        if (datasourcePolicy == null || !STATE_ACTIVE.equalsIgnoreCase(datasourcePolicy.getState())) {
            return false;
        }

        Set<String> permissions = resolvePermissions();
        if (GOVERNANCE_DATASOURCE_ID.equals(dataSourceId)) {
            return permissions.contains("*")
                || permissions.contains("governance.tenant-config.read")
                || permissions.contains("governance.permission.manage");
        }
        return permissions.contains("*")
            || permissions.contains("query.execute")
            || permissions.contains("optimization.submit")
            || permissions.contains("optimization.status.read")
            || permissions.contains("benchmark.submit")
            || permissions.contains("benchmark.status.read")
            || permissions.contains("benchmark.report.read");
    }

    private Set<String> resolvePermissions() {
        Set<String> permissions = new LinkedHashSet<String>();
        for (String roleCode : RequestContext.getRoleCodes()) {
            GovernanceAccessProperties.RolePolicyProperties rolePolicy =
                governanceAccessProperties.getRoleMatrix().get(roleCode);
            if (rolePolicy != null) {
                permissions.addAll(rolePolicy.getPermissions());
            }
        }
        return permissions;
    }
}
