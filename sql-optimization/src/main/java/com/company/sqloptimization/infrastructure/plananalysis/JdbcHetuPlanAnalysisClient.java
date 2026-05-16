package com.company.sqloptimization.infrastructure.plananalysis;

import com.company.sqloptimization.config.HetuPlanAnalysisProperties;
import com.company.sqloptimization.domain.parse.HetuPlanAnalysisResult;
import com.company.sqloptimization.infrastructure.governance.GovernanceCapabilityClient;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveRequest;
import com.company.sqlforge.common.governance.GovernanceJdbcDatasourceResolveResponse;
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
    private final GovernanceCapabilityClient governanceCapabilityClient;

    public JdbcHetuPlanAnalysisClient(HetuPlanAnalysisProperties properties,
                                      GovernanceCapabilityClient governanceCapabilityClient) {
        this.properties = properties;
        this.governanceCapabilityClient = governanceCapabilityClient;
    }

    @Override
    public HetuPlanAnalysisResult explain(String sqlText, String tenantId, String datasourceCode) {
        long start = System.currentTimeMillis();
        String normalizedDatasource = trimToNull(datasourceCode);
        if (!StringUtils.hasText(normalizedDatasource)) {
            return failed(null, "HETU_PLAN_DATASOURCE_REQUIRED", start, "datasourceCode=missing");
        }
        ResolvedDatasource datasource = resolveDatasource(tenantId, normalizedDatasource);
        if (datasource != null && StringUtils.hasText(datasource.failureReason)) {
            return failed(normalizedDatasource, datasource.failureReason, start, "configSource=" + datasource.configSource);
        }
        if (datasource == null || !StringUtils.hasText(datasource.jdbcUrl)) {
            return failed(normalizedDatasource, "HETU_JDBC_CONFIG_NOT_FOUND", start, "datasourceCode=" + normalizedDatasource);
        }
        try {
            String explainSql = buildExplainSql(sqlText);
            if (StringUtils.hasText(datasource.driverClassName)) {
                Class.forName(datasource.driverClassName.trim());
            }
            try (Connection connection = DriverManager.getConnection(datasource.jdbcUrl, connectionProperties(datasource));
                 Statement statement = connection.createStatement()) {
                int queryTimeoutSeconds = resolveQueryTimeoutSeconds(datasource);
                statement.setQueryTimeout(queryTimeoutSeconds);
                try (ResultSet resultSet = statement.executeQuery(explainSql)) {
                    String planText = readPlanText(resultSet);
                    List<String> evidence = new ArrayList<String>();
                    evidence.add("configSource=" + datasource.configSource);
                    evidence.add("sqlExecution=EXPLAIN_ONLY");
                    evidence.add("realJdbcExecution=true");
                    evidence.add("datasourceCode=" + normalizedDatasource);
                    evidence.add("timeoutMs=" + datasource.timeoutMs);
                    evidence.add("driverConfigured=" + StringUtils.hasText(datasource.driverClassName));
                    evidence.add("queryTimeoutSeconds=" + queryTimeoutSeconds);
                    return HetuPlanAnalysisResult.success(
                        normalizedDatasource,
                        planText,
                        System.currentTimeMillis() - start,
                        evidence
                    );
                }
            }
        } catch (ClassNotFoundException ex) {
            return HetuPlanAnalysisResult.failed(
                normalizedDatasource,
                "HETU_JDBC_CONNECT_FAILED: " + compact(ex.getMessage()),
                System.currentTimeMillis() - start,
                failureEvidence(datasource)
            );
        } catch (java.sql.SQLException ex) {
            return HetuPlanAnalysisResult.failed(
                normalizedDatasource,
                resolveSqlFailureReason(ex),
                System.currentTimeMillis() - start,
                failureEvidence(datasource)
            );
        } catch (Exception ex) {
            return HetuPlanAnalysisResult.failed(
                normalizedDatasource,
                "HETU_PLAN_EXPLAIN_FAILED: " + compact(ex.getMessage()),
                System.currentTimeMillis() - start,
                failureEvidence(datasource)
            );
        }
    }

    private ResolvedDatasource resolveDatasource(String tenantId, String datasourceCode) {
        GovernanceJdbcDatasourceResolveResponse governanceConfig = resolveFromGovernance(tenantId, datasourceCode);
        if (governanceConfig != null) {
            if (governanceConfig.isResolved()) {
                return ResolvedDatasource.fromGovernance(governanceConfig);
            }
            if (!"HETU_JDBC_CONFIG_NOT_FOUND".equals(governanceConfig.getFailureReason())) {
                return ResolvedDatasource.failed("GOVERNANCE_INTERNAL", governanceConfig.getFailureReason());
            }
        }
        return resolveLocalDatasource(datasourceCode);
    }

    private GovernanceJdbcDatasourceResolveResponse resolveFromGovernance(String tenantId, String datasourceCode) {
        if (governanceCapabilityClient == null || !StringUtils.hasText(tenantId)) {
            return null;
        }
        GovernanceJdbcDatasourceResolveRequest request = new GovernanceJdbcDatasourceResolveRequest();
        request.setTenantId(tenantId);
        request.setDatasourceCode(datasourceCode);
        request.setEngineType("HETU");
        try {
            return governanceCapabilityClient.resolveJdbcDatasource(request);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private ResolvedDatasource resolveLocalDatasource(String datasourceCode) {
        if (properties == null || !properties.isEnabled()) {
            return null;
        }
        Map<String, HetuPlanAnalysisProperties.Datasource> datasources = properties.getDatasources();
        if (datasources == null || datasources.isEmpty()) {
            return null;
        }
        HetuPlanAnalysisProperties.Datasource direct = datasources.get(datasourceCode);
        if (direct != null) {
            return ResolvedDatasource.fromLocal(datasourceCode, direct, resolveQueryTimeoutSeconds(null) * 1000);
        }
        HetuPlanAnalysisProperties.Datasource fallback = datasources.get(datasourceCode.toUpperCase(Locale.ROOT));
        return fallback == null ? null : ResolvedDatasource.fromLocal(datasourceCode, fallback, resolveQueryTimeoutSeconds(null) * 1000);
    }

    private Properties connectionProperties(ResolvedDatasource datasource) {
        Properties props = new Properties();
        if (StringUtils.hasText(datasource.username)) {
            props.setProperty("user", datasource.username.trim());
        }
        if (StringUtils.hasText(datasource.password)) {
            props.setProperty("password", datasource.password);
        }
        return props;
    }

    private String buildExplainSql(String sqlText) {
        String normalized = trimTrailingSemicolons(trimToNull(sqlText));
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("用于 Hetu 计划分析的 SQL 文本不能为空");
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if (upper.startsWith("EXPLAIN")) {
            throw new IllegalArgumentException("请提交原始 SQL 文本；不接受 EXPLAIN 语句作为输入");
        }
        if (containsStatementSeparator(normalized)) {
            throw new IllegalArgumentException("Hetu 计划分析只接受单条 SQL 语句");
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

    private int resolveQueryTimeoutSeconds(ResolvedDatasource datasource) {
        if (datasource != null && datasource.timeoutMs > 0) {
            return Math.max(1, (datasource.timeoutMs + 999) / 1000);
        }
        return properties == null || properties.getQueryTimeoutSeconds() <= 0 ? 5 : properties.getQueryTimeoutSeconds();
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

    private List<String> failureEvidence(ResolvedDatasource datasource) {
        List<String> evidence = new ArrayList<String>();
        evidence.add("sqlExecution=EXPLAIN_ONLY");
        if (datasource != null) {
            evidence.add("configSource=" + datasource.configSource);
            evidence.add("realJdbcExecution=true");
            evidence.add("timeoutMs=" + datasource.timeoutMs);
            evidence.add("driverConfigured=" + StringUtils.hasText(datasource.driverClassName));
        }
        return evidence;
    }

    private String resolveSqlFailureReason(java.sql.SQLException ex) {
        String message = compact(ex.getMessage());
        String state = trimToNull(ex.getSQLState());
        if (state == null || state.startsWith("08")) {
            return "HETU_JDBC_CONNECT_FAILED: " + message;
        }
        return "HETU_PLAN_EXPLAIN_FAILED: " + message;
    }

    private static final class ResolvedDatasource {

        private final String configSource;
        private final String jdbcUrl;
        private final String driverClassName;
        private final String username;
        private final String password;
        private final int timeoutMs;
        private final String failureReason;

        private ResolvedDatasource(String configSource,
                                   String jdbcUrl,
                                   String driverClassName,
                                   String username,
                                   String password,
                                   int timeoutMs,
                                   String failureReason) {
            this.configSource = configSource;
            this.jdbcUrl = jdbcUrl;
            this.driverClassName = driverClassName;
            this.username = username;
            this.password = password;
            this.timeoutMs = timeoutMs;
            this.failureReason = failureReason;
        }

        private static ResolvedDatasource fromGovernance(GovernanceJdbcDatasourceResolveResponse response) {
            return new ResolvedDatasource(
                "GOVERNANCE_INTERNAL",
                response.getJdbcUrl(),
                response.getDriverClassName(),
                response.getUsername(),
                response.getPassword(),
                response.getTimeoutMs() == null ? 5000 : response.getTimeoutMs().intValue(),
                null
            );
        }

        private static ResolvedDatasource fromLocal(String datasourceCode,
                                                    HetuPlanAnalysisProperties.Datasource datasource,
                                                    int timeoutMs) {
            return new ResolvedDatasource(
                "LOCAL_YML_FALLBACK",
                datasource.getJdbcUrl(),
                datasource.getDriverClassName(),
                datasource.getUsername(),
                datasource.getPassword(),
                timeoutMs,
                null
            );
        }

        private static ResolvedDatasource failed(String configSource, String failureReason) {
            return new ResolvedDatasource(configSource, null, null, null, null, 0, failureReason);
        }
    }
}
