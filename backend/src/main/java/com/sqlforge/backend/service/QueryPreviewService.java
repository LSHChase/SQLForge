package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDescriptor;
import com.sqlforge.backend.model.QueryPreviewResult;
import com.sqlforge.backend.web.dto.QueryPreviewRequest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QueryPreviewService {

    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final JdbcConnectionSupportService jdbcConnectionSupportService;

    public QueryPreviewService(JdbcConnectionSupportService jdbcConnectionSupportService) {
        this.jdbcConnectionSupportService = jdbcConnectionSupportService;
    }

    public QueryPreviewResult preview(QueryPreviewRequest request) {
        EngineDescriptor engine = jdbcConnectionSupportService.findEngine(request.getEngineCode());
        String jdbcUrl = jdbcConnectionSupportService.buildJdbcUrl(request);
        String driverClassName = engine == null ? "" : engine.getDriverClassName();
        long startedAt = System.currentTimeMillis();

        if (!jdbcConnectionSupportService.isDriverAvailable(driverClassName)) {
            long duration = System.currentTimeMillis() - startedAt;
            return new QueryPreviewResult(
                "driver-missing",
                "missing",
                jdbcUrl,
                0,
                new ArrayList<String>(),
                new ArrayList<Map<String, Object>>(),
                duration,
                Arrays.asList("jdbc driver is not available on the classpath", driverClassName)
            );
        }

        try {
            return executePreview(jdbcUrl, request, DEFAULT_TIMEOUT_MS, startedAt);
        } catch (SQLException exception) {
            long duration = System.currentTimeMillis() - startedAt;
            return new QueryPreviewResult(
                "jdbc-query-failed",
                "loaded",
                jdbcUrl,
                0,
                new ArrayList<String>(),
                new ArrayList<Map<String, Object>>(),
                duration,
                Arrays.asList("jdbc query preview failed", exception.getMessage())
            );
        }
    }

    protected QueryPreviewResult executePreview(
        String jdbcUrl,
        QueryPreviewRequest request,
        int timeoutMs,
        long startedAt
    ) throws SQLException {
        try (
            Connection connection = jdbcConnectionSupportService.openConnection(jdbcUrl, request, timeoutMs);
            Statement statement = connection.createStatement()
        ) {
            int maxRows = Math.max(1, Math.min(request.getMaxRows(), 100));
            statement.setMaxRows(maxRows);
            boolean hasResultSet = statement.execute(request.getSql());

            if (!hasResultSet) {
                long duration = System.currentTimeMillis() - startedAt;
                return new QueryPreviewResult(
                    "statement-executed",
                    "loaded",
                    jdbcUrl,
                    statement.getUpdateCount(),
                    new ArrayList<String>(),
                    new ArrayList<Map<String, Object>>(),
                    duration,
                    Arrays.asList("statement executed without result set")
                );
            }

            try (ResultSet resultSet = statement.getResultSet()) {
                List<String> columns = new ArrayList<String>();
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                for (int index = 1; index <= columnCount; index += 1) {
                    columns.add(metaData.getColumnLabel(index));
                }

                while (resultSet.next()) {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();

                    for (int index = 1; index <= columnCount; index += 1) {
                        row.put(columns.get(index - 1), resultSet.getObject(index));
                    }

                    rows.add(row);
                }

                long duration = System.currentTimeMillis() - startedAt;
                return new QueryPreviewResult(
                    "result-set",
                    "loaded",
                    jdbcUrl,
                    rows.size(),
                    columns,
                    rows,
                    duration,
                    Arrays.asList("query preview succeeded", "rows limited to " + maxRows)
                );
            }
        }
    }
}
