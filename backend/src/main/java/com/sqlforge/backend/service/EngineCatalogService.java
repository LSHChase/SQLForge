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
                "com.mysql.cj.jdbc.Driver",
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
                "io.trino.jdbc.TrinoDriver",
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
                "com.facebook.presto.jdbc.PrestoDriver",
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
                "com.clickhouse.jdbc.ClickHouseDriver",
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
                "io.hetu.core.jdbc.HetuDriver",
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
                "org.apache.kylin.jdbc.Driver",
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
