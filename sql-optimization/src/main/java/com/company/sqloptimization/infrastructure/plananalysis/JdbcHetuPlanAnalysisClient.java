package com.company.sqloptimization.infrastructure.plananalysis;

import com.company.sqloptimization.config.HetuPlanAnalysisProperties;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JdbcHetuPlanAnalysisClient implements HetuPlanAnalysisClient {

    private static final int MAX_PLAN_CHARS = 20000;

    private final HetuPlanAnalysisProperties properties;

    public JdbcHetuPlanAnalysisClient(HetuPlanAnalysisProperties properties) {
        this.properties = properties;
    }

    @Override
    public HetuPlanAnalysisResult explain(String sqlText, String datasourceCode) {
        long start = System.currentTimeMillis();
        String normalizedDatasource = trimToNull(datasourceCode);
        if (properties == null || !properties.isEnabled()) {
            return failed(normalizedDatasource, "HETU_PLAN_DISABLED", start, "enabled=false");
        }
        if (!StringUtils.hasText(normalizedDatasource)) {
            return failed(null, "HETU_PLAN_DATASOURCE_REQUIRED", start, "datasourceCode=missing");
        }
        HetuPlanAnalysisProperties.Datasource datasource = resolveDatasource(normalizedDatasource);
        if (datasource == null || !StringUtils.hasText(datasource.getJdbcUrl())) {
            return failed(normalizedDatasource, "HETU_PLAN_DATASOURCE_NOT_CONFIGURED", start, "datasourceCode=" + normalizedDatasource);
        }
        try {
            String explainSql = buildExplainSql(sqlText);
            if (StringUtils.hasText(datasource.getDriverClassName())) {
                Class.forName(datasource.getDriverClassName().trim());
            }
            try (Connection connection = DriverManager.getConnection(datasource.getJdbcUrl(), connectionProperties(datasource));
                 Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(resolveQueryTimeoutSeconds());
                try (ResultSet resultSet = statement.executeQuery(explainSql)) {
                    String planText = readPlanText(resultSet);
                    List<String> evidence = new ArrayList<String>();
                    evidence.add("sqlExecution=EXPLAIN_ONLY");
                    evidence.add("datasourceCode=" + normalizedDatasource);
                    evidence.add("driverConfigured=" + StringUtils.hasText(datasource.getDriverClassName()));
                    evidence.add("queryTimeoutSeconds=" + resolveQueryTimeoutSeconds());
                    return HetuPlanAnalysisResult.success(
                        normalizedDatasource,
                        planText,
                        System.currentTimeMillis() - start,
                        evidence
                    );
                }
            }
        } catch (Exception ex) {
            return HetuPlanAnalysisResult.failed(
                normalizedDatasource,
                "HETU_PLAN_EXPLAIN_FAILED: " + compact(ex.getMessage()),
                System.currentTimeMillis() - start,
                Collections.singletonList("sqlExecution=EXPLAIN_ONLY")
            );
        }
    }

    private HetuPlanAnalysisProperties.Datasource resolveDatasource(String datasourceCode) {
        Map<String, HetuPlanAnalysisProperties.Datasource> datasources = properties.getDatasources();
        if (datasources == null || datasources.isEmpty()) {
            return null;
        }
        HetuPlanAnalysisProperties.Datasource direct = datasources.get(datasourceCode);
        if (direct != null) {
            return direct;
        }
        return datasources.get(datasourceCode.toUpperCase(Locale.ROOT));
    }

    private Properties connectionProperties(HetuPlanAnalysisProperties.Datasource datasource) {
        Properties props = new Properties();
        if (StringUtils.hasText(datasource.getUsername())) {
            props.setProperty("user", datasource.getUsername().trim());
        }
        if (StringUtils.hasText(datasource.getPassword())) {
            props.setProperty("password", datasource.getPassword());
        }
        return props;
    }

    private String buildExplainSql(String sqlText) {
        String normalized = trimTrailingSemicolons(trimToNull(sqlText));
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("SQL text must not be empty for Hetu plan analysis");
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("EXPLAIN")) {
            throw new IllegalArgumentException("Submit the original SQL text; EXPLAIN statements are not accepted as input");
        }
        if (containsStatementSeparator(normalized)) {
            throw new IllegalArgumentException("Hetu plan analysis accepts a single SQL statement only");
        }
        return "EXPLAIN " + normalized;
    }

    private boolean containsStatementSeparator(String sqlText) {
        int cursor = 0;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            if (current == '\'' || current == '"' || current == '`') {
                cursor = readQuotedSegment(sqlText, cursor, current);
                continue;
            }
            if (current == '-' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '-') {
                cursor = readLineComment(sqlText, cursor);
                continue;
            }
            if (current == '/' && cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == '*') {
                cursor = readBlockComment(sqlText, cursor);
                continue;
            }
            if (current == ';') {
                return true;
            }
            cursor++;
        }
        return false;
    }

    private int readQuotedSegment(String sqlText, int start, char quote) {
        int cursor = start + 1;
        while (cursor < sqlText.length()) {
            char current = sqlText.charAt(cursor);
            if (current == quote) {
                if (cursor + 1 < sqlText.length() && sqlText.charAt(cursor + 1) == quote) {
                    cursor += 2;
                    continue;
                }
                return cursor + 1;
            }
            if (current == '\\') {
                cursor += 2;
                continue;
            }
            cursor++;
        }
        return cursor;
    }

    private int readLineComment(String sqlText, int start) {
        int newline = sqlText.indexOf('\n', start);
        return newline < 0 ? sqlText.length() : newline;
    }

    private int readBlockComment(String sqlText, int start) {
        int end = sqlText.indexOf("*/", start + 2);
        return end < 0 ? sqlText.length() : end + 2;
    }

    private String readPlanText(ResultSet resultSet) throws Exception {
        StringBuilder builder = new StringBuilder();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (resultSet.next() && builder.length() < MAX_PLAN_CHARS) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            for (int index = 1; index <= columnCount; index++) {
                if (index > 1) {
                    builder.append(" | ");
                }
                builder.append(resultSet.getString(index));
            }
        }
        return builder.length() > MAX_PLAN_CHARS ? builder.substring(0, MAX_PLAN_CHARS) : builder.toString();
    }

    private int resolveQueryTimeoutSeconds() {
        return properties.getQueryTimeoutSeconds() <= 0 ? 5 : properties.getQueryTimeoutSeconds();
    }

    private HetuPlanAnalysisResult failed(String datasourceCode, String reason, long start, String evidence) {
        return HetuPlanAnalysisResult.failed(
            datasourceCode,
            reason,
            System.currentTimeMillis() - start,
            Collections.singletonList(evidence)
        );
    }

    private String trimTrailingSemicolons(String value) {
        String normalized = value;
        while (StringUtils.hasText(normalized) && normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String compact(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return "unknown";
        }
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }
}
