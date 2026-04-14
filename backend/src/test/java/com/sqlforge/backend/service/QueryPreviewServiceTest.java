package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.model.QueryPreviewResult;
import com.sqlforge.backend.web.dto.QueryPreviewRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPreviewServiceTest {

    @Test
    void shouldReportDriverMissingWhenDriverIsUnavailable() {
        QueryPreviewService service = new QueryPreviewService(
            new JdbcConnectionSupportService(new EngineCatalogService())
        );

        QueryPreviewResult result = service.preview(request("trino", "select 1", 10));

        assertEquals("driver-missing", result.getStatus());
        assertEquals("missing", result.getDriverStatus());
        assertTrue(result.getRows().isEmpty());
    }

    @Test
    void shouldReturnPreviewRowsWhenJdbcQuerySucceeds() {
        QueryPreviewService service = new PreviewServiceForTest(new FakeEngineCatalogService(), false);

        QueryPreviewResult result = service.preview(request("fake-engine", "select * from demo", 10));

        assertEquals("result-set", result.getStatus());
        assertEquals("loaded", result.getDriverStatus());
        assertEquals(Arrays.asList("id", "name"), result.getColumns());
        assertEquals(2, result.getRowCount());
        assertEquals("alpha", result.getRows().get(0).get("name"));
    }

    @Test
    void shouldReturnFailureWhenJdbcExecutionFails() {
        QueryPreviewService service = new PreviewServiceForTest(new FakeEngineCatalogService(), true);

        QueryPreviewResult result = service.preview(request("fake-engine", "select * from demo", 10));

        assertEquals("jdbc-query-failed", result.getStatus());
        assertEquals("loaded", result.getDriverStatus());
        assertFalse(result.getMessages().isEmpty());
    }

    private QueryPreviewRequest request(String engineCode, String sql, int maxRows) {
        QueryPreviewRequest request = new QueryPreviewRequest();
        request.setName("Preview");
        request.setEngineCode(engineCode);
        request.setHost("127.0.0.1");
        request.setPort(1111);
        request.setCatalog("catalog");
        request.setUsername("analyst");
        request.setPassword("changeit");
        request.setSslEnabled(true);
        request.setSql(sql);
        request.setMaxRows(maxRows);
        return request;
    }

    static class PreviewServiceForTest extends QueryPreviewService {

        private final boolean fail;

        PreviewServiceForTest(EngineCatalogService engineCatalogService, boolean fail) {
            super(new JdbcConnectionSupportService(engineCatalogService));
            this.fail = fail;
        }

        @Override
        protected QueryPreviewResult executePreview(
            String jdbcUrl,
            QueryPreviewRequest request,
            int timeoutMs,
            long startedAt
        ) throws SQLException {
            if (fail) {
                throw new SQLException("simulated query execution failure");
            }

            Connection connection = fakeConnection();
            Statement statement = connection.createStatement();
            statement.setMaxRows(Math.max(1, Math.min(request.getMaxRows(), 100)));
            boolean hasResultSet = statement.execute(request.getSql());

            if (!hasResultSet) {
                throw new SQLException("expected a fake result set");
            }

            ResultSet resultSet = statement.getResultSet();
            ResultSetMetaData metaData = resultSet.getMetaData();

            java.util.List<String> columns = new java.util.ArrayList<String>();
            java.util.List<java.util.Map<String, Object>> rows =
                new java.util.ArrayList<java.util.Map<String, Object>>();

            for (int index = 1; index <= metaData.getColumnCount(); index += 1) {
                columns.add(metaData.getColumnLabel(index));
            }

            while (resultSet.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<String, Object>();

                for (int index = 1; index <= metaData.getColumnCount(); index += 1) {
                    row.put(columns.get(index - 1), resultSet.getObject(index));
                }

                rows.add(row);
            }

            return new QueryPreviewResult(
                "result-set",
                "loaded",
                jdbcUrl,
                rows.size(),
                columns,
                rows,
                1L,
                java.util.Arrays.asList("query preview succeeded", "rows limited to " + request.getMaxRows())
            );
        }

        private Connection fakeConnection() {
            InvocationHandler connectionHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("createStatement".equals(method.getName())) {
                        return fakeStatement();
                    }

                    if ("close".equals(method.getName())) {
                        return null;
                    }

                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }

                    return defaultValue(method.getReturnType());
                }
            };

            return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                connectionHandler
            );
        }

        private Statement fakeStatement() {
            InvocationHandler statementHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("execute".equals(method.getName())) {
                        return true;
                    }

                    if ("getResultSet".equals(method.getName())) {
                        return fakeResultSet();
                    }

                    if ("setMaxRows".equals(method.getName()) || "close".equals(method.getName())) {
                        return null;
                    }

                    if ("getUpdateCount".equals(method.getName())) {
                        return 0;
                    }

                    return defaultValue(method.getReturnType());
                }
            };

            return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[] { Statement.class },
                statementHandler
            );
        }

        private ResultSet fakeResultSet() {
            java.util.List<Map<String, Object>> data = new java.util.ArrayList<Map<String, Object>>();
            Map<String, Object> row1 = new LinkedHashMap<String, Object>();
            row1.put("id", 1);
            row1.put("name", "alpha");
            data.add(row1);

            Map<String, Object> row2 = new LinkedHashMap<String, Object>();
            row2.put("id", 2);
            row2.put("name", "beta");
            data.add(row2);

            InvocationHandler resultSetHandler = new InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("next".equals(method.getName())) {
                        index += 1;
                        return index < data.size();
                    }

                    if ("getObject".equals(method.getName())) {
                        int columnIndex = ((Integer) args[0]).intValue();
                        return data.get(index).values().toArray()[columnIndex - 1];
                    }

                    if ("getMetaData".equals(method.getName())) {
                        return fakeMetaData();
                    }

                    if ("close".equals(method.getName())) {
                        return null;
                    }

                    return defaultValue(method.getReturnType());
                }
            };

            return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] { ResultSet.class },
                resultSetHandler
            );
        }

        private ResultSetMetaData fakeMetaData() {
            InvocationHandler metadataHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("getColumnCount".equals(method.getName())) {
                        return 2;
                    }

                    if ("getColumnLabel".equals(method.getName())) {
                        int columnIndex = ((Integer) args[0]).intValue();
                        return columnIndex == 1 ? "id" : "name";
                    }

                    return defaultValue(method.getReturnType());
                }
            };

            return (ResultSetMetaData) Proxy.newProxyInstance(
                ResultSetMetaData.class.getClassLoader(),
                new Class<?>[] { ResultSetMetaData.class },
                metadataHandler
            );
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType.equals(boolean.class)) {
                return false;
            }

            if (returnType.equals(int.class)) {
                return 0;
            }

            if (returnType.equals(long.class)) {
                return 0L;
            }

            return null;
        }
    }

    static class FakeEngineCatalogService extends EngineCatalogService {

        @Override
        public EngineDescriptor findByCode(String code) {
            if ("fake-engine".equals(code)) {
                return new EngineDescriptor(
                    "fake-engine",
                    "Fake Engine",
                    "test",
                    1111,
                    "tcp",
                    "fake",
                    QueryPreviewServiceTest.class.getName(),
                    "fake profile for query preview tests",
                    true
                );
            }

            return super.findByCode(code);
        }
    }
}
