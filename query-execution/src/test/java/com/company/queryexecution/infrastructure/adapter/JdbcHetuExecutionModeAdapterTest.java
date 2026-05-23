package com.company.queryexecution.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.queryexecution.infrastructure.governance.QueryExecutionAuditRecord;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteRequest;
import com.company.sqlforge.common.governance.GovernanceQueryExecutionHistoryWriteResponse;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class JdbcHetuExecutionModeAdapterTest {

    @Test
    void shouldExecuteJdbcModeAndLimitRows() throws Exception {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.getJdbc().setUrl("jdbc:test-hetu://unit");
        properties.getJdbc().setQueryTimeoutSeconds(1);
        properties.getJdbc().setMaxRows(1);
        AtomicInteger configuredTimeout = new AtomicInteger(-1);
        AtomicReference<Properties> connectionProperties = new AtomicReference<Properties>();
        Driver driver = new TestDriver(
            properties.getJdbc().getUrl(),
            Arrays.<Map<String, Object>>asList(
                row(7, "READY"),
                row(8, "DONE")
            ),
            Arrays.asList("order_id", "state"),
            configuredTimeout,
            connectionProperties
        );
        DriverManager.registerDriver(driver);
        try {
            JdbcHetuExecutionModeAdapter adapter = new JdbcHetuExecutionModeAdapter(properties);

            QueryExecutionStep step = adapter.execute("SELECT order_id, state FROM orders", acceleratedRequest(), false);

            assertEquals("JDBC", step.getExecutionMode());
            assertEquals(Collections.singletonList("JDBC"), step.getAttemptedModes());
            assertTrue(step.isAccelerationApplied());
            assertEquals(1, step.getRows().size());
            assertEquals("HETU", step.getRows().get(0).get("engine"));
            assertEquals("PRIMARY", step.getRows().get(0).get("mode"));
            assertEquals("JDBC", step.getRows().get(0).get("executionMode"));
            assertEquals(7, ((Number) step.getRows().get(0).get("order_id")).intValue());
            assertEquals("READY", step.getRows().get(0).get("state"));
            assertEquals(1, configuredTimeout.get());
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @Test
    void shouldUseGovernanceResolvedJdbcDatasourceWhenDatasourceCodeIsPresent() throws Exception {
        QueryExecutionHetuProperties properties = new QueryExecutionHetuProperties();
        properties.getJdbc().setQueryTimeoutSeconds(2);
        properties.getJdbc().setMaxRows(2);
        AtomicInteger configuredTimeout = new AtomicInteger(-1);
        AtomicReference<Properties> connectionProperties = new AtomicReference<Properties>();
        AtomicReference<GovernanceJdbcDatasourceResolveRequest> capturedRequest =
            new AtomicReference<GovernanceJdbcDatasourceResolveRequest>();
        Driver driver = new TestDriver(
            "jdbc:test-hetu://governance",
            Collections.singletonList(row(9, "GOVERNED")),
            Arrays.asList("order_id", "state"),
            configuredTimeout,
            connectionProperties
        );
        DriverManager.registerDriver(driver);
        try {
            JdbcHetuExecutionModeAdapter adapter = new JdbcHetuExecutionModeAdapter(
                properties,
                governanceClient(capturedRequest, resolvedDatasource())
            );
            QueryExecuteRequest request = acceleratedRequest();
            request.setDatasourceCode("hetu_main");

            QueryExecutionStep step = adapter.execute("SELECT order_id, state FROM orders", request, false);

            assertEquals("tenant-a", capturedRequest.get().getTenantId());
            assertEquals("hetu_main", capturedRequest.get().getDatasourceCode());
            assertEquals("HETU", capturedRequest.get().getEngineType());
            assertEquals(1, step.getRows().size());
            assertEquals(9, ((Number) step.getRows().get(0).get("order_id")).intValue());
            assertEquals("GOVERNED", step.getRows().get(0).get("state"));
            assertEquals(2, configuredTimeout.get());
            assertEquals("governance-user", connectionProperties.get().getProperty("user"));
            assertEquals("governance-secret", connectionProperties.get().getProperty("password"));
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @Test
    void shouldRejectJdbcModeWithoutConfiguredUrl() {
        JdbcHetuExecutionModeAdapter adapter = new JdbcHetuExecutionModeAdapter(new QueryExecutionHetuProperties());

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> adapter.execute("SELECT 1", acceleratedRequest(), false)
        );

        assertEquals("Hetu JDBC url 未配置", ex.getMessage());
    }

    private QueryExecuteRequest acceleratedRequest() {
        QueryExecuteRequest request = new QueryExecuteRequest();
        request.setTenantId("tenant-a");
        request.setDatasourceType(DataSourceTypeEnum.HETU);
        request.setAccelerationPreference(AccelerationPreference.PREFER_ACCELERATED);
        return request;
    }

    private GovernanceJdbcDatasourceResolveResponse resolvedDatasource() {
        GovernanceJdbcDatasourceResolveResponse response = new GovernanceJdbcDatasourceResolveResponse();
        response.setTenantId("tenant-a");
        response.setDatasourceCode("hetu_main");
        response.setEngineType("HETU");
        response.setResolved(true);
        response.setEnabled(true);
        response.setConnectionMode("JDBC");
        response.setJdbcUrl("jdbc:test-hetu://governance");
        response.setUsername("governance-user");
        response.setPassword("governance-secret");
        response.setDriverSourceType("CLASSPATH");
        return response;
    }

    private GovernanceCapabilityClient governanceClient(
        AtomicReference<GovernanceJdbcDatasourceResolveRequest> capturedRequest,
        GovernanceJdbcDatasourceResolveResponse response
    ) {
        return new GovernanceCapabilityClient() {
            @Override
            public void assertDatasourceAccess(String tenantId,
                                               DataSourceTypeEnum datasourceType,
                                               String resourceType,
                                               String resourceId,
                                               String operationCode) {
            }

            @Override
            public void writeAudit(QueryExecutionAuditRecord auditRecord) {
            }

            @Override
            public GovernanceJdbcRouteResolveResponse resolveJdbcRoute(GovernanceJdbcRouteResolveRequest request) {
                return new GovernanceJdbcRouteResolveResponse();
            }

            @Override
            public GovernanceJdbcDatasourceResolveResponse resolveJdbcDatasource(
                GovernanceJdbcDatasourceResolveRequest request
            ) {
                capturedRequest.set(request);
                return response;
            }

            @Override
            public GovernanceQueryExecutionHistoryWriteResponse writeQueryExecutionHistory(
                GovernanceQueryExecutionHistoryWriteRequest request
            ) {
                return new GovernanceQueryExecutionHistoryWriteResponse();
            }
        };
    }

    private static Map<String, Object> row(int orderId, String state) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("order_id", Integer.valueOf(orderId));
        row.put("state", state);
        return row;
    }

    private static final class TestDriver implements Driver {

        private final String acceptedUrl;
        private final List<Map<String, Object>> rows;
        private final List<String> labels;
        private final AtomicInteger configuredTimeout;
        private final AtomicReference<Properties> connectionProperties;

        private TestDriver(String acceptedUrl,
                           List<Map<String, Object>> rows,
                           List<String> labels,
                           AtomicInteger configuredTimeout,
                           AtomicReference<Properties> connectionProperties) {
            this.acceptedUrl = acceptedUrl;
            this.rows = new ArrayList<Map<String, Object>>(rows);
            this.labels = new ArrayList<String>(labels);
            this.configuredTimeout = configuredTimeout;
            this.connectionProperties = connectionProperties;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            connectionProperties.set(info);
            return connectionProxy(rows, labels, configuredTimeout);
        }

        @Override
        public boolean acceptsURL(String url) {
            return acceptedUrl.equals(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }
    }

    private static Connection connectionProxy(List<Map<String, Object>> rows,
                                              List<String> labels,
                                              AtomicInteger configuredTimeout) {
        return (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[]{Connection.class},
            new DefaultingInvocationHandler() {
                @Override
                protected Object invokeKnownMethod(Object proxy, Method method, Object[] args) {
                    if ("prepareStatement".equals(method.getName())) {
                        return preparedStatementProxy(rows, labels, configuredTimeout);
                    }
                    return unsupported(method);
                }
            }
        );
    }

    private static PreparedStatement preparedStatementProxy(List<Map<String, Object>> rows,
                                                            List<String> labels,
                                                            AtomicInteger configuredTimeout) {
        return (PreparedStatement) Proxy.newProxyInstance(
            PreparedStatement.class.getClassLoader(),
            new Class<?>[]{PreparedStatement.class},
            new DefaultingInvocationHandler() {
                @Override
                protected Object invokeKnownMethod(Object proxy, Method method, Object[] args) {
                    if ("setQueryTimeout".equals(method.getName())) {
                        configuredTimeout.set(((Integer) args[0]).intValue());
                        return null;
                    }
                    if ("executeQuery".equals(method.getName())) {
                        return resultSetProxy(rows, labels);
                    }
                    return unsupported(method);
                }
            }
        );
    }

    private static ResultSet resultSetProxy(List<Map<String, Object>> rows, List<String> labels) {
        return (ResultSet) Proxy.newProxyInstance(
            ResultSet.class.getClassLoader(),
            new Class<?>[]{ResultSet.class},
            new DefaultingInvocationHandler() {
                private int index = -1;

                @Override
                protected Object invokeKnownMethod(Object proxy, Method method, Object[] args) {
                    if ("next".equals(method.getName())) {
                        index++;
                        return Boolean.valueOf(index < rows.size());
                    }
                    if ("getMetaData".equals(method.getName())) {
                        return resultSetMetaDataProxy(labels);
                    }
                    if ("getObject".equals(method.getName())) {
                        return rows.get(index).get(labels.get(((Integer) args[0]).intValue() - 1));
                    }
                    return unsupported(method);
                }
            }
        );
    }

    private static ResultSetMetaData resultSetMetaDataProxy(List<String> labels) {
        return (ResultSetMetaData) Proxy.newProxyInstance(
            ResultSetMetaData.class.getClassLoader(),
            new Class<?>[]{ResultSetMetaData.class},
            new DefaultingInvocationHandler() {
                @Override
                protected Object invokeKnownMethod(Object proxy, Method method, Object[] args) {
                    if ("getColumnCount".equals(method.getName())) {
                        return Integer.valueOf(labels.size());
                    }
                    if ("getColumnLabel".equals(method.getName())) {
                        return labels.get(((Integer) args[0]).intValue() - 1);
                    }
                    return unsupported(method);
                }
            }
        );
    }

    private abstract static class DefaultingInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("close".equals(name)) {
                return null;
            }
            if ("isClosed".equals(name)) {
                return Boolean.FALSE;
            }
            if ("unwrap".equals(name)) {
                return proxy;
            }
            if ("isWrapperFor".equals(name)) {
                return Boolean.FALSE;
            }
            if ("toString".equals(name)) {
                return getClass().getSimpleName();
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == args[0]);
            }
            Object knownResult = invokeKnownMethod(proxy, method, args);
            if (knownResult != null || method.getReturnType() == Void.TYPE) {
                return knownResult;
            }
            return defaultValue(method.getReturnType());
        }

        protected abstract Object invokeKnownMethod(Object proxy, Method method, Object[] args);

        protected Object unsupported(Method method) {
            return defaultValue(method.getReturnType());
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == Boolean.TYPE) {
                return Boolean.FALSE;
            }
            if (returnType == Integer.TYPE) {
                return Integer.valueOf(0);
            }
            if (returnType == Long.TYPE) {
                return Long.valueOf(0L);
            }
            if (returnType == Double.TYPE) {
                return Double.valueOf(0D);
            }
            if (returnType == Float.TYPE) {
                return Float.valueOf(0F);
            }
            if (returnType == Short.TYPE) {
                return Short.valueOf((short) 0);
            }
            if (returnType == Byte.TYPE) {
                return Byte.valueOf((byte) 0);
            }
            if (returnType == Character.TYPE) {
                return Character.valueOf((char) 0);
            }
            return null;
        }
    }
}
