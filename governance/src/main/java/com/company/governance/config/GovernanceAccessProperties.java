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
    private final Map<String, RolePolicyProperties> roleMatrix =
        new LinkedHashMap<String, RolePolicyProperties>();
    private final Map<String, ResourcePolicyProperties> resourceModel =
        new LinkedHashMap<String, ResourcePolicyProperties>();
    private final Map<String, Map<String, DatasourceAuthorizationProperties>> datasourceAuthorizationMatrix =
        new LinkedHashMap<String, Map<String, DatasourceAuthorizationProperties>>();

    public GovernanceAccessProperties() {
        roleMatrix.put("PLATFORM_ADMIN", role("*"));
        roleMatrix.put("TENANT_ADMIN", role(
            "query.execute",
            "optimization.submit",
            "optimization.status.read",
            "benchmark.submit",
            "benchmark.status.read",
            "benchmark.report.read",
            "governance.tenant-config.read",
            "governance.history.read",
            "governance.permission.manage"
        ));
        roleMatrix.put("OPERATOR", role(
            "query.execute",
            "optimization.submit",
            "optimization.status.read",
            "benchmark.submit",
            "benchmark.status.read",
            "benchmark.report.read",
            "governance.history.read"
        ));
        roleMatrix.put("ANALYST", role(
            "query.execute",
            "optimization.submit",
            "optimization.status.read"
        ));
        roleMatrix.put("AUDITOR", role("governance.history.read"));
        roleMatrix.put("READONLY", role("optimization.status.read", "benchmark.status.read", "benchmark.report.read"));

        resourceModel.put("QUERY_EXECUTION_QUERY", resource("QUERY_EXECUTE_SYNC", permission("query.execute", "USE")));
        resourceModel.put(
            "SQL_OPTIMIZATION_TASK",
            resource(
                "OPTIMIZATION_TASK_SUBMIT", permission("optimization.submit", "USE"),
                "OPTIMIZATION_TASK_STATUS_QUERY", permission("optimization.status.read", "USE")
            )
        );
        resourceModel.put(
            "BENCHMARK_ENGINE_TASK",
            resource(
                "BENCHMARK_TASK_SUBMIT", permission("benchmark.submit", "USE"),
                "BENCHMARK_TASK_STATUS_QUERY", permission("benchmark.status.read", "USE")
            )
        );
        resourceModel.put(
            "BENCHMARK_ENGINE_REPORT",
            resource("BENCHMARK_REPORT_QUERY", permission("benchmark.report.read", "EXPORT"))
        );
        resourceModel.put(
            "GOVERNANCE_TENANT_CONFIG",
            resource("TENANT_CONFIG_READ", permission("governance.tenant-config.read", "READ"))
        );
        resourceModel.put(
            "GOVERNANCE_HISTORY",
            resource("HISTORY_READ", permission("governance.history.read", "READ"))
        );
        resourceModel.put(
            "DATASOURCE_AUTHORIZATION_MATRIX",
            resource("PERMISSION_CHANGE", permission("governance.permission.manage", null))
        );

        datasourceAuthorizationMatrix.put("tenant-a", datasourceMatrix(
            "query-hetu", datasource("ACTIVE", "USE"),
            "query-hive", datasource("ACTIVE", "USE"),
            "optimization-hetu", datasource("ACTIVE", "USE"),
            "optimization-hive", datasource("ACTIVE", "USE"),
            "benchmark-hetu", datasource("ACTIVE", "USE", "EXPORT"),
            "benchmark-hive", datasource("ACTIVE", "USE", "EXPORT"),
            "governance-tenant-config", datasource("ACTIVE", "READ", "MANAGE")
        ));
        datasourceAuthorizationMatrix.put("system", datasourceMatrix(
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

    public Map<String, RolePolicyProperties> getRoleMatrix() {
        return roleMatrix;
    }

    public Map<String, ResourcePolicyProperties> getResourceModel() {
        return resourceModel;
    }

    public Map<String, Map<String, DatasourceAuthorizationProperties>> getDatasourceAuthorizationMatrix() {
        return datasourceAuthorizationMatrix;
    }

    public static class RolePolicyProperties {

        private final List<String> permissions = new ArrayList<String>();

        public List<String> getPermissions() {
            return permissions;
        }
    }

    public static class ResourcePolicyProperties {

        private final Map<String, OperationPolicyProperties> operations =
            new LinkedHashMap<String, OperationPolicyProperties>();

        public Map<String, OperationPolicyProperties> getOperations() {
            return operations;
        }
    }

    public static class OperationPolicyProperties {

        private final List<String> requiredPermissions = new ArrayList<String>();
        private String datasourceAction;

        public List<String> getRequiredPermissions() {
            return requiredPermissions;
        }

        public String getDatasourceAction() {
            return datasourceAction;
        }

        public void setDatasourceAction(String datasourceAction) {
            this.datasourceAction = datasourceAction;
        }
    }

    public static class DatasourceAuthorizationProperties {

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

    private static RolePolicyProperties role(String... permissions) {
        RolePolicyProperties properties = new RolePolicyProperties();
        properties.getPermissions().addAll(Arrays.asList(permissions));
        return properties;
    }

    private static ResourcePolicyProperties resource(Object... operationTuples) {
        ResourcePolicyProperties properties = new ResourcePolicyProperties();
        for (int index = 0; index < operationTuples.length; index += 2) {
            properties.getOperations().put(
                String.valueOf(operationTuples[index]),
                (OperationPolicyProperties) operationTuples[index + 1]
            );
        }
        return properties;
    }

    private static OperationPolicyProperties permission(String permission, String datasourceAction) {
        OperationPolicyProperties properties = new OperationPolicyProperties();
        properties.getRequiredPermissions().add(permission);
        properties.setDatasourceAction(datasourceAction);
        return properties;
    }

    private static Map<String, DatasourceAuthorizationProperties> datasourceMatrix(Object... datasourceTuples) {
        Map<String, DatasourceAuthorizationProperties> matrix =
            new LinkedHashMap<String, DatasourceAuthorizationProperties>();
        for (int index = 0; index < datasourceTuples.length; index += 2) {
            matrix.put(String.valueOf(datasourceTuples[index]), (DatasourceAuthorizationProperties) datasourceTuples[index + 1]);
        }
        return matrix;
    }

    private static DatasourceAuthorizationProperties datasource(String state, String... actions) {
        DatasourceAuthorizationProperties properties = new DatasourceAuthorizationProperties();
        properties.setState(state);
        properties.getActions().addAll(Arrays.asList(actions));
        return properties;
    }
}
