package com.company.governance.infrastructure.datasource;

import com.company.governance.application.service.DatasourceJdbcConnectionProbe;
import com.company.governance.application.service.JdbcDriverArtifactApplicationService;
import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.JdbcDriverArtifact;
import com.company.sqlforge.common.jdbc.ManagedJdbcConnectionFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultDatasourceJdbcConnectionProbe implements DatasourceJdbcConnectionProbe {

    private static final int MAX_REASON_LENGTH = 160;
    private final ManagedJdbcConnectionFactory connectionFactory = new ManagedJdbcConnectionFactory();
    private final JdbcDriverArtifactApplicationService jdbcDriverArtifactApplicationService;

    public DefaultDatasourceJdbcConnectionProbe(JdbcDriverArtifactApplicationService jdbcDriverArtifactApplicationService) {
        this.jdbcDriverArtifactApplicationService = jdbcDriverArtifactApplicationService;
    }

    @Override
    public JdbcProbeResult probe(DatasourceConfig config, String password, int timeoutMs) {
        long start = System.nanoTime();
        if (config == null || !StringUtils.hasText(config.getJdbcUrl())) {
            return failed("JDBC_URL_MISSING", start);
        }
        try {
            JdbcDriverArtifact artifact = resolveArtifact(config);
            java.sql.DriverManager.setLoginTimeout(Math.max(1, timeoutMs / 1000));
            try (Connection connection = connectionFactory.openConnection(
                config.getJdbcUrl(),
                connectionProperties(config, password),
                config.getJdbcDriverClassName(),
                config.getDriverSourceType(),
                artifact == null ? null : jdbcDriverArtifactApplicationService.resolveArtifactPath(artifact),
                artifact == null ? null : artifact.getArtifactId(),
                artifact == null ? null : artifact.getSha256()
            )) {
                try {
                    connection.setReadOnly(true);
                } catch (SQLException ignored) {
                    // 部分驱动不支持连接建立后切换只读模式。
                }
                if (!connection.isValid(Math.max(1, timeoutMs / 1000))) {
                    return failed("JDBC_CONNECTION_INVALID", start);
                }
                return new JdbcProbeResult(true, null, elapsedMs(start));
            }
        } catch (ClassNotFoundException ex) {
            return failed("JDBC_DRIVER_CLASS_NOT_FOUND: " + compact(ex.getMessage()), start);
        } catch (SQLException ex) {
            return failed("JDBC_CONNECT_FAILED: " + compact(ex.getMessage()), start);
        } catch (RuntimeException ex) {
            return failed("JDBC_CONNECT_FAILED: " + compact(ex.getMessage()), start);
        }
    }

    private JdbcDriverArtifact resolveArtifact(DatasourceConfig config) {
        if (config == null || !"UPLOADED".equalsIgnoreCase(config.getDriverSourceType())
            || !StringUtils.hasText(config.getDriverArtifactId())) {
            return null;
        }
        return jdbcDriverArtifactApplicationService.requireArtifact(config.getTenantId(), config.getDriverArtifactId());
    }

    private Properties connectionProperties(DatasourceConfig config, String password) {
        Properties properties = new Properties();
        if (StringUtils.hasText(config.getUsername())) {
            properties.setProperty("user", config.getUsername().trim());
        }
        if (StringUtils.hasText(password)) {
            properties.setProperty("password", password);
        }
        return properties;
    }

    private JdbcProbeResult failed(String reason, long start) {
        return new JdbcProbeResult(false, reason, elapsedMs(start));
    }

    private long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private String compact(String value) {
        String normalized = value == null ? "unknown" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= MAX_REASON_LENGTH ? normalized : normalized.substring(0, MAX_REASON_LENGTH);
    }
}
