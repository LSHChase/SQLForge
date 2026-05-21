package com.company.sqloptimization.infrastructure.metadata;

import com.company.sqloptimization.config.OptimizationViewMetadataProperties;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteCandidate;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcRouteResolveResponse;
import com.company.sqlforge.common.jdbc.ManagedJdbcConnectionFactory;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JdbcDatasourceViewMetadataClient implements DatasourceViewMetadataClient {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*");
    private static final Pattern VIEW_BODY_PATTERN = Pattern.compile("(?is)\\bAS\\s+((SELECT|WITH)\\b.*)$");

    private final OptimizationViewMetadataProperties properties;
    private final GovernanceCapabilityClient governanceCapabilityClient;
    private final ManagedJdbcConnectionFactory connectionFactory = new ManagedJdbcConnectionFactory();

    public JdbcDatasourceViewMetadataClient(OptimizationViewMetadataProperties properties,
                                            GovernanceCapabilityClient governanceCapabilityClient) {
        this.properties = properties;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    @Override
    public DatasourceViewMetadataResponse resolveView(DatasourceViewMetadataRequest request) {
        if (request == null) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_REQUEST_MISSING");
        }
        if (!properties.isEnabled()) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_DISABLED");
        }
        String datasourceCode = trimToNull(request.getDatasourceCode());
        if (datasourceCode == null) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_CODE_MISSING");
        }
        ResolvedDatasource datasource = resolveDatasource(request);
        if (datasource == null || !StringUtils.hasText(datasource.jdbcUrl)) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_CONFIG_MISSING");
        }
        try (Connection connection = connectionFactory.openConnection(
            datasource.jdbcUrl,
            connectionProperties(datasource),
            datasource.driverClassName,
            datasource.driverSourceType,
            datasource.driverJarPath,
            datasource.driverArtifactId,
            datasource.driverSha256
        )) {
            DatasourceViewMetadataResponse showCreate = tryShowCreateView(connection, request);
            if (showCreate != null) {
                return showCreate;
            }
            return queryInformationSchema(connection, request);
        } catch (ClassNotFoundException ex) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_DRIVER_MISSING");
        } catch (SQLException ex) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_QUERY_FAILED");
        }
    }

    private DatasourceViewMetadataResponse tryShowCreateView(Connection connection,
                                                             DatasourceViewMetadataRequest request) {
        String qualifiedName = safeQualifiedName(request);
        if (qualifiedName == null) {
            return null;
        }
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(Math.max(1, properties.getQueryTimeoutSeconds()));
            try (ResultSet resultSet = statement.executeQuery("SHOW CREATE VIEW " + qualifiedName)) {
                if (!resultSet.next()) {
                    return null;
                }
                String definitionSql = firstTextColumn(resultSet);
                if (!StringUtils.hasText(definitionSql)) {
                    return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_DEFINITION_EMPTY");
                }
                return DatasourceViewMetadataResponse.view(extractViewBody(definitionSql));
            }
        } catch (SQLException ex) {
            return null;
        }
    }

    private DatasourceViewMetadataResponse queryInformationSchema(Connection connection,
                                                                  DatasourceViewMetadataRequest request)
        throws SQLException {
        QueryPlan queryPlan = informationSchemaQuery(request);
        try (PreparedStatement statement = connection.prepareStatement(queryPlan.sql)) {
            statement.setQueryTimeout(Math.max(1, properties.getQueryTimeoutSeconds()));
            for (int index = 0; index < queryPlan.parameters.size(); index++) {
                statement.setString(index + 1, queryPlan.parameters.get(index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return DatasourceViewMetadataResponse.table();
                }
                String definitionSql = resultSet.getString(1);
                if (!StringUtils.hasText(definitionSql)) {
                    return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_DEFINITION_EMPTY");
                }
                return DatasourceViewMetadataResponse.view(extractViewBody(definitionSql));
            }
        }
    }

    private QueryPlan informationSchemaQuery(DatasourceViewMetadataRequest request) {
        StringBuilder sql = new StringBuilder();
        List<String> parameters = new ArrayList<String>();
        sql.append("SELECT view_definition FROM information_schema.views WHERE LOWER(table_name) = LOWER(?)");
        parameters.add(trimToNull(request.getObjectName()));
        if (StringUtils.hasText(request.getSchemaName())) {
            sql.append(" AND LOWER(table_schema) = LOWER(?)");
            parameters.add(trimToNull(request.getSchemaName()));
        }
        if (StringUtils.hasText(request.getCatalogName())) {
            sql.append(" AND LOWER(table_catalog) = LOWER(?)");
            parameters.add(trimToNull(request.getCatalogName()));
        }
        return new QueryPlan(sql.toString(), parameters);
    }

    private String firstTextColumn(ResultSet resultSet) throws SQLException {
        int columnCount = resultSet.getMetaData().getColumnCount();
        for (int index = 1; index <= columnCount; index++) {
            String value = resultSet.getString(index);
            if (StringUtils.hasText(value)
                && (startsWithSelect(value) || value.toUpperCase(Locale.ROOT).contains(" CREATE "))) {
                return value;
            }
        }
        for (int index = 1; index <= columnCount; index++) {
            String value = resultSet.getString(index);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String extractViewBody(String definitionSql) {
        String normalized = trimToNull(definitionSql);
        if (normalized == null) {
            return null;
        }
        if (startsWithSelect(normalized)) {
            return normalized;
        }
        Matcher matcher = VIEW_BODY_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return normalized;
    }

    private boolean startsWithSelect(String sql) {
        String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("SELECT") || upper.startsWith("WITH");
    }

    private String safeQualifiedName(DatasourceViewMetadataRequest request) {
        List<String> parts = new ArrayList<String>();
        if (!addSafeIdentifier(parts, request.getCatalogName())
            || !addSafeIdentifier(parts, request.getSchemaName())
            || !addSafeIdentifier(parts, request.getObjectName())) {
            return null;
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(".", parts);
    }

    private boolean addSafeIdentifier(List<String> parts, String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return true;
        }
        if (!SAFE_IDENTIFIER.matcher(normalized).matches()) {
            return false;
        }
        if (!parts.isEmpty() || StringUtils.hasText(normalized)) {
            parts.add(normalized);
        }
        return true;
    }

    private Properties connectionProperties(ResolvedDatasource datasource) {
        Properties connectionProperties = new Properties();
        if (StringUtils.hasText(datasource.username)) {
            connectionProperties.setProperty("user", datasource.username);
        }
        if (StringUtils.hasText(datasource.password)) {
            connectionProperties.setProperty("password", datasource.password);
        }
        return connectionProperties;
    }

    private OptimizationViewMetadataProperties.DatasourceMetadataProperties findDatasource(String datasourceCode) {
        OptimizationViewMetadataProperties.DatasourceMetadataProperties direct =
            properties.getDatasources().get(datasourceCode);
        if (direct != null) {
            return direct;
        }
        return properties.getDatasources().get(datasourceCode.toLowerCase(Locale.ROOT));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ResolvedDatasource resolveDatasource(DatasourceViewMetadataRequest request) {
        ResolvedDatasource governanceDatasource = resolveGovernanceDatasource(request);
        if (governanceDatasource != null) {
            return governanceDatasource;
        }
        OptimizationViewMetadataProperties.DatasourceMetadataProperties localDatasource =
            findDatasource(trimToNull(request.getDatasourceCode()));
        if (localDatasource == null) {
            return null;
        }
        return ResolvedDatasource.local(localDatasource);
    }

    private ResolvedDatasource resolveGovernanceDatasource(DatasourceViewMetadataRequest request) {
        if (governanceCapabilityClient == null || !StringUtils.hasText(request.getTenantId())) {
            return null;
        }
        GovernanceJdbcRouteResolveRequest routeRequest = new GovernanceJdbcRouteResolveRequest();
        routeRequest.setTenantId(request.getTenantId());
        routeRequest.setDatasourceCode(request.getDatasourceCode());
        routeRequest.setDatasourceType(request.getDatasourceType() == null ? DataSourceTypeEnum.AUTO : request.getDatasourceType());
        try {
            GovernanceJdbcRouteResolveResponse response = governanceCapabilityClient.resolveJdbcRoute(routeRequest);
            if (response == null || response.getCandidates() == null) {
                return null;
            }
            for (GovernanceJdbcRouteCandidate candidate : response.getCandidates()) {
                if (candidate != null && candidate.isEnabled() && StringUtils.hasText(candidate.getEngineType())) {
                    GovernanceJdbcDatasourceResolveRequest resolveRequest = new GovernanceJdbcDatasourceResolveRequest();
                    resolveRequest.setTenantId(request.getTenantId());
                    resolveRequest.setDatasourceCode(request.getDatasourceCode());
                    resolveRequest.setEngineType(candidate.getEngineType());
                    GovernanceJdbcDatasourceResolveResponse datasourceResponse =
                        governanceCapabilityClient.resolveJdbcDatasource(resolveRequest);
                    if (datasourceResponse != null && datasourceResponse.isResolved()
                        && StringUtils.hasText(datasourceResponse.getJdbcUrl())) {
                        return ResolvedDatasource.governance(datasourceResponse);
                    }
                }
            }
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static final class QueryPlan {

        private final String sql;
        private final List<String> parameters;

        private QueryPlan(String sql, List<String> parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }
    }

    private static final class ResolvedDatasource {

        private final String jdbcUrl;
        private final String driverClassName;
        private final String username;
        private final String password;
        private final String driverSourceType;
        private final String driverArtifactId;
        private final String driverSha256;
        private final java.nio.file.Path driverJarPath;

        private ResolvedDatasource(String jdbcUrl,
                                   String driverClassName,
                                   String username,
                                   String password,
                                   String driverSourceType,
                                   String driverArtifactId,
                                   String driverSha256,
                                   java.nio.file.Path driverJarPath) {
            this.jdbcUrl = jdbcUrl;
            this.driverClassName = driverClassName;
            this.username = username;
            this.password = password;
            this.driverSourceType = driverSourceType;
            this.driverArtifactId = driverArtifactId;
            this.driverSha256 = driverSha256;
            this.driverJarPath = driverJarPath;
        }

        private static ResolvedDatasource governance(GovernanceJdbcDatasourceResolveResponse response) {
            return new ResolvedDatasource(
                response.getJdbcUrl(),
                response.getDriverClassName(),
                response.getUsername(),
                response.getPassword(),
                response.getDriverSourceType(),
                response.getDriverArtifactId(),
                response.getDriverSha256(),
                StringUtils.hasText(response.getDriverRelativePath()) ? Paths.get(response.getDriverRelativePath()) : null
            );
        }

        private static ResolvedDatasource local(OptimizationViewMetadataProperties.DatasourceMetadataProperties datasource) {
            return new ResolvedDatasource(
                datasource.getJdbcUrl(),
                datasource.getDriverClassName(),
                datasource.getUsername(),
                datasource.getPassword(),
                "CLASSPATH",
                null,
                null,
                null
            );
        }
    }
}
