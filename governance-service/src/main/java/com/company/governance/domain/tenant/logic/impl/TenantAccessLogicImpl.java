package com.company.governance.domain.tenant.logic.impl;

import com.company.governance.config.GovernanceAccessProperties;
import com.company.governance.domain.tenant.logic.TenantAccessLogic;
import com.company.sqlforge.common.context.RequestContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class TenantAccessLogicImpl implements TenantAccessLogic {

    private final GovernanceAccessProperties governanceAccessProperties;

    public TenantAccessLogicImpl(GovernanceAccessProperties governanceAccessProperties) {
        this.governanceAccessProperties = governanceAccessProperties;
    }

    @Override
    public boolean validateDataSourceAccess(String tenantId, String dataSourceId) {
        GovernanceAccessProperties.PlaceholderProperties placeholder = governanceAccessProperties.getPlaceholder();
        if (!placeholder.isEnabled()
            || !StringUtils.hasText(tenantId)
            || !StringUtils.hasText(dataSourceId)) {
            return false;
        }
        if (placeholder.getGovernanceDatasourceIds().contains(dataSourceId)) {
            return hasAnyRole(placeholder.getTenantConfigAllowedRoles());
        }
        if (!hasAnyRole(placeholder.getDatasourceUseAllowedRoles())) {
            return false;
        }
        List<String> tenantBindings = placeholder.getTenantDatasourceBindings().get(tenantId);
        if (CollectionUtils.isEmpty(tenantBindings)) {
            return !placeholder.isDenyByDefault();
        }
        return tenantBindings.contains(dataSourceId);
    }

    private boolean hasAnyRole(List<String> roleCodes) {
        if (CollectionUtils.isEmpty(roleCodes)) {
            return false;
        }
        for (String roleCode : roleCodes) {
            if (RequestContext.hasRole(roleCode)) {
                return true;
            }
        }
        return false;
    }
}
