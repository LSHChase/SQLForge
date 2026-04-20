package com.company.sqlforge.common.constants;

/**
 * Unified error-code ownership across shared and governance capabilities.
 */
public final class ErrorCodeConstants {

    public static final int SYSTEM_UNKNOWN_ERROR = 10000;
    public static final int SYSTEM_INVALID_ARGUMENT = 10001;
    public static final int SYSTEM_UNAUTHORIZED = 10002;
    public static final int SYSTEM_ACCESS_DENIED = 10003;
    public static final int SYSTEM_RESOURCE_NOT_FOUND = 10004;
    public static final int SYSTEM_CONTEXT_MISSING = 10005;
    public static final int SYSTEM_CONTEXT_EXPIRED = 10006;
    public static final int SYSTEM_CONFIG_INVALID = 10007;
    public static final int SYSTEM_MESSAGE_MODE_INVALID = 10008;
    public static final int SYSTEM_AUDIT_CONTRACT_INVALID = 10009;

    public static final int GOVERNANCE_SYSTEM_AUTH_INVALID = 11000;
    public static final int GOVERNANCE_SYSTEM_TENANT_CONTEXT_INVALID = 11001;
    public static final int GOVERNANCE_SYSTEM_MESSAGE_ROUTE_INVALID = 11002;

    public static final int GOVERNANCE_ACCESS_DENIED = 20000;
    public static final int GOVERNANCE_TENANT_ACCESS_DENIED = 20001;
    public static final int GOVERNANCE_DATASOURCE_ACCESS_DENIED = 20002;
    public static final int GOVERNANCE_TENANT_CONFIG_NOT_FOUND = 20003;
    public static final int GOVERNANCE_MESSAGE_RETRY_FAILED = 20004;
    public static final int GOVERNANCE_MESSAGE_STATS_UNAVAILABLE = 20005;

    public static final String GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE = "当前租户无权访问目标租户配置";
    public static final String GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE = "当前租户无权访问治理数据源";

    private ErrorCodeConstants() {
    }
}
