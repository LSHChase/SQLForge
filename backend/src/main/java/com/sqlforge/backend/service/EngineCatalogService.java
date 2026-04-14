package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDescriptor;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EngineCatalogService {

    public List<EngineDescriptor> listSupportedEngines() {
        return Arrays.asList(
            new EngineDescriptor("mysql", "MySQL", "oltp", true),
            new EngineDescriptor("trino", "Trino", "query-engine", true),
            new EngineDescriptor("presto", "Presto", "query-engine", true),
            new EngineDescriptor("clickhouse", "ClickHouse", "olap", true),
            new EngineDescriptor("mrs-hetu", "MRS-Hetu", "query-engine", true),
            new EngineDescriptor("kyligence", "Kyligence", "cube-engine", true)
        );
    }
}
