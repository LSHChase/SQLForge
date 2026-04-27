package com.company.governance.domain.reportinterface.repository;

import com.company.governance.domain.reportinterface.ReportInterfaceConfig;
import java.util.List;
import java.util.Optional;

public interface ReportInterfaceConfigRepository {

    ReportInterfaceConfig save(ReportInterfaceConfig config);

    Optional<ReportInterfaceConfig> findByTenantIdAndConfigId(String tenantId, String configId);

    Optional<ReportInterfaceConfig> findBestMatch(String tenantId, String datasourceCode, String stage);

    List<ReportInterfaceConfig> findByTenantId(String tenantId);
}
