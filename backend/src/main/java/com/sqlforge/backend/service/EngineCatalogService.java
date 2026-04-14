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
            new EngineDescriptor(
                "mysql",
                "MySQL",
                "oltp",
                3306,
                "tcp",
                "mysql",
                "default profile for transactional MySQL instances",
                true
            ),
            new EngineDescriptor(
                "trino",
                "Trino",
                "query-engine",
                8080,
                "http",
                "trino",
                "coordinator endpoint; many secure clusters use 8443",
                true
            ),
            new EngineDescriptor(
                "presto",
                "Presto",
                "query-engine",
                8080,
                "http",
                "presto",
                "classic coordinator endpoint for Presto deployments",
                true
            ),
            new EngineDescriptor(
                "clickhouse",
                "ClickHouse",
                "olap",
                8123,
                "http",
                "clickhouse",
                "http endpoint; native tcp deployments often use 9000",
                true
            ),
            new EngineDescriptor(
                "mrs-hetu",
                "MRS-Hetu",
                "query-engine",
                28443,
                "http",
                "mrs-hetu",
                "hetu-compatible coordinator profile for MRS distributions",
                true
            ),
            new EngineDescriptor(
                "kyligence",
                "Kyligence",
                "cube-engine",
                7070,
                "http",
                "kylin",
                "kylin-compatible profile used by Kyligence gateways",
                true
            )
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
