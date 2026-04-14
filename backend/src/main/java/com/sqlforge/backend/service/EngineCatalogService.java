package com.sqlforge.backend.service;

import com.sqlforge.backend.model.EngineDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

    public EngineDescriptor findByCode(String code) {
        if (code == null) {
            return null;
        }

        String normalized = code.toLowerCase(Locale.ROOT);

        for (EngineDescriptor descriptor : listSupportedEngines()) {
            if (descriptor.getCode().toLowerCase(Locale.ROOT).equals(normalized)) {
                return descriptor;
            }
        }

        return null;
    }
}
