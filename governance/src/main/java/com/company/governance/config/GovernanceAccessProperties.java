package com.company.governance.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.access-control")
public class GovernanceAccessProperties {

    private final PlaceholderProperties placeholder = new PlaceholderProperties();

    public PlaceholderProperties getPlaceholder() {
        return placeholder;
    }

    public static class PlaceholderProperties {

        private boolean enabled = true;
        private boolean denyByDefault = true;
        private final List<String> governanceDatasourceIds = new ArrayList<String>();
        private final List<String> tenantConfigAllowedRoles = new ArrayList<String>();
        private final List<String> datasourceUseAllowedRoles = new ArrayList<String>();
        private final Map<String, List<String>> tenantDatasourceBindings =
            new LinkedHashMap<String, List<String>>();

        public PlaceholderProperties() {
            governanceDatasourceIds.add("governance-tenant-config");
            tenantConfigAllowedRoles.add("PLATFORM_ADMIN");
            tenantConfigAllowedRoles.add("TENANT_ADMIN");
            datasourceUseAllowedRoles.add("PLATFORM_ADMIN");
            datasourceUseAllowedRoles.add("TENANT_ADMIN");
            datasourceUseAllowedRoles.add("OPERATOR");
            datasourceUseAllowedRoles.add("ANALYST");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDenyByDefault() {
            return denyByDefault;
        }

        public void setDenyByDefault(boolean denyByDefault) {
            this.denyByDefault = denyByDefault;
        }

        public List<String> getGovernanceDatasourceIds() {
            return governanceDatasourceIds;
        }

        public List<String> getTenantConfigAllowedRoles() {
            return tenantConfigAllowedRoles;
        }

        public List<String> getDatasourceUseAllowedRoles() {
            return datasourceUseAllowedRoles;
        }

        public Map<String, List<String>> getTenantDatasourceBindings() {
            return tenantDatasourceBindings;
        }
    }
}
