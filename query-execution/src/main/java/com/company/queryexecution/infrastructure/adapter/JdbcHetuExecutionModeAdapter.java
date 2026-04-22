package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.AccelerationPreference;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.utils.SqlFingerprintUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JdbcHetuExecutionModeAdapter implements HetuExecutionModeAdapter {

    private final QueryExecutionHetuProperties properties;

    public JdbcHetuExecutionModeAdapter(QueryExecutionHetuProperties properties) {
        this.properties = properties;
    }

    @Override
    public QueryExecutionAccessMode getMode() {
        return QueryExecutionAccessMode.JDBC;
    }

    @Override
    public QueryExecutionStep execute(String actualSql, QueryExecuteRequest request, boolean degradedPath) {
        QueryExecutionHetuProperties.Jdbc jdbc = properties.getJdbc();
        if (!StringUtils.hasText(jdbc.getUrl())) {
            throw new IllegalStateException("Hetu JDBC url is not configured");
        }
        long start = System.currentTimeMillis();
        try (Connection connection = DriverManager.getConnection(
            jdbc.getUrl(),
            jdbc.getUsername(),
            jdbc.getPassword()
        );
             PreparedStatement statement = connection.prepareStatement(actualSql)) {
            statement.setQueryTimeout(jdbc.getQueryTimeoutSeconds());
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Map<String, Object>> rows = readRows(resultSet, jdbc.getMaxRows());
                return new QueryExecutionStep(
                    com.company.sqlforge.common.constants.DataSourceTypeEnum.HETU,
                    rows,
                    System.currentTimeMillis() - start,
                    rows.size(),
                    false,
                    shouldApplyAcceleration(request, degradedPath),
                    QueryExecutionAccessMode.JDBC.name(),
                    Collections.singletonList(QueryExecutionAccessMode.JDBC.name())
                );
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Hetu JDBC execution failed", ex);
        }
    }

    private List<Map<String, Object>> readRows(ResultSet resultSet, int maxRows) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();
        while (resultSet.next() && rows.size() < maxRows) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("engine", "HETU");
            row.put("mode", "PRIMARY");
            row.put("executionMode", QueryExecutionAccessMode.JDBC.name());
            for (int index = 1; index <= columnCount; index++) {
                row.put(metadata.getColumnLabel(index), resultSet.getObject(index));
            }
            row.put("sqlFingerprint", SqlFingerprintUtils.fingerprint(String.valueOf(row)));
            rows.add(row);
        }
        return rows;
    }

    private boolean shouldApplyAcceleration(QueryExecuteRequest request, boolean degradedPath) {
        return !degradedPath
            && request != null
            && request.getAccelerationPreference() == AccelerationPreference.PREFER_ACCELERATED;
    }
}
