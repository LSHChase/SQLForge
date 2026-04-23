package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ClientHetuExecutionModeAdapterTest {

    @Test
    void shouldWrapClientOperatorResultWithClientExecutionMetadata() {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.getClient().setEnabled(true);
        ClientHetuExecutionModeAdapter adapter = new ClientHetuExecutionModeAdapter(
            properties,
            new HetuClientOperator() {
                @Override
                public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
                    return new QueryExecutionStep(
                        DataSourceTypeEnum.HETU,
                        Collections.<Map<String, Object>>singletonList(sampleRow()),
                        21L,
                        64L,
                        true,
                        true
                    );
                }
            }
        );

        QueryExecutionStep step = adapter.execute("SELECT 1", new QueryExecuteRequest(), false);

        assertEquals("CLIENT", step.getExecutionMode());
        assertEquals(Collections.singletonList("CLIENT"), step.getAttemptedModes());
        assertEquals(21L, step.getElapsedMs());
        assertEquals(64L, step.getScannedRows());
        assertEquals("HETU", step.getRows().get(0).get("engine"));
        assertEquals("PRIMARY", step.getRows().get(0).get("mode"));
        assertEquals("CLIENT", step.getRows().get(0).get("executionMode"));
    }

    @Test
    void shouldRejectClientModeWhenItIsDisabled() {
        ClientHetuExecutionModeAdapter adapter = new ClientHetuExecutionModeAdapter(
            new QueryExecutionHetuProperties(),
            new NoopHetuClientOperator()
        );

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> adapter.execute("SELECT 1", new QueryExecuteRequest(), false)
        );

        assertEquals("Hetu client mode is disabled", ex.getMessage());
    }

    private static Map<String, Object> sampleRow() {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("value", "ok");
        return row;
    }
}
