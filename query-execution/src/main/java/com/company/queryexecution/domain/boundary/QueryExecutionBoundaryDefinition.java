package com.company.queryexecution.domain.boundary;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Canonical boundary definition for the query execution service.
 */
public final class QueryExecutionBoundaryDefinition {

    /**
     * Capabilities owned by the query execution service.
     */
    public enum Capability {
        QUERY_SUBMISSION,
        ROUTING_DECISION,
        EXECUTION_CONTROL,
        RESULT_AGGREGATION,
        LIGHTWEIGHT_SQL_PARSING,
        LIGHTWEIGHT_SQL_REWRITE,
        APPROVED_ACCELERATION_APPLICATION
    }

    /**
     * Capabilities explicitly excluded from the query execution service.
     */
    public enum ExcludedCapability {
        TENANT_AND_ROLE_ADMINISTRATION,
        DATASOURCE_POLICY_GOVERNANCE,
        ASYNC_DEEP_OPTIMIZATION,
        ACCELERATION_STRATEGY_MANAGEMENT,
        BENCHMARK_ORCHESTRATION
    }

    /**
     * Governance interactions required before the service executes user traffic.
     */
    public enum GovernanceDependency {
        TENANT_SCOPE_CHECK,
        DATASOURCE_ACCESS_CHECK,
        AUDIT_WRITE
    }

    /**
     * Supported Hetu access modes defined by ADR-004.
     */
    public enum ExecutionAccessMode {
        JDBC,
        REST,
        CLIENT
    }

    /**
     * Parser integration boundary defined by ADR-005.
     */
    public enum ParserBoundaryMode {
        OPEN_SOURCE_ABSTRACTION_FOR_LIGHTWEIGHT_PATH
    }

    /**
     * Read-only policy defined by ADR-005 and repository rules.
     */
    public enum ReadonlyPolicy {
        READONLY_FIRST
    }

    /**
     * Acceleration ownership boundary defined by ADR-013.
     */
    public enum AccelerationOwnershipMode {
        APPLY_APPROVED_RUNTIME_CONFIG_ONLY
    }

    private final List<Capability> ownedCapabilities;
    private final List<ExcludedCapability> excludedCapabilities;
    private final List<GovernanceDependency> governanceDependencies;
    private final List<ExecutionAccessMode> hetuAccessModes;
    private final ParserBoundaryMode parserBoundaryMode;
    private final ReadonlyPolicy readonlyPolicy;
    private final AccelerationOwnershipMode accelerationOwnershipMode;

    private QueryExecutionBoundaryDefinition(
        List<Capability> ownedCapabilities,
        List<ExcludedCapability> excludedCapabilities,
        List<GovernanceDependency> governanceDependencies,
        List<ExecutionAccessMode> hetuAccessModes,
        ParserBoundaryMode parserBoundaryMode,
        ReadonlyPolicy readonlyPolicy,
        AccelerationOwnershipMode accelerationOwnershipMode
    ) {
        this.ownedCapabilities = ownedCapabilities;
        this.excludedCapabilities = excludedCapabilities;
        this.governanceDependencies = governanceDependencies;
        this.hetuAccessModes = hetuAccessModes;
        this.parserBoundaryMode = parserBoundaryMode;
        this.readonlyPolicy = readonlyPolicy;
        this.accelerationOwnershipMode = accelerationOwnershipMode;
    }

    public static QueryExecutionBoundaryDefinition baseline() {
        return new QueryExecutionBoundaryDefinition(
            Collections.unmodifiableList(
                Arrays.asList(
                    Capability.QUERY_SUBMISSION,
                    Capability.ROUTING_DECISION,
                    Capability.EXECUTION_CONTROL,
                    Capability.RESULT_AGGREGATION,
                    Capability.LIGHTWEIGHT_SQL_PARSING,
                    Capability.LIGHTWEIGHT_SQL_REWRITE,
                    Capability.APPROVED_ACCELERATION_APPLICATION
                )
            ),
            Collections.unmodifiableList(
                Arrays.asList(
                    ExcludedCapability.TENANT_AND_ROLE_ADMINISTRATION,
                    ExcludedCapability.DATASOURCE_POLICY_GOVERNANCE,
                    ExcludedCapability.ASYNC_DEEP_OPTIMIZATION,
                    ExcludedCapability.ACCELERATION_STRATEGY_MANAGEMENT,
                    ExcludedCapability.BENCHMARK_ORCHESTRATION
                )
            ),
            Collections.unmodifiableList(
                Arrays.asList(
                    GovernanceDependency.TENANT_SCOPE_CHECK,
                    GovernanceDependency.DATASOURCE_ACCESS_CHECK,
                    GovernanceDependency.AUDIT_WRITE
                )
            ),
            Collections.unmodifiableList(
                Arrays.asList(
                    ExecutionAccessMode.JDBC,
                    ExecutionAccessMode.REST,
                    ExecutionAccessMode.CLIENT
                )
            ),
            ParserBoundaryMode.OPEN_SOURCE_ABSTRACTION_FOR_LIGHTWEIGHT_PATH,
            ReadonlyPolicy.READONLY_FIRST,
            AccelerationOwnershipMode.APPLY_APPROVED_RUNTIME_CONFIG_ONLY
        );
    }

    public List<Capability> getOwnedCapabilities() {
        return ownedCapabilities;
    }

    public List<ExcludedCapability> getExcludedCapabilities() {
        return excludedCapabilities;
    }

    public List<GovernanceDependency> getGovernanceDependencies() {
        return governanceDependencies;
    }

    public List<ExecutionAccessMode> getHetuAccessModes() {
        return hetuAccessModes;
    }

    public ParserBoundaryMode getParserBoundaryMode() {
        return parserBoundaryMode;
    }

    public ReadonlyPolicy getReadonlyPolicy() {
        return readonlyPolicy;
    }

    public AccelerationOwnershipMode getAccelerationOwnershipMode() {
        return accelerationOwnershipMode;
    }
}
