package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.queryexecution.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.jdbc.ManagedJdbcConnectionFactory;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JdbcHetuExecutionModeAdapter implements HetuExecutionModeAdapter {

    private final QueryExecutionHetuProperties properties;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final ManagedJdbcConnectionFactory connectionFactory = new ManagedJdbcConnectionFactory();

    public JdbcHetuExecutionModeAdapter(QueryExecutionHetuProperties properties) {
        this(properties, null);
    }

    @Autowired
    public JdbcHetuExecutionModeAdapter(QueryExecutionHetuProperties properties,
                                        GovernanceCapabilityClient governanceCapabilityClient) {
        this.properties = properties;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    @Override
    public QueryExecutionAccessMode getMode() {
        return QueryExecutionAccessMode.JDBC;
    }

    @Override
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        QueryExecutionHetuProperties.Jdbc jdbc = properties.getJdbc();
        ResolvedJdbcDatasource datasource = resolveDatasource(request);
        if (datasource == null || !StringUtils.hasText(datasource.getJdbcUrl())) {
            throw new IllegalStateException("Hetu JDBC url 未配置");
        }
        long start = System.currentTimeMillis();
        try (Connection connection = connectionFactory.openConnection(
            datasource.getJdbcUrl(),
            datasource.toConnectionProperties(),
            datasource.getDriverClassName(),
            datasource.getDriverSourceType(),
            StringUtils.hasText(datasource.getDriverRelativePath()) ? Paths.get(datasource.getDriverRelativePath()) : null,
            datasource.getDriverArtifactId(),
            datasource.getDriverSha256()
        );
             PreparedStatement statement = connection.prepareStatement(actualSql)) {
            statement.setQueryTimeout(jdbc.getQueryTimeoutSeconds());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = readRows(resultSet, jdbc.getMaxRows());
                return new QueryExecutionStep(
                    DataSourceTypeEnum.HETU,
                    rows,
                    System.currentTimeMillis() - start,
                    rows.size(),
                    false,
                    shouldApplyAcceleration(request, degradedPath),
                    QueryExecutionAccessMode.JDBC.name(),
                    Collections.singletonList(QueryExecutionAccessMode.JDBC.name())
                );
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Hetu JDBC 驱动加载失败", ex);
        } catch (SQLException ex) {
            throw new IllegalStateException("Hetu JDBC 执行失败", ex);
        }
    }

    private ResolvedJdbcDatasource resolveDatasource(QueryExecuteRequest request) {
        ResolvedJdbcDatasource governanceDatasource = resolveGovernanceDatasource(request);
        if (governanceDatasource != null) {
            return governanceDatasource;
        }
        QueryExecutionHetuProperties.Jdbc jdbc = properties.getJdbc();
        if (!StringUtils.hasText(jdbc.getUrl())) {
            return null;
        }
        return ResolvedJdbcDatasource.local(jdbc);
    }

    private ResolvedJdbcDatasource resolveGovernanceDatasource(QueryExecuteRequest request) {
        if (governanceCapabilityClient == null
            || !properties.getJdbc().isGovernanceResolutionEnabled()
            || request == null
            || !StringUtils.hasText(request.getTenantId())
            || !StringUtils.hasText(request.getDatasourceCode())) {
            return null;
        }
        GovernanceJdbcDatasourceResolveRequest resolveRequest = new GovernanceJdbcDatasourceResolveRequest();
        resolveRequest.setTenantId(request.getTenantId());
        resolveRequest.setDatasourceCode(request.getDatasourceCode());
        resolveRequest.setEngineType(
            request.getDatasourceType() == null ? DataSourceTypeEnum.HETU.name() : request.getDatasourceType().name()
        );
        try {
            GovernanceJdbcDatasourceResolveResponse response =
                governanceCapabilityClient.resolveJdbcDatasource(resolveRequest);
            if (response == null) {
                return null;
            }
            if (response.isResolved() && StringUtils.hasText(response.getJdbcUrl())) {
                return ResolvedJdbcDatasource.governance(response);
            }
            if (StringUtils.hasText(response.getFailureReason())
                && !"HETU_JDBC_CONFIG_NOT_FOUND".equals(response.getFailureReason())) {
                throw new IllegalStateException("治理 JDBC 数据源不可用: " + response.getFailureReason());
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (!StringUtils.hasText(properties.getJdbc().getUrl())) {
                throw new IllegalStateException("治理 JDBC 数据源解析失败", ex);
            }
        }
        return null;
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet, int maxRows) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        while (resultSet.next() && rows.size() < maxRows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            for (int index = 1; index <= columnCount; index++) {
                row.put(metadata.getColumnLabel(index), resultSet.getObject(index));
            }
            rows.add(row);
        }
        return rows;
    }

    private boolean shouldApplyAcceleration(QueryExecuteRequest request, boolean degradedPath) {
        return !degradedPath
            && request != null
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }

    private static final class ResolvedJdbcDatasource {

        private final String jdbcUrl;
        private final String driverClassName;
        private final String driverSourceType;
        private final String driverArtifactId;
        private final String driverSha256;
        private final String driverRelativePath;
        private final String username;
        private final String password;

        private ResolvedJdbcDatasource(String jdbcUrl,
                                       String driverClassName,
                                       String driverSourceType,
                                       String driverArtifactId,
                                       String driverSha256,
                                       String driverRelativePath,
                                       String username,
                                       String password) {
            this.jdbcUrl = jdbcUrl;
            this.driverClassName = driverClassName;
            this.driverSourceType = driverSourceType;
            this.driverArtifactId = driverArtifactId;
            this.driverSha256 = driverSha256;
            this.driverRelativePath = driverRelativePath;
            this.username = username;
            this.password = password;
        }

        private static ResolvedJdbcDatasource local(QueryExecutionHetuProperties.Jdbc jdbc) {
            return new ResolvedJdbcDatasource(
                jdbc.getUrl(),
                null,
                null,
                null,
                null,
                null,
                jdbc.getUsername(),
                jdbc.getPassword()
            );
        }

        private static ResolvedJdbcDatasource governance(GovernanceJdbcDatasourceResolveResponse response) {
            return new ResolvedJdbcDatasource(
                response.getJdbcUrl(),
                response.getDriverClassName(),
                response.getDriverSourceType(),
                response.getDriverArtifactId(),
                response.getDriverSha256(),
                response.getDriverRelativePath(),
                response.getUsername(),
                response.getPassword()
            );
        }

        private Properties toConnectionProperties() {
            Properties connectionProperties = new Properties();
            if (StringUtils.hasText(username)) {
                connectionProperties.setProperty("user", username.trim());
            }
            if (StringUtils.hasText(password)) {
                connectionProperties.setProperty("password", password);
            }
            return connectionProperties;
        }

        private String getJdbcUrl() {
            return jdbcUrl;
        }

        private String getDriverClassName() {
            return driverClassName;
        }

        private String getDriverSourceType() {
            return driverSourceType;
        }

        private String getDriverArtifactId() {
            return driverArtifactId;
        }

        private String getDriverSha256() {
            return driverSha256;
        }

        private String getDriverRelativePath() {
            return driverRelativePath;
        }
    }
}
