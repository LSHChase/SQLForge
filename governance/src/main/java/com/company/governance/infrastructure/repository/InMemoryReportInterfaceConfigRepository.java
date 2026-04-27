package com.company.governance.infrastructure.repository;

import com.company.governance.domain.reportinterface.ReportInterfaceConfig;
import com.company.governance.domain.reportinterface.repository.ReportInterfaceConfigRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class InMemoryReportInterfaceConfigRepository implements ReportInterfaceConfigRepository {

    private final Map<String, ReportInterfaceConfig> configs = new ConcurrentHashMap<String, ReportInterfaceConfig>();

    @Override
    public ReportInterfaceConfig save(ReportInterfaceConfig config) {
        configs.put(config.getConfigId(), config);
        return config;
    }

    @Override
    public Optional<ReportInterfaceConfig> findByTenantIdAndConfigId(String tenantId, String configId) {
        ReportInterfaceConfig config = configs.get(configId);
        if (config == null || !tenantId.equals(config.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    @Override
    public Optional<ReportInterfaceConfig> findBestMatch(String tenantId, String datasourceCode, String stage) {
        List<ReportInterfaceConfig> candidates = findByTenantId(tenantId);
        candidates.sort(Comparator.comparingInt(config -> -score(config, datasourceCode, stage)));
        for (ReportInterfaceConfig candidate : candidates) {
            if (score(candidate, datasourceCode, stage) >= 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ReportInterfaceConfig> findByTenantId(String tenantId) {
        List<ReportInterfaceConfig> result = new ArrayList<ReportInterfaceConfig>();
        for (ReportInterfaceConfig config : configs.values()) {
            if (tenantId.equals(config.getTenantId())) {
                result.add(config);
            }
        }
        result.sort(Comparator.comparing(ReportInterfaceConfig::getEndpointCode));
        return result;
    }

    private int score(ReportInterfaceConfig config, String datasourceCode, String stage) {
        int score = 0;
        String configDatasource = config.getDatasourceCode();
        if (StringUtils.hasText(configDatasource)) {
            if (!configDatasource.equals(datasourceCode)) {
                return -1;
            }
            score += 2;
        }
        String configStage = config.getStage();
        if (StringUtils.hasText(configStage)) {
            if (!configStage.equals(stage)) {
                return -1;
            }
            score += 1;
        }
        return score;
    }
}
