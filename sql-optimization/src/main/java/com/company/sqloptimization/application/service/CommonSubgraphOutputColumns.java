package com.company.sqloptimization.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class CommonSubgraphOutputColumns {

    final List<String> columns;
    final Set<String> normalizedColumns;
    final List<Map<String, Object>> blockingReasons;

    CommonSubgraphOutputColumns(List<String> columns,
                                Set<String> normalizedColumns,
                                List<Map<String, Object>> blockingReasons) {
        this.columns = columns;
        this.normalizedColumns = normalizedColumns;
        this.blockingReasons = blockingReasons;
    }
}
