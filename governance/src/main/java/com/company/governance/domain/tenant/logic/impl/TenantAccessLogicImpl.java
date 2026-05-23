package com.company.governance.domain.tenant.logic.impl;

import com.company.governance.config.GovernanceAccessProperties;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantAccessLogicImpl implements TenantAccessLogic {

    private static final String STATE_ACTIVE = "ACTIVE";

    private final GovernanceAccessProperties governanceAccessProperties;

    public TenantAccessLogicImpl(GovernanceAccessProperties governanceAccessProperties) {
        this.governanceAccessProperties = governanceAccessProperties;
    }

    @Override
    public boolean validateDataSourceAccess(String tenantId, String dataSourceId, String action) {
        if (!governanceAccessProperties.isEnabled()
            || !StringUtils.hasText(tenantId)
            || !StringUtils.hasText(dataSourceId)
            || !StringUtils.hasText(action)) {
            return false;
        }
        Map<String, GovernanceAccessProperties.DatasourceAccessScopeProperties> datasourceScopes =
            governanceAccessProperties.getDatasourceScopes().get(tenantId);
        if (datasourceScopes == null) {
            return false;
        }
        GovernanceAccessProperties.DatasourceAccessScopeProperties datasourceScope = datasourceScopes.get(dataSourceId);
        return datasourceScope != null
            && STATE_ACTIVE.equalsIgnoreCase(datasourceScope.getState())
            && datasourceScope.getActions().contains(action.trim().toUpperCase());
    }
}
