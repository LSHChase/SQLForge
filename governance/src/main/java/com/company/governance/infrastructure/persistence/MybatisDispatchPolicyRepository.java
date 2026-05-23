package com.company.governance.infrastructure.persistence;

import com.company.governance.domain.dispatchpolicy.DispatchPolicyConfig;
import com.company.governance.domain.dispatchpolicy.repository.DispatchPolicyRepository;
import com.company.governance.infrastructure.persistence.entity.DispatchPolicyRecord;
import com.company.governance.infrastructure.persistence.mapper.DispatchPolicyMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisDispatchPolicyRepository implements DispatchPolicyRepository {

    private final DispatchPolicyMapper mapper;

    public MybatisDispatchPolicyRepository(DispatchPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DispatchPolicyConfig save(DispatchPolicyConfig config) {
        mapper.upsert(toRecord(config));
        return config;
    }

    @Override
    public List<DispatchPolicyConfig> findByTenantId(String tenantId) {
        List<DispatchPolicyRecord> records = mapper.selectByTenantId(tenantId);
        List<DispatchPolicyConfig> result = new ArrayList<DispatchPolicyConfig>(records == null ? 0 : records.size());
        if (records != null) {
            for (DispatchPolicyRecord record : records) {
                result.add(toDomain(record));
            }
        }
        return result;
    }

    private DispatchPolicyRecord toRecord(DispatchPolicyConfig config) {
        DispatchPolicyRecord record = new DispatchPolicyRecord();
        record.setPolicyId(config.getPolicyId());
        record.setTenantId(config.getTenantId());
        record.setPolicyName(config.getPolicyName());
        record.setDispatchType(config.getDispatchType());
        record.setTargetEngine(config.getTargetEngine());
        record.setTargetDatasource(config.getTargetDatasource());
        record.setAckMode(config.getAckMode());
        record.setPullWindowSeconds(Integer.valueOf(config.getPullWindowSeconds()));
        record.setMaxBatchSize(Integer.valueOf(config.getMaxBatchSize()));
        record.setRetryStrategy(config.getRetryStrategy());
        record.setEnabled(Boolean.valueOf(config.isEnabled()));
        Instant now = config.getUpdatedAt() == null ? Instant.now() : config.getUpdatedAt();
        record.setCreateTime(now);
        record.setUpdateTime(now);
        return record;
    }

    private DispatchPolicyConfig toDomain(DispatchPolicyRecord record) {
        if (record == null) {
            return null;
        }
        return new DispatchPolicyConfig(
            record.getPolicyId(),
            record.getTenantId(),
            record.getPolicyName(),
            record.getDispatchType(),
            record.getTargetEngine(),
            record.getTargetDatasource(),
            record.getAckMode(),
            record.getPullWindowSeconds() == null ? 60 : record.getPullWindowSeconds().intValue(),
            record.getMaxBatchSize() == null ? 100 : record.getMaxBatchSize().intValue(),
            record.getRetryStrategy(),
            record.getEnabled() == null || Boolean.TRUE.equals(record.getEnabled()),
            record.getUpdateTime()
        );
    }
}
