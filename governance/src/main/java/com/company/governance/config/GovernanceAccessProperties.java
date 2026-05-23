package com.company.governance.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "governance.access-control")
public class GovernanceAccessProperties {

    private boolean enabled = true;
    private boolean denyByDefault = true;
    private boolean auditDecisions = true;
    private final Map<String, Map<String, DatasourceAccessScopeProperties>> datasourceScopes =
        new LinkedHashMap<String, Map<String, DatasourceAccessScopeProperties>>();

    public GovernanceAccessProperties() {
        datasourceScopes.put("tenant-a", datasourceScopes(
            "query-trino", datasource("ACTIVE", "USE"),
            "query-hetu", datasource("ACTIVE", "USE"),
            "query-hive", datasource("ACTIVE", "USE"),
            "optimization-hetu", datasource("ACTIVE", "USE"),
            "optimization-hive", datasource("ACTIVE", "USE"),
            "benchmark-hetu", datasource("ACTIVE", "USE", "EXPORT"),
            "benchmark-hive", datasource("ACTIVE", "USE", "EXPORT"),
            "governance-tenant-config", datasource("ACTIVE", "READ", "MANAGE")
        ));
        datasourceScopes.put("system", datasourceScopes(
            "governance-tenant-config", datasource("ACTIVE", "READ", "MANAGE")
        ));
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

    public boolean isAuditDecisions() {
        return auditDecisions;
    }

    public void setAuditDecisions(boolean auditDecisions) {
        this.auditDecisions = auditDecisions;
    }

    public Map<String, Map<String, DatasourceAccessScopeProperties>> getDatasourceScopes() {
        return datasourceScopes;
    }

    public static class DatasourceAccessScopeProperties {

        private String state = "ACTIVE";
        private final List<String> actions = new ArrayList<String>();

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public List<String> getActions() {
            return actions;
        }
    }

    private static Map<String, DatasourceAccessScopeProperties> datasourceScopes(Object... datasourceTuples) {
        Map<String, DatasourceAccessScopeProperties> scopes =
            new LinkedHashMap<String, DatasourceAccessScopeProperties>();
        for (int index = 0; index < datasourceTuples.length; index += 2) {
            scopes.put(String.valueOf(datasourceTuples[index]), (DatasourceAccessScopeProperties) datasourceTuples[index + 1]);
        }
        return scopes;
    }

    private static DatasourceAccessScopeProperties datasource(String state, String... actions) {
        DatasourceAccessScopeProperties properties = new DatasourceAccessScopeProperties();
        properties.setState(state);
        properties.getActions().addAll(Arrays.asList(actions));
        return properties;
    }
}
