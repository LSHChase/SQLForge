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

    public static final int QUERY_EXECUTION_SYSTEM_ROUTE_UNAVAILABLE = 12000;
    public static final int QUERY_EXECUTION_SYSTEM_ENGINE_TIMEOUT = 12001;
    public static final int QUERY_EXECUTION_SYSTEM_PARSER_FAILURE = 12002;
    public static final int QUERY_EXECUTION_SYSTEM_PIPELINE_NOT_READY = 12003;
    public static final int SQL_OPTIMIZATION_SYSTEM_PIPELINE_NOT_READY = 13000;
    public static final int SQL_OPTIMIZATION_SYSTEM_STATE_TRANSITION_INVALID = 13001;
    public static final int SQL_OPTIMIZATION_SYSTEM_CALLBACK_CONTRACT_INVALID = 13002;
    public static final int BENCHMARK_ENGINE_SYSTEM_PIPELINE_NOT_READY = 14000;
    public static final int BENCHMARK_ENGINE_SYSTEM_STATE_TRANSITION_INVALID = 14001;
    public static final int BENCHMARK_ENGINE_SYSTEM_REPORT_MODEL_INVALID = 14002;

    public static final int GOVERNANCE_ACCESS_DENIED = 20000;
    public static final int GOVERNANCE_TENANT_ACCESS_DENIED = 20001;
    public static final int GOVERNANCE_DATASOURCE_ACCESS_DENIED = 20002;
    public static final int GOVERNANCE_TENANT_CONFIG_NOT_FOUND = 20003;
    public static final int GOVERNANCE_MESSAGE_RETRY_FAILED = 20004;
    public static final int GOVERNANCE_MESSAGE_STATS_UNAVAILABLE = 20005;

    public static final int QUERY_EXECUTION_RISK_REJECTED = 21000;
    public static final int QUERY_EXECUTION_ROUTE_REJECTED = 21001;
    public static final int QUERY_EXECUTION_RESULT_LIMIT_EXCEEDED = 21002;
    public static final int SQL_OPTIMIZATION_TASK_INVALID = 22000;
    public static final int SQL_OPTIMIZATION_TASK_NOT_FOUND = 22001;
    public static final int SQL_OPTIMIZATION_TASK_ALREADY_FINISHED = 22002;
    public static final int SQL_OPTIMIZATION_SUGGESTION_NOT_READY = 22003;
    public static final int BENCHMARK_TASK_INVALID = 23000;
    public static final int BENCHMARK_TASK_NOT_FOUND = 23001;
    public static final int BENCHMARK_REPORT_NOT_FOUND = 23002;
    public static final int BENCHMARK_ISOLATION_POLICY_REJECTED = 23003;

    public static final String GOVERNANCE_TENANT_ACCESS_DENIED_MESSAGE = "当前租户无权访问目标租户配置";
    public static final String GOVERNANCE_DATASOURCE_ACCESS_DENIED_MESSAGE = "当前租户无权访问治理数据源";
    public static final String QUERY_EXECUTION_PIPELINE_NOT_READY_MESSAGE = "联机查询同步执行闭环尚未就绪";
    public static final String SQL_OPTIMIZATION_PIPELINE_NOT_READY_MESSAGE = "SQL 优化异步任务骨架尚未接入真实队列与持久化";
    public static final String BENCHMARK_ENGINE_PIPELINE_NOT_READY_MESSAGE = "压测引擎任务与报告模型已固化，但提交流程、执行链路和报告查询接口仍待接入";

    private ErrorCodeConstants() {
    }
}
