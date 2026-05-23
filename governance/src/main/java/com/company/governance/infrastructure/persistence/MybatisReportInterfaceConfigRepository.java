package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.reportinterface.ReportInterfaceConfig;
import com.company.governance.domain.reportinterface.repository.ReportInterfaceConfigRepository;
import com.company.governance.infrastructure.persistence.entity.ReportInterfaceConfigRecord;
import com.company.governance.infrastructure.persistence.mapper.ReportInterfaceConfigMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisReportInterfaceConfigRepository implements ReportInterfaceConfigRepository {

    private final ReportInterfaceConfigMapper mapper;

    public MybatisReportInterfaceConfigRepository(ReportInterfaceConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public ReportInterfaceConfig save(ReportInterfaceConfig config) {
        mapper.upsert(toRecord(config));
        return config;
    }

    @Override
    public Optional<ReportInterfaceConfig> findByTenantIdAndConfigId(String tenantId, String configId) {
        return Optional.ofNullable(toDomain(mapper.selectByTenantIdAndConfigId(tenantId, configId)));
    }

    @Override
    public Optional<ReportInterfaceConfig> findBestMatch(String tenantId, String datasourceCode, String stage) {
        List<ReportInterfaceConfigRecord> candidates = mapper.selectMatchCandidates(tenantId, datasourceCode, stage);
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDomain(candidates.get(0)));
    }

    @Override
    public List<ReportInterfaceConfig> findByTenantId(String tenantId) {
        List<ReportInterfaceConfigRecord> records = mapper.selectByTenantId(tenantId);
        List<ReportInterfaceConfig> result = new ArrayList<ReportInterfaceConfig>(records == null ? 0 : records.size());
        if (records != null) {
            for (ReportInterfaceConfigRecord record : records) {
                result.add(toDomain(record));
            }
        }
        return result;
    }

    private ReportInterfaceConfigRecord toRecord(ReportInterfaceConfig config) {
        ReportInterfaceConfigRecord record = new ReportInterfaceConfigRecord();
        record.setConfigId(config.getConfigId());
        record.setTenantId(config.getTenantId());
        record.setDatasourceCode(config.getDatasourceCode());
        record.setStage(config.getStage());
        record.setSourceType(config.getSourceType());
        record.setEndpointCode(config.getEndpointCode());
        record.setEndpointName(config.getEndpointName());
        record.setBaseUrl(config.getBaseUrl());
        record.setPathTemplate(config.getPathTemplate());
        record.setHttpMethod(config.getHttpMethod());
        record.setReportCodeParamName(config.getReportCodeParamName());
        record.setSqlJsonPath(config.getSqlJsonPath());
        record.setAuthMode(config.getAuthMode());
        record.setTimeoutMs(Integer.valueOf(config.getTimeoutMs()));
        record.setEnabled(Boolean.valueOf(config.isEnabled()));
        Instant now = config.getUpdatedAt() == null ? Instant.now() : config.getUpdatedAt();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private ReportInterfaceConfig toDomain(ReportInterfaceConfigRecord record) {
        if (record == null) {
            return null;
        }
        return new ReportInterfaceConfig(
            record.getConfigId(),
            record.getTenantId(),
            record.getDatasourceCode(),
            record.getStage(),
            record.getSourceType(),
            record.getEndpointCode(),
            record.getEndpointName(),
            record.getBaseUrl(),
            record.getPathTemplate(),
            record.getHttpMethod(),
            record.getReportCodeParamName(),
            record.getSqlJsonPath(),
            record.getAuthMode(),
            record.getTimeoutMs() == null ? 3000 : record.getTimeoutMs().intValue(),
            record.getEnabled() == null || Boolean.TRUE.equals(record.getEnabled()),
            record.getUpdateTime()
        );
    }
}
