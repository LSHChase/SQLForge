package com.company.common.constants;

/**
 * Global error code ranges.
 */
public final class ErrorCodeConstants {

    public static final int SYSTEM_UNKNOWN_ERROR = 10000;
    public static final int SYSTEM_UNAUTHORIZED = 10001;
    public static final int SYSTEM_INVALID_ARGUMENT = 10002;
    public static final int SYSTEM_ACCESS_DENIED = 10003;
    public static final int SYSTEM_RESOURCE_NOT_FOUND = 10004;

    public static final int GOVERNANCE_BASE = 20000;
    public static final int ERROR_ACCESS_DENIED = 20001;
    public static final String ERROR_ACCESS_DENIED_MESSAGE = "当前租户无权访问目标资源";
    public static final int QUERY_GATEWAY_BASE = 21000;
    public static final int SQL_PARSER_BASE = 22000;
    public static final int SQL_REWRITE_BASE = 23000;
    public static final int EXECUTION_BASE = 24000;
    public static final int METADATA_BASE = 25000;
    public static final int AUDIT_BASE = 26000;
    public static final int BENCHMARK_BASE = 27000;
    public static final int SCHEDULER_BASE = 28000;
    public static final int ACCELERATION_BASE = 29000;

    private ErrorCodeConstants() {
    }
}
