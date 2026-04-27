package com.company.governance.infrastructure.repository;

import com.company.governance.domain.datasource.DatasourceConfig;
import com.company.governance.domain.datasource.repository.DatasourceConfigRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
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
}
