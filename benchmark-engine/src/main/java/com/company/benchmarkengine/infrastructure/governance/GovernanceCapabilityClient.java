package com.company.benchmarkengine.infrastructure.governance;

import com.company.sqlforge.common.constants.DataSourceTypeEnum;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkRegressionAlertResponse;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceRequest;
import com.company.sqlforge.common.governance.GovernanceBenchmarkReportTraceResponse;
import com.company.sqlforge.common.governance.GovernanceTenantArtifactPolicyResponse;

public interface GovernanceCapabilityClient {

    void assertAuthorization(String tenantId,
                             DataSourceTypeEnum datasourceType,
                             String resourceType,
                             String resourceId,
                             String operationCode);

    GovernanceBenchmarkReportTraceResponse writeBenchmarkReportTrace(GovernanceBenchmarkReportTraceRequest request);

    GovernanceBenchmarkRegressionAlertResponse emitBenchmarkRegressionAlert(GovernanceBenchmarkRegressionAlertRequest request);

    GovernanceTenantArtifactPolicyResponse resolveTenantArtifactPolicy(String tenantId, String policyScope);

    void writeAudit(BenchmarkAuditRecord auditRecord);
}
