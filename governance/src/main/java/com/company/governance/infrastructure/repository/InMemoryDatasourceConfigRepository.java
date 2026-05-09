package com.company.governance.infrastructure.repository;

import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.repository.DatasourceConfigRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatasourceConfigRepository implements DatasourceConfigRepository {

    private final Map<String, DatasourceConfig> configs = new ConcurrentHashMap<String, DatasourceConfig>();

    @Override
    public DatasourceConfig save(DatasourceConfig config) {
        configs.put(config.getDatasourceId(), config);
        return config;
    }

    @Override
    public Optional<DatasourceConfig> findByTenantIdAndDatasourceId(String tenantId, String datasourceId) {
        DatasourceConfig config = configs.get(datasourceId);
        if (config == null || !tenantId.equals(config.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    @Override
    public Optional<DatasourceConfig> findByTenantIdAndDatasourceCodeAndEngineType(String tenantId,
                                                                                   String datasourceCode,
                                                                                   String engineType) {
        for (DatasourceConfig config : configs.values()) {
            if (tenantId.equals(config.getTenantId())
                && equalsIgnoreCase(datasourceCode, config.getDatasourceCode())
                && equalsIgnoreCase(engineType, config.getEngineType())) {
                return Optional.of(config);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<DatasourceConfig> findByTenantId(String tenantId) {
        List<DatasourceConfig> result = new ArrayList<DatasourceConfig>();
        for (DatasourceConfig config : configs.values()) {
            if (tenantId.equals(config.getTenantId())) {
                result.add(config);
            }
        }
        result.sort(Comparator.comparing(DatasourceConfig::getDatasourceCode));
        return result;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equalsIgnoreCase(right);
    }
}
