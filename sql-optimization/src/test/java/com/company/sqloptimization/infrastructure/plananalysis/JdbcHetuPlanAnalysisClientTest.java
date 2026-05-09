package com.company.sqloptimization.infrastructure.plananalysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqloptimization.config.HetuPlanAnalysisProperties;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.domain.parse.PlanAnalysisStatus;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcHetuPlanAnalysisClientTest {

    private static final FakeHetuDriver FAKE_DRIVER = new FakeHetuDriver();

    @BeforeAll
    static void registerDriver() throws SQLException {
        DriverManager.registerDriver(FAKE_DRIVER);
    }

    @AfterAll
    static void deregisterDriver() throws SQLException {
        DriverManager.deregisterDriver(FAKE_DRIVER);
    }

    @BeforeEach
    void resetDriver() {
        FAKE_DRIVER.reset();
    }

    @Test
    void shouldUseGovernanceResolvedJdbcDatasourceForExplain() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        when(governanceClient.resolveJdbcDatasource(org.mockito.ArgumentMatchers.any()))
            .thenReturn(resolvedGovernanceConfig("jdbc:sqlforgehetu:governance", "hetu_user", "secret", 3500));
        JdbcHetuPlanAnalysisClient client = new JdbcHetuPlanAnalysisClient(disabledLocalProperties(), governanceClient);

        HetuPlanAnalysisResult result = client.explain("SELECT * FROM orders", "tenant-a", "hetu_main");

        assertEquals(PlanAnalysisStatus.SUCCESS, result.getStatus());
        assertTrue(result.getPlanText().contains("Fragment 0 [SINGLE]"));
        assertTrue(result.getEvidence().contains("configSource=GOVERNANCE_INTERNAL"));
        assertTrue(result.getEvidence().contains("timeoutMs=3500"));
        assertEquals("jdbc:sqlforgehetu:governance", FAKE_DRIVER.lastUrl);
        assertEquals("EXPLAIN SELECT * FROM orders", FAKE_DRIVER.lastSql);
        assertEquals("hetu_user", FAKE_DRIVER.lastProperties.getProperty("user"));
        assertEquals("secret", FAKE_DRIVER.lastProperties.getProperty("password"));
        assertEquals(4, FAKE_DRIVER.lastQueryTimeoutSeconds);
        verify(governanceClient).resolveJdbcDatasource(argThat(request ->
            "tenant-a".equals(request.getTenantId())
                && "hetu_main".equals(request.getDatasourceCode())
                && "HETU".equals(request.getEngineType())
        ));
    }

    @Test
    void shouldFallbackToLocalYmlWhenGovernanceConfigIsMissing() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        when(governanceClient.resolveJdbcDatasource(org.mockito.ArgumentMatchers.any()))
            .thenReturn(unresolvedGovernanceConfig("HETU_JDBC_CONFIG_NOT_FOUND"));
        JdbcHetuPlanAnalysisClient client = new JdbcHetuPlanAnalysisClient(localProperties(), governanceClient);

        HetuPlanAnalysisResult result = client.explain("SELECT id FROM orders", "tenant-a", "hetu_main");

        assertEquals(PlanAnalysisStatus.SUCCESS, result.getStatus());
        assertTrue(result.getEvidence().contains("configSource=LOCAL_YML_FALLBACK"));
        assertEquals("jdbc:sqlforgehetu:local", FAKE_DRIVER.lastUrl);
        assertEquals("local_user", FAKE_DRIVER.lastProperties.getProperty("user"));
        assertEquals(2, FAKE_DRIVER.lastQueryTimeoutSeconds);
    }

    @Test
    void shouldNotFallbackWhenGovernanceReturnsConcreteFailure() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        when(governanceClient.resolveJdbcDatasource(org.mockito.ArgumentMatchers.any()))
            .thenReturn(unresolvedGovernanceConfig("HETU_JDBC_DATASOURCE_DISABLED"));
        JdbcHetuPlanAnalysisClient client = new JdbcHetuPlanAnalysisClient(localProperties(), governanceClient);

        HetuPlanAnalysisResult result = client.explain("SELECT id FROM orders", "tenant-a", "hetu_main");

        assertEquals(PlanAnalysisStatus.FAILED, result.getStatus());
        assertEquals("HETU_JDBC_DATASOURCE_DISABLED", result.getFailureReason());
        assertTrue(result.getEvidence().contains("configSource=GOVERNANCE_INTERNAL"));
        assertEquals(null, FAKE_DRIVER.lastUrl);
    }

    @Test
    void shouldReturnConfigNotFoundWhenBothGovernanceAndLocalConfigAreMissing() {
        GovernanceCapabilityClient governanceClient = mock(GovernanceCapabilityClient.class);
        when(governanceClient.resolveJdbcDatasource(org.mockito.ArgumentMatchers.any()))
            .thenReturn(unresolvedGovernanceConfig("HETU_JDBC_CONFIG_NOT_FOUND"));
        JdbcHetuPlanAnalysisClient client = new JdbcHetuPlanAnalysisClient(disabledLocalProperties(), governanceClient);

        HetuPlanAnalysisResult result = client.explain("SELECT id FROM orders", "tenant-a", "missing_ds");

        assertEquals(PlanAnalysisStatus.FAILED, result.getStatus());
        assertEquals("HETU_JDBC_CONFIG_NOT_FOUND", result.getFailureReason());
        assertTrue(result.getEvidence().contains("datasourceCode=missing_ds"));
    }

    private GovernanceJdbcDatasourceResolveResponse resolvedGovernanceConfig(String jdbcUrl,
                                                                             String username,
                                                                             String password,
                                                                             int timeoutMs) {
        GovernanceJdbcDatasourceResolveResponse response = new GovernanceJdbcDatasourceResolveResponse();
        response.setTenantId("tenant-a");
        response.setDatasourceCode("hetu_main");
        response.setEngineType("HETU");
        response.setConnectionMode("JDBC");
        response.setResolved(true);
        response.setEnabled(true);
        response.setReadonly(true);
        response.setJdbcUrl(jdbcUrl);
        response.setUsername(username);
        response.setPassword(password);
        response.setTimeoutMs(Integer.valueOf(timeoutMs));
        return response;
    }

    private GovernanceJdbcDatasourceResolveResponse unresolvedGovernanceConfig(String failureReason) {
        GovernanceJdbcDatasourceResolveResponse response = new GovernanceJdbcDatasourceResolveResponse();
        response.setTenantId("tenant-a");
        response.setDatasourceCode("hetu_main");
        response.setEngineType("HETU");
        response.setConnectionMode("JDBC");
        response.setResolved(false);
        response.setFailureReason(failureReason);
        return response;
    }

    private HetuPlanAnalysisProperties disabledLocalProperties() {
        HetuPlanAnalysisProperties properties = new HetuPlanAnalysisProperties();
        properties.setEnabled(false);
        return properties;
    }

    private HetuPlanAnalysisProperties localProperties() {
        HetuPlanAnalysisProperties properties = new HetuPlanAnalysisProperties();
        properties.setEnabled(true);
        properties.setQueryTimeoutSeconds(2);
        HetuPlanAnalysisProperties.Datasource datasource = new HetuPlanAnalysisProperties.Datasource();
        datasource.setJdbcUrl("jdbc:sqlforgehetu:local");
        datasource.setUsername("local_user");
        datasource.setPassword("local_secret");
        properties.getDatasources().put("HETU_MAIN", datasource);
        return properties;
    }

    private static final class FakeHetuDriver implements Driver {

        private String lastUrl;
        private String lastSql;
        private Properties lastProperties = new Properties();
        private int lastQueryTimeoutSeconds;

        private void reset() {
            lastUrl = null;
            lastSql = null;
            lastProperties = new Properties();
            lastQueryTimeoutSeconds = 0;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) {
                return null;
            }
            lastUrl = url;
            lastProperties = info == null ? new Properties() : info;
            return proxy(Connection.class, (proxy, method, args) -> {
                if ("createStatement".equals(method.getName())) {
                    return statementProxy();
                }
                return defaultValue(method.getReturnType());
            });
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:sqlforgehetu:");
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
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("not supported");
        }

        private Statement statementProxy() {
            return proxy(Statement.class, (proxy, method, args) -> {
                if ("setQueryTimeout".equals(method.getName())) {
                    lastQueryTimeoutSeconds = ((Integer) args[0]).intValue();
                    return null;
                }
                if ("executeQuery".equals(method.getName())) {
                    lastSql = (String) args[0];
                    return resultSetProxy();
                }
                return defaultValue(method.getReturnType());
            });
        }

        private ResultSet resultSetProxy() {
            int[] cursor = new int[] {0};
            return proxy(ResultSet.class, (proxy, method, args) -> {
                if ("next".equals(method.getName())) {
                    cursor[0] += 1;
                    return Boolean.valueOf(cursor[0] == 1);
                }
                if ("getMetaData".equals(method.getName())) {
                    return resultSetMetaDataProxy();
                }
                if ("getString".equals(method.getName())) {
                    return "Fragment 0 [SINGLE]";
                }
                return defaultValue(method.getReturnType());
            });
        }

        private ResultSetMetaData resultSetMetaDataProxy() {
            return proxy(ResultSetMetaData.class, (proxy, method, args) -> {
                if ("getColumnCount".equals(method.getName())) {
                    return Integer.valueOf(1);
                }
                return defaultValue(method.getReturnType());
            });
        }

        @SuppressWarnings("unchecked")
        private <T> T proxy(Class<T> type, InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == Void.TYPE) {
                return null;
            }
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
            return null;
        }
    }
}
