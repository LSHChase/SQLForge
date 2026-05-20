package com.company.queryexecution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.domain.boundary.QueryExecutionBoundaryDefinition;
import org.junit.jupiter.api.Test;

class QueryExecutionBoundaryApplicationServiceTest {

    @Test
    void shouldExposeBoundaryAlignedWithCurrentAdrs() {
        QueryExecutionBoundaryApplicationService service = new QueryExecutionBoundaryApplicationService();

        QueryExecutionBoundaryDefinition definition = service.describeBoundary();

        assertEquals(7, definition.getOwnedCapabilities().size());
        assertTrue(
            definition.getOwnedCapabilities().contains(
                QueryExecutionBoundaryDefinition.Capability.ACTIVE_ACCELERATION_APPLICATION
            )
        );
        assertTrue(
            definition.getExcludedCapabilities().contains(
                QueryExecutionBoundaryDefinition.ExcludedCapability.ASYNC_DEEP_OPTIMIZATION
            )
        );
        assertTrue(
            definition.getExcludedCapabilities().contains(
                QueryExecutionBoundaryDefinition.ExcludedCapability.BENCHMARK_ORCHESTRATION
            )
        );
        assertTrue(
            definition.getGovernanceDependencies().contains(
                QueryExecutionBoundaryDefinition.GovernanceDependency.TENANT_SCOPE_CHECK
            )
        );
        assertTrue(
            definition.getGovernanceDependencies().contains(
                QueryExecutionBoundaryDefinition.GovernanceDependency.DATASOURCE_ACCESS_CHECK
            )
        );
        assertTrue(
            definition.getGovernanceDependencies().contains(
                QueryExecutionBoundaryDefinition.GovernanceDependency.AUDIT_WRITE
            )
        );
        assertEquals(
            QueryExecutionBoundaryDefinition.ParserBoundaryMode.OPEN_SOURCE_ABSTRACTION_FOR_LIGHTWEIGHT_PATH,
            definition.getParserBoundaryMode()
        );
        assertEquals(QueryExecutionBoundaryDefinition.ReadonlyPolicy.READONLY_FIRST, definition.getReadonlyPolicy());
        assertEquals(
            QueryExecutionBoundaryDefinition.AccelerationOwnershipMode.APPLY_ACTIVE_RUNTIME_CONFIG_ONLY,
            definition.getAccelerationOwnershipMode()
        );
        assertEquals(3, definition.getHetuAccessModes().size());
        assertTrue(
            definition.getHetuAccessModes().contains(QueryExecutionBoundaryDefinition.ExecutionAccessMode.JDBC)
        );
        assertTrue(
            definition.getHetuAccessModes().contains(QueryExecutionBoundaryDefinition.ExecutionAccessMode.REST)
        );
        assertTrue(
            definition.getHetuAccessModes().contains(QueryExecutionBoundaryDefinition.ExecutionAccessMode.CLIENT)
        );
    }
}
