package com.company.sqloptimization.infrastructure.metadata;

import com.company.sqloptimization.config.OptimizationViewMetadataProperties;
import java.sql.Connection;
import java.sql.DriverManager;
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

    public JdbcDatasourceViewMetadataClient(OptimizationViewMetadataProperties properties) {
        this.properties = properties;
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
        OptimizationViewMetadataProperties.DatasourceMetadataProperties datasource =
            findDatasource(datasourceCode);
        if (datasource == null || !StringUtils.hasText(datasource.getJdbcUrl())) {
            return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_CONFIG_MISSING");
        }
        if (StringUtils.hasText(datasource.getDriverClassName())) {
            try {
                Class.forName(datasource.getDriverClassName());
            } catch (ClassNotFoundException ex) {
                return DatasourceViewMetadataResponse.unresolved("DATASOURCE_VIEW_METADATA_DRIVER_MISSING");
            }
        }
        try (Connection connection = DriverManager.getConnection(datasource.getJdbcUrl(), connectionProperties(datasource))) {
            DatasourceViewMetadataResponse showCreate = tryShowCreateView(connection, request);
            if (showCreate != null) {
                return showCreate;
            }
            return queryInformationSchema(connection, request);
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

    private Properties connectionProperties(OptimizationViewMetadataProperties.DatasourceMetadataProperties datasource) {
        Properties connectionProperties = new Properties();
        if (StringUtils.hasText(datasource.getUsername())) {
            connectionProperties.setProperty("user", datasource.getUsername());
        }
        if (StringUtils.hasText(datasource.getPassword())) {
            connectionProperties.setProperty("password", datasource.getPassword());
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

    private static final class QueryPlan {

        private final String sql;
        private final List<String> parameters;

        private QueryPlan(String sql, List<String> parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }
    }
}
