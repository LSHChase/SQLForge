package com.company.queryexecution.infrastructure.adapter;

import com.company.queryexecution.application.controller.dto.QueryExecuteRequest;
import com.company.queryexecution.config.QueryExecutionHetuProperties;
import com.company.queryexecution.domain.query.QueryExecutionAccessMode;
import com.company.queryexecution.domain.query.QueryExecutionStep;
import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ModeRoutingQueryExecutionAdapter implements QueryExecutionAdapter {

    private final QueryExecutionHetuProperties properties;
    private final DeterministicQueryExecutionAdapter deterministicQueryExecutionAdapter;
    private final Map<QueryExecutionAccessMode, HetuExecutionModeAdapter> modeAdapters;

    public ModeRoutingQueryExecutionAdapter(QueryExecutionHetuProperties properties,
                                            List<HetuExecutionModeAdapter> adapters) {
        this.properties = properties;
        this.deterministicQueryExecutionAdapter = new DeterministicQueryExecutionAdapter();
        this.modeAdapters = new LinkedHashMap<QueryExecutionAccessMode, HetuExecutionModeAdapter>();
        for (HetuExecutionModeAdapter adapter : adapters) {
            this.modeAdapters.put(adapter.getMode(), adapter);
        }
    }

    @Override
    public QueryExecutionStep execute(DataSourceTypeEnum targetEngine,
                                      String actualSql,
                                      QueryExecuteRequest request,
                                      boolean degradedPath) {
        if (targetEngine != DataSourceTypeEnum.HETU) {
            return deterministicQueryExecutionAdapter.execute(targetEngine, actualSql, request, degradedPath);
        }
        if (!properties.isEnabled()) {
            throw new HetuExecutionUnavailableException(
                "Hetu execution chain is disabled for the current environment",
                java.util.Collections.singletonList("CHAIN_DISABLED")
            );
        }
        if (properties.getAllowedModes().isEmpty()) {
            throw new HetuExecutionUnavailableException(
                "No Hetu execution mode is configured for the current environment",
                java.util.Collections.singletonList("CHAIN_UNCONFIGURED")
            );
        }
        List<String> attemptedModes = new ArrayList<String>();
        RuntimeException lastFailure = null;
        for (QueryExecutionAccessMode mode : properties.getAllowedModes()) {
            HetuExecutionModeAdapter adapter = modeAdapters.get(mode);
            if (adapter == null) {
                attemptedModes.add(mode.name() + ":UNAVAILABLE");
                continue;
            }
            try {
                attemptedModes.add(mode.name());
                QueryExecutionStep step = adapter.execute(actualSql, request, degradedPath);
                return step.withAttemptedModes(new ArrayList<String>(attemptedModes));
            } catch (RuntimeException ex) {
                attemptedModes.add(mode.name() + ":FAILED");
                lastFailure = ex;
            }
        }
        if (lastFailure != null) {
            throw new HetuExecutionUnavailableException(
                "No Hetu execution mode succeeded. attemptedModes=" + attemptedModes,
                attemptedModes,
                lastFailure
            );
        }
        throw new HetuExecutionUnavailableException(
            "No Hetu execution mode is available for the current configuration",
            attemptedModes
        );
    }
}
